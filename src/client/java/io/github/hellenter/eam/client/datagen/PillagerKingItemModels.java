package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.king.PillagerKing;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * The Pillager King's axes: netherite axes with a haft about twice as long, switched in by the item_model component.
 * The texture is 32x32 drawn at the density of a 16x16 item, so the held model is scaled up twice the vanilla handheld
 * size; the grip sits where the vanilla axe's does after that scaling.
 */
final class PillagerKingItemModels {
	private static final Identifier AXE_MODEL = Eam.id("item/pillager_king_axe");
	/** Twice the vanilla handheld scale: the 32x32 texture keeps the pixel size of a 16x16 one. */
	private static final double SCALE = 1.7;

	private PillagerKingItemModels() {
	}

	static void generate(ItemModelGenerators generators) {
		generators.modelOutput.accept(AXE_MODEL, PillagerKingItemModels::axeModel);
	}

	static Map<Identifier, ClientItem> clientItems() {
		return Map.of(PillagerKing.AXE_MODEL, new ClientItem(ItemModelUtils.plainModel(AXE_MODEL), ClientItem.Properties.DEFAULT));
	}

	private static JsonObject axeModel() {
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:item/generated");
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", AXE_MODEL.toString());
		model.add("textures", textures);
		JsonObject display = new JsonObject();
		// The vanilla handheld poses, scaled up.
		display.add("thirdperson_righthand", FallenLordItemModels.transform(new double[] {0, -90, 55}, new double[] {0, 4.0, 0.5}, SCALE));
		display.add("thirdperson_lefthand", FallenLordItemModels.transform(new double[] {0, 90, -55}, new double[] {0, 4.0, 0.5}, SCALE));
		display.add("firstperson_righthand", FallenLordItemModels.transform(new double[] {0, -90, 25}, new double[] {1.13, 3.2, 1.13}, 1.36));
		display.add("firstperson_lefthand", FallenLordItemModels.transform(new double[] {0, 90, -25}, new double[] {1.13, 3.2, 1.13}, 1.36));
		model.add("display", display);
		return model;
	}
}
