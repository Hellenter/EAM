package io.github.hellenter.eam.mobs.entity.warlock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The warlock's fight: he keeps the player at casting range, backing away from one who comes close and walking up to
 * one far off or out of sight, and casts one spell at a time, standing still while his hands work.
 */
final class WarlockCastGoal extends Goal {
	private static final int REPATH_INTERVAL = 10;
	private static final double RETREAT_SPEED = 1.15;

	private final UndeadWarlock warlock;
	private int repath;
	private int strafeTicks;
	private boolean strafeRight;

	WarlockCastGoal(UndeadWarlock warlock) {
		this.warlock = warlock;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = warlock.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public void start() {
		warlock.setAggressive(true);
		repath = 0;
	}

	@Override
	public void stop() {
		warlock.setAggressive(false);
		warlock.getNavigation().stop();
		warlock.getMoveControl().strafe(0.0F, 0.0F);
		if (warlock.spell() != WarlockSpell.NONE) {
			warlock.endSpell(0, 0);
		}
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = warlock.getTarget();
		if (target == null || !(warlock.level() instanceof ServerLevel level)) {
			return;
		}
		UndeadWarlockSettings settings = UndeadWarlockModule.settings();
		warlock.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (warlock.spell() != WarlockSpell.NONE) {
			warlock.getNavigation().stop();
			warlock.getMoveControl().strafe(0.0F, 0.0F);
			warlock.lookAt(target, 30.0F, 30.0F);
			cast(level, target, settings);
			return;
		}
		double distance = warlock.distanceTo(target);
		boolean sees = warlock.getSensing().hasLineOfSight(target);
		move(target, distance, sees, settings);
		WarlockSpell spell = UndeadWarlockLogic.choose(distance, sees, warlock.readySpells(),
				warlock.unbuffedLost(level, settings.buff().radius()).size(), warlock.batsNearby(level), warlock.getRandom().nextDouble(), settings);
		if (spell != WarlockSpell.NONE) {
			warlock.startSpell(spell);
		}
	}

	private void move(LivingEntity target, double distance, boolean sees, UndeadWarlockSettings settings) {
		repath--;
		switch (UndeadWarlockLogic.move(distance, sees, settings.minRange(), settings.maxRange())) {
			case RETREAT -> {
				if (repath <= 0 || warlock.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					Vec3 away = DefaultRandomPos.getPosAway(warlock, (int) Math.ceil(settings.minRange()) + 4, 4, target.position());
					if (away != null) {
						warlock.getNavigation().moveTo(away.x, away.y, away.z, RETREAT_SPEED);
					}
				}
			}
			case APPROACH -> {
				if (repath <= 0 || warlock.getNavigation().isDone()) {
					repath = REPATH_INTERVAL;
					warlock.getNavigation().moveTo(target, 1.0);
				}
			}
			case HOLD -> {
				warlock.getNavigation().stop();
				if (--strafeTicks <= 0) {
					strafeTicks = 30 + warlock.getRandom().nextInt(40);
					strafeRight = warlock.getRandom().nextBoolean();
				}
				warlock.getMoveControl().strafe(0.0F, strafeRight ? 0.35F : -0.35F);
			}
		}
	}

	/** One tick of the spell in his hands: it goes off on its tick and ends when its gesture does. */
	private void cast(ServerLevel level, LivingEntity target, UndeadWarlockSettings settings) {
		WarlockSpell spell = warlock.spell();
		int tick = warlock.castTick();
		switch (spell) {
			case MISSILES -> {
				int index = UndeadWarlockLogic.missileAt(tick, settings.missiles().count());
				if (index >= 0) {
					warlock.fireMissile(target, index, settings.missiles().count(), settings.missiles());
				}
			}
			case WIND -> {
				if (tick == spell.release()) {
					warlock.gust(level, settings.wind());
				}
			}
			case BUFF -> {
				if (tick == spell.release()) {
					warlock.buff(level, target, settings.buff());
				}
			}
			case BATS -> {
				if (tick == spell.release()) {
					warlock.summonBats(level, target, settings.bats());
				}
			}
			case NONE -> {
			}
		}
		if (tick >= spell.length(settings.missiles().count())) {
			int cooldown = switch (spell) {
				case MISSILES -> settings.missiles().cooldown();
				case WIND -> settings.wind().cooldown();
				case BUFF -> settings.buff().cooldown();
				case BATS -> settings.bats().cooldown();
				case NONE -> 0;
			};
			warlock.endSpell(cooldown, settings.spellGap());
		}
	}
}
