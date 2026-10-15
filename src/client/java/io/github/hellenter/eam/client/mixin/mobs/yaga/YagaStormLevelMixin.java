package io.github.hellenter.eam.client.mixin.mobs.yaga;

import io.github.hellenter.eam.client.mobs.yaga.YagaStormClient;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Baba Yaga's storm raises the rain and thunder the client's level reports, near her only. */
@Mixin(Level.class)
abstract class YagaStormLevelMixin {
	@ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
	private float eam$yagaStormRain(float original, float partialTick) {
		return YagaStormClient.weather((Level) (Object) this, original, partialTick);
	}

	@ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
	private float eam$yagaStormThunder(float original, float partialTick) {
		return YagaStormClient.weather((Level) (Object) this, original, partialTick);
	}
}
