package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * The cloak worn under the gear model's helmet: three hanging rows that widen downward, then three torn strips at the
 * hem, each hinged on the one above so it bends like cloth. It hangs nearly straight at rest, trails and ripples as he
 * walks and runs, streams back in dashes and spins, and is pushed out by whatever swings behind him: each row and strip
 * is lifted just far enough to clear his thighs, shins and arms, so it never cuts through them.
 * In the gear texture (128x64) the rows sit at the top right from u 24, the strips under them; only the broad faces
 * are drawn, the outside on the south face and the lining on the north face.
 */
final class LordCloak {
	/** Row heights and widths, top to bottom. */
	private static final float[] ROW_HEIGHT = {5.0F, 5.0F, 5.0F};
	private static final float[] ROW_WIDTH = {12.0F, 13.0F, 14.0F};
	/** The hem strips, left to right in model x: where each starts and how wide it is. */
	private static final float[] STRIP_X = {-7.0F, -2.0F, 2.0F};
	private static final float[] STRIP_WIDTH = {5.0F, 4.0F, 5.0F};
	private static final float STRIP_HEIGHT = 7.0F;
	/**
	 * Where the cloak hangs from, just behind the breastplate under the helmet's rim. The pieces hinge on their outside
	 * face, so the outside stays closed where they bend; the lining is a pixel in front of it.
	 */
	private static final float TOP_Y = -0.5F;
	private static final float TOP_Z = 4.1F;
	private static final float THICKNESS = 1.0F;
	/** How far the cloth keeps off his limbs, in model pixels: it just brushes the backs of the pauldrons at rest. */
	private static final float CLEARANCE = 0.1F;
	/** The most a piece swings back from hanging straight down. */
	private static final float MAX_ANGLE = 1.55F;
	private static final float STEP = 0.03F;

	private final ModelPart body;
	private final ModelPart[] rows = new ModelPart[3];
	private final ModelPart[] strips = new ModelPart[3];
	/** Each limb segment as start, end and radius, refreshed in body space every frame. */
	private final Vector3f[] limbStart = new Vector3f[8];
	private final Vector3f[] limbEnd = new Vector3f[8];
	private final float[] limbRadius = {2.4F, 2.9F, 2.4F, 2.9F, 3.0F, 2.0F, 3.0F, 2.0F};

	LordCloak(ModelPart body) {
		this.body = body;
		rows[0] = body.getChild("cape");
		rows[1] = rows[0].getChild("cape_middle");
		rows[2] = rows[1].getChild("cape_lower");
		for (int i = 0; i < 3; i++) {
			strips[i] = rows[2].getChild("cape_strip_" + i);
		}
		for (int i = 0; i < limbStart.length; i++) {
			limbStart[i] = new Vector3f();
			limbEnd[i] = new Vector3f();
		}
	}

	/** The top row, the parent of the rest: hide it and the whole cloak goes. */
	ModelPart top() {
		return rows[0];
	}

	ModelPart row(int index) {
		return rows[index];
	}

	ModelPart strip(int index) {
		return strips[index];
	}

	static void addTo(PartDefinition body) {
		Set<Direction> sheet = EnumSet.of(Direction.NORTH, Direction.SOUTH);
		PartDefinition top = body.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-ROW_WIDTH[0] / 2.0F, 0.0F, -THICKNESS, ROW_WIDTH[0], ROW_HEIGHT[0], THICKNESS, sheet),
				PartPose.offset(0.0F, TOP_Y, TOP_Z));
		PartDefinition middle = top.addOrReplaceChild("cape_middle", CubeListBuilder.create()
				.texOffs(50, 0).addBox(-ROW_WIDTH[1] / 2.0F, 0.0F, -THICKNESS, ROW_WIDTH[1], ROW_HEIGHT[1], THICKNESS, sheet),
				PartPose.offset(0.0F, ROW_HEIGHT[0], 0.0F));
		PartDefinition lower = middle.addOrReplaceChild("cape_lower", CubeListBuilder.create()
				.texOffs(78, 0).addBox(-ROW_WIDTH[2] / 2.0F, 0.0F, -THICKNESS, ROW_WIDTH[2], ROW_HEIGHT[2], THICKNESS, sheet),
				PartPose.offset(0.0F, ROW_HEIGHT[1], 0.0F));
		int[] stripU = {24, 36, 46};
		for (int i = 0; i < 3; i++) {
			// Each strip hinges at its own top centre, so it swings about its middle.
			float centre = STRIP_X[i] + STRIP_WIDTH[i] / 2.0F;
			lower.addOrReplaceChild("cape_strip_" + i, CubeListBuilder.create()
					.texOffs(stripU[i], 6).addBox(-STRIP_WIDTH[i] / 2.0F, 0.0F, -THICKNESS, STRIP_WIDTH[i], STRIP_HEIGHT, THICKNESS, sheet),
					PartPose.offset(centre, ROW_HEIGHT[2], 0.0F));
		}
	}

	/** Poses the cloak for this frame once the limbs are posed. */
	void animate(FallenLordRenderState state, FallenLordModel model) {
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float wind = wind(state);
		float time = state.ageInTicks;
		// Ripples run down the cloth, faster and stronger as he moves.
		float rippleSpeed = 0.12F + 0.25F * walk + 0.3F * wind;
		float ripple = 0.025F + 0.07F * walk + 0.08F * wind;
		float step = state.walkAnimationPos * 0.6662F;
		// Once the empty armor collapses there are no limbs left to keep off.
		boolean solve = state.dying < FallenLord.COLLAPSE_START;
		if (solve) {
			collectLimbs(model);
		}

		float above = 0.0F;
		float y = TOP_Y;
		float z = TOP_Z;
		for (int i = 0; i < 3; i++) {
			// Gravity keeps it near straight down; moving lifts it, more toward the hem, and it streams back in the wind.
			float target = 0.05F + walk * (0.22F + 0.12F * i) + wind * (0.75F + 0.3F * i)
					+ ripple * Mth.sin(time * rippleSpeed - 0.9F * i)
					+ 1.2F * Math.max(0.0F, -body.xRot);
			if (solve) {
				target = clear(y, z, target, ROW_HEIGHT[i], -ROW_WIDTH[i] / 2.0F, ROW_WIDTH[i] / 2.0F);
			}
			target = Math.min(target, MAX_ANGLE);
			ModelPart row = rows[i];
			row.xRot = target - above;
			row.yRot = 0.0F;
			// Swings a little side to side with his steps, and stirs in the air when he stands.
			row.zRot = walk * 0.05F * Mth.sin(step + 0.6F * i) + 0.015F * Mth.sin(time * 0.05F + 0.8F * i);
			above = target;
			y += ROW_HEIGHT[i] * Mth.cos(target);
			z += ROW_HEIGHT[i] * Mth.sin(target);
		}
		for (int i = 0; i < 3; i++) {
			// The torn strips flap on their own, out of step with each other, and each is pushed off the leg behind it.
			float target = above + 0.04F * walk + (0.03F + 0.09F * walk + 0.1F * wind) * Mth.sin(time * (rippleSpeed + 0.08F) - 2.7F - 1.9F * i);
			if (solve) {
				target = clear(y, z, target, STRIP_HEIGHT, STRIP_X[i], STRIP_X[i] + STRIP_WIDTH[i]);
			}
			target = Math.min(target, MAX_ANGLE);
			ModelPart strip = strips[i];
			strip.xRot = target - above;
			strip.yRot = 0.0F;
			// The side strips splay out a little as they stream.
			strip.zRot = (i - 1) * -(0.06F * walk + 0.12F * wind) + 0.03F * Mth.sin(time * 0.09F + 2.1F * i);
		}
	}

	/** How hard the air pushes the cloak back during an attack, 0 to about 1. */
	private static float wind(FallenLordRenderState state) {
		float p = state.action.length() > 0 ? Mth.clamp(state.actionTime / state.action.length(), 0.0F, 1.0F) : 0.0F;
		float arc = Mth.sin((float) Math.PI * p);
		return switch (state.action) {
			case DASH, SWORD_DASH -> 0.75F;
			case RETREAT -> 0.5F * arc;
			case LUNGE -> 0.45F * arc;
			case WHIRL -> 0.9F * arc;
			case HOP -> 0.4F * arc;
			case LEAP_UP -> 0.6F * arc;
			case BACKSTAB -> 0.35F * arc;
			default -> 0.0F;
		};
	}

	/**
	 * The smallest angle from straight down, at least {@code target}, at which a piece hanging from {@code (y, z)} in
	 * body space keeps clear of every limb across its width.
	 */
	private float clear(float y, float z, float target, float length, float minX, float maxX) {
		float angle = target;
		while (angle < MAX_ANGLE && blocked(y, z, angle, length, minX, maxX)) {
			angle += STEP;
		}
		return angle;
	}

	private boolean blocked(float y, float z, float angle, float length, float minX, float maxX) {
		float cos = Mth.cos(angle);
		float sin = Mth.sin(angle);
		for (int k = 1; k <= 3; k++) {
			float s = length * k / 3.0F;
			if (z + s * sin - THICKNESS < limbBack(y + s * cos, minX, maxX) + CLEARANCE) {
				return true;
			}
		}
		return false;
	}

	/** How far back the limbs reach at height {@code y} in body space, among those overlapping the piece's width. */
	private float limbBack(float y, float minX, float maxX) {
		float back = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < limbStart.length; i++) {
			Vector3f a = limbStart[i];
			Vector3f b = limbEnd[i];
			float r = limbRadius[i];
			if (y < Math.min(a.y, b.y) - r || y > Math.max(a.y, b.y) + r) {
				continue;
			}
			float span = b.y - a.y;
			float p = Math.abs(span) < 1.0E-3F ? 0.5F : Mth.clamp((y - a.y) / span, 0.0F, 1.0F);
			float x = Mth.lerp(p, a.x, b.x);
			if (x + r < minX || x - r > maxX) {
				continue;
			}
			back = Math.max(back, Mth.lerp(p, a.z, b.z) + r);
		}
		return back;
	}

	/** Thighs, shins, upper arms and forearms, from joint to end, in the body's own space. */
	private void collectLimbs(FallenLordModel model) {
		Quaternionf toBody = new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot).conjugate();
		segment(0, model.rightLeg, model.rightShin, 0.0F, 0.0F, toBody);
		segment(2, model.leftLeg, model.leftShin, 0.0F, 0.0F, toBody);
		// The arm armor's box sits a pixel out from the shoulder joint.
		segment(4, model.rightArm, model.rightForearm, -1.0F, -2.0F, toBody);
		segment(6, model.leftArm, model.leftForearm, 1.0F, -2.0F, toBody);
	}

	/**
	 * Fills slots {@code index} (the upper segment, from {@code top} down its length, {@code side} out from the joint)
	 * and {@code index + 1} (the lower).
	 */
	private void segment(int index, ModelPart upper, ModelPart lower, float side, float top, Quaternionf toBody) {
		Quaternionf upperTurn = turn(upper);
		Vector3f joint = upperTurn.transform(new Vector3f(lower.x, lower.y, lower.z)).add(upper.x, upper.y, upper.z);
		// The arm's armor runs the whole arm's length from the shoulder: the upper segment covers it.
		float upperLength = top < 0.0F ? 12.0F : lower.y;
		toBodySpace(upperTurn.transform(new Vector3f(side, top, 0.0F)).add(upper.x, upper.y, upper.z), toBody, limbStart[index]);
		toBodySpace(upperTurn.transform(new Vector3f(side, top + upperLength, 0.0F)).add(upper.x, upper.y, upper.z), toBody, limbEnd[index]);
		Quaternionf lowerTurn = new Quaternionf(upperTurn).mul(turn(lower));
		toBodySpace(joint, toBody, limbStart[index + 1]);
		toBodySpace(lowerTurn.transform(new Vector3f(side, 6.0F, 0.0F)).add(joint), toBody, limbEnd[index + 1]);
	}

	private static Quaternionf turn(ModelPart part) {
		return new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
	}

	private void toBodySpace(Vector3f point, Quaternionf toBody, Vector3f out) {
		toBody.transform(point.sub(body.x, body.y, body.z), out);
	}
}
