package io.github.hellenter.eam.world.shield;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import org.apache.commons.lang3.mutable.MutableFloat;

public final class ShieldLogic {
	private ShieldLogic() {
	}

	/** Seconds {@code shield} stays disabled instead of {@code seconds}, after its enchantments' disable time effects. */
	public static float disableSeconds(ItemStack shield, float seconds, RandomSource random) {
		MutableFloat value = new MutableFloat(seconds);
		EnchantmentHelper.runIterationOnItem(shield, (enchantment, level) ->
				enchantment.value().modifyUnfilteredValue(ShieldModule.DISABLE_TIME, random, level, value));
		return Math.max(0.0F, value.floatValue());
	}
}
