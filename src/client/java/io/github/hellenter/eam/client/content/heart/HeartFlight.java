package io.github.hellenter.eam.client.content.heart;

import com.mojang.math.Transformation;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.heart.HeartFlightPayload;
import io.github.hellenter.eam.content.heart.HeartInJarLogic;
import io.github.hellenter.eam.mixin.content.heart.DisplayInvoker;
import io.github.hellenter.eam.mixin.content.heart.ItemDisplayInvoker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Flies the vampire heart out of a squeezed jar. The heart is a small item display that exists only on this client and
 * is placed every tick from the player's current position and view, after the player has moved, so it keeps up with a
 * walking, sprinting or riding player and the whole flight stays on screen in first person.
 */
public final class HeartFlight {
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0xA8164E, 0.5F);
	private static final DustParticleOptions SPARK = new DustParticleOptions(0xC42466, 0.45F);
	private static final ItemParticleOption GLASS = new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE);

	private static final List<HeartFlight> FLIGHTS = new ArrayList<>();
	/** Ids of client-only hearts count down from here, far from the ids the server hands out. */
	private static int nextId = -0x4EA70000;

	private final ClientLevel level;
	private final Player player;
	private final double side;
	private final Display.ItemDisplay heart;
	private int age;

	private HeartFlight(ClientLevel level, Player player, double side, Display.ItemDisplay heart) {
		this.level = level;
		this.player = player;
		this.side = side;
		this.heart = heart;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(HeartFlightPayload.TYPE, (payload, context) -> start(context.client(), payload));
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
	}

	private static void start(Minecraft client, HeartFlightPayload payload) {
		ClientLevel level = client.level;
		if (level == null || !(level.getEntity(payload.playerId()) instanceof Player player)) {
			return;
		}
		double side = payload.rightArm() ? 1.0 : -1.0;
		Vec3 handPos = inView(player, side, HeartInJarLogic.HAND);
		burst(level, GLASS, handPos, 16, 0.06, 0.12);
		burst(level, TRAIL, handPos, 6, 0.05, 0.0);

		Display.ItemDisplay heart = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (heart == null) {
			return;
		}
		heart.setId(nextId--);
		((ItemDisplayInvoker) heart).eam$setItemStack(new ItemStack(EamItems.VAMPIRE_HEART));
		DisplayInvoker display = (DisplayInvoker) heart;
		float scale = HeartInJarLogic.HEART_SCALE;
		display.eam$setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale),
				new Quaternionf()));
		display.eam$setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.eam$setBrightnessOverride(Brightness.FULL_BRIGHT);
		heart.snapTo(handPos);
		level.addEntity(heart);
		FLIGHTS.add(new HeartFlight(level, player, side, heart));
	}

	/** Runs after every entity has ticked, so the heart is placed from where the player is this tick. */
	private static void tick() {
		Iterator<HeartFlight> iterator = FLIGHTS.iterator();
		while (iterator.hasNext()) {
			HeartFlight flight = iterator.next();
			if (!flight.step()) {
				flight.heart.discard();
				iterator.remove();
			}
		}
	}

	/** One tick; false once it is over. */
	private boolean step() {
		if (Minecraft.getInstance().level != level || player.isRemoved() || heart.isRemoved()) {
			return false;
		}
		age++;
		Vec3 chest = player.position().add(0, player.getBbHeight() * 0.62, 0);
		double[] point = HeartInJarLogic.bezier(HeartInJarLogic.flightProgress(age),
				array(inView(player, side, HeartInJarLogic.HAND)),
				array(inView(player, side, HeartInJarLogic.RISE)),
				array(inView(player, side, HeartInJarLogic.ACROSS)),
				array(chest));
		Vec3 pos = new Vec3(point[0], point[1], point[2]);
		heart.setPos(pos);
		burst(level, TRAIL, pos, 2, 0.03, 0.0);
		if (age < HeartInJarLogic.FLIGHT_TICKS) {
			return true;
		}
		burst(level, SPARK, chest, 24, 0.3, 0.0);
		level.playLocalSound(chest.x, chest.y, chest.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.2F, false);
		return false;
	}

	/** The point {@code local} of the flight path (side, up, forward from the eyes) in the world, as the player looks now. */
	private static Vec3 inView(Player player, double side, double[] local) {
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

	/** {@code count} particles around {@code pos}, spread by {@code spread} and flying apart at {@code speed}. */
	private static void burst(ClientLevel level, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < count; i++) {
			level.addParticle(particle,
					pos.x + random.nextGaussian() * spread, pos.y + random.nextGaussian() * spread, pos.z + random.nextGaussian() * spread,
					random.nextGaussian() * speed, random.nextGaussian() * speed, random.nextGaussian() * speed);
		}
	}
}
