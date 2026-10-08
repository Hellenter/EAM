package io.github.hellenter.eam.world.structures;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/** Pure rules for picking an abandoned village start. */
public final class VillageLogic {
	private VillageLogic() {
	}

	/** Vanilla keeps the abandoned (zombie) variant of every village piece under a {@code zombie/} folder. */
	public static boolean isAbandoned(Identifier template) {
		return template.getPath().contains("/zombie/");
	}

	/**
	 * Picks one abandoned entry by weight, as the pool itself would among only those entries.
	 *
	 * @param entries  the pool's elements with their weights
	 * @param template the element's template id, empty for elements that are not a single template
	 * @param nextInt  returns a random int in {@code [0, bound)}
	 * @return the picked element, or empty when the pool has no abandoned element
	 */
	public static <T> Optional<T> pickAbandoned(List<Pair<T, Integer>> entries, Function<T, Optional<Identifier>> template,
			IntUnaryOperator nextInt) {
		List<Pair<T, Integer>> abandoned = entries.stream()
				.filter(entry -> entry.getSecond() > 0 && template.apply(entry.getFirst()).filter(VillageLogic::isAbandoned).isPresent())
				.toList();
		int total = abandoned.stream().mapToInt(Pair::getSecond).sum();
		if (total == 0) return Optional.empty();

		int roll = nextInt.applyAsInt(total);
		for (Pair<T, Integer> entry : abandoned) {
			roll -= entry.getSecond();
			if (roll < 0) return Optional.of(entry.getFirst());
		}
		return Optional.of(abandoned.getLast().getFirst());
	}
}
