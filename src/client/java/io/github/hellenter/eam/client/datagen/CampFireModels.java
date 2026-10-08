package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.campfire.CampFireBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.stream.IntStream;

/**
 * Camp fire models: a ring of stones, up to nine sticks leaning together like a teepee (fewer than three lie on the
 * ground as if fallen), a heap of coals that grows as sticks burn, and flames when lit. Multipart, so each stick shows
 * once the fire holds that many, with the texture of how burnt its group of sticks is: sticks put on a blackening
 * fire start fresh (see {@link io.github.hellenter.eam.content.campfire.StickPile}).
 */
final class CampFireModels {
	private static final String STONE = "minecraft:block/cobblestone";
	/** Stick textures by {@link CampFireBlock#CHAR}: fresh wood, scorched, charred black. */
	private static final String[] STICK = {"eam:block/camp_fire_stick", "eam:block/camp_fire_stick_scorched", "eam:block/camp_fire_stick_charred"};
	private static final String FIRE = "minecraft:block/campfire_fire";
	private static final String COALS = "eam:block/camp_fire_coals";

	private static final int STONES = 8;
	private static final double STONE_RADIUS = 6.25;
	/** Per stick 1-9: length, distance of its foot from the centre, and how far from the centre its top stops. */
	private static final double[] STICK_LENGTHS = {11.0, 9.5, 12.0, 10.0, 8.5, 11.5, 9.0, 12.5, 10.5};
	private static final double[] STICK_RADII = {4.5, 4.0, 5.0, 5.0, 4.0, 4.5, 3.5, 5.0, 4.5};
	private static final double[] STICK_GAPS = {1.5, 2.0, 1.0, 2.25, 1.75, 1.25, 2.0, 1.0, 1.5};
	/** Directions of the sticks: each group of three makes a tripod, the groups turned apart. */
	private static final double[] GROUP_TURN = {0.0, 60.0, 30.0};
	/** Coal lumps (x, z, width, height), heap size 1 first; bigger heaps add the next three. */
	private static final double[][] COAL_LUMPS = {
			{6.0, 7.0, 2.0, 1.0}, {8.5, 6.0, 2.0, 0.75}, {7.5, 9.0, 2.5, 1.0},
			{5.0, 9.0, 1.5, 0.75}, {9.5, 8.5, 2.0, 1.0}, {7.0, 5.0, 1.5, 0.5},
			{10.0, 6.5, 1.5, 0.75}, {6.0, 10.5, 2.0, 1.0}, {9.0, 10.0, 1.5, 0.75}};

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
			// Standing while the fire holds at least STANDING_STICKS sticks; stick 1 and 2 then lie when fewer are left.
			int[] standing = range(Math.max(stick, CampFireBlock.STANDING_STICKS), CampFireBlock.MAX_STICKS);
			int[] lying = range(stick, CampFireBlock.STANDING_STICKS - 1);
			for (int stage = CampFireBlock.FRESH; stage <= CampFireBlock.CHARRED; stage++) {
				String suffix = stage == CampFireBlock.FRESH ? "" : "_char_" + stage;
				String texture = STICK[stage];
				Identifier standingModel = Eam.id("block/camp_fire_stick_" + stick + suffix);
				generators.modelOutput.accept(standingModel, () -> model(texture, single(stickElement(index))));
				addStick(blockState, stick, standing, stage, standingModel);
				if (stick < CampFireBlock.STANDING_STICKS) {
					Identifier lyingModel = Eam.id("block/camp_fire_stick_" + stick + "_lying" + suffix);
					generators.modelOutput.accept(lyingModel, () -> model(texture, single(lyingStickElement(index))));
					addStick(blockState, stick, lying, stage, lyingModel);
				}
			}
		}
		for (int heap = 1; heap <= CampFireBlock.MAX_COALS; heap++) {
			int size = heap;
			Identifier coals = Eam.id("block/camp_fire_coals_" + heap);
			generators.modelOutput.accept(coals, () -> model(COALS, coalElements(size)));
			blockState.with(BlockModelGenerators.condition().term(CampFireBlock.COALS, heap), BlockModelGenerators.plainVariant(coals));
		}
		blockState.with(BlockModelGenerators.condition().term(CampFireBlock.LIT, true), BlockModelGenerators.plainVariant(fire));
		generators.blockStateOutput.accept(blockState);

		// The item uses Maxim's flat icon, eam:item/camp_fire; see EamModelProvider.
	}

	/**
	 * Shows stick {@code stick} with {@code model} while the fire holds {@code sticks} sticks: with the old sticks'
	 * look when it is one of them, else with the look of the sticks put in later.
	 */
	private static void addStick(MultiPartGenerator blockState, int stick, int[] sticks, int stage, Identifier model) {
		blockState.with(BlockModelGenerators.condition()
						.term(CampFireBlock.STICKS, sticks[0], rest(sticks))
						.term(CampFireBlock.OLD_STICKS, stick, rest(range(stick, CampFireBlock.MAX_STICKS)))
						.term(CampFireBlock.CHAR, stage),
				BlockModelGenerators.plainVariant(model));
		blockState.with(BlockModelGenerators.condition()
						.term(CampFireBlock.STICKS, sticks[0], rest(sticks))
						.term(CampFireBlock.OLD_STICKS, 0, rest(range(0, stick - 1)))
						.term(CampFireBlock.NEW_CHAR, stage),
				BlockModelGenerators.plainVariant(model));
	}

	private static int[] range(int from, int to) {
		return IntStream.rangeClosed(from, to).toArray();
	}

	private static Integer[] rest(int[] values) {
		return IntStream.of(values).skip(1).boxed().toArray(Integer[]::new);
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
	 * Stick {@code index} (1 to 9) stands on a circle around the centre and leans in, stopping just short of the
	 * middle. Lengths and gaps differ, so the tops end at different heights instead of merging into one cone.
	 */
	private static JsonObject stickElement(int index) {
		int i = index - 1;
		double around = i % 3 * 120.0 + GROUP_TURN[i / 3];
		double radius = STICK_RADII[i];
		double length = STICK_LENGTHS[i];
		double x = 8 + radius * Math.cos(Math.toRadians(around));
		double z = 8 + radius * Math.sin(Math.toRadians(around));
		double tilt = Math.toDegrees(Math.asin(Math.min(1.0, (radius - STICK_GAPS[i]) / length)));
		// The element is rotated about x first, then about y: the top then points along
		// (sin(tilt) * sin(turn), cos(tilt), sin(tilt) * cos(turn)), which must point back at the centre.
		double turn = Math.toDegrees(Math.atan2(-Math.cos(Math.toRadians(around)), -Math.sin(Math.toRadians(around))));

		JsonObject element = box(x - 1, 0, z - 1, x + 1, length, z + 1);
		stickUvs(element, length, false);
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(x, 0, z));
		rotation.addProperty("x", round(tilt));
		rotation.addProperty("y", round(turn));
		rotation.addProperty("z", 0);
		element.add("rotation", rotation);
		return element;
	}

	/** Stick 1 or 2 lying on the ground as if it fell over; the second rests across the first. */
	private static JsonObject lyingStickElement(int index) {
		double length = STICK_LENGTHS[index - 1];
		double z = index == 1 ? 7.0 : 9.0;
		double bottom = index == 1 ? 0.0 : 1.0;
		JsonObject element = box(8 - length / 2, bottom, z - 1, 8 + length / 2, bottom + 2, z + 1);
		stickUvs(element, length, true);
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(8, bottom, z));
		rotation.addProperty("axis", "y");
		rotation.addProperty("angle", index == 1 ? 25 : -40);
		element.add("rotation", rotation);
		return element;
	}

	/**
	 * Explicit UVs: each long side is a 2 pixel wide strip of the stick texture, the ends a 2x2 patch. A lying stick
	 * runs along x, so its north, south, up and down faces turn the strip by 90 degrees.
	 */
	private static void stickUvs(JsonObject element, double length, boolean lying) {
		JsonObject faces = element.getAsJsonObject("faces");
		for (String name : faces.keySet()) {
			boolean end = lying ? name.equals("east") || name.equals("west") : name.equals("up") || name.equals("down");
			JsonObject face = faces.getAsJsonObject(name);
			face.add("uv", end ? vector4(7, 7, 9, 9) : vector4(7, 16 - length, 9, 16));
			if (lying && !end) {
				face.addProperty("rotation", 90);
			}
		}
	}

	private static JsonArray coalElements(int heap) {
		JsonArray elements = new JsonArray();
		for (int i = 0; i < heap * 3; i++) {
			double[] lump = COAL_LUMPS[i];
			JsonObject element = box(lump[0], 0, lump[1], lump[0] + lump[2], lump[3], lump[1] + lump[2]);
			JsonObject faces = element.getAsJsonObject("faces");
			for (String name : faces.keySet()) {
				faces.getAsJsonObject(name).add("uv", vector4(i, i, i + lump[2], i + lump[2]));
			}
			elements.add(element);
		}
		return elements;
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
