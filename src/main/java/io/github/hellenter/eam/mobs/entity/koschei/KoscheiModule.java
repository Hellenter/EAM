package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;

/**
 * Koschei the Deathless, a boss in three phases; only the first, a sword duel, is built yet. Not placed in the world:
 * spawn him with the spawn egg or {@code /summon eam:koschei}. Numbers are in {@code data/eam/mobs/koschei.json}.
 */
public final class KoscheiModule {
	/** His red blows: a shield only halves them, which he works out himself. */
	public static final ResourceKey<DamageType> CLEAVE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("koschei_cleave"));
	/** His lightning: pure damage, through armor and Protection; he works out the shield himself. */
	public static final ResourceKey<DamageType> LIGHTNING_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("koschei_lightning"));
	public static final Holder<MobEffect> CONVULSIONS = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("convulsions"),
			new ConvulsionsEffect());

	private static volatile KoscheiSettings settings = KoscheiSettings.DEFAULT;

	private KoscheiModule() {
	}

	public static void register() {
		BossParry.register();
		FabricDefaultAttributeRegistry.register(EamEntities.KOSCHEI, Koschei.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.KOSCHEI_RAVEN, KoscheiRaven.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(KoscheiSettingsLoader.ID, new KoscheiSettingsLoader());
	}

	public static KoscheiSettings settings() {
		return settings;
	}

	static void setSettings(KoscheiSettings value) {
		settings = value;
	}
}
