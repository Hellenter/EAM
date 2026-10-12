package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;

/**
 * Pillager guards: pillagers in a helmet and cuirass with an axe, the king's onslaught (weakened) and a thrown axe. A
 * share of the pillagers in patrols, outposts and raids comes as guards ({@link GuardSpawns}). Numbers are in
 * {@code data/eam/mobs/pillager_guard.json}.
 */
public final class PillagerGuardModule {
	/** The blow at the end of a guard's onslaught: a shield only softens it. */
	public static final ResourceKey<DamageType> ONSLAUGHT_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("pillager_guard_onslaught"));

	private static volatile PillagerGuardSettings settings = PillagerGuardSettings.DEFAULT;

	private PillagerGuardModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.PILLAGER_GUARD, PillagerGuard.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PillagerGuardSettingsLoader.ID, new PillagerGuardSettingsLoader());
	}

	/** Guards start replacing pillagers in patrols, outposts and raids. */
	public static void init() {
		GuardSpawns.enable();
	}

	public static PillagerGuardSettings settings() {
		return settings;
	}

	static void setSettings(PillagerGuardSettings value) {
		settings = value;
	}
}
