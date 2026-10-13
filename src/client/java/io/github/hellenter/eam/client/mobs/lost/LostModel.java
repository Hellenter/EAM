package io.github.hellenter.eam.client.mobs.lost;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;

/**
 * One of the Lost: a husk's head and dried-up torso on limbs thinner than a husk's and thicker than a skeleton's, bone
 * showing through on the forearms and shins, with a rag round the hips. Built on the Fallen Lord's model so it bends
 * at the elbows and knees and fights, guards and jabs with his animations. The armor models have the same joints.
 */
public class LostModel extends FallenLordModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("lost"), "main");
	public static final ArmorModelSet<ModelLayerLocation> ARMOR = new ArmorModelSet<>(armorLayer("helmet"), armorLayer("chestplate"),
			armorLayer("leggings"), armorLayer("boots"));

	public LostModel(ModelPart root) {
		super(root);
	}

	private static ModelLayerLocation armorLayer(String name) {
		return new ModelLayerLocation(Eam.id("lost"), name);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		// The torso, and the rag round the hips over it.
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(16, 34).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.35F)), PartPose.ZERO);
		addLimbs(root,
				CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().texOffs(40, 25).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().mirror().texOffs(40, 25).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().texOffs(0, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
				CubeListBuilder.create().mirror().texOffs(0, 25).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F));
		return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	/**
	 * Armor models on his joints: the vanilla armor boxes, the arm and leg boxes each split in two at the elbow and the
	 * knee, so the vanilla armor textures fit (the lower half reads the lower rows of the same texture).
	 */
	public static ArmorModelSet<LayerDefinition> createArmorLayers() {
		CubeDeformation outer = new CubeDeformation(1.0F);
		CubeDeformation inner = new CubeDeformation(0.5F);
		return new ArmorModelSet<>(armor(true, false, false, outer), armor(false, true, false, outer), armor(false, true, true, inner),
				armor(false, false, true, outer));
	}

	private static LayerDefinition armor(boolean head, boolean body, boolean legs, CubeDeformation g) {
		boolean arms = body && !legs;
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition headPart = root.addOrReplaceChild("head",
				head ? CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g) : CubeListBuilder.create(),
				PartPose.ZERO);
		headPart.addOrReplaceChild("hat",
				head ? CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, g.extend(0.5F)) : CubeListBuilder.create(),
				PartPose.ZERO);
		root.addOrReplaceChild("body",
				body ? CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, g) : CubeListBuilder.create(),
				PartPose.ZERO);
		CubeDeformation leg = g.extend(-0.1F);
		addLimbs(root,
				arms ? CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, g) : CubeListBuilder.create(),
				arms ? CubeListBuilder.create().texOffs(40, 22).addBox(-3.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, g) : CubeListBuilder.create(),
				arms ? CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, g) : CubeListBuilder.create(),
				arms ? CubeListBuilder.create().mirror().texOffs(40, 22).addBox(-1.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, g) : CubeListBuilder.create(),
				legs ? CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, leg) : CubeListBuilder.create(),
				legs ? CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, leg) : CubeListBuilder.create(),
				legs ? CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, leg) : CubeListBuilder.create(),
				legs ? CubeListBuilder.create().mirror().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, leg) : CubeListBuilder.create());
		return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		// Drawing a bow or aiming a crossbow he holds both arms out straight.
		HumanoidModel.ArmPose pose = state.rightArmPose;
		if (pose == HumanoidModel.ArmPose.BOW_AND_ARROW || pose == HumanoidModel.ArmPose.CROSSBOW_HOLD
				|| pose == HumanoidModel.ArmPose.CROSSBOW_CHARGE) {
			rightForearm.xRot = 0.0F;
			leftForearm.xRot = 0.0F;
		}
	}
}
