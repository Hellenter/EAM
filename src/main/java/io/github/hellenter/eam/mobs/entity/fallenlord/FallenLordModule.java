package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * The Fallen Lord, boss of the crypt. Not placed in the world yet: spawn him with the spawn egg or
 * {@code /summon eam:fallen_lord}. Numbers are in {@code data/eam/mobs/fallen_lord.json}.
 */
public final class FallenLordModule {
	/** The red-eyed dash; shields don't stop it. */
	public static final ResourceKey<DamageType> DASH_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("fallen_lord_dash"));
	/** The overhead strike with the red blade: it goes through shields, which take only part of it. */
	public static final ResourceKey<DamageType> HEAVY_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("fallen_lord_heavy"));
	/** Weapon hits and projectiles: his physical resistance applies to these. */
	public static final TagKey<DamageType> PHYSICAL = TagKey.create(Registries.DAMAGE_TYPE, Eam.id("physical"));

	private static volatile FallenLordSettings settings = FallenLordSettings.DEFAULT;

	private FallenLordModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.FALLEN_LORD, FallenLord.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FallenLordSettingsLoader.ID, new FallenLordSettingsLoader());
	}

	static FallenLordSettings settings() {
		return settings;
	}

	static void setSettings(FallenLordSettings value) {
		settings = value;
	}
}
