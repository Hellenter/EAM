package io.github.hellenter.eam.client.mobs.upyr;

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
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

/**
 * The upyr: a gaunt villager corpse with thin arms reaching out like a zombie's, two fangs over a bloodied chin, lank
 * hair, and a burial shirt hanging to mid-thigh with a torn hem. Its head hangs forward a little.
 */
public class UpyrModel extends HumanoidModel<ZombieRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("upyr"), "main");
	/** How far the head droops forward, in radians. */
	private static final float HEAD_DROOP = 0.18F;

	public UpyrModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				// Two fangs over the lower lip.
				.texOffs(0, 32).addBox(-2.5F, -3.0F, -4.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(4, 32).addBox(1.5F, -3.0F, -4.5F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
		// Lank hair: the outer layer, mostly see-through.
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		// The body, and the burial shirt over it, hanging below the hips with a torn hem.
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 15.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.ZERO);
		// Thin arms, three pixels across.
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
				.addBox(-2.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
				.addBox(-1.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(ZombieRenderState state) {
		super.setupAnim(state);
		head.xRot += HEAD_DROOP;
	}

	/** Arms held out in front like a zombie's, raised higher when it goes for someone. */
	@Override
	protected void setupAttackAnimation(ZombieRenderState state) {
		super.setupAttackAnimation(state);
		AnimationUtils.animateZombieArms(leftArm, rightArm, state.isAggressive, state);
	}
}
