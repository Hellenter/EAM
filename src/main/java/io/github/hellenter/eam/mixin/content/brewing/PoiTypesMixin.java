package io.github.hellenter.eam.mixin.content.brewing;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.content.brewing.BrewingModule;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** The silver brewing stand counts as the cleric's job site. */
@Mixin(PoiTypes.class)
public abstract class PoiTypesMixin {
	@ModifyReturnValue(method = "forState", at = @At("RETURN"))
	private static Optional<Holder<PoiType>> eam$silverStand(Optional<Holder<PoiType>> type, BlockState state) {
		return BrewingModule.poiType(state, type);
	}

	@ModifyReturnValue(method = "hasPoi", at = @At("RETURN"))
	private static boolean eam$silverStandHasPoi(boolean has, BlockState state) {
		return has || BrewingModule.poiType(state, Optional.empty()).isPresent();
	}
}
