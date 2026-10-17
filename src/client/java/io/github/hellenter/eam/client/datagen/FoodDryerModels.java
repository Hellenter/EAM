package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.dryer.DryerBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Food dryer model: four stick legs, a frame of sticks on top and rows of twine strung across it, on which the food lies
 * (drawn by {@code FoodDryerRenderer}).
 */
final class FoodDryerModels {
	private static final String STICK = "eam:block/camp_fire_stick";
	private static final String TWINE = "eam:block/food_dryer_twine";

	private static final double FRAME_BOTTOM = DryerBlock.RACK_HEIGHT - 1.0;
	private static final double FRAME_TOP = DryerBlock.RACK_HEIGHT + 1.0;
	/** Where the twine runs across the frame, along x, by its z. */
	private static final double[] TWINE_ROWS = {4.5, 7.0, 9.5, 12.0};

	private FoodDryerModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Identifier model = Eam.id("block/food_dryer");
		generators.modelOutput.accept(model, FoodDryerModels::model);
		generators.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(EamBlocks.FOOD_DRYER, BlockModelGenerators.plainVariant(model)));
		generators.registerSimpleItemModel(EamItems.FOOD_DRYER, model);
	}

	private static JsonObject model() {
		JsonArray elements = new JsonArray();
		for (double[] corner : new double[][] {{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
			elements.add(stick(box(corner[0], 0, corner[1], corner[0] + 2, FRAME_BOTTOM, corner[1] + 2), Direction.Axis.Y));
		}
		// The long rails the twine is tied to run along x; the short ones along z sit between them.
		elements.add(stick(box(0.5, FRAME_BOTTOM, 1, 15.5, FRAME_TOP, 3), Direction.Axis.X));
		elements.add(stick(box(0.5, FRAME_BOTTOM, 13, 15.5, FRAME_TOP, 15), Direction.Axis.X));
		elements.add(stick(box(1, FRAME_BOTTOM, 3, 3, FRAME_TOP, 13), Direction.Axis.Z));
		elements.add(stick(box(13, FRAME_BOTTOM, 3, 15, FRAME_TOP, 13), Direction.Axis.Z));
		for (int row = 0; row < TWINE_ROWS.length; row++) {
			elements.add(twine(TWINE_ROWS[row], row));
		}

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", STICK);
		textures.addProperty("stick", STICK);
		textures.addProperty("twine", TWINE);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/**
	 * A stick running along {@code axis}: each long side is a 2 pixel wide strip of the stick texture, the ends a 2x2
	 * patch. Sides that see the stick lying turn the strip by 90 degrees.
	 */
	private static JsonObject stick(JsonObject element, Direction.Axis axis) {
		JsonArray from = element.getAsJsonArray("from");
		JsonArray to = element.getAsJsonArray("to");
		int index = axis.ordinal();
		double length = to.get(index).getAsDouble() - from.get(index).getAsDouble();
		JsonObject faces = element.getAsJsonObject("faces");
		for (Direction direction : Direction.values()) {
			JsonObject face = faces.getAsJsonObject(direction.getSerializedName());
			face.addProperty("texture", "#stick");
			if (direction.getAxis() == axis) {
				face.add("uv", vector4(7, 7, 9, 9));
				continue;
			}
			face.add("uv", vector4(7, 16 - length, 9, 16));
			// The strip runs up the texture; a face whose long edge is horizontal on screen turns it.
			boolean lying = axis != Direction.Axis.Y && !(axis == Direction.Axis.Z && direction.getAxis() == Direction.Axis.Y);
			if (lying) {
				face.addProperty("rotation", 90);
			}
		}
		return element;
	}

	/** A strand of twine just above the frame, seen from above and below; each strand uses its own row of the texture. */
	private static JsonObject twine(double z, int row) {
		JsonObject faces = new JsonObject();
		for (Direction direction : new Direction[] {Direction.UP, Direction.DOWN}) {
			JsonObject face = new JsonObject();
			face.add("uv", vector4(1, row * 4 + 1, 15, row * 4 + 2));
			face.addProperty("texture", "#twine");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(1, FRAME_TOP - 0.25, z));
		element.add("to", vector(15, FRAME_TOP - 0.25, z + 1));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			faces.add(direction.getSerializedName(), new JsonObject());
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x1, y1, z1));
		element.add("to", vector(x2, y2, z2));
		element.add("faces", faces);
		return element;
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
