package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.Eam;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The swampy forest biomes and how they get into the overworld. There is no Fabric API for new overworld biomes, so two
 * small mixins call this class: one lists the biomes in the overworld biome list (at a climate point no spot ever
 * reaches, so vanilla never picks them itself, but chunk generators know their features), the other wraps the overworld
 * biome resolver so that inside a swampy forest region ({@link SwampyForestLogic}) the replaceable land biomes become
 * the swamp, and in a band around it plain vanilla forests, so the swamp never borders mountains or deserts directly.
 */
public final class SwampyForestBiomes {
	public static final ResourceKey<Biome> SWAMPY_FOREST = key("swampy_forest");
	public static final ResourceKey<Biome> SWAMPY_LAKES = key("swampy_lakes");
	public static final ResourceKey<Biome> SWAMPY_HUMMOCKS = key("swampy_hummocks");
	public static final List<ResourceKey<Biome>> ALL = List.of(SWAMPY_FOREST, SWAMPY_LAKES, SWAMPY_HUMMOCKS);

	/** Land biomes a swampy forest region takes over (all of them, since its terrain is flattened); oceans, beaches and caves stay. */
	public static final TagKey<Biome> REPLACEABLE = TagKey.create(Registries.BIOME, Eam.id("swampy_forest_replaceable"));
	/** All three parts of the swamp, for spawns and features that belong to the whole swampy forest. */
	public static final TagKey<Biome> IS_SWAMPY_FOREST = TagKey.create(Registries.BIOME, Eam.id("is_swampy_forest"));

	/** Far outside every real climate (the codec allows -2..2) with the largest offset: never the nearest point. */
	private static final Climate.ParameterPoint UNREACHABLE = Climate.parameters(2.0F, 2.0F, 2.0F, 2.0F, 2.0F, 2.0F, 1.0F);

	private static volatile boolean enabled;

	private SwampyForestBiomes() {
	}

	static void enable() {
		enabled = true;
	}

	static boolean enabled() {
		return enabled;
	}

	/** Called at the end of the vanilla overworld biome list. */
	public static void addToOverworldList(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
		if (enabled) {
			for (ResourceKey<Biome> biome : ALL) {
				biomes.accept(Pair.of(UNREACHABLE, biome));
			}
		}
	}

	/**
	 * Wraps the overworld's biome resolver. A chunk's resolver keeps the decision per column, since every input to it is
	 * a 2D noise; the long-lived resolvers are shared between threads, so they keep nothing.
	 */
	public static BiomeResolver wrap(BiomeResolver vanilla, Climate.Sampler sampler, MultiNoiseBiomeSource source, boolean perChunk) {
		if (!enabled || !source.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) {
			return vanilla;
		}
		Holders holders = Holders.find(source);
		if (holders == null) {
			return vanilla;
		}
		if (!perChunk) {
			return (quartX, quartY, quartZ) -> {
				Holder<Biome> biome = vanilla.getNoiseBiome(quartX, quartY, quartZ);
				return biome.is(REPLACEABLE) ? holders.or(choose(sampler, quartX, quartZ), biome) : biome;
			};
		}
		Long2ObjectOpenHashMap<Pick> columns = new Long2ObjectOpenHashMap<>();
		return (quartX, quartY, quartZ) -> {
			Holder<Biome> biome = vanilla.getNoiseBiome(quartX, quartY, quartZ);
			if (!biome.is(REPLACEABLE)) {
				return biome;
			}
			long column = ChunkPos.pack(quartX, quartZ);
			Pick pick = columns.get(column);
			if (pick == null) {
				pick = choose(sampler, quartX, quartZ);
				columns.put(column, pick);
			}
			return holders.or(pick, biome);
		};
	}

	/** What a replaceable column becomes: a part of the swamp, a forest of its rim, or nothing (vanilla stays). */
	private enum Pick {
		FOREST, LAKES, HUMMOCKS, RIM_FOREST, RIM_BIRCH, RIM_DARK, NONE
	}

	private static Pick choose(Climate.Sampler sampler, int quartX, int quartZ) {
		int x = QuartPos.toBlock(quartX);
		int z = QuartPos.toBlock(quartZ);
		SwampyForestLogic.Climate climate = climate(sampler);
		float weight = SwampyForestLogic.weight(climate, x, z);
		if (SwampyForestLogic.inSwamp(weight)) {
			return switch (SwampyForestLogic.part(climate.humidity(x, z))) {
				case FOREST -> Pick.FOREST;
				case LAKES -> Pick.LAKES;
				case HUMMOCKS -> Pick.HUMMOCKS;
			};
		}
		if (!SwampyForestLogic.inRim(climate, x, z, weight)) {
			return Pick.NONE;
		}
		return switch (SwampyForestLogic.rimForest(x, z)) {
			case FOREST -> Pick.RIM_FOREST;
			case BIRCH -> Pick.RIM_BIRCH;
			case DARK -> Pick.RIM_DARK;
		};
	}

	private static SwampyForestLogic.Climate climate(Climate.Sampler sampler) {
		return new SwampyForestLogic.Climate() {
			@Override
			public float temperature(int x, int z) {
				return sampler.temperature().sampleValue(x, 0, z);
			}

			@Override
			public float humidity(int x, int z) {
				return sampler.humidity().sampleValue(x, 0, z);
			}

			@Override
			public float continents(int x, int z) {
				return sampler.continentalness().sampleValue(x, 0, z);
			}

			@Override
			public float relief(int x, int z) {
				return VanillaRelief.height(sampler.continentalness().sampleValue(x, 0, z), sampler.erosion().sampleValue(x, 0, z),
						sampler.weirdness().sampleValue(x, 0, z));
			}
		};
	}

	private record Holders(Holder<Biome> forest, Holder<Biome> lakes, Holder<Biome> hummocks, Holder<Biome> rimForest,
			Holder<Biome> rimBirch, Holder<Biome> rimDark) {
		static @Nullable Holders find(MultiNoiseBiomeSource source) {
			Holder<Biome> forest = null;
			Holder<Biome> lakes = null;
			Holder<Biome> hummocks = null;
			Holder<Biome> rimForest = null;
			Holder<Biome> rimBirch = null;
			Holder<Biome> rimDark = null;
			for (Holder<Biome> biome : source.possibleBiomes()) {
				if (biome.is(SWAMPY_FOREST)) {
					forest = biome;
				} else if (biome.is(SWAMPY_LAKES)) {
					lakes = biome;
				} else if (biome.is(SWAMPY_HUMMOCKS)) {
					hummocks = biome;
				} else if (biome.is(Biomes.FOREST)) {
					rimForest = biome;
				} else if (biome.is(Biomes.BIRCH_FOREST)) {
					rimBirch = biome;
				} else if (biome.is(Biomes.DARK_FOREST)) {
					rimDark = biome;
				}
			}
			if (forest == null || lakes == null || hummocks == null || rimForest == null || rimBirch == null || rimDark == null) {
				return null;
			}
			return new Holders(forest, lakes, hummocks, rimForest, rimBirch, rimDark);
		}

		Holder<Biome> or(Pick pick, Holder<Biome> vanilla) {
			return switch (pick) {
				case FOREST -> this.forest;
				case LAKES -> this.lakes;
				case HUMMOCKS -> this.hummocks;
				// The rim leaves forests, taiga and rivers as they are; everything else becomes forest.
				case RIM_FOREST, RIM_BIRCH, RIM_DARK -> keepsInRim(vanilla) ? vanilla : switch (pick) {
					case RIM_BIRCH -> this.rimBirch;
					case RIM_DARK -> this.rimDark;
					default -> this.rimForest;
				};
				case NONE -> vanilla;
			};
		}

		private static boolean keepsInRim(Holder<Biome> vanilla) {
			return vanilla.is(BiomeTags.IS_FOREST) || vanilla.is(BiomeTags.IS_TAIGA) || vanilla.is(BiomeTags.IS_RIVER);
		}
	}

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, Eam.id(name));
	}
}
