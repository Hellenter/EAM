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
 * Flattens the overworld terrain under the swampy forest. Vanilla shapes the ground from three named density functions:
 * {@code offset} (base height), {@code factor} (how much the 3D noise bends it) and {@code jaggedness} (peaks). Every
 * function built on them (final density, depth, the preliminary surface the aquifers and surface rules use) gets them
 * swapped for blends that, by {@link SwampyFlatness}, squeeze the offset toward sea level, raise the factor and drop the
 * jaggedness. Caves below are untouched, and the biome map is not involved, so nothing else in the world moves.
 */
public final class SwampyForestTerrain {
	/** Routers this class rewrote, by identity, with the rewrite to apply to their aquifer settings too. */
	private static final Map<NoiseRouter, SwampyForestTerrain> REWRITTEN = new MapMaker().weakKeys().makeMap();

	private final DfRewriteRule rule;
	private volatile Aquifer.@Nullable Config aquifersIn;
	private volatile Aquifer.@Nullable Config aquifersOut;

	private SwampyForestTerrain(DensityFunction flatness) {
		DensityFunction weight = DensityFunctions.cache(flatness);
		this.rule = new DfRewriteRule() {
			@Override
			public DensityFunction rewrite(DensityFunction function) {
				if (function instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder)) {
					String name = terrainFunction(holder);
					if (name != null) {
						return DensityFunctions.cache(flatten(name, function, weight));
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
		SwampyForestTerrain terrain = new SwampyForestTerrain(new SwampyFlatness(router.temperature(), router.vegetation(), router.continents()));
		NoiseRouter flat = new NoiseRouter(router.temperature(), router.vegetation(), router.continents(), router.erosion(),
				terrain.rule.rewrite(router.depth()), router.ridges(), terrain.rule.rewrite(router.chunkSurfaceLevel()),
				terrain.rule.rewrite(router.finalDensity()));
		REWRITTEN.put(flat, terrain);
		return flat;
	}

	/** The aquifer settings matching a router from {@link #flatten}, so aquifers see the flat surface. */
	public static Aquifer.Config aquifers(NoiseRouter router, Aquifer.Config aquifers) {
		SwampyForestTerrain terrain = REWRITTEN.get(router);
		return terrain == null ? aquifers : terrain.rewrite(aquifers);
	}

	private Aquifer.Config rewrite(Aquifer.Config aquifers) {
		if (aquifers != this.aquifersIn) {
			Aquifer.Config out = new Aquifer.Config(aquifers.barrierNoise(), aquifers.fluidLevelFloodednessNoise(),
					aquifers.fluidLevelSpreadNoise(), aquifers.lavaNoise(), aquifers.exclusion(), this.rule.rewrite(aquifers.surfaceLevel()));
			this.aquifersOut = out;
			this.aquifersIn = aquifers;
			return out;
		}
		return this.aquifersOut;
	}

	private static DensityFunction flatten(String name, DensityFunction vanilla, DensityFunction weight) {
		DensityFunction flat = switch (name) {
			case "offset" -> vanilla.clamp(-SwampyForestLogic.OFFSET_LIMIT, SwampyForestLogic.OFFSET_LIMIT).mul(SwampyForestLogic.OFFSET_SQUEEZE)
					.add(SwampyForestLogic.FLAT_OFFSET * (1.0F - SwampyForestLogic.OFFSET_SQUEEZE));
			case "factor" -> DensityFunctions.max(vanilla, DensityFunctions.constant(SwampyForestLogic.MIN_FACTOR));
			default -> DensityFunctions.zero();
		};
		return DensityFunctions.lerp(weight, vanilla, flat);
	}

	/** "offset", "factor" or "jaggedness" for vanilla's overworld terrain functions (any overworld preset), else null. */
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
		return name.equals("offset") || name.equals("factor") || name.equals("jaggedness") ? name : null;
	}

	private static boolean isOverworld(DensityFunction finalDensity) {
		return finalDensity instanceof DensityFunctions.HolderHolder(Holder<DensityFunction> holder)
				&& holder.unwrapKey().map(key -> key.identifier().getPath().startsWith("overworld")).orElse(false);
	}
}
