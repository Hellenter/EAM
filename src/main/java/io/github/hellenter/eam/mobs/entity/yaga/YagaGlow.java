package io.github.hellenter.eam.mobs.entity.yaga;

/**
 * What glows on Baba Yaga, synced to clients: her whole body while she can be parried, or her staff's head in the colour
 * of what she casts.
 */
public enum YagaGlow {
	NONE(0),
	/** She can be parried: she lights up yellow all over. */
	PARRY(0xFFE36A),
	SUMMON(0x46F04A),
	ABSORPTION(0xFFC93A),
	SPEED(0x63E6FF),
	STRENGTH(0xFF3B2E),
	BLIND(0x9A4DFF),
	DRAIN(0x7BE03A),
	HARM(0xFF5A1A),
	ACID(0xA6FF2A),
	THREADS(0xC040FF),
	BEAM(0xFF8A1E);

	private static final YagaGlow[] VALUES = values();

	public final int color;

	YagaGlow(int color) {
		this.color = color;
	}

	public static YagaGlow byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
	}

	public static YagaGlow of(BabaYagaLogic.Buff buff) {
		return switch (buff) {
			case ABSORPTION -> ABSORPTION;
			case SPEED -> SPEED;
			case STRENGTH -> STRENGTH;
		};
	}

	public static YagaGlow of(BabaYagaLogic.Glyph glyph) {
		return switch (glyph) {
			case BLIND -> BLIND;
			case DRAIN -> DRAIN;
			case HARM -> HARM;
			case ACID -> ACID;
		};
	}
}
