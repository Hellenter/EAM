package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.bucket.BucketContent;
import io.github.hellenter.eam.content.bucket.MetalBuckets;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;
import io.github.hellenter.eam.content.swamp.DenseShrubBlock;
import io.github.hellenter.eam.content.swamp.DuckweedBlock;
import io.github.hellenter.eam.content.swamp.HollowLogBlock;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

public class EamModelProvider extends FabricModelProvider {
	static final Identifier TALISMAN_GLOWING_MODEL = Eam.id("item/black_talisman_glowing");

	public EamModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		groundItem(generators, EamBlocks.PEBBLE, ModelLocationUtils.getModelLocation(EamBlocks.PEBBLE));
		groundItem(generators, EamBlocks.FLINT_SHARD, ModelLocationUtils.getModelLocation(EamBlocks.FLINT_SHARD));
		groundItem(generators, EamBlocks.GROUND_STICK, Identifier.withDefaultNamespace("block/oak_log"));
		CampFireModels.generate(generators);
		SieveModels.generate(generators);
		generators.createTrivialCube(EamBlocks.SILVER_ORE);
		generators.createTrivialCube(EamBlocks.DEEPSLATE_SILVER_ORE);
		generators.createTrivialCube(EamBlocks.SILVER_BLOCK);
		BrewingStandModels.generate(generators);
		AnvilModels.generate(generators);
		generators.createTrivialCube(EamBlocks.MITHRIL_ORE);
		generators.createTrivialCube(EamBlocks.DEEPSLATE_MITHRIL_ORE);
		generators.createTrivialCube(EamBlocks.MITHRIL_BLOCK);
		generators.createTrivialCube(EamBlocks.ANCIENT_METAL_BLOCK);
		generators.createFurnace(EamBlocks.OBSIDIAN_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
		for (Block bars : List.of(EamBlocks.SILVER_BARS, EamBlocks.MITHRIL_BARS)) {
			generators.createBarsAndItem(bars);
		}
		for (Block door : List.of(EamBlocks.SILVER_DOOR, EamBlocks.MITHRIL_DOOR)) {
			generators.createDoor(door);
		}
		swampPlants(generators);
		// Like the iron trapdoor: the same model whichever way it was placed.
		for (Block trapdoor : List.of(EamBlocks.SILVER_TRAPDOOR, EamBlocks.MITHRIL_TRAPDOOR)) {
			generators.createTrapdoor(trapdoor);
		}
	}

	/** Untinted crosses like vanilla flowers; duckweed is a flat sheet on the water like a lily pad, without the tint. */
	private static void swampPlants(BlockModelGenerators generators) {
		// Duckweed: one sheet per coverage stage, turned at random like lily pads.
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.DUCKWEED).with(
				PropertyDispatch.initial(DuckweedBlock.COVERAGE).generate(stage -> {
					Identifier model = ModelLocationUtils.getModelLocation(EamBlocks.DUCKWEED, "_stage" + stage);
					Identifier texture = TextureMapping.getBlockTexture(EamBlocks.DUCKWEED, "_stage" + stage).sprite();
					generators.modelOutput.accept(model, () -> floatingSheet(texture));
					return BlockModelGenerators.createRotatedVariants(BlockModelGenerators.plainModel(model));
				})));
		generators.registerSimpleFlatItemModel(EamBlocks.DUCKWEED);
		denseShrub(generators);
		EamBlocks.HOLLOW_LOGS.forEach((log, hollow) -> hollowLog(generators, log, hollow));
		shelfMushroom(generators);
		generators.createCrossBlockWithDefaultItem(EamBlocks.SWAMP_GRASS, BlockModelGenerators.PlantType.NOT_TINTED);
		generators.createCrossBlockWithDefaultItem(EamBlocks.SWAMP_SHRUB, BlockModelGenerators.PlantType.NOT_TINTED);
		generators.createCrossBlockWithDefaultItem(EamBlocks.MARSH_MARIGOLD, BlockModelGenerators.PlantType.NOT_TINTED);
		for (Block tall : List.of(EamBlocks.CATTAIL, EamBlocks.TALL_SWAMP_GRASS, EamBlocks.FIREWEED)) {
			generators.createDoublePlantWithDefaultItem(tall, BlockModelGenerators.PlantType.NOT_TINTED);
		}
	}

	/**
	 * The dense shrub, like an azalea: a box of leaves from y 5 up on a cross of twig legs. The upper block of a
	 * two-block shrub is the leaves alone, full height, so the two read as one bush.
	 */
	private static void denseShrub(BlockModelGenerators generators) {
		Block block = EamBlocks.DENSE_SHRUB;
		String leaves = TextureMapping.getBlockTexture(block, "_leaves").sprite().toString();
		String top = TextureMapping.getBlockTexture(block, "_top").sprite().toString();
		String stem = TextureMapping.getBlockTexture(block, "_stem").sprite().toString();
		Identifier lower = ModelLocationUtils.getModelLocation(block);
		Identifier upper = ModelLocationUtils.getModelLocation(block, "_top");
		generators.modelOutput.accept(lower, () -> {
			JsonArray elements = new JsonArray();
			elements.add(leafBox(5, 16));
			elements.add(crossPlane(true));
			elements.add(crossPlane(false));
			return shrubModel(leaves, top, stem, elements);
		});
		generators.modelOutput.accept(upper, () -> {
			JsonArray elements = new JsonArray();
			elements.add(leafBox(0, 15));
			return shrubModel(leaves, top, stem, elements);
		});
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(
				DenseShrubBlock.TOP, BlockModelGenerators.plainVariant(upper), BlockModelGenerators.plainVariant(lower))));
		generators.registerSimpleItemModel(block.asItem(), lower);
	}

	/**
	 * A hollow log: four bark walls two pixels thick around an empty core, the vanilla log's bark outside, its stripped
	 * wood inside and its rings on the wall ends. Lies along an axis like the log, by rotating the upright model.
	 */
	private static void hollowLog(BlockModelGenerators generators, Block log, Block hollow) {
		Identifier logTexture = TextureMapping.getBlockTexture(log).sprite();
		String bark = logTexture.toString();
		String rings = TextureMapping.getBlockTexture(log, "_top").sprite().toString();
		String inner = logTexture.withPath(path -> path.replace("block/", "block/stripped_")).toString();
		Identifier model = ModelLocationUtils.getModelLocation(hollow);
		int w = HollowLogBlock.WALL;
		generators.modelOutput.accept(model, () -> {
			JsonObject textures = new JsonObject();
			textures.addProperty("particle", bark);
			textures.addProperty("bark", bark);
			textures.addProperty("rings", rings);
			textures.addProperty("inner", inner);
			JsonArray elements = new JsonArray();
			elements.add(wall(0, 0, 16, w));
			elements.add(wall(0, 16 - w, 16, 16));
			elements.add(wall(0, w, w, 16 - w));
			elements.add(wall(16 - w, w, 16, 16 - w));
			JsonObject json = new JsonObject();
			json.addProperty("parent", "minecraft:block/block");
			json.add("textures", textures);
			json.add("elements", elements);
			return json;
		});
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(hollow, BlockModelGenerators.plainVariant(model))
				.with(BlockModelGenerators.createRotatedPillar()));
		generators.registerSimpleItemModel(hollow.asItem(), model);
	}

	/** One upright wall of a hollow log, from (x0, z0) to (x1, z1); a face on the block's edge is bark, else the inside. */
	private static JsonObject wall(double x0, double z0, double x1, double z1) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			String texture;
			JsonArray uv;
			switch (direction) {
				case UP, DOWN -> {
					texture = "#rings";
					uv = vector4(x0, z0, x1, z1);
				}
				case NORTH, SOUTH -> {
					double at = direction == Direction.NORTH ? z0 : z1;
					texture = at == 0 || at == 16 ? "#bark" : "#inner";
					uv = vector4(x0, 0, x1, 16);
				}
				default -> {
					double at = direction == Direction.WEST ? x0 : x1;
					texture = at == 0 || at == 16 ? "#bark" : "#inner";
					uv = vector4(z0, 0, z1, 16);
				}
			}
			face.add("uv", uv);
			face.addProperty("texture", texture);
			if (texture.equals("#bark") || direction.getAxis() == Direction.Axis.Y) {
				face.addProperty("cullface", direction.getSerializedName());
			}
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x0, 0, z0));
		element.add("to", vector(x1, 16, z1));
		element.add("faces", faces);
		return element;
	}

	/** Two bracket shelves against the log to the north, turned to face whichever side the log is on. */
	private static void shelfMushroom(BlockModelGenerators generators) {
		Block block = EamBlocks.SHELF_MUSHROOM;
		String texture = TextureMapping.getBlockTexture(block).sprite().toString();
		Identifier model = ModelLocationUtils.getModelLocation(block);
		generators.modelOutput.accept(model, () -> {
			JsonObject textures = new JsonObject();
			textures.addProperty("particle", texture);
			textures.addProperty("shelf", texture);
			JsonArray elements = new JsonArray();
			elements.add(shelf(2, 5, 14, 8, 6, 0));
			elements.add(shelf(5, 10, 12, 12, 4, 8));
			JsonObject json = new JsonObject();
			json.addProperty("parent", "minecraft:block/block");
			json.add("textures", textures);
			json.add("elements", elements);
			return json;
		});
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
				.with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
		generators.registerSimpleItemModel(block.asItem(), model);
	}

	/**
	 * One shelf from the log's face (z 0) out to {@code depth}: the top of the texture's upper half is its cap, the
	 * lower half its rim and underside; {@code v} picks which rows of the texture this shelf uses.
	 */
	private static JsonObject shelf(double x0, double y0, double x1, double y1, double depth, double v) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			if (direction == Direction.NORTH) {
				continue;
			}
			JsonObject face = new JsonObject();
			JsonArray uv = switch (direction) {
				case UP -> vector4(x0, v, x1, v + depth);
				case DOWN -> vector4(x0, 16 - depth, x1, 16);
				case SOUTH -> vector4(x0, 16 - depth - (y1 - y0), x1, 16 - depth);
				default -> vector4(0, v, depth, v + (y1 - y0));
			};
			face.add("uv", uv);
			face.addProperty("texture", "#shelf");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x0, y0, 0));
		element.add("to", vector(x1, y1, depth));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject shrubModel(String leaves, String top, String stem, JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", leaves);
		textures.addProperty("leaves", leaves);
		textures.addProperty("top", top);
		textures.addProperty("stem", stem);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/** A box of leaves from {@code y0} to {@code y1}, all six faces drawn: the leaves have holes to look through. */
	private static JsonObject leafBox(double y0, double y1) {
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			boolean horizontal = direction.getAxis().isHorizontal();
			face.add("uv", horizontal ? vector4(0, 16 - y1, 16, 16 - y0) : vector4(0, 0, 16, 16));
			face.addProperty("texture", direction == Direction.UP ? "#top" : "#leaves");
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(0, y0, 0));
		element.add("to", vector(16, y1, 16));
		element.add("faces", faces);
		return element;
	}

	/** One of the two diagonal twig planes under the leaves, as in vanilla's azalea. */
	private static JsonObject crossPlane(boolean alongX) {
		JsonObject faces = new JsonObject();
		for (String side : alongX ? List.of("north", "south") : List.of("west", "east")) {
			JsonObject face = new JsonObject();
			face.add("uv", vector4(0, 0, 16, 16));
			face.addProperty("texture", "#stem");
			faces.add(side, face);
		}
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(8, 8, 8));
		rotation.addProperty("axis", "y");
		rotation.addProperty("angle", 45);
		rotation.addProperty("rescale", true);
		JsonObject element = new JsonObject();
		element.add("from", alongX ? vector(0.1, 0, 8) : vector(8, 0, 0.1));
		element.add("to", alongX ? vector(15.9, 15.9, 8) : vector(8, 15.9, 15.9));
		element.add("rotation", rotation);
		element.addProperty("shade", false);
		element.add("faces", faces);
		return element;
	}

	private static JsonObject floatingSheet(Identifier texture) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", texture.toString());
		textures.addProperty("texture", texture.toString());
		JsonObject faces = new JsonObject();
		JsonObject down = new JsonObject();
		down.add("uv", vector4(0, 16, 16, 0));
		down.addProperty("texture", "#texture");
		faces.add("down", down);
		JsonObject up = new JsonObject();
		up.add("uv", vector4(0, 0, 16, 16));
		up.addProperty("texture", "#texture");
		faces.add("up", up);
		JsonObject element = new JsonObject();
		element.add("from", vector(0, 0.25, 0));
		element.add("to", vector(16, 0.25, 16));
		element.add("faces", faces);
		JsonArray elements = new JsonArray();
		elements.add(element);
		JsonObject model = new JsonObject();
		model.addProperty("ambientocclusion", false);
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonArray vector4(double a, double b, double c, double d) {
		JsonArray vector = vector(a, b, c);
		vector.add(d);
		return vector;
	}

	/** One box per position on the top face, from the block's own shape; the block state picks the model by position. */
	private static void groundItem(BlockModelGenerators generators, Block block, Identifier texture) {
		GroundItemLogic.Shape shape = ((GroundItemBlock) block).shape();
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(
				PropertyDispatch.initial(GroundItemBlock.POSITION).generate(position -> {
					Identifier model = ModelLocationUtils.getModelLocation(block, "_" + position);
					GroundItemLogic.Box box = shape.at(position);
					generators.modelOutput.accept(model, () -> boxModel(texture, box));
					return BlockModelGenerators.plainVariant(model);
				})));
	}

	private static JsonObject boxModel(Identifier texture, GroundItemLogic.Box box) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", texture.toString());
		textures.addProperty("all", texture.toString());

		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			JsonObject face = new JsonObject();
			face.addProperty("texture", "#all");
			if (direction == Direction.DOWN) {
				face.addProperty("cullface", "down");
			}
			faces.add(direction.getSerializedName(), face);
		}

		JsonObject element = new JsonObject();
		element.add("from", vector(box.minX(), 0, box.minZ()));
		element.add("to", vector(box.maxX(), box.height(), box.maxZ()));
		if (box.angle() != 0) {
			JsonObject rotation = new JsonObject();
			rotation.addProperty("angle", box.angle());
			rotation.addProperty("axis", "y");
			rotation.add("origin", vector(8, 0, 8));
			element.add("rotation", rotation);
		}
		element.add("faces", faces);
		JsonArray elements = new JsonArray();
		elements.add(element);

		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		generators.generateFlatItem(EamItems.LEATHER_THREAD, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WATER_BOWL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.BOILED_WATER_BOWL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.MILK_BOWL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.BEEF_STEW, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CHICKEN_SOUP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.VEGETABLE_SOUP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CREAM_OF_MUSHROOM_SOUP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CREAM_OF_VEGETABLE_SOUP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.PUMPKIN_SOUP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.MASHED_POTATO, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SALAD, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.ICE_CREAM, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SORBET, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.PORRIDGE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CEREAL, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CHOCOLATE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CHEESE, ModelTemplates.FLAT_ITEM);
		for (Item item : List.of(EamItems.BURNT_BEEF, EamItems.BURNT_PORKCHOP, EamItems.BURNT_MUTTON, EamItems.BURNT_CHICKEN,
				EamItems.BURNT_RABBIT, EamItems.BURNT_COD, EamItems.BURNT_SALMON, EamItems.BURNT_POTATO, EamItems.ASH)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		generators.generateFlatItem(EamItems.FALLEN_LORD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SKELETON_KNIGHT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SWAMP_LURKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WIGHT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FILTERED_LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FLINT_HATCHET, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.FLINT_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.FLINT_KNIFE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.COPPER_KNIFE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.SILVER_KNIFE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.IRON_KNIFE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.COPPER_HAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.SILVER_HAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.IRON_HAMMER, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.COPPER_SAW, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.SILVER_SAW, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.IRON_SAW, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.PEBBLE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FLINT_SHARD, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.HOT_IRON_INGOT, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.IRON_PLATE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CAMP_FIRE, ModelTemplates.FLAT_ITEM);
		for (Item item : List.of(EamItems.SILVER_BREWING_STAND, EamItems.LAPIS_GUNPOWDER_MIX, EamItems.SILVER_REDSTONE_DUST, EamItems.FLOWER_MIX,
				EamItems.FERMENTED_ENDER_EYE)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.RAW_SILVER, EamItems.SILVER_INGOT, EamItems.SILVER_NUGGET,
				EamItems.SILVER_HELMET, EamItems.SILVER_CHESTPLATE, EamItems.SILVER_LEGGINGS, EamItems.SILVER_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.SILVER_SWORD, EamItems.SILVER_SHOVEL, EamItems.SILVER_PICKAXE, EamItems.SILVER_AXE, EamItems.SILVER_HOE)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
		for (Item item : List.of(EamItems.RAW_MITHRIL, EamItems.MITHRIL_NUGGET, EamItems.MITHRIL_INGOT, EamItems.HOT_MITHRIL_INGOT, EamItems.MITHRIL_PLATE,
				EamItems.MITHRIL_HELMET, EamItems.MITHRIL_CHESTPLATE, EamItems.MITHRIL_LEGGINGS, EamItems.MITHRIL_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.MITHRIL_SWORD, EamItems.MITHRIL_SHOVEL, EamItems.MITHRIL_PICKAXE, EamItems.MITHRIL_AXE,
				EamItems.MITHRIL_HOE, EamItems.MITHRIL_KNIFE, EamItems.MITHRIL_SAW, EamItems.MITHRIL_HAMMER)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
		// Metal buckets, each with every content.
		for (Map<BucketContent, Item> set : MetalBuckets.metalSets()) {
			for (Item item : set.values()) {
				generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
			}
		}
		for (Item item : List.of(EamItems.RUSTY_IRON_HELMET, EamItems.RUSTY_IRON_CHESTPLATE, EamItems.RUSTY_IRON_LEGGINGS,
				EamItems.RUSTY_IRON_BOOTS, EamItems.RUSTY_CHAINMAIL_HELMET, EamItems.RUSTY_CHAINMAIL_CHESTPLATE,
				EamItems.RUSTY_CHAINMAIL_LEGGINGS, EamItems.RUSTY_CHAINMAIL_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.RUSTY_IRON_SWORD, EamItems.RUSTY_IRON_SHOVEL, EamItems.RUSTY_IRON_PICKAXE, EamItems.RUSTY_IRON_AXE,
				EamItems.RUSTY_IRON_HOE)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
		for (Item item : List.of(EamItems.ANCIENT_METAL_NUGGET, EamItems.ANCIENT_METAL_INGOT, EamItems.HOT_ANCIENT_METAL_INGOT,
				EamItems.ANCIENT_METAL_PLATE, EamItems.ANCIENT_METAL_HELMET, EamItems.ANCIENT_METAL_CHESTPLATE,
				EamItems.ANCIENT_METAL_LEGGINGS, EamItems.ANCIENT_METAL_BOOTS, EamItems.ANCIENT_METAL_CHAINMAIL_HELMET,
				EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE, EamItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS, EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.COPPER_RING, EamItems.SILVER_RING, EamItems.IRON_RING, EamItems.ANCIENT_METAL_RING,
				EamItems.MITHRIL_RING, EamItems.COPPER_CHAINMAIL_HELMET, EamItems.COPPER_CHAINMAIL_CHESTPLATE, EamItems.COPPER_CHAINMAIL_LEGGINGS,
				EamItems.COPPER_CHAINMAIL_BOOTS, EamItems.SILVER_CHAINMAIL_HELMET, EamItems.SILVER_CHAINMAIL_CHESTPLATE,
				EamItems.SILVER_CHAINMAIL_LEGGINGS, EamItems.SILVER_CHAINMAIL_BOOTS, EamItems.MITHRIL_CHAINMAIL_HELMET,
				EamItems.MITHRIL_CHAINMAIL_CHESTPLATE, EamItems.MITHRIL_CHAINMAIL_LEGGINGS, EamItems.MITHRIL_CHAINMAIL_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.ANCIENT_METAL_SWORD, EamItems.ANCIENT_METAL_SHOVEL, EamItems.ANCIENT_METAL_PICKAXE,
				EamItems.ANCIENT_METAL_AXE, EamItems.ANCIENT_METAL_HOE, EamItems.ANCIENT_METAL_KNIFE, EamItems.ANCIENT_METAL_SAW,
				EamItems.ANCIENT_METAL_HAMMER)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
		for (Item item : List.of(EamItems.COPPER_ARROW, EamItems.SILVER_ARROW, EamItems.IRON_ARROW, EamItems.ANCIENT_METAL_ARROW,
				EamItems.MITHRIL_ARROW)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		PotionModels.generate(generators);
		FallenLordItemModels.generate(generators);
		generators.generateFlatItem(EamItems.BLACK_TALISMAN, ModelTemplates.FLAT_ITEM);
		// The same talisman with its crystal glowing, shown while it lights a portal.
		ModelTemplates.FLAT_ITEM.create(TALISMAN_GLOWING_MODEL, TextureMapping.layer0(new Material(TALISMAN_GLOWING_MODEL)), generators.modelOutput);
		// generateBow expects the idle model to exist already, as vanilla's hand-written bow.json does.
		ModelTemplates.BOW.create(ModelLocationUtils.getModelLocation(EamItems.SLINGSHOT), TextureMapping.layer0(EamItems.SLINGSHOT), generators.modelOutput);
		generators.generateBow(EamItems.SLINGSHOT);
	}
}
