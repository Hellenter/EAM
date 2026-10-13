package io.github.hellenter.eam.content.brewing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WhiteHoneyLogicTest {
	@Test
	void witherHurtsHalfAsMuch() {
		assertEquals(0.5F, WhiteHoneyLogic.witherDamage(1.0F));
		assertEquals(4.0F, WhiteHoneyLogic.witherDamage(8.0F));
	}
}
