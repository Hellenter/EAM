package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class VillagerNameLogicTest {
	@Test
	void surnamesTakeTheFeminineForm() {
		assertEquals("Kuznetsova", VillagerNameLogic.feminine("Kuznetsov"));
		assertEquals("Кузнецова", VillagerNameLogic.feminine("Кузнецов"));
		assertEquals("Lisitsyna", VillagerNameLogic.feminine("Lisitsyn"));
		assertEquals("Belozerskaya", VillagerNameLogic.feminine("Belozersky"));
		assertEquals("Белозерская", VillagerNameLogic.feminine("Белозерский"));
		assertEquals("Журавлёва", VillagerNameLogic.feminine("Журавлёв"));
	}

	@Test
	void idsAreUniqueAndBothSpellingsGiven() {
		for (List<VillagerNameLogic.Entry> list : List.of(VillagerNameLogic.MALE, VillagerNameLogic.FEMALE, VillagerNameLogic.SURNAMES)) {
			Set<String> ids = new HashSet<>();
			for (VillagerNameLogic.Entry entry : list) {
				assertTrue(ids.add(entry.id()), entry.id());
				assertTrue(entry.id().matches("[a-z_]+"), entry.id());
				assertTrue(!entry.english().isBlank() && !entry.russian().isBlank(), entry.id());
			}
		}
		Set<String> given = new HashSet<>();
		Stream.concat(VillagerNameLogic.MALE.stream(), VillagerNameLogic.FEMALE.stream()).forEach(entry -> assertTrue(given.add(entry.id())));
	}
}
