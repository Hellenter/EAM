package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import org.jspecify.annotations.Nullable;

/** A copy of Koschei in ice: his model and poses, in clouded see-through ice; its eyes and staff glow as his do. */
public class KoscheiIceCopyRenderer extends KoscheiRenderer {
	private static final Identifier TEXTURE = Eam.id("textures/entity/koschei/koschei_ice_copy.png");
	/** A little see-through. */
	private static final int TINT = ARGB.color(205, 255, 255, 255);

	public KoscheiIceCopyRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(KoscheiRenderState state) {
		return TEXTURE;
	}

	@Override
	protected @Nullable RenderType getRenderType(KoscheiRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
		if (!isBodyVisible && !forceTransparent) {
			return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
		}
		return RenderTypes.entityTranslucent(TEXTURE);
	}

	@Override
	protected int getModelTint(KoscheiRenderState state) {
		return TINT;
	}
}
