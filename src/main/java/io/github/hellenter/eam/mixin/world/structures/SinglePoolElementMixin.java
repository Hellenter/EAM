package io.github.hellenter.eam.mixin.world.structures;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Abandoned village pieces generate looted. */
@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin {
	@Shadow
	@Final
	protected Holder<StructureProcessorList> processors;

	@ModifyReturnValue(method = "getSettings", at = @At("RETURN"))
	private StructurePlaceSettings eam$lootAbandoned(StructurePlaceSettings settings) {
		return VillageModule.pieceSettings(this.processors, settings);
	}
}
