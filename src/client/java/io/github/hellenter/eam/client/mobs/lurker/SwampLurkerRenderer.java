package io.github.hellenter.eam.client.mobs.lurker;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurker;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** The swamp lurker; nothing is drawn while it hides inside its mud block. */
public class SwampLurkerRenderer extends MobRenderer<SwampLurker, SwampLurkerRenderState, SwampLurkerModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/swamp_lurker/swamp_lurker.png");

	public SwampLurkerRenderer(EntityRendererProvider.Context context) {
		super(context, new SwampLurkerModel(context.bakeLayer(SwampLurkerModel.LAYER)), 0.5F);
	}

	@Override
	public boolean shouldRender(SwampLurker entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		return !entity.isHidden() && super.shouldRender(entity, culler, camX, camY, camZ, partialTicks);
	}

	@Override
	public Identifier getTextureLocation(SwampLurkerRenderState state) {
		return TEXTURE;
	}

	@Override
	public SwampLurkerRenderState createRenderState() {
		return new SwampLurkerRenderState();
	}

	@Override
	public void extractRenderState(SwampLurker entity, SwampLurkerRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.action = entity.action();
		state.actionTime = entity.actionTime(partialTicks);
		state.verticalSpeed = (float) (entity.getY() - entity.yo);
	}
}
