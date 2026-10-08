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

/** Added to the vanilla crafting table: keeps the crafting tools between uses and drops them when the table is broken. */
public class CraftingTableBlockEntity extends BlockEntity {
	private final SimpleContainer tools = new SimpleContainer(CraftingTableMenu.TOOL_COUNT) {
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
		// Tables from before there were three slots kept one tool under "Tool".
		tools.setItem(0, input.read("Tool", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		for (int i = 0; i < tools.getContainerSize(); i++) {
			int slot = i;
			input.read("Tool" + i, ItemStack.CODEC).ifPresent(stack -> tools.setItem(slot, stack));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		for (int i = 0; i < tools.getContainerSize(); i++) {
			ItemStack tool = tools.getItem(i);
			if (!tool.isEmpty()) {
				output.store("Tool" + i, ItemStack.CODEC, tool);
			}
		}
	}
}
