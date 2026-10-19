package io.github.hellenter.eam.content.oven;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.packs.PackType;

/**
 * The clay oven: an early-game smoker of clay around a camp fire. It cooks food only, slower than a smoker, but its fuel
 * lasts longer. The block is registered in {@code EamBlocks}; the numbers are in {@link ClayOvenSettings}.
 */
public final class ClayOvenModule {
	private static volatile ClayOvenSettings settings = ClayOvenSettings.DEFAULT;

	private ClayOvenModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ClayOvenSettingsLoader.ID, new ClayOvenSettingsLoader());
	}

	public static ClayOvenSettings settings() {
		return settings;
	}

	static void setSettings(ClayOvenSettings value) {
		settings = value;
	}
}
