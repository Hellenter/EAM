package io.github.hellenter.eam.world.structures;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Until a player of the world earns {@link #UNLOCK_ADVANCEMENT}, every village that generates is abandoned: its start
 * piece is drawn only from the abandoned (zombie) entries of its start pool, and the rest of the village follows that
 * piece. The unlock is saved with the world and only affects chunks generated after it.
 */
public final class VillageModule {
	/** Village start pools that hold both normal and abandoned start pieces. */
	public static final TagKey<StructureTemplatePool> GATED_VILLAGE_STARTS =
			TagKey.create(Registries.TEMPLATE_POOL, Eam.id("gated_village_starts"));
	/** Earning this advancement lets normal villages generate. The advancement itself comes later. */
	public static final Identifier UNLOCK_ADVANCEMENT = Eam.id("iron_age");

	/** World flag, stored on the overworld. */
	public static final AttachmentType<Boolean> UNLOCKED = AttachmentRegistry.create(Eam.id("villages_unlocked"),
			builder -> builder.persistent(Codec.BOOL));

	private static volatile boolean enabled;
	/** Read from worldgen threads, so cached here instead of looked up on the level. */
	private static volatile boolean unlocked;

	private VillageModule() {
	}

	/** The attachment type must be known even with the module off, so saved worlds load cleanly. */
	public static void register() {
	}

	public static void init() {
		enabled = true;
		ServerLifecycleEvents.SERVER_STARTED.register(server -> unlocked = server.overworld().getAttachedOrElse(UNLOCKED, false));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> unlocked = false);
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> registerCommand(dispatcher));
	}

	/** The start piece of a jigsaw structure: an abandoned one while villages are locked. */
	public static StructurePoolElement startElement(Holder<StructureTemplatePool> startPool, StructureTemplatePool pool,
			RandomSource random, Supplier<StructurePoolElement> original) {
		if (!enabled || unlocked || !startPool.is(GATED_VILLAGE_STARTS)) return original.get();
		return VillageLogic.pickAbandoned(pool.getTemplates(), VillageModule::templateOf, random::nextInt)
				.orElseGet(original);
	}

	public static void onAdvancementDone(ServerPlayer player, AdvancementHolder advancement) {
		if (enabled && !unlocked && advancement.id().equals(UNLOCK_ADVANCEMENT)) {
			setUnlocked(player.level().getServer(), true);
		}
	}

	public static void setUnlocked(MinecraftServer server, boolean value) {
		unlocked = value;
		server.overworld().setAttached(UNLOCKED, value);
		Eam.LOGGER.info("Normal villages {}", value ? "unlocked" : "locked");
	}

	private static Optional<Identifier> templateOf(StructurePoolElement element) {
		return element instanceof SinglePoolElement single ? Optional.of(single.getTemplateLocation()) : Optional.empty();
	}

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Eam.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("villages")
						.executes(context -> {
							context.getSource().sendSuccess(() -> status(), false);
							return unlocked ? 1 : 0;
						})
						.then(Commands.literal("unlock").executes(context -> set(context.getSource(), true)))
						.then(Commands.literal("lock").executes(context -> set(context.getSource(), false)))));
	}

	private static int set(CommandSourceStack source, boolean value) {
		setUnlocked(source.getServer(), value);
		source.sendSuccess(VillageModule::status, true);
		return 1;
	}

	private static Component status() {
		return Component.translatable(unlocked ? "commands.eam.villages.unlocked" : "commands.eam.villages.locked");
	}
}
