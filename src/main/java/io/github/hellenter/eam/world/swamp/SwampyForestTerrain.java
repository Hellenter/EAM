package io.github.hellenter.eam.world.swamp;

import com.google.common.collect.MapMaker;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Lays the swamp plain into the overworld terrain. The final density becomes a {@link SwampyGround}: the plain's
 * surface and crust over vanilla's density, blended in by {@link SwampyFlatness}. The preliminary surface (what the
 * aquifers and surface rules read) blends to the plain's {@link SwampyHeight} the same way, and vanilla's own
 * {@code offset}, {@code factor} and {@code jaggedness} are squeezed toward sea level so that what lies under the crust
 * and on the rim stays low too. Caves below are untouched, and the biome map is not involved.
 *
 * <p>The chunk generator asks its random state for samplers of the functions in its settings, not in the router the
 * random state was made with, so {@link #substitute} swaps the settings' final density for the swamp one there.
 */
public final class SwampyForestTerrain {
	/** Routers this class rewrote, by identity, with the rewrite to apply to their aquifer settings too. */
	private static final Map<NoiseRouter, SwampyForestTerrain> REWRITTEN = new MapMaker().weakKeys().makeMap();

	/** Vanilla's offset is clamped to this and squeezed to this share of its distance from {@link #FLAT_OFFSET}. */
	static final float OFFSET_LIMIT = 0.3F;
	static final float OFFSET_SQUEEZE = 0.15F;
	/** Terrain offset of the swamp plain (0 is about y 64, 0.0078 a block). */
	static final float FLAT_OFFSET = -0.01F;
	/** Vanilla's terrain factor: the higher, the less the 3D noise bends the ground. Flat plains are about 6. */
	static final float MIN_FACTOR = 12.0F;

	/**
	 * Preliminary surface the aquifers see under the swamp: low enough that every lake bed below sea level counts as
	 * under the sea, so the lakes fill with water to sea level instead of being left as dry pits.
	 */
	static final float AQUIFER_SURFACE = SwampyForestLogic.SEA_LEVEL - 13;

	private final DfRewriteRule rule;
	private final DensityFunction weight;
	private final DensityFunction vanillaFinalDensity;
	private @Nullable DensityFunction finalDensity;
	private volatile Aquifer.@Nullable Config aquifersIn;
	private volatile Aquifer.@Nullable Config aquifersOut;

	private SwampyForestTerrain(DensityFunction weight, DensityFunction height, DensityFunction vanillaFinalDensity) {
		this.weight = weight;
		this.vanillaFinalDensity = vanillaFinalDensity;
		this.rule = new DfRewriteRule() {
			@Override
			public DensityFunction rewrite(DensityFunction function) {
				if (function instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder)) {
					String name = terrainFunction(holder);
					if (name != null) {
						return DensityFunctions.cache(flatten(name, function, weight, height));
					}
				}
				return function.rewriteChildren(this);
			}
		};
	}

	/** The overworld router with flat swamps, or the router unchanged when the swamp is off or it is another dimension. */
	public static NoiseRouter flatten(NoiseRouter router) {
		if (!SwampyForestBiomes.enabled() || !isOverworld(router.finalDensity())) {
			return router;
		}
		DensityFunction weight = DensityFunctions.cache(new SwampyFlatness(router.temperature(), router.vegetation(), router.continents()));
		DensityFunction height = DensityFunctions.cache(new SwampyHeight(router.vegetation()));
		SwampyForestTerrain terrain = new SwampyForestTerrain(weight, height, router.finalDensity());
		DensityFunction finalDensity = new SwampyGround(terrain.rule.rewrite(router.finalDensity()), weight, height);
		terrain.finalDensity = finalDensity;
		// Biome depth is 0 on the plain's surface and grows by 1 every 128 blocks down, so caves keep their biomes under it.
		DensityFunction plainDepth = DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 0.5F, -2.5F),
				height.mul(1.0F / 128.0F));
		NoiseRouter flat = new NoiseRouter(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
				DensityFunctions.lerp(weight, terrain.rule.rewrite(router.depth()), plainDepth), router.ridges(), terrain.rule.rewrite(router.chunkSurfaceLevel()), finalDensity);
		REWRITTEN.put(flat, terrain);
		return flat;
	}

	/** The function a random state with this router should really sample: the swamp's final density for vanilla's. */
	public static DensityFunction substitute(NoiseRouter router, DensityFunction function) {
		SwampyForestTerrain terrain = REWRITTEN.get(router);
		return terrain != null && function == terrain.vanillaFinalDensity && terrain.finalDensity != null ? terrain.finalDensity : function;
	}

	/** The aquifer settings matching a router from {@link #flatten}, so aquifers fill the swamp's lakes. */
	public static Aquifer.Config aquifers(NoiseRouter router, Aquifer.Config aquifers) {
		SwampyForestTerrain terrain = REWRITTEN.get(router);
		return terrain == null ? aquifers : terrain.rewrite(aquifers);
	}

	private Aquifer.Config rewrite(Aquifer.Config aquifers) {
		if (aquifers != this.aquifersIn) {
			Aquifer.Config out = new Aquifer.Config(aquifers.barrierNoise(), aquifers.fluidLevelFloodednessNoise(),
					aquifers.fluidLevelSpreadNoise(), aquifers.lavaNoise(), aquifers.exclusion(),
					DensityFunctions.lerp(this.weight, this.rule.rewrite(aquifers.surfaceLevel()), DensityFunctions.constant(AQUIFER_SURFACE)));
			this.aquifersOut = out;
			this.aquifersIn = aquifers;
			return out;
		}
		return this.aquifersOut;
	}

	private static DensityFunction flatten(String name, DensityFunction vanilla, DensityFunction weight, DensityFunction height) {
		DensityFunction flat = switch (name) {
			case "offset" -> vanilla.clamp(-OFFSET_LIMIT, OFFSET_LIMIT).mul(OFFSET_SQUEEZE).add(FLAT_OFFSET * (1.0F - OFFSET_SQUEEZE));
			case "factor" -> DensityFunctions.max(vanilla, DensityFunctions.constant(MIN_FACTOR));
			case "preliminary_surface_level" -> height;
			default -> DensityFunctions.zero();
		};
		return DensityFunctions.lerp(weight, vanilla, flat);
	}

	/** The name of a vanilla overworld terrain function this class blends (any overworld preset), else null. */
	private static @Nullable String terrainFunction(Holder<DensityFunction> holder) {
		ResourceKey<DensityFunction> key = holder.unwrapKey().orElse(null);
		if (key == null || !key.identifier().getNamespace().equals("minecraft")) {
			return null;
		}
		String path = key.identifier().getPath();
		int slash = path.indexOf('/');
		if (slash < 0 || !path.startsWith("overworld")) {
			return null;
		}
		String name = path.substring(slash + 1);
		return switch (name) {
			case "offset", "factor", "jaggedness", "preliminary_surface_level" -> name;
			default -> null;
		};
	}

	private static boolean isOverworld(DensityFunction finalDensity) {
		return finalDensity instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder)
				&& holder.unwrapKey().map(key -> key.identifier().getPath().startsWith("overworld")).orElse(false);
	}
}
