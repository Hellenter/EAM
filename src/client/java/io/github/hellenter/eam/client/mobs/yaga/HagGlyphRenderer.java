package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Glyph;
import io.github.hellenter.eam.mobs.entity.yaga.HagGlyph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Baba Yaga's glyphs: a glowing circle with a pentagram laid flat on the ground, slowly turning, each kind in its own
 * colour (violet blinding, green draining, fiery orange harm) and the acid bolt's bubbling puddle. The glyph of harm
 * pulses faster and faster as it is about to strike, then flares. The small copy over her head faces the camera.
 */
public class HagGlyphRenderer extends EntityRenderer<HagGlyph, HagGlyphRenderer.State> {
	private static final RenderType[] TYPES = {
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_blind.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_drain.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_harm.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_acid.png")),
	};
	private static final int FADE = 10;
	private static final int HARM_FLASH = 10;

	public HagGlyphRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HagGlyph entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.kind = entity.kind();
		state.radius = entity.radius();
		state.lifetime = entity.lifetime();
		state.preview = entity.isPreview();
	}

	@Override
	protected AABB getBoundingBoxForCulling(HagGlyph entity, float partialTicks) {
		float r = entity.radius();
		return entity.getBoundingBox().inflate(r, 1.0, r);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		float age = state.ageInTicks;
		float alpha = Math.min(1.0F, age / FADE);
		float scale = 1.0F;
		float spin = age * (state.kind == Glyph.ACID ? 0.0F : 1.2F);
		if (state.kind == Glyph.HARM && !state.preview) {
			float left = state.lifetime - age;
			if (left > 0.0F) {
				// The fuse: faster and brighter the nearer it is to striking.
				float urgency = 1.0F - Mth.clamp(left / state.lifetime, 0.0F, 1.0F);
				float pulse = 0.5F + 0.5F * Mth.sin(age * (0.25F + urgency * 1.4F));
				alpha *= 0.55F + 0.45F * pulse * (0.4F + 0.6F * urgency);
				spin = age * (1.2F + urgency * 6.0F);
			} else {
				// The strike: a flare that swells and fades.
				float flash = Mth.clamp(-left / HARM_FLASH, 0.0F, 1.0F);
				alpha = 1.0F - flash;
				scale = 1.0F + flash * 0.25F;
			}
		} else if (!state.preview) {
			alpha *= Math.min(1.0F, (state.lifetime - age) / FADE);
			alpha *= 0.8F + 0.2F * Mth.sin(age * 0.2F);
		}
		if (alpha <= 0.01F) {
			return;
		}
		float r = state.radius * scale;
		int color = ARGB.color(Mth.floor(255.0F * Mth.clamp(alpha, 0.0F, 1.0F)), 255, 255, 255);
		poseStack.pushPose();
		if (state.preview) {
			poseStack.rotate(camera.orientation);
			poseStack.rotateDegrees(Axis.ZP, spin);
			collector.submitCustomGeometry(poseStack, TYPES[state.kind.ordinal()], (pose, buffer) -> {
				vertex(buffer, pose, -r, -r, 0.0F, color, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, r, -r, 0.0F, color, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, r, r, 0.0F, color, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, -r, r, 0.0F, color, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
			});
		} else {
			poseStack.translate(0.0F, 0.03F, 0.0F);
			poseStack.rotateDegrees(Axis.YP, spin);
			collector.submitCustomGeometry(poseStack, TYPES[state.kind.ordinal()], (pose, buffer) -> {
				vertex(buffer, pose, -r, 0.0F, -r, color, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F);
				vertex(buffer, pose, -r, 0.0F, r, color, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
				vertex(buffer, pose, r, 0.0F, r, color, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
				vertex(buffer, pose, r, 0.0F, -r, color, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
			});
		}
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color, float u, float v, float nx, float ny,
			float nz) {
		buffer.addVertex(pose, x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	public static class State extends EntityRenderState {
		public Glyph kind = Glyph.BLIND;
		public float radius = 1.0F;
		public int lifetime = 100;
		public boolean preview;
	}
}
