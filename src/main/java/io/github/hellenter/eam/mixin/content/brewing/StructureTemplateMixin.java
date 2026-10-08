package io.github.hellenter.eam.mixin.content.brewing;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.content.brewing.BrewingModule;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Structures place silver brewing stands where their templates have vanilla ones. */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
	@ModifyReturnValue(method = "processBlockInfos", at = @At("RETURN"))
	private static List<StructureTemplate.StructureBlockInfo> eam$silverStands(List<StructureTemplate.StructureBlockInfo> blocks) {
		return BrewingModule.replaceStands(blocks);
	}
}
