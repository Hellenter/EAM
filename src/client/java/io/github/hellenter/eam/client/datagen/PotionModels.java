package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.brewing.BrewingModule;

import net.minecraft.client.color.item.Potion;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.select.ComponentContents;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Light potions and holy water look like vanilla potions of the same color, but their bottle has a stopper of
 * patinated silver. The vanilla potion item models pick our bottle by the stack's potion contents.
 */
final class PotionModels {
	private static final Material OVERLAY = new Material(Identifier.withDefaultNamespace("item/potion_overlay"));

	private PotionModels() {
	}

	static void generate(ItemModelGenerators generators) {
		List<PotionContents> silver = new ArrayList<>();
		silver.add(new PotionContents(BrewingModule.HOLY_WATER));
		for (Holder<net.minecraft.world.item.alchemy.Potion> potion : BrewingModule.LIGHT_POTIONS) {
			silver.add(new PotionContents(potion));
		}
		bottle(generators, Items.POTION, "light_potion", silver);
		bottle(generators, Items.SPLASH_POTION, "light_splash_potion", silver);
	}

	private static void bottle(ItemModelGenerators generators, Item item, String name, List<PotionContents> silver) {
		Identifier vanilla = Identifier.withDefaultNamespace("item/" + item.builtInRegistryHolder().key().identifier().getPath());
		Identifier light = generators.generateLayeredItem(Eam.id("item/" + name), OVERLAY, new Material(Eam.id("item/" + name)));
		ItemModel.Unbaked vanillaModel = ItemModelUtils.tintedModel(vanilla, new Potion());
		ItemModel.Unbaked lightModel = ItemModelUtils.tintedModel(light, new Potion());
		generators.itemModelOutput.accept(item, ItemModelUtils.select(new ComponentContents<>(DataComponents.POTION_CONTENTS), vanillaModel,
				ItemModelUtils.when(silver, lightModel)));
	}
}
