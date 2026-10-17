package io.github.hellenter.eam.mobs.vanilla.noise;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Noisy players are heard: digging, sprinting, fighting and banging doors let mobs in {@link #HEARERS} find a player
 * through walls and out of sight, for a few seconds after the noise. A quiet, sneaking player can slip past. Numbers
 * are in {@code data/eam/mobs/noise.json}.
 */
public final class NoiseModule {
	/** Mobs that hear noisy players. */
	public static final TagKey<EntityType<?>> HEARERS = TagKey.create(Registries.ENTITY_TYPE, Eam.id("hears_noise"));
	/** Blocks that make a noise when a player opens or closes them. */
	public static final TagKey<Block> NOISY_WHEN_USED = TagKey.create(Registries.BLOCK, Eam.id("noisy_when_used"));
	/** The player's current noise; it lasts seconds, so it is neither saved nor synced. */
	public static final AttachmentType<NoiseLogic.Noise> NOISE = AttachmentRegistry.create(Eam.id("noise"));

	/** Same priority as the vanilla "attack the nearest player" target goal. */
	private static final int GOAL_PRIORITY = 2;
	private static final int SPRINT_INTERVAL = 5;

	private static volatile NoiseSettings settings = NoiseSettings.DEFAULT;
	private static boolean enabled;

	private NoiseModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(NoiseSettingsLoader.ID, new NoiseSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob) {
				addGoal(mob);
			}
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!level.isClientSide()) dig(player, level.getBlockState(pos).getDestroySpeed(level, pos));
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!level.isClientSide()) dig(player, state.getDestroySpeed(level, pos));
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide()) {
				BlockState state = level.getBlockState(hit.getBlockPos());
				if (state.is(NOISY_WHEN_USED)) noise(player, sneaking(player, settings.door()));
			}
			return InteractionResult.PASS;
		});
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!level.isClientSide()) noise(player, sneaking(player, settings.fight()));
			return InteractionResult.PASS;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			// A player hit by a mob, or a player whose arrow hit something.
			if (entity instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity) {
				noise(player, settings.fight());
			}
			if (source.getEntity() instanceof ServerPlayer player && player != entity) {
				noise(player, sneaking(player, settings.fight()));
			}
		});
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % SPRINT_INTERVAL == 0) {
				for (ServerPlayer player : level.players()) {
					if (player.isSprinting()) noise(player, settings.sprint());
				}
			}
		});
	}

	/**
	 * Gives {@code mob} its hearing if it is a {@link #HEARERS} mob. The block breaking module calls this before it
	 * collects the mob's target goals, so a breaker keeps a heard target while it digs toward it.
	 */
	public static void addGoal(Mob mob) {
		if (!enabled || !mob.is(HEARERS)) {
			return;
		}
		GoalSelector targets = ((MobAccessor) mob).eam$targetSelector();
		for (WrappedGoal wrapped : targets.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof HearNoiseGoal) {
				return; // Already added: the same entity loaded again.
			}
		}
		targets.addGoal(GOAL_PRIORITY, new HearNoiseGoal(mob));
	}

	/** Blocks within which {@code player} is heard right now. */
	public static float radius(Player player) {
		return NoiseLogic.radius(player.getAttached(NOISE), player.level().getGameTime());
	}

	static NoiseSettings settings() {
		return settings;
	}

	static void setSettings(NoiseSettings value) {
		settings = value;
	}

	private static void dig(Player player, float hardness) {
		// Unbreakable blocks report -1: hitting bedrock is as loud as hitting the hardest block.
		float radius = hardness < 0.0F ? settings.digMax() : NoiseLogic.dig(settings, hardness);
		noise(player, sneaking(player, radius));
	}

	private static float sneaking(Player player, float radius) {
		return NoiseLogic.sneak(radius, player.isShiftKeyDown(), settings.sneakMultiplier());
	}

	private static void noise(Player player, float radius) {
		if (player.isSpectator() || player.isCreative()) {
			return;
		}
		NoiseLogic.Noise current = player.getAttached(NOISE);
		NoiseLogic.Noise next = NoiseLogic.add(current, radius, player.level().getGameTime(), settings.holdTicks());
		if (next != current) {
			player.setAttached(NOISE, next);
		}
	}
}
