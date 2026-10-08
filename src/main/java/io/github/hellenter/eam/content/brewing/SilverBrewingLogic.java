package io.github.hellenter.eam.content.brewing;

/** Numbers for the silver brewing stand and its light potions. Pure, unit tested. */
public final class SilverBrewingLogic {
	/** Brews one lapis-gunpowder mix fuels. */
	public static final int MIX_FUEL_USES = 3;

	/** Ticks one brew takes, as in the vanilla stand. */
	public static final int BREW_TICKS = 400;

	/** Vanilla Instant Health I heals 4 HP and hurts undead for 6. */
	private static final int VANILLA_HEAL = 4;
	private static final int VANILLA_HARM = 6;

	private SilverBrewingLogic() {
	}

	/** A light potion lasts half as long as its vanilla level I version. */
	public static int lightDuration(int vanillaTicks) {
		return vanillaTicks / 2;
	}

	/** Health a light healing potion restores: half of vanilla Instant Health at the same level. */
	public static int lightHeal(int amplifier) {
		return (VANILLA_HEAL << Math.max(amplifier, 0)) / 2;
	}

	/** Damage a light healing potion does to undead, half of vanilla. */
	public static int lightHarm(int amplifier) {
		return (VANILLA_HARM << Math.max(amplifier, 0)) / 2;
	}

	/** Splash potions heal less the farther the target is from the impact. */
	public static int scaled(int amount, double scale) {
		return (int) (scale * amount + 0.5);
	}
}
