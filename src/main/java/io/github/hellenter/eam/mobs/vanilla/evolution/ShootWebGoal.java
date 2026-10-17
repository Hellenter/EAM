package io.github.hellenter.eam.mobs.vanilla.evolution;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * An evolved spider spits a ball of web at the player it hunts, from a few blocks off; the web holds the player in
 * place for a few seconds. Numbers are in the {@code spider_web} section of {@code data/eam/mobs/evolution.json}.
 */
class ShootWebGoal extends Goal {
	private final Mob mob;
	/** Tick count before which the spider does not shoot again; not saved, a reload just lets it shoot sooner. */
	private int readyAt;

	ShootWebGoal(Mob mob) {
		this.mob = mob;
	}

	@Override
	public boolean canUse() {
		if (!Boolean.TRUE.equals(mob.getAttached(MonsterEvolutionModule.WEB_SPINNER))
				|| !(mob.getTarget() instanceof Player target) || !target.isAlive()) {
			return false;
		}
		EvolutionSettings.Web web = MonsterEvolutionModule.settings().web();
		return EvolutionLogic.canShootWeb(mob.distanceTo(target), mob.getSensing().hasLineOfSight(target), readyAt - mob.tickCount,
				web.minRange(), web.maxRange());
	}

	@Override
	public boolean canContinueToUse() {
		return false;
	}

	@Override
	public void start() {
		if (!(mob.level() instanceof ServerLevel level) || !(mob.getTarget() instanceof Player target)) {
			return;
		}
		EvolutionSettings.Web web = MonsterEvolutionModule.settings().web();
		readyAt = mob.tickCount + web.cooldownTicks();
		WebShot shot = new WebShot(level, mob);
		double dx = target.getX() - mob.getX();
		double dz = target.getZ() - mob.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		double dy = EliteCrossbowmanLogic.aimHeight(flat, target.getY(1.0 / 3.0) - shot.getY(), web.speed(),
				EliteCrossbowmanLogic.THROWN_GRAVITY);
		shot.shoot(dx, dy, dz, web.speed(), 2.0F);
		level.addFreshEntity(shot);
		mob.playSound(SoundEvents.LLAMA_SPIT, 0.8F, 1.4F);
	}
}
