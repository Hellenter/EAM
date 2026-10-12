package io.github.hellenter.eam.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.hellenter.eam.dimension.UnderworldDimension;
import io.github.hellenter.eam.dimension.UnderworldPortals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * In the underworld, portals are found and built only in the level the traveller is headed for; exit portals in the
 * underworld, the overworld and the nether are built at the nearest safe spot.
 */
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@WrapMethod(method = "findClosestPortalPosition")
	private Optional<BlockPos> eam$findInLevel(BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder,
			Operation<Optional<BlockPos>> original) {
		if (UnderworldDimension.is(this.level)) {
			return UnderworldPortals.findClosestPortal(this.level, approximateExitPos, toNether, worldBorder);
		}
		return original.call(approximateExitPos, toNether, worldBorder);
	}

	@WrapMethod(method = "createPortal")
	private Optional<BlockUtil.FoundRectangle> eam$createInLevel(BlockPos origin, Direction.Axis portalAxis,
			Operation<Optional<BlockUtil.FoundRectangle>> original) {
		if (UnderworldPortals.buildsPortalsIn(this.level)) {
			return UnderworldPortals.createPortal(this.level, origin, portalAxis);
		}
		return original.call(origin, portalAxis);
	}
}
