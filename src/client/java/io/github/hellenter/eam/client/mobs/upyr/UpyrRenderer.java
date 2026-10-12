package io.github.hellenter.eam.client.mobs.upyr;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.upyr.Upyr;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** The upyr, on its own model and texture; arms out like a zombie. */
public class UpyrRenderer extends HumanoidMobRenderer<Upyr, ZombieRenderState, UpyrModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/upyr/upyr.png");

	public UpyrRenderer(EntityRendererProvider.Context context) {
		super(context, new UpyrModel(context.bakeLayer(UpyrModel.LAYER)), 0.5F);
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
	public void extractRenderState(Upyr entity, ZombieRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAggressive = entity.isAggressive();
	}
}
