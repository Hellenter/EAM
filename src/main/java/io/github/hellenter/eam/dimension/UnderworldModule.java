package io.github.hellenter.eam.dimension;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.dimension.feature.CaveBoulderFeature;
import io.github.hellenter.eam.dimension.feature.CavePillarFeature;
import io.github.hellenter.eam.dimension.feature.CaveStreamFeature;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature;
import io.github.hellenter.eam.dimension.feature.IceSpikeCaveFeature;
import io.github.hellenter.eam.dimension.feature.IceSpireFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldFluidsFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldLakeFeature;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

/**
 * The underworld (MITE's dimension under the overworld): {@link UnderworldLogic} has its layout. The dimension, its
 * terrain, biomes and features are datagen JSON and always exist; this module adds the feature types they use and, with
 * the {@code underworld} flag, routes nether portals through it ({@link UnderworldPortals}).
 */
public final class UnderworldModule {
	private UnderworldModule() {
	}

	/** The feature types exist whatever the config says, so the biome JSON always loads. */
	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("underworld_fluids"), UnderworldFluidsFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("giant_mushroom"), GiantMushroomFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("ice_spike_cave"), IceSpikeCaveFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("underworld_lake"), UnderworldLakeFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("cave_pillar"), CavePillarFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("ice_spire"), IceSpireFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("cave_stream"), CaveStreamFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("cave_boulder"), CaveBoulderFeature.CODEC);
	}

	public static void init() {
		UnderworldPortals.enable();
	}

	/** Whether nether portals work in this level beyond vanilla's overworld and nether: in the underworld, when enabled. */
	public static boolean portalsWork(Level level) {
		return UnderworldPortals.enabled() && UnderworldDimension.is(level);
	}
}
