package io.github.hellenter.eam.content.heart;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The heart in a jar: heals and buffs at once, then plays out the squeeze. The jar bursts into glass shards in the
 * hand, and the glowing heart flies into the player's chest, where it scatters into small red sparks.
 */
public final class HeartInJarModule {
	private static final DustParticleOptions HEART_CORE = new DustParticleOptions(0x8E0A1E, 1.4F);
	private static final DustParticleOptions HEART_GLOW = new DustParticleOptions(0xFF3B3B, 0.9F);
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xE01E2E, 0.5F);
	private static final ItemParticleOption GLASS = new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE);

	private static final List<Flight> FLIGHTS = new ArrayList<>();

	private HeartInJarModule() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
	}

	/** Applies the jar to {@code player} and starts its animation. */
	public static void use(ServerLevel level, ServerPlayer player, InteractionHand hand) {
		player.heal(HeartInJarLogic.HEAL);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HeartInJarLogic.REGENERATION_TICKS,
				HeartInJarLogic.REGENERATION_AMPLIFIER));
		player.addEffect(new MobEffectInstance(MobEffects.SATURATION, HeartInJarLogic.SATURATION_TICKS,
				HeartInJarLogic.SATURATION_AMPLIFIER));

		Vec3 handPos = handPosition(player, hand);
		level.sendParticles(GLASS, handPos.x, handPos.y, handPos.z, 16, 0.06, 0.06, 0.06, 0.12);
		level.sendParticles(HEART_GLOW, handPos.x, handPos.y, handPos.z, 6, 0.05, 0.05, 0.05, 0.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);
		FLIGHTS.add(new Flight(level, player, handPos));
	}

	/** A point in front of the hand that holds the jar, a little below the eyes. */
	private static Vec3 handPosition(ServerPlayer player, InteractionHand hand) {
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		Vec3 look = player.getLookAngle();
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
		double side = arm == HumanoidArm.RIGHT ? 0.35 : -0.35;
		return player.getEyePosition().add(look.scale(0.7)).add(right.scale(side)).add(0, -0.35, 0);
	}

	private static void tick() {
		Iterator<Flight> iterator = FLIGHTS.iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().tick()) {
				iterator.remove();
			}
		}
	}

	private static final class Flight {
		private final ServerLevel level;
		private final ServerPlayer player;
		private final Vec3 start;
		private int age;

		private Flight(ServerLevel level, ServerPlayer player, Vec3 start) {
			this.level = level;
			this.player = player;
			this.start = start;
		}

		/** One tick; false once it is over. */
		private boolean tick() {
			if (player.isRemoved() || player.level() != level) {
				return false;
			}
			age++;
			Vec3 chest = player.position().add(0, player.getBbHeight() * 0.6, 0);
			Vec3 heart = start.lerp(chest, HeartInJarLogic.flightProgress(age));
			level.sendParticles(HEART_CORE, heart.x, heart.y, heart.z, 3, 0.04, 0.04, 0.04, 0.0);
			level.sendParticles(HEART_GLOW, heart.x, heart.y, heart.z, 3, 0.09, 0.09, 0.09, 0.0);
			if (age < HeartInJarLogic.FLIGHT_TICKS) {
				return true;
			}
			level.sendParticles(SPARK, chest.x, chest.y, chest.z, 24, 0.3, 0.35, 0.3, 0.0);
			level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.2F);
			return false;
		}
	}
}
