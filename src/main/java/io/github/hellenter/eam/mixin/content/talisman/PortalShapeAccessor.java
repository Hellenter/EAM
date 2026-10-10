package io.github.hellenter.eam.mixin.content.talisman;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.portal.PortalShape;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a portal frame's inside is, so the talisman's particles can fill it. */
@Mixin(PortalShape.class)
public interface PortalShapeAccessor {
	@Accessor("bottomLeft")
	BlockPos eam$bottomLeft();

	@Accessor("rightDir")
	Direction eam$rightDir();

	@Accessor("width")
	int eam$width();

	@Accessor("height")
	int eam$height();
}
