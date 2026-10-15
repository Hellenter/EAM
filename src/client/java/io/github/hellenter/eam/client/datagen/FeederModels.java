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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Feeder models, drawn lying along X with the back wall on the north and turned or mirrored for the other ways: a
 * trough on legs with a tall back wall, a low front lip and end walls on the sides not joined to another feeder, and a
 * heap of feed that rises as it fills. Each block stands on two legs at its west end, plus two at its east end when
 * nothing joins there, so a trough of one has four legs and each further block adds two. Multipart, so a joined row
 * reads as one long trough. The wooden feeder is made of planks; the metal one, of galvanized sheet on thin legs.
 *
 * <p>No two boxes overlap or share a face plane facing the same way where they meet: such faces flicker.
 */
final class FeederModels {
	private static final String FEED = "eam:block/feeder_feed";

	/** Feed heap height per {@link FeederBlock#FILL} step, in pixels. */
	private static final double FEED_STEP = 1.5;
	private static final double FLOOR = 4.0;
	private static final double BACK_TOP = 15.0;
	private static final double FRONT_TOP = 9.0;
	/** Outer side of the back and front walls. */
	private static final double OUTER = 1.0;

	/**
	 * How one feeder is built.
	 *
	 * @param wall  thickness of the walls and the floor
	 * @param leg   thickness of a leg
	 * @param body  texture of the trough
	 * @param legs  texture of the legs
	 */
	private record Style(String name, Block block, Item item, double wall, double leg, String body, String legs) {
		double floorTop() {
			return FLOOR + wall;
		}

		double innerBack() {
			return OUTER + wall;
		}

		double innerFront() {
			return 16 - OUTER - wall;
		}
	}

	private static final Style WOOD = new Style("feeder", EamBlocks.FEEDER, EamItems.FEEDER, 2, 2,
			"eam:block/feeder_planks", "eam:block/feeder_post");
	private static final Style METAL = new Style("metal_feeder", EamBlocks.METAL_FEEDER, EamItems.METAL_FEEDER, 1, 1,
			"eam:block/metal_feeder_trough", "eam:block/metal_feeder_bars");

	private FeederModels() {
	}

	static void generate(BlockModelGenerators generators) {
		generate(generators, WOOD);
		generate(generators, METAL);
	}

	private static void generate(BlockModelGenerators generators, Style style) {
		MultiPartGenerator blockState = MultiPartGenerator.multiPart(style.block);
		Identifier body = Eam.id("block/" + style.name + "_body");
		Identifier bodyMirrored = Eam.id("block/" + style.name + "_body_mirrored");
		generators.modelOutput.accept(body, () -> model(style, bodyElements(style, false)));
		generators.modelOutput.accept(bodyMirrored, () -> model(style, bodyElements(style, true)));
		Identifier single = Eam.id("block/" + style.name);
		generators.modelOutput.accept(single, () -> {
			JsonArray elements = bodyElements(style, false);
			elements.addAll(endElements(style, false, false));
			elements.addAll(endElements(style, true, false));
			return model(style, elements);
		});

		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			for (boolean backPositive : new boolean[] {false, true}) {
				// Turned a quarter, the north back wall of the X model ends up east: positive across a Z trough.
				boolean mirrored = backPositive == (axis == Direction.Axis.X);
				blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.BACK_POSITIVE, backPositive),
						turned(mirrored ? bodyMirrored : body, axis));
				for (boolean positive : new boolean[] {false, true}) {
					Identifier end = Eam.id("block/" + style.name + "_end_" + (positive ? "positive" : "negative") + (mirrored ? "_mirrored" : ""));
					if (axis == Direction.Axis.X) generators.modelOutput.accept(end, () -> model(style, endElements(style, positive, mirrored)));
					blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.BACK_POSITIVE, backPositive)
							.term(positive ? FeederBlock.POSITIVE : FeederBlock.NEGATIVE, false), turned(end, axis));
				}
			}
		}
		for (int fill = 1; fill <= FeederLogic.MAX_FILL; fill++) {
			for (boolean negative : new boolean[] {false, true}) {
				for (boolean positive : new boolean[] {false, true}) {
					double height = fill * FEED_STEP;
					Identifier model = Eam.id("block/" + style.name + "_feed_" + fill + (negative ? "_n" : "") + (positive ? "_p" : ""));
					// Inside an end wall the feed stops at it; between joined feeders it runs on.
					double from = negative ? 0 : style.wall;
					double to = positive ? 16 : 16 - style.wall;
					generators.modelOutput.accept(model, () -> model(style, array(box(from, style.floorTop(), style.innerBack(), to,
							style.floorTop() + height, style.innerFront(), "#feed"))));
					for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
						blockState.with(BlockModelGenerators.condition().term(FeederBlock.AXIS, axis).term(FeederBlock.FILL, fill)
								.term(FeederBlock.NEGATIVE, negative).term(FeederBlock.POSITIVE, positive), turned(model, axis));
					}
				}
			}
		}
		generators.blockStateOutput.accept(blockState);
		generators.registerSimpleItemModel(style.item, single);
	}

	/** Lying along Z is the X model turned a quarter clockwise, which takes its west end north. */
	private static MultiVariant turned(Identifier model, Direction.Axis axis) {
		MultiVariant variant = BlockModelGenerators.plainVariant(model);
		return axis == Direction.Axis.Z ? variant.with(BlockModelGenerators.Y_ROT_90) : variant;
	}

	/**
	 * The floor, the tall back wall, the low front lip and the two legs at the west end, with the back wall on the north,
	 * or on the south when {@code mirrored}. The floor lies between the walls rather than under them.
	 */
	private static JsonArray bodyElements(Style style, boolean mirrored) {
		JsonArray elements = new JsonArray();
		elements.add(box(0, FLOOR, style.innerBack(), 16, style.floorTop(), style.innerFront(), "#body", mirrored));
		elements.add(box(0, FLOOR, OUTER, 16, BACK_TOP, style.innerBack(), "#body", mirrored));
		elements.add(box(0, FLOOR, style.innerFront(), 16, FRONT_TOP, 16 - OUTER, "#body", mirrored));
		elements.addAll(legs(style, 1));
		return elements;
	}

	/**
	 * The end wall at the west ({@code positive} false) or east end, between the long walls above the floor, stepping
	 * down from the back wall to the front lip, mirrored like the body; at the east end also the two legs there.
	 */
	private static JsonArray endElements(Style style, boolean positive, boolean mirrored) {
		double from = positive ? 16 - style.wall : 0;
		double to = from + style.wall;
		double middle = 8.0;
		JsonArray elements = new JsonArray();
		elements.add(box(from, style.floorTop(), style.innerBack(), to, BACK_TOP - 2, middle, "#body", mirrored));
		elements.add(box(from, style.floorTop(), middle, to, FRONT_TOP + 1, style.innerFront(), "#body", mirrored));
		if (positive) elements.addAll(legs(style, 16 - 1 - style.leg));
		return elements;
	}

	/** Two legs under the back and front walls, from {@code x} along the trough. */
	private static JsonArray legs(Style style, double x) {
		JsonArray elements = new JsonArray();
		elements.add(box(x, 0, OUTER, x + style.leg, FLOOR, OUTER + style.leg, "#legs"));
		elements.add(box(x, 0, 16 - OUTER - style.leg, x + style.leg, FLOOR, 16 - OUTER, "#legs"));
		return elements;
	}

	private static JsonObject model(Style style, JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", style.body);
		textures.addProperty("body", style.body);
		textures.addProperty("legs", style.legs);
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

	/** A box, with its Z range mirrored across the middle of the block when {@code mirrored}. */
	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, String texture, boolean mirrored) {
		return mirrored ? box(x1, y1, 16 - z2, x2, y2, 16 - z1, texture) : box(x1, y1, z1, x2, y2, z2, texture);
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
