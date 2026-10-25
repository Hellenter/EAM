package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamParticles;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Boiling water in a cauldron: a water cauldron standing on a heat source boils after a while, and a bottle, a bowl or a
 * gulp taken from it is safe to drink. While it heats the cauldron ticks itself every {@link #STEP} ticks; boiled water
 * over the fire keeps steaming. Water poured in starts the boil over.
 */
public final class CauldronBoiling {
	/** Blocks that heat a water cauldron standing right on top of them; one with a {@code lit} property only while lit. */
	public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, Eam.id("cauldron_heat_sources"));

	/** Heating cauldrons in a chunk and how long each has been on the heat. */
	public static final AttachmentType<BoilingCauldrons> CAULDRONS = AttachmentRegistry.create(Eam.id("boiling_cauldrons"),
			builder -> builder.persistent(BoilingCauldrons.CODEC));

	/** Ticks between two heat checks of a cauldron. */
	static final int STEP = 20;

	private static boolean enabled;

	private CauldronBoiling() {
	}

	/** Loads the class so the attachment is registered whatever the config says. */
	static void register() {
	}

	static void init() {
		enabled = true;
	}

	/** Whether the water cauldron at {@code pos} holds boiled water. Only the server knows; false on the client. */
	public static boolean isBoiled(Level level, BlockPos pos) {
		BoilingCauldrons cauldrons = level.getChunk(pos).getAttached(CAULDRONS);
		return enabled && cauldrons != null && cauldrons.heat(pos.asLong()) >= ThirstModule.settings().cauldron().boilTime();
	}

	/**
	 * Every server-side block state change: water poured into a cauldron starts its boil over, and a water cauldron
	 * that now stands on a heat source, or a heat source that now has one on top, starts heating.
	 */
	public static void onChanged(Level level, BlockPos pos, BlockState state, BlockState oldState) {
		if (!enabled || !(level instanceof ServerLevel serverLevel)) return;
		int newLevel = waterLevel(state);
		int oldLevel = waterLevel(oldState);
		if (newLevel != 0 || oldLevel != 0) {
			int heat = heat(level, pos);
			setHeat(level, pos, ThirstLogic.boilHeatAfter(heat, oldLevel, newLevel));
			if (newLevel != 0) startHeating(serverLevel, pos);
		}
		if (state.is(HEAT_SOURCES)) {
			startHeating(serverLevel, pos.above());
		}
	}

	/** A scheduled tick of a water cauldron: one step of heating, or cooling off once the heat is gone. */
	public static void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!enabled || waterLevel(state) == 0) return;
		int boilTime = ThirstModule.settings().cauldron().boilTime();
		boolean heated = isHeated(level, pos);
		int heat = heat(level, pos);
		int next = ThirstLogic.heatAfter(heat, heated, STEP, boilTime);
		if (next != heat) setHeat(level, pos, next);
		if (!heated) return;

		if (next >= boilTime) {
			if (heat < boilTime) {
				level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.4F);
			} else if (random.nextInt(4) == 0) {
				level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4F, 0.8F + random.nextFloat() * 0.3F);
			}
			double surface = pos.getY() + 0.375 + 0.1875 * state.getValue(BlockStateProperties.LEVEL_CAULDRON);
			level.sendParticles(EamParticles.CAULDRON_STEAM, pos.getX() + 0.5, surface, pos.getZ() + 0.5, 3, 0.22, 0.02, 0.22, 0.01);
		}
		level.scheduleTick(pos, state.getBlock(), STEP);
	}

	private static void startHeating(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (waterLevel(state) != 0 && isHeated(level, pos)) {
			level.scheduleTick(pos, state.getBlock(), STEP);
		}
	}

	private static boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(HEAT_SOURCES) && below.getValueOrElse(BlockStateProperties.LIT, true);
	}

	private static int heat(Level level, BlockPos pos) {
		BoilingCauldrons cauldrons = level.getChunk(pos).getAttached(CAULDRONS);
		return cauldrons == null ? 0 : cauldrons.heat(pos.asLong());
	}

	private static void setHeat(Level level, BlockPos pos, int value) {
		ChunkAccess chunk = level.getChunk(pos);
		BoilingCauldrons cauldrons = chunk.getAttachedOrElse(CAULDRONS, BoilingCauldrons.EMPTY);
		if (cauldrons.heat(pos.asLong()) == value) return;
		BoilingCauldrons next = cauldrons.with(pos.asLong(), value);
		if (next.isEmpty()) {
			chunk.removeAttached(CAULDRONS);
		} else {
			chunk.setAttached(CAULDRONS, next);
		}
	}

	private static int waterLevel(BlockState state) {
		return state.is(Blocks.WATER_CAULDRON) ? state.getValue(BlockStateProperties.LEVEL_CAULDRON) : 0;
	}
}
