package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.vanilla.evolution.WornLooks;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** The worn weapons evolved monsters carry: handheld models over {@code textures/item/worn_*.png}. */
final class WornGearItemModels {
	private WornGearItemModels() {
	}

	static void generate(ItemModelGenerators generators) {
		for (Identifier look : WornLooks.WEAPONS.values()) {
			Identifier model = look.withPrefix("item/");
			ModelTemplates.FLAT_HANDHELD_ITEM.create(model, TextureMapping.layer0(new Material(model)), generators.modelOutput);
		}
	}

	/** Item definitions, keyed by the ids the item_model component names. */
	static Map<Identifier, ClientItem> clientItems() {
		Map<Identifier, ClientItem> items = new HashMap<>();
		for (Identifier look : WornLooks.WEAPONS.values()) {
			items.put(look, new ClientItem(ItemModelUtils.plainModel(look.withPrefix("item/")), ClientItem.Properties.DEFAULT));
		}
		return items;
	}
}
