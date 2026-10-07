package io.github.hellenter.eam.content.campfire;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The camp fire: a fire pit fed with sticks. The block itself is registered in {@code EamBlocks}. */
public final class CampFireModule {
	/** Items that light a camp fire: flint and steel, fire charge, flint, flint shard. */
	public static final TagKey<Item> IGNITERS = TagKey.create(Registries.ITEM, Eam.id("camp_fire_igniters"));

	private static volatile CampFireSettings settings = CampFireSettings.DEFAULT;

	private CampFireModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(CampFireSettingsLoader.ID, new CampFireSettingsLoader());
	}

	public static CampFireSettings settings() {
		return settings;
	}

	static void setSettings(CampFireSettings value) {
		settings = value;
	}
}
