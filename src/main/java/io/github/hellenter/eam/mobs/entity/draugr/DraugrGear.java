package io.github.hellenter.eam.mobs.entity.draugr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * What is left of a draugr's armor. Before they died the draugr were warriors of many peoples, so each wears pieces from
 * one of six factions, often mixed with a stray piece taken from another: one to four of head, chest, shoulders, arms and
 * legs, and now and then a belt. The pieces are drawn by the model (one variant per slot, 0 for none) and packed into one
 * int, three bits a slot; each worn piece adds a little armor.
 */
public final class DraugrGear {
	/** The slots, in the order the model lists their pieces. */
	public enum Slot {
		HEAD(6, 2), CHEST(6, 3), SHOULDERS(5, 1), ARMS(4, 1), LEGS(5, 2), BELT(4, 0);

		private final int variants;
		private final int armor;

		Slot(int variants, int armor) {
			this.variants = variants;
			this.armor = armor;
		}

		public int variants() {
			return variants;
		}

		int shift() {
			return ordinal() * 3;
		}
	}

	/** The five armor slots; the belt comes on top of them. */
	static final List<Slot> ARMOR_SLOTS = List.of(Slot.HEAD, Slot.CHEST, Slot.SHOULDERS, Slot.ARMS, Slot.LEGS);

	/**
	 * The factions and their own piece in each slot (variant number, 0 where they have none), in {@link Slot} order:
	 * Norse raiders (horned helm, studded jerkin, a single round pauldron, leather wraps, a skirt of strips, a belt with
	 * a round boss), huscarls in mail, eastern riders in lamellar, knights in plate, mercenaries in kettle hats and
	 * brigandines, and the hide-clad clans with antlered skulls.
	 */
	public enum Faction {
		NORSE(1, 4, 1, 1, 1, 1),
		HUSCARL(2, 1, 5, 3, 4, 0),
		EASTERN(3, 2, 3, 0, 3, 3),
		KNIGHT(4, 3, 2, 2, 2, 0),
		MERCENARY(5, 6, 0, 0, 5, 4),
		CLAN(6, 5, 4, 4, 0, 2);

		private final int[] pieces;

		Faction(int... pieces) {
			this.pieces = pieces;
		}

		public int piece(Slot slot) {
			return pieces[slot.ordinal()];
		}
	}

	private DraugrGear() {
	}

	/** The variant worn in {@code slot}, 0 for none. */
	public static int piece(int gear, Slot slot) {
		return (gear >> slot.shift()) & 7;
	}

	static int with(int gear, Slot slot, int variant) {
		return (gear & ~(7 << slot.shift())) | ((variant & 7) << slot.shift());
	}

	/** How many armor pieces (the belt aside) are worn. */
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
	 * A random outfit: a faction, then {@code pieceWeights}-weighted one to four armor slots, each with the faction's own
	 * piece or, now and then (and where the faction has none), a piece of another; then maybe a belt.
	 *
	 * @param pieceWeights   weights of wearing 1, 2, 3, 4 armor pieces
	 * @param ownPieceChance chance a slot takes the faction's own piece
	 * @param beltChance     chance of a belt
	 */
	public static int choose(DoubleSupplier random, double[] pieceWeights, double ownPieceChance, double beltChance) {
		Faction faction = Faction.values()[index(random, Faction.values().length)];
		int count = 1 + weighted(random, pieceWeights);
		List<Slot> slots = new ArrayList<>(ARMOR_SLOTS);
		int gear = 0;
		for (int n = 0; n < count && !slots.isEmpty(); n++) {
			Slot slot = slots.remove(index(random, slots.size()));
			gear = with(gear, slot, pick(random, faction, slot, ownPieceChance));
		}
		if (random.getAsDouble() < beltChance) {
			gear = with(gear, Slot.BELT, pick(random, faction, Slot.BELT, ownPieceChance));
		}
		return gear;
	}

	private static int pick(DoubleSupplier random, Faction faction, Slot slot, double ownPieceChance) {
		int own = faction.piece(slot);
		boolean ownRoll = random.getAsDouble() < ownPieceChance;
		return own != 0 && ownRoll ? own : 1 + index(random, slot.variants);
	}

	private static int index(DoubleSupplier random, int size) {
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
