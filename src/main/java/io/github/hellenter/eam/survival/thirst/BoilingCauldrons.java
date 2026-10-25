package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Water cauldrons in a chunk that are heating or have boiled: ticks each one has spent over a heat source, keyed by
 * {@code BlockPos.asLong}. The water is boiled once its heat reaches the boil time. Immutable.
 */
public record BoilingCauldrons(Map<Long, Integer> heat) {
	public static final BoilingCauldrons EMPTY = new BoilingCauldrons(Map.of());

	private record Entry(long pos, int heat) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
				Codec.INT.fieldOf("heat").forGetter(Entry::heat)
		).apply(instance, Entry::new));
	}

	public static final Codec<BoilingCauldrons> CODEC = Entry.CODEC.listOf().xmap(
			list -> {
				Map<Long, Integer> map = new HashMap<>();
				list.forEach(entry -> map.put(entry.pos(), entry.heat()));
				return new BoilingCauldrons(map);
			},
			cauldrons -> cauldrons.heat.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

	public BoilingCauldrons {
		heat = Map.copyOf(heat);
	}

	public int heat(long pos) {
		return heat.getOrDefault(pos, 0);
	}

	/** Sets the heat of one cauldron; zero forgets it. */
	public BoilingCauldrons with(long pos, int value) {
		Map<Long, Integer> copy = new HashMap<>(heat);
		if (value > 0) {
			copy.put(pos, value);
		} else {
			copy.remove(pos);
		}
		return new BoilingCauldrons(copy);
	}

	public boolean isEmpty() {
		return heat.isEmpty();
	}
}
