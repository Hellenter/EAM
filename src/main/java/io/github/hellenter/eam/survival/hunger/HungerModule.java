package io.github.hellenter.eam.survival.hunger;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.survival.hunger.FoodDataAccessor;
import io.github.hellenter.eam.survival.progression.ProgressionModule;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * MITE hunger: a food bar that grows with the XP level (see {@link ProgressionModule}), passive hunger, slow natural regeneration that
 * does not use saturation, starvation that kills on every difficulty, and weaker food. On top of that, food
 * carries protein, fat and carbs; keeping all three high slows hunger and speeds up regeneration.
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

	/** Protein, fat and carbs of the player. Reset on death. */
	public static final AttachmentType<Nutrition> NUTRITION = AttachmentRegistry.create(
			Eam.id("nutrition"),
			builder -> builder.persistent(Nutrition.CODEC).syncWith(Nutrition.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Protein, fat and carbs a food gives. */
	public static final DataComponentType<Nutrients> NUTRIENTS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("nutrients"),
			DataComponentType.<Nutrients>builder().persistent(Nutrients.CODEC).networkSynchronized(Nutrients.STREAM_CODEC).build()
	);

	/** Given while any nutrient is at zero. */
	public static final Holder<MobEffect> MALNOURISHED = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("malnourished"), new MalnourishedEffect());

	private static boolean enabled;
	private static volatile HungerRules rules = HungerRules.DEFAULT;

	private HungerModule() {
	}

	public static void init() {
		enabled = true;
		HungerFoods.register();
		ItemComponentTooltipProviderRegistry.addLast(NUTRIENTS);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(HungerRulesLoader.ID, new HungerRulesLoader());
	}

	/**
	 * Forces class loading so the attachments and the item component are registered whether or not the module
	 * is on, keeping client and server registries the same.
	 */
	public static void registerTypes() {
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
		int maxFood = ProgressionModule.capacity(player);
		HungerStats stats = new HungerStats(maxFood, current.balanceThreshold());
		if (!stats.equals(player.getAttached(STATS))) {
			player.setAttached(STATS, stats);
		}
		Nutrition nutrition = nutrition(player, current);

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
						level.getDifficulty() == Difficulty.PEACEFUL,
						HungerLogic.balanced(nutrition, current.balanceThreshold()),
						ProgressionModule.inGracePeriod(player),
						HungerLogic.malnourished(nutrition)
				),
				current
		);

		HungerLogic.FoodState state = result.state();
		foodData.setFoodLevel(state.food());
		foodData.setSaturation(state.saturation());
		accessor.eam$setExhaustionLevel(state.exhaustion());
		accessor.eam$setTickTimer(state.tickTimer());

		if (result.pointSpent()) {
			nutrition = HungerLogic.decay(nutrition, current.nutrientDecay());
			setNutrition(player, nutrition);
		}
		updateMalnourished(player, HungerLogic.malnourished(nutrition));
		if (result.heal() > 0.0F) {
			player.heal(result.heal());
		}
		if (result.starvationDamage() > 0.0F) {
			player.hurtServer(level, player.damageSources().starve(), result.starvationDamage());
		}
	}

	/** Called when the player finishes eating a food with {@link Nutrients}. */
	static void eat(ServerPlayer player, Nutrients nutrients) {
		if (enabled) {
			Nutrition before = nutrition(player, rules);
			setNutrition(player, HungerLogic.eat(before, nutrients.protein(), nutrients.fat(), nutrients.carbs()));
		}
	}

	/** Keeps the effect on while a nutrient is at zero, so milk only clears it for a tick. */
	private static void updateMalnourished(ServerPlayer player, boolean malnourished) {
		boolean has = player.hasEffect(MALNOURISHED);
		if (malnourished && !has) {
			player.addEffect(new MobEffectInstance(MALNOURISHED, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
		} else if (!malnourished && has) {
			player.removeEffect(MALNOURISHED);
		}
	}

	private static Nutrition nutrition(ServerPlayer player, HungerRules current) {
		Nutrition nutrition = player.getAttached(NUTRITION);
		if (nutrition == null) {
			nutrition = Nutrition.of(current.startingNutrients());
			player.setAttached(NUTRITION, nutrition);
		}
		return nutrition;
	}

	private static void setNutrition(ServerPlayer player, Nutrition nutrition) {
		if (!nutrition.equals(player.getAttached(NUTRITION))) {
			player.setAttached(NUTRITION, nutrition);
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
