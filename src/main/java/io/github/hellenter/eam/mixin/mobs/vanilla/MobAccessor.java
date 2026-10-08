package io.github.hellenter.eam.mixin.mobs.vanilla;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets modules add goals to vanilla mobs after they load. */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("goalSelector")
	GoalSelector eam$goalSelector();

	@Accessor("targetSelector")
	GoalSelector eam$targetSelector();
}
