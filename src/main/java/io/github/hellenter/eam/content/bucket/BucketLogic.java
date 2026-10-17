package io.github.hellenter.eam.content.bucket;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure rules for buckets: melting in lava, and how often an animal gives milk. */
public final class BucketLogic {
	private BucketLogic() {
	}

	/** Whether an animal last milked at {@code lastMilked} (null: never) gives milk at {@code now}. */
	public static boolean givesMilk(@Nullable Long lastMilked, long now, int interval) {
		return lastMilked == null || now - lastMilked >= interval || now < lastMilked;
	}

	/**
	 * MITE's chance for a bucket to melt when it scoops lava: 1% for mithril, growing as the metal gets weaker
	 * (iron 8%, copper and silver 16%), except gold, which always has 20%.
	 *
	 * @param durability the metal's durability in MITE's units: copper, silver and gold 4, iron 8, mithril 64
	 */
	public static float miteMeltChance(float durability) {
		return 0.01F * 64.0F / durability;
	}

	/** Whether a bucket with {@code chance} melts on a uniform {@code roll} in [0, 1). */
	public static boolean melts(float chance, float roll) {
		return roll < chance;
	}

	/** {@code chance} as a percentage for the tooltip, to one decimal place without a trailing zero: 0.16 is "16", 0.005 is "0.5". */
	public static String percent(float chance) {
		return BigDecimal.valueOf(chance * 100.0F).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
