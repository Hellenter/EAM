package io.github.hellenter.eam.content.heart;

import com.mojang.math.Transformation;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mixin.content.heart.DisplayInvoker;
import io.github.hellenter.eam.mixin.content.heart.ItemDisplayInvoker;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The heart in a jar: heals and buffs at once, then plays out the squeeze. The jar bursts into glass shards in the
 * hand, and the vampire heart flies out of it, rises, swings across the player's view and drops into the chest, where
 * it scatters into small purple-red sparks. The flying heart is a small item display that follows the player's view,
 * so the whole flight stays on screen in first person.
 */
public final class HeartInJarModule {
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0xA8164E, 0.5F);
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xC42466, 0.45F);
	private static final ItemParticleOption GLASS = new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE);

	private static final List<Flight> FLIGHTS = new ArrayList<>();

	private HeartInJarModule() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		// A flying heart must never be saved with the world.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			FLIGHTS.forEach(Flight::end);
			FLIGHTS.clear();
		});
	}

	/** Applies the jar to {@code player} and starts its animation. */
	public static void use(ServerLevel level, ServerPlayer player, InteractionHand hand) {
		player.heal(HeartInJarLogic.HEAL);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HeartInJarLogic.REGENERATION_TICKS,
				HeartInJarLogic.REGENERATION_AMPLIFIER));
		player.addEffect(new MobEffectInstance(MobEffects.SATURATION, HeartInJarLogic.SATURATION_TICKS,
				HeartInJarLogic.SATURATION_AMPLIFIER));

		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		double side = arm == HumanoidArm.RIGHT ? 1.0 : -1.0;
		Vec3 handPos = inView(player, side, HeartInJarLogic.HAND);
		level.sendParticles(GLASS, handPos.x, handPos.y, handPos.z, 16, 0.06, 0.06, 0.06, 0.12);
		level.sendParticles(TRAIL, handPos.x, handPos.y, handPos.z, 6, 0.05, 0.05, 0.05, 0.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);

		Display.ItemDisplay heart = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (heart == null) {
			return;
		}
		((ItemDisplayInvoker) heart).eam$setItemStack(new ItemStack(EamItems.VAMPIRE_HEART));
		DisplayInvoker display = (DisplayInvoker) heart;
		float scale = HeartInJarLogic.HEART_SCALE;
		display.eam$setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale),
				new Quaternionf()));
		display.eam$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.eam$setBrightnessOverride(Brightness.FULL_BRIGHT);
		display.eam$setPosRotInterpolationDuration(2);
		heart.snapTo(handPos);
		level.addFreshEntity(heart);
		FLIGHTS.add(new Flight(level, player, side, heart));
	}

	/** The point {@code local} of the flight path (side, up, forward from the eyes) in the world, as the player looks now. */
	private static Vec3 inView(ServerPlayer player, double side, double[] local) {
		Vec3 look = player.getLookAngle();
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
		Vec3 up = right.cross(look).normalize();
		return player.getEyePosition()
				.add(right.scale(local[0] * side))
				.add(up.scale(local[1]))
				.add(look.scale(local[2]));
	}

	private static double[] array(Vec3 v) {
		return new double[] {v.x, v.y, v.z};
	}

	private static void tick() {
		Iterator<Flight> iterator = FLIGHTS.iterator();
		while (iterator.hasNext()) {
			Flight flight = iterator.next();
			if (!flight.tick()) {
				flight.end();
				iterator.remove();
			}
		}
	}

	private static final class Flight {
		private final ServerLevel level;
		private final ServerPlayer player;
		private final double side;
		private final Display.ItemDisplay heart;
		private int age;

		private Flight(ServerLevel level, ServerPlayer player, double side, Display.ItemDisplay heart) {
			this.level = level;
			this.player = player;
			this.side = side;
			this.heart = heart;
		}

		/** One tick; false once it is over. */
		private boolean tick() {
			if (player.isRemoved() || player.level() != level || heart.isRemoved()) {
				return false;
			}
			age++;
			Vec3 chest = player.position().add(0, player.getBbHeight() * 0.62, 0);
			double[] point = HeartInJarLogic.bezier(HeartInJarLogic.flightProgress(age),
					array(inView(player, side, HeartInJarLogic.HAND)),
					array(inView(player, side, HeartInJarLogic.RISE)),
					array(inView(player, side, HeartInJarLogic.ACROSS)),
					array(chest));
			heart.setPos(point[0], point[1], point[2]);
			level.sendParticles(TRAIL, point[0], point[1], point[2], 2, 0.03, 0.03, 0.03, 0.0);
			if (age < HeartInJarLogic.FLIGHT_TICKS) {
				return true;
			}
			level.sendParticles(SPARK, chest.x, chest.y, chest.z, 24, 0.3, 0.35, 0.3, 0.0);
			level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.2F);
			return false;
		}

		private void end() {
			heart.discard();
		}
	}
}
