package io.github.hellenter.eam.client.mobs.ghosthorse;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;

/**
 * The ghost horse: a horse's frame starved thin, a narrow barrel of ribs, a thin neck, a bare skull with a hanging jaw,
 * a rat's tail, and legs in two parts, heavy hooves under them, that bend at the knee as it walks. The parts keep the vanilla horse's names and
 * pivots, so the horse animation drives it; {@link #createArmorLayer()} shrinks vanilla horse armor onto it.
 */
public class GhostHorseModel extends AbstractEquineModel<EquineRenderState> {
	/** A living horse's model scale; vanilla horse armor is drawn at it. */
	private static final float LIVING_HORSE_SCALE = 1.1F;
	private static final String LOWER_LEG = "lower_leg";
	/** How far a knee folds for each radian its leg swings forward. */
	private static final float KNEE_FOLD = 1.3F;

	private final ModelPart rightHindShin;
	private final ModelPart leftHindShin;
	private final ModelPart rightFrontShin;
	private final ModelPart leftFrontShin;

	public GhostHorseModel(ModelPart root) {
		super(root);
		rightHindShin = rightHindLeg.getChild(LOWER_LEG);
		leftHindShin = leftHindLeg.getChild(LOWER_LEG);
		rightFrontShin = rightFrontLeg.getChild(LOWER_LEG);
		leftFrontShin = leftFrontLeg.getChild(LOWER_LEG);
	}

	@Override
	public void setupAnim(EquineRenderState state) {
		super.setupAnim(state);
		fold(rightHindLeg, rightHindShin);
		fold(leftHindLeg, leftHindShin);
		fold(rightFrontLeg, rightFrontShin);
		fold(leftFrontLeg, leftFrontShin);
	}

	/** A leg swinging forward lifts its hoof: the lower leg folds back under the knee. */
	private static void fold(ModelPart leg, ModelPart shin) {
		shin.xRot = Math.max(0.0F, -leg.xRot) * KNEE_FOLD;
	}

	/** Texture 64x64, painted by {@code tools/ghost-horse/ghost_horse_textures.py} over the same boxes. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 34).addBox(-3.5F, -8.0F, -17.0F, 7.0F, 8.0F, 22.0F),
				PartPose.offset(0.0F, 11.0F, 5.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 14).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 14.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition headParts = root.addOrReplaceChild("head_parts", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-1.5F, -6.0F, -1.0F, 3.0F, 12.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition head = headParts.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 0).addBox(-2.5F, -11.0F, -2.0F, 5.0F, 5.0F, 7.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 0).addBox(0.5F, -13.0F, 4.0F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-1.5F, -13.0F, 4.0F, 1.0F, 3.0F, 1.0F),
				PartPose.ZERO);
		headParts.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(40, 0).addBox(-1.5F, -10.5F, -7.0F, 3.0F, 4.0F, 5.0F),
				PartPose.ZERO);
		headParts.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(40, 9).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, -6.5F, -2.5F, 0.3F, 0.0F, 0.0F));
		headParts.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(56, 9).addBox(-1.0F, -11.0F, 4.0F, 2.0F, 16.0F, 2.0F), PartPose.ZERO);
		leg(root, "left_hind_leg", 2.5F, 7.0F, true);
		leg(root, "right_hind_leg", -2.5F, 7.0F, false);
		leg(root, "left_front_leg", 2.5F, -10.0F, true);
		leg(root, "right_front_leg", -2.5F, -10.0F, false);
		return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(LIVING_HORSE_SCALE));
	}

	/**
	 * A leg in two parts: the forearm or gaskin down to the knee, then the shin, as thick as the forearm, ending in a wider
	 * hoof under a fringe of hair, ground at y 24.
	 */
	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		PartDefinition upper = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(16, 12).mirror(mirror)
				.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(x, 12.0F, z));
		upper.addOrReplaceChild(LOWER_LEG, CubeListBuilder.create().mirror(mirror)
				.texOffs(0, 17).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F)
				.texOffs(0, 24).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 5.0F, 0.0F));
	}

	/**
	 * Vanilla horse armor shrunk onto the thin frame: the vanilla armor boxes, so its textures fit as on a horse, each
	 * part scaled and moved to sit just over the matching part here.
	 */
	public static LayerDefinition createArmorLayer() {
		MeshDefinition mesh = AbstractEquineModel.createBodyMesh(new CubeDeformation(0.1F));
		PartDefinition root = mesh.getRoot();
		// Body: 10 x 10 down to 7.4 x 8.4, its top kept on the back.
		reshape(root, "body", pose -> sized(pose, 0.0F, -1.5F, 0.0F, 0.74F, 0.83F, 1.0F));
		// Neck narrower and shallower; the head and muzzle under it get their length back.
		PartDefinition headParts = reshape(root, "head_parts", pose -> sized(pose, 0.0F, 0.0F, 0.0F, 0.8F, 1.0F, 0.8F));
		reshape(headParts, "head", pose -> sized(pose, 0.0F, 0.0F, 0.0F, 1.1F, 1.0F, 1.25F));
		reshape(headParts, "upper_mouth", pose -> sized(pose, 0.0F, 0.0F, 0.0F, 1.0625F, 1.0F, 1.25F));
		reshape(headParts, "mane", pose -> sized(pose, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.25F));
		// Legs: 4 x 4 down to 3.4 x 3.4, stretched to the longer legs, centred over them.
		float legHeight = 13.1F / 11.2F;
		reshape(root, "left_hind_leg", pose -> sized(pose, -0.7F, -2.0F, -0.8F, 0.8F, legHeight, 0.8F));
		reshape(root, "right_hind_leg", pose -> sized(pose, 0.7F, -2.0F, -0.8F, 0.8F, legHeight, 0.8F));
		reshape(root, "left_front_leg", pose -> sized(pose, -0.7F, -2.0F, -0.08F, 0.8F, legHeight, 0.8F));
		reshape(root, "right_front_leg", pose -> sized(pose, 0.7F, -2.0F, -0.08F, 0.8F, legHeight, 0.8F));
		return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(LIVING_HORSE_SCALE));
	}

	private static PartDefinition reshape(PartDefinition parent, String name, java.util.function.UnaryOperator<PartPose> change) {
		return parent.addOrReplaceChild(name, parent.getChild(name).transformed(change));
	}

	private static PartPose sized(PartPose pose, float dx, float dy, float dz, float xScale, float yScale, float zScale) {
		return new PartPose(pose.x() + dx, pose.y() + dy, pose.z() + dz, pose.xRot(), pose.yRot(), pose.zRot(), pose.xScale() * xScale,
				pose.yScale() * yScale, pose.zScale() * zScale);
	}
}
