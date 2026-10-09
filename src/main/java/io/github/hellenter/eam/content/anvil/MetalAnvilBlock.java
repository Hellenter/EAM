package io.github.hellenter.eam.content.anvil;

import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/**
 * An anvil of a metal other than iron: works like the vanilla anvil, but repairs only items of its tier and below. It wears
 * through the same three stages as the vanilla anvil, kept in {@link #WEAR} instead of separate blocks.
 */
public class MetalAnvilBlock extends AnvilBlock {
	/** 0 new, 1 chipped, 2 damaged; one more wear breaks it. */
	public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, 2);

	private final int tier;

	public MetalAnvilBlock(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
		registerDefaultState(defaultBlockState().setValue(WEAR, 0));
	}

	public int tier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WEAR);
	}

	/** The next wear stage, or null when the anvil breaks; what {@link AnvilBlock#damage} gives vanilla anvils. */
	public static @Nullable BlockState worn(BlockState state) {
		int wear = state.getValue(WEAR);
		return wear < 2 ? state.setValue(WEAR, wear + 1) : null;
	}
}
