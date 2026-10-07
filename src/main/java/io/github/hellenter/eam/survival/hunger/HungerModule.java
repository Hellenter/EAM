package io.github.hellenter.eam.survival.hunger;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.survival.hunger.FoodDataAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * MITE hunger: a food bar that grows with the XP level, passive hunger, slow natural regeneration that
 * does not use saturation, starvation that kills on every difficulty, and weaker food.
 */
public final class HungerModule {
	/**
	 * Synced food bar size. Registered even when the module is off, so a client with hunger disabled can
	 * still join a server that has it on. Its presence on a player means the server runs this module.
	 */
	public static final AttachmentType<HungerStats> STATS = AttachmentRegistry.create(
			Eam.id("hunger"),
			builder -> builder.syncWith(HungerStats.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	private static boolean enabled;
	private static volatile HungerRules rules = HungerRules.DEFAULT;

	private HungerModule() {
	}

	public static void init() {
		enabled = true;
		HungerFoods.register();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(HungerRulesLoader.ID, new HungerRulesLoader());
	}

	/** Forces class loading so {@link #STATS} is registered whether or not the module is on. */
	public static void registerAttachments() {
	}

	public static boolean enabled() {
		return enabled;
	}

	static void setRules(HungerRules newRules) {
		rules = newRules;
	}

	/** Replaces {@code FoodData.tick} while the module is on. */
	public static void tick(FoodData foodData, ServerPlayer player) {
		HungerRules current = rules;
		int maxFood = HungerLogic.maxFood(player.experienceLevel, current);
		HungerStats stats = player.getAttached(STATS);
		if (stats == null || stats.maxFood() != maxFood) {
			player.setAttached(STATS, new HungerStats(maxFood));
		}

		ServerLevel level = player.level();
		FoodDataAccessor accessor = (FoodDataAccessor) foodData;
		HungerLogic.TickResult result = HungerLogic.tick(
				new HungerLogic.FoodState(
						foodData.getFoodLevel(),
						foodData.getSaturationLevel(),
						accessor.eam$getExhaustionLevel(),
						accessor.eam$getTickTimer()
				),
				new HungerLogic.TickContext(
						maxFood,
						player.isHurt(),
						level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION),
						level.getDifficulty() == Difficulty.PEACEFUL
				),
				current
		);

		HungerLogic.FoodState state = result.state();
		foodData.setFoodLevel(state.food());
		foodData.setSaturation(state.saturation());
		accessor.eam$setExhaustionLevel(state.exhaustion());
		accessor.eam$setTickTimer(state.tickTimer());

		if (result.heal() > 0.0F) {
			player.heal(result.heal());
		}
		if (result.starvationDamage() > 0.0F) {
			player.hurtServer(level, player.damageSources().starve(), result.starvationDamage());
		}
	}

	/** Whether the player can eat: on both sides, from the synced food bar size. */
	public static boolean needsFood(Player player, boolean vanilla) {
		HungerStats stats = player.getAttached(STATS);
		return stats == null ? vanilla : HungerLogic.needsFood(player.getFoodData().getFoodLevel(), stats.maxFood());
	}

	/** Whether the player has enough food to sprint: on both sides, from the synced food bar size. */
	public static boolean canSprint(Player player, boolean vanilla) {
		HungerStats stats = player.getAttached(STATS);
		return stats == null ? vanilla : HungerLogic.canSprint(player.getFoodData().getFoodLevel(), stats.maxFood());
	}

	/** Number of shank icons the HUD draws. */
	public static int shanks(Player player, int vanilla) {
		HungerStats stats = player.getAttached(STATS);
		return stats == null ? vanilla : HungerLogic.shanks(stats.maxFood());
	}
}
