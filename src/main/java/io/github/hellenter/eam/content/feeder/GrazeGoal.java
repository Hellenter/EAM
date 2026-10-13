package io.github.hellenter.eam.content.feeder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.EnumSet;

/**
 * Livestock that has not had its meal today stops to eat the grass it stands on, like a sheep: a grass plant or the
 * grass of the block underneath, which turns to dirt. Each bite is half a meal, see {@link FeederLogic#bite}.
 */
public class GrazeGoal extends Goal {
	private static final int EAT_TICKS = 40;
	/** One in this many ticks a hungry adult standing on grass starts to graze. */
	private static final int CHANCE = 200;

	private final Animal animal;
	private int eatTicks;

	public GrazeGoal(Animal animal) {
		this.animal = animal;
		setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (!FeederModule.enabled() || animal.getRandom().nextInt(reducedTickDelay(animal.isBaby() ? CHANCE / 4 : CHANCE)) != 0) {
			return false;
		}
		if (!FeederLogic.canEat(animal.getAttached(FeederModule.LIVESTOCK_STATE), FeederModule.today(animal.level()))) return false;
		BlockPos pos = animal.blockPosition();
		return animal.level().getBlockState(pos).is(BlockTags.EDIBLE_FOR_SHEEP) || animal.level().getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
	}

	@Override
	public void start() {
		eatTicks = adjustedTickDelay(EAT_TICKS);
		// Sheep lower their head for this event; other animals ignore it.
		animal.level().broadcastEntityEvent(animal, (byte) 10);
		animal.getNavigation().stop();
	}

	@Override
	public void stop() {
		eatTicks = 0;
	}

	@Override
	public boolean canContinueToUse() {
		return eatTicks > 0;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		eatTicks = Math.max(0, eatTicks - 1);
		if (eatTicks != adjustedTickDelay(4) || !(animal.level() instanceof ServerLevel level)) return;
		BlockPos pos = animal.blockPosition();
		boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
		if (level.getBlockState(pos).is(BlockTags.EDIBLE_FOR_SHEEP)) {
			if (griefing) level.destroyBlock(pos, false);
		} else if (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
			if (griefing) {
				level.levelEvent(2001, pos.below(), Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
				level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		} else {
			return;
		}
		FeederModule.bite(animal);
		animal.ate();
	}
}
