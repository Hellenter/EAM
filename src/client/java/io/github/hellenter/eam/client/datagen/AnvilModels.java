package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonObject;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;

/** Copper, silver, mithril and adamantium anvils: the vanilla anvil template with recolored textures, one model per wear stage. */
final class AnvilModels {
	private static final String[] TOP_SUFFIX = {"_top", "_top_chipped", "_top_damaged"};

	private AnvilModels() {
	}

	static void generate(BlockModelGenerators generators) {
		anvil(generators, EamBlocks.COPPER_ANVIL);
		anvil(generators, EamBlocks.SILVER_ANVIL);
		anvil(generators, EamBlocks.MITHRIL_ANVIL);
		anvil(generators, EamBlocks.ADAMANTIUM_ANVIL);
	}

	private static void anvil(BlockModelGenerators generators, Block block) {
		Identifier body = BuiltInRegistries.BLOCK.getKey(block).withPrefix("block/");
		Identifier[] models = new Identifier[TOP_SUFFIX.length];
		for (int wear = 0; wear < TOP_SUFFIX.length; wear++) {
			Identifier top = body.withSuffix(TOP_SUFFIX[wear]);
			Identifier location = wear == 0 ? ModelLocationUtils.getModelLocation(block) : ModelLocationUtils.getModelLocation(block, "_wear" + wear);
			generators.modelOutput.accept(location, () -> {
				JsonObject textures = new JsonObject();
				textures.addProperty("particle", body.toString());
				textures.addProperty("body", body.toString());
				textures.addProperty("top", top.toString());
				JsonObject model = new JsonObject();
				model.addProperty("parent", "minecraft:block/template_anvil");
				model.add("textures", textures);
				return model;
			});
			models[wear] = location;
		}
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(
				PropertyDispatch.initial(AnvilBlock.FACING, MetalAnvilBlock.WEAR).generate((facing, wear) -> {
					MultiVariant variant = BlockModelGenerators.plainVariant(models[wear]);
					return variant.with(rotation(facing));
				})));
		generators.registerSimpleItemModel(block, models[0]);
	}

	/** The vanilla anvil faces south unrotated. */
	private static VariantMutator rotation(Direction facing) {
		return switch (facing) {
			case EAST -> BlockModelGenerators.Y_ROT_270;
			case NORTH -> BlockModelGenerators.Y_ROT_180;
			case WEST -> BlockModelGenerators.Y_ROT_90;
			default -> BlockModelGenerators.NOP;
		};
	}
}
