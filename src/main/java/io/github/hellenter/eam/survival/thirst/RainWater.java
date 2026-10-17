package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.content.bucket.CauldronDispatcherInvoker;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.BooleanSupplier;

/**
 * Rain water: a cauldron filled only by rain holds clean water, so a bottle or a bowl taken from it is safe to drink
 * without boiling. Water poured in by hand, a bucket or a dripstone spoils the whole cauldron.
 */
public final class RainWater {
	/** Water cauldrons in a chunk that hold rain water. */
	public static final AttachmentType<RainCauldrons> CAULDRONS = AttachmentRegistry.create(Eam.id("rain_cauldrons"),
			builder -> builder.persistent(RainCauldrons.CODEC));

	private static boolean enabled;
	/** Set while rain fills a cauldron; precipitation only ticks on the server thread. */
	private static boolean raining;

	private RainWater() {
	}

	/** Loads the class so the attachment is registered whatever the config says. */
	static void register() {
	}

	static void init() {
		enabled = true;
		((CauldronDispatcherInvoker) (Object) CauldronInteractions.WATER).eam$put(Items.GLASS_BOTTLE,
				(state, level, pos, player, hand, itemInHand) -> takeWater(state, level, pos, player, hand, itemInHand,
						isRainWater(level, pos) ? RawWater.rainWaterBottle() : RawWater.rawWaterBottle()));
	}

	/** Runs a rain fill of a cauldron so the water it adds counts as rain water. */
	public static boolean fillWithRain(BooleanSupplier fill) {
		raining = true;
		try {
			return fill.getAsBoolean();
		} finally {
			raining = false;
		}
	}

	/** Every server-side block state change: keeps the rain water mark of a cauldron in step with its water. */
	public static void onChanged(Level level, BlockPos pos, BlockState state, BlockState oldState) {
		if (!enabled) return;
		int newLevel = waterLevel(state);
		int oldLevel = waterLevel(oldState);
		if (newLevel == 0 && oldLevel == 0) return;

		ChunkAccess chunk = level.getChunk(pos);
		RainCauldrons cauldrons = chunk.getAttachedOrElse(CAULDRONS, RainCauldrons.EMPTY);
		boolean was = cauldrons.contains(pos.asLong());
		boolean now = ThirstLogic.rainWaterAfter(was, oldLevel, newLevel, raining);
		if (now == was) return;

		RainCauldrons next = now ? cauldrons.with(pos.asLong()) : cauldrons.without(pos.asLong());
		if (next.positions().isEmpty()) {
			chunk.removeAttached(CAULDRONS);
		} else {
			chunk.setAttached(CAULDRONS, next);
		}
	}

	/** Whether the water cauldron at {@code pos} holds rain water. Only the server knows; false on the client. */
	public static boolean isRainWater(Level level, BlockPos pos) {
		RainCauldrons cauldrons = level.getChunk(pos).getAttached(CAULDRONS);
		return enabled && cauldrons != null && cauldrons.contains(pos.asLong());
	}

	/** Takes one level of water out of a water cauldron into {@code filled}, the way vanilla fills a glass bottle. */
	public static InteractionResult takeWater(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			ItemStack itemInHand, ItemStack filled) {
		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemInHand, player, filled));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemInHand.getItem()));
			LayeredCauldronBlock.lowerFillLevel(state, level, pos);
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
		}
		return InteractionResult.SUCCESS;
	}

	private static int waterLevel(BlockState state) {
		return state.is(Blocks.WATER_CAULDRON) ? state.getValue(LayeredCauldronBlock.LEVEL) : 0;
	}
}
