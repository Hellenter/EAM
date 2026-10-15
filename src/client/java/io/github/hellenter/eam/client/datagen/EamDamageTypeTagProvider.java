package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryModule;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanModule;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardModule;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingModule;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaModule;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiModule;
import io.github.hellenter.eam.survival.bleeding.BleedingModule;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.concurrent.CompletableFuture;

public class EamDamageTypeTagProvider extends FabricTagsProvider<DamageType> {
	public EamDamageTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.DAMAGE_TYPE, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Dehydration behaves like vanilla starvation.
		builder(DamageTypeTags.BYPASSES_ARMOR).add(ThirstModule.DEHYDRATION);
		builder(DamageTypeTags.BYPASSES_EFFECTS).add(ThirstModule.DEHYDRATION);
		builder(DamageTypeTags.NO_KNOCKBACK).add(ThirstModule.DEHYDRATION);
		builder(DamageTypeTags.BYPASSES_WOLF_ARMOR).add(ThirstModule.DEHYDRATION);
		// Bleeding goes through armor without knockback.
		builder(DamageTypeTags.BYPASSES_ARMOR).add(BleedingModule.DAMAGE);
		builder(DamageTypeTags.NO_KNOCKBACK).add(BleedingModule.DAMAGE);
		// The Fallen Lord's dash can only be dodged.
		// His heavy strike goes through shields too: he works out the share a shield takes himself.
		builder(DamageTypeTags.BYPASSES_SHIELD).add(FallenLordModule.DASH_DAMAGE, FallenLordModule.HEAVY_DAMAGE);
		// The heavy cavalryman's lance works out the share a shield takes itself; his axe sweep goes round a shield.
		builder(DamageTypeTags.BYPASSES_SHIELD).add(LostCavalryModule.LANCE_DAMAGE, LostCavalryModule.SWEEP_DAMAGE);
		// The elite crossbowman's red bolt goes through a shield, which still takes part of the damage (worked out by the bolt).
		builder(DamageTypeTags.BYPASSES_SHIELD).add(EliteCrossbowmanModule.RED_BOLT_DAMAGE);
		builder(DamageTypeTags.IS_PROJECTILE).add(EliteCrossbowmanModule.RED_BOLT_DAMAGE);
		// The onslaught of the Pillager King and his guards works out the share a shield takes itself.
		builder(DamageTypeTags.BYPASSES_SHIELD).add(PillagerKingModule.ONSLAUGHT_DAMAGE, PillagerGuardModule.ONSLAUGHT_DAMAGE);
		// Baba Yaga's curse (glyph of harm, acid) is pure damage: through armor, its enchantments and shields. Her fire beam
		// goes through armor too; she checks a shield held towards her herself.
		builder(DamageTypeTags.BYPASSES_ARMOR).add(BabaYagaModule.CURSE_DAMAGE, BabaYagaModule.BEAM_DAMAGE);
		builder(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(BabaYagaModule.CURSE_DAMAGE, BabaYagaModule.BEAM_DAMAGE);
		builder(DamageTypeTags.BYPASSES_SHIELD).add(BabaYagaModule.CURSE_DAMAGE, BabaYagaModule.BEAM_DAMAGE);
		builder(DamageTypeTags.NO_KNOCKBACK).add(BabaYagaModule.CURSE_DAMAGE, BabaYagaModule.BEAM_DAMAGE);
		// Koschei's red blows: a shield only halves them, which he works out himself. His lightning is pure damage, and
		// he checks a shield held towards it himself.
		builder(DamageTypeTags.BYPASSES_SHIELD).add(KoscheiModule.CLEAVE_DAMAGE, KoscheiModule.LIGHTNING_DAMAGE);
		builder(DamageTypeTags.BYPASSES_ARMOR).add(KoscheiModule.LIGHTNING_DAMAGE);
		builder(DamageTypeTags.BYPASSES_ENCHANTMENTS).add(KoscheiModule.LIGHTNING_DAMAGE);
		builder(DamageTypeTags.NO_KNOCKBACK).add(KoscheiModule.LIGHTNING_DAMAGE);
		// Weapon hits and projectiles, which the Fallen Lord partly resists.
		builder(FallenLordModule.PHYSICAL).add(DamageTypes.PLAYER_ATTACK, DamageTypes.MOB_ATTACK, DamageTypes.MOB_ATTACK_NO_AGGRO,
				DamageTypes.SPEAR, DamageTypes.ARROW, DamageTypes.TRIDENT, DamageTypes.MOB_PROJECTILE, DamageTypes.THROWN, DamageTypes.STING,
				DamageTypes.MACE_SMASH).add(FallenLordModule.HEAVY_DAMAGE, LostCavalryModule.LANCE_DAMAGE, LostCavalryModule.SWEEP_DAMAGE,
				EliteCrossbowmanModule.RED_BOLT_DAMAGE, KoscheiModule.CLEAVE_DAMAGE);
	}
}
