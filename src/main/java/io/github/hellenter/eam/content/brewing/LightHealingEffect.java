package io.github.hellenter.eam.content.brewing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.jspecify.annotations.Nullable;

/** Instant Health at half strength: heals 2 HP at level I and hurts undead instead, like the vanilla effect. */
public class LightHealingEffect extends InstantaneousMobEffect {
	public LightHealingEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		if (mob.isInvertedHealAndHarm()) {
			mob.hurtServer(level, mob.damageSources().magic(), SilverBrewingLogic.lightHarm(amplification));
		} else {
			mob.heal(SilverBrewingLogic.lightHeal(amplification));
		}
		return true;
	}

	@Override
	public void applyInstantaneousEffect(ServerLevel level, @Nullable Entity source, @Nullable Entity owner, LivingEntity mob,
			int amplification, double scale) {
		if (mob.isInvertedHealAndHarm()) {
			int amount = SilverBrewingLogic.scaled(SilverBrewingLogic.lightHarm(amplification), scale);
			mob.hurtServer(level, source == null ? mob.damageSources().magic() : mob.damageSources().indirectMagic(source, owner), amount);
		} else {
			mob.heal(SilverBrewingLogic.scaled(SilverBrewingLogic.lightHeal(amplification), scale));
		}
	}
}
