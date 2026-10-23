package io.github.hellenter.eam.client.mobs.frozen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.frozen.FrozenCorpse;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** The frozen corpse, on its own model and texture; arms out like a zombie. */
public class FrozenCorpseRenderer extends HumanoidMobRenderer<FrozenCorpse, ZombieRenderState, FrozenCorpseModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/frozen_corpse/frozen_corpse.png");

	public FrozenCorpseRenderer(EntityRendererProvider.Context context) {
		super(context, new FrozenCorpseModel(context.bakeLayer(FrozenCorpseModel.LAYER)), 0.5F);
	}

	@Override
	public Identifier getTextureLocation(ZombieRenderState state) {
		return TEXTURE;
	}

	@Override
	public ZombieRenderState createRenderState() {
		return new ZombieRenderState();
	}

	@Override
	public void extractRenderState(FrozenCorpse entity, ZombieRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAggressive = entity.isAggressive();
	}
}
