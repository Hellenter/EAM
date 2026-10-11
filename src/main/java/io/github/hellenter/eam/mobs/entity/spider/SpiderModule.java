package io.github.hellenter.eam.mobs.entity.spider;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The small spiders from MITE: the wood spider of the tree crowns and the poisonous black widow. Both are weak
 * early-game monsters. Numbers are in {@code data/eam/mobs/wood_spider.json} and {@code black_widow.json}; where they
 * spawn are the biome tags {@code eam:has_wood_spiders} and {@code eam:has_black_widows}.
 */
public final class SpiderModule {
	public static final TagKey<Biome> WOOD_SPIDERS_SPAWN_IN = TagKey.create(Registries.BIOME, Eam.id("has_wood_spiders"));
	public static final TagKey<Biome> BLACK_WIDOWS_SPAWN_IN = TagKey.create(Registries.BIOME, Eam.id("has_black_widows"));
	/** Spawn weights among the biome's monsters (zombies and skeletons have about 100). */
	private static final int WOOD_SPIDER_WEIGHT = 30;
	private static final int BLACK_WIDOW_WEIGHT = 10;

	/** On the ground like any monster, or on top of leaves, which other monsters cannot spawn on. */
	private static final SpawnPlacementType ON_GROUND_OR_LEAVES = new SpawnPlacementType() {
		@Override
		public boolean isSpawnPositionOk(LevelReader level, BlockPos pos, @Nullable EntityType<?> type) {
			if (SpawnPlacementTypes.ON_GROUND.isSpawnPositionOk(level, pos, type)) {
				return true;
			}
			if (type == null || !level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos.below()).is(BlockTags.LEAVES)) {
				return false;
			}
			BlockState state = level.getBlockState(pos);
			return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), type);
		}

		@Override
		public BlockPos adjustSpawnPosition(LevelReader level, BlockPos candidate) {
			return SpawnPlacementTypes.ON_GROUND.adjustSpawnPosition(level, candidate);
		}
	};

	private static volatile SpiderSettings woodSpider = SpiderSettings.WOOD_SPIDER;
	private static volatile SpiderSettings blackWidow = SpiderSettings.BLACK_WIDOW;

	private SpiderModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.WOOD_SPIDER, WoodSpider.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.BLACK_WIDOW, WoodSpider.createAttributes());
		SpawnPlacements.register(EamEntities.WOOD_SPIDER, ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, WoodSpider::checkSpawnRules);
		SpawnPlacements.register(EamEntities.BLACK_WIDOW, ON_GROUND_OR_LEAVES, Heightmap.Types.MOTION_BLOCKING, WoodSpider::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SpiderSettingsLoader.ID, new SpiderSettingsLoader());
	}

	/** Natural spawning; the mobs themselves exist whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(WOOD_SPIDERS_SPAWN_IN), MobCategory.MONSTER, EamEntities.WOOD_SPIDER,
				WOOD_SPIDER_WEIGHT, 1, 2);
		BiomeModifications.addSpawn(BiomeSelectors.tag(BLACK_WIDOWS_SPAWN_IN), MobCategory.MONSTER, EamEntities.BLACK_WIDOW,
				BLACK_WIDOW_WEIGHT, 1, 1);
	}

	static SpiderSettings woodSpider() {
		return woodSpider;
	}

	static SpiderSettings blackWidow() {
		return blackWidow;
	}

	static SpiderSettings settings(EntityType<?> type) {
		return type == EamEntities.BLACK_WIDOW ? blackWidow : woodSpider;
	}

	static void setSettings(SpiderSettings wood, SpiderSettings widow) {
		woodSpider = wood;
		blackWidow = widow;
	}
}
