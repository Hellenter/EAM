package io.github.hellenter.eam.client.mobs.ram;

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
 * A cave ram: a sheep's build, bigger and deeper in the chest, with a shaggy ruff round the neck, a narrow muzzle and
 * heavy horns that go back from the brow, down behind the head and curl forward under the ears. Lambs have no horns
 * and a bigger head.
 */
public class CaveRamModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("cave_ram"), "main");
	private static final float LAMB_HEAD_SCALE = 1.4F;

	private final ModelPart head;
	private final ModelPart horns;
	private final ModelPart tail;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

	public CaveRamModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		horns = head.getChild("horns");
		tail = root.getChild("tail");
		rightHindLeg = root.getChild("right_hind_leg");
		leftHindLeg = root.getChild("left_hind_leg");
		rightFrontLeg = root.getChild("right_front_leg");
		leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -5.0F, -8.0F, 10.0F, 10.0F, 16.0F), PartPose.offset(0.0F, 11.0F, 1.0F));
		// The ruff: long wool round the neck and over the chest.
		root.addOrReplaceChild("ruff", CubeListBuilder.create()
				.texOffs(0, 40).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 9.0F, -8.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 6.0F)
				.texOffs(24, 26).addBox(-2.0F, 0.0F, -8.0F, 4.0F, 3.0F, 2.0F),
				PartPose.offset(0.0F, 6.0F, -9.0F));
		// Each horn: back from the brow, down behind the head, forward under the ear.
		head.addOrReplaceChild("horns", CubeListBuilder.create()
				.texOffs(36, 26).addBox(-4.5F, -4.0F, -4.0F, 2.0F, 2.0F, 5.0F)
				.texOffs(50, 26).addBox(-5.0F, -2.0F, 0.0F, 2.0F, 5.0F, 2.0F)
				.texOffs(36, 33).addBox(-5.0F, 2.0F, -4.0F, 2.0F, 2.0F, 4.0F)
				.mirror()
				.texOffs(36, 26).addBox(2.5F, -4.0F, -4.0F, 2.0F, 2.0F, 5.0F)
				.texOffs(50, 26).addBox(3.0F, -2.0F, 0.0F, 2.0F, 5.0F, 2.0F)
				.texOffs(36, 33).addBox(3.0F, 2.0F, -4.0F, 2.0F, 2.0F, 4.0F)
				.mirror(false), PartPose.ZERO);
		root.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(40, 40).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 7.0F, 9.0F, 0.3F, 0.0F, 0.0F));
		CubeListBuilder leg = CubeListBuilder.create().texOffs(24, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F);
		CubeListBuilder mirroredLeg = CubeListBuilder.create().mirror().texOffs(24, 40).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 14.0F, 6.0F));
		root.addOrReplaceChild("left_hind_leg", mirroredLeg, PartPose.offset(3.0F, 14.0F, 6.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 14.0F, -4.0F));
		root.addOrReplaceChild("left_front_leg", mirroredLeg, PartPose.offset(3.0F, 14.0F, -4.0F));
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
		tail.zRot = Mth.cos(pos * 0.6662F) * 0.3F * speed;
		horns.visible = !state.isBaby;
		if (state.isBaby) {
			head.xScale = LAMB_HEAD_SCALE;
			head.yScale = LAMB_HEAD_SCALE;
			head.zScale = LAMB_HEAD_SCALE;
		}
	}
}
