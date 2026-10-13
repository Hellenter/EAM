package io.github.hellenter.eam.client.mobs.warg;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.warg.Warg;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** A warg or a pack leader; the leader is drawn a fifth bigger, with his own darker coat. */
public class WargRenderer extends MobRenderer<Warg, WargRenderState, WargModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/warg/warg.png");
	private static final Identifier LEADER_TEXTURE = Eam.id("textures/entity/warg/warg_leader.png");
	private static final float LEADER_SCALE = 1.2F;

	public WargRenderer(EntityRendererProvider.Context context) {
		super(context, new WargModel(context.bakeLayer(WargModel.LAYER)), 0.6F);
	}

	@Override
	public Identifier getTextureLocation(WargRenderState state) {
		return state.leader ? LEADER_TEXTURE : TEXTURE;
	}

	@Override
	public WargRenderState createRenderState() {
		return new WargRenderState();
	}

	@Override
	public void extractRenderState(Warg entity, WargRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.leader = entity.isLeader();
		state.angry = entity.isAggressive();
	}

	@Override
	protected void scale(WargRenderState state, PoseStack poseStack) {
		if (state.leader) {
			poseStack.scale(LEADER_SCALE, LEADER_SCALE, LEADER_SCALE);
		}
	}
}
