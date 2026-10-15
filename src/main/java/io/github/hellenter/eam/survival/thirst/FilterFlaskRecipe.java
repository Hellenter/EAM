package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;

import java.util.List;

/**
 * Shapeless recipe that turns a plain flask into a filtered one, or puts a fresh filter into a filtered flask, and
 * keeps what the flask held (its water), the way vanilla transmute recipes keep components. Transmute itself allows
 * only one kind of material.
 */
public class FilterFlaskRecipe extends ShapelessRecipe {
	public static final MapCodec<FilterFlaskRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
			CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
			Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients)
	).apply(i, FilterFlaskRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, FilterFlaskRecipe> STREAM_CODEC = StreamCodec.composite(
			Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
			CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
			ItemStackTemplate.STREAM_CODEC, o -> o.result,
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
			FilterFlaskRecipe::new);
	public static final RecipeSerializer<FilterFlaskRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final ItemStackTemplate result;
	private final List<Ingredient> ingredients;

	public FilterFlaskRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
			List<Ingredient> ingredients) {
		super(commonInfo, bookInfo, result, ingredients);
		this.result = result;
		this.ingredients = ingredients;
	}

	@Override
	@SuppressWarnings("unchecked")
	public RecipeSerializer<ShapelessRecipe> getSerializer() {
		return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		for (ItemStack stack : input.items()) {
			if (stack.getItem() instanceof FlaskItem) {
				ItemStack flask = TransmuteRecipe.createWithOriginalComponents(result, stack);
				// The filter is new, whatever was left of the old one.
				flask.set(ThirstModule.FILTER, flask.getPrototype().get(ThirstModule.FILTER));
				return flask;
			}
		}
		return super.assemble(input);
	}
}
