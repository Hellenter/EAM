package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.lockedchest.LockedChestBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Locked chest model: a plank box a little smaller than a block with iron bands, its lid seam two pixels below the top
 * of the front, and an iron lock standing out from the front across the seam. Faces north unrotated.
 */
final class LockedChestModels {
	private static final String FRONT = "eam:block/locked_chest_front";
	private static final String SIDE = "eam:block/locked_chest_side";
	private static final String TOP = "eam:block/locked_chest_top";

	private LockedChestModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Identifier model = Eam.id("block/locked_chest");
		generators.modelOutput.accept(model, LockedChestModels::model);
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.LOCKED_CHEST, BlockModelGenerators.plainVariant(model))
				.with(PropertyDispatch.modify(LockedChestBlock.FACING)
						.select(Direction.NORTH, BlockModelGenerators.NOP)
						.select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
						.select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
						.select(Direction.WEST, BlockModelGenerators.Y_ROT_270)));
		generators.registerSimpleItemModel(EamItems.LOCKED_CHEST, model);
	}

	private static JsonObject model() {
		JsonArray elements = new JsonArray();
		JsonObject body = element(1, 0, 1, 15, 14, 15);
		JsonObject faces = new JsonObject();
		faces.add("north", face("#front", 1, 2, 15, 16));
		faces.add("south", face("#side", 1, 2, 15, 16));
		faces.add("east", face("#side", 1, 2, 15, 16));
		faces.add("west", face("#side", 1, 2, 15, 16));
		faces.add("up", face("#top", 1, 1, 15, 15));
		JsonObject down = face("#top", 1, 1, 15, 15);
		down.addProperty("cullface", "down");
		faces.add("down", down);
		body.add("faces", faces);
		elements.add(body);
		// The lock: the front texture paints it at x 7-9, y 5-9, right where this box stands out.
		JsonObject lock = element(7, 7, 0, 9, 11, 1);
		JsonObject lockFaces = new JsonObject();
		lockFaces.add("north", face("#front", 7, 5, 9, 9));
		lockFaces.add("east", face("#front", 8, 5, 9, 9));
		lockFaces.add("west", face("#front", 7, 5, 8, 9));
		lockFaces.add("up", face("#front", 7, 5, 9, 6));
		lockFaces.add("down", face("#front", 7, 8, 9, 9));
		lock.add("faces", lockFaces);
		elements.add(lock);

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", SIDE);
		textures.addProperty("front", FRONT);
		textures.addProperty("side", SIDE);
		textures.addProperty("top", TOP);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonObject element(double x0, double y0, double z0, double x1, double y1, double z1) {
		JsonObject element = new JsonObject();
		element.add("from", vector(x0, y0, z0));
		element.add("to", vector(x1, y1, z1));
		return element;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}

	private static JsonObject face(String texture, double u0, double v0, double u1, double v1) {
		JsonObject face = new JsonObject();
		JsonArray uv = new JsonArray();
		uv.add(u0);
		uv.add(v0);
		uv.add(u1);
		uv.add(v1);
		face.add("uv", uv);
		face.addProperty("texture", texture);
		return face;
	}
}
