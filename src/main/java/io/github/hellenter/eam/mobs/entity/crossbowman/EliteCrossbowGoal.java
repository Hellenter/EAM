package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic.Attack;
import io.github.hellenter.eam.mobs.entity.lost.LostLogic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * The elite crossbowman's fight: he keeps his target between his minimum and maximum range, loads one of his shots,
 * then stops, turns to the target, holds his aim (the charged shot glows red all that time) and shoots: one bolt, a
 * burst of three, a red bolt or a net.
 */
final class EliteCrossbowGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	private static final double RETREAT_SPEED = 1.15;
	/** How fast he turns to the target while aiming, degrees per tick. */
	private static final float AIM_TURN = 40.0F;
	/** He shoots only once he faces the target this closely, in degrees. */
	private static final float AIM_TOLERANCE = 12.0F;

	private enum Phase {
		IDLE,
		LOADING,
		AIMING,
		FIRING
	}

	private final EliteCrossbowman mob;
	private final int[] cooldowns = new int[Attack.values().length];
	private Phase phase = Phase.IDLE;
	private @Nullable Attack attack;
	private int phaseTicks;
	private int pause;
	private int shotsLeft;
	private int repath;
	private int strafeTicks;
	private boolean strafeRight;

	EliteCrossbowGoal(EliteCrossbowman mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isAlive() && mob.isHolding(Items.CROSSBOW);
	}

	@Override
	public void start() {
		mob.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		mob.setAggressive(false);
		mob.setTarget(null);
		reset();
		mob.getNavigation().stop();
		mob.getMoveControl().strafe(0.0F, 0.0F);
	}

	private void reset() {
		if (mob.isUsingItem()) {
			mob.stopUsingItem();
		}
		mob.setChargingCrossbow(false);
		mob.setRedCharging(false);
		mob.setLoaded(false);
		phase = Phase.IDLE;
		attack = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		EliteCrossbowmanSettings settings = EliteCrossbowmanModule.settings();
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		double distance = mob.distanceTo(target);
		boolean sees = mob.getSensing().hasLineOfSight(target);
		mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		repath--;
		phaseTicks++;

		boolean aiming = (phase == Phase.AIMING || phase == Phase.FIRING) && sees;
		if (aiming) {
			holdAim(target);
		} else {
			move(target, distance, sees, settings);
		}

		switch (phase) {
			case IDLE -> {
				if (--pause <= 0 && sees && distance <= settings.maxRange() + 2.0) {
					attack = EliteCrossbowmanLogic.choose(mob.getRandom().nextFloat(), cooldowns, settings, distance, target instanceof Player);
					if (attack != null) {
						mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(mob, Items.CROSSBOW));
						mob.setChargingCrossbow(true);
						setPhase(Phase.LOADING);
					}
				}
			}
			case LOADING -> {
				if (attack == null) {
					reset();
				} else if (phaseTicks >= EliteCrossbowmanLogic.loadTicks(attack, settings)) {
					mob.stopUsingItem();
					mob.setChargingCrossbow(false);
					mob.setLoaded(true);
					mob.playSound(SoundEvents.CROSSBOW_LOADING_END.value(), 1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
					setPhase(Phase.AIMING);
				}
			}
			case AIMING -> {
				if (attack == Attack.CHARGED) {
					mob.setRedCharging(true);
					mob.sparkle(level);
				}
				if (aiming && phaseTicks >= EliteCrossbowmanLogic.aimTicks(attack, settings) && facing(target)) {
					shotsLeft = attack == Attack.BURST ? settings.burst().shots() : 1;
					setPhase(Phase.FIRING);
					fire(level, target, settings);
				}
			}
			case FIRING -> {
				if (shotsLeft > 0 && phaseTicks >= settings.burst().interval()) {
					phaseTicks = 0;
					fire(level, target, settings);
				}
			}
		}
	}

	private void setPhase(Phase next) {
		phase = next;
		phaseTicks = 0;
	}

	/** One bolt of the chosen shot; after the last one the crossbow is empty and he pauses. */
	private void fire(ServerLevel level, LivingEntity target, EliteCrossbowmanSettings settings) {
		if (attack == null) {
			reset();
			return;
		}
		EliteCrossbowmanSettings.Normal normal = settings.normal();
		switch (attack) {
			case NORMAL -> mob.shootBolt(level, target, normal.damage(), normal.speed(), normal.inaccuracy());
			case BURST -> mob.shootBolt(level, target, normal.damage() * settings.burst().damageMultiplier(), normal.speed(),
					settings.burst().inaccuracy());
			case CHARGED -> mob.shootRedBolt(level, target, normal.damage() * settings.charged().damageMultiplier(),
					normal.speed() * settings.charged().speedMultiplier(), settings.charged().shieldMultiplier());
			case NET -> mob.shootNet(level, target, settings.net().speed());
		}
		mob.onCrossbowAttackPerformed();
		if (--shotsLeft > 0) {
			return;
		}
		cooldowns[attack.ordinal()] = EliteCrossbowmanLogic.cooldown(attack, settings);
		pause = Mth.randomBetweenInclusive(mob.getRandom(), settings.pauseMin(), Math.max(settings.pauseMin(), settings.pauseMax()));
		reset();
	}

	/** He stands still and turns to face the target. */
	private void holdAim(LivingEntity target) {
		mob.getNavigation().stop();
		// The move control keeps the sideways input of an earlier strafe: clear it.
		mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
		mob.setXxa(0.0F);
		float yaw = yawTo(target);
		mob.setYRot(LostLogic.turnTowards(mob.getYRot(), yaw, AIM_TURN));
		mob.setYBodyRot(mob.getYRot());
		mob.setYHeadRot(mob.getYRot());
	}

	private boolean facing(LivingEntity target) {
		return Mth.degreesDifferenceAbs(mob.getYRot(), yawTo(target)) <= AIM_TOLERANCE;
	}

	private float yawTo(LivingEntity target) {
		return (float) (Mth.atan2(target.getZ() - mob.getZ(), target.getX() - mob.getX()) * Mth.RAD_TO_DEG) - 90.0F;
	}

	private void move(LivingEntity target, double distance, boolean sees, EliteCrossbowmanSettings settings) {
		// Slower while he loads, like a vanilla pillager.
		double speed = phase == Phase.LOADING ? 0.6 : 1.0;
		switch (EliteCrossbowmanLogic.move(distance, sees, settings)) {
			case RETREAT -> {
				if (repath <= 0 || mob.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					Vec3 away = DefaultRandomPos.getPosAway(mob, (int) Math.ceil(settings.minRange()) + 4, 4, target.position());
					if (away != null) {
						mob.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED * speed);
					}
				}
			}
			case APPROACH -> {
				if (repath <= 0 || mob.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					mob.getNavigation().moveTo(target, speed);
				}
			}
			case HOLD -> {
				mob.getNavigation().stop();
				if (--strafeTicks <= 0) {
					strafeTicks = 30 + mob.getRandom().nextInt(40);
					strafeRight = mob.getRandom().nextBoolean();
				}
				mob.getMoveControl().strafe(0.0F, (strafeRight ? 0.4F : -0.4F) * (float) speed);
				mob.lookAt(target, 30.0F, 30.0F);
			}
		}
	}
}
