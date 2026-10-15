package io.github.hellenter.eam.world.farming;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.bluemoon.BlueMoonModule;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Crops on farmland grow several times slower, bone meal speeds a crop up instead of adding stages, and animals grow up
 * and get ready to breed again several times slower. Numbers are in {@code data/eam/farming/growth.json}.
 */
public final class FarmingModule {
	/** Crops in a chunk that got bone meal. Kept until the crop is broken by a player. */
	public static final AttachmentType<BoostedCrops> BOOSTED_CROPS = AttachmentRegistry.create(Eam.id("boosted_crops"),
			builder -> builder.persistent(BoostedCrops.CODEC));

	private static volatile FarmingSettings settings = FarmingSettings.DEFAULT;
	private static boolean crops;
	private static boolean animals;

	private FarmingModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FarmingSettingsLoader.ID, new FarmingSettingsLoader());
	}

	public static void initCrops() {
		crops = true;
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			// The broken block may be the upper half of a pitcher plant, keyed by its lower half.
			forget(level.getChunk(pos), pos);
			forget(level.getChunk(pos.below()), pos.below());
		});
	}

	public static void initAnimals() {
		animals = true;
	}

	/**
	 * Bound of a crop's vanilla growth roll at {@code pos}, stretched by the slowdown and shortened by the bone meal boost
	 * and a blue moon.
	 */
	public static int growthBound(LevelReader level, BlockPos pos, int vanillaBound) {
		double moon = BlueMoonModule.growthMultiplier(level);
		if (!crops) return moon == 1.0 ? vanillaBound : FarmingLogic.growthBound(vanillaBound, 1.0, moon);
		FarmingSettings current = settings;
		double boost = isBoosted(level, pos) ? current.boneMealBoost() : 1.0;
		return FarmingLogic.growthBound(vanillaBound, current.cropSlowdown(), boost * moon);
	}

	/** Whether bone meal can be used on the crop at {@code pos}: a crop takes the boost once. */
	public static boolean canBoneMeal(LevelReader level, BlockPos pos, boolean vanillaValid) {
		return vanillaValid && !(crops && isBoosted(level, pos));
	}

	/** Gives the crop at {@code pos} the bone meal boost. False if vanilla bone meal applies instead. */
	public static boolean boneMeal(LevelReader level, BlockPos pos) {
		if (!crops) return false;
		ChunkAccess chunk = chunk(level, pos);
		chunk.setAttached(BOOSTED_CROPS, chunk.getAttachedOrElse(BOOSTED_CROPS, BoostedCrops.EMPTY).with(pos.asLong()));
		return true;
	}

	/** The block a pitcher plant's growth is keyed by: its lower half. */
	public static BlockPos pitcherRoot(BlockPos pos, BlockState state) {
		return state.getValueOrElse(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	/** Whether {@code mob} ages on this tick. Only animals are slowed, villagers grow up as in vanilla. */
	public static boolean agesThisTick(AgeableMob mob) {
		return !animals || !(mob instanceof Animal) || FarmingLogic.agesThisTick(mob.tickCount, settings.animalSlowdown());
	}

	private static boolean isBoosted(LevelReader level, BlockPos pos) {
		BoostedCrops boosted = chunk(level, pos).getAttached(BOOSTED_CROPS);
		return boosted != null && boosted.contains(pos.asLong());
	}

	private static void forget(ChunkAccess chunk, BlockPos pos) {
		BoostedCrops boosted = chunk.getAttached(BOOSTED_CROPS);
		if (boosted == null || !boosted.contains(pos.asLong())) return;
		BoostedCrops rest = boosted.without(pos.asLong());
		if (rest.positions().isEmpty()) {
			chunk.removeAttached(BOOSTED_CROPS);
		} else {
			chunk.setAttached(BOOSTED_CROPS, rest);
		}
	}

	private static ChunkAccess chunk(LevelReader level, BlockPos pos) {
		return level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
	}

	static void setSettings(FarmingSettings value) {
		settings = value;
	}
}
