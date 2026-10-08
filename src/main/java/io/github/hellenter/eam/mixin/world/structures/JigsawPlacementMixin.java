package io.github.hellenter.eam.mixin.world.structures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Villages start abandoned until they are unlocked. */
@Mixin(JigsawPlacement.class)
public abstract class JigsawPlacementMixin {
	@WrapOperation(method = "addPieces", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/structure/pools/StructureTemplatePool;getRandomTemplate(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/levelgen/structure/pools/StructurePoolElement;"))
	private static StructurePoolElement eam$abandonedVillages(StructureTemplatePool pool, RandomSource random,
			Operation<StructurePoolElement> original, @Local(argsOnly = true) Holder<StructureTemplatePool> startPool) {
		return VillageModule.startElement(startPool, pool, random, () -> original.call(pool, random));
	}
}
