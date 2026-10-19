package io.github.hellenter.eam.client.mobs.boar;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * An underground boar: a pig's build, a little bigger and higher in the shoulder, with a ridge of bristles down the
 * back, a long snout, tusks curving up from the lower jaw and a thin tail. Piglets have no tusks and a bigger head.
 */
public class BoarModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("boar"), "main");
	private static final float BABY_HEAD_SCALE = 1.4F;

	private final ModelPart head;
	private final ModelPart tusks;
	private final ModelPart tail;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

	public BoarModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		tusks = head.getChild("tusks");
		tail = root.getChild("tail");
		rightHindLeg = root.getChild("right_hind_leg");
		leftHindLeg = root.getChild("left_hind_leg");
		rightFrontLeg = root.getChild("right_front_leg");
		leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -4.5F, -8.0F, 10.0F, 9.0F, 16.0F), PartPose.offset(0.0F, 12.5F, 1.0F));
		// Bristles from the shoulders halfway down the back.
		body.addOrReplaceChild("ridge", CubeListBuilder.create()
				.texOffs(0, 25).addBox(-1.5F, -6.5F, -8.0F, 3.0F, 2.0F, 12.0F), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(30, 25).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
				.texOffs(0, 39).addBox(-2.5F, 0.0F, -9.0F, 5.0F, 4.0F, 3.0F)
				.texOffs(20, 39).addBox(-4.0F, -6.0F, -3.0F, 2.0F, 2.0F, 1.0F)
				.texOffs(20, 39).mirror().addBox(2.0F, -6.0F, -3.0F, 2.0F, 2.0F, 1.0F).mirror(false),
				PartPose.offset(0.0F, 12.0F, -7.0F));
		// Tusks from the sides of the lower jaw, sticking up past the snout.
		head.addOrReplaceChild("tusks", CubeListBuilder.create()
				.texOffs(16, 39).addBox(-3.5F, -1.0F, -8.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(16, 39).mirror().addBox(2.5F, -1.0F, -8.5F, 1.0F, 3.0F, 1.0F).mirror(false), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(28, 46).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 9.0F, 0.3F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F);
		CubeListBuilder mirroredLeg = CubeListBuilder.create().mirror().texOffs(0, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 17.0F, 6.0F));
		root.addOrReplaceChild("left_hind_leg", mirroredLeg, PartPose.offset(3.0F, 17.0F, 6.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 17.0F, -5.0F));
		root.addOrReplaceChild("left_front_leg", mirroredLeg, PartPose.offset(3.0F, 17.0F, -5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float swing = Mth.cos(pos * 0.6662F) * 1.4F * speed;
		rightHindLeg.xRot = swing;
		leftHindLeg.xRot = -swing;
		rightFrontLeg.xRot = -swing;
		leftFrontLeg.xRot = swing;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		tail.zRot = Mth.cos(pos * 0.6662F) * 0.4F * speed;
		tusks.visible = !state.isBaby;
		if (state.isBaby) {
			head.xScale = BABY_HEAD_SCALE;
			head.yScale = BABY_HEAD_SCALE;
			head.zScale = BABY_HEAD_SCALE;
		}
	}
}
