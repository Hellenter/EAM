package io.github.hellenter.eam.content.heart;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The heart in a jar: heals and buffs at once, then plays out the squeeze. The jar bursts into glass shards in the
 * hand, and the vampire heart flies out of it, rises, swings across the player's view and drops into the chest, where
 * it scatters into small purple-red sparks. The server only tells the clients; each client flies the heart from the
 * player's position it renders, so the heart stays in front of a walking, sprinting or riding player.
 */
public final class HeartInJarModule {
	private HeartInJarModule() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(HeartFlightPayload.TYPE, HeartFlightPayload.STREAM_CODEC);
	}

	/** Applies the jar to {@code player} and starts its animation. */
	public static void use(ServerLevel level, ServerPlayer player, InteractionHand hand) {
		player.heal(HeartInJarLogic.HEAL);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HeartInJarLogic.REGENERATION_TICKS,
				HeartInJarLogic.REGENERATION_AMPLIFIER));
		player.addEffect(new MobEffectInstance(MobEffects.SATURATION, HeartInJarLogic.SATURATION_TICKS,
				HeartInJarLogic.SATURATION_AMPLIFIER));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);

		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		HeartFlightPayload payload = new HeartFlightPayload(player.getId(), arm == HumanoidArm.RIGHT);
		send(player, payload);
		PlayerLookup.tracking(player).forEach(viewer -> send(viewer, payload));
	}

	private static void send(ServerPlayer player, HeartFlightPayload payload) {
		if (ServerPlayNetworking.canSend(player, HeartFlightPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
