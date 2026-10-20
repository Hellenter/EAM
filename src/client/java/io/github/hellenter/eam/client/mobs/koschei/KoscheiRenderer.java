package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.Koschei;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiModule;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Koschei, two and a half blocks tall. His blade glows orange before the blow only a shield stops and red before the blows
 * a shield only halves; his eyes glow yellow while a hit would parry him and flicker purple and blue before he vanishes.
 * In his third phase they burn red while he is in a fury; his darts leave dark red after-images behind him, his daggers fly
 * on chains, and killed he shrinks away to nothing. The rest of the time they glow a cold pale blue. The crystal on his
 * staff glows blue. His lightning is drawn by {@link KoscheiBolts}.
 */
public class KoscheiRenderer extends HumanoidMobRenderer<Koschei, KoscheiRenderState, KoscheiModel> {
	/** The model is 2 blocks tall. */
	private static final float SCALE = 2.5F / 2.0F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/koschei/koschei.png");
	private static final RenderType GLOWING_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_eyes_glow.png"));
	private static final RenderType YELLOW_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_eyes_yellow.png"));
	private static final RenderType PURPLE_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_eyes_purple.png"));
	private static final RenderType BLUE_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_eyes_blue.png"));
	private static final RenderType RED_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_eyes_red.png"));
	private static final RenderType ORANGE_BLADE = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_blade_orange.png"));
	private static final RenderType RED_BLADE = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_blade_red.png"));
	private static final RenderType STAFF_GLOW = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_staff_glow.png"));
	private static final RenderType GHOST = RenderTypes.entityTranslucent(TEXTURE);
	private static final RenderType CHAIN = RenderTypes.entityCutout(Eam.id("textures/entity/koschei/koschei_chain.png"));
	/** His after-images: a dark red shade, fainter the older. */
	private static final int GHOST_COLOR = ARGB.color(0, 150, 25, 45);
	private static final float GHOST_ALPHA = 0.5F;
	/** He leaves after-images while he moves faster than this, blocks a tick. */
	private static final double GHOST_SPEED = 0.4;
	private static final float CHAIN_HALF_WIDTH = 0.09F;
	/** The two chains run from his fists this far apart and meet the daggers this far apart. */
	private static final double CHAIN_HANDS_APART = 0.35;
	private static final double CHAIN_TIPS_APART = 0.18;
	/** His model is this many units to his height, above his feet: he shrinks toward his middle as he dies. */
	private static final float MIDDLE = 0.9F;

	/** A dagger of his flying on its chain, drawn on its own from a copy of his model's. */
	private final ModelPart flyingDagger;

	public KoscheiRenderer(EntityRendererProvider.Context context) {
		super(context, new KoscheiModel(context.bakeLayer(KoscheiModel.LAYER)), 0.5F * SCALE);
		addLayer(new Glow(this));
		flyingDagger = KoscheiModel.flyingDagger(context.bakeLayer(KoscheiModel.LAYER));
	}

	@Override
	public Identifier getTextureLocation(KoscheiRenderState state) {
		return TEXTURE;
	}

	@Override
	public KoscheiRenderState createRenderState() {
		return new KoscheiRenderState();
	}

	@Override
	public void extractRenderState(Koschei koschei, KoscheiRenderState state, float partialTicks) {
		super.extractRenderState(koschei, state, partialTicks);
		state.action = koschei.action();
		state.actionTime = koschei.actionTime(partialTicks);
		float t = state.actionTime;
		int window = parryWindow(state.action);
		int parryStart = state.action.parryStart(window);
		boolean parry = koschei.parryOpen() || parryStart >= 0 && t >= parryStart - 1 && t < state.action.parryEnd() && !koschei.parryLost();
		boolean leapBlock = state.action == KoscheiAction.DAGGER_LEAP
				&& t >= KoscheiAction.LEAP_APEX - KoscheiModule.settings().phaseThree().leap().blockEarly() - 1
				&& t < KoscheiAction.LEAP_APEX + KoscheiModule.settings().phaseThree().leap().blockLate() + 1;
		boolean warp = state.action == KoscheiAction.TELEPORT || state.action == KoscheiAction.TRAP
				|| state.action == KoscheiAction.DAGGER_FEINT && (t < KoscheiAction.FEINT_WARP || t >= KoscheiAction.FEINT_APEX - 2
				&& t < KoscheiAction.FEINT_APEX);
		state.eyes = parry || leapBlock ? KoscheiRenderState.Eyes.YELLOW
				: warp ? KoscheiRenderState.Eyes.WARP
				: koschei.fury() > 0 ? KoscheiRenderState.Eyes.RED
				: KoscheiRenderState.Eyes.GLOW;
		state.phase = koschei.phase();
		boolean transforming = state.action == KoscheiAction.TRANSFORM;
		state.sword = state.phase < 2 || transforming && t < KoscheiAction.TRANSFORM_CRUMBLE;
		boolean shattering = state.action == KoscheiAction.SHATTER;
		state.staff = state.phase == 2 && (!transforming || t >= KoscheiAction.TRANSFORM_STAFF)
				|| state.phase >= 3 && shattering && t < KoscheiAction.SHATTER_STAFF;
		state.daggers = state.phase >= 3 && (!shattering || t >= KoscheiAction.SHATTER_BURST);
		state.fury = koschei.fury();
		state.guardHeight = koschei.guardHeight();
		state.guardSide = koschei.guardSide();
		state.blade = switch (state.action) {
			case ORANGE -> t < KoscheiAction.ORANGE_HIT + 1 ? KoscheiRenderState.Blade.ORANGE : KoscheiRenderState.Blade.NONE;
			case RED -> t < KoscheiAction.RED_HIT + 1 ? KoscheiRenderState.Blade.RED : KoscheiRenderState.Blade.NONE;
			case OVERHEAD -> t < KoscheiAction.OVERHEAD_HIT + 1 ? KoscheiRenderState.Blade.RED : KoscheiRenderState.Blade.NONE;
			// The blade flickers orange as he runs in.
			case DAGGER_ORANGE -> t < KoscheiAction.DAGGER_ORANGE_HIT + 1 && (t > KoscheiAction.DAGGER_ORANGE_HIT - 3 || (int) (t * 0.7F) % 2 == 0)
					? KoscheiRenderState.Blade.ORANGE : KoscheiRenderState.Blade.NONE;
			case DAGGER_LEAP -> t < KoscheiAction.LEAP_LAND + 1 ? KoscheiRenderState.Blade.RED : KoscheiRenderState.Blade.NONE;
			default -> KoscheiRenderState.Blade.NONE;
		};
		state.chainsOut = state.action == KoscheiAction.CHAINS && t >= KoscheiAction.CHAINS_THROW;
		if (state.chainsOut) {
			state.chainTip = koschei.chainTip().subtract(state.x, state.y, state.z);
		}
		state.ghosts.clear();
		if (state.phase >= 3) {
			List<Vec3> trail = koschei.trail();
			for (int i = 1; i < trail.size() && state.ghosts.size() < 4; i++) {
				if (trail.get(i - 1).distanceTo(trail.get(i)) > GHOST_SPEED) {
					state.ghosts.add(trail.get(i).subtract(state.x, state.y, state.z));
				} else if (!state.ghosts.isEmpty()) {
					break;
				}
			}
			// Shrinking away into his needle: no red flash.
			state.hasRedOverlay &= state.deathTime <= 0.0F;
		}
		state.turnRate = Mth.wrapDegrees(koschei.yBodyRot - koschei.yBodyRotO);
		state.fallSpeed = (float) (koschei.getY() - koschei.yo);
		double dx = koschei.getX() - koschei.xo;
		double dz = koschei.getZ() - koschei.zo;
		state.dash = Math.clamp(((float) Math.sqrt(dx * dx + dz * dz) - 0.25F) / 0.35F, 0.0F, 1.0F);
	}

	private static int parryWindow(KoscheiAction action) {
		return switch (action) {
			case LUNGE -> KoscheiModule.settings().phaseOne().lunge().parryWindow();
			case OVERHEAD -> KoscheiModule.settings().phaseOne().overhead().parryWindow();
			default -> 0;
		};
	}

	@Override
	protected void scale(KoscheiRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
		if (state.phase >= 3 && state.deathTime > 0.0F) {
			// Shrinking away toward his middle, faster at the end.
			float left = Math.max(0.0F, 1.0F - state.deathTime / Koschei.DEATH_TICKS);
			float size = Math.max(0.01F, left * (0.4F + 0.6F * left));
			poseStack.translate(0.0F, -MIDDLE, 0.0F);
			poseStack.scale(size, size, size);
			poseStack.translate(0.0F, MIDDLE, 0.0F);
		}
	}

	/** Killed in his third phase he does not topple over: he spins as he shrinks away. */
	@Override
	protected void setupRotations(KoscheiRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		if (state.phase >= 3 && state.deathTime > 0.0F) {
			float death = state.deathTime;
			state.deathTime = 0.0F;
			super.setupRotations(state, poseStack, bodyRot + death * death * 1.2F, entityScale);
			state.deathTime = death;
			return;
		}
		super.setupRotations(state, poseStack, bodyRot, entityScale);
	}

	@Override
	public void submit(KoscheiRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		for (int i = 0; i < state.ghosts.size(); i++) {
			submitGhost(state, poseStack, collector, state.ghosts.get(i), GHOST_ALPHA * (1.0F - i / (float) (state.ghosts.size() + 1)));
		}
		if (state.chainsOut) {
			submitChains(state, poseStack, collector);
		}
	}

	/** One after-image of him where he stood a moment ago, posed as he is now. */
	private void submitGhost(KoscheiRenderState state, PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float alpha) {
		poseStack.pushPose();
		poseStack.translate(at.x, at.y, at.z);
		poseStack.scale(state.scale, state.scale, state.scale);
		poseStack.rotate(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		scale(state, poseStack);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		int color = ARGB.color(Math.round(alpha * 255.0F), GHOST_COLOR);
		collector.submitModel(getModel(), state, poseStack, GHOST, state.lightCoords, OverlayTexture.NO_OVERLAY, color, null, 0);
		poseStack.popPose();
	}

	/** His two chains, from his fists out to the daggers at their ends. */
	private void submitChains(KoscheiRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, state.bodyRot);
		Vec3 right = Vec3.directionFromRotation(0.0F, state.bodyRot + 90.0F);
		Vec3 hands = forward.scale(0.6).add(0.0, state.boundingBoxHeight * 0.55, 0.0);
		Vec3 tip = state.chainTip;
		for (int side = -1; side <= 1; side += 2) {
			Vec3 from = hands.add(right.scale(side * CHAIN_HANDS_APART));
			Vec3 to = tip.add(right.scale(side * CHAIN_TIPS_APART));
			collector.submitCustomGeometry(poseStack, CHAIN, (pose, buffer) -> chain(pose, buffer, from, to, state.lightCoords));
			Vec3 along = to.subtract(from);
			if (along.lengthSqr() < 1.0E-4) {
				continue;
			}
			along = along.normalize();
			poseStack.pushPose();
			poseStack.translate(to.x, to.y, to.z);
			poseStack.rotate(Axis.YP.rotation((float) Math.atan2(-along.x, -along.z)));
			poseStack.rotate(Axis.XP.rotation((float) Math.asin(Mth.clamp(along.y, -1.0, 1.0))));
			poseStack.scale(SCALE, SCALE, SCALE);
			collector.submitModelPart(flyingDagger, poseStack, getModel().renderType(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
			poseStack.popPose();
		}
	}

	/** One chain from {@code from} to {@code to}: two crossed strips of links, seen from both sides. */
	private static void chain(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, int light) {
		Vec3 along = to.subtract(from);
		double length = along.length();
		if (length < 1.0E-3) {
			return;
		}
		Vec3 dir = along.scale(1.0 / length);
		Vec3 a = dir.cross(new Vec3(0.0, 1.0, 0.0));
		a = a.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : a.normalize();
		Vec3 b = dir.cross(a).normalize();
		float v = (float) (length / (CHAIN_HALF_WIDTH * 4.0));
		for (Vec3 across : new Vec3[] {a, b}) {
			Vec3 w = across.scale(CHAIN_HALF_WIDTH);
			strip(pose, buffer, from.add(w), from.subtract(w), to.subtract(w), to.add(w), v, light);
			strip(pose, buffer, to.add(w), to.subtract(w), from.subtract(w), from.add(w), v, light);
		}
	}

	private static void strip(PoseStack.Pose pose, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float v, int light) {
		vertex(pose, buffer, a, 0.0F, 0.0F, light);
		vertex(pose, buffer, b, 1.0F, 0.0F, light);
		vertex(pose, buffer, c, 1.0F, v, light);
		vertex(pose, buffer, d, 0.0F, v, light);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, float u, float v, int light) {
		buffer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	/** His sword is part of the model; no item is drawn in his hands. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(Koschei koschei, HumanoidArm arm) {
		return HumanoidModel.ArmPose.EMPTY;
	}

	/** The glowing eyes and blade, lit whatever the light around. */
	private static final class Glow extends RenderLayer<KoscheiRenderState, KoscheiModel> {
		Glow(RenderLayerParent<KoscheiRenderState, KoscheiModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, KoscheiRenderState state, float yRot, float xRot) {
			RenderType eyes = switch (state.eyes) {
				case YELLOW -> YELLOW_EYES;
				// Purple and blue in turn, quicker toward the end.
				case WARP -> ((int) (state.actionTime * (state.actionTime < 6.0F ? 0.5F : 1.0F))) % 2 == 0 ? PURPLE_EYES : BLUE_EYES;
				case GLOW -> GLOWING_EYES;
				case RED -> RED_EYES;
			};
			if (eyes != null) {
				collector.order(1).submitModel(getParentModel(), state, poseStack, eyes, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			}
			RenderType blade = switch (state.blade) {
				case ORANGE -> ORANGE_BLADE;
				case RED -> RED_BLADE;
				case NONE -> null;
			};
			if (blade != null) {
				collector.order(2).submitModel(getParentModel(), state, poseStack, blade, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			}
			if (state.staff) {
				collector.order(3).submitModel(getParentModel(), state, poseStack, STAFF_GLOW, lightCoords, OverlayTexture.NO_OVERLAY,
						state.outlineColor);
			}
		}
	}
}
