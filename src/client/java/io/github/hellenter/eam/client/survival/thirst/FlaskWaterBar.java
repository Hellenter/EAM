package io.github.hellenter.eam.client.survival.thirst;

import io.github.hellenter.eam.survival.thirst.FlaskItem;
import io.github.hellenter.eam.survival.thirst.FlaskLogic;
import io.github.hellenter.eam.survival.thirst.FlaskWater;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/**
 * Water level of a flask, drawn above the item icon like the vanilla durability bar is drawn below it, so a
 * filtered flask shows both its water and its filter.
 */
public final class FlaskWaterBar {
	private FlaskWaterBar() {
	}

	public static void extract(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
		if (!(stack.getItem() instanceof FlaskItem)) {
			return;
		}
		FlaskWater water = stack.getOrDefault(ThirstModule.FLASK_WATER, FlaskWater.EMPTY);
		if (water.isEmpty()) {
			return;
		}
		int left = x + 2;
		int top = y + 1;
		graphics.fill(RenderPipelines.GUI, left, top, left + 13, top + 2, 0xFF000000);
		graphics.fill(RenderPipelines.GUI, left, top, left + FlaskLogic.barWidth(water.amount()), top + 1,
				ARGB.opaque(FlaskLogic.barColor(water.amount())));
	}
}
