package io.github.hellenter.eam.mobs.entity.yaga;

/**
 * What Baba Yaga is busy with, synced to clients for her animations. Lengths are in ticks; those that depend on the data
 * file are worked out by {@link BabaYaga} itself.
 */
public enum YagaAction {
	NONE(0),
	/** Staff raised, glowing green: her creatures rise on {@link #SUMMON_RAISE}. */
	SUMMON(30),
	/** A flask thrown on {@link #THROW_RELEASE}. */
	THROW(24),
	/** A spell sent from her staff on {@link #SPELL_RELEASE}. */
	SPELL(20),
	/** A small glyph over her head from the start; the real one appears under the player on {@link #GLYPH_PLACE}. */
	GLYPH(20),
	/** Phase one: the dive with her staff; she can be parried just before it strikes. */
	SWOOP(0),
	/** Phase one: back up after the dive. */
	RETREAT(0),
	/** Knocked back by a parry. */
	STAGGER(20),
	/** Her mortar bursts and she drops to the ground: phase two begins. Untouchable meanwhile. */
	SHATTER(40),
	/** Phase two: an acid bolt from her staff on {@link #BOLT_RELEASE}. */
	ACID(18),
	/** Phase two: a purple bolt from her staff on {@link #BOLT_RELEASE}. */
	THREADS(20),
	/** Phase two: aims, then a fire beam from her staff. */
	BEAM(0),
	/** Phase two: blinks behind the player and rakes them with her claws. */
	CLAWS(0),
	/** Phase two: runs to one of her creatures to eat it. */
	EAT_RUN(60),
	/** Phase two: eats it. */
	EAT(0),
	/** Phase two: runs off into the dark after the player hit her, to blink back behind them. */
	ESCAPE(0);

	public static final int SUMMON_RAISE = 20;
	public static final int THROW_RELEASE = 14;
	public static final int SPELL_RELEASE = 10;
	public static final int GLYPH_PLACE = 10;
	public static final int BOLT_RELEASE = 12;
	/** How long the mini glyph stays over her head. */
	public static final int GLYPH_PREVIEW = 30;

	private static final YagaAction[] VALUES = values();

	private final int length;

	YagaAction(int length) {
		this.length = length;
	}

	/** Length in ticks, or 0 when {@link BabaYaga} works it out from the data file. */
	public int length() {
		return length;
	}

	public static YagaAction byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
	}
}
