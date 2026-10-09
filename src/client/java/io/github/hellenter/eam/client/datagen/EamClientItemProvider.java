package io.github.hellenter.eam.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Item definitions ({@code assets/eam/items}) that no registered item owns: looks picked by the item_model component. */
public class EamClientItemProvider extends FabricCodecDataProvider<ClientItem> {
	public EamClientItemProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.RESOURCE_PACK, "items", ClientItem.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ClientItem> provider, HolderLookup.Provider registries) {
		FallenLordItemModels.clientItems().forEach(provider);
	}

	@Override
	public String getName() {
		return "EAM item definitions without items";
	}
}
