package io.github.hellenter.eam.world.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class VillageLogicTest {
	private static final String FOUNTAIN = "village/plains/town_centers/plains_fountain_01";
	private static final String ZOMBIE_FOUNTAIN = "village/plains/zombie/town_centers/plains_fountain_01";
	private static final String ZOMBIE_MEETING = "village/plains/zombie/town_centers/plains_meeting_point_1";

	private static Optional<Identifier> id(String path) {
		return Optional.of(Identifier.withDefaultNamespace(path));
	}

	@Test
	void zombieTemplatesAreAbandoned() {
		assertTrue(VillageLogic.isAbandoned(Identifier.withDefaultNamespace(ZOMBIE_FOUNTAIN)));
		assertFalse(VillageLogic.isAbandoned(Identifier.withDefaultNamespace(FOUNTAIN)));
	}

	@Test
	void picksOnlyAbandonedByWeight() {
		List<Pair<String, Integer>> pool = List.of(Pair.of(FOUNTAIN, 50), Pair.of(ZOMBIE_FOUNTAIN, 1), Pair.of(ZOMBIE_MEETING, 3));
		assertEquals(Optional.of(ZOMBIE_FOUNTAIN), VillageLogic.pickAbandoned(pool, VillageLogicTest::id, bound -> 0));
		assertEquals(Optional.of(ZOMBIE_MEETING), VillageLogic.pickAbandoned(pool, VillageLogicTest::id, bound -> 1));
		assertEquals(Optional.of(ZOMBIE_MEETING), VillageLogic.pickAbandoned(pool, VillageLogicTest::id, bound -> bound - 1));
	}

	@Test
	void poolWithoutAbandonedEntriesGivesNothing() {
		List<Pair<String, Integer>> pool = List.of(Pair.of(FOUNTAIN, 50), Pair.of("other", 2));
		assertEquals(Optional.empty(), VillageLogic.pickAbandoned(pool, path -> path.equals("other") ? Optional.empty() : id(path), bound -> 0));
	}
}
