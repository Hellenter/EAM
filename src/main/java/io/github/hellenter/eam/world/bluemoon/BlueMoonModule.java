package io.github.hellenter.eam.world.bluemoon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * The blue moon: a calm night about 30% more often than the blood moon and never on the same night. While it is up
 * no monster spawns in the overworld (natural spawns, patrols, phantoms, sieges), crops on farmland grow faster and
 * harvested crops give more produce. The calendar is saved on the overworld; numbers are in
 * {@code data/eam/events/blue_moon.json}. Ops start and stop it with {@code /eam bluemoon start|stop}.
 */
public final class BlueMoonModule {
	/** Plants whose harvest grows under a blue moon. */
	public static final TagKey<Block> HARVEST_BLOCKS = TagKey.create(Registries.BLOCK, Eam.id("blue_moon_harvest"));
	/** Produce that drops more under a blue moon; seeds are left out. */
	public static final TagKey<Item> HARVEST_ITEMS = TagKey.create(Registries.ITEM, Eam.id("blue_moon_harvest"));

	/**
	 * The calendar, stored on the overworld.
	 *
	 * @param nextDay   the day of the next blue moon; 0 until the first one is rolled
	 * @param forcedDay a day an op started the event on, which then lasts until that day ends; 0 for none
	 */
	public record State(int nextDay, int forcedDay) {
		static final State NONE = new State(0, 0);
		static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("next_day").forGetter(State::nextDay),
				Codec.INT.optionalFieldOf("forced_day", 0).forGetter(State::forcedDay)
		).apply(instance, State::new));
	}

	public static final AttachmentType<State> STATE = AttachmentRegistry.create(Eam.id("blue_moon"),
			builder -> builder.persistent(State.CODEC));

	private static final int CHECK_INTERVAL = 20;

	private static volatile BlueMoonSettings settings = BlueMoonSettings.DEFAULT;
	private static boolean enabled;
	private static volatile boolean active;

	private BlueMoonModule() {
	}

	/** The payload and settings exist whatever the config says, so client and server always match. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(BlueMoonPayload.TYPE, BlueMoonPayload.STREAM_CODEC);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BlueMoonSettingsLoader.ID, new BlueMoonSettingsLoader());
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
		LootTableEvents.MODIFY_DROPS.register(BlueMoonModule::modifyDrops);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommand(dispatcher));
	}

	static void setSettings(BlueMoonSettings value) {
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
			updated = new State(BlueMoonLogic.firstDay(settings, overworld.getRandom()::nextInt), updated.forcedDay());
		} else if (updated.nextDay() < today) {
			updated = new State(BlueMoonLogic.nextDay(updated.nextDay(), today, settings, overworld.getRandom()::nextInt), updated.forcedDay());
		}
		updated = new State(BlueMoonLogic.avoid(updated.nextDay(), BloodMoonModule.scheduledDay(overworld)), updated.forcedDay());
		if (updated.forcedDay() != 0 && updated.forcedDay() != today) {
			updated = new State(updated.nextDay(), 0);
		}
		if (!updated.equals(state)) {
			overworld.setAttached(STATE, updated);
		}
		// An op's blood moon wins over a blue moon on the same night.
		setActive(server, !BloodMoonModule.activeIn(overworld)
				&& BloodMoonLogic.active(updated.nextDay(), updated.forcedDay(), dayTime, settings.startTime()));
	}

	private static void setActive(MinecraftServer server, boolean value) {
		if (active == value) {
			return;
		}
		active = value;
		Eam.LOGGER.info("Blue moon {}", value ? "rises" : "sets");
		Component message = Component.translatable(value ? "event.eam.blue_moon.start" : "event.eam.blue_moon.end")
				.withStyle(ChatFormatting.AQUA);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sync(player);
			if (player.level().dimension() != Level.OVERWORLD) {
				continue;
			}
			player.sendSystemMessage(message);
			if (value) {
				player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("event.eam.blue_moon.title")
						.withStyle(ChatFormatting.AQUA)));
			}
		}
	}

	private static void sync(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, BlueMoonPayload.TYPE)) {
			ServerPlayNetworking.send(player, new BlueMoonPayload(active));
		}
	}

	/** True while a blue moon is up over {@code level}: only the overworld has one. */
	public static boolean activeIn(LevelReader level) {
		return enabled && active && level instanceof Level world && world.dimension() == Level.OVERWORLD;
	}

	// --- Hooks ---

	/** Whether {@code level} may spawn monsters now: none spawn under a blue moon. */
	public static boolean spawnEnemies(ServerLevel level, boolean vanilla) {
		return vanilla && !activeIn(level);
	}

	/** How many times faster crops at a position in {@code level} grow now. */
	public static double growthMultiplier(LevelReader level) {
		return activeIn(level) ? settings.growthMultiplier() : 1.0;
	}

	private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
		if (state == null || !state.is(HARVEST_BLOCKS) || !activeIn(context.getLevel())) {
			return;
		}
		double multiplier = settings.harvestMultiplier();
		for (ItemStack stack : drops) {
			if (stack.is(HARVEST_ITEMS)) {
				stack.setCount(BlueMoonLogic.harvestCount(stack.getCount(), multiplier, context.getRandom().nextDouble()));
			}
		}
		drops.removeIf(ItemStack::isEmpty);
	}

	// --- Command ---

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Eam.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("bluemoon")
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
			// Stopping tonight's blue moon moves the calendar on to the next one.
			int next = state.nextDay() == today ? BlueMoonLogic.nextDay(today, today, settings, overworld.getRandom()::nextInt) : state.nextDay();
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
		return active ? Component.translatable("commands.eam.blue_moon.active", today)
				: Component.translatable("commands.eam.blue_moon.next", today, next);
	}
}
