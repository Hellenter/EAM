package io.github.hellenter.eam.world.structures.castle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.List;
import java.util.Optional;

/**
 * A random spread that keeps away from several other structure sets, where vanilla's keeps away from one: the castle
 * must neither swallow a village nor stand on an outpost.
 */
public class CastlePlacement extends RandomSpreadStructurePlacement {
	private static final MapCodec<CastlePlacement> OWN_CODEC = RecordCodecBuilder.mapCodec(instance -> placementCodec(instance).and(instance.group(
			Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
			Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
			RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType),
			ExclusionZone.CODEC.listOf().fieldOf("avoid").forGetter(placement -> placement.avoid)
	)).apply(instance, CastlePlacement::new));
	@SuppressWarnings("unchecked")
	public static final MapCodec<RandomSpreadStructurePlacement> CODEC = (MapCodec<RandomSpreadStructurePlacement>) (MapCodec<?>) OWN_CODEC;

	private final List<ExclusionZone> avoid;

	public CastlePlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt, Optional<ExclusionZone> exclusionZone,
			int spacing, int separation, RandomSpreadType spreadType, List<ExclusionZone> avoid) {
		super(locateOffset, method, frequency, salt, exclusionZone, spacing, separation, spreadType);
		this.avoid = List.copyOf(avoid);
	}

	public CastlePlacement(int spacing, int separation, float frequency, int salt, List<ExclusionZone> avoid) {
		this(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, frequency, salt, Optional.empty(),
				spacing, separation, RandomSpreadType.LINEAR, avoid);
	}

	@Override
	public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
		if (!super.applyInteractionsWithOtherStructures(state, x, z)) return false;
		for (ExclusionZone zone : avoid) {
			if (state.hasStructureChunkInRange(zone.otherSet(), x, z, zone.chunkCount())) return false;
		}
		return true;
	}

	@Override
	public MapCodec<RandomSpreadStructurePlacement> codec() {
		return CODEC;
	}
}
