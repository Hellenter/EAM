package io.github.hellenter.eam.mixin.mobs.vanilla.raid;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.hellenter.eam.mobs.vanilla.raid.VillageRaidsModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** No raid starts near a lit Pillager King's helm. */
@Mixin(Raids.class)
abstract class RaidsMixin {
	@WrapMethod(method = "createOrExtendRaid")
	private @Nullable Raid eam$helmGuards(ServerPlayer player, BlockPos raidPosition, Operation<Raid> original) {
		return VillageRaidsModule.guarded(player.level(), raidPosition) ? null : original.call(player, raidPosition);
	}
}
