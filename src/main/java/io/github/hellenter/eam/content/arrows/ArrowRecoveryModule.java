package io.github.hellenter.eam.content.arrows;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

/**
 * Arrows that hit a mob stay in it, and when the mob dies each one drops with a chance (50% by default) raised by the
 * Arrow Recovery enchantment of the bow that shot it. The rest break. Arrows that miss stay in the world as in vanilla;
 * arrows that cannot be picked up (creative, Infinity, skeletons) are not kept. Players are left out: their arrows
 * stay vanilla.
 */
public final class ArrowRecoveryModule {
	public static final ResourceKey<Enchantment> ARROW_RECOVERY = ResourceKey.create(Registries.ENCHANTMENT, Eam.id("arrow_recovery"));

	/** Arrows stuck in a mob, saved with it. */
	public static final AttachmentType<StuckArrows> STUCK_ARROWS = AttachmentRegistry.create(Eam.id("stuck_arrows"),
			builder -> builder.persistent(StuckArrows.CODEC));

	private static volatile ArrowRecoverySettings settings = ArrowRecoverySettings.DEFAULT;
	private static boolean enabled;

	private ArrowRecoveryModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ArrowRecoverySettingsLoader.ID, new ArrowRecoverySettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerLivingEntityEvents.AFTER_DEATH.register(ArrowRecoveryModule::onDeath);
	}

	static void setSettings(ArrowRecoverySettings loaded) {
		settings = loaded;
	}

	/** Called when an arrow that does not pierce has hurt {@code target} and stays in it. */
	public static void onArrowStuck(AbstractArrow arrow, LivingEntity target) {
		if (!enabled || target instanceof Player || arrow.pickup != AbstractArrow.Pickup.ALLOWED
				|| !(target.level() instanceof ServerLevel level)) {
			return;
		}
		ItemStack stack = arrow.getPickupItemStackOrigin().copyWithCount(1);
		if (stack.isEmpty()) {
			return;
		}
		StuckArrows.Arrow stuck = new StuckArrows.Arrow(stack, recoveryLevel(level, arrow.getWeaponItem()));
		if (target.isDeadOrDying()) {
			// The killing arrow lands after the death drops, so it rolls on its own.
			drop(level, target, stuck);
			return;
		}
		StuckArrows arrows = target.getAttached(STUCK_ARROWS);
		target.setAttached(STUCK_ARROWS, arrows == null ? new StuckArrows(List.of(stuck)) : arrows.with(stuck));
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		StuckArrows arrows = entity.removeAttached(STUCK_ARROWS);
		if (arrows == null || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		for (StuckArrows.Arrow arrow : arrows.arrows()) {
			drop(level, entity, arrow);
		}
	}

	private static void drop(ServerLevel level, LivingEntity entity, StuckArrows.Arrow arrow) {
		if (entity.getRandom().nextFloat() < settings.chance(arrow.recoveryLevel())) {
			entity.spawnAtLocation(level, arrow.stack().copy());
		}
	}

	private static int recoveryLevel(ServerLevel level, ItemStack weapon) {
		if (weapon == null || weapon.isEmpty()) {
			return 0;
		}
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ARROW_RECOVERY)
				.map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, weapon))
				.orElse(0);
	}
}
