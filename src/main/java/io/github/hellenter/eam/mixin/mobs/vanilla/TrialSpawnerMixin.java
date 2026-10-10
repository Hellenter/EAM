package io.github.hellenter.eam.mixin.mobs.vanilla;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.hellenter.eam.mobs.vanilla.crypt.CryptSpawners;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Crypt spawners put their mobs on floors a few blocks above or below them, not only at their own height. */
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerMixin {
	@Shadow
	private TrialSpawner.FullConfig config;

	@ModifyExpressionValue(method = "spawnMob", at = @At(value = "INVOKE",
			target = "Ljava/util/Optional;orElseGet(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
	private Object eam$cryptSpawnHeight(Object spawnPos, ServerLevel level, BlockPos spawnerPos) {
		return CryptSpawners.spawnPos(level, spawnerPos, this.config, (Vec3) spawnPos);
	}
}
