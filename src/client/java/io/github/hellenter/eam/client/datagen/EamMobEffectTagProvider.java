package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.brewing.WhiteHoneyLogic;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.concurrent.CompletableFuture;

public class EamMobEffectTagProvider extends FabricTagsProvider<MobEffect> {
	public EamMobEffectTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.MOB_EFFECT, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// White honey keeps these off its drinker and washes them away.
		builder(WhiteHoneyLogic.WARDED).add(MobEffects.POISON.unwrapKey().orElseThrow(), MobEffects.SLOWNESS.unwrapKey().orElseThrow(),
				MobEffects.WEAKNESS.unwrapKey().orElseThrow());
	}
}
