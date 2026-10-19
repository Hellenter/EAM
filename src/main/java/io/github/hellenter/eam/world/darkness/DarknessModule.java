package io.github.hellenter.eam.world.darkness;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.attribute.AttributeRange;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.timeline.Timeline;

/**
 * Darkness and light. Darker nights (by moon phase) and caves are pure data: the {@code eam:darkness} timeline in
 * {@code #minecraft:in_overworld} and the underworld's dimension type. Rain putting out fires is code, behind the
 * {@code rain_douses_fires} flag.
 */
public final class DarknessModule {
	/**
	 * The highest brightness (gamma) setting the lightmap honours where the player stands. 1 keeps vanilla; the
	 * darkness timeline and the underworld lower it. Synced, so the client reads it from the server's data.
	 */
	public static final EnvironmentAttribute<Float> MAX_BRIGHTNESS = EnvironmentAttribute.builder(AttributeTypes.FLOAT)
			.defaultValue(1.0F)
			.valueRange(AttributeRange.UNIT_FLOAT)
			.syncable()
			.build();

	public static final ResourceKey<Timeline> TIMELINE = ResourceKey.create(Registries.TIMELINE, Eam.id("darkness"));

	private DarknessModule() {
	}

	/** The attribute is registered whatever the config says, so client and server always match. */
	public static void register() {
		Registry.register(BuiltInRegistries.ENVIRONMENT_ATTRIBUTE, Eam.id("visual/max_brightness"), MAX_BRIGHTNESS);
	}

	public static void init() {
		RainDousing.enable();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(RainDousingSettingsLoader.ID, new RainDousingSettingsLoader());
	}
}
