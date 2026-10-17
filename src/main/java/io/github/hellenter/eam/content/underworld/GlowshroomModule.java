package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Glowshrooms as a resource: shears cut the caps off ripe glowshroom tips ({@link GlowshroomVinesBlock}), and bare
 * tips ripen again over time. The caps are eaten raw or crafted into a glowshroom lamp, a light rain does not put out.
 */
public final class GlowshroomModule {
	/** What shears cut off one ripe tip. */
	public static final ResourceKey<LootTable> HARVEST = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("harvest/glowshroom_vines"));

	private static volatile GlowshroomSettings settings = GlowshroomSettings.DEFAULT;

	private GlowshroomModule() {
	}

	public static void init() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GlowshroomSettingsLoader.ID, new GlowshroomSettingsLoader());
	}

	public static GlowshroomSettings settings() {
		return settings;
	}

	static void setSettings(GlowshroomSettings value) {
		settings = value;
	}
}
