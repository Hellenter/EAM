package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The heavy cavalryman's own poses, laid over the Fallen Lord's animations on every layer of his model (his body and his
 * plate), so they bend together: the seat in the saddle, the lance upright or couched, the dismount and his two axe
 * moves of his own. Each move looks unlike the others, so a player can tell what is coming: the couched lance with the
 * body bent over it, two quick cuts from the right and the left, and for the sweep a deep crouch with the axe drawn far
 * back behind him before the turn.
 */
final class CavalryPoses {
	private static final float KNEE = 6.0F;
	private static final float SHIN = 6.0F;
	private static final float ARM_FORWARD = -(float) Math.PI / 2.0F;

	private CavalryPoses() {
	}

	static void apply(FallenLordModel m, FallenLordRenderState s) {
		if (s.dying >= 0.0F) {
			return;
		}
		float t = s.actionTime;
		if (s.isPassenger) {
			if (s.knightAction == KnightAction.LANCE_CHARGE) {
				charge(m, t);
			} else if (s.knightAction == KnightAction.NONE) {
				lanceUpright(m, 1.0F - s.guard);
			}
			seat(m, s);
			return;
		}
		switch (s.knightAction) {
			case AXE_DOUBLE -> doubleCut(m, t);
			case AXE_SWEEP -> sweep(m, t);
			case DISMOUNT -> dismount(m, t);
			default -> {
				return;
			}
		}
		sink(m);
	}

	/** 0 before {@code start}, 1 after {@code end}, smooth in between. */
	private static float seg(float t, float start, float end) {
		float p = Mth.clamp((t - start) / (end - start), 0.0F, 1.0F);
		return p * p * (3.0F - 2.0F * p);
	}

	private static void bendLeg(ModelPart leg, ModelPart shin, float amount, float hip, float knee) {
		leg.xRot = Mth.lerp(amount, leg.xRot, hip);
		shin.xRot = Mth.lerp(amount, shin.xRot, knee);
	}

	/** Bent knees would lift his feet off the ground: he sinks by as much, as the Fallen Lord's model does for its own moves. */
	private static void sink(FallenLordModel m) {
		float right = KNEE * (1.0F - Mth.cos(m.rightLeg.xRot)) + SHIN * (1.0F - Mth.cos(m.rightLeg.xRot + m.rightShin.xRot));
		float left = KNEE * (1.0F - Mth.cos(m.leftLeg.xRot)) + SHIN * (1.0F - Mth.cos(m.leftLeg.xRot + m.leftShin.xRot));
		m.root().y = m.root().getInitialPose().y() + Math.min(right, left) * FallenLordModel.MODEL_SCALE;
	}

	/**
	 * Astride the horse: thighs forward and out along its flanks, shins hanging down by its sides, sitting upright and
	 * rocking a little with the gallop. Not sunk: the saddle holds him.
	 */
	private static void seat(FallenLordModel m, FallenLordRenderState s) {
		m.rightLeg.xRot = -1.05F;
		m.rightLeg.yRot = 0.3F;
		m.rightLeg.zRot = 0.3F;
		m.rightShin.xRot = 1.15F;
		m.leftLeg.xRot = -1.05F;
		m.leftLeg.yRot = -0.3F;
		m.leftLeg.zRot = -0.3F;
		m.leftShin.xRot = 1.15F;
		m.root().y = m.root().getInitialPose().y();
		float rock = Mth.sin(s.ageInTicks * 0.9F) * 0.06F * s.mountSpeed;
		m.body.xRot += rock;
		m.head.xRot -= rock;
	}

	/** The lance held upright at his right side, its butt on his thigh, while he rides between charges. */
	private static void lanceUpright(FallenLordModel m, float amount) {
		if (amount <= 0.0F) {
			return;
		}
		m.rightArm.xRot = Mth.lerp(amount, m.rightArm.xRot, -0.3F);
		m.rightArm.yRot = Mth.lerp(amount, m.rightArm.yRot, 0.0F);
		m.rightArm.zRot = Mth.lerp(amount, m.rightArm.zRot, 0.05F);
		m.rightForearm.xRot = Mth.lerp(amount, m.rightForearm.xRot, -1.25F);
	}

	/**
	 * The charge: the lance comes down level under his arm, pointing ahead and a little down at a man on foot, the
	 * shield up on his left and his body bent forward over the horse's neck.
	 */
	private static void charge(FallenLordModel m, float t) {
		float lower = seg(t, 0.0F, 6.0F);
		m.raiseShieldGuard(lower);
		lanceUpright(m, 1.0F - lower);
		m.rightArm.xRot = Mth.lerp(lower, m.rightArm.xRot, 0.3F);
		m.rightArm.yRot = Mth.lerp(lower, m.rightArm.yRot, 0.08F);
		m.rightArm.zRot = Mth.lerp(lower, m.rightArm.zRot, 0.0F);
		m.rightArm.z += 1.5F * lower;
		m.rightForearm.xRot = Mth.lerp(lower, m.rightForearm.xRot, -0.12F);
		m.body.xRot += 0.3F * lower;
		m.head.xRot -= 0.25F * lower;
		m.head.z -= 1.0F * lower;
	}

	/**
	 * One diagonal axe cut from {@code start}: raised over three ticks, down in two, the blade landing on the fifth.
	 * {@code dir} flips it to the other side. It starts from the arm's pose given, so the second cut follows straight on
	 * from the first; {@code recover} eases back afterwards.
	 */
	private static void cut(FallenLordModel m, float t, float start, float dir, float fromX, float fromY, float fromElbow, float recoverEnd) {
		float baseX = m.rightArm.xRot;
		float baseY = m.rightArm.yRot;
		float baseElbow = m.rightForearm.xRot;
		float raise = seg(t, start, start + 3.0F);
		float strike = seg(t, start + 3.0F, start + 5.0F);
		float back = recoverEnd > 0.0F ? seg(t, start + 5.0F, recoverEnd) : 0.0F;
		float x = Mth.lerp(raise, fromX, -2.5F);
		x = Mth.lerp(strike, x, -0.3F);
		m.rightArm.xRot = Mth.lerp(back, x, baseX);
		float y = Mth.lerp(raise, fromY, -0.6F * dir);
		y = Mth.lerp(strike, y, 0.7F * dir);
		m.rightArm.yRot = Mth.lerp(back, y, baseY);
		float elbow = Mth.lerp(raise, fromElbow, -1.1F);
		elbow = Mth.lerp(strike, elbow, -0.1F);
		m.rightForearm.xRot = Mth.lerp(back, elbow, baseElbow);
		m.body.yRot = Mth.lerp(strike, -0.25F * dir * raise, 0.4F * dir) * (1.0F - back);
		float step = Math.max(raise, strike) * (1.0F - back);
		bendLeg(m.rightLeg, m.rightShin, step, -0.4F, 0.5F);
		bendLeg(m.leftLeg, m.leftShin, step, 0.25F, 0.35F);
	}

	/** Two quick cuts: from his right across to his left, then straight back the other way, landing on ticks 5 and 10. */
	private static void doubleCut(FallenLordModel m, float t) {
		int first = KnightAction.AXE_DOUBLE_HITS[0];
		if (t < first + 0.5F) {
			cut(m, t, first - 5.0F, 1.0F, m.rightArm.xRot, m.rightArm.yRot, m.rightForearm.xRot, 0.0F);
		} else {
			int second = KnightAction.AXE_DOUBLE_HITS[1];
			cut(m, t, second - 5.0F, -1.0F, -0.3F, 0.7F, -0.1F, KnightAction.AXE_DOUBLE.length());
		}
	}

	/**
	 * The sweep: a deep crouch, twisted to his right, the axe drawn low and far behind him and the shield out in front;
	 * he holds it a moment, then turns full circle with the axe arm straight out (the renderer turns him), and stands.
	 */
	private static void sweep(FallenLordModel m, float t) {
		float windup = seg(t, 0.0F, 8.0F) * (1.0F - seg(t, KnightAction.AXE_SWEEP_WINDUP, KnightAction.AXE_SWEEP_WINDUP + 1.5F));
		float spin = seg(t, KnightAction.AXE_SWEEP_WINDUP, KnightAction.AXE_SWEEP_WINDUP + 1.5F) * (1.0F - seg(t, 18.0F, 26.0F));
		float crouch = seg(t, 0.0F, 8.0F) * (1.0F - seg(t, 18.0F, 26.0F));
		float tremble = Mth.sin(t * 2.5F) * 0.03F * seg(t, 7.0F, 9.0F) * windup;
		// Wound up.
		m.rightArm.xRot = Mth.lerp(windup, m.rightArm.xRot, 0.5F) + tremble;
		m.rightArm.yRot = Mth.lerp(windup, m.rightArm.yRot, 1.5F);
		m.rightArm.zRot = Mth.lerp(windup, m.rightArm.zRot, 0.55F);
		m.rightForearm.xRot = Mth.lerp(windup, m.rightForearm.xRot, -0.25F);
		m.leftArm.xRot = Mth.lerp(windup, m.leftArm.xRot, -1.1F);
		m.leftArm.yRot = Mth.lerp(windup, m.leftArm.yRot, 0.3F);
		m.leftForearm.xRot = Mth.lerp(windup, m.leftForearm.xRot, -0.5F);
		m.body.yRot += -0.8F * windup;
		// The turn: the axe arm straight out to his right, the shield arm out to his left.
		m.rightArm.xRot = Mth.lerp(spin, m.rightArm.xRot, ARM_FORWARD);
		m.rightArm.yRot = Mth.lerp(spin, m.rightArm.yRot, -1.35F);
		m.rightArm.zRot = Mth.lerp(spin, m.rightArm.zRot, 0.0F);
		m.rightForearm.xRot = Mth.lerp(spin, m.rightForearm.xRot, -0.1F);
		m.leftArm.zRot -= 1.0F * spin;
		m.leftForearm.xRot = Mth.lerp(spin, m.leftForearm.xRot, -0.3F);
		m.body.xRot += 0.2F * crouch;
		m.head.xRot -= 0.2F * crouch;
		bendLeg(m.rightLeg, m.rightShin, crouch, -0.55F, 1.1F);
		bendLeg(m.leftLeg, m.leftShin, crouch, 0.45F, 0.9F);
	}

	/** Thrown from the saddle: he lands crouched, bent over, arms out, and straightens up as he draws the axe. */
	private static void dismount(FallenLordModel m, float t) {
		float down = 1.0F - seg(t, 8.0F, 20.0F);
		float shake = Mth.sin(t * 1.3F) * 0.15F * down;
		m.body.xRot += 0.5F * down;
		m.head.xRot += 0.3F * down;
		m.head.yRot += shake;
		m.rightArm.xRot = Mth.lerp(down, m.rightArm.xRot, -0.6F);
		m.rightArm.zRot = Mth.lerp(down, m.rightArm.zRot, 0.6F);
		m.leftArm.xRot = Mth.lerp(down, m.leftArm.xRot, -0.6F);
		m.leftArm.zRot = Mth.lerp(down, m.leftArm.zRot, -0.6F);
		bendLeg(m.rightLeg, m.rightShin, down, -1.0F, 1.9F);
		bendLeg(m.leftLeg, m.leftShin, down, -0.3F, 1.6F);
	}
}
