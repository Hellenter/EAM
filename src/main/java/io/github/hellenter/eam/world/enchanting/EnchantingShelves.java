package io.github.hellenter.eam.world.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;

/**
 * Counts the bookshelves around an enchanting table for the choice of enchantments. Same places as vanilla (a ring two
 * blocks out, at the table's height and one above, 32 in all, with air between) but without vanilla's cap of 15.
 */
public final class EnchantingShelves {
	private EnchantingShelves() {
	}

	public static int count(Level level, BlockPos table) {
		int count = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
				count++;
			}
		}
		return count;
	}

	/** An enchanting menu that knows the shelves around its table; on the client the server keeps the number in sync. */
	public interface Counted {
		int eam$shelves();
	}
}
