package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;

/**
 * The Pillager King, the first boss: an armored giant with two axes. Not placed in the world yet: spawn him with the
 * spawn egg or {@code /summon eam:pillager_king}. Numbers are in {@code data/eam/mobs/pillager_king.json}.
 */
public final class PillagerKingModule {
	/** His whirl with both axes: shields don't stop it. */
	public static final ResourceKey<DamageType> WHIRL_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("pillager_king_whirl"));

	private static volatile PillagerKingSettings settings = PillagerKingSettings.DEFAULT;

	private PillagerKingModule() {
	}

	public static void register() {
		BossParry.register();
		FabricDefaultAttributeRegistry.register(EamEntities.PILLAGER_KING, PillagerKing.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(PillagerKingSettingsLoader.ID, new PillagerKingSettingsLoader());
	}

	public static PillagerKingSettings settings() {
		return settings;
	}

	static void setSettings(PillagerKingSettings value) {
		settings = value;
	}
}
