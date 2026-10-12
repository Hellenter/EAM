package io.github.hellenter.eam.mobs.entity.bat;

import io.github.hellenter.eam.mobs.entity.upyr.UpyrModule;
import io.github.hellenter.eam.survival.bleeding.BleedingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A blood-sucking bat of the underworld, a little bigger than a cave bat and tinted violet-blue. It never roosts:
 * it flutters about until it finds a player, then darts in, bites and pulls away before the next pass. A bite does
 * little damage but opens a bleeding wound ({@link BleedingModule}) and feeds the bat a little.
 */
public class VampireBat extends Bat implements Enemy {
	private static final TargetingConditions PREY = TargetingConditions.forCombat();
	/** How close its box has to come to the prey's box for a bite. */
	private static final double BITE_REACH = 0.3;

	private @Nullable BlockPos wanderTarget;
	private int attackCooldown;
	/** Ticks left of pulling away after a bite. */
	private int retreatTicks;
	private @Nullable Vec3 retreatTarget;

	public VampireBat(EntityType<? extends VampireBat> type, Level level) {
		super(type, level);
		xpReward = 3;
		if (!level.isClientSide()) {
			setResting(false);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Bat.createAttributes()
				.add(Attributes.MAX_HEALTH, 5.0)
				.add(Attributes.ATTACK_DAMAGE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		if (attackCooldown > 0) {
			attackCooldown--;
		}
		if (isResting()) {
			setResting(false);
		}

		LivingEntity prey = updatePrey(level);
		Vec3 aim;
		double speed = 0.5;
		double ease = 0.1;
		if (prey != null && retreatTicks > 0 && retreatTarget != null) {
			retreatTicks--;
			aim = retreatTarget;
		} else if (prey != null) {
			VampireBatSettings settings = VampireBatModule.settings();
			aim = prey.position().add(0.0, prey.getBbHeight() * 0.6, 0.0);
			speed = settings.chaseSpeed();
			ease = 0.15;
			if (attackCooldown == 0 && getBoundingBox().inflate(BITE_REACH).intersects(prey.getBoundingBox())) {
				bite(level, prey, settings);
			}
		} else {
			aim = Vec3.atBottomCenterOf(wander(level)).add(0.0, 0.1, 0.0);
		}
		fly(aim, speed, ease);
	}

	/** Keeps a live, reachable prey, or looks for the nearest player once a second. */
	private @Nullable LivingEntity updatePrey(ServerLevel level) {
		LivingEntity prey = getTarget();
		double range = getAttributeValue(Attributes.FOLLOW_RANGE);
		if (prey != null && (!prey.isAlive() || prey instanceof Player player && (player.isCreative() || player.isSpectator())
				|| distanceToSqr(prey) > range * range)) {
			setTarget(null);
			prey = null;
		}
		if (prey == null && (tickCount + getId()) % 20 == 0) {
			prey = level.getNearestPlayer(PREY.range(range), this);
			setTarget(prey);
		}
		return prey;
	}

	private void bite(ServerLevel level, LivingEntity prey, VampireBatSettings settings) {
		attackCooldown = settings.attackCooldown();
		if (doHurtTarget(level, prey)) {
			BleedingModule.bleed(prey, this, settings.bleedingTicks());
			heal(1.0F);
		}
		// Pull away up and back, then come round for another pass.
		Vec3 away = position().subtract(prey.position()).multiply(1.0, 0.0, 1.0);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5) : away;
		retreatTarget = position().add(away.normalize().scale(4.0 + random.nextDouble() * 3.0)).add(0.0, 1.5 + random.nextDouble() * 2.0, 0.0);
		retreatTicks = 10 + random.nextInt(15);
	}

	/** Vanilla bat wandering: a new random spot nearby now and then. */
	private BlockPos wander(ServerLevel level) {
		if (wanderTarget != null && (!level.isEmptyBlock(wanderTarget) || wanderTarget.getY() <= level.getMinY())) {
			wanderTarget = null;
		}
		if (wanderTarget == null || random.nextInt(30) == 0 || wanderTarget.closerToCenterThan(position(), 2.0)) {
			wanderTarget = BlockPos.containing(
					getX() + random.nextInt(7) - random.nextInt(7),
					getY() + random.nextInt(6) - 2.0,
					getZ() + random.nextInt(7) - random.nextInt(7));
		}
		return wanderTarget;
	}

	private void fly(Vec3 aim, double speed, double ease) {
		Vec3 movement = getDeltaMovement();
		Vec3 next = new Vec3(
				VampireBatLogic.steer(movement.x, aim.x - getX(), speed, ease),
				VampireBatLogic.steer(movement.y, aim.y - getY(), speed * 1.4, ease),
				VampireBatLogic.steer(movement.z, aim.z - getZ(), speed, ease));
		setDeltaMovement(next);
		float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90.0F;
		zza = 0.5F;
		setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// Garlic and black blood ward against its bite as against the upyr's.
		boolean hurt = UpyrModule.wardedAttack(this, level, target, () -> super.doHurtTarget(level, target));
		if (hurt) {
			playSound(SoundEvents.BAT_HURT, 0.6F, 1.4F + random.nextFloat() * 0.2F);
		}
		return hurt;
	}

	@Override
	protected float getSoundVolume() {
		return 0.35F;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.85F;
	}

	@Override
	public @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.BAT_AMBIENT;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return getTarget() == null;
	}
}
