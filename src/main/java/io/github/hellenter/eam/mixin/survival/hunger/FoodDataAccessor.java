package io.github.hellenter.eam.mixin.survival.hunger;

import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccessor {
	@Accessor("exhaustionLevel")
	float eam$getExhaustionLevel();

	@Accessor("exhaustionLevel")
	void eam$setExhaustionLevel(float exhaustionLevel);

	@Accessor("tickTimer")
	int eam$getTickTimer();

	@Accessor("tickTimer")
	void eam$setTickTimer(int tickTimer);
}
