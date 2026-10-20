package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Slingshots are flat sprites everywhere, like the bow: upright in the hand, and while drawn the pouch with its pebble
 * comes down over the handle in three stages, the band stretching after it, at the bow's thresholds.
 */
final class SlingshotModels {
	private static final int PULL_STAGES = 3;

	private SlingshotModels() {
	}

	static void generate(ItemModelGenerators generators) {
		generate(generators, EamItems.SLINGSHOT, "slingshot");
		// Same sprite on a metal fork with lighter metal tips.
		generate(generators, EamItems.COPPER_POWER_SLINGSHOT, "copper_power_slingshot");
		generate(generators, EamItems.SILVER_POWER_SLINGSHOT, "silver_power_slingshot");
		generate(generators, EamItems.IRON_POWER_SLINGSHOT, "iron_power_slingshot");
		generate(generators, EamItems.ANCIENT_METAL_POWER_SLINGSHOT, "ancient_metal_power_slingshot");
		generate(generators, EamItems.MITHRIL_POWER_SLINGSHOT, "mithril_power_slingshot");
		generate(generators, EamItems.ADAMANTIUM_POWER_SLINGSHOT, "adamantium_power_slingshot");
	}

	private static void generate(ItemModelGenerators generators, Item item, String name) {
		Identifier idle = Eam.id("item/" + name);
		generators.modelOutput.accept(idle, () -> model("minecraft:item/generated", idle, true));
		Identifier[] pulling = new Identifier[PULL_STAGES];
		for (int stage = 0; stage < PULL_STAGES; stage++) {
			pulling[stage] = Eam.id("item/" + name + "_pulling_" + stage);
			Identifier texture = pulling[stage];
			generators.modelOutput.accept(texture, () -> model(idle.toString(), texture, false));
		}
		// Vanilla's bow thresholds: the first stage at once, the second at 65% of a full draw, the third at 90%.
		generators.itemModelOutput.accept(item, ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
				ItemModelUtils.rangeSelect(new UseDuration(false), 0.05F, ItemModelUtils.plainModel(pulling[0]),
						ItemModelUtils.override(ItemModelUtils.plainModel(pulling[1]), 0.65F),
						ItemModelUtils.override(ItemModelUtils.plainModel(pulling[2]), 0.9F)),
				ItemModelUtils.plainModel(idle)));
	}

	/** A sprite model on {@code parent}; the idle one also carries the hand poses, which the drawn stages inherit. */
	private static JsonObject model(String parent, Identifier texture, boolean withDisplay) {
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", texture.toString());
		JsonObject model = new JsonObject();
		model.addProperty("parent", parent);
		model.add("textures", textures);
		if (withDisplay) {
			model.add("display", display());
		}
		return model;
	}

	/** Hand poses; the inventory, ground and frames keep item/generated's. */
	private static JsonObject display() {
		JsonObject display = new JsonObject();
		// Held out in front, fork up.
		display.add("thirdperson_righthand", FallenLordItemModels.transform(new double[] {0, 0, 0}, new double[] {0, 2, 1}, 0.85));
		display.add("thirdperson_lefthand", FallenLordItemModels.transform(new double[] {0, 0, 0}, new double[] {0, 2, 1}, 0.85));
		// Upright, facing the player, low enough that the drawn pouch stays on screen.
		display.add("firstperson_righthand", FallenLordItemModels.transform(new double[] {-10, 0, 0}, new double[] {0, 2.5, -3}, 0.5));
		display.add("firstperson_lefthand", FallenLordItemModels.transform(new double[] {-10, 0, 0}, new double[] {0, 2.5, -3}, 0.5));
		return display;
	}
}
