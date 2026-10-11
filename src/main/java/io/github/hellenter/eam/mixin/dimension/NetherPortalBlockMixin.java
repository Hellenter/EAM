package io.github.hellenter.eam.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.hellenter.eam.dimension.UnderworldPortals;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Nether portals lead between the overworld and the underworld, and between the underworld's lower level and the nether. */
@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
	@Shadow
	private @Nullable TeleportTransition getExitPortal(ServerLevel newLevel, Entity entity, BlockPos portalEntryPos,
			BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder) {
		throw new AssertionError();
	}

	@WrapMethod(method = "getPortalDestination")
	private @Nullable TeleportTransition eam$underworldRoute(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos,
			Operation<TeleportTransition> original) {
		ServerLevel target = UnderworldPortals.targetLevel(currentLevel, portalEntryPos);
		if (target == null) {
			return original.call(currentLevel, entity, portalEntryPos);
		}
		return this.getExitPortal(target, entity, portalEntryPos, UnderworldPortals.approximateExit(currentLevel, target, entity, portalEntryPos),
				target.dimension() == Level.NETHER, target.getWorldBorder());
	}
}
