package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.Eam;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
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
 * the swamp.
 */
public final class SwampyForestBiomes {
	public static final ResourceKey<Biome> SWAMPY_FOREST = key("swampy_forest");
	public static final ResourceKey<Biome> SWAMPY_LAKES = key("swampy_lakes");
	public static final ResourceKey<Biome> SWAMPY_HUMMOCKS = key("swampy_hummocks");
	public static final List<ResourceKey<Biome>> ALL = List.of(SWAMPY_FOREST, SWAMPY_LAKES, SWAMPY_HUMMOCKS);

	/** Land biomes a swampy forest region may take over; rivers, beaches, oceans, mountains and caves stay. */
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
		Long2ObjectOpenHashMap<SwampyForestLogic.Part> columns = new Long2ObjectOpenHashMap<>();
		return (quartX, quartY, quartZ) -> {
			Holder<Biome> biome = vanilla.getNoiseBiome(quartX, quartY, quartZ);
			if (!biome.is(REPLACEABLE)) {
				return biome;
			}
			long column = ChunkPos.pack(quartX, quartZ);
			SwampyForestLogic.Part part = columns.get(column);
			if (part == null && !columns.containsKey(column)) {
				part = choose(sampler, quartX, quartZ);
				columns.put(column, part);
			}
			return holders.or(part, biome);
		};
	}

	private static SwampyForestLogic.@Nullable Part choose(Climate.Sampler sampler, int quartX, int quartZ) {
		int x = QuartPos.toBlock(quartX);
		int z = QuartPos.toBlock(quartZ);
		int regionX = SwampyForestLogic.regionCoordinate(x);
		int regionZ = SwampyForestLogic.regionCoordinate(z);
		return SwampyForestLogic.choose(
				sampler.temperature().sampleValue(regionX, 0, regionZ),
				sampler.humidity().sampleValue(regionX, 0, regionZ),
				sampler.continentalness().sampleValue(regionX, 0, regionZ),
				sampler.humidity().sampleValue(x, 0, z));
	}

	private record Holders(Holder<Biome> forest, Holder<Biome> lakes, Holder<Biome> hummocks) {
		static @Nullable Holders find(MultiNoiseBiomeSource source) {
			Holder<Biome> forest = null;
			Holder<Biome> lakes = null;
			Holder<Biome> hummocks = null;
			for (Holder<Biome> biome : source.possibleBiomes()) {
				if (biome.is(SWAMPY_FOREST)) {
					forest = biome;
				} else if (biome.is(SWAMPY_LAKES)) {
					lakes = biome;
				} else if (biome.is(SWAMPY_HUMMOCKS)) {
					hummocks = biome;
				}
			}
			return forest == null || lakes == null || hummocks == null ? null : new Holders(forest, lakes, hummocks);
		}

		Holder<Biome> or(SwampyForestLogic.@Nullable Part part, Holder<Biome> vanilla) {
			if (part == null) {
				return vanilla;
			}
			return switch (part) {
				case FOREST -> this.forest;
				case LAKES -> this.lakes;
				case HUMMOCKS -> this.hummocks;
			};
		}
	}

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, Eam.id(name));
	}
}
