package io.github.hellenter.eam.content.brewing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** A brewing recipe of the silver stand: same shape as vanilla's, its own recipe type, so the two stands don't share recipes. */
public class SilverBrewingRecipe extends BrewingRecipe {
	public static final MapCodec<BrewingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PotionIngredient.MAP_CODEC.fieldOf("input").forGetter(BrewingRecipe::getInput),
			PotionIngredient.MAP_CODEC.fieldOf("reagent").forGetter(BrewingRecipe::getReagent),
			ItemStackTemplate.CODEC.fieldOf("output").forGetter(BrewingRecipe::getOutput)
	).apply(i, SilverBrewingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, BrewingRecipe> STREAM_CODEC = StreamCodec.composite(
			PotionIngredient.STREAM_CODEC, BrewingRecipe::getInput,
			PotionIngredient.STREAM_CODEC, BrewingRecipe::getReagent,
			ItemStackTemplate.STREAM_CODEC, BrewingRecipe::getOutput,
			SilverBrewingRecipe::new);

	public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public SilverBrewingRecipe(PotionIngredient input, PotionIngredient reagent, ItemStackTemplate output) {
		super(input, reagent, output);
	}

	@Override
	public RecipeType<BrewingRecipe> getType() {
		return BrewingModule.RECIPE_TYPE;
	}

	@Override
	public RecipeSerializer<BrewingRecipe> getSerializer() {
		return SERIALIZER;
	}
}
