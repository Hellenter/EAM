package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

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
	}
}
