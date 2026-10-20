package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.Koschei;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiModule;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Koschei, two and a half blocks tall. His blade glows orange before the blow only a shield stops and red before the blows
 * a shield only halves; his eyes glow yellow while a hit would parry him and flicker purple and blue before he vanishes.
 * In his third phase they burn red while he is in a fury. The rest of the time they glow a cold pale blue. The crystal on his staff glows blue. His lightning is drawn by
 * {@link KoscheiBolts}.
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

	public KoscheiRenderer(EntityRendererProvider.Context context) {
		super(context, new KoscheiModel(context.bakeLayer(KoscheiModel.LAYER)), 0.5F * SCALE);
		addLayer(new Glow(this));
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
		state.eyes = parry ? KoscheiRenderState.Eyes.YELLOW
				: state.action == KoscheiAction.TELEPORT || state.action == KoscheiAction.TRAP ? KoscheiRenderState.Eyes.WARP
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
			default -> KoscheiRenderState.Blade.NONE;
		};
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
