package io.github.hellenter.eam.mobs.vanilla.villager;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import io.github.hellenter.eam.Eam;

import java.util.ArrayList;
import java.util.List;

/**
 * The blocks of a building a blueprint is of, the same on server and client: the server reads them from the structure
 * template and sends them to a client holding the blueprint, which shows them as a ghost. The bottom layer is the
 * foundation: it takes the place of the ground and keeps the ground where the template has air.
 *
 * @param blocks every block of the template but structure voids, air included
 * @param front  the side the building faces in the unturned template: its entrance, toward the street
 */
public record BlueprintTemplate(Identifier id, int sizeX, int sizeY, int sizeZ, List<Entry> blocks, Direction front) {
	/** What the foundation may take the place of besides air and plants: dirt, sand, stone and the like. */
	public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Eam.id("blueprint_ground"));
	/** How far from a player the near side of a blueprint's building stands. */
	public static final double DISTANCE = 4.0;
	/** How high a blueprint rises off the ground at most as its player looks up. */
	public static final int MAX_LIFT = 16;
	/** How far above and below a player's feet the ground under a blueprint is looked for. */
	private static final int GROUND_UP = 4;
	private static final int GROUND_DOWN = 8;

	public static final StreamCodec<ByteBuf, BlueprintTemplate> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, BlueprintTemplate::id,
			ByteBufCodecs.VAR_INT, BlueprintTemplate::sizeX,
			ByteBufCodecs.VAR_INT, BlueprintTemplate::sizeY,
			ByteBufCodecs.VAR_INT, BlueprintTemplate::sizeZ,
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), BlueprintTemplate::blocks,
			Direction.STREAM_CODEC, BlueprintTemplate::front,
			BlueprintTemplate::new);

	/** One block, at its place in the unturned template. */
	public record Entry(int x, int y, int z, BlockState state) {
		public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Entry::x,
				ByteBufCodecs.VAR_INT, Entry::y,
				ByteBufCodecs.VAR_INT, Entry::z,
				ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Entry::state,
				Entry::new);
	}

	/**
	 * One block where it goes in the world.
	 *
	 * @param ground whether it is of the foundation
	 */
	public record Placed(BlockPos pos, BlockState state, boolean ground) {
	}

	/** The rotation of {@code turns} clockwise quarter turns. */
	public static Rotation rotation(int turns) {
		return switch (Math.floorMod(turns, 4)) {
			case 1 -> Rotation.CLOCKWISE_90;
			case 2 -> Rotation.CLOCKWISE_180;
			case 3 -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	/** The corner the template is placed from to stand centred on {@code look}, turned {@code turns} times. */
	public BlockPos origin(BlockPos look, int turns) {
		int[] size = BlueprintLogic.turnedSize(sizeX, sizeZ, turns);
		int[] origin = BlueprintLogic.origin(look.getX(), look.getY(), look.getZ(), size[0], size[1]);
		return new BlockPos(origin[0], origin[1], origin[2]);
	}

	/** The side the building faces once turned {@code turns} times. */
	public Direction front(int turns) {
		return rotation(turns).rotate(front);
	}

	/** The box the building fills, placed from {@code origin}. */
	public AABB box(BlockPos origin, int turns) {
		int[] size = BlueprintLogic.turnedSize(sizeX, sizeZ, turns);
		return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size[0], origin.getY() + sizeY, origin.getZ() + size[1]);
	}

	/** Every block where it goes, turned; foundation air is left out, so the ground stays there. */
	public List<Placed> placed(BlockPos origin, int turns) {
		Rotation rotation = rotation(turns);
		List<Placed> placed = new ArrayList<>(blocks.size());
		for (Entry entry : blocks) {
			boolean ground = entry.y() == 0;
			if (ground && entry.state().isAir()) continue;
			int[] xz = BlueprintLogic.turn(entry.x(), entry.z(), sizeX, sizeZ, turns);
			placed.add(new Placed(origin.offset(xz[0], entry.y(), xz[1]), entry.state().rotate(rotation), ground));
		}
		return placed;
	}

	/** Whether what is in the world at the block's place keeps it from being built: anything but air and plants, and for the foundation the ground. */
	public static boolean blocked(BlockGetter level, Placed block) {
		BlockState there = level.getBlockState(block.pos());
		if (there.isAir() || there.canBeReplaced() || there.equals(block.state())) return false;
		return !block.ground() || !there.is(GROUND);
	}

	/**
	 * The corner a player's blueprint is placed from, turned {@code turns} times: the building straight ahead of him,
	 * its near side {@link #DISTANCE} blocks away, standing on the ground there, or above it as he looks up.
	 */
	public BlockPos placement(Player player, int turns, float partialTicks) {
		int[] size = BlueprintLogic.turnedSize(sizeX, sizeZ, turns);
		int[] ahead = BlueprintLogic.ahead(player.getX(), player.getZ(), player.getViewYRot(partialTicks),
				player.getViewXRot(partialTicks), size[0], size[1], DISTANCE, MAX_LIFT);
		BlockPos ground = ground(player.level(), new BlockPos(ahead[0], player.getBlockY(), ahead[1]));
		return origin(ground.above(ahead[2]), turns);
	}

	/** The ground block under {@code column}: the highest one near its height that a plant or air does not fill. */
	private static BlockPos ground(BlockGetter level, BlockPos column) {
		BlockPos.MutableBlockPos pos = column.mutable().move(0, GROUND_UP, 0);
		for (int i = 0; i <= GROUND_UP + GROUND_DOWN; i++, pos.move(0, -1, 0)) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && !state.canBeReplaced()) return pos.immutable();
		}
		return column.below();
	}
}
