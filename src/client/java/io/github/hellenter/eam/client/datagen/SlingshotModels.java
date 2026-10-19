package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The slingshot is a flat icon in the inventory, on the ground and in frames, and a 3D model in the hand: a forked
 * branch with a leather-wrapped grip, a sinew band tied round both prongs and a leather pouch. While drawn the pouch
 * holds a pebble and comes back in three stages, the band stretching after it, at the bow's thresholds.
 */
final class SlingshotModels {
	/** Pouch distance behind the fork, idle first, then the three draw stages. */
	private static final double[] PULL = {0.0, 3.0, 5.5, 8.0};
	/** Prong lean from upright, degrees. */
	private static final double ARM_TILT = 30.0;
	/** Centre of the fork in x and z, and the height where the arms leave the handle. */
	private static final double CENTRE = 8.0;
	private static final double CROTCH = 7.5;
	private static final double ARM_LENGTH = 5.5;
	private static final double ARM_HALF = 0.875;
	/** Height of the band where it is tied to the prongs, and half its cross-section. */
	private static final double BAND_Y = 14.0;
	private static final double BAND_HALF_HEIGHT = 0.375;
	private static final double BAND_HALF_WIDTH = 0.25;
	private static final double POUCH_HALF_WIDTH = 1.0;

	/** Texture regions (u, v, width, height) of slingshot_model.png. */
	private static final double[] BARK = {0, 0, 4, 16};
	private static final double[] END_GRAIN = {4, 0, 2, 2};
	private static final double[] WRAP = {6, 0, 4, 8};
	private static final double[] BAND = {10, 0, 6, 2};
	private static final double[] POUCH = {10, 2, 4, 4};
	private static final double[] PEBBLE = {10, 6, 2, 2};

	private SlingshotModels() {
	}

	static void generate(ItemModelGenerators generators) {
		generate(generators, EamItems.SLINGSHOT, "slingshot");
		// Same shape on a metal fork with lighter metal tips.
		generate(generators, EamItems.COPPER_POWER_SLINGSHOT, "copper_power_slingshot");
		generate(generators, EamItems.SILVER_POWER_SLINGSHOT, "silver_power_slingshot");
		generate(generators, EamItems.IRON_POWER_SLINGSHOT, "iron_power_slingshot");
		generate(generators, EamItems.ANCIENT_METAL_POWER_SLINGSHOT, "ancient_metal_power_slingshot");
		generate(generators, EamItems.MITHRIL_POWER_SLINGSHOT, "mithril_power_slingshot");
		generate(generators, EamItems.ADAMANTIUM_POWER_SLINGSHOT, "adamantium_power_slingshot");
	}

	private static void generate(ItemModelGenerators generators, Item item, String name) {
		Identifier icon = ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item),
				generators.modelOutput);
		String texture = "eam:item/" + name + "_model";
		Identifier[] inHand = new Identifier[PULL.length];
		for (int stage = 0; stage < PULL.length; stage++) {
			inHand[stage] = Eam.id(stage == 0 ? "item/" + name + "_in_hand" : "item/" + name + "_in_hand_pulling_" + (stage - 1));
			double pull = PULL[stage];
			generators.modelOutput.accept(inHand[stage], () -> model(pull, texture));
		}
		// Vanilla's bow thresholds: the first stage at once, the second at 65% of a full draw, the third at 90%.
		generators.itemModelOutput.accept(item, ItemModelGenerators.createFlatModelDispatch(ItemModelUtils.plainModel(icon),
				ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
						ItemModelUtils.rangeSelect(new UseDuration(false), 0.05F, ItemModelUtils.plainModel(inHand[1]),
								ItemModelUtils.override(ItemModelUtils.plainModel(inHand[2]), 0.65F),
								ItemModelUtils.override(ItemModelUtils.plainModel(inHand[3]), 0.9F)),
						ItemModelUtils.plainModel(inHand[0]))));
	}

	/** The slingshot with its pouch {@code pull} pixels behind the fork (towards +z), painted with {@code texture}; 0 is the band hanging straight. */
	private static JsonObject model(double pull, String texture) {
		JsonArray elements = new JsonArray();
		// Handle, with the leather grip wound round its middle.
		elements.add(box(CENTRE - 1, 0, CENTRE - 1, CENTRE + 1, CROTCH + 0.5, CENTRE + 1, BARK, 0));
		elements.add(box(CENTRE - 1.25, 1, CENTRE - 1.25, CENTRE + 1.25, 5, CENTRE + 1.25, WRAP, 0));
		for (int side = -1; side <= 1; side += 2) {
			// The arm leans out from the crotch; the prong above it stands upright.
			JsonObject arm = box(CENTRE - ARM_HALF, CROTCH - 0.5, CENTRE - ARM_HALF, CENTRE + ARM_HALF, CROTCH + ARM_LENGTH, CENTRE + ARM_HALF,
					BARK, side < 0 ? 3 : 6);
			arm.add("rotation", rotation(CENTRE, CROTCH, CENTRE, "z", -side * ARM_TILT));
			elements.add(arm);
			double prongX = CENTRE + side * ARM_LENGTH * Math.sin(Math.toRadians(ARM_TILT));
			double prongBottom = CROTCH + ARM_LENGTH * Math.cos(Math.toRadians(ARM_TILT)) - 0.75;
			elements.add(box(prongX - ARM_HALF, prongBottom, CENTRE - ARM_HALF, prongX + ARM_HALF, BAND_Y + 1.5, CENTRE + ARM_HALF,
					BARK, side < 0 ? 9 : 11));
			// The band's knot round the prong.
			elements.add(box(prongX - ARM_HALF - 0.25, BAND_Y - 0.5, CENTRE - ARM_HALF - 0.25, prongX + ARM_HALF + 0.25, BAND_Y + 0.5,
					CENTRE + ARM_HALF + 0.25, BAND, 0));
			elements.add(bandStrip(prongX - side * ARM_HALF, CENTRE + side * POUCH_HALF_WIDTH, pull));
		}
		double pouchZ = CENTRE + pull;
		if (pull == 0) {
			elements.add(box(CENTRE - POUCH_HALF_WIDTH, BAND_Y - 0.75, pouchZ - 0.4, CENTRE + POUCH_HALF_WIDTH, BAND_Y + 0.75, pouchZ + 0.4,
					POUCH, 0));
		} else {
			// The pouch cups a pebble on the side facing the fork.
			elements.add(box(CENTRE - POUCH_HALF_WIDTH, BAND_Y - 0.9, pouchZ - 0.25, CENTRE + POUCH_HALF_WIDTH, BAND_Y + 0.9, pouchZ + 0.5,
					POUCH, 0));
			elements.add(box(CENTRE - 0.7, BAND_Y - 0.7, pouchZ - 1.4, CENTRE + 0.7, BAND_Y + 0.7, pouchZ, PEBBLE, 0));
		}

		JsonObject textures = new JsonObject();
		textures.addProperty("all", texture);
		textures.addProperty("particle", texture);
		JsonObject model = new JsonObject();
		model.add("textures", textures);
		model.add("elements", elements);
		model.add("display", display());
		return model;
	}

	/**
	 * One side of the band, from the inner face of a prong at x {@code fromX} to the pouch's edge at x {@code toX}
	 * {@code pull} pixels behind: a thin box along z, turned about y to point at the pouch.
	 */
	private static JsonObject bandStrip(double fromX, double toX, double pull) {
		double dx = toX - fromX;
		double length = Math.hypot(dx, pull);
		JsonObject strip = box(fromX - BAND_HALF_WIDTH, BAND_Y - BAND_HALF_HEIGHT, CENTRE, fromX + BAND_HALF_WIDTH, BAND_Y + BAND_HALF_HEIGHT,
				CENTRE + length, BAND, 0);
		// A turn about y by a carries +z to (sin a, 0, cos a).
		strip.add("rotation", rotation(fromX, BAND_Y, CENTRE, "y", Math.toDegrees(Math.atan2(dx, pull))));
		return strip;
	}

	/** Hand poses; the inventory, ground and frames use the flat icon instead. */
	private static JsonObject display() {
		JsonObject display = new JsonObject();
		// Held out in front: the fork up when the arm is raised to shoot, pointing ahead while it hangs.
		display.add("thirdperson_righthand", FallenLordItemModels.transform(new double[] {0, 0, 0}, new double[] {0, 2, 1}, 0.85));
		display.add("thirdperson_lefthand", FallenLordItemModels.transform(new double[] {0, 0, 0}, new double[] {0, 2, 1}, 0.85));
		// Upright and leaning a little away, low enough that the drawn pouch stays on screen.
		display.add("firstperson_righthand", FallenLordItemModels.transform(new double[] {-10, 0, 0}, new double[] {0, 2.5, -3}, 0.5));
		display.add("firstperson_lefthand", FallenLordItemModels.transform(new double[] {-10, 0, 0}, new double[] {0, 2.5, -3}, 0.5));
		return display;
	}

	/**
	 * A box textured from {@code region}: each face takes a patch of its own size from the region's corner, moved
	 * down by {@code offset} rows (wrapping) so neighbouring sticks don't show the same grain.
	 */
	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, double[] region, double offset) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			double width = switch (direction.getAxis()) {
				case X -> z2 - z1;
				case Y, Z -> x2 - x1;
			};
			double height = switch (direction.getAxis()) {
				case Y -> z2 - z1;
				case X, Z -> y2 - y1;
			};
			double[] patch = direction.getAxis() == Direction.Axis.Y && region == BARK ? END_GRAIN : region;
			double u = Math.min(width, patch[2]);
			double v = Math.min(height, patch[3]);
			double top = patch[3] > v ? offset % (patch[3] - v + 1) : 0;
			JsonObject face = new JsonObject();
			face.add("uv", FallenLordItemModels.vector(new double[] {round(patch[0]), round(patch[1] + top), round(patch[0] + u),
					round(patch[1] + top + v)}));
			face.addProperty("texture", "#all");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", FallenLordItemModels.vector(new double[] {round(x1), round(y1), round(z1)}));
		element.add("to", FallenLordItemModels.vector(new double[] {round(x2), round(y2), round(z2)}));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject rotation(double x, double y, double z, String axis, double angle) {
		JsonObject rotation = new JsonObject();
		rotation.add("origin", FallenLordItemModels.vector(new double[] {round(x), round(y), round(z)}));
		rotation.addProperty("axis", axis);
		rotation.addProperty("angle", round(angle));
		return rotation;
	}

	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}
}
