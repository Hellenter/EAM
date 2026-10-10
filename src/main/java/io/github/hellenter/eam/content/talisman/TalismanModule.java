package io.github.hellenter.eam.content.talisman;

import io.github.hellenter.eam.mixin.content.talisman.PortalShapeAccessor;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The black talisman opens nether portals. In the overworld nothing else does: fire in a frame stays plain fire. The
 * activation plays out over two seconds: the talisman flies to the frame trailing purple and blue sparks, the frame
 * lights up, and it flies back to the hand one durability poorer.
 */
public final class TalismanModule {
	private static final DustParticleOptions PURPLE = new DustParticleOptions(0x9B4DFF, 0.9F);
	private static final DustParticleOptions BLUE = new DustParticleOptions(0x4D6BFF, 0.8F);
	private static final DustParticleOptions PALE = new DustParticleOptions(0xD9B8FF, 0.7F);

	private static boolean restricted;
	private static final List<Activation> ACTIVATIONS = new ArrayList<>();

	private TalismanModule() {
	}

	/** Always: the client must know the payload whatever the server config says. */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TalismanActivationPayload.TYPE, TalismanActivationPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TalismanLitPayload.TYPE, TalismanLitPayload.STREAM_CODEC);
		// Before the levels tick, so the portal blocks go out to clients in the same tick as the flash and the glow.
		ServerTickEvents.START_SERVER_TICK.register(server -> tick());
	}

	/** Turns on the overworld rule: fire alone no longer lights a portal there. */
	public static void init() {
		restricted = true;
	}

	public static boolean fireLightsPortal(Level level) {
		return TalismanLogic.fireLightsPortal(level.dimension() == Level.OVERWORLD, restricted);
	}

	/** Whether the talisman works here: wherever vanilla fire could light a portal. */
	public static boolean canOpenPortalIn(Level level) {
		return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER;
	}

	/** An empty, complete frame around {@code pos}, trying the frame across the player's view first; or nothing. */
	public static Optional<PortalShape> findFrame(Level level, BlockPos pos, Direction facing) {
		return PortalShape.findEmptyPortalShape(level, pos, facing.getCounterClockWise().getAxis());
	}

	/** Starts the activation; the portal lights on {@link TalismanLogic#IGNITE_TICK}. */
	public static void start(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack talisman, PortalShape shape) {
		PortalShapeAccessor frame = (PortalShapeAccessor) shape;
		for (Activation activation : ACTIVATIONS) {
			if (activation.level == level && activation.bottomLeft.equals(frame.eam$bottomLeft())) {
				return;
			}
		}
		Activation activation = new Activation(level, player, hand, talisman, frame.eam$bottomLeft(), frame.eam$rightDir(),
				frame.eam$width(), frame.eam$height());
		ACTIVATIONS.add(activation);
		if (ServerPlayNetworking.canSend(player, TalismanActivationPayload.TYPE)) {
			ServerPlayNetworking.send(player, TalismanActivationPayload.INSTANCE);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 0.6F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
	}

	private static void tick() {
		Iterator<Activation> iterator = ACTIVATIONS.iterator();
		while (iterator.hasNext()) {
			Activation activation = iterator.next();
			if (!activation.tick()) {
				iterator.remove();
			}
		}
	}

	private static final class Activation {
		private final ServerLevel level;
		private final ServerPlayer player;
		private final InteractionHand hand;
		private final ItemStack talisman;
		private final BlockPos bottomLeft;
		private final Direction rightDir;
		private final int width;
		private final int height;
		private final Vec3 center;
		private int age;
		private boolean lit;

		private Activation(ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack talisman, BlockPos bottomLeft,
				Direction rightDir, int width, int height) {
			this.level = level;
			this.player = player;
			this.hand = hand;
			this.talisman = talisman;
			this.bottomLeft = bottomLeft;
			this.rightDir = rightDir;
			this.width = width;
			this.height = height;
			this.center = Vec3.atLowerCornerOf(bottomLeft)
					.add(Vec3.atLowerCornerOf(rightDir.getUnitVec3i()).scale((width - 1) / 2.0))
					.add(0.5, height / 2.0, 0.5);
		}

		/** One tick; false once it is over. */
		private boolean tick() {
			age++;
			if (age > TalismanLogic.ACTIVATION_TICKS) {
				return false;
			}
			trail();
			frame();
			if (age == TalismanLogic.IGNITE_TICK) {
				ignite();
			}
			if (age == TalismanLogic.ACTIVATION_TICKS) {
				returnToHand();
				return false;
			}
			return true;
		}

		/** The talisman's flight: a twisting purple and blue streak between the hand and the portal. */
		private void trail() {
			// From a little in front of the player, so the sparks do not fill their own screen.
			Vec3 hand = player.isRemoved() || player.level() != level ? center
					: player.getEyePosition().add(player.getLookAngle().scale(1.2)).add(0, -0.35, 0);
			Vec3 path = center.subtract(hand);
			Vec3 head = hand.add(path.scale(TalismanLogic.trailProgress(age)));
			Vec3 side = path.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : path.cross(new Vec3(0, 1, 0)).normalize();
			Vec3 up = side.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : side.cross(path).normalize();
			for (int i = 0; i < 2; i++) {
				double angle = age * 0.9 + i * Math.PI;
				Vec3 offset = side.scale(Math.cos(angle) * 0.25).add(up.scale(Math.sin(angle) * 0.25));
				level.sendParticles(i == 0 ? PURPLE : BLUE, head.x + offset.x, head.y + offset.y, head.z + offset.z, 3, 0.04, 0.04, 0.04, 0.0);
			}
			level.sendParticles(ParticleTypes.WITCH, head.x, head.y, head.z, 1 + age % 2, 0.1, 0.1, 0.1, 0.0);
		}

		/** The frame and the space inside it sparkle, strongest at the moment it lights. */
		private void frame() {
			double glow = TalismanLogic.frameGlow(age);
			int sparks = (int) Math.round(1.5 * glow * (width + height));
			for (int i = 0; i < sparks; i++) {
				// Inside the frame.
				int x = level.getRandom().nextInt(width);
				int y = level.getRandom().nextInt(height);
				BlockPos inside = bottomLeft.relative(rightDir, x).above(y);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, inside.getX() + 0.5, inside.getY() + 0.5, inside.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.02);
				// On the frame blocks.
				BlockPos edge = frameBlock(level.getRandom().nextInt(2 * (width + height) + 4));
				level.sendParticles(level.getRandom().nextBoolean() ? PURPLE : BLUE, edge.getX() + 0.5, edge.getY() + 0.5, edge.getZ() + 0.5,
						1, 0.35, 0.35, 0.35, 0.0);
			}
		}

		/** The n-th block of the frame ring, going round it. */
		private BlockPos frameBlock(int n) {
			int ring = 2 * (width + height) + 4;
			n = Math.floorMod(n, ring);
			BlockPos corner = bottomLeft.relative(rightDir, -1).below();
			if (n < width + 2) {
				return corner.relative(rightDir, n);
			}
			n -= width + 2;
			if (n < height) {
				return corner.relative(rightDir, width + 1).above(n + 1);
			}
			n -= height;
			if (n < width + 2) {
				return corner.relative(rightDir, width + 1 - n).above(height + 1);
			}
			n -= width + 2;
			return corner.above(height - n);
		}

		private void ignite() {
			Optional<PortalShape> shape = PortalShape.findEmptyPortalShape(level, bottomLeft, rightDir.getAxis());
			if (shape.isEmpty() || !((PortalShapeAccessor) shape.get()).eam$bottomLeft().equals(bottomLeft)) {
				// The frame was broken or filled while the talisman flew: it fizzles.
				level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 20, width * 0.3, height * 0.3, 0.3, 0.02);
				level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
				return;
			}
			shape.get().createPortalBlocks(level);
			lit = true;
			if (!player.isRemoved() && ServerPlayNetworking.canSend(player, TalismanLitPayload.TYPE)) {
				ServerPlayNetworking.send(player, new TalismanLitPayload(bottomLeft));
			}
			level.gameEvent(player, GameEvent.BLOCK_PLACE, BlockPos.containing(center));
			level.sendParticles(PURPLE, center.x, center.y, center.z, 60, width * 0.35, height * 0.35, 0.35, 0.0);
			level.sendParticles(BLUE, center.x, center.y, center.z, 45, width * 0.35, height * 0.35, 0.35, 0.0);
			level.sendParticles(PALE, center.x, center.y, center.z, 30, width * 0.3, height * 0.3, 0.3, 0.0);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 90, width * 0.3, height * 0.3, 0.3, 0.15);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.8F);
		}

		/** Back in the hand: it pays for the portal it lit. */
		private void returnToHand() {
			if (!lit || player.isRemoved() || talisman.isEmpty()) {
				return;
			}
			// Hurt it where it is now; if it left the player's inventory, the portal was free.
			if (player.getItemInHand(hand) == talisman) {
				talisman.hurtAndBreak(TalismanLogic.DURABILITY_COST, player, hand);
			} else if (player.getInventory().contains(stack -> stack == talisman)) {
				talisman.hurtAndBreak(TalismanLogic.DURABILITY_COST, level, player, item -> player.onEquippedItemBroken(item, hand.asEquipmentSlot()));
			}
		}
	}
}
