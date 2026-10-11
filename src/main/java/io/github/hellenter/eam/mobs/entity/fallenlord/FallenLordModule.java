package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.king.PillagerKing;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingModule;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

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
	/** Weak blocks his dashes smash through: earth, wood, cobblestone. */
	public static final TagKey<Block> DASH_SMASHES = TagKey.create(Registries.BLOCK, Eam.id("fallen_lord_dash_smashes"));

	private static volatile FallenLordSettings settings = FallenLordSettings.DEFAULT;

	private FallenLordModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.FALLEN_LORD, FallenLord.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FallenLordSettingsLoader.ID, new FallenLordSettingsLoader());
	}

	/** Share of this damage that goes through armor and Protection: only the bosses' own hits have one. */
	public static double armorPiercing(DamageSource source) {
		if (source.getEntity() instanceof PillagerKing) {
			return PillagerKingModule.settings().stats().armorPiercing();
		}
		return source.getEntity() instanceof FallenLord ? settings.later().armorPiercing() : 0.0;
	}

	/** The armor-piercing part of a hit as the player takes it: only the Resistance effect still lowers it. */
	public static float piercedAfterResistance(LivingEntity victim, DamageSource source, float part) {
		if (part <= 0.0F) {
			return 0.0F;
		}
		MobEffectInstance resistance = victim.getEffect(MobEffects.RESISTANCE);
		boolean bypassed = source.is(DamageTypeTags.BYPASSES_EFFECTS) || source.is(DamageTypeTags.BYPASSES_RESISTANCE);
		return FallenLordLogic.afterResistance(part, resistance == null || bypassed ? -1 : resistance.getAmplifier());
	}

	static FallenLordSettings settings() {
		return settings;
	}

	static void setSettings(FallenLordSettings value) {
		settings = value;
	}
}
