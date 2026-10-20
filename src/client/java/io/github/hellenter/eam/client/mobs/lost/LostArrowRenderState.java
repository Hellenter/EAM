package io.github.hellenter.eam.client.mobs.lost;

import io.github.hellenter.eam.mobs.entity.lost.LostArrow;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;

public class LostArrowRenderState extends ArrowRenderState {
	public LostArrow.Kind kind = LostArrow.Kind.PLAIN;
}
