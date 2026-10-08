package io.github.hellenter.eam.mobs.vanilla.running;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Speeds the mob up while its melee attack goal chases a target (vanilla marks that with {@link Mob#isAggressive()}).
 * Takes no goal flags, so it runs next to the attack goal instead of replacing it.
 */
final class RunWhenChasingGoal extends Goal {
	private static final Identifier MODIFIER = Eam.id("running");

	private final Mob mob;

	RunWhenChasingGoal(Mob mob) {
		this.mob = mob;
		setFlags(EnumSet.noneOf(Flag.class));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return mob.isAggressive() && target != null && target.isAlive();
	}

	@Override
	public void start() {
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		speed.removeModifier(MODIFIER);
		double multiplier = RunningLogic.speedMultiplier(speed.getValue(), RunningModule.settings().chasePace());
		if (multiplier > 1.0) {
			speed.addTransientModifier(new AttributeModifier(MODIFIER, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	@Override
	public void stop() {
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(MODIFIER);
		}
	}
}
