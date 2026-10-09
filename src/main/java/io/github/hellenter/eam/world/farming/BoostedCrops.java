package io.github.hellenter.eam.world.farming;

import com.mojang.serialization.Codec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Positions in a chunk of crops that got bone meal, packed with {@code BlockPos.asLong}. Immutable. */
public record BoostedCrops(Set<Long> positions) {
	public static final BoostedCrops EMPTY = new BoostedCrops(Set.of());

	public static final Codec<BoostedCrops> CODEC = Codec.LONG.listOf()
			.xmap(list -> new BoostedCrops(Set.copyOf(list)), crops -> List.copyOf(crops.positions));

	public BoostedCrops {
		positions = Set.copyOf(positions);
	}

	public boolean contains(long pos) {
		return positions.contains(pos);
	}

	public BoostedCrops with(long pos) {
		Set<Long> copy = new HashSet<>(positions);
		copy.add(pos);
		return new BoostedCrops(copy);
	}

	public BoostedCrops without(long pos) {
		Set<Long> copy = new HashSet<>(positions);
		copy.remove(pos);
		return new BoostedCrops(copy);
	}
}
