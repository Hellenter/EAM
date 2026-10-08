package io.github.hellenter.eam.mixin.mobs.vanilla;

import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A mob digging toward its target keeps it even though the walls hide it. */
@Mixin(TargetGoal.class)
public interface TargetGoalAccessor {
	@Accessor("unseenTicks")
	void eam$setUnseenTicks(int ticks);
}
