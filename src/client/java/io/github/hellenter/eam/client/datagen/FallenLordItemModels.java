package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Models for the Fallen Lord's spear and shield, which keep their vanilla items and switch looks through the
 * item_model component: a mithril spear head and a mithril shield instead of a wooden one.
 */
final class FallenLordItemModels {
	static final Identifier SPEAR = Eam.id("fallen_lord_spear");
	static final Identifier SHIELD = Eam.id("fallen_lord_shield");
	private static final Identifier SPEAR_MODEL = Eam.id("item/fallen_lord_spear");
	private static final Identifier SPEAR_IN_HAND_MODEL = Eam.id("item/fallen_lord_spear_in_hand");
	private static final Identifier SHIELD_MODEL = Eam.id("item/fallen_lord_shield");
	/** The shield texture has the vanilla shield layout (64x64); model UVs count in sixteenths. */
	private static final double UV = 16.0 / 64.0;

	private FallenLordItemModels() {
	}

	static void generate(ItemModelGenerators generators) {
		ModelTemplates.FLAT_ITEM.create(SPEAR_MODEL, TextureMapping.layer0(new Material(SPEAR_MODEL)), generators.modelOutput);
		ModelTemplates.SPEAR_IN_HAND.create(SPEAR_IN_HAND_MODEL, TextureMapping.layer0(new Material(SPEAR_IN_HAND_MODEL)), generators.modelOutput);
		generators.modelOutput.accept(SHIELD_MODEL, FallenLordItemModels::shieldModel);
	}

	/** Item definitions, keyed by the ids the item_model component names. */
	static Map<Identifier, ClientItem> clientItems() {
		return Map.of(
				SPEAR, new ClientItem(ItemModelGenerators.createFlatModelDispatch(ItemModelUtils.plainModel(SPEAR_MODEL),
						ItemModelUtils.plainModel(SPEAR_IN_HAND_MODEL)), new ClientItem.Properties(true, false, 1.95F)),
				SHIELD, new ClientItem(ItemModelUtils.plainModel(SHIELD_MODEL), ClientItem.Properties.DEFAULT));
	}

	/**
	 * The vanilla shield's plate and handle as model elements, placed where its special renderer draws them (the
	 * shield model flipped in y and z, centred on the item), so the vanilla shield's hand poses fit unchanged.
	 */
	private static JsonObject shieldModel() {
		JsonObject model = new JsonObject();
		model.addProperty("gui_light", "front");
		JsonObject textures = new JsonObject();
		textures.addProperty("shield", SHIELD_MODEL.toString());
		textures.addProperty("particle", SHIELD_MODEL.toString());
		model.add("textures", textures);
		JsonArray elements = new JsonArray();
		// Plate: front face shows the texture at (1, 1), back at (14, 1), 12x22 each.
		elements.add(element(new double[] {2, -3, 9}, new double[] {14, 19, 10}, Map.of(
				"south", new double[] {1, 1, 13, 23}, "north", new double[] {14, 1, 26, 23},
				"east", new double[] {13, 1, 14, 23}, "west", new double[] {0, 1, 1, 23},
				"up", new double[] {1, 0, 13, 1}, "down", new double[] {13, 0, 25, 1})));
		// Handle behind the plate.
		elements.add(element(new double[] {7, 5, 3}, new double[] {9, 11, 9}, Map.of(
				"south", new double[] {32, 6, 34, 12}, "north", new double[] {40, 6, 42, 12},
				"east", new double[] {34, 6, 40, 12}, "west", new double[] {26, 6, 32, 12},
				"up", new double[] {32, 0, 34, 6}, "down", new double[] {34, 0, 36, 6})));
		model.add("elements", elements);
		model.add("display", display());
		return model;
	}

	private static JsonObject element(double[] from, double[] to, Map<String, double[]> uvs) {
		JsonObject element = new JsonObject();
		element.add("from", vector(from));
		element.add("to", vector(to));
		JsonObject faces = new JsonObject();
		for (String name : new String[] {"north", "east", "south", "west", "up", "down"}) {
			double[] uv = uvs.get(name);
			JsonObject face = new JsonObject();
			face.add("uv", vector(new double[] {uv[0] * UV, uv[1] * UV, uv[2] * UV, uv[3] * UV}));
			face.addProperty("texture", "#shield");
			faces.add(name, face);
		}
		element.add("faces", faces);
		return element;
	}

	/** Copied from the vanilla shield item model. */
	private static JsonObject display() {
		JsonObject display = new JsonObject();
		display.add("thirdperson_righthand", transform(new double[] {0, 90, 0}, new double[] {10, 6, -4}, 1));
		display.add("thirdperson_lefthand", transform(new double[] {0, 90, 0}, new double[] {10, 6, 12}, 1));
		display.add("firstperson_righthand", transform(new double[] {0, 180, 5}, new double[] {-10, 1.75, -10}, 1.25));
		display.add("firstperson_lefthand", transform(new double[] {0, 180, 5}, new double[] {10, 0, -10}, 1.25));
		display.add("gui", transform(new double[] {15, -25, -5}, new double[] {2, 3, 0}, 0.65));
		display.add("fixed", transform(new double[] {0, 180, 0}, new double[] {-4.5, 4.5, -5}, 0.55));
		display.add("on_shelf", transform(new double[] {0, 0, 0}, new double[] {11, 18.5, 8.7}, 1.4));
		display.add("ground", transform(new double[] {0, 0, 0}, new double[] {2, 4, 2}, 0.25));
		return display;
	}

	private static JsonObject transform(double[] rotation, double[] translation, double scale) {
		JsonObject transform = new JsonObject();
		transform.add("rotation", vector(rotation));
		transform.add("translation", vector(translation));
		transform.add("scale", vector(new double[] {scale, scale, scale}));
		return transform;
	}

	private static JsonArray vector(double[] values) {
		JsonArray vector = new JsonArray();
		for (double value : values) {
			vector.add(value);
		}
		return vector;
	}
}
