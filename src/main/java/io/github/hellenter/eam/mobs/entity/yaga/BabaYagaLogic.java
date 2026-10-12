package io.github.hellenter.eam.mobs.entity.yaga;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/** Baba Yaga's decisions, free of the game so they can be unit tested. */
public final class BabaYagaLogic {
	private BabaYagaLogic() {
	}

	/** The creatures she raises. */
	public enum Creature { LURKER, WIGHT, SOLDIER }

	/** The two kinds of phase-one group: swamp lurkers or wights, each with drowned soldiers. */
	public enum GroupKind { LURKERS, WIGHTS }

	/** The spells she casts on a group of her creatures. */
	public enum Buff { ABSORPTION, SPEED, STRENGTH }

	/** Her glyphs on the ground, and the acid puddle that works the same way. */
	public enum Glyph { BLIND, DRAIN, HARM, ACID }

	/** Her attacks on foot. */
	public enum Attack { ACID, THREADS, BEAM, CLAWS }

	/**
	 * The spell for a group: absorption if the player hurt it lately, speed if it is still far from the player, strength
	 * once it is close.
	 */
	public static Buff buffFor(double distanceToPlayer, boolean hurtLately, BabaYagaSettings.Spell spell) {
		if (hurtLately) {
			return Buff.ABSORPTION;
		}
		if (distanceToPlayer >= spell.farDistance()) {
			return Buff.SPEED;
		}
		return Buff.STRENGTH;
	}

	/**
	 * Which group to cast on: the one the player hurt, else the one nearest the player; groups that already carry the
	 * spell they would get are skipped. Returns the group's index, or -1 when none is worth a spell.
	 *
	 * @param alive    creatures alive in each group
	 * @param distance each group's distance to the player
	 * @param hurt     whether the player hurt each group lately
	 * @param has      whether each group already carries the spell it would get now
	 */
	public static int groupToBuff(int[] alive, double[] distance, boolean[] hurt, boolean[] has) {
		int best = -1;
		for (int i = 0; i < alive.length; i++) {
			if (alive[i] <= 0 || has[i]) {
				continue;
			}
			if (best < 0 || hurt[i] && !hurt[best] || hurt[i] == hurt[best] && distance[i] < distance[best]) {
				best = i;
			}
		}
		return best;
	}

	/** One creature for a group, by the weights; a wight only while the group has fewer than {@code maxWights}. */
	public static Creature creature(int lurkerWeight, int wightWeight, int soldierWeight, int wightsSoFar, int maxWights,
			DoubleSupplier random) {
		int wights = wightsSoFar < maxWights ? wightWeight : 0;
		int total = lurkerWeight + wights + soldierWeight;
		if (total <= 0) {
			return Creature.SOLDIER;
		}
		double roll = random.getAsDouble() * total;
		if (roll < lurkerWeight) {
			return Creature.LURKER;
		}
		if (roll < lurkerWeight + wights) {
			return Creature.WIGHT;
		}
		return Creature.SOLDIER;
	}

	/** Kind of a wave's group number {@code index}: the kinds take turns, starting with {@code first}. */
	public static GroupKind waveGroupKind(int index, GroupKind first) {
		GroupKind[] kinds = GroupKind.values();
		return kinds[(first.ordinal() + index) % kinds.length];
	}

	/** The creatures of a phase-one group of this kind: its lurkers or wights first, then the drowned soldiers. */
	public static List<Creature> waveGroup(GroupKind kind, BabaYagaSettings.Summon summon, DoubleSupplier random) {
		BabaYagaSettings.Summon.Mix mix = kind == GroupKind.LURKERS ? summon.lurkerGroup() : summon.wightGroup();
		Creature own = kind == GroupKind.LURKERS ? Creature.LURKER : Creature.WIGHT;
		int owns = groupSize(mix.min(), mix.max(), random);
		int soldiers = groupSize(mix.soldiersMin(), mix.soldiersMax(), random);
		List<Creature> creatures = new ArrayList<>(owns + soldiers);
		for (int i = 0; i < owns; i++) {
			creatures.add(own);
		}
		for (int i = 0; i < soldiers; i++) {
			creatures.add(Creature.SOLDIER);
		}
		return creatures;
	}

	/**
	 * A wave group split into the half that rises at once and the half that rises later, picked at random. With an odd
	 * count the first half gets the extra creature.
	 */
	public static <T> List<List<T>> halves(List<T> creatures, DoubleSupplier random) {
		List<T> shuffled = new ArrayList<>(creatures);
		for (int i = shuffled.size() - 1; i > 0; i--) {
			int j = Math.min(i, (int) Math.floor(random.getAsDouble() * (i + 1)));
			T swap = shuffled.get(i);
			shuffled.set(i, shuffled.get(j));
			shuffled.set(j, swap);
		}
		int first = (shuffled.size() + 1) / 2;
		return List.of(List.copyOf(shuffled.subList(0, first)), List.copyOf(shuffled.subList(first, shuffled.size())));
	}

	/** How many creatures go in a group, from {@code min} to {@code max}. */
	public static int groupSize(int min, int max, DoubleSupplier random) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return low + (int) Math.floor(random.getAsDouble() * (high - low + 1 - 1.0E-9));
	}

	/**
	 * Whether a player's hit on tick {@code t} of an attack parries it: the window is the {@code window} ticks before
	 * {@code strike}, the tick the blow lands.
	 */
	public static boolean parries(int t, int strike, int window) {
		return t >= strike - window && t < strike;
	}

	/**
	 * Her speed along a dive or lunge, timed to arrive on the strike tick: the distance left over the ticks left, at
	 * most {@code max}.
	 */
	public static double closingSpeed(double distance, int ticksLeft, double max) {
		return Math.min(max, Math.max(0.0, distance) / Math.max(1, ticksLeft));
	}

	/**
	 * Where she blinks to behind a player: a flat offset {x, z} from them, {@code distance} away and {@code turn} degrees
	 * off straight behind their back. {@code yaw} is the player's facing in Minecraft degrees (0 is south, +z).
	 */
	public static double[] behind(float yaw, float turn, double distance) {
		double angle = Math.toRadians(yaw + 180.0F + turn);
		// Minecraft's facing: x = -sin(yaw), z = cos(yaw).
		return new double[] {-Math.sin(angle) * distance, Math.cos(angle) * distance};
	}

	/** Whether {@code hits} hits in the last {@code window} ticks make her escape. */
	public static boolean escapes(int hitsInWindow, BabaYagaSettings.Escape escape, int cooldown) {
		return cooldown <= 0 && hitsInWindow >= escape.hits();
	}

	/** Whether she goes to eat one of her creatures. */
	public static boolean eats(float health, int cooldown, boolean creatureNear, BabaYagaSettings.Eat eat) {
		return cooldown <= 0 && creatureNear && health < eat.below();
	}

	/** Damage of one tick of the fire beam, so its whole length deals {@code beam.damage()}. */
	public static float beamTickDamage(BabaYagaSettings.Beam beam, int every) {
		return beam.damage() * every / beam.ticks();
	}

	/**
	 * Her next attack on foot, picked among those ready by weight, or null when none is. Bolts and the beam need a
	 * player in range and in sight; the claws work from anywhere but point-blank.
	 */
	public static Attack chooseAttack(double distance, boolean sees, boolean targetTied, int[] cooldowns, BabaYagaSettings settings,
			DoubleSupplier random) {
		double[] weights = new double[Attack.values().length];
		weights[Attack.ACID.ordinal()] = sees && distance <= settings.acid().range() ? 3.0 : 0.0;
		weights[Attack.THREADS.ordinal()] = sees && !targetTied && distance <= settings.threads().range() ? 1.5 : 0.0;
		weights[Attack.BEAM.ordinal()] = sees && distance <= settings.beam().range() ? 2.0 : 0.0;
		weights[Attack.CLAWS.ordinal()] = distance >= 4.0 ? 1.5 : 0.0;
		double total = 0.0;
		for (Attack attack : Attack.values()) {
			if (cooldowns[attack.ordinal()] > 0) {
				weights[attack.ordinal()] = 0.0;
			}
			total += weights[attack.ordinal()];
		}
		if (total <= 0.0) {
			return null;
		}
		double roll = random.getAsDouble() * total;
		for (Attack attack : Attack.values()) {
			roll -= weights[attack.ordinal()];
			if (roll < 0.0 && weights[attack.ordinal()] > 0.0) {
				return attack;
			}
		}
		for (int i = Attack.values().length - 1; i >= 0; i--) {
			if (weights[i] > 0.0) {
				return Attack.values()[i];
			}
		}
		return null;
	}

	/** Height of the spell's arc over the straight line, at {@code s} from 0 (staff) to 1 (group). */
	public static double arc(double s, double height) {
		return 4.0 * height * s * (1.0 - s);
	}
}
