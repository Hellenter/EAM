package io.github.hellenter.eam.world.structures.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Drops a placement that would start inside a piece of a structure in {@code structures}. Duergar villages are built
 * before the cave decorations, so without it giant mushrooms and boulders would grow in their streets. With a
 * {@code margin} (up to 16) it also drops one beside or above a piece within that many blocks, for springs whose water
 * would run down into the streets.
 */
public record OutsideStructureFilter(TagKey<Structure> structures, int margin) implements PlacementFilter {
	public static final MapCodec<OutsideStructureFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TagKey.codec(Registries.STRUCTURE).fieldOf("structures").forGetter(OutsideStructureFilter::structures),
			Codec.intRange(0, 16).optionalFieldOf("margin", 0).forGetter(OutsideStructureFilter::margin))
			.apply(i, OutsideStructureFilter::new));

	public OutsideStructureFilter(TagKey<Structure> structures) {
		this(structures, 0);
	}

	@Override
	public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
		if (!(context.getLevel() instanceof WorldGenRegion region)) return true;
		if (margin > 0) {
			return !VillageArea.around(context.getLevel(), origin, margin, structures).overlooks(origin, 0);
		}
		return !region.getLevel().structureManager().forWorldGenRegion(region).getStructureWithPieceAt(origin, structures).isValid();
	}

	@Override
	public MapCodec<OutsideStructureFilter> codec() {
		return CODEC;
	}
}
