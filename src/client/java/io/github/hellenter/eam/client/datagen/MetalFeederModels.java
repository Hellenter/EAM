package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.feeder.FeederBlock;
import io.github.hellenter.eam.content.feeder.FeederLogic;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Metal feeder models, drawn lying along X and turned for Z: a galvanized V-shaped rack of thin bars over a narrow
 * trough, crossed legs and trough end plates on the sides not joined to another feeder, and hay in the rack that rises
 * as it fills.
 */
final class MetalFeederModels {
	private static final String METAL = "eam:block/metal_feeder_bars";
	private static final String TROUGH = "eam:block/metal_feeder_trough";
	private static final String FEED = "eam:block/feeder_feed";

	/** Lean of each rack side away from the middle, in degrees. */
	private static final float LEAN = 22.5F;
	private static final double RACK_BOTTOM = 8.0;
	private static final double RACK_TOP = 16.0;
	/** Thickness of a rack bar, halved. */
	private static final double BAR_HALF = 0.3;
	/** Thickness of the end fence rods. */
	private static final double END_FENCE = 0.8;
	/** Hay height per {@link FeederBlock#FILL} step, in pixels. */
	private static final double FEED_STEP = 2.4;

	private MetalFeederModels() {
	}

	static void generate(BlockModelGenerators generators) {
		MultiPartGenerator blockState = MultiPartGenerator.multiPart(EamBlocks.METAL_FEEDER);
		Identifier body = Eam.id("block/metal_feeder_body");
		Identifier negativeEnd = Eam.id("block/metal_feeder_end_negative");
		Identifier positiveEnd = Eam.id("block/metal_feeder_end_positive");
		generators.modelOutput.accept(body, () -> model(bodyElements()));
		generators.modelOutput.accept(negativeEnd, () -> model(endElements(false)));
		generators.modelOutput.accept(positiveEnd, () -> model(endElements(true)));
		Identifier single = Eam.id("block/metal_feeder");
		generators.modelOutput.accept(single, () -> {
			JsonArray elements = bodyElements();
			elements.addAll(endElements(false));
			elements.addAll(endElements(true));
			return model(elements);
		});

		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis), turned(body, axis));
			blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.NEGATIVE, false),
					turned(negativeEnd, axis));
			blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.POSITIVE, false),
					turned(positiveEnd, axis));
		}
		for (int fill = 1; fill <= FeederLogic.MAX_FILL; fill++) {
			for (boolean negative : new boolean[] {false, true}) {
				for (boolean positive : new boolean[] {false, true}) {
					int level = fill;
					// Inside an end fence the hay stops short of it; between joined feeders it runs on.
					double from = negative ? 0 : END_FENCE + 0.5;
					double to = positive ? 16 : 16 - END_FENCE - 0.5;
					Identifier model = Eam.id("block/metal_feeder_feed_" + fill + (negative ? "_n" : "") + (positive ? "_p" : ""));
					generators.modelOutput.accept(model, () -> model(hay(level, from, to)));
					for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
						blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.FILL, fill)
								.term(FeederBlock.NEGATIVE, negative).term(FeederBlock.POSITIVE, positive), turned(model, axis));
					}
				}
			}
		}
		generators.blockStateOutput.accept(blockState);
		generators.registerSimpleItemModel(EamItems.METAL_FEEDER, single);
	}

	/**
	 * Layers of hay that widen with the rack as they rise, each as wide as the rack is at its bottom so no hay shows
	 * through the bars.
	 */
	private static JsonArray hay(int level, double from, double to) {
		JsonArray elements = new JsonArray();
		for (int layer = 1; layer <= level; layer++) {
			double bottom = RACK_BOTTOM + (layer - 1) * FEED_STEP;
			double halfWidth = rackHalfWidth(bottom) - BAR_HALF - 0.2;
			elements.add(FeederModels.box(from, bottom, 8 - halfWidth, to, bottom + FEED_STEP, 8 + halfWidth, "#feed"));
		}
		return elements;
	}

	/** Half the distance between the middles of the two rack sides at height {@code y}. */
	private static double rackHalfWidth(double y) {
		return 2 + (y - RACK_BOTTOM) * Math.tan(Math.toRadians(LEAN));
	}

	private static MultiVariant turned(Identifier model, Direction.Axis axis) {
		MultiVariant variant = BlockModelGenerators.plainVariant(model);
		return axis == Direction.Axis.Z ? variant.with(BlockModelGenerators.Y_ROT_90) : variant;
	}

	/** The trough and the two leaning sides of the rack, each a row of bars between a bottom and a top rail. */
	private static JsonArray bodyElements() {
		JsonArray elements = new JsonArray();
		// The floor sits between the walls rather than under them, so no two boxes share a face and flicker.
		elements.add(FeederModels.box(0, 4, 5, 16, 5, 11, "#trough"));
		elements.add(FeederModels.box(0, 4, 4, 16, 7, 5, "#trough"));
		elements.add(FeederModels.box(0, 4, 11, 16, 7, 12, "#trough"));
		for (int side = 0; side < 2; side++) {
			double z = side == 0 ? 6 : 10;
			float angle = side == 0 ? -LEAN : LEAN;
			elements.add(leaning(FeederModels.box(0, RACK_BOTTOM - 1, z - 0.5, 16, RACK_BOTTOM, z + 0.5, "#metal"), z, angle));
			elements.add(leaning(FeederModels.box(0, RACK_TOP - 1, z - 0.5, 16, RACK_TOP, z + 0.5, "#metal"), z, angle));
			for (double x = 0.6; x < 16; x += 2) {
				elements.add(leaning(FeederModels.box(x, RACK_BOTTOM, z - 0.3, x + 0.8, RACK_TOP - 1, z + 0.3, "#metal"), z, angle));
			}
		}
		return elements;
	}

	/** Crossed legs and the trough's end plate at the west ({@code positive} false) or east end. */
	private static JsonArray endElements(boolean positive) {
		double legX = positive ? 14.5 : 0.5;
		double plateX = positive ? 15 : 0;
		JsonArray elements = new JsonArray();
		for (float angle : new float[] {LEAN, -LEAN}) {
			JsonObject leg = FeederModels.box(legX, -1, 7.5, legX + 1, 14, 8.5, "#metal");
			elements.add(rotated(leg, legX + 0.5, 6.5, 8, angle));
		}
		// Only the gap between the trough's walls above its floor, so the plate shares no face with them.
		elements.add(FeederModels.box(plateX, 5, 5, plateX + 1, 7, 11, "#trough"));
		// The end fence: rods across the V from one side of the rack to the other, like the rack's own rails.
		double fenceX = positive ? 16 - END_FENCE : 0;
		for (double y = RACK_BOTTOM + 1.5; y < RACK_TOP; y += 2.5) {
			double half = rackHalfWidth(y + 0.4);
			elements.add(FeederModels.box(fenceX, y, 8 - half, fenceX + END_FENCE, y + 0.8, 8 + half, "#metal"));
		}
		elements.add(FeederModels.box(fenceX, RACK_BOTTOM - 1, 6, fenceX + END_FENCE, RACK_BOTTOM, 10, "#metal"));
		return elements;
	}

	private static JsonObject leaning(JsonObject element, double z, float angle) {
		return rotated(element, 8, RACK_BOTTOM, z, angle);
	}

	private static JsonObject rotated(JsonObject element, double x, double y, double z, float angle) {
		JsonObject rotation = new JsonObject();
		rotation.add("origin", FeederModels.vector(x, y, z));
		rotation.addProperty("axis", "x");
		rotation.addProperty("angle", angle);
		element.add("rotation", rotation);
		return element;
	}

	private static JsonObject model(JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", METAL);
		textures.addProperty("metal", METAL);
		textures.addProperty("trough", TROUGH);
		textures.addProperty("feed", FEED);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}
}
