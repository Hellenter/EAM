package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.Eam;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * {@code eam:convulsions}: Koschei's lightning makes the body jerk on its own. Walking is slowed to a stagger, every
 * other tick a jolt shoves the body a little to a random side, and on the client the view twitches.
 */
public class ConvulsionsEffect extends MobEffect {
	private static final int COLOR = 0x7FB8FF;
	/** Speed of a jolt, blocks per tick. */
	private static final double JOLT = 0.18;

	ConvulsionsEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, Eam.id("convulsions"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.JUMP_STRENGTH, Eam.id("convulsions"), -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		double angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
		mob.push(Math.cos(angle) * JOLT, mob.onGround() ? 0.05 : 0.0, Math.sin(angle) * JOLT);
		mob.syncVelocity = true;
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return tickCount % 2 == 0;
	}
}
