package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.villager.BlueprintIcon;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintData;
import io.github.hellenter.eam.mobs.vanilla.villager.BlueprintItem;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The blueprint's models: one for each building of each village style the default orders offer, the paper coloured
 * by the style ({@code item/blueprint_paper_<style>}, {@code defense} for any village) under the building's emblem
 * ({@code item/blueprint_emblem_<order>}). A blueprint of anything else shows the plain one.
 */
final class BlueprintModels {
	private BlueprintModels() {
	}

	static void generate(ItemModelGenerators generators) {
		BlueprintIcon.register();
		Identifier plain = ModelTemplates.FLAT_ITEM.create(EamItems.BLUEPRINT, TextureMapping.layer0(EamItems.BLUEPRINT), generators.modelOutput);
		Set<String> keys = new LinkedHashSet<>();
		List<SelectItemModel.SwitchCase<String>> cases = new ArrayList<>();
		for (BlueprintData data : BlueprintItem.creativeBlueprints()) {
			String style = data.style();
			String key = BlueprintIcon.key(style, data.order());
			if (!keys.add(key)) continue;
			Identifier model = ModelTemplates.TWO_LAYERED_ITEM.create(Eam.id("item/blueprint_" + key),
					TextureMapping.layered(new Material(Eam.id("item/blueprint_paper_" + (style == null ? "defense" : style))),
							new Material(Eam.id("item/blueprint_emblem_" + data.order()))),
					generators.modelOutput);
			cases.add(ItemModelUtils.when(key, ItemModelUtils.plainModel(model)));
		}
		generators.itemModelOutput.accept(EamItems.BLUEPRINT,
				ItemModelUtils.select(new BlueprintIcon(), ItemModelUtils.plainModel(plain), cases));
	}
}
