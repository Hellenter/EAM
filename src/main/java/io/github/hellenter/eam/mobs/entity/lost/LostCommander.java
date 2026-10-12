package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.cavalry.LostWarhorse;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The commander's horn: the Lost serve whoever carries it. They never attack him, they go for whoever he attacks or
 * whoever attacks him, and for every hostile mob near him unbidden. Its numbers are in {@code lost.json} under
 * {@code commander_horn}.
 */
public final class LostCommander {
	private LostCommander() {
	}

	static LostSettings.CommanderHorn settings() {
		return LostModule.settings().commanderHorn();
	}

	/** Whether {@code entity} is a living player who carries the commander's horn anywhere in his inventory. */
	public static boolean commands(@Nullable Entity entity) {
		return entity instanceof Player player && player.isAlive() && !player.isSpectator()
				&& player.getInventory().contains(stack -> stack.is(EamItems.COMMANDER_HORN));
	}

	/** The Lost and those who march with them: they serve the horn and are never its foes. */
	public static boolean isKin(@Nullable Entity entity) {
		return entity instanceof Lost || entity instanceof LostCavalryman || entity instanceof LostWarhorse || entity instanceof UndeadWarlock;
	}

	/** The nearest player with the horn whom {@code mob} serves, or {@code null}. */
	static @Nullable Player near(Mob mob) {
		double range = settings().guardRange();
		return mob.level().getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), range, LostCommander::commands);
	}

	/** Blows the horn: {@code wave} comes near {@code player}, in sight, and stays. */
	public static List<Mob> call(ServerLevel level, Player player, LostWave wave) {
		LostSettings.CommanderHorn horn = settings();
		Vec3 fallback = player.position();
		return LostReinforcements.bring(level, player, wave, horn.minDistance(), horn.maxDistance(), false, fallback, true);
	}

	/** Ticks between blasts of the horn. */
	public static int cooldown() {
		return settings().cooldown();
	}
}
