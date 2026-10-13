package io.github.hellenter.eam.mobs.entity.duergar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * What a duergar wears, and his beard and hair. Every duergar shares one model with every piece in it; which piece of
 * each slot is shown (one variant per slot, 0 for none) is packed into one int, three bits a slot. Each worn armor
 * piece adds a little armor.
 */
public final class DuergarGear {
	/** The slots, in the order the model lists their pieces. */
	public enum Slot {
		// Helms: dented cap, spiked helm, miner's helm with a lamp, mail coif, great helm, horned helm; then a cloth hood.
		HEAD(7, 6, 2),
		// Torn mail, holed breastplate, miner's jerkin, scale shirt, heavy cuirass; then a smith's apron and an elder's robe.
		CHEST(7, 5, 3),
		// One pauldron, spiked pauldrons, mail mantle, leather shoulders.
		SHOULDERS(4, 4, 1),
		// Bracers, mail sleeves, wraps, gauntlets.
		ARMS(4, 4, 1),
		// Mail skirt, greaves, boots, tassets.
		LEGS(4, 4, 2),
		// Tool belt, rope, buckled belt, chain.
		BELT(4, 4, 0),
		// Long, braided, short, bushy.
		BEARD(4, 4, 0),
		// Topknot, fringe, crest; 0 is bald.
		HAIR(3, 3, 0);

		private final int variants;
		private final int armorVariants;
		private final int armor;

		Slot(int variants, int armorVariants, int armor) {
			this.variants = variants;
			this.armorVariants = armorVariants;
			this.armor = armor;
		}

		public int variants() {
			return variants;
		}

		/** The first variants are armor and clothing a fighter may wear; the ones after are home clothes. */
		public int armorVariants() {
			return armorVariants;
		}

		int shift() {
			return ordinal() * 3;
		}
	}

	/** The five armor slots. */
	static final List<Slot> ARMOR_SLOTS = List.of(Slot.HEAD, Slot.CHEST, Slot.SHOULDERS, Slot.ARMS, Slot.LEGS);

	private DuergarGear() {
	}

	/** The variant worn in {@code slot}, 0 for none. */
	public static int piece(int gear, Slot slot) {
		return (gear >> slot.shift()) & 7;
	}

	static int with(int gear, Slot slot, int variant) {
		return (gear & ~(7 << slot.shift())) | ((variant & 7) << slot.shift());
	}

	/** How many armor pieces are worn. */
	public static int armorPieces(int gear) {
		int count = 0;
		for (Slot slot : ARMOR_SLOTS) {
			if (piece(gear, slot) != 0) {
				count++;
			}
		}
		return count;
	}

	/** Armor points from what is worn. */
	public static int armor(int gear) {
		int armor = 0;
		for (Slot slot : Slot.values()) {
			if (piece(gear, slot) != 0) {
				armor += slot.armor;
			}
		}
		return armor;
	}

	/**
	 * A zombie duergar's outfit, whatever he died in: {@code pieceWeights}-weighted none to four armor slots, each a random
	 * piece, maybe a belt, always a beard and, unless {@code baldChance} comes up, some hair.
	 *
	 * @param pieceWeights weights of wearing 0, 1, 2, 3, 4 armor pieces
	 */
	public static int scavenged(DoubleSupplier random, double[] pieceWeights, double beltChance, double baldChance) {
		int count = weighted(random, pieceWeights);
		List<Slot> slots = new ArrayList<>(ARMOR_SLOTS);
		int gear = 0;
		for (int n = 0; n < count && !slots.isEmpty(); n++) {
			Slot slot = slots.remove(index(random, slots.size()));
			gear = with(gear, slot, 1 + index(random, slot.armorVariants));
		}
		if (random.getAsDouble() < beltChance) {
			gear = with(gear, Slot.BELT, 1 + index(random, Slot.BELT.variants));
		}
		return withLooks(gear, random, baldChance);
	}

	/** Renegade pieces per armor slot, in {@link #ARMOR_SLOTS} order: the heavy, whole ones. */
	private static final int[][] RENEGADE_PIECES = {{2, 5, 6}, {4, 5}, {1, 2, 3}, {1, 2, 4}, {2, 3, 4}};
	private static final int[] RENEGADE_BELTS = {1, 3, 4};
	/** The leader's own outfit: horned helm, heavy cuirass, spiked pauldrons, gauntlets, boots and a chain belt. */
	private static final int[] LEADER_PIECES = {6, 5, 2, 4, 3};
	private static final int LEADER_BELT = 4;

	/**
	 * A renegade's outfit: armor in every slot, heavy and whole, and a belt. The leader always wears the same set, so he
	 * stands out from his squad.
	 */
	public static int armored(DoubleSupplier random, boolean leader, double baldChance) {
		int gear = 0;
		for (int i = 0; i < ARMOR_SLOTS.size(); i++) {
			int[] pool = RENEGADE_PIECES[i];
			gear = with(gear, ARMOR_SLOTS.get(i), leader ? LEADER_PIECES[i] : pool[index(random, pool.length)]);
		}
		gear = with(gear, Slot.BELT, leader ? LEADER_BELT : RENEGADE_BELTS[index(random, RENEGADE_BELTS.length)]);
		return withLooks(gear, random, baldChance);
	}

	static final int MINER_HELM = 3;
	static final int HOOD = 7;
	static final int MINER_JERKIN = 3;
	static final int APRON = 6;
	static final int ROBE = 7;
	static final int WRAPS = 3;
	static final int BRACERS = 1;
	static final int LEATHER_SHOULDERS = 4;
	static final int TOOL_BELT = 1;
	static final int ROPE_BELT = 2;
	static final int BUCKLE_BELT = 3;
	static final int LONG_BEARD = 1;
	static final int BRAIDED_BEARD = 2;

	/**
	 * A village duergar's clothes, by his trade: the smith wears a leather apron, bracers or wraps and a tool belt; the
	 * miner his helm with a lamp and a jerkin; the elder a hooded robe and always a long or braided beard. Who wears
	 * no armor counts no armor: {@link #armor} is only for those who fight in it.
	 */
	public static int homespun(DoubleSupplier random, DuergarVillager.Role role) {
		int gear = switch (role) {
			case SMITH -> {
				int g = with(0, Slot.CHEST, APRON);
				g = with(g, Slot.ARMS, random.getAsDouble() < 0.5 ? BRACERS : WRAPS);
				yield with(g, Slot.BELT, TOOL_BELT);
			}
			case MINER -> {
				int g = with(0, Slot.HEAD, random.getAsDouble() < 0.7 ? MINER_HELM : 0);
				g = with(g, Slot.CHEST, MINER_JERKIN);
				g = with(g, Slot.SHOULDERS, random.getAsDouble() < 0.4 ? LEATHER_SHOULDERS : 0);
				g = with(g, Slot.ARMS, random.getAsDouble() < 0.5 ? WRAPS : 0);
				yield with(g, Slot.BELT, random.getAsDouble() < 0.5 ? TOOL_BELT : ROPE_BELT);
			}
			case ELDER -> with(with(with(0, Slot.HEAD, HOOD), Slot.CHEST, ROBE), Slot.BELT, BUCKLE_BELT);
		};
		gear = withLooks(gear, random, 0.5);
		if (role == DuergarVillager.Role.ELDER) {
			// Under the hood: no hair shows.
			gear = with(with(gear, Slot.BEARD, random.getAsDouble() < 0.5 ? LONG_BEARD : BRAIDED_BEARD), Slot.HAIR, 0);
		}
		return gear;
	}

	/** A beard always, and hair unless {@code baldChance} comes up. */
	static int withLooks(int gear, DoubleSupplier random, double baldChance) {
		gear = with(gear, Slot.BEARD, 1 + index(random, Slot.BEARD.variants));
		if (random.getAsDouble() >= baldChance) {
			gear = with(gear, Slot.HAIR, 1 + index(random, Slot.HAIR.variants));
		}
		return gear;
	}

	static int index(DoubleSupplier random, int size) {
		return Math.min(size - 1, (int) (random.getAsDouble() * size));
	}

	static int weighted(DoubleSupplier random, double[] weights) {
		double total = 0.0;
		for (double weight : weights) {
			total += Math.max(0.0, weight);
		}
		if (total <= 0.0) {
			return 0;
		}
		double roll = random.getAsDouble() * total;
		for (int i = 0; i < weights.length; i++) {
			roll -= Math.max(0.0, weights[i]);
			if (roll < 0.0) {
				return i;
			}
		}
		return weights.length - 1;
	}
}
