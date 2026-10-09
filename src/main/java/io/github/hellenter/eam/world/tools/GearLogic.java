package io.github.hellenter.eam.world.tools;

import java.util.Map;
import java.util.Optional;

/**
 * Tool durability and armor numbers. Pure logic: {@link GearModule} applies them to the default item components.
 */
public final class GearLogic {
	/** Armor points left after the cut: a full iron set goes from 15 to 12. */
	public static final double ARMOR_FACTOR = 0.8;

	/** Copper armor wears out twice as fast as in vanilla; so does silver armor, which copies copper. */
	public static final int COPPER_ARMOR_DURABILITY_DIVISOR = 2;

	private static final Map<String, Integer> TOOL_DURABILITY = Map.ofEntries(
			Map.entry("copper_pickaxe", 24),
			Map.entry("copper_axe", 36),
			Map.entry("copper_shovel", 48),
			Map.entry("copper_sword", 120),
			Map.entry("copper_hoe", 48),
			// Silver tools last as long as copper ones.
			Map.entry("silver_pickaxe", 24),
			Map.entry("silver_axe", 36),
			Map.entry("silver_shovel", 48),
			Map.entry("silver_sword", 120),
			Map.entry("silver_hoe", 48),
			Map.entry("iron_pickaxe", 120),
			Map.entry("iron_axe", 120),
			Map.entry("iron_shovel", 180),
			Map.entry("iron_sword", 250),
			Map.entry("iron_hoe", 180),
			// Rusty iron tools last three quarters as long as iron ones.
			Map.entry("rusty_iron_pickaxe", 90),
			Map.entry("rusty_iron_axe", 90),
			Map.entry("rusty_iron_shovel", 135),
			Map.entry("rusty_iron_sword", 188),
			Map.entry("rusty_iron_hoe", 135),
			// Ancient metal tools last twice as long as iron ones, as in MITE.
			Map.entry("ancient_metal_pickaxe", 240),
			Map.entry("ancient_metal_axe", 240),
			Map.entry("ancient_metal_shovel", 360),
			Map.entry("ancient_metal_sword", 500),
			Map.entry("ancient_metal_hoe", 360)
	);

	private GearLogic() {
	}

	/** New max damage for a tool, by its item id path; empty keeps the vanilla value. */
	public static Optional<Integer> toolDurability(String path) {
		return Optional.ofNullable(TOOL_DURABILITY.get(path));
	}

	public static int copperArmorDurability(int vanillaDurability) {
		return Math.max(1, vanillaDurability / COPPER_ARMOR_DURABILITY_DIVISOR);
	}

	/**
	 * Scales the armor points of one set (helmet, chestplate, leggings, boots, in that order) by {@code factor} so that
	 * the set total is the rounded scaled total. Each piece gets its floor, then the pieces with the largest remainders
	 * get one point more; ties go to the earlier piece in the array. A piece that had armor keeps at least one point.
	 */
	public static int[] scaleSet(int[] vanilla, double factor) {
		int[] scaled = new int[vanilla.length];
		double[] remainder = new double[vanilla.length];
		int total = 0;
		int sum = 0;
		for (int i = 0; i < vanilla.length; i++) {
			double exact = vanilla[i] * factor;
			scaled[i] = (int) Math.floor(exact + 1e-9);
			remainder[i] = exact - scaled[i];
			total += vanilla[i];
			sum += scaled[i];
		}

		int target = (int) Math.round(total * factor);
		while (sum < target) {
			int best = -1;
			for (int i = 0; i < vanilla.length; i++) {
				if (remainder[i] > 1e-9 && (best < 0 || remainder[i] > remainder[best] + 1e-9)) {
					best = i;
				}
			}
			if (best < 0) {
				break;
			}
			scaled[best]++;
			remainder[best] = 0;
			sum++;
		}

		for (int i = 0; i < vanilla.length; i++) {
			if (vanilla[i] > 0 && scaled[i] == 0) {
				scaled[i] = 1;
			}
		}
		return scaled;
	}
}
