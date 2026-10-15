package io.github.hellenter.eam.world.bloodmoon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic.Piece;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The blood moon: one night between days 15 and 20 of a world, then every 10 to 20 days. While it is up, hostile mobs
 * spawn in armor and mostly armed, hit 20% harder, sense players through walls from 10 blocks, hunt animals, and give
 * double loot and experience; nobody can sleep. The calendar is saved on the overworld; numbers are in
 * {@code data/eam/events/blood_moon.json}. Ops start and stop it with {@code /eam bloodmoon start|stop}.
 */
public final class BloodMoonModule {
	/** Mobs that spawn in armor during a blood moon. */
	public static final TagKey<EntityType<?>> ARMORED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("blood_moon_armored"));
	/** Mobs that may spawn with a melee weapon during a blood moon. */
	public static final TagKey<EntityType<?>> ARMED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("blood_moon_armed"));
	/** Mobs that hunt animals during a blood moon. */
	public static final TagKey<EntityType<?>> HUNTERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("blood_moon_hunters"));
	/** Bosses whose loot is one of a kind: the blood moon does not multiply it. */
	public static final TagKey<EntityType<?>> SINGLE_LOOT = TagKey.create(Registries.ENTITY_TYPE, Eam.id("blood_moon_single_loot"));

	/**
	 * The calendar, stored on the overworld.
	 *
	 * @param nextDay   the day of the next blood moon; 0 until the first one is rolled
	 * @param forcedDay a day an op started the event on, which then lasts until that day ends; 0 for none
	 */
	public record State(int nextDay, int forcedDay) {
		static final State NONE = new State(0, 0);
		static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("next_day").forGetter(State::nextDay),
				Codec.INT.optionalFieldOf("forced_day", 0).forGetter(State::forcedDay)
		).apply(instance, State::new));
	}

	public static final AttachmentType<State> STATE = AttachmentRegistry.create(Eam.id("blood_moon"),
			builder -> builder.persistent(State.CODEC));

	private static final int HUNT_PRIORITY = 4;
	private static final int CHECK_INTERVAL = 20;

	private static volatile BloodMoonSettings settings = BloodMoonSettings.DEFAULT;
	private static boolean enabled;
	private static volatile boolean active;

	private BloodMoonModule() {
	}

	/** The payload and settings exist whatever the config says, so client and server always match. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(BloodMoonPayload.TYPE, BloodMoonPayload.STREAM_CODEC);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BloodMoonSettingsLoader.ID, new BloodMoonSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL == 0) {
				update(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> active = false);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob.is(HUNTERS)) {
				addHuntGoal(mob);
			}
		});
		LootTableEvents.MODIFY_DROPS.register(BloodMoonModule::modifyDrops);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommand(dispatcher));
	}

	static void setSettings(BloodMoonSettings value) {
		settings = value;
	}

	// --- Calendar ---

	private static void update(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long dayTime = overworld.getOverworldClockTime();
		int today = BloodMoonLogic.dayNumber(dayTime);
		State state = overworld.getAttachedOrElse(STATE, State.NONE);
		State updated = state;
		if (updated.nextDay() == 0) {
			updated = new State(BloodMoonLogic.firstDay(settings, overworld.getRandom()::nextInt), updated.forcedDay());
		} else if (updated.nextDay() < today) {
			updated = new State(BloodMoonLogic.nextDay(updated.nextDay(), today, settings, overworld.getRandom()::nextInt), updated.forcedDay());
		}
		if (updated.forcedDay() != 0 && updated.forcedDay() != today) {
			updated = new State(updated.nextDay(), 0);
		}
		if (!updated.equals(state)) {
			overworld.setAttached(STATE, updated);
		}
		setActive(server, BloodMoonLogic.active(updated.nextDay(), updated.forcedDay(), dayTime, settings.startTime()));
	}

	private static void setActive(MinecraftServer server, boolean value) {
		if (active == value) {
			return;
		}
		active = value;
		Eam.LOGGER.info("Blood moon {}", value ? "rises" : "sets");
		Component message = Component.translatable(value ? "event.eam.blood_moon.start" : "event.eam.blood_moon.end")
				.withStyle(ChatFormatting.DARK_RED);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sync(player);
			if (player.level().dimension() != Level.OVERWORLD) {
				continue;
			}
			player.sendSystemMessage(message);
			if (value) {
				player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("event.eam.blood_moon.title")
						.withStyle(ChatFormatting.DARK_RED)));
				if (player.isSleeping()) {
					player.stopSleepInBed(true, true);
				}
			}
		}
	}

	private static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, BloodMoonPayload.TYPE)) {
			ServerPlayNetworking.send(player, new BloodMoonPayload(active));
		}
	}

	/** The day of the next scheduled blood moon, 0 before the first is rolled; the blue moon keeps off it. */
	public static int scheduledDay(ServerLevel overworld) {
		return overworld.getAttachedOrElse(STATE, State.NONE).nextDay();
	}

	/** True while a blood moon is up over {@code level}: only the overworld has one. */
	public static boolean activeIn(Level level) {
		return enabled && active && level.dimension() == Level.OVERWORLD;
	}

	// --- Hooks ---

	/** Why a player cannot sleep now, or null. */
	public static Player.BedSleepingProblem sleepProblem(Level level) {
		return activeIn(level) ? new Player.BedSleepingProblem(Component.translatable("block.eam.bed.blood_moon")) : null;
	}

	/** How far hostile mobs sense players through walls; 0 outside a blood moon. */
	public static int senseRadius(Level level) {
		return activeIn(level) ? settings.senseRadius() : 0;
	}

	/** Damage {@code target} takes from {@code source}: hostile mobs hit harder under a blood moon. */
	public static float damage(LivingEntity target, DamageSource source, float amount) {
		return source.getEntity() instanceof Enemy && activeIn(target.level())
				? BloodMoonLogic.damage(amount, settings.damageMultiplier()) : amount;
	}

	/** Experience a dying mob leaves. */
	public static int experience(LivingEntity entity, int amount) {
		return entity instanceof Enemy && activeIn(entity.level()) ? (int) Math.round(amount * settings.experienceMultiplier()) : amount;
	}

	/** Called for every entity freshly added to a level (spawns, not chunk loads): arms and armors hostile mobs. */
	public static void onFreshEntity(Entity entity) {
		if (!(entity instanceof Mob mob) || !activeIn(mob.level())) {
			return;
		}
		RandomSource random = mob.getRandom();
		BloodMoonSettings settings = BloodMoonModule.settings;
		if (mob.is(ARMORED)) {
			equipArmor(mob, random, settings);
		}
		if (mob.is(ARMED) && random.nextDouble() < settings.weaponChance()) {
			String material = BloodMoonLogic.pick(settings.weaponMaterials(), random::nextInt);
			String weapon = BloodMoonLogic.pick(settings.weapons(), random::nextInt);
			if (material != null && weapon != null) {
				item(material, weapon).ifPresent(item -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item)));
			}
		}
	}

	private static void equipArmor(Mob mob, RandomSource random, BloodMoonSettings settings) {
		Integer count = BloodMoonLogic.pick(Map.of(1, settings.armorPieceWeights().get(0), 2, settings.armorPieceWeights().get(1),
				3, settings.armorPieceWeights().get(2), 4, settings.armorPieceWeights().get(3)), random::nextInt);
		String material = BloodMoonLogic.pick(settings.armorMaterials(), random::nextInt);
		if (count == null || material == null) {
			return;
		}
		for (Piece piece : BloodMoonLogic.armorPieces(count, random::nextInt)) {
			EquipmentSlot slot = switch (piece) {
				case HELMET -> EquipmentSlot.HEAD;
				case CHESTPLATE -> EquipmentSlot.CHEST;
				case LEGGINGS -> EquipmentSlot.LEGS;
				case BOOTS -> EquipmentSlot.FEET;
			};
			item(material, piece.name().toLowerCase()).ifPresent(item -> mob.setItemSlot(slot, new ItemStack(item)));
		}
	}

	/** {@code minecraft:iron} and {@code sword} make {@code minecraft:iron_sword}. */
	private static Optional<Item> item(String material, String suffix) {
		Identifier id = Identifier.tryParse(material + "_" + suffix);
		return id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
	}

	private static void addHuntGoal(Mob mob) {
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof HuntAnimalsGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		targets.addGoal(HUNT_PRIORITY, new HuntAnimalsGoal(mob));
	}

	private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		// Only a hostile mob's own death table, not tables nested in it or block drops.
		if (!(entity instanceof Enemy) || !context.hasParameter(LootContextParams.DAMAGE_SOURCE)
				|| context.hasParameter(LootContextParams.BLOCK_STATE) || entity.is(SINGLE_LOOT) || !activeIn(entity.level())
				|| entity.getType().getDefaultLootTable().filter(table::is).isEmpty()) {
			return;
		}
		List<ItemStack> original = new ArrayList<>(drops);
		for (int i = 1; i < settings.dropMultiplier(); i++) {
			original.forEach(stack -> drops.add(stack.copy()));
		}
	}

	// --- Command ---

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Eam.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("bloodmoon")
						.executes(context -> {
							context.getSource().sendSuccess(() -> status(context.getSource().getServer()), false);
							return active ? 1 : 0;
						})
						.then(Commands.literal("start").executes(context -> set(context.getSource(), true)))
						.then(Commands.literal("stop").executes(context -> set(context.getSource(), false)))));
	}

	private static int set(CommandSourceStack source, boolean start) {
		MinecraftServer server = source.getServer();
		ServerLevel overworld = server.overworld();
		int today = BloodMoonLogic.dayNumber(overworld.getOverworldClockTime());
		State state = overworld.getAttachedOrElse(STATE, State.NONE);
		if (start) {
			overworld.setAttached(STATE, new State(state.nextDay(), today));
		} else {
			// Stopping tonight's blood moon moves the calendar on to the next one.
			int next = state.nextDay() == today ? BloodMoonLogic.nextDay(today, today, settings, overworld.getRandom()::nextInt) : state.nextDay();
			overworld.setAttached(STATE, new State(next, 0));
		}
		update(server);
		source.sendSuccess(() -> status(server), true);
		return 1;
	}

	private static Component status(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		int today = BloodMoonLogic.dayNumber(overworld.getOverworldClockTime());
		int next = overworld.getAttachedOrElse(STATE, State.NONE).nextDay();
		return active ? Component.translatable("commands.eam.blood_moon.active", today)
				: Component.translatable("commands.eam.blood_moon.next", today, next);
	}
}
