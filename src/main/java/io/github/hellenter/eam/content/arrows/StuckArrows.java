package io.github.hellenter.eam.content.arrows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Arrows shot into a mob that it carries until it dies, each with the Arrow Recovery level of the bow it came from. */
public record StuckArrows(List<Arrow> arrows) {
	public static final Codec<StuckArrows> CODEC = Arrow.CODEC.listOf().xmap(StuckArrows::new, StuckArrows::arrows);

	public StuckArrows {
		arrows = List.copyOf(arrows);
	}

	public StuckArrows with(Arrow arrow) {
		List<Arrow> more = new ArrayList<>(arrows);
		more.add(arrow);
		return new StuckArrows(more);
	}

	/** One arrow item and the Arrow Recovery level of the bow that shot it. */
	public record Arrow(ItemStack stack, int recoveryLevel) {
		public static final Codec<Arrow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStack.CODEC.fieldOf("stack").forGetter(Arrow::stack),
				Codec.INT.optionalFieldOf("recovery_level", 0).forGetter(Arrow::recoveryLevel)
		).apply(instance, Arrow::new));
	}
}
