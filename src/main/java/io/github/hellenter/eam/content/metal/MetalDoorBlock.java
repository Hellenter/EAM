package io.github.hellenter.eam.content.metal;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** A metal door that, like the iron one, opens only with redstone. */
public class MetalDoorBlock extends DoorBlock {
	public MetalDoorBlock(BlockBehaviour.Properties properties) {
		super(BlockSetType.IRON, properties);
	}
}
