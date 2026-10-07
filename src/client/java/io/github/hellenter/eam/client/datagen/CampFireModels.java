package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.campfire.CampFireBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.stream.IntStream;

/**
 * Camp fire models: a ring of stones, up to nine sticks leaning together like a teepee, and flames when lit.
 * Multipart, so each stick shows once the fire holds that many, with the texture of how burnt the sticks are.
 */
final class CampFireModels {
	private static final String STONE = "minecraft:block/cobblestone";
	/** Stick textures by {@link CampFireBlock#CHAR}: fresh wood, scorched, charred black. */
	private static final String[] STICK = {"eam:block/camp_fire_stick", "eam:block/camp_fire_stick_scorched", "eam:block/camp_fire_stick_charred"};
	private static final String FIRE = "minecraft:block/campfire_fire";

	private static final int STONES = 8;
	private static final double STONE_RADIUS = 6.25;
	private static final double STICK_LENGTH = 12.0;

	private CampFireModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Identifier ring = Eam.id("block/camp_fire_ring");
		Identifier fire = Eam.id("block/camp_fire_fire");
		generators.modelOutput.accept(ring, () -> model(STONE, ringElements()));
		generators.modelOutput.accept(fire, () -> model(FIRE, fireElements()));

		MultiPartGenerator blockState = MultiPartGenerator.multiPart(EamBlocks.CAMP_FIRE)
				.with(BlockModelGenerators.plainVariant(ring));
		for (int stick = 1; stick <= CampFireBlock.MAX_STICKS; stick++) {
			int index = stick;
			Integer[] laterCounts = IntStream.rangeClosed(stick + 1, CampFireBlock.MAX_STICKS).boxed().toArray(Integer[]::new);
			for (int stage = CampFireBlock.FRESH; stage <= CampFireBlock.CHARRED; stage++) {
				Identifier model = Eam.id("block/camp_fire_stick_" + stick + (stage == CampFireBlock.FRESH ? "" : "_char_" + stage));
				String texture = STICK[stage];
				generators.modelOutput.accept(model, () -> model(texture, single(stickElement(index))));
				blockState.with(BlockModelGenerators.condition()
								.term(CampFireBlock.STICKS, stick, laterCounts)
								.term(CampFireBlock.CHAR, stage),
						BlockModelGenerators.plainVariant(model));
			}
		}
		blockState.with(BlockModelGenerators.condition().term(CampFireBlock.LIT, true), BlockModelGenerators.plainVariant(fire));
		generators.blockStateOutput.accept(blockState);

		// The item looks like a freshly placed fire: the ring and three sticks.
		Identifier inventory = Eam.id("block/camp_fire_inventory");
		generators.modelOutput.accept(inventory, () -> {
			JsonArray elements = ringElements();
			for (int stick = 1; stick <= CampFireBlock.PLACED_STICKS; stick++) {
				JsonObject element = stickElement(stick);
				retexture(element, "#log");
				elements.add(element);
			}
			JsonObject model = model(STONE, elements);
			model.getAsJsonObject("textures").addProperty("log", STICK[CampFireBlock.FRESH]);
			model.getAsJsonObject("textures").addProperty("particle", STICK[CampFireBlock.FRESH]);
			return model;
		});
		generators.registerSimpleItemModel(EamItems.CAMP_FIRE, inventory);
	}

	private static JsonArray ringElements() {
		JsonArray elements = new JsonArray();
		for (int i = 0; i < STONES; i++) {
			double angle = 360.0 / STONES * i;
			double x = 8 + STONE_RADIUS * Math.cos(Math.toRadians(angle));
			double z = 8 + STONE_RADIUS * Math.sin(Math.toRadians(angle));
			// Every other stone is a bit smaller, so the ring looks hand-made.
			double half = i % 2 == 0 ? 1.5 : 1.0;
			double height = i % 2 == 0 ? 1.5 : 1.0;
			JsonObject element = box(x - half, 0, z - 1.0, x + half, height, z + 1.0);
			JsonObject rotation = new JsonObject();
			rotation.add("origin", vector(x, 0, z));
			rotation.addProperty("axis", "y");
			// Turn each stone so its long side runs along the ring.
			rotation.addProperty("angle", round(90.0 - angle));
			element.add("rotation", rotation);
			elements.add(element);
		}
		return elements;
	}

	/**
	 * Stick {@code index} (1 to 9) stands on a circle around the centre and leans in so its top reaches the middle.
	 * Sticks 1-3, 4-6 and 7-9 each make a tripod, turned so the nine end up evenly spread.
	 */
	private static JsonObject stickElement(int index) {
		int round = (index - 1) / 3;
		double around = (index - 1) % 3 * 120.0 + round * 40.0;
		double radius = 4.0 + 0.5 * round;
		double length = STICK_LENGTH - round;
		double x = 8 + radius * Math.cos(Math.toRadians(around));
		double z = 8 + radius * Math.sin(Math.toRadians(around));
		double tilt = Math.toDegrees(Math.asin(Math.min(1.0, (radius + 0.5) / length)));
		// The element is rotated about x first, then about y: the top then points along
		// (sin(tilt) * sin(turn), cos(tilt), sin(tilt) * cos(turn)), which must point back at the centre.
		double turn = Math.toDegrees(Math.atan2(-Math.cos(Math.toRadians(around)), -Math.sin(Math.toRadians(around))));

		JsonObject element = box(x - 1, 0, z - 1, x + 1, length, z + 1);
		// Explicit UVs: each side is a 2 pixel wide strip of the stick texture, the ends a 2x2 patch.
		JsonObject faces = element.getAsJsonObject("faces");
		for (String face : faces.keySet()) {
			boolean end = face.equals("up") || face.equals("down");
			faces.getAsJsonObject(face).add("uv", end ? vector4(7, 7, 9, 9) : vector4(7, 16 - length, 9, 16));
		}
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(x, 0, z));
		rotation.addProperty("x", round(tilt));
		rotation.addProperty("y", round(turn));
		rotation.addProperty("z", 0);
		element.add("rotation", rotation);
		return element;
	}

	/** Two crossed planes of flame, like the vanilla campfire. */
	private static JsonArray fireElements() {
		JsonArray elements = new JsonArray();
		elements.add(plane(box(2, 0, 8, 14, 14, 8), Direction.NORTH, Direction.SOUTH));
		elements.add(plane(box(8, 0, 2, 8, 14, 14), Direction.WEST, Direction.EAST));
		return elements;
	}

	private static JsonObject plane(JsonObject element, Direction front, Direction back) {
		JsonObject faces = new JsonObject();
		for (Direction direction : new Direction[] {front, back}) {
			JsonObject face = new JsonObject();
			face.add("uv", vector4(0, 0, 16, 16));
			face.addProperty("texture", "#all");
			faces.add(direction.getSerializedName(), face);
		}
		element.add("faces", faces);
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(8, 8, 8));
		rotation.addProperty("axis", "y");
		rotation.addProperty("angle", 45);
		rotation.addProperty("rescale", true);
		element.add("rotation", rotation);
		element.addProperty("shade_direction_override", "up");
		return element;
	}

	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			face.addProperty("texture", "#all");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x1, y1, z1));
		element.add("to", vector(x2, y2, z2));
		element.add("faces", faces);
		return element;
	}

	private static void retexture(JsonObject element, String texture) {
		JsonObject faces = element.getAsJsonObject("faces");
		for (String face : faces.keySet()) {
			faces.getAsJsonObject(face).addProperty("texture", texture);
		}
	}

	private static JsonArray single(JsonObject element) {
		JsonArray elements = new JsonArray();
		elements.add(element);
		return elements;
	}

	private static JsonObject model(String texture, JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", texture);
		textures.addProperty("all", texture);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(round(x));
		vector.add(round(y));
		vector.add(round(z));
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

	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}
}
