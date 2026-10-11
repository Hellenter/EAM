package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;

/**
 * The heavy Lost cavalrymen: undead knights in full holed plate on armored undead horses, always in pairs. They ride round
 * the player and charge in turn with couched lances, the first stunning a shield and the second striking past it; they
 * throw lances at a player they cannot ride down. Unhorsed, they fight on foot with axe and shield. They will guard the
 * monuments; until then only {@code /summon eam:lost_cavalryman} or the egg brings a pair. Numbers are in
 * {@code data/eam/mobs/lost_cavalry.json}.
 */
public final class LostCavalryModule {
	/** The couched lance at the gallop: it goes through a shield in part, worked out by the cavalryman himself. */
	public static final ResourceKey<DamageType> LANCE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("lost_cavalry_lance"));
	/** The axe sweep, which goes round a shield. */
	public static final ResourceKey<DamageType> SWEEP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("lost_cavalry_sweep"));

	private static volatile LostCavalrySettings settings = LostCavalrySettings.DEFAULT;

	private LostCavalryModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_CAVALRYMAN, LostCavalryman.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.LOST_WARHORSE, LostWarhorse.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(LostCavalrySettingsLoader.ID, new LostCavalrySettingsLoader());
	}

	static LostCavalrySettings settings() {
		return settings;
	}

	static void setSettings(LostCavalrySettings value) {
		settings = value;
	}
}
