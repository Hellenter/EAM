package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.dryer.DryerBlock;
import io.github.hellenter.eam.content.dryer.DryerBlockEntity;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Food dryer model, like a drying rack in the woods: at each end two sticks lean together and cross like an A, a pole
 * lies in the crossings and four small hooks hang from it on twine, two in front and two lower behind. The food on them
 * is drawn by {@code FoodDryerRenderer} from the same hook positions. Faces south unrotated.
 */
final class FoodDryerModels {
	private static final String STICK = "eam:block/camp_fire_stick";
	private static final String TWINE = "eam:block/food_dryer_twine";
	private static final String HOOK = "eam:block/food_dryer_hook";

	/** Where the legs stand, from the front and back edge. */
	private static final double FOOT_Z = 2.5;
	/** The legs cross under the pole's middle. */
	private static final double CROSS_Y = DryerBlockEntity.POLE_BOTTOM + 1.0;
	/** Legs run past the crossing, so their ends stick out above the pole. */
	private static final double LEG_OVERSHOOT = 2.0;

	private FoodDryerModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Identifier model = Eam.id("block/food_dryer");
		generators.modelOutput.accept(model, FoodDryerModels::model);
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.FOOD_DRYER, BlockModelGenerators.plainVariant(model))
				.with(PropertyDispatch.modify(DryerBlock.FACING)
						.select(Direction.SOUTH, BlockModelGenerators.NOP)
						.select(Direction.WEST, BlockModelGenerators.Y_ROT_90)
						.select(Direction.NORTH, BlockModelGenerators.Y_ROT_180)
						.select(Direction.EAST, BlockModelGenerators.Y_ROT_270)));
		generators.registerSimpleItemModel(EamItems.FOOD_DRYER, model);
	}

	private static JsonObject model() {
		JsonArray elements = new JsonArray();
		// Front and back leg of each A sit side by side, so their faces never share a plane.
		for (double x : new double[] {1.0, 13.0}) {
			elements.add(leg(x, FOOT_Z, 1));
			elements.add(leg(x + 0.25 * (x < 8 ? 1 : -1), 16.0 - FOOT_Z, -1));
		}
		elements.add(stick(box(0, DryerBlockEntity.POLE_BOTTOM, 7, 16, DryerBlockEntity.POLE_BOTTOM + 2, 9), Direction.Axis.X));
		for (int slot = 0; slot < DryerBlockEntity.SLOTS; slot++) {
			hook(elements, slot);
		}

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", STICK);
		textures.addProperty("stick", STICK);
		textures.addProperty("twine", TWINE);
		textures.addProperty("hook", HOOK);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/**
	 * A 2x2 leg standing at ({@code x}, {@code footZ}) and leaning toward the middle ({@code toward} is +1 for a front
	 * leg, -1 for a back one) so it reaches the centre line at the crossing height, then runs on a little.
	 */
	private static JsonObject leg(double x, double footZ, int toward) {
		double run = 8.0 - footZ;
		double tilt = Math.toDegrees(Math.atan2(Math.abs(run), CROSS_Y));
		double length = round(Math.hypot(run, CROSS_Y) + LEG_OVERSHOOT);
		JsonObject element = stick(box(x, 0, footZ - 1, x + 2, length, footZ + 1), Direction.Axis.Y);
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(x + 1, 0, footZ));
		// Turning about x tips the top toward +z for a positive angle.
		rotation.addProperty("x", round(tilt * toward));
		rotation.addProperty("y", 0);
		rotation.addProperty("z", 0);
		element.add("rotation", rotation);
		return element;
	}

	/** Twine from the pole down to a small bent-twig hook: a shank, the bend the food hangs on and the tip turned up. */
	private static void hook(JsonArray elements, int slot) {
		double x = DryerBlockEntity.HOOK_X[slot];
		double z = DryerBlockEntity.HOOK_Z[slot];
		double top = DryerBlockEntity.POLE_BOTTOM - DryerBlockEntity.HOOK_DROP[slot];
		double bottom = DryerBlockEntity.hookBottom(slot);
		elements.add(textured(box(x - 0.25, top, z - 0.25, x + 0.25, DryerBlockEntity.POLE_BOTTOM, z + 0.25), "#twine", 1 + slot * 4));
		elements.add(textured(box(x - 0.25, bottom + 0.5, z - 0.25, x + 0.25, top, z + 0.25), "#hook", 1));
		elements.add(textured(box(x - 0.25, bottom, z - 0.25, x + 1.0, bottom + 0.5, z + 0.25), "#hook", 5));
		elements.add(textured(box(x + 0.5, bottom + 0.5, z - 0.25, x + 1.0, bottom + 1.0, z + 0.25), "#hook", 9));
	}

	/** Every face shows a short strip of row {@code row} of {@code texture}. */
	private static JsonObject textured(JsonObject element, String texture, int row) {
		JsonObject faces = element.getAsJsonObject("faces");
		for (String name : faces.keySet()) {
			JsonObject face = faces.getAsJsonObject(name);
			face.addProperty("texture", texture);
			face.add("uv", vector4(6, row, 8, row + 1));
		}
		return element;
	}

	private static double round(double value) {
		return Math.round(value * 100.0) / 100.0;
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
			// Sticks longer than the texture stretch it a little.
			face.add("uv", vector4(7, Math.max(0.0, 16 - length), 9, 16));
			// The strip runs up the texture; a face whose long edge is horizontal on screen turns it.
			boolean lying = axis != Direction.Axis.Y && !(axis == Direction.Axis.Z && direction.getAxis() == Direction.Axis.Y);
			if (lying) {
				face.addProperty("rotation", 90);
			}
		}
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
