package io.github.hellenter.eam.world.structures.village;

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
 * before the cave decorations, so without it giant mushrooms and boulders would grow in their streets.
 */
public record OutsideStructureFilter(TagKey<Structure> structures) implements PlacementFilter {
	public static final MapCodec<OutsideStructureFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TagKey.codec(Registries.STRUCTURE).fieldOf("structures").forGetter(OutsideStructureFilter::structures))
			.apply(i, OutsideStructureFilter::new));

	@Override
	public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
		if (!(context.getLevel() instanceof WorldGenRegion region)) return true;
		return !region.getLevel().structureManager().forWorldGenRegion(region).getStructureWithPieceAt(origin, structures).isValid();
	}

	@Override
	public MapCodec<OutsideStructureFilter> codec() {
		return CODEC;
	}
}
