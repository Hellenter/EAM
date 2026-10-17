package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYaga;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaModule;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaSettings;
import io.github.hellenter.eam.mobs.entity.yaga.YagaAction;
import io.github.hellenter.eam.mobs.entity.yaga.YagaDeath;
import io.github.hellenter.eam.mobs.entity.yaga.YagaGlow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;

/**
 * Baba Yaga. Her eyes always glow a sickly green; the skull on her staff lights up in the colour of whatever she casts
 * (green as she raises her creatures); and while a hit would parry her she glows yellow from head to foot. Her fire beam
 * is drawn like a beacon's, from the staff to where it hits.
 */
public class BabaYagaRenderer extends MobRenderer<BabaYaga, BabaYagaRenderState, BabaYagaModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/baba_yaga/baba_yaga.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/baba_yaga/baba_yaga_eyes.png"));
	private static final RenderType STAFF_GLOW = RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/baba_yaga_glow.png"));
	private static final RenderType AURA = RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/baba_yaga_aura.png"));
	private static final RenderType BEAM_CORE = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, false);
	private static final RenderType BEAM_GLOW = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);
	private static final int BEAM_COLOR = 0xFFFF7A1E;
	private static final float BEAM_CORE_RADIUS = 0.1F;
	private static final float BEAM_GLOW_RADIUS = 0.15F;

	public BabaYagaRenderer(EntityRendererProvider.Context context) {
		super(context, new BabaYagaModel(context.bakeLayer(BabaYagaModel.LAYER)), 0.6F);
		addLayer(new Glow(this));
	}

	@Override
	public boolean shouldRender(BabaYaga entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
		// Waiting in her hut she is not there to see, shadow included.
		return !entity.waiting() && super.shouldRender(entity, culler, camX, camY, camZ, partialTicks);
	}

	@Override
	public Identifier getTextureLocation(BabaYagaRenderState state) {
		return TEXTURE;
	}

	@Override
	public BabaYagaRenderState createRenderState() {
		return new BabaYagaRenderState();
	}

	@Override
	public void extractRenderState(BabaYaga entity, BabaYagaRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.inMortar = entity.inMortar();
		state.action = entity.action();
		state.actionTime = entity.actionTime(partialTicks);
		state.glow = entity.glow();
		BabaYagaSettings settings = BabaYagaModule.settings();
		state.strikeTick = settings.swoop().strikeTick();
		state.lunge = settings.claws().lunge();
		state.clawGap = settings.claws().gap();
		state.clawHits = settings.claws().hits();
		state.beamWindup = settings.beam().windup();
		Vec3 beamEnd = entity.beamEnd();
		boolean burning = beamEnd != null && state.action == YagaAction.BEAM && state.actionTime >= state.beamWindup;
		state.beamFrom = burning ? BabaYaga.staffTipOffset(state.bodyRot, entity.getBbHeight(), state.inMortar) : null;
		state.beamTo = burning ? beamEnd : null;
		if (state.deathTime > 0.0F) {
			// Her own death, burning and writhing where she stands, not the vanilla roll onto the side in red.
			state.dying = state.deathTime;
			state.deathTime = 0.0F;
			state.hasRedOverlay = false;
			state.action = YagaAction.NONE;
			state.glow = YagaGlow.NONE;
			state.walkAnimationSpeed = 0.0F;
			state.displayFireAnimation = YagaDeath.burning(state.dying);
			state.beamFrom = null;
			state.beamTo = null;
		} else {
			state.dying = -1.0F;
		}
	}

	/** Charring black as she burns, then ash grey. */
	@Override
	protected int getModelTint(BabaYagaRenderState state) {
		return state.dying < 0.0F ? super.getModelTint(state) : ARGB.opaque(YagaDeath.tint(state.dying));
	}

	@Override
	protected AABB getBoundingBoxForCulling(BabaYaga entity, float partialTicks) {
		AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
		Vec3 beamEnd = entity.beamEnd();
		return beamEnd == null ? box : box.minmax(new AABB(entity.position(), entity.position().add(beamEnd)));
	}

	@Override
	public void submit(BabaYagaRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.beamFrom != null && state.beamTo != null) {
			submitBeam(poseStack, collector, state.beamFrom, state.beamTo, state.ageInTicks);
		}
	}

	/** A beacon's beam laid along {@code from} to {@code to}: a turning solid core inside a faint glow. */
	private static void submitBeam(PoseStack poseStack, SubmitNodeCollector collector, Vec3 from, Vec3 to, float time) {
		Vec3 along = to.subtract(from);
		float length = (float) along.length();
		if (length < 0.1F) {
			return;
		}
		Vec3 dir = along.scale(1.0 / length);
		float scroll = -time;
		float vOffset = Mth.frac(scroll * 0.2F - Mth.floor(scroll * 0.1F));
		poseStack.pushPose();
		poseStack.translate(from);
		poseStack.rotate(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) dir.x, (float) dir.y, (float) dir.z));
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, time * 6.0F - 45.0F);
		float coreV0 = -1.0F + vOffset;
		float coreV1 = length * (0.5F / BEAM_CORE_RADIUS) + coreV0;
		float r = BEAM_CORE_RADIUS;
		collector.submitCustomGeometry(poseStack, BEAM_CORE,
				(pose, buffer) -> beamSides(pose, buffer, BEAM_COLOR, length, 0.0F, r, r, 0.0F, -r, 0.0F, 0.0F, -r, coreV1, coreV0));
		poseStack.popPose();
		float g = BEAM_GLOW_RADIUS;
		float glowV0 = -1.0F + vOffset;
		float glowV1 = length + glowV0;
		collector.submitCustomGeometry(poseStack, BEAM_GLOW,
				(pose, buffer) -> beamSides(pose, buffer, ARGB.color(48, BEAM_COLOR), length, -g, -g, g, -g, -g, g, g, g, glowV1, glowV0));
		poseStack.popPose();
	}

	/** The four sides of a beam square, as the beacon draws them, {@code length} long up its own Y axis. */
	private static void beamSides(PoseStack.Pose pose, VertexConsumer buffer, int color, float length, float wnx, float wnz, float enx,
			float enz, float wsx, float wsz, float esx, float esz, float v1, float v2) {
		beamQuad(pose, buffer, color, length, wnx, wnz, enx, enz, v1, v2);
		beamQuad(pose, buffer, color, length, esx, esz, wsx, wsz, v1, v2);
		beamQuad(pose, buffer, color, length, enx, enz, esx, esz, v1, v2);
		beamQuad(pose, buffer, color, length, wsx, wsz, wnx, wnz, v1, v2);
	}

	private static void beamQuad(PoseStack.Pose pose, VertexConsumer buffer, int color, float length, float ax, float az, float bx, float bz,
			float v1, float v2) {
		beamVertex(pose, buffer, color, length, ax, az, 1.0F, v1);
		beamVertex(pose, buffer, color, 0.0F, ax, az, 1.0F, v2);
		beamVertex(pose, buffer, color, 0.0F, bx, bz, 0.0F, v2);
		beamVertex(pose, buffer, color, length, bx, bz, 0.0F, v1);
	}

	private static void beamVertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float y, float x, float z, float u, float v) {
		buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	/** The eyes, the staff's glow and the parry glow, all lit whatever the light around. */
	private static final class Glow extends RenderLayer<BabaYagaRenderState, BabaYagaModel> {
		Glow(RenderLayerParent<BabaYagaRenderState, BabaYagaModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, BabaYagaRenderState state, float yRot, float xRot) {
			if (state.dying >= YagaDeath.EYES_OUT) {
				// Her eyes have burnt out.
				return;
			}
			BabaYagaModel model = getParentModel();
			collector.order(1).submitModel(model, state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			YagaGlow glow = state.glow;
			if (glow == YagaGlow.NONE) {
				return;
			}
			float pulse = 0.5F + 0.5F * Mth.sin(state.ageInTicks * (glow == YagaGlow.PARRY ? 0.9F : 0.5F));
			if (glow == YagaGlow.PARRY) {
				int tint = ARGB.color(Mth.floor(255.0F * (0.45F + 0.3F * pulse)), glow.color);
				collector.order(2).submitModel(model, state, poseStack, AURA, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint,
						null, state.outlineColor);
			} else {
				int tint = ARGB.color(Mth.floor(255.0F * (0.75F + 0.25F * pulse)), glow.color);
				collector.order(2).submitModel(model, state, poseStack, STAFF_GLOW, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
						tint, null, state.outlineColor);
			}
		}
	}
}
