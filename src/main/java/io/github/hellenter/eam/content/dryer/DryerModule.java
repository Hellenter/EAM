package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The food dryer: a rack food dries on, so it keeps longer. The block is registered in {@code EamBlocks}; what dried
 * food does is in {@link io.github.hellenter.eam.survival.spoilage.Dried}.
 */
public final class DryerModule {
	/** Foods that can be dried: fruit, berries, root vegetables, bread and cooked meat and fish. */
	public static final TagKey<Item> DRYABLE = TagKey.create(Registries.ITEM, Eam.id("dryable"));

	private static volatile DryerSettings settings = DryerSettings.DEFAULT;

	private DryerModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(DryerSettingsLoader.ID, new DryerSettingsLoader());
	}

	public static DryerSettings settings() {
		return settings;
	}

	static void setSettings(DryerSettings value) {
		settings = value;
	}
}
