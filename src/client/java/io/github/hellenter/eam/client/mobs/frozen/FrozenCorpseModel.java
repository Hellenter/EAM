package io.github.hellenter.eam.client.mobs.frozen;

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
 * The frozen corpse: a frostbitten traveller in a worn coat, every part wearing a ragged second skin of ice a little
 * off the body. Icicles hang off the arms (on their back face, which points down while the arms reach forward) and
 * off the chin. It walks stiffly, head tilted to one side.
 */
public class FrozenCorpseModel extends HumanoidModel<ZombieRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("frozen_corpse"), "main");
	/** The head lolls to one side, frozen stiff. */
	private static final float HEAD_TILT = 0.2F;
	/** Stiff legs swing at this share of a zombie's stride. */
	private static final float STIFF_STRIDE = 0.6F;

	public FrozenCorpseModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		CubeDeformation crust = new CubeDeformation(0.35F);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				// Icicles off the chin.
				.texOffs(56, 0).addBox(-2.5F, 0.0F, -3.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(56, 0).addBox(0.5F, 0.0F, -3.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(56, 0).addBox(2.0F, 0.0F, -3.0F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, crust), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", arm(-3.0F, false, crust), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", arm(-1.0F, true, crust), PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
				.texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, crust), PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
				.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
				.texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, crust), PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** An arm with its crust and three icicles of different lengths along its back face. */
	private static CubeListBuilder arm(float x, boolean mirror, CubeDeformation crust) {
		return CubeListBuilder.create().mirror(mirror)
				.texOffs(40, 16).addBox(x, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
				.texOffs(40, 32).addBox(x, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, crust)
				.texOffs(0, 48).addBox(x + 0.5F, 3.0F, 2.0F, 1.0F, 1.0F, 3.0F)
				.texOffs(8, 48).addBox(x + 2.5F, 6.0F, 2.0F, 1.0F, 1.0F, 2.0F)
				.texOffs(0, 52).addBox(x + 1.5F, 8.5F, 2.0F, 1.0F, 1.0F, 4.0F);
	}

	@Override
	public void setupAnim(ZombieRenderState state) {
		super.setupAnim(state);
		head.zRot += HEAD_TILT;
		rightLeg.xRot *= STIFF_STRIDE;
		leftLeg.xRot *= STIFF_STRIDE;
	}

	/** Arms held out in front like a zombie's, raised higher when it goes for someone. */
	@Override
	protected void setupAttackAnimation(ZombieRenderState state) {
		super.setupAttackAnimation(state);
		AnimationUtils.animateZombieArms(leftArm, rightArm, state.isAggressive, state);
	}
}
