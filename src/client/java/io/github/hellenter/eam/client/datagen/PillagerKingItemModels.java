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
 * The texture is 32x32 drawn at the density of a 16x16 item, so the held model is scaled up past the vanilla handheld
 * size. His model's {@code translateToHand} already ends at the fist and the item layer then moves 10 down the arm, so
 * the held pose pulls the axe back up: the fist closes on the end of the haft and the head points straight out from the
 * forearm, its edge down. The game negates the left-hand rotation, so the left pose is written with the right one's
 * turns reversed: the axe lies across the arm, so the same turns give its mirror image.
 */
final class PillagerKingItemModels {
	private static final Identifier AXE_MODEL = Eam.id("item/pillager_king_axe");
	/** One and a half times the vanilla handheld scale, in pixels of a 16x16 texture. */
	private static final double SCALE = 1.275;

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
		// The fist on the end of the haft, the head straight out from the forearm, the edge down.
		display.add("thirdperson_righthand", FallenLordItemModels.transform(new double[] {0, 90, 35}, new double[] {-0.5, 3.6, 10.2}, SCALE));
		display.add("thirdperson_lefthand", FallenLordItemModels.transform(new double[] {0, -90, -35}, new double[] {-0.5, 3.6, 10.2}, SCALE));
		display.add("firstperson_righthand", FallenLordItemModels.transform(new double[] {0, -90, 25}, new double[] {1.13, 3.2, 1.13}, 1.36));
		display.add("firstperson_lefthand", FallenLordItemModels.transform(new double[] {0, 90, -25}, new double[] {1.13, 3.2, 1.13}, 1.36));
		model.add("display", display);
		return model;
	}
}
