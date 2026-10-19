package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.survival.spoilage.Dried;
import io.github.hellenter.eam.survival.spoilage.SpoilageModule;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.server.packs.PackType;

/**
 * The food dryer: a rack food dries on, turning into its dried item ({@link DriedFoods}), which keeps longer. The block
 * is registered in {@code EamBlocks}; what dried food does is in {@link Dried}.
 */
public final class DryerModule {
	private static volatile DryerSettings settings = DryerSettings.DEFAULT;

	private DryerModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(DryerSettingsLoader.ID, new DryerSettingsLoader());
		// Dried food from the creative tab or a command costs water too; the dryer stamps the datapack's amount.
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(DriedFoods::isDried,
				(builder, item) -> builder.set(SpoilageModule.DRIED, new Dried(DryerSettings.DEFAULT.water()))));
	}

	public static DryerSettings settings() {
		return settings;
	}

	static void setSettings(DryerSettings value) {
		settings = value;
	}
}
