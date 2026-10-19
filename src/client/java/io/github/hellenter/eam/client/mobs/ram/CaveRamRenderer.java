package io.github.hellenter.eam.client.mobs.ram;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.ram.CaveRam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** A cave ram; lambs are drawn at half size in their own lighter, softer fleece. */
public class CaveRamRenderer extends MobRenderer<CaveRam, LivingEntityRenderState, CaveRamModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/cave_ram/cave_ram.png");
	private static final Identifier LAMB_TEXTURE = Eam.id("textures/entity/cave_ram/cave_ram_lamb.png");
	private static final float LAMB_SCALE = 0.5F;

	public CaveRamRenderer(EntityRendererProvider.Context context) {
		super(context, new CaveRamModel(context.bakeLayer(CaveRamModel.LAYER)), 0.7F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return state.isBaby ? LAMB_TEXTURE : TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		if (state.isBaby) {
			poseStack.scale(LAMB_SCALE, LAMB_SCALE, LAMB_SCALE);
		}
	}
}
