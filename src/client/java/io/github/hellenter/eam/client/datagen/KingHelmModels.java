package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.trophy.KingHelmBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.resources.Identifier;

/**
 * The Pillager King's helm on a pike, built from the same boxes as the helm he wears ({@code PillagerKingHauberkModel}):
 * the shell, the dome, the ridge over the top and its tip at the brow, the visor with its pointed middle and the flare
 * over the nape, drawn at his head scale. Its texture is the helm corner of his own texture (rows 0 to 29 of the 128 wide
 * sheet, cut to 96 x 96 with the rest left clear), so each face reads the same pixels as on him. While the helm guards
 * against raids, embers glow in its eye slits. The model looks north and is turned for the other facings; a strip of red
 * cloth is tied round the pike under it.
 */
final class KingHelmModels {
	private static final Identifier MODEL = Eam.id("block/pillager_king_helm");
	private static final Identifier LIT_MODEL = Eam.id("block/pillager_king_helm_lit");
	private static final String HELM = "eam:block/pillager_king_helm";
	private static final String HELM_LIT = "eam:block/pillager_king_helm_lit";
	private static final String CLOTH = "eam:block/pillager_king_helm_cloth";
	private static final String PIKE = "eam:block/pillager_king_helm_pike";

	/** The helm texture's width in pixels: 16 model UV units across it. */
	private static final double TEXTURE_SIZE = 96.0;
	/** His head scale ({@code PillagerKingModel.HEAD_SCALE}). */
	private static final double SCALE = 0.8;
	/** Where the bottom of his head (the neck) sits on the pike. */
	private static final double NECK_Y = 17.0;

	/**
	 * The helm's boxes as in his model, x y z of the corner, then width, height, depth, then the texture offset. His
	 * model's y points down and his right is -x; the face is toward -z.
	 */
	private static final double[][] BOXES = {
			{-5.0, -10.0, -5.0, 10, 10, 10, 0, 0},
			{-4.0, -11.0, -4.0, 8, 1, 8, 40, 0},
			{-0.5, -12.0, -4.0, 1, 1, 8, 72, 0},
			{-0.5, -11.0, -5.5, 1, 3, 1, 90, 0},
			{-5.0, -8.0, -6.0, 10, 8, 1, 0, 20},
			{-1.0, -8.0, -7.0, 2, 8, 1, 22, 20},
			{-5.5, -1.5, 0.5, 11, 2, 5, 28, 20},
	};

	private KingHelmModels() {
	}

	static void generate(BlockModelGenerators generators) {
		generators.modelOutput.accept(MODEL, () -> model(HELM));
		generators.modelOutput.accept(LIT_MODEL, () -> model(HELM_LIT));
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.PILLAGER_KING_HELM)
				.with(BlockModelGenerators.createBooleanModelDispatch(KingHelmBlock.ENABLED, BlockModelGenerators.plainVariant(LIT_MODEL),
						BlockModelGenerators.plainVariant(MODEL)))
				.with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
	}

	private static JsonObject model(String helm) {
		JsonArray elements = new JsonArray();
		// The pike, driven into the ground, its top inside the helm.
		elements.add(plain(7, 0, 7, 9, 20, 9, "#pike", true));
		for (double[] box : BOXES) {
			elements.add(helmBox(box));
		}
		// A strip of red cloth tied round the pike under the helm, its end hanging loose.
		elements.add(plain(6.75, 13, 6.75, 9.25, 14.5, 9.25, "#cloth", true));
		elements.add(plain(9.25, 8, 8, 12, 14.5, 8, "#cloth", false));

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", helm);
		textures.addProperty("helm", helm);
		textures.addProperty("cloth", CLOTH);
		textures.addProperty("pike", PIKE);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/**
	 * One of his helm's boxes in block space, its faces cut from the texture the way an entity box lays them out: the top
	 * and bottom in the first {@code depth} rows, then his right side, the front, his left side and the back. Facing north,
	 * his right is east.
	 */
	private static JsonObject helmBox(double[] box) {
		double x = box[0];
		double y = box[1];
		double z = box[2];
		double w = box[3];
		double h = box[4];
		double d = box[5];
		double u = box[6];
		double v = box[7];
		JsonObject faces = new JsonObject();
		faces.add("up", face(u + d + w, v + d, u + d, v));
		faces.add("down", face(u + d + w, v, u + d + 2 * w, v + d));
		faces.add("east", face(u, v + d, u + d, v + d + h));
		faces.add("north", face(u + d, v + d, u + d + w, v + d + h));
		faces.add("west", face(u + d + w, v + d, u + 2 * d + w, v + d + h));
		faces.add("south", face(u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h));
		JsonObject element = new JsonObject();
		element.add("from", vector(8 - SCALE * (x + w), NECK_Y - SCALE * (y + h), 8 + SCALE * z));
		element.add("to", vector(8 - SCALE * x, NECK_Y - SCALE * y, 8 + SCALE * (z + d)));
		element.add("faces", faces);
		return element;
	}

	/** A face cut from the helm texture between two pixel corners. */
	private static JsonObject face(double u1, double v1, double u2, double v2) {
		double unit = 16.0 / TEXTURE_SIZE;
		JsonArray uv = new JsonArray();
		uv.add(u1 * unit);
		uv.add(v1 * unit);
		uv.add(u2 * unit);
		uv.add(v2 * unit);
		JsonObject face = new JsonObject();
		face.add("uv", uv);
		face.addProperty("texture", "#helm");
		return face;
	}

	/**
	 * A box with every face textured from the middle of its texture at one texel per pixel. A flat box (one size 0) gets
	 * only its two wide faces; without {@code ends} it has no top and bottom.
	 */
	private static JsonObject plain(double x1, double y1, double z1, double x2, double y2, double z2, String texture, boolean ends) {
		JsonObject faces = new JsonObject();
		addPlain(faces, "north", x2 - x1, y2 - y1, texture);
		addPlain(faces, "south", x2 - x1, y2 - y1, texture);
		addPlain(faces, "east", z2 - z1, y2 - y1, texture);
		addPlain(faces, "west", z2 - z1, y2 - y1, texture);
		if (ends) {
			addPlain(faces, "up", x2 - x1, z2 - z1, texture);
			addPlain(faces, "down", x2 - x1, z2 - z1, texture);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x1, y1, z1));
		element.add("to", vector(x2, y2, z2));
		element.add("faces", faces);
		return element;
	}

	private static void addPlain(JsonObject faces, String name, double w, double h, String texture) {
		if (w == 0 || h == 0) {
			return;
		}
		w = Math.min(w, 16);
		h = Math.min(h, 16);
		double u = (16 - w) / 2;
		double v = (16 - h) / 2;
		JsonArray uv = new JsonArray();
		uv.add(u);
		uv.add(v);
		uv.add(u + w);
		uv.add(v + h);
		JsonObject face = new JsonObject();
		face.add("uv", uv);
		face.addProperty("texture", texture);
		faces.add(name, face);
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}
}
