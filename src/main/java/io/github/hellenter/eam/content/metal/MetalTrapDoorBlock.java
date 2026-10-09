package io.github.hellenter.eam.content.metal;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** A metal trapdoor that, like the iron one, opens only with redstone. */
public class MetalTrapDoorBlock extends TrapDoorBlock {
	public MetalTrapDoorBlock(BlockBehaviour.Properties properties) {
		super(BlockSetType.IRON, properties);
	}
}
