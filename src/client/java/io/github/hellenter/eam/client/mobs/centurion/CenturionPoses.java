package io.github.hellenter.eam.client.mobs.centurion;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;

import net.minecraft.util.Mth;

/**
 * The centurion's own pose, the same on his body and on his plate: blowing the horn, the left hand brings it to his
 * mouth with the elbow out to the side, and he tips his head back a little.
 */
final class CenturionPoses {
	private CenturionPoses() {
	}

	/** How far the horn is raised to his mouth, 0 to 1. */
	static float horn(FallenLordRenderState state) {
		if (state.knightAction != KnightAction.HORN) {
			return 0.0F;
		}
		float t = state.actionTime;
		return seg(t, 0.0F, 6.0F) * (1.0F - seg(t, KnightAction.HORN.length() - 8.0F, KnightAction.HORN.length()));
	}

	static void apply(FallenLordModel model, FallenLordRenderState state) {
		float horn = horn(state);
		if (horn <= 0.0F) {
			return;
		}
		model.leftArm.xRot = Mth.lerp(horn, model.leftArm.xRot, -1.45F);
		model.leftArm.yRot = Mth.lerp(horn, model.leftArm.yRot, -0.05F);
		model.leftArm.zRot = Mth.lerp(horn, model.leftArm.zRot, -1.0F);
		model.leftForearm.xRot = Mth.lerp(horn, model.leftForearm.xRot, -1.35F);
		model.head.xRot = Mth.lerp(horn, model.head.xRot, -0.3F);
	}

	private static float seg(float t, float start, float end) {
		float p = Mth.clamp((t - start) / (end - start), 0.0F, 1.0F);
		return p * p * (3.0F - 2.0F * p);
	}
}
