package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryModule;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanModule;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardModule;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingModule;
import io.github.hellenter.eam.survival.bleeding.BleedingModule;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaModule;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiModule;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;

public class EamDamageTypeProvider extends FabricDynamicRegistryProvider {
	public EamDamageTypeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<DamageType> context) {
		// Death message key: death.attack.eam.dehydration
		context.register(ThirstModule.DEHYDRATION, new DamageType("eam.dehydration", 0.0F));
		// Death message key: death.attack.eam.bleeding
		context.register(BleedingModule.DAMAGE, new DamageType("eam.bleeding", 0.0F));
		// Death message key: death.attack.eam.fallen_lord_dash
		context.register(FallenLordModule.DASH_DAMAGE, new DamageType("eam.fallen_lord_dash", 0.1F));
		// Death message key: death.attack.eam.fallen_lord_heavy
		context.register(FallenLordModule.HEAVY_DAMAGE, new DamageType("eam.fallen_lord_heavy", 0.1F));
		// Death message keys: death.attack.eam.lost_cavalry_lance, death.attack.eam.lost_cavalry_sweep
		context.register(LostCavalryModule.LANCE_DAMAGE, new DamageType("eam.lost_cavalry_lance", 0.1F));
		context.register(LostCavalryModule.SWEEP_DAMAGE, new DamageType("eam.lost_cavalry_sweep", 0.1F));
		// Death message key: death.attack.eam.pillager_king_onslaught
		context.register(PillagerKingModule.ONSLAUGHT_DAMAGE, new DamageType("eam.pillager_king_onslaught", 0.1F));
		// Death message key: death.attack.eam.red_bolt
		context.register(EliteCrossbowmanModule.RED_BOLT_DAMAGE, new DamageType("eam.red_bolt", 0.1F));
		// Death message key: death.attack.eam.pillager_guard_onslaught
		context.register(PillagerGuardModule.ONSLAUGHT_DAMAGE, new DamageType("eam.pillager_guard_onslaught", 0.1F));
		// Death message keys: death.attack.eam.baba_yaga_curse, death.attack.eam.baba_yaga_beam
		context.register(BabaYagaModule.CURSE_DAMAGE, new DamageType("eam.baba_yaga_curse", 0.0F));
		context.register(BabaYagaModule.BEAM_DAMAGE, new DamageType("eam.baba_yaga_beam", 0.1F));
		// Death message keys: death.attack.eam.koschei_cleave, death.attack.eam.koschei_lightning
		context.register(KoscheiModule.CLEAVE_DAMAGE, new DamageType("eam.koschei_cleave", 0.1F));
		context.register(KoscheiModule.LIGHTNING_DAMAGE, new DamageType("eam.koschei_lightning", 0.0F));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), ThirstModule.DEHYDRATION);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), BleedingModule.DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), FallenLordModule.DASH_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), FallenLordModule.HEAVY_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), LostCavalryModule.LANCE_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), LostCavalryModule.SWEEP_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), PillagerKingModule.ONSLAUGHT_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), EliteCrossbowmanModule.RED_BOLT_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), PillagerGuardModule.ONSLAUGHT_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), BabaYagaModule.CURSE_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), BabaYagaModule.BEAM_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), KoscheiModule.CLEAVE_DAMAGE);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), KoscheiModule.LIGHTNING_DAMAGE);
	}

	@Override
	public String getName() {
		return "EAM damage types";
	}
}
