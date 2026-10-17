package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.bucket.BucketContent;
import io.github.hellenter.eam.content.bucket.MetalBuckets;
import io.github.hellenter.eam.survival.progression.ProgressionModule;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Thirst: a water bar that grows with the XP level (see {@link ProgressionModule}), drains with activity and time, refills from drinks and water sources,
 * and hurts the player at zero.
 */
public final class ThirstModule {
	public static final AttachmentType<Thirst> THIRST = AttachmentRegistry.create(Eam.id("thirst"), builder -> builder
			.initializer(Thirst::full)
			.persistent(Thirst.CODEC)
			.syncWith(Thirst.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	public static final AttachmentType<ThirstCounters> COUNTERS = AttachmentRegistry.create(Eam.id("thirst_counters"), builder -> builder
			.initializer(() -> ThirstCounters.ZERO)
			.persistent(ThirstCounters.CODEC));

	public static final DataComponentType<Hydration> HYDRATION = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("hydration"),
			DataComponentType.<Hydration>builder().persistent(Hydration.CODEC).networkSynchronized(Hydration.STREAM_CODEC).build());

	/** Unboiled water in a bowl or a bottle: drinking it may poison like a sip from a source. */
	public static final DataComponentType<RawWater> RAW_WATER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("raw_water"),
			DataComponentType.<RawWater>builder().persistent(RawWater.CODEC).networkSynchronized(RawWater.STREAM_CODEC).build());

	public static final DataComponentType<FlaskWater> FLASK_WATER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("flask_water"),
			DataComponentType.<FlaskWater>builder().persistent(FlaskWater.CODEC).networkSynchronized(FlaskWater.STREAM_CODEC).build());

	/** Charcoal filter uses left on a filtered flask. */
	public static final DataComponentType<Integer> FILTER = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Eam.id("filter"),
			DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	public static final RecipeSerializer<FilterFlaskRecipe> FILTER_FLASK_RECIPE = Registry.register(
			BuiltInRegistries.RECIPE_SERIALIZER, Eam.id("filter_flask"), FilterFlaskRecipe.SERIALIZER);
	public static final Holder<MobEffect> FOOD_POISONING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("food_poisoning"), new FoodPoisoningEffect());

	/** Biomes whose water poisons more often (jungles, swamps). */
	public static final TagKey<Biome> DIRTY_WATER = TagKey.create(Registries.BIOME, Eam.id("dirty_water"));

	public static final ResourceKey<DamageType> DEHYDRATION = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("dehydration"));

	/** Default water restored per item. Every potion counts, the water bottle included. */
	private static final Map<Item, Integer> DRINKS = Map.ofEntries(
			Map.entry(Items.POTION, 6),
			Map.entry(Items.MILK_BUCKET, 8),
			Map.entry(Items.HONEY_BOTTLE, 2),
			Map.entry(Items.MUSHROOM_STEW, 3),
			Map.entry(Items.BEETROOT_SOUP, 3),
			Map.entry(Items.RABBIT_STEW, 3),
			Map.entry(Items.SUSPICIOUS_STEW, 3),
			Map.entry(Items.MELON_SLICE, 1),
			// Fruits: half a droplet.
			Map.entry(Items.APPLE, 1),
			Map.entry(Items.SWEET_BERRIES, 1),
			Map.entry(Items.GLOW_BERRIES, 1),
			Map.entry(Items.CHORUS_FRUIT, 1),
			Map.entry(Items.GOLDEN_APPLE, 1),
			Map.entry(Items.ENCHANTED_GOLDEN_APPLE, 1)
	);

	private static boolean enabled;
	private static volatile ThirstSettings settings = ThirstSettings.DEFAULT;

	private ThirstModule() {
	}

	/**
	 * Registers the attachment, item component and payload. Runs even when thirst is off in the config, so client and
	 * server agree on registries whatever their config files say.
	 */
	public static void register() {
		RainWater.register();
		PayloadTypeRegistry.serverboundPlay().register(DrinkFromSourcePayload.TYPE, DrinkFromSourcePayload.STREAM_CODEC);
		DefaultItemComponentEvents.MODIFY.register(context -> {
			DRINKS.forEach((item, water) -> context.modify(item, components -> components.set(HYDRATION, new Hydration(water))));
			// A water bottle must be boiled first; the component checks the potion, so brewed potions stay safe.
			context.modify(Items.POTION, components -> components.set(RAW_WATER, RawWater.INSTANCE));
			// Milk in a metal bucket quenches thirst like milk in the vanilla one.
			Hydration milk = new Hydration(DRINKS.get(Items.MILK_BUCKET));
			MetalBuckets.metalSets().forEach(set -> context.modify(set.get(BucketContent.MILK), components -> components.set(HYDRATION, milk)));
		});
		// A world played with thirst on keeps the saved bar; drop it so a disabled module leaves no HUD behind.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!enabled) {
				handler.player.removeAttached(THIRST);
				handler.player.removeAttached(COUNTERS);
			}
		});
	}

	/** Turns the mechanic on: drain, damage, drinking. */
	public static void init() {
		enabled = true;
		RainWater.init();
		ItemComponentTooltipProviderRegistry.addLast(RAW_WATER);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ThirstSettingsLoader.ID, new ThirstSettingsLoader());
		ServerTickEvents.END_SERVER_TICK.register(ThirstModule::tick);
		ServerPlayNetworking.registerGlobalReceiver(DrinkFromSourcePayload.TYPE, (payload, context) -> sipFromSource(context.player()));
	}

	static void setSettings(ThirstSettings value) {
		settings = value;
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tick(player);
		}
	}

	private static void tick(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable) {
			return;
		}

		ThirstSettings settings = ThirstModule.settings;
		ServerLevel level = player.level();
		ThirstLogic.Environment environment = new ThirstLogic.Environment(
				level.getDifficulty() == Difficulty.PEACEFUL,
				level.getBiome(player.blockPosition()).value().getBaseTemperature(),
				player.hasEffect(FOOD_POISONING),
				ProgressionModule.inGracePeriod(player));

		Thirst thirst = player.getAttachedOrCreate(THIRST);
		int max = ProgressionModule.capacity(player);
		if (thirst.max() != max) {
			thirst = thirst.withMax(max);
			player.setAttached(THIRST, thirst);
		}
		ThirstLogic.TickResult result = ThirstLogic.tick(thirst, player.getAttachedOrCreate(COUNTERS), settings, environment);

		if (!result.thirst().equals(thirst)) {
			player.setAttached(THIRST, result.thirst());
		}
		player.setAttached(COUNTERS, result.counters());

		if (result.dehydrate()) {
			player.hurtServer(level, player.damageSources().source(DEHYDRATION), settings.dehydrationDamage());
		}
		if (player.isSprinting() && !ThirstLogic.canSprint(result.thirst())) {
			player.setSprinting(false);
		}
	}

	/** Called from the food exhaustion hook: activity that tires the player also makes them thirsty. */
	public static void addExhaustion(Player player, float amount) {
		if (enabled && player instanceof ServerPlayer serverPlayer) {
			serverPlayer.setAttached(COUNTERS, ThirstLogic.addActivity(serverPlayer.getAttachedOrCreate(COUNTERS), amount, settings));
		}
	}

	/** Whether thirst lets the player sprint. Runs on both sides; the client reads the synced bar. */
	public static boolean canSprint(Player player) {
		Thirst thirst = player.getAttached(THIRST);
		boolean off = !player.level().isClientSide() && !enabled;
		return off || thirst == null || player.getAbilities().mayfly || ThirstLogic.canSprint(thirst);
	}

	/** Extra passive thirst for one tick: {@code factor} times the normal drain of standing still. */
	public static void addPassiveDrain(ServerPlayer player, float factor) {
		if (enabled && factor > 0.0F && !ProgressionModule.inGracePeriod(player)) {
			ThirstSettings settings = ThirstModule.settings;
			float temperature = player.level().getBiome(player.blockPosition()).value().getBaseTemperature();
			player.setAttached(COUNTERS, ThirstLogic.addPassive(player.getAttachedOrCreate(COUNTERS), settings,
					factor * ThirstLogic.climateMultiplier(settings, temperature)));
		}
	}

	/** One tick of food poisoning: costs water and food. */
	static void poisoningTick(ServerPlayer player) {
		if (!enabled || player.getAbilities().invulnerable) {
			return;
		}
		ThirstSettings settings = ThirstModule.settings;
		player.setAttached(COUNTERS, ThirstLogic.addPoisoning(player.getAttachedOrCreate(COUNTERS), settings));
		player.getFoodData().addExhaustion(settings.poisoning().foodExhaustion());
	}

	/** Fills a flask from a water source; the server rolls whether the water is dirty. */
	public static void fillFlask(ServerPlayer player, ItemStack flask, FlaskLogic.Type type) {
		FlaskWater current = flask.getOrDefault(FLASK_WATER, FlaskWater.EMPTY);
		flask.set(FLASK_WATER, FlaskLogic.fill(current, type, settings.flask(), player.getRandom().nextFloat()));
	}

	/** One drink from a flask: water for the player, wear for the filter, maybe food poisoning. */
	public static void drinkFromFlask(ServerPlayer player, ItemStack flask, FlaskLogic.Type type) {
		FlaskWater current = flask.getOrDefault(FLASK_WATER, FlaskWater.EMPTY);
		FlaskLogic.Drink drink = FlaskLogic.drink(current, type, flask.getOrDefault(FILTER, -1));

		flask.set(FLASK_WATER, drink.remaining());
		if (drink.filter() >= 0) {
			flask.set(FILTER, drink.filter());
		}
		drink(player, drink.water());
		if (drink.poisons() && enabled) {
			poison(player, settings.flask().poisoningDuration());
		}
	}

	/** Drinking unboiled water out of a bowl or a bottle: the same chance of food poisoning as a sip from a source. */
	static void drinkRawWater(ServerPlayer player) {
		PoisoningSettings poisoning = settings.poisoning();
		if (enabled && player.getRandom().nextFloat() < poisoning.chance()) {
			poison(player, poisoning.duration());
		}
	}

	/** Whether the player has room for more water. Runs on both sides; true when the bar is not synced. */
	public static boolean canDrink(Player player) {
		Thirst thirst = player.getAttached(THIRST);
		return thirst == null || !thirst.isFull();
	}

	/** Food poisoning, unless the player is still immune after the last bout. */
	private static void poison(ServerPlayer player, int duration) {
		if (player.getAttachedOrCreate(COUNTERS).immunity() == 0) {
			player.addEffect(new MobEffectInstance(FOOD_POISONING, duration));
		}
	}

	static void drink(ServerPlayer player, int water) {
		if (enabled) {
			player.setAttached(THIRST, player.getAttachedOrCreate(THIRST).add(water));
		}
	}

	private static void sipFromSource(ServerPlayer player) {
		Thirst thirst = player.getAttachedOrCreate(THIRST);
		ThirstCounters counters = player.getAttachedOrCreate(COUNTERS);
		if (!player.isAlive() || player.isSpectator() || !player.getMainHandItem().isEmpty() || !ThirstLogic.canSip(thirst, counters)) {
			return;
		}

		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.WATER, player));
		if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
			return;
		}

		player.setAttached(THIRST, thirst.add(settings.sourceSip()));
		player.setAttached(COUNTERS, counters.withSipCooldown(settings.sipCooldown()));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS,
				0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);

		boolean dirtyWater = level.getBiome(hit.getBlockPos()).is(DIRTY_WATER);
		if (ThirstLogic.poisonsOnSip(counters, settings, dirtyWater, level.getRandom().nextFloat())) {
			poison(player, settings.poisoning().duration());
		}
	}
}
