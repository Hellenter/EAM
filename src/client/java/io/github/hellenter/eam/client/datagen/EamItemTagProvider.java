package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.tools.FlintTools;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class EamItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public EamItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(EamItems.FLASK_THREADS).add(EamItems.LEATHER_THREAD.builtInRegistryHolder().key(), Items.STRING.builtInRegistryHolder().key());
		builder(CampFireModule.IGNITERS).add(
				Items.FLINT_AND_STEEL.builtInRegistryHolder().key(),
				Items.FIRE_CHARGE.builtInRegistryHolder().key(),
				Items.FLINT.builtInRegistryHolder().key(),
				EamItems.FLINT_SHARD.builtInRegistryHolder().key());
		builder(FlintTools.REPAIR_MATERIALS).add(Items.FLINT.builtInRegistryHolder().key());
		builder(ItemTags.AXES).add(EamItems.FLINT_HATCHET.builtInRegistryHolder().key(), EamItems.FLINT_AXE.builtInRegistryHolder().key());
	}
}
