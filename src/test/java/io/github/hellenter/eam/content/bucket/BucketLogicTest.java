package io.github.hellenter.eam.content.bucket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BucketLogicTest {
	@Test
	void weakerMetalsMeltMoreOftenAsInMite() {
		assertEquals(0.16F, BucketLogic.miteMeltChance(4.0F), 1e-6F);
		assertEquals(0.08F, BucketLogic.miteMeltChance(8.0F), 1e-6F);
		assertEquals(0.01F, BucketLogic.miteMeltChance(64.0F), 1e-6F);
	}

	@Test
	void meltsWhenTheRollIsBelowTheChance() {
		assertTrue(BucketLogic.melts(0.16F, 0.1F));
		assertFalse(BucketLogic.melts(0.16F, 0.16F));
		assertFalse(BucketLogic.melts(0.0F, 0.0F));
	}

	@Test
	void percentHasAtMostOneDecimal() {
		assertEquals("16", BucketLogic.percent(0.16F));
		assertEquals("8", BucketLogic.percent(0.08F));
		assertEquals("1", BucketLogic.percent(0.01F));
		assertEquals("0.5", BucketLogic.percent(0.005F));
		assertEquals("100", BucketLogic.percent(1.0F));
	}
}
