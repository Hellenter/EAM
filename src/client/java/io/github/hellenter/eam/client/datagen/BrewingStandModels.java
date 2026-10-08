package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonObject;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.BrewingStandBlock;

/** The silver brewing stand: vanilla's brewing stand models with the silver rod texture, same multipart layout. */
final class BrewingStandModels {
	private BrewingStandModels() {
	}

	static void generate(BlockModelGenerators generators) {
		Block block = EamBlocks.SILVER_BREWING_STAND;
		String stand = ModelLocationUtils.getModelLocation(block).toString();
		MultiPartGenerator multiPart = MultiPartGenerator.multiPart(block)
				.with(BlockModelGenerators.plainVariant(model(generators, block, "", stand)));
		for (int i = 0; i < BrewingStandBlock.HAS_BOTTLE.length; i++) {
			BooleanProperty hasBottle = BrewingStandBlock.HAS_BOTTLE[i];
			multiPart.with(BlockModelGenerators.condition().term(hasBottle, true),
					BlockModelGenerators.plainVariant(model(generators, block, "_bottle" + i, stand)));
			multiPart.with(BlockModelGenerators.condition().term(hasBottle, false),
					BlockModelGenerators.plainVariant(model(generators, block, "_empty" + i, stand)));
		}
		generators.blockStateOutput.accept(multiPart);
	}

	/** A child of the vanilla model with the rod texture swapped; the stone base stays vanilla's. */
	private static Identifier model(BlockModelGenerators generators, Block block, String suffix, String standTexture) {
		Identifier location = ModelLocationUtils.getModelLocation(block, suffix);
		generators.modelOutput.accept(location, () -> {
			JsonObject textures = new JsonObject();
			textures.addProperty("particle", standTexture);
			textures.addProperty("stand", standTexture);
			JsonObject model = new JsonObject();
			model.addProperty("parent", "minecraft:block/brewing_stand" + suffix);
			model.add("textures", textures);
			return model;
		});
		return location;
	}
}
