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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

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
	}

	/** One box per position on the top face, from the block's own shape; the block state picks the model by position. */
	private static void groundItem(BlockModelGenerators generators, Block block, Identifier texture) {
		GroundItemLogic.Shape shape = ((GroundItemBlock) block).shape();
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(
				PropertyDispatch.initial(GroundItemBlock.POSITION).generate(position -> {
					Identifier model = ModelLocationUtils.getModelLocation(block, "_" + position);
					GroundItemLogic.Box box = shape.at(position);
					generators.modelOutput.accept(model, () -> boxModel(texture, box.minX(), box.height(), box.minZ(), box.maxX(), box.maxZ()));
					return BlockModelGenerators.plainVariant(model);
				})));
	}

	private static JsonObject boxModel(Identifier texture, double x1, double height, double z1, double x2, double z2) {
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
		element.add("from", vector(x1, 0, z1));
		element.add("to", vector(x2, height, z2));
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
		generators.generateFlatItem(EamItems.LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FILTERED_LEATHER_FLASK, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FLINT_HATCHET, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.FLINT_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
		generators.generateFlatItem(EamItems.PEBBLE, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.FLINT_SHARD, ModelTemplates.FLAT_ITEM);
		generators.generateFlatItem(EamItems.CAMP_FIRE, ModelTemplates.FLAT_ITEM);
	}
}
