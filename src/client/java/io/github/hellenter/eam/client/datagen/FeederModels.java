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
 * Feeder models, drawn lying along X and turned for Z: a plank trough with slatted rack sides, end posts and end walls
 * on the sides not joined to another feeder, and a heap of feed that rises as it fills. Multipart, so a joined row
 * reads as one long manger.
 */
final class FeederModels {
	private static final String WOOD = "eam:block/feeder_planks";
	private static final String POST = "eam:block/feeder_post";
	private static final String FEED = "eam:block/feeder_feed";

	private static final double FLOOR = 3.0;
	private static final double FLOOR_TOP = 5.0;
	private static final double WALL_TOP = 9.0;
	private static final double RAIL_TOP = 14.0;
	/** Feed heap height per {@link FeederBlock#FILL} step, in pixels. */
	private static final double FEED_STEP = 1.5;

	private FeederModels() {
	}

	static void generate(BlockModelGenerators generators) {
		MultiPartGenerator blockState = MultiPartGenerator.multiPart(EamBlocks.FEEDER);
		Identifier body = Eam.id("block/feeder_body");
		Identifier negativeEnd = Eam.id("block/feeder_end_negative");
		Identifier positiveEnd = Eam.id("block/feeder_end_positive");
		generators.modelOutput.accept(body, () -> model(WOOD, bodyElements()));
		generators.modelOutput.accept(negativeEnd, () -> model(POST, endElements(false)));
		generators.modelOutput.accept(positiveEnd, () -> model(POST, endElements(true)));
		Identifier single = Eam.id("block/feeder");
		generators.modelOutput.accept(single, () -> {
			JsonArray elements = bodyElements();
			elements.addAll(endElements(false));
			elements.addAll(endElements(true));
			return model(WOOD, elements);
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
					double height = fill * FEED_STEP;
					Identifier model = Eam.id("block/feeder_feed_" + fill + (negative ? "_n" : "") + (positive ? "_p" : ""));
					double from = negative ? 0 : 1;
					double to = positive ? 16 : 15;
					generators.modelOutput.accept(model, () -> model(FEED, array(box(from, FLOOR_TOP, 3, to, FLOOR_TOP + height, 13, "#feed"))));
					for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
						blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.FILL, fill)
								.term(FeederBlock.NEGATIVE, negative).term(FeederBlock.POSITIVE, positive), turned(model, axis));
					}
				}
			}
		}
		generators.blockStateOutput.accept(blockState);
		generators.registerSimpleItemModel(EamItems.FEEDER, single);
	}

	/** Lying along Z is the X model turned a quarter clockwise, which takes its west end north. */
	private static MultiVariant turned(Identifier model, Direction.Axis axis) {
		MultiVariant variant = BlockModelGenerators.plainVariant(model);
		return axis == Direction.Axis.Z ? variant.with(BlockModelGenerators.Y_ROT_90) : variant;
	}

	/** Floor, the two long walls, slats above them and the top rails. */
	private static JsonArray bodyElements() {
		JsonArray elements = new JsonArray();
		elements.add(box(0, FLOOR, 3, 16, FLOOR_TOP, 13, "#wood"));
		elements.add(box(0, FLOOR, 2, 16, WALL_TOP, 3, "#wood"));
		elements.add(box(0, FLOOR, 13, 16, WALL_TOP, 14, "#wood"));
		for (double x : new double[] {1.5, 5.5, 9.5, 13.5}) {
			elements.add(box(x, WALL_TOP, 2, x + 1, RAIL_TOP - 1, 3, "#wood"));
			elements.add(box(x, WALL_TOP, 13, x + 1, RAIL_TOP - 1, 14, "#wood"));
		}
		elements.add(box(0, RAIL_TOP - 1, 2, 16, RAIL_TOP, 3, "#wood"));
		elements.add(box(0, RAIL_TOP - 1, 13, 16, RAIL_TOP, 14, "#wood"));
		return elements;
	}

	/** Two posts and the end wall at the west ({@code positive} false) or east end. */
	private static JsonArray endElements(boolean positive) {
		double postFrom = positive ? 14 : 0;
		double wallFrom = positive ? 15 : 0;
		JsonArray elements = new JsonArray();
		elements.add(box(postFrom, 0, 1, postFrom + 2, RAIL_TOP, 3, "#post"));
		elements.add(box(postFrom, 0, 13, postFrom + 2, RAIL_TOP, 15, "#post"));
		elements.add(box(wallFrom, FLOOR, 3, wallFrom + 1, WALL_TOP, 13, "#wood"));
		return elements;
	}

	private static JsonObject model(String particle, JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", particle.equals(FEED) ? FEED : WOOD);
		textures.addProperty("wood", WOOD);
		textures.addProperty("post", POST);
		textures.addProperty("feed", FEED);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonArray array(JsonObject element) {
		JsonArray elements = new JsonArray();
		elements.add(element);
		return elements;
	}

	static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, String texture) {
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

	static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}
}
