package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * The mark a Lost longbowman's marking arrow leaves: for a while the marked one takes more damage from everything, and
 * every one of the Lost near him who sees him leaves whoever he fights and goes for him.
 */
public final class LostMark {
	/** Blood red, as the arrow's trail. */
	static final int COLOR = 0xC4161C;
	public static final Holder<MobEffect> MARKED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("marked"),
			new MarkEffect(COLOR));

	private LostMark() {
	}

	/** Called once at startup, so the effect is registered with the other mod content. */
	static void register() {
	}

	/** The damage {@code entity} takes for a hit of {@code amount}: more while it is marked. */
	public static float damage(LivingEntity entity, float amount) {
		if (amount <= 0.0F || !entity.hasEffect(MARKED)) {
			return amount;
		}
		return LongbowmanLogic.markedDamage(amount, LostModule.settings().longbowman().mark().damageBonus());
	}

	static boolean marked(LivingEntity entity) {
		return entity.hasEffect(MARKED);
	}

	/** One of the Lost who sees a marked player near him leaves whoever he fights for him; the nearest, if several. */
	static void preferMarked(Lost lost) {
		LivingEntity target = lost.getTarget();
		if (target != null && target.isAlive() && marked(target)) {
			return;
		}
		Player marked = nearestMarked(lost);
		if (marked != null) {
			lost.setTarget(marked);
		}
	}

	private static @Nullable Player nearestMarked(Lost lost) {
		double range = LostModule.settings().longbowman().mark().priorityRange();
		return lost.level().getEntitiesOfClass(Player.class, lost.getBoundingBox().inflate(range),
						player -> player.isAlive() && marked(player) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
								&& lost.canAttack(player) && lost.distanceToSqr(player) <= range * range
								&& lost.getSensing().hasLineOfSight(player))
				.stream()
				.min(Comparator.comparingDouble(lost::distanceToSqr))
				.orElse(null);
	}
}
