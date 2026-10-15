package io.github.hellenter.eam.survival.hunger;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.survival.hunger.FoodDataAccessor;
import io.github.hellenter.eam.survival.progression.ProgressionModule;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * MITE hunger: a food bar that grows with the XP level (see {@link ProgressionModule}), passive hunger, slow natural regeneration that
 * does not use saturation, starvation that kills on every difficulty, and weaker food. On top of that, food
 * carries protein, fat and carbs; keeping all three high slows hunger and speeds up regeneration, while running out of
 * one gives its own deficiency: protein weakens hits and mining, fat speeds up hunger, carbs cut sprints short.
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

	/** Present while the player is winded from running without carbs and cannot sprint; synced for client-side sprinting. */
	public static final AttachmentType<Boolean> WINDED = AttachmentRegistry.create(
			Eam.id("winded"),
			builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** Sprint endurance counters while carbs are deficient. Server-only, not saved. */
	private static final AttachmentType<HungerLogic.Stamina> STAMINA = AttachmentRegistry.create(Eam.id("stamina"));

	/** Given while protein is deficient: weaker hits and slower mining. */
	public static final Holder<MobEffect> PROTEIN_DEFICIENCY = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("protein_deficiency"), new DeficiencyEffect(0xB5413B));
	/** Given while fat is deficient: hunger drains faster. */
	public static final Holder<MobEffect> FAT_DEFICIENCY = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("fat_deficiency"), new DeficiencyEffect(0xC9A43A));
	/** Given while carbs are deficient: sprinting gives out. */
	public static final Holder<MobEffect> CARBS_DEFICIENCY = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("carbs_deficiency"), new DeficiencyEffect(0x6E9A3C));

	private static final Identifier PROTEIN_MODIFIER = Eam.id("protein_deficiency");

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

	/** Food exhaustion every player gets each tick just for being alive; zero while the module is off. */
	public static float passiveExhaustion() {
		return enabled ? rules.passiveExhaustion() : 0.0F;
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
		boolean paused = ProgressionModule.inGracePeriod(player) || exempt(player);

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
						paused,
						HungerLogic.deficiencies(nutrition, current.deficiency().threshold())
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
		HungerLogic.Deficiencies deficiencies = HungerLogic.deficiencies(nutrition, current.deficiency().threshold());
		updateDeficiencies(player, deficiencies, current.deficiency());
		updateStamina(player, deficiencies.carbs() && !paused, current.deficiency());
		if (result.heal() > 0.0F) {
			player.heal(result.heal());
		}
		if (result.starvationDamage() > 0.0F) {
			player.hurtServer(level, player.damageSources().starve(), result.starvationDamage());
		}
	}

	/** Creative and spectator players do not get hungry, as in vanilla. */
	private static boolean exempt(ServerPlayer player) {
		return player.isSpectator() || player.getAbilities().invulnerable;
	}

	/** Called when the player finishes eating a food with {@link Nutrients}. */
	static void eat(ServerPlayer player, Nutrients nutrients) {
		if (enabled) {
			Nutrition before = nutrition(player, rules);
			setNutrition(player, HungerLogic.eat(before, nutrients.protein(), nutrients.fat(), nutrients.carbs()));
		}
	}

	/** Keeps each deficiency effect and its penalties on while the nutrient is low, so milk only clears the icon for a tick. */
	private static void updateDeficiencies(ServerPlayer player, HungerLogic.Deficiencies deficiencies, DeficiencyRules rules) {
		updateEffect(player, PROTEIN_DEFICIENCY, deficiencies.protein(), "protein");
		updateEffect(player, FAT_DEFICIENCY, deficiencies.fat(), "fat");
		updateEffect(player, CARBS_DEFICIENCY, deficiencies.carbs(), "carbs");
		updateModifier(player, Attributes.ATTACK_DAMAGE, deficiencies.protein(), -rules.proteinAttackPenalty());
		updateModifier(player, Attributes.BLOCK_BREAK_SPEED, deficiencies.protein(), -rules.proteinMiningPenalty());
	}

	private static void updateEffect(ServerPlayer player, Holder<MobEffect> effect, boolean deficient, String nutrient) {
		boolean has = player.hasEffect(effect);
		if (deficient && !has) {
			player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, true, true));
			player.sendOverlayMessage(Component.translatable("eam.hunger.deficiency." + nutrient).withStyle(ChatFormatting.RED));
		} else if (!deficient && has) {
			player.removeEffect(effect);
		}
	}

	private static void updateModifier(ServerPlayer player, Holder<Attribute> attribute, boolean deficient, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(PROTEIN_MODIFIER);
		if (!deficient || amount == 0.0) {
			if (current != null) {
				instance.removeModifier(PROTEIN_MODIFIER);
			}
		} else if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(
					new AttributeModifier(PROTEIN_MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** Without carbs, sprinting gives out after a while and the player must rest before running again. */
	private static void updateStamina(ServerPlayer player, boolean carbsDeficient, DeficiencyRules rules) {
		HungerLogic.Stamina before = player.getAttachedOrElse(STAMINA, HungerLogic.Stamina.FRESH);
		HungerLogic.Stamina after = HungerLogic.stamina(before, carbsDeficient, player.isSprinting(), rules);
		if (!after.equals(before)) {
			player.setAttached(STAMINA, after);
		}
		if (after.winded() && !before.winded()) {
			player.setAttached(WINDED, true);
			player.setSprinting(false);
			player.sendOverlayMessage(Component.translatable("eam.hunger.winded").withStyle(ChatFormatting.YELLOW));
		} else if (!after.winded() && player.hasAttached(WINDED)) {
			player.removeAttached(WINDED);
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
		return stats == null ? vanilla : HungerLogic.canSprint(player.getFoodData().getFoodLevel(), stats.maxFood()) && !player.hasAttached(WINDED);
	}

	/** Number of shank icons the HUD draws. */
	public static int shanks(Player player, int vanilla) {
		HungerStats stats = player.getAttached(STATS);
		return stats == null ? vanilla : HungerLogic.shanks(stats.maxFood());
	}
}
