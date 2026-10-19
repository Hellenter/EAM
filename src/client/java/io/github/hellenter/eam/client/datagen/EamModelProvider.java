package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.swamp.CattailBlock;
import io.github.hellenter.eam.content.swamp.BerryShrub;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.bucket.BucketContent;
import io.github.hellenter.eam.content.bucket.MetalBuckets;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;
import io.github.hellenter.eam.content.swamp.DuckweedBlock;
import io.github.hellenter.eam.content.swamp.HollowLogBlock;
import io.github.hellenter.eam.content.underworld.GlowshroomVinesBlock;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
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
		FoodDryerModels.generate(generators);
		LockedChestModels.generate(generators);
		generators.createNormalTorch(EamBlocks.UNLIT_TORCH, EamBlocks.UNLIT_WALL_TORCH);
		SieveModels.generate(generators);
		KingHelmModels.generate(generators);
		FeederModels.generate(generators);
		generators.new BlockFamilyProvider(TextureMapping.cube(Blocks.STRIPPED_POPLAR_LOG)).fence(EamBlocks.STRIPPED_POPLAR_FENCE);
		generators.createTrivialCube(EamBlocks.SILVER_ORE);
		generators.createTrivialCube(EamBlocks.DEEPSLATE_SILVER_ORE);
		generators.createTrivialCube(EamBlocks.SILVER_BLOCK);
		BrewingStandModels.generate(generators);
		AnvilModels.generate(generators);
		generators.createTrivialCube(EamBlocks.MITHRIL_ORE);
		generators.createTrivialCube(EamBlocks.DEEPSLATE_MITHRIL_ORE);
		generators.createTrivialCube(EamBlocks.MITHRIL_BLOCK);
		generators.createTrivialCube(EamBlocks.ADAMANTIUM_ORE);
		generators.createTrivialCube(EamBlocks.DEEPSLATE_ADAMANTIUM_ORE);
		generators.createTrivialCube(EamBlocks.ADAMANTIUM_BLOCK);
		generators.createTrivialCube(EamBlocks.ANCIENT_METAL_BLOCK);
		generators.createTrivialCube(EamBlocks.DEEP_BEDROCK);
		generators.createTrivialCube(EamBlocks.KOSCHEI_ICE);
		generators.createMushroomBlock(EamBlocks.CHANTERELLE_CAP);
		generators.createMushroomBlock(EamBlocks.CHANTERELLE_STEM);
		generators.createMushroomBlock(EamBlocks.CHAMPIGNON_CAP);
		generators.createMushroomBlock(EamBlocks.GLOWING_CHAMPIGNON_CAP);
		variedMushroomBlock(generators, EamBlocks.BURNT_MUSHROOM_CAP, "_cracked", "_ashy", "_charred");
		variedMushroomBlock(generators, EamBlocks.BURNT_MUSHROOM_STEM, "_cracked", "_ashy");
		generators.createMushroomBlock(EamBlocks.SMOLDERING_MUSHROOM_CAP);
		// The tip shows its bright caps while ripe; cut, it is a bare dim thread.
		MultiVariant ripeTip = BlockModelGenerators.plainVariant(generators.createSuffixedVariant(EamBlocks.GLOWSHROOM_VINES, "", ModelTemplates.CROSS, TextureMapping::cross));
		MultiVariant bareTip = BlockModelGenerators.plainVariant(generators.createSuffixedVariant(EamBlocks.GLOWSHROOM_VINES, "_bare", ModelTemplates.CROSS, TextureMapping::cross));
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.GLOWSHROOM_VINES)
				.with(BlockModelGenerators.createBooleanModelDispatch(GlowshroomVinesBlock.RIPE, ripeTip, bareTip)));
		generators.createCrossBlock(EamBlocks.GLOWSHROOM_VINES_PLANT, BlockModelGenerators.PlantType.NOT_TINTED);
		generators.createTrivialCube(EamBlocks.GLOWSHROOM_LAMP);
		generators.registerSimpleFlatItemModel(EamBlocks.GLOWSHROOM_VINES, "_plant");
		generators.createFurnace(EamBlocks.OBSIDIAN_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
		generators.createHorizontallyRotatedBlock(EamBlocks.SMALL_FRIDGE, TexturedModel.ORIENTABLE);
		generators.createHorizontallyRotatedBlock(EamBlocks.LARGE_FRIDGE, TexturedModel.ORIENTABLE);
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
		generators.createCrossBlockWithDefaultItem(EamBlocks.WILD_GARLIC, BlockModelGenerators.PlantType.NOT_TINTED);
		// Four looks over eight ages, like carrots.
		// Garlic is the crop's item, so its definition comes from here; dried garlic looks darker.
		new BlockModelGenerators(generators.blockStateOutput, DriedItemModels.output(generators.itemModelOutput), generators.modelOutput)
				.createCropBlock(EamBlocks.GARLIC_CROP, BlockStateProperties.AGE_7, 0, 0, 1, 1, 2, 2, 2, 3);
		cattail(generators);
		generators.createCrossBlockWithDefaultItem(EamBlocks.NET, BlockModelGenerators.PlantType.NOT_TINTED);
		// An evolved spider's web looks like cobweb and has no item.
		generators.createCrossBlock(EamBlocks.SPIDER_WEB, BlockModelGenerators.PlantType.NOT_TINTED, TextureMapping.cross(Blocks.COBWEB));
		bigShrub(generators);
		for (Block tall : List.of(EamBlocks.TALL_SWAMP_GRASS, EamBlocks.FIREWEED)) {
			generators.createDoublePlantWithDefaultItem(tall, BlockModelGenerators.PlantType.NOT_TINTED);
		}
	}

	/**
	 * The small dense shrub: a box of its own chunky 16x16 leaves wider than the block, tinted with the biome's foliage
	 * colour like the trees, with leafy planes through it whose edges stick out as tufts, on a cross of twig legs. A ripe cranberry
	 * shrub has the berries drawn just outside its leaves.
	 */
	private static void denseShrub(BlockModelGenerators generators) {
		Block block = EamBlocks.DENSE_SHRUB;
		String stem = TextureMapping.getBlockTexture(block, "_stem").sprite().toString();
		String leaves = TextureMapping.getBlockTexture(block, "_leaves").sprite().toString();
		String berries = TextureMapping.getBlockTexture(block, "_cranberries").sprite().toString();
		Identifier plain = ModelLocationUtils.getModelLocation(block);
		Identifier ripe = ModelLocationUtils.getModelLocation(block, "_berries");
		for (boolean withBerries : List.of(false, true)) {
			generators.modelOutput.accept(withBerries ? ripe : plain, () -> {
				JsonArray elements = new JsonArray();
				leafBox(elements, -1, 3, 17, 17, "#leaves", true);
				plane(elements, true, 45, "#stem", 0, 7, false, -3, 19);
				plane(elements, false, 45, "#stem", 0, 7, false, -3, 19);
				plane(elements, true, 45, "#leaves", 4, 18, true, -4, 20);
				plane(elements, false, 45, "#leaves", 4, 18, true, -4, 20);
				plane(elements, true, 0, "#leaves", 4, 17, true, -3, 19);
				plane(elements, false, 0, "#leaves", 4, 17, true, -3, 19);
				if (withBerries) {
					leafBox(elements, -1.1, 2.9, 17.1, 17.1, "#berries", false);
				}
				return shrubModel(stem, leaves, berries, elements);
			});
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(BerryShrub.BERRIES)
				.select(BerryShrub.NONE, BlockModelGenerators.plainVariant(plain))
				.select(BerryShrub.PICKED, BlockModelGenerators.plainVariant(plain))
				.select(BerryShrub.RIPE, BlockModelGenerators.plainVariant(ripe))));
		generators.registerSimpleTintedItemModel(block, plain, ItemModelUtils.constantTint(-12012264));
	}

	/**
	 * The big shrub: a thin bark post, and a full block of its own tinted small-leaved foliage with tufts sticking
	 * out on every side, the ripe ones with wolfberries outside the leaves.
	 */
	private static void bigShrub(BlockModelGenerators generators) {
		Block stem = EamBlocks.BIG_SHRUB_STEM;
		String bark = TextureMapping.getBlockTexture(stem).sprite().toString();
		Identifier stemModel = ModelLocationUtils.getModelLocation(stem);
		generators.modelOutput.accept(stemModel, () -> {
			JsonObject faces = new JsonObject();
			for (Direction direction : Direction.values()) {
				JsonObject face = new JsonObject();
				face.add("uv", direction.getAxis().isHorizontal() ? vector4(6, 0, 10, 16) : vector4(6, 6, 10, 10));
				face.addProperty("texture", "#bark");
				if (direction.getAxis().isVertical()) {
					face.addProperty("cullface", direction.getSerializedName());
				}
				faces.add(direction.getSerializedName(), face);
			}
			JsonObject post = new JsonObject();
			post.add("from", vector(6, 0, 6));
			post.add("to", vector(10, 16, 10));
			post.add("faces", faces);
			JsonArray elements = new JsonArray();
			elements.add(post);
			JsonObject textures = new JsonObject();
			textures.addProperty("particle", bark);
			textures.addProperty("bark", bark);
			JsonObject json = new JsonObject();
			json.addProperty("parent", "minecraft:block/block");
			json.add("textures", textures);
			json.add("elements", elements);
			return json;
		});
		generators.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(stem, BlockModelGenerators.plainVariant(stemModel)));
		generators.registerSimpleItemModel(stem.asItem(), stemModel);

		Block leaves = EamBlocks.BIG_SHRUB_LEAVES;
		String leafTexture = TextureMapping.getBlockTexture(leaves).sprite().toString();
		String berries = TextureMapping.getBlockTexture(leaves, "_wolfberries").sprite().toString();
		Identifier plain = ModelLocationUtils.getModelLocation(leaves);
		Identifier ripe = ModelLocationUtils.getModelLocation(leaves, "_berries");
		for (boolean withBerries : List.of(false, true)) {
			generators.modelOutput.accept(withBerries ? ripe : plain, () -> {
				JsonArray elements = new JsonArray();
				leafBox(elements, 0, 0, 16, 16, "#leaves", true);
				plane(elements, true, 45, "#leaves", -1, 17, true, -3, 19);
				plane(elements, false, 45, "#leaves", -1, 17, true, -3, 19);
				plane(elements, true, 0, "#leaves", 0, 16, true, -3, 19);
				plane(elements, false, 0, "#leaves", 0, 16, true, -3, 19);
				if (withBerries) {
					leafBox(elements, -0.1, -0.1, 16.1, 16.1, "#berries", false);
				}
				return shrubModel(bark, leafTexture, berries, elements);
			});
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(leaves).with(PropertyDispatch.initial(BerryShrub.BERRIES)
				.select(BerryShrub.NONE, BlockModelGenerators.plainVariant(plain))
				.select(BerryShrub.PICKED, BlockModelGenerators.plainVariant(plain))
				.select(BerryShrub.RIPE, BlockModelGenerators.plainVariant(ripe))));
		generators.registerSimpleTintedItemModel(leaves, plain, ItemModelUtils.constantTint(-12012264));
	}

	/** Cattail clumps: one cross of stalks for each of the 1 to 4 stalks, spread over the block. */
	private static void cattail(BlockModelGenerators generators) {
		Block block = EamBlocks.CATTAIL;
		double[][][] stalks = {
				{{8, 8}},
				{{5, 6}, {11, 10}},
				{{5, 5}, {11, 6}, {8, 12}},
				{{4, 5}, {11, 4}, {5, 11}, {12, 11}}};
		PropertyDispatch.C2<MultiVariant, DoubleBlockHalf, Integer> dispatch = PropertyDispatch.initial(DoublePlantBlock.HALF, CattailBlock.AMOUNT);
		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			String suffix = half == DoubleBlockHalf.UPPER ? "_top" : "_bottom";
			String texture = TextureMapping.getBlockTexture(block, suffix).sprite().toString();
			for (int amount = 1; amount <= 4; amount++) {
				Identifier model = ModelLocationUtils.getModelLocation(block, suffix + (amount == 1 ? "" : "_" + amount));
				double[][] spots = stalks[amount - 1];
				double width = amount == 1 ? 7.2 : 6.0;
				generators.modelOutput.accept(model, () -> {
					JsonArray elements = new JsonArray();
					for (double[] spot : spots) {
						elements.add(stalk(spot[0], spot[1], width, true));
						elements.add(stalk(spot[0], spot[1], width, false));
					}
					JsonObject textures = new JsonObject();
					textures.addProperty("particle", texture);
					textures.addProperty("cross", texture);
					JsonObject json = new JsonObject();
					json.addProperty("ambientocclusion", false);
					json.add("textures", textures);
					json.add("elements", elements);
					return json;
				});
				dispatch.select(half, amount, BlockModelGenerators.plainVariant(model));
			}
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(dispatch));
		generators.registerSimpleFlatItemModel(block, "_top");
	}

	/** One plane of a cattail stalk's cross, centred on ({@code x}, {@code z}), turned 45 degrees. */
	private static JsonObject stalk(double x, double z, double halfWidth, boolean alongX) {
		JsonObject faces = new JsonObject();
		for (String side : alongX ? List.of("north", "south") : List.of("west", "east")) {
			JsonObject face = new JsonObject();
			face.add("uv", vector4(0, 0, 16, 16));
			face.addProperty("texture", "#cross");
			faces.add(side, face);
		}
		JsonObject element = new JsonObject();
		element.add("from", alongX ? vector(x - halfWidth, 0, z) : vector(x, 0, z - halfWidth));
		element.add("to", alongX ? vector(x + halfWidth, 16, z) : vector(x, 16, z + halfWidth));
		JsonObject rotation = new JsonObject();
		rotation.add("origin", vector(x, 8, z));
		rotation.addProperty("axis", "y");
		rotation.addProperty("angle", 45);
		element.add("rotation", rotation);
		element.addProperty("shade", false);
		element.add("faces", faces);
		return element;
	}

	private static JsonObject shrubModel(String stem, String leaves, String berries, JsonArray elements) {
		JsonObject textures = new JsonObject();
		textures.addProperty("particle", leaves);
		textures.addProperty("leaves", leaves);
		textures.addProperty("stem", stem);
		textures.addProperty("berries", berries);
		JsonObject json = new JsonObject();
		json.addProperty("parent", "minecraft:block/block");
		json.add("textures", textures);
		json.add("elements", elements);
		return json;
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

	/**
	 * A box of leaves (or berries), all six faces drawn: the leaves have holes to look through. Faces wider than a block
	 * are tiled so every texel stays one sixteenth of a block, like on a vanilla leaf block.
	 */
	private static void leafBox(JsonArray out, double from, double y0, double to, double y1, String texture, boolean tinted) {
		for (double[] u : tiles(from, to)) {
			for (double[] v : tiles(y0, y1)) {
				out.add(sheet(Direction.NORTH, vector(u[0], v[0], from), vector(u[1], v[1], from), u, v, texture, tinted, null));
				out.add(sheet(Direction.SOUTH, vector(u[0], v[0], to), vector(u[1], v[1], to), u, v, texture, tinted, null));
				out.add(sheet(Direction.WEST, vector(from, v[0], u[0]), vector(from, v[1], u[1]), u, v, texture, tinted, null));
				out.add(sheet(Direction.EAST, vector(to, v[0], u[0]), vector(to, v[1], u[1]), u, v, texture, tinted, null));
			}
			for (double[] w : tiles(from, to)) {
				out.add(sheet(Direction.UP, vector(u[0], y1, w[0]), vector(u[1], y1, w[1]), u, w, texture, tinted, null));
				out.add(sheet(Direction.DOWN, vector(u[0], y0, w[0]), vector(u[1], y0, w[1]), u, w, texture, tinted, null));
			}
		}
	}

	/**
	 * A vertical plane through the block's centre, from {@code y0} to {@code y1} and {@code from} to {@code to} across,
	 * turned by {@code angle} degrees: twig legs below the leaves, or leaves whose edges stick out as tufts. Tiled like
	 * {@link #leafBox} so the texture is never stretched.
	 */
	private static void plane(JsonArray out, boolean alongX, float angle, String texture, double y0, double y1, boolean tinted, double from, double to) {
		JsonObject rotation = null;
		if (angle != 0) {
			rotation = new JsonObject();
			rotation.add("origin", vector(8, 8, 8));
			rotation.addProperty("axis", "y");
			rotation.addProperty("angle", angle);
		}
		for (double[] u : tiles(from, to)) {
			for (double[] v : tiles(y0, y1)) {
				JsonObject element = alongX
						? sheet(Direction.NORTH, vector(u[0], v[0], 8), vector(u[1], v[1], 8), u, v, texture, tinted, rotation)
						: sheet(Direction.WEST, vector(8, v[0], u[0]), vector(8, v[1], u[1]), u, v, texture, tinted, rotation);
				JsonObject back = new JsonObject();
				back.add("uv", vector4(u[2], 16 - v[3], u[3], 16 - v[2]));
				back.addProperty("texture", texture);
				if (tinted) {
					back.addProperty("tintindex", 0);
				}
				element.getAsJsonObject("faces").add(alongX ? "south" : "east", back);
				element.addProperty("shade", false);
				out.add(element);
			}
		}
	}

	/** Splits {@code from..to} at block edges: each piece with its texture coordinates inside its own block. */
	private static List<double[]> tiles(double from, double to) {
		List<double[]> pieces = new ArrayList<>();
		double a = from;
		while (a < to - 1.0E-6) {
			double edge = Math.floor(a / 16 + 1.0E-6) * 16 + 16;
			double b = Math.min(to, edge);
			double base = edge - 16;
			pieces.add(new double[]{a, b, a - base, b - base});
			a = b;
		}
		return pieces;
	}

	/** One flat face of a leaf box or plane, its texture from the pieces' own texture coordinates. */
	private static JsonObject sheet(Direction side, JsonArray from, JsonArray to, double[] u, double[] v, String texture, boolean tinted, JsonObject rotation) {
		JsonObject face = new JsonObject();
		face.add("uv", side.getAxis().isVertical()
				? vector4(u[2], v[2], u[3], v[3])
				: vector4(u[2], 16 - v[3], u[3], 16 - v[2]));
		face.addProperty("texture", texture);
		if (tinted) {
			face.addProperty("tintindex", 0);
		}
		JsonObject faces = new JsonObject();
		faces.add(side.getSerializedName(), face);
		JsonObject element = new JsonObject();
		element.add("from", from);
		element.add("to", to);
		if (rotation != null) {
			element.add("rotation", rotation);
		}
		element.add("faces", faces);
		return element;
	}

	/**
	 * Height of the duckweed sheet in its own block. A still water block's surface is drawn at 8/9 of a block, 16/9 pixels
	 * below the duckweed's block; the sheet lies a hair above it so it does not flicker with the water.
	 */
	private static final double DUCKWEED_SHEET_Y = -1.7;

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
		element.add("from", vector(0, DUCKWEED_SHEET_Y, 0));
		element.add("to", vector(16, DUCKWEED_SHEET_Y, 16));
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
		generators.generateFlatItem(EamItems.PILLAGER_KING_HELM, ModelTemplates.FLAT_ITEM);
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
		// Dryable food: a darker look while the stack is dried.
		ItemModelGenerators dryable = new ItemModelGenerators(DriedItemModels.output(generators.itemModelOutput), generators.modelOutput);
		dryable.generateFlatItem(EamItems.CRANBERRY, ModelTemplates.FLAT_ITEM);
		dryable.generateFlatItem(EamItems.WOLFBERRY, ModelTemplates.FLAT_ITEM);
		dryable.generateFlatItem(EamItems.COOKED_WOLF_MEAT, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.GLOWSHROOM, ModelTemplates.FLAT_ITEM);
		for (Item item : List.of(EamItems.BURNT_BEEF, EamItems.BURNT_PORKCHOP, EamItems.BURNT_MUTTON, EamItems.BURNT_CHICKEN,
				EamItems.BURNT_RABBIT, EamItems.BURNT_COD, EamItems.BURNT_SALMON, EamItems.BURNT_POTATO, EamItems.BURNT_WOLF_MEAT, EamItems.ASH,
				EamItems.ROTTEN_FOOD, EamItems.WOLF_MEAT)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		generators.generateFlatItem(EamItems.FALLEN_LORD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SKELETON_KNIGHT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LOST_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LOST_CENTURION_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LOST_WALL_COMMANDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LOST_CAVALRYMAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.UNDEAD_WARLOCK_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.PILLAGER_KING_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.ELITE_CROSSBOWMAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.PILLAGER_GUARD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.SWAMP_LURKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WIGHT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.DRAUGR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.ZOMBIE_DUERGAR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.DUERGAR_RENEGADE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.DUERGAR_VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.UPYR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WARG_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WARG_LEADER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.BABA_YAGA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.KOSCHEI_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.DROWNED_SOLDIER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.WOOD_SPIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.VAMPIRE_BAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.BLACK_WIDOW_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FILTERED_LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FILTERED_GLASS_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FLINT_HATCHET, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.BEDROCK_BREAKER, ModelTemplates.FLAT_HANDHELD_ITEM);
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
				EamItems.FERMENTED_ENDER_EYE, EamItems.GARLIC_ORCHID_MIX, EamItems.HERBAL_HONEY)) {
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
		for (Item item : List.of(EamItems.RAW_ADAMANTIUM, EamItems.ADAMANTIUM_NUGGET, EamItems.ADAMANTIUM_INGOT, EamItems.HOT_ADAMANTIUM_INGOT,
				EamItems.ADAMANTIUM_PLATE, EamItems.ADAMANTIUM_HELMET, EamItems.ADAMANTIUM_CHESTPLATE, EamItems.ADAMANTIUM_LEGGINGS,
				EamItems.ADAMANTIUM_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.ADAMANTIUM_SWORD, EamItems.ADAMANTIUM_SHOVEL, EamItems.ADAMANTIUM_PICKAXE, EamItems.ADAMANTIUM_AXE,
				EamItems.ADAMANTIUM_HOE, EamItems.ADAMANTIUM_KNIFE, EamItems.ADAMANTIUM_SAW, EamItems.ADAMANTIUM_HAMMER)) {
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
		BowModels.generate(generators);
		FallenLordItemModels.generate(generators);
		WornGearItemModels.generate(generators);
		PillagerKingItemModels.generate(generators);
		generators.generateFlatItem(EamItems.BLACK_TALISMAN, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.COMMANDER_HORN, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.KOSCHEI_NEEDLE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.KING_CASTLE_MAP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.BLUEPRINT, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.YAGA_GRAVE_MAP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.LAIR_MAP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CRYPT_MAP, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.DUERGAR_BOMB, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.VAMPIRE_HEART, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.HEART_IN_JAR, ModelTemplates.FLAT_ITEM);
		// The same talisman with its crystal glowing, shown while it lights a portal.
		ModelTemplates.FLAT_ITEM.create(TALISMAN_GLOWING_MODEL, TextureMapping.layer0(new Material(TALISMAN_GLOWING_MODEL)), generators.modelOutput);
		SlingshotModels.generate(generators);
	}

	/**
	 * A giant mushroom block like vanilla's, but each block picks one of several skins at random by its position, so a wide
	 * cap doesn't read as one endless texture. The plain skin comes up as often as all the others together.
	 */
	private static void variedMushroomBlock(BlockModelGenerators generators, Block block, String... suffixes) {
		WeightedList.Builder<Variant> skins = WeightedList.<Variant>builder()
				.add(BlockModelGenerators.plainModel(ModelTemplates.SINGLE_FACE.create(block, TextureMapping.defaultTexture(block), generators.modelOutput)),
						suffixes.length);
		for (String suffix : suffixes) {
			skins.add(BlockModelGenerators.plainModel(ModelTemplates.SINGLE_FACE.createWithSuffix(block, suffix,
					TextureMapping.defaultTexture(TextureMapping.getBlockTexture(block, suffix)), generators.modelOutput)), 1);
		}
		MultiVariant skin = new MultiVariant(skins.build());
		MultiVariant skinless = BlockModelGenerators.plainVariant(ModelLocationUtils.decorateBlockModelLocation("mushroom_block_inside"));
		generators.blockStateOutput.accept(MultiPartGenerator.multiPart(block)
				.with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true), skin)
				.with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, true), skin.with(BlockModelGenerators.Y_ROT_90).with(BlockModelGenerators.UV_LOCK))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true), skin.with(BlockModelGenerators.Y_ROT_180).with(BlockModelGenerators.UV_LOCK))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, true), skin.with(BlockModelGenerators.Y_ROT_270).with(BlockModelGenerators.UV_LOCK))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.UP, true), skin.with(BlockModelGenerators.X_ROT_270).with(BlockModelGenerators.UV_LOCK))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, true), skin.with(BlockModelGenerators.X_ROT_90).with(BlockModelGenerators.UV_LOCK))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false), skinless)
				.with(BlockModelGenerators.condition().term(BlockStateProperties.EAST, false), skinless.with(BlockModelGenerators.Y_ROT_90))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, false), skinless.with(BlockModelGenerators.Y_ROT_180))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.WEST, false), skinless.with(BlockModelGenerators.Y_ROT_270))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.UP, false), skinless.with(BlockModelGenerators.X_ROT_270))
				.with(BlockModelGenerators.condition().term(BlockStateProperties.DOWN, false), skinless.with(BlockModelGenerators.X_ROT_90)));
		generators.registerSimpleItemModel(block, TexturedModel.CUBE.createWithSuffix(block, "_inventory", generators.modelOutput));
	}
}
