package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.HagThread;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A purple thread: a glowing, writhing strand rising out of the ground to the legs of whoever it holds, or a short
 * tendril waving in the air once it has nothing to hold.
 */
public class HagThreadRenderer extends EntityRenderer<HagThread, HagThreadRenderer.State> {
	private static final RenderType TYPE = RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/hag_thread.png"));
	private static final int SEGMENTS = 10;
	private static final float WIDTH = 0.09F;
	private static final int COLOR = 0xFFFFFFFF;

	public HagThreadRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HagThread entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.dead = entity.isDeadOrDying();
		LivingEntity held = entity.held();
		if (held != null) {
			Vec3 to = held.getPosition(partialTicks).add(0.0, held.getBbHeight() * 0.3, 0.0);
			state.to = to.subtract(entity.getPosition(partialTicks));
		} else {
			state.to = new Vec3(0.0, 1.0, 0.0);
		}
	}

	@Override
	protected AABB getBoundingBoxForCulling(HagThread entity, float partialTicks) {
		return entity.getBoundingBox().inflate(2.0);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.dead) {
			Vec3 to = state.to;
			float age = state.ageInTicks;
			float length = (float) to.length();
			// Two sideways directions across the strand, for the wiggle and the crossed ribbons.
			Vec3 dir = length > 1.0E-4 ? to.scale(1.0 / length) : new Vec3(0.0, 1.0, 0.0);
			Vec3 side = Math.abs(dir.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : dir.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
			Vec3 other = dir.cross(side).normalize();
			Vec3[] points = new Vec3[SEGMENTS + 1];
			for (int i = 0; i <= SEGMENTS; i++) {
				float s = (float) i / SEGMENTS;
				// Pinned at both ends, writhing in between.
				float sway = Mth.sin(s * Mth.PI) * 0.18F;
				double a = Mth.sin(age * 0.45F + s * 7.0F) * sway;
				double b = Mth.cos(age * 0.37F + s * 5.0F) * sway;
				points[i] = to.scale(s).add(side.scale(a)).add(other.scale(b));
			}
			float vScale = length * 2.0F;
			collector.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> {
				for (Vec3 across : new Vec3[] {side, other}) {
					Vec3 w = across.scale(WIDTH);
					for (int i = 0; i < SEGMENTS; i++) {
						Vec3 p0 = points[i];
						Vec3 p1 = points[i + 1];
						float v0 = vScale * i / SEGMENTS - age * 0.05F;
						float v1 = vScale * (i + 1) / SEGMENTS - age * 0.05F;
						// Thinner toward the end it holds.
						float w0 = 1.0F - 0.4F * i / SEGMENTS;
						float w1 = 1.0F - 0.4F * (i + 1) / SEGMENTS;
						HagGlyphRenderer.vertex(buffer, pose, (float) (p0.x - w.x * w0), (float) (p0.y - w.y * w0), (float) (p0.z - w.z * w0), COLOR,
								0.0F, v0, 0.0F, 1.0F, 0.0F);
						HagGlyphRenderer.vertex(buffer, pose, (float) (p0.x + w.x * w0), (float) (p0.y + w.y * w0), (float) (p0.z + w.z * w0), COLOR,
								1.0F, v0, 0.0F, 1.0F, 0.0F);
						HagGlyphRenderer.vertex(buffer, pose, (float) (p1.x + w.x * w1), (float) (p1.y + w.y * w1), (float) (p1.z + w.z * w1), COLOR,
								1.0F, v1, 0.0F, 1.0F, 0.0F);
						HagGlyphRenderer.vertex(buffer, pose, (float) (p1.x - w.x * w1), (float) (p1.y - w.y * w1), (float) (p1.z - w.z * w1), COLOR,
								0.0F, v1, 0.0F, 1.0F, 0.0F);
					}
				}
			});
		}
		super.submit(state, poseStack, collector, camera);
	}

	public static class State extends EntityRenderState {
		public Vec3 to = Vec3.ZERO;
		public boolean dead;
	}
}
