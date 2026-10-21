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
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Locked chest models: a plank box a little smaller than a block with iron bands, its lid seam two pixels below the top
 * of the front, and an iron lock standing out from the front across the seam. A double chest is two halves that meet
 * without a frame between them, its lock across the middle. Face north unrotated; the left half's partner is east of it.
 */
final class LockedChestModels {
	private static final String FRONT = "eam:block/locked_chest_front";
	private static final String SIDE = "eam:block/locked_chest_side";
	private static final String TOP = "eam:block/locked_chest_top";
	/** The half textures have the seam on their right edge, so a half that meets its partner on the left flips them. */
	private static final String FRONT_HALF = "eam:block/locked_chest_front_half";
	private static final String SIDE_HALF = "eam:block/locked_chest_side_half";
	private static final String TOP_HALF = "eam:block/locked_chest_top_half";

	private LockedChestModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Identifier single = Eam.id("block/locked_chest");
		Identifier left = Eam.id("block/locked_chest_left");
		Identifier right = Eam.id("block/locked_chest_right");
		generators.modelOutput.accept(single, () -> model(ChestType.SINGLE));
		generators.modelOutput.accept(left, () -> model(ChestType.LEFT));
		generators.modelOutput.accept(right, () -> model(ChestType.RIGHT));
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.LOCKED_CHEST)
				.with(PropertyDispatch.initial(LockedChestBlock.TYPE)
						.select(ChestType.SINGLE, BlockModelGenerators.plainVariant(single))
						.select(ChestType.LEFT, BlockModelGenerators.plainVariant(left))
						.select(ChestType.RIGHT, BlockModelGenerators.plainVariant(right)))
				.with(PropertyDispatch.modify(LockedChestBlock.FACING)
						.select(Direction.NORTH, BlockModelGenerators.NOP)
						.select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
						.select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
						.select(Direction.WEST, BlockModelGenerators.Y_ROT_270)));
		generators.registerSimpleItemModel(EamItems.LOCKED_CHEST, single);
	}

	private static JsonObject model(ChestType type) {
		JsonArray elements = new JsonArray();
		JsonObject faces = new JsonObject();
		JsonObject body;
		JsonObject lock;
		JsonObject lockFaces = new JsonObject();
		switch (type) {
			case SINGLE -> {
				body = element(1, 0, 1, 15, 14, 15);
				faces.add("north", face("#front", 1, 2, 15, 16));
				faces.add("south", face("#side", 1, 2, 15, 16));
				faces.add("east", face("#side", 1, 2, 15, 16));
				faces.add("west", face("#side", 1, 2, 15, 16));
				faces.add("up", face("#top", 1, 1, 15, 15));
				faces.add("down", culled(face("#top", 1, 1, 15, 15)));
				// The lock: the front texture paints it at x 7-9, y 5-9, right where this box stands out.
				lock = element(7, 7, 0, 9, 11, 1);
				lockFaces.add("north", face("#lock", 7, 5, 9, 9));
				lockFaces.add("east", face("#lock", 8, 5, 9, 9));
				lockFaces.add("west", face("#lock", 7, 5, 8, 9));
				lockFaces.add("up", face("#lock", 7, 5, 9, 6));
				lockFaces.add("down", face("#lock", 7, 8, 9, 9));
			}
			case LEFT -> {
				// The west half: open on its east side, where the right half goes on.
				body = element(1, 0, 1, 16, 14, 15);
				faces.add("north", face("#front_half", 16, 2, 1, 16));
				faces.add("south", face("#side_half", 1, 2, 16, 16));
				faces.add("west", face("#side", 1, 2, 15, 16));
				faces.add("up", face("#top_half", 1, 1, 16, 15));
				faces.add("down", culled(face("#top_half", 1, 1, 16, 15)));
				lock = element(15, 7, 0, 16, 11, 1);
				lockFaces.add("north", face("#lock", 8, 5, 9, 9));
				lockFaces.add("west", face("#lock", 7, 5, 8, 9));
				lockFaces.add("up", face("#lock", 7, 5, 8, 6));
				lockFaces.add("down", face("#lock", 7, 8, 8, 9));
			}
			default -> {
				body = element(0, 0, 1, 15, 14, 15);
				faces.add("north", face("#front_half", 1, 2, 16, 16));
				faces.add("south", face("#side_half", 16, 2, 1, 16));
				faces.add("east", face("#side", 1, 2, 15, 16));
				faces.add("up", face("#top_half", 16, 1, 1, 15));
				faces.add("down", culled(face("#top_half", 16, 1, 1, 15)));
				lock = element(0, 7, 0, 1, 11, 1);
				lockFaces.add("north", face("#lock", 7, 5, 8, 9));
				lockFaces.add("east", face("#lock", 8, 5, 9, 9));
				lockFaces.add("up", face("#lock", 8, 5, 9, 6));
				lockFaces.add("down", face("#lock", 8, 8, 9, 9));
			}
		}
		body.add("faces", faces);
		elements.add(body);
		lock.add("faces", lockFaces);
		elements.add(lock);

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", SIDE);
		textures.addProperty("lock", FRONT);
		textures.addProperty("side", SIDE);
		if (type == ChestType.SINGLE) {
			textures.addProperty("front", FRONT);
			textures.addProperty("top", TOP);
		} else {
			textures.addProperty("front_half", FRONT_HALF);
			textures.addProperty("side_half", SIDE_HALF);
			textures.addProperty("top_half", TOP_HALF);
		}
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonObject culled(JsonObject face) {
		face.addProperty("cullface", "down");
		return face;
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
