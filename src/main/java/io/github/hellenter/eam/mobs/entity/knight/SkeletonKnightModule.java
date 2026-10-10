package io.github.hellenter.eam.mobs.entity.knight;

import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.packs.PackType;

/**
 * The skeleton knight: a crypt skeleton with a shield and a sword, spawned by the crypt's green-wool trial spawners
 * ({@code eam:crypt/knight}). Numbers are in {@code data/eam/mobs/skeleton_knight.json}.
 */
public final class SkeletonKnightModule {
	private static volatile SkeletonKnightSettings settings = SkeletonKnightSettings.DEFAULT;

	private SkeletonKnightModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.SKELETON_KNIGHT, SkeletonKnight.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SkeletonKnightSettingsLoader.ID, new SkeletonKnightSettingsLoader());
	}

	static SkeletonKnightSettings settings() {
		return settings;
	}

	static void setSettings(SkeletonKnightSettings value) {
		settings = value;
	}
}
