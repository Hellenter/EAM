package io.github.hellenter.eam.mobs.entity.draugr;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Draugr: packs of dead warriors frozen into the floor of the underworld's ice caves. They do not spawn like other
 * monsters: the worldgen feature {@code eam:draugr_pack} lays them into the floor when the caves generate. Numbers are in
 * {@code data/eam/mobs/draugr.json}; what floor they lie in is the block tag {@code eam:draugr_buried_in}.
 */
public final class DraugrModule {
	public static final TagKey<Block> BURIED_IN = TagKey.create(Registries.BLOCK, Eam.id("draugr_buried_in"));

	private static volatile DraugrSettings settings = DraugrSettings.DEFAULT;

	private DraugrModule() {
	}

	/** The mob and its pack feature exist whatever the config says, so the biome JSON always loads. */
	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.DRAUGR, Draugr.createAttributes());
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("draugr_pack"), DraugrPackFeature.CODEC);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(DraugrSettingsLoader.ID, new DraugrSettingsLoader());
		ServerTickEvents.END_LEVEL_TICK.register(DraugrAlarms::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> DraugrAlarms.clear());
	}

	static DraugrSettings settings() {
		return settings;
	}

	static void setSettings(DraugrSettings value) {
		settings = value;
	}
}
