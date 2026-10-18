package io.github.hellenter.eam.survival.death;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * A softer death: the player loses only a few experience levels (numbers in {@code data/eam/survival/death.json}) and no
 * experience orbs fall. Items still drop, unless a boss dealt the killing blow and the
 * {@code eam:keep_inventory_on_boss_death} game rule is on. With {@code keep_inventory} on, vanilla keeps everything.
 */
public final class DeathModule {
	/** Bosses: a player they kill keeps their things while {@link #KEEP_INVENTORY_ON_BOSS_DEATH} is on. */
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Eam.id("bosses"));

	public static final GameRule<Boolean> KEEP_INVENTORY_ON_BOSS_DEATH = GameRuleBuilder.forBoolean(true)
			.category(GameRuleCategory.PLAYER)
			.buildAndRegister(Eam.id("keep_inventory_on_boss_death"));

	/** Set on a dying player whose things stay with them; not saved, it only lives until the respawn copies them over. */
	private static final AttachmentType<Boolean> KEEPS_INVENTORY = AttachmentRegistry.create(Eam.id("keeps_inventory_on_death"));

	private static volatile DeathSettings settings = DeathSettings.DEFAULT;
	private static boolean enabled;

	private DeathModule() {
	}

	/** The game rule and the settings exist whatever the config says, so worlds keep the rule's value. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(DeathSettingsLoader.ID, new DeathSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player) {
				markKeepsInventory(player, source);
			}
			return true;
		});
		ServerPlayerEvents.COPY_FROM.register(DeathModule::onRespawn);
	}

	static void setSettings(DeathSettings value) {
		settings = value;
	}

	/** Called by the mixin with vanilla's {@code keep_inventory}: true if the dead player's things stay with them. */
	public static boolean keepsInventory(Player player, boolean keepInventory) {
		return keepInventory || enabled && player.hasAttached(KEEPS_INVENTORY);
	}

	/** Called by the mixin: the lost levels are gone, nothing falls for others to pick up. */
	public static int experienceReward(int original) {
		return enabled ? 0 : original;
	}

	/** Every fatal blow decides anew, so a totem that saved the player earlier leaves nothing behind. */
	private static void markKeepsInventory(ServerPlayer player, DamageSource source) {
		boolean byBoss = source.getEntity() instanceof LivingEntity killer && killer.is(BOSSES);
		if (byBoss && player.level().getGameRules().get(KEEP_INVENTORY_ON_BOSS_DEATH)) {
			player.setAttached(KEEPS_INVENTORY, true);
		} else {
			player.removeAttached(KEEPS_INVENTORY);
		}
	}

	private static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		// Vanilla already moved everything over for keep_inventory and for spectators.
		if (alive || oldPlayer.isSpectator() || newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}
		if (oldPlayer.hasAttached(KEEPS_INVENTORY)) {
			newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
		}
		newPlayer.experienceLevel = DeathLogic.levelsAfterDeath(oldPlayer.experienceLevel, settings.levelsLost());
		newPlayer.experienceProgress = oldPlayer.experienceProgress;
	}
}
