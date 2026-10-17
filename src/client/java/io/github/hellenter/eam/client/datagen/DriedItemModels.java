package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.survival.spoilage.SpoilageModule;

import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Dried food looks darker and browner: its item definition shows the same model with a constant tint while the stack
 * has {@code eam:dried}. Vanilla foods get their definitions replaced; ours are wrapped as they are generated.
 */
final class DriedItemModels {
	/** Multiplied into the texture: a warm, dull brown. */
	private static final int TINT = 0xFFC2A07C;

	private DriedItemModels() {
	}

	/** The fresh look, or the tinted one while the stack is dried. Only plain untinted models can be tinted. */
	static ItemModel.Unbaked wrap(ItemModel.Unbaked fresh) {
		if (!(fresh instanceof CuboidItemModelWrapper.Unbaked plain) || !plain.tints().isEmpty()) {
			throw new IllegalArgumentException("Dryable food needs a plain item model: " + fresh);
		}
		ItemModel.Unbaked dried = new CuboidItemModelWrapper.Unbaked(plain.model(), plain.transformation(),
				List.of(ItemModelUtils.constantTint(TINT)));
		return ItemModelUtils.conditional(ItemModelUtils.hasComponent(SpoilageModule.DRIED), dried, fresh);
	}

	/** An output that wraps every definition put into it; generators handed it make dryable food. */
	static ItemModelOutput output(ItemModelOutput out) {
		return new ItemModelOutput() {
			@Override
			public void accept(Item item, ItemModel.Unbaked generator, ClientItem.Properties properties) {
				out.accept(item, wrap(generator), properties);
			}

			@Override
			public void copy(Item donor, Item acceptor) {
				out.copy(donor, acceptor);
			}
		};
	}

	/** Definitions for the vanilla dryable foods, all of which use their plain {@code item/<name>} model. */
	static void vanilla(BiConsumer<Identifier, ClientItem> provider) {
		for (Item item : EamItemTagProvider.DRYABLE) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
				provider.accept(id, new ClientItem(wrap(ItemModelUtils.plainModel(id.withPrefix("item/"))), ClientItem.Properties.DEFAULT));
			}
		}
	}
}
