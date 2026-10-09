package io.github.hellenter.eam.world.forging;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Added to every anvil: keeps the hammer between uses and drops it when the anvil is broken or falls. When an iron anvil
 * wears into the next block (anvil to chipped to damaged), the hammer moves over to the new block entity.
 */
public class AnvilBlockEntity extends BlockEntity {
	/** Server: a hammer on its way from a worn anvil to the block entity that replaces it in the same block change. */
	private static @Nullable Moving moving;

	private final SimpleContainer hammer = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			AnvilBlockEntity.this.setChanged();
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return ForgingModule.isHammer(stack);
		}
	};

	public AnvilBlockEntity(BlockPos pos, BlockState state) {
		super(ForgingTypes.ANVIL, pos, state);
	}

	public Container hammer() {
		return hammer;
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		Moving pending = moving;
		if (pending != null && pending.level() == level && pending.pos().equals(worldPosition)) {
			moving = null;
			hammer.setItem(0, pending.hammer());
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level == null || hammer.isEmpty()) {
			return;
		}
		// The chunk already holds the new block here: a worn anvil keeps its hammer.
		if (level.getBlockState(pos).is(BlockTags.ANVIL)) {
			moving = new Moving(level, pos.immutable(), hammer.removeItemNoUpdate(0));
		} else {
			Containers.dropContents(level, pos, hammer);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		hammer.setItem(0, input.read("Hammer", ItemStack.CODEC).orElse(ItemStack.EMPTY));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ItemStack stack = hammer.getItem(0);
		if (!stack.isEmpty()) {
			output.store("Hammer", ItemStack.CODEC, stack);
		}
	}

	private record Moving(Level level, BlockPos pos, ItemStack hammer) {
	}
}
