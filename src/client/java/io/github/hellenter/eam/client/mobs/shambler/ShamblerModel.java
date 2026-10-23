package io.github.hellenter.eam.client.mobs.shambler;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The mushroom shambler: a body wrapped in mycelium with a broad mushroom cap grown over its head, smaller caps on its
 * shoulders and one on its back, and thin arms. Disguised it sinks down onto its folded legs with its arms pressed to its
 * sides and its head bowed under the cap: a thick stalk under a broad cap, like the mushrooms round it.
 */
public class ShamblerModel extends HumanoidModel<ShamblerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("mushroom_shambler"), "main");
	/** How far the whole body sinks when it crouches into a mushroom: the length of its legs, in pixels. */
	private static final float SINK = 12.0F;
	/** How far in toward the body its arms are drawn when disguised, in pixels. */
	private static final float ARM_TUCK = 2.0F;
	/** How far its head hangs forward under the cap while it walks, in radians. */
	private static final float HEAD_DROOP = 0.12F;

	public ShamblerModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		// The cap: a broad brim and a lower dome on top of it, tipped a little to one side.
		head.addOrReplaceChild("cap", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 3.0F, 14.0F)
				.texOffs(64, 0).addBox(-5.0F, -12.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.rotation(0.0F, 0.0F, 0.06F));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F), PartPose.ZERO);
		// A small cap sprouting from its back.
		body.addOrReplaceChild("back_cap", CubeListBuilder.create()
				.texOffs(64, 24).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(1.0F, 4.0F, 1.5F, -0.35F, 0.0F, 0.0F));
		// Thin arms, three pixels across, each with a little cap on the shoulder.
		PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
				.addBox(-2.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("shoulder_cap", CubeListBuilder.create().texOffs(64, 16)
				.addBox(-3.0F, -4.0F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.rotation(0.0F, 0.0F, -0.2F));
		PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
				.addBox(-1.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("shoulder_cap", CubeListBuilder.create().texOffs(64, 16).mirror()
				.addBox(-2.0F, -3.5F, -2.5F, 5.0F, 2.0F, 5.0F), PartPose.rotation(0.0F, 0.0F, 0.25F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(ShamblerRenderState state) {
		super.setupAnim(state);
		head.xRot += HEAD_DROOP;
		float d = state.disguise;
		if (d <= 0.0F) {
			return;
		}
		// Sinking onto its folded legs: everything above the hips drops by the length of the legs, which squash into the ground.
		float sink = SINK * d;
		head.y += sink;
		body.y += sink;
		rightArm.y += sink;
		leftArm.y += sink;
		rightLeg.y += sink;
		leftLeg.y += sink;
		float legs = 1.0F - d;
		rightLeg.yScale = legs;
		leftLeg.yScale = legs;
		rightLeg.visible = legs > 0.02F;
		leftLeg.visible = legs > 0.02F;
		rightLeg.xRot *= legs;
		leftLeg.xRot *= legs;
		// Arms hanging still and pressed in against its sides.
		rightArm.xRot *= legs;
		leftArm.xRot *= legs;
		rightArm.yRot *= legs;
		leftArm.yRot *= legs;
		rightArm.zRot *= legs;
		leftArm.zRot *= legs;
		rightArm.x += ARM_TUCK * d;
		leftArm.x -= ARM_TUCK * d;
		// Face forward, bowed under the cap.
		head.yRot *= legs;
		head.xRot = Mth.lerp(d, head.xRot, 0.2F);
	}

	/** Arms reaching out in front, like a zombie's, raised higher when it goes for someone. */
	@Override
	protected void setupAttackAnimation(ShamblerRenderState state) {
		super.setupAttackAnimation(state);
		AnimationUtils.animateZombieArms(leftArm, rightArm, state.isAggressive, state);
	}
}
