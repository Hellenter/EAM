package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Thirst: a 0..20 water bar that drains with activity and time, refills from drinks and water sources,
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

	public static final ResourceKey<DamageType> DEHYDRATION = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("dehydration"));

	/** Default water restored per item. Every potion counts, the water bottle included. */
	private static final Map<Item, Integer> DRINKS = Map.of(
			Items.POTION, 6,
			Items.MILK_BUCKET, 8,
			Items.HONEY_BOTTLE, 2,
			Items.MUSHROOM_STEW, 3,
			Items.BEETROOT_SOUP, 3,
			Items.RABBIT_STEW, 3,
			Items.SUSPICIOUS_STEW, 3,
			Items.MELON_SLICE, 1
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
		PayloadTypeRegistry.serverboundPlay().register(DrinkFromSourcePayload.TYPE, DrinkFromSourcePayload.STREAM_CODEC);
		DefaultItemComponentEvents.MODIFY.register(context -> DRINKS.forEach((item, water) ->
				context.modify(item, components -> components.set(HYDRATION, new Hydration(water)))));
	}

	/** Turns the mechanic on: drain, damage, drinking. */
	public static void init() {
		enabled = true;
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
				level.getBiome(player.blockPosition()).value().getBaseTemperature() > settings.hotTemperature());

		Thirst thirst = player.getAttachedOrCreate(THIRST);
		ThirstLogic.TickResult result = ThirstLogic.tick(thirst, player.getAttachedOrCreate(COUNTERS), settings, environment);

		if (!result.thirst().equals(thirst)) {
			player.setAttached(THIRST, result.thirst());
		}
		player.setAttached(COUNTERS, result.counters());

		if (result.dehydrate()) {
			player.hurtServer(level, player.damageSources().source(DEHYDRATION), settings.dehydrationDamage());
		}
		if (player.isSprinting() && !ThirstLogic.canSprint(result.thirst(), settings)) {
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
		return thirst == null || player.getAbilities().mayfly || ThirstLogic.canSprint(thirst, settings);
	}

	static void drink(ServerPlayer player, int water) {
		if (enabled) {
			player.setAttached(THIRST, player.getAttachedOrCreate(THIRST).add(water));
		}
	}

	private static void sipFromSource(ServerPlayer player) {
		Thirst thirst = player.getAttachedOrCreate(THIRST);
		ThirstCounters counters = player.getAttachedOrCreate(COUNTERS);
		if (!player.isAlive() || !player.getMainHandItem().isEmpty() || !ThirstLogic.canSip(thirst, counters)) {
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
	}
}
