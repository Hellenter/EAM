package io.github.hellenter.eam.client.mobs.boar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.boar.Boar;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** An underground boar; piglets are drawn at half size in their own striped coat. */
public class BoarRenderer extends MobRenderer<Boar, LivingEntityRenderState, BoarModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/boar/boar.png");
	private static final Identifier BABY_TEXTURE = Eam.id("textures/entity/boar/boar_baby.png");
	private static final float BABY_SCALE = 0.5F;

	public BoarRenderer(EntityRendererProvider.Context context) {
		super(context, new BoarModel(context.bakeLayer(BoarModel.LAYER)), 0.7F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return state.isBaby ? BABY_TEXTURE : TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		if (state.isBaby) {
			poseStack.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
		}
	}
}
