package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class EamModelProvider extends FabricModelProvider {
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
		generators.createFurnace(EamBlocks.OBSIDIAN_BLAST_FURNACE, TexturedModel.ORIENTABLE_ONLY_TOP);
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
		generators.generateFlatItem(EamItems.FALLEN_LORD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
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
		for (Item item : List.of(EamItems.RUSTY_IRON_HELMET, EamItems.RUSTY_IRON_CHESTPLATE, EamItems.RUSTY_IRON_LEGGINGS,
				EamItems.RUSTY_IRON_BOOTS, EamItems.RUSTY_CHAINMAIL_HELMET, EamItems.RUSTY_CHAINMAIL_CHESTPLATE,
				EamItems.RUSTY_CHAINMAIL_LEGGINGS, EamItems.RUSTY_CHAINMAIL_BOOTS)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		for (Item item : List.of(EamItems.RUSTY_IRON_SWORD, EamItems.RUSTY_IRON_SHOVEL, EamItems.RUSTY_IRON_PICKAXE, EamItems.RUSTY_IRON_AXE,
				EamItems.RUSTY_IRON_HOE)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
		}
		for (Item item : List.of(EamItems.COPPER_ARROW, EamItems.SILVER_ARROW, EamItems.IRON_ARROW, EamItems.MITHRIL_ARROW)) {
			generators.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
		}
		PotionModels.generate(generators);
		// generateBow expects the idle model to exist already, as vanilla's hand-written bow.json does.
		ModelTemplates.BOW.create(ModelLocationUtils.getModelLocation(EamItems.SLINGSHOT), TextureMapping.layer0(EamItems.SLINGSHOT), generators.modelOutput);
		generators.generateBow(EamItems.SLINGSHOT);
	}
}
