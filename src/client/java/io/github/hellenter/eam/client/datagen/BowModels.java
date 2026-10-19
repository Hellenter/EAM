package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.content.arrows.NockedArrow;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.longbow.LongbowLogic;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The vanilla bow drawn with a metal arrow shows that arrow's head, like MITE's {@code _arrow_0..2} bow textures. Any
 * other arrow keeps the vanilla pulling textures. The longbow has its own draw stages, on a model held larger than the
 * bow ({@code assets/eam/models/item/longbow_base.json}).
 */
final class BowModels {
	private static final Map<Item, String> HEADS = Map.of(
			EamItems.COPPER_ARROW, "copper",
			EamItems.SILVER_ARROW, "silver",
			EamItems.IRON_ARROW, "iron",
			EamItems.ANCIENT_METAL_ARROW, "ancient_metal",
			EamItems.MITHRIL_ARROW, "mithril");

	/** The longbow's models: the bow's, held larger. */
	private static final ModelTemplate LONGBOW = new ModelTemplate(Optional.of(Eam.id("item/longbow_base")), Optional.empty(),
			TextureSlot.LAYER0);

	private BowModels() {
	}

	static void generate(ItemModelGenerators generators) {
		NockedArrow.register();
		List<SelectItemModel.SwitchCase<Item>> cases = new ArrayList<>();
		HEADS.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(head -> {
			Identifier[] stages = new Identifier[3];
			for (int stage = 0; stage < 3; stage++) {
				Identifier id = Eam.id("item/bow_pulling_" + stage + "_" + head.getValue());
				stages[stage] = ModelTemplates.BOW.create(id, TextureMapping.layer0(new Material(id)), generators.modelOutput);
			}
			cases.add(ItemModelUtils.when(head.getKey(), pulling(stages)));
		});
		Identifier[] vanilla = new Identifier[3];
		for (int stage = 0; stage < 3; stage++) {
			vanilla[stage] = Identifier.withDefaultNamespace("item/bow_pulling_" + stage);
		}
		generators.itemModelOutput.accept(Items.BOW, ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
				ItemModelUtils.select(new NockedArrow(), pulling(vanilla), cases),
				ItemModelUtils.plainModel(Identifier.withDefaultNamespace("item/bow"))));
		generateLongbow(generators);
	}

	/** Its draw is twice as long, so the stages come at the same shares of it. */
	private static void generateLongbow(ItemModelGenerators generators) {
		Identifier idle = LONGBOW.create(EamItems.LONGBOW, TextureMapping.layer0(EamItems.LONGBOW), generators.modelOutput);
		Identifier[] stages = new Identifier[3];
		for (int stage = 0; stage < 3; stage++) {
			Identifier id = Eam.id("item/longbow_pulling_" + stage);
			stages[stage] = LONGBOW.create(id, TextureMapping.layer0(new Material(id)), generators.modelOutput);
		}
		generators.itemModelOutput.accept(EamItems.LONGBOW, ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
				pulling(stages, 1.0F / LongbowLogic.FULL_DRAW_TICKS), ItemModelUtils.plainModel(idle)));
	}

	/** Vanilla's draw stages: the first at once, the second at 65% of a full draw, the third at 90%. */
	private static ItemModel.Unbaked pulling(Identifier[] stages) {
		return pulling(stages, 0.05F);
	}

	/** The draw stages for a weapon fully drawn after {@code 1 / scale} ticks. */
	private static ItemModel.Unbaked pulling(Identifier[] stages, float scale) {
		return ItemModelUtils.rangeSelect(new UseDuration(false), scale, ItemModelUtils.plainModel(stages[0]),
				ItemModelUtils.override(ItemModelUtils.plainModel(stages[1]), 0.65F),
				ItemModelUtils.override(ItemModelUtils.plainModel(stages[2]), 0.9F));
	}
}
