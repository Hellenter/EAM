package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiNeedle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Koschei's needle in the air: the needle itself, glinting and spinning faster as it nears the burst, in a pillar of
 * white light running up into the sky and down into the ground, wound round with two ribbons of blue and violet light
 * that spiral down it. After the burst the needle is gone and the light reaches down as far as the blasts have gone.
 */
public class KoscheiNeedleRenderer extends EntityRenderer<KoscheiNeedle, KoscheiNeedleRenderer.State> {
	private static final Identifier BEAM = Eam.id("textures/entity/koschei/needle_beam.png");
	/** The light reaches this far up. */
	private static final float BEAM_UP = 320.0F;
	/** And this far down while the needle hangs, before the blasts show how deep it goes. */
	private static final float BEAM_DOWN = 48.0F;
	private static final int CORE = ARGB.color(255, 235, 245, 255);
	private static final int GLOW = ARGB.color(60, 170, 210, 255);
	private static final int[] RIBBONS = {ARGB.color(255, 110, 180, 255), ARGB.color(255, 175, 120, 255)};
	private static final float CORE_RADIUS = 0.13F;
	private static final float GLOW_RADIUS = 0.32F;
	/** The spiral: how far out from the light it winds, how wide its ribbons are, how tightly it turns and how fast it runs down. */
	private static final float SPIRAL_RADIUS = 0.55F;
	private static final float RIBBON_HALF_WIDTH = 0.14F;
	private static final float TURNS_PER_BLOCK = 0.16F;
	private static final float DOWN_SPEED = 0.25F;
	private static final float SPIRAL_UP = 10.0F;
	private static final float SPIRAL_STEP = 0.2F;

	private final ItemModelResolver itemModelResolver;

	public KoscheiNeedleRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	protected int getBlockLightLevel(KoscheiNeedle entity, BlockPos pos) {
		return 15;
	}

	/** The pillar of light is far taller than the needle: draw it whenever it is in range at all. */
	@Override
	public boolean shouldRender(KoscheiNeedle entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
		return true;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KoscheiNeedle needle, State state, float partialTicks) {
		super.extractRenderState(needle, state, partialTicks);
		itemModelResolver.updateForNonLiving(state.item, needle.getItem(), ItemDisplayContext.GROUND, needle);
		float age = needle.age() + partialTicks;
		state.age = age;
		// Faster and faster: a quarter turn a tick at first, a full turn and more by the end.
		state.spin = age * age * 0.9F;
		state.light = age >= KoscheiNeedle.FLIGHT_TICKS;
		state.burst = needle.burst();
		state.down = state.burst ? Math.max(1.0F, needle.depth()) : BEAM_DOWN;
		// The light opens out over its first few ticks.
		state.open = Mth.clamp((age - KoscheiNeedle.FLIGHT_TICKS) / 6.0F, 0.0F, 1.0F);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.burst) {
			poseStack.pushPose();
			poseStack.rotate(Axis.YP.rotationDegrees(state.spin));
			poseStack.scale(1.4F, 1.4F, 1.4F);
			poseStack.translate(0.0F, -0.15F, 0.0F);
			state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			poseStack.popPose();
		}
		if (state.light) {
			submitLight(state, poseStack, collector);
		}
		super.submit(state, poseStack, collector, camera);
	}

	private static void submitLight(State state, PoseStack poseStack, SubmitNodeCollector collector) {
		float time = state.age;
		float open = state.open;
		float bottom = -state.down;
		float scroll = time * 0.08F;
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(time * 3.0F));
		collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM, false),
				(pose, buffer) -> prism(pose, buffer, CORE, CORE_RADIUS * open, bottom, BEAM_UP, scroll));
		collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM, true),
				(pose, buffer) -> prism(pose, buffer, GLOW, GLOW_RADIUS * open, bottom, BEAM_UP, scroll * 0.5F));
		poseStack.popPose();
		collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM, true), (pose, buffer) -> {
			for (int i = 0; i < RIBBONS.length; i++) {
				spiral(pose, buffer, RIBBONS[i], SPIRAL_RADIUS * open, bottom, SPIRAL_UP, time, i * Mth.PI);
			}
		});
	}

	/** A six-sided column of light from {@code bottom} to {@code top}, seen from inside and out, its texture running up it. */
	private static void prism(PoseStack.Pose pose, VertexConsumer buffer, int color, float radius, float bottom, float top, float scroll) {
		for (int k = 0; k < 6; k++) {
			float a0 = k * Mth.TWO_PI / 6.0F;
			float a1 = (k + 1) * Mth.TWO_PI / 6.0F;
			float x0 = Mth.cos(a0) * radius;
			float z0 = Mth.sin(a0) * radius;
			float x1 = Mth.cos(a1) * radius;
			float z1 = Mth.sin(a1) * radius;
			float vTop = top * 0.5F - scroll;
			float vBottom = bottom * 0.5F - scroll;
			quad(pose, buffer, color, x0, top, z0, 0.0F, vTop, x0, bottom, z0, 0.0F, vBottom, x1, bottom, z1, 1.0F, vBottom, x1, top, z1, 1.0F, vTop);
		}
	}

	/**
	 * One ribbon of the spiral wound round the light from {@code top} down to {@code bottom}. Its turn shifts with
	 * {@code time} so the coils run down the light; it fades in at the top and out at the bottom.
	 */
	private static void spiral(PoseStack.Pose pose, VertexConsumer buffer, int color, float radius, float bottom, float top, float time, float phase) {
		float length = top - bottom;
		for (float y = top; y > bottom; y -= SPIRAL_STEP) {
			float y1 = Math.max(bottom, y - SPIRAL_STEP);
			float t0 = angle(y, time, phase);
			float t1 = angle(y1, time, phase);
			float x0 = Mth.cos(t0) * radius;
			float z0 = Mth.sin(t0) * radius;
			float x1 = Mth.cos(t1) * radius;
			float z1 = Mth.sin(t1) * radius;
			float fade = Mth.clamp(Math.min((top - y) / 2.0F, (y1 - bottom) / Math.min(8.0F, length * 0.3F)), 0.0F, 1.0F);
			int c = ARGB.color((int) (ARGB.alpha(color) * fade * 0.85F), color);
			float v0 = y * 0.6F;
			float v1 = y1 * 0.6F;
			quad(pose, buffer, c, x0, y + RIBBON_HALF_WIDTH, z0, 0.0F, v0, x0, y - RIBBON_HALF_WIDTH, z0, 1.0F, v0, x1, y1 - RIBBON_HALF_WIDTH, z1, 1.0F, v1,
					x1, y1 + RIBBON_HALF_WIDTH, z1, 0.0F, v1);
		}
	}

	/** Where round the light the spiral passes at height {@code y}: the angle grows with time, so its coils run down. */
	private static float angle(float y, float time, float phase) {
		return (y * TURNS_PER_BLOCK + time * DOWN_SPEED * TURNS_PER_BLOCK) * Mth.TWO_PI + phase;
	}

	/** A quad seen from both sides. */
	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color, float ax, float ay, float az, float au, float av, float bx, float by,
			float bz, float bu, float bv, float cx, float cy, float cz, float cu, float cv, float dx, float dy, float dz, float du, float dv) {
		vertex(pose, buffer, color, ax, ay, az, au, av);
		vertex(pose, buffer, color, bx, by, bz, bu, bv);
		vertex(pose, buffer, color, cx, cy, cz, cu, cv);
		vertex(pose, buffer, color, dx, dy, dz, du, dv);
		vertex(pose, buffer, color, dx, dy, dz, du, dv);
		vertex(pose, buffer, color, cx, cy, cz, cu, cv);
		vertex(pose, buffer, color, bx, by, bz, bu, bv);
		vertex(pose, buffer, color, ax, ay, az, au, av);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float x, float y, float z, float u, float v) {
		buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	public static final class State extends EntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		float age;
		float spin;
		boolean light;
		boolean burst;
		float down;
		float open;
	}
}
