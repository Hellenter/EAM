package io.github.hellenter.eam.world.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Added to the vanilla crafting table: keeps the crafting tool between uses and drops it when the table is broken. */
public class CraftingTableBlockEntity extends BlockEntity {
	private final SimpleContainer tools = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			CraftingTableBlockEntity.this.setChanged();
		}
	};

	public CraftingTableBlockEntity(BlockPos pos, BlockState state) {
		super(CraftingTypes.CRAFTING_TABLE, pos, state);
	}

	public Container tools() {
		return tools;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, tools);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tools.setItem(0, input.read("Tool", ItemStack.CODEC).orElse(ItemStack.EMPTY));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ItemStack tool = tools.getItem(0);
		if (!tool.isEmpty()) {
			output.store("Tool", ItemStack.CODEC, tool);
		}
	}
}
