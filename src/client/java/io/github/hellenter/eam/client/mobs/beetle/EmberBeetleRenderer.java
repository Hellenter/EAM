package io.github.hellenter.eam.client.mobs.beetle;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.beetle.EmberBeetle;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** An ember beetle: its coal shell, with the red cracks and the eyes drawn again at full brightness so they glow in the dark. */
public class EmberBeetleRenderer extends MobRenderer<EmberBeetle, LivingEntityRenderState, EmberBeetleModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/ember_beetle/ember_beetle.png");
	private static final RenderType GLOW = RenderTypes.eyes(Eam.id("textures/entity/ember_beetle/ember_beetle_glow.png"));

	public EmberBeetleRenderer(EntityRendererProvider.Context context) {
		super(context, new EmberBeetleModel(context.bakeLayer(EmberBeetleModel.LAYER)), 0.45F);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
