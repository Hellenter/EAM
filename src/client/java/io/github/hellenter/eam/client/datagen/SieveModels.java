package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.sieve.SieveBlock;
import io.github.hellenter.eam.content.sieve.SieveLogic;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Sieve models: four stick legs, a frame of planks and a mesh of leather thread that tears as the sieve wears, plus a
 * heap of gravel on the mesh that shrinks while it sifts. Multipart: the wear picks the sieve, the gravel left picks
 * the heap.
 */
final class SieveModels {
	private static final String LEG = "eam:block/camp_fire_stick";
	private static final String FRAME = "minecraft:block/oak_planks";
	private static final String GRAVEL = "minecraft:block/gravel";
	/** Mesh textures by {@link SieveBlock#WEAR}: whole, worn, torn. */
	private static final String[] MESH = {"eam:block/sieve_mesh", "eam:block/sieve_mesh_worn", "eam:block/sieve_mesh_torn"};

	private static final double LEG_TOP = 11.0;
	private static final double FRAME_TOP = 14.0;
	private static final double MESH_Y = 12.5;
	/** Gravel heap height per {@link SieveBlock#GRAVEL} step, in pixels. */
	private static final double GRAVEL_STEP = 0.5;

	private SieveModels() {
	}

	static void generate(BlockModelGenerators generators) {
		MultiPartGenerator blockState = MultiPartGenerator.multiPart(EamBlocks.SIEVE);
		for (int wear = 0; wear <= SieveLogic.MAX_WEAR; wear++) {
			String mesh = MESH[wear];
			Identifier model = Eam.id("block/sieve" + (wear == 0 ? "" : "_wear_" + wear));
			generators.modelOutput.accept(model, () -> sieveModel(mesh));
			blockState.with(BlockModelGenerators.condition().term(SieveBlock.WEAR, wear), BlockModelGenerators.plainVariant(model));
		}
		for (int gravel = 1; gravel <= SieveLogic.MAX_GRAVEL; gravel++) {
			double height = gravel * GRAVEL_STEP;
			Identifier model = Eam.id("block/sieve_gravel_" + gravel);
			generators.modelOutput.accept(model, () -> gravelModel(height));
			blockState.with(BlockModelGenerators.condition().term(SieveBlock.GRAVEL, gravel), BlockModelGenerators.plainVariant(model));
		}
		generators.blockStateOutput.accept(blockState);
		generators.registerSimpleItemModel(EamItems.SIEVE, Eam.id("block/sieve"));
	}

	private static JsonObject sieveModel(String mesh) {
		JsonArray elements = new JsonArray();
		for (double[] corner : new double[][] {{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
			elements.add(leg(corner[0], corner[1]));
		}
		elements.add(box(0, LEG_TOP, 0, 16, FRAME_TOP, 2, "#frame"));
		elements.add(box(0, LEG_TOP, 14, 16, FRAME_TOP, 16, "#frame"));
		elements.add(box(0, LEG_TOP, 2, 2, FRAME_TOP, 14, "#frame"));
		elements.add(box(14, LEG_TOP, 2, 16, FRAME_TOP, 14, "#frame"));
		elements.add(meshElement());

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", FRAME);
		textures.addProperty("leg", LEG);
		textures.addProperty("frame", FRAME);
		textures.addProperty("mesh", mesh);
		return model(textures, elements);
	}

	/** A 2x2 stick from the ground up to the frame; each side shows a strip of the stick texture. */
	private static JsonObject leg(double x, double z) {
		JsonObject element = box(x, 0, z, x + 2, LEG_TOP, z + 2, "#leg");
		JsonObject faces = element.getAsJsonObject("faces");
		for (String name : faces.keySet()) {
			boolean end = name.equals("up") || name.equals("down");
			faces.getAsJsonObject(name).add("uv", vector4(7, end ? 7 : 16 - LEG_TOP, 9, end ? 9 : 16));
		}
		return element;
	}

	/** The mesh is a flat sheet inside the frame, seen from above and below. */
	private static JsonObject meshElement() {
		JsonObject faces = new JsonObject();
		for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN}) {
			JsonObject face = new JsonObject();
			face.add("uv", vector4(2, 2, 14, 14));
			face.addProperty("texture", "#mesh");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(2, MESH_Y, 2));
		element.add("to", vector(14, MESH_Y, 14));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject gravelModel(double height) {
		JsonArray elements = new JsonArray();
		elements.add(box(2, MESH_Y, 2, 14, MESH_Y + height, 14, "#gravel"));
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", GRAVEL);
		textures.addProperty("gravel", GRAVEL);
		return model(textures, elements);
	}

	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, String texture) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			face.addProperty("texture", texture);
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x1, y1, z1));
		element.add("to", vector(x2, y2, z2));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject model(JsonObject textures, JsonArray elements) {
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}

	private static JsonArray vector4(double a, double b, double c, double d) {
		JsonArray vector = new JsonArray();
		vector.add(a);
		vector.add(b);
		vector.add(c);
		vector.add(d);
		return vector;
	}
}
