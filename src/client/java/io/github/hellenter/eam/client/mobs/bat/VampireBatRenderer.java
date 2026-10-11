package io.github.hellenter.eam.client.mobs.bat;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.resources.Identifier;

/** The vampire bat: the vanilla bat model a third bigger, in its own violet-blue skin with red eyes. */
public class VampireBatRenderer extends BatRenderer {
	private static final Identifier TEXTURE = Eam.id("textures/entity/bat/vampire_bat.png");
	private static final float SCALE = 1.3F;

	public VampireBatRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0.25F * SCALE;
	}

	@Override
	public Identifier getTextureLocation(BatRenderState state) {
		return TEXTURE;
	}

	@Override
	protected void scale(BatRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
