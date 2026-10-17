package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * One of the phantom ravens Koschei calls up: a see-through black bird that flies through walls at his target, pecks it and
 * pulls away for another pass. A single hit kills it; it fades away when its time is up or he is gone. Never saved: one
 * found in a loaded chunk has lost him and fades at once.
 */
public class KoscheiRaven extends Monster {
	/** How close its box has to come to the target's box for a peck. */
	private static final double PECK_REACH = 0.25;
	private static final int PECK_COOLDOWN = 20;
	/** It eases toward the speed it wants by this share each tick. */
	private static final double EASE = 0.12;

	private @Nullable Koschei owner;
	private int lifeTicks;
	private int peckCooldown;
	/** Ticks left of pulling away after a peck, and where to. */
	private int retreatTicks;
	private @Nullable Vec3 retreatTarget;
	private double speed = KoscheiSettings.Ravens.DEFAULT.speed();

	public KoscheiRaven(EntityType<? extends KoscheiRaven> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		KoscheiSettings.Ravens ravens = KoscheiSettings.DEFAULT.ravens();
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, ravens.health())
				.add(Attributes.ATTACK_DAMAGE, ravens.damage())
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	/** Sets it on {@code target} for {@code owner}, with the numbers of {@code settings}. */
	void summon(Koschei owner, LivingEntity target, KoscheiSettings.Ravens settings) {
		this.owner = owner;
		setTarget(target);
		lifeTicks = settings.lifetime();
		speed = settings.speed();
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(settings.health());
		setHealth(settings.health());
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(settings.damage());
		// A short wait before the first dive, so the three do not strike as one.
		peckCooldown = random.nextInt(PECK_COOLDOWN);
	}

	@Override
	public void tick() {
		// A phantom: it passes through blocks.
		noPhysics = true;
		super.tick();
		noPhysics = false;
		setNoGravity(true);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (owner == null || !owner.isAlive() || --lifeTicks <= 0 || target == null || !target.isAlive()
				|| target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			vanish(level);
			return;
		}
		if (peckCooldown > 0) {
			peckCooldown--;
		}
		Vec3 aim;
		double want = speed;
		if (retreatTicks > 0 && retreatTarget != null) {
			retreatTicks--;
			aim = retreatTarget;
			want = speed * 0.8;
		} else {
			aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
			if (peckCooldown == 0 && getBoundingBox().inflate(PECK_REACH).intersects(target.getBoundingBox())) {
				peck(level, target);
			}
		}
		fly(aim, want);
	}

	private void peck(ServerLevel level, LivingEntity target) {
		peckCooldown = PECK_COOLDOWN;
		doHurtTarget(level, target);
		// Off up and away, then round for another pass.
		Vec3 away = position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5) : away;
		Vec3 side = new Vec3(-away.z, 0.0, away.x).normalize().scale((random.nextDouble() - 0.5) * 4.0);
		retreatTarget = position().add(away.normalize().scale(3.0 + random.nextDouble() * 2.0)).add(side)
				.add(0.0, 1.5 + random.nextDouble() * 1.5, 0.0);
		retreatTicks = 12 + random.nextInt(12);
	}

	/** Eases toward {@code want} blocks per tick at {@code aim}, a little unsteadily, and faces the way it flies. */
	private void fly(Vec3 aim, double want) {
		Vec3 to = aim.subtract(position());
		double length = to.length();
		Vec3 desired = length < 1.0E-4 ? Vec3.ZERO : to.scale(Math.min(want, length) / length);
		double wobble = Mth.sin((tickCount + getId() * 7) * 0.5F) * 0.04;
		Vec3 next = getDeltaMovement().add(desired.subtract(getDeltaMovement()).scale(EASE)).add(0.0, wobble, 0.0);
		setDeltaMovement(next);
		if (next.horizontalDistanceSqr() > 1.0E-4) {
			float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(yaw);
			yBodyRot = yaw;
			setYHeadRot(yaw);
		}
		setXRot((float) (-Mth.atan2(next.y, next.horizontalDistance()) * Mth.RAD_TO_DEG) * 0.6F);
	}

	@Override
	public void travel(Vec3 input) {
		// Steered in customServerAiStep: it only carries on as it goes, slowing a little.
		travelFlying(input, 0.0F);
	}

	/** Fades into a puff of dark motes and a few feathers. */
	private void vanish(ServerLevel level) {
		puff(level);
		discard();
	}

	private void puff(ServerLevel level) {
		level.sendParticles(EamParticles.KOSCHEI_WARP, getX(), getY(0.5), getZ(), 12, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(EamParticles.YAGA_FEATHER, getX(), getY(0.5), getZ(), 3, 0.2, 0.2, 0.2, 0.02);
	}

	@Override
	protected void tickDeath() {
		// No body left to fall: it bursts into motes at once.
		if (level() instanceof ServerLevel level) {
			puff(level);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return EamSounds.KOSCHEI_RAVEN_CAW;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 60;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return EamSounds.KOSCHEI_RAVEN_CAW;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.8F;
	}
}
