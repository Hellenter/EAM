package io.github.hellenter.eam.mixin.content.heart;

import com.mojang.math.Transformation;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the heart in a jar set up the display entity of its flying heart. */
@Mixin(Display.class)
public interface DisplayInvoker {
	@Invoker("setTransformation")
	void eam$setTransformation(Transformation transformation);

	@Invoker("setPosRotInterpolationDuration")
	void eam$setPosRotInterpolationDuration(int ticks);

	@Invoker("setBillboardConstraints")
	void eam$setBillboardConstraints(Display.BillboardConstraints constraints);

	@Invoker("setBrightnessOverride")
	void eam$setBrightnessOverride(Brightness brightness);
}
