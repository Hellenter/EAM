package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.resources.Identifier;

/**
 * The cursed horse skull set on the ground: the cranium at the back, the brow with the eye sockets, the long muzzle, the
 * cheekbones, the lower jaw lying loose under it and two short curved horns. Each box takes its faces from the 128x128
 * texture the way an entity box lays them out, two texels to a pixel (painted by
 * {@code tools/ghost-horse/ghost_horse_textures.py}, which uses the same boxes). The model looks north and is turned for
 * the other facings; the item shows the same model.
 */
final class CursedHorseSkullModels {
	private static final Identifier MODEL = Eam.id("block/cursed_horse_skull");
	private static final String TEXTURE = "eam:block/cursed_horse_skull";
	/** Texels to a model pixel. */
	private static final double TEXELS = 2.0;
	/** The texture's width in texels: 16 model UV units across it. */
	private static final double TEXTURE_SIZE = 128.0;

	/** x y z of the corner (block pixels, y up, the muzzle toward -z), width, height, depth, then the texel offset. */
	private static final double[][] BOXES = {
			{4, 0, 9, 8, 6, 6, 0, 0},       // cranium
			{5, 1, 5, 6, 5, 4, 56, 0},      // brow and eye sockets
			{6, 1, 0, 4, 4, 5, 0, 24},      // muzzle
			{6, 0, 1, 4, 1, 8, 36, 24},     // lower jaw
			{4, 1, 6, 8, 2, 2, 28, 48},     // cheekbones
			{4, 5, 11, 2, 3, 2, 0, 48},     // left horn
			{10, 5, 11, 2, 3, 2, 0, 48},    // right horn
			{3, 7, 12, 1, 3, 2, 16, 48},    // left horn tip, curling out
			{12, 7, 12, 1, 3, 2, 16, 48},   // right horn tip
	};

	private CursedHorseSkullModels() {
	}

	static void generate(BlockModelGenerators generators) {
		generators.modelOutput.accept(MODEL, CursedHorseSkullModels::model);
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.CURSED_HORSE_SKULL, BlockModelGenerators.plainVariant(MODEL))
				.with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
		generators.registerSimpleItemModel(EamItems.CURSED_HORSE_SKULL, MODEL);
	}

	private static JsonObject model() {
		JsonArray elements = new JsonArray();
		for (double[] box : BOXES) {
			elements.add(element(box));
		}
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", TEXTURE);
		textures.addProperty("skull", TEXTURE);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/**
	 * A box with its faces cut from the texture as an entity box lays them out: the top and bottom in the first
	 * {@code depth} rows (the top's first row toward the muzzle), then the west side, the front, the east side and the
	 * back, each as seen from outside.
	 */
	private static JsonObject element(double[] box) {
		double w = box[3] * TEXELS;
		double h = box[4] * TEXELS;
		double d = box[5] * TEXELS;
		double u = box[6];
		double v = box[7];
		JsonObject faces = new JsonObject();
		faces.add("up", face(u + d, v, u + d + w, v + d));
		faces.add("down", face(u + d + w, v + d, u + d + 2 * w, v));
		faces.add("west", face(u, v + d, u + d, v + d + h));
		faces.add("north", face(u + d, v + d, u + d + w, v + d + h));
		faces.add("east", face(u + d + w, v + d, u + 2 * d + w, v + d + h));
		faces.add("south", face(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h));
		JsonObject element = new JsonObject();
		element.add("from", vector(box[0], box[1], box[2]));
		element.add("to", vector(box[0] + box[3], box[1] + box[4], box[2] + box[5]));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject face(double u1, double v1, double u2, double v2) {
		double unit = 16.0 / TEXTURE_SIZE;
		JsonArray uv = new JsonArray();
		uv.add(u1 * unit);
		uv.add(v1 * unit);
		uv.add(u2 * unit);
		uv.add(v2 * unit);
		JsonObject face = new JsonObject();
		face.add("uv", uv);
		face.addProperty("texture", "#skull");
		return face;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}
}
