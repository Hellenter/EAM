package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
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
		// Death message key: death.attack.eam.fallen_lord_dash
		context.register(FallenLordModule.DASH_DAMAGE, new DamageType("eam.fallen_lord_dash", 0.1F));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), ThirstModule.DEHYDRATION);
		entries.add(registries.lookupOrThrow(Registries.DAMAGE_TYPE), FallenLordModule.DASH_DAMAGE);
	}

	@Override
	public String getName() {
		return "EAM damage types";
	}
}
