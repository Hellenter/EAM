package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Positions in a chunk of water cauldrons filled only by rain, packed with {@code BlockPos.asLong}. Immutable. */
public record RainCauldrons(Set<Long> positions) {
	public static final RainCauldrons EMPTY = new RainCauldrons(Set.of());

	public static final Codec<RainCauldrons> CODEC = Codec.LONG.listOf()
			.xmap(list -> new RainCauldrons(Set.copyOf(list)), cauldrons -> List.copyOf(cauldrons.positions));

	public RainCauldrons {
		positions = Set.copyOf(positions);
	}

	public boolean contains(long pos) {
		return positions.contains(pos);
	}

	public RainCauldrons with(long pos) {
		Set<Long> copy = new HashSet<>(positions);
		copy.add(pos);
		return new RainCauldrons(copy);
	}

	public RainCauldrons without(long pos) {
		Set<Long> copy = new HashSet<>(positions);
		copy.remove(pos);
		return new RainCauldrons(copy);
	}
}
