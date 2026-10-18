package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** The template's palettes, for the blocks a blueprint shows and the villagers build. */
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {
	@Accessor("palettes")
	List<StructureTemplate.Palette> eam$palettes();
}
