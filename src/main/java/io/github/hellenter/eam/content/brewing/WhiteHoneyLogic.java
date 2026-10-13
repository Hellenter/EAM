package io.github.hellenter.eam.content.brewing;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** What white honey does to its drinker: some effects cannot take hold, and withering hurts less. */
public final class WhiteHoneyLogic {
	/** Effects white honey keeps off: poison, slowness and weakness unless a datapack changes the tag. */
	public static final TagKey<MobEffect> WARDED = TagKey.create(Registries.MOB_EFFECT, Eam.id("white_honey_wards"));
	/** The share of wither damage that still gets through. */
	public static final float WITHER_FACTOR = 0.5F;

	private WhiteHoneyLogic() {
	}

	static boolean isWarded(Holder<MobEffect> effect) {
		return effect.is(WARDED);
	}

	/** Called from {@code LivingEntity.canBeAffected}: a drinker of white honey turns the warded effects away. */
	public static boolean canBeAffected(LivingEntity entity, MobEffectInstance effect, boolean vanilla) {
		return vanilla && !(isWarded(effect.getEffect()) && entity.hasEffect(BrewingModule.WHITE_HONEY_EFFECT));
	}

	/** Called from {@code LivingEntity.hurtServer}: wither damage on a drinker of white honey is cut. */
	public static float hurt(LivingEntity entity, DamageSource source, float amount) {
		if (source.is(DamageTypes.WITHER) && entity.hasEffect(BrewingModule.WHITE_HONEY_EFFECT)) {
			return witherDamage(amount);
		}
		return amount;
	}

	static float witherDamage(float amount) {
		return amount * WITHER_FACTOR;
	}
}
