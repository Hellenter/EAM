package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Map;
import java.util.Set;

/**
 * The vanilla villager with his arms apart, hanging at his sides like a person's, so he walks swinging them and holds
 * items in his hands. Same texture layout as vanilla: each arm is the crossed-arms sleeve, a little longer, on top of a
 * bare hand cut from the crossed hands, the lower quarter of the arm. The clothes layers have no hands, so the skin always
 * shows. Children keep their crossed arms (their hands hold nothing).
 */
public class VillagerFigureModel extends HumanoidModel<VillagerFigureRenderState> implements VillagerLikeModel<VillagerFigureRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("villager"), "main");
	public static final ModelLayerLocation NO_HAT_LAYER = new ModelLayerLocation(Eam.id("villager"), "no_hat");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(Eam.id("villager_baby"), "main");
	public static final ModelLayerLocation BABY_NO_HAT_LAYER = new ModelLayerLocation(Eam.id("villager_baby"), "no_hat");
	public static final ArmorModelSet<ModelLayerLocation> ARMOR = new ArmorModelSet<>(armorLayer("helmet"), armorLayer("chestplate"),
			armorLayer("leggings"), armorLayer("boots"));
	/** Armor parts per slot: leggings are a skirt over the robe instead of the legs under it. */
	private static final Map<EquipmentSlot, Set<String>> ARMOR_PARTS = Map.of(EquipmentSlot.HEAD, Set.of("head"),
			EquipmentSlot.CHEST, Set.of("body", "left_arm", "right_arm"), EquipmentSlot.LEGS, Set.of("skirt"),
			EquipmentSlot.FEET, Set.of("left_leg", "right_leg"));

	private static final CubeDeformation SLEEVE = new CubeDeformation(0.0F, 0.5F, 0.0F);
	private static final CubeDeformation OUTER_ARMOR = new CubeDeformation(1.0F);
	private static final CubeDeformation INNER_ARMOR = new CubeDeformation(0.5F);

	public VillagerFigureModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		return LayerDefinition.create(createMesh(true), 64, 64);
	}

	/** The clothes layers: no head, and no hands, which their textures would cover with sleeve. */
	public static LayerDefinition createNoHatLayer() {
		MeshDefinition mesh = createMesh(false);
		mesh.getRoot().clearChild("head").clearRecursively();
		return LayerDefinition.create(mesh, 64, 64);
	}

	/**
	 * Armor shaped to the villager: the helmet over his tall head, the chestplate deep enough to sit over the robe and
	 * its sleeves down to the wrist, leggings as an armored skirt over the robe and boots on the feet below it.
	 */
	public static ArmorModelSet<LayerDefinition> createArmorLayerSet() {
		MeshDefinition head = createArmorMesh(OUTER_ARMOR);
		head.getRoot().retainPartsAndChildren(ARMOR_PARTS.get(EquipmentSlot.HEAD));
		MeshDefinition chest = createArmorMesh(OUTER_ARMOR);
		chest.getRoot().retainExactParts(ARMOR_PARTS.get(EquipmentSlot.CHEST));
		MeshDefinition legs = createArmorMesh(INNER_ARMOR);
		legs.getRoot().retainExactParts(ARMOR_PARTS.get(EquipmentSlot.LEGS));
		MeshDefinition feet = createArmorMesh(OUTER_ARMOR);
		feet.getRoot().retainExactParts(ARMOR_PARTS.get(EquipmentSlot.FEET));
		return new ArmorModelSet<>(head, chest, legs, feet).map(mesh -> LayerDefinition.create(mesh, 64, 32));
	}

	private static MeshDefinition createArmorMesh(CubeDeformation g) {
		MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0F);
		PartDefinition root = mesh.getRoot();
		// Just over the hat, which is 0.51 out from the head.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 8.0F, 8.0F, g.extend(-0.25F)), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		// The robe is 3.5 deep each way from the middle: the chestplate's 4-deep box grows to 3.7.
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, g.extend(0.1F, 0.1F, 0.7F)), PartPose.ZERO);
		// Each half of the skirt is a leg texture squeezed to 9 high and widened over its half of the robe.
		CubeDeformation skirt = new CubeDeformation(0.3F, -1.75F, 1.8F);
		body.addOrReplaceChild("skirt", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-4.3F, 11.25F, -2.0F, 4.0F, 12.0F, 4.0F, skirt)
				.texOffs(0, 16).mirror().addBox(0.3F, 11.25F, -2.0F, 4.0F, 12.0F, 4.0F, skirt), PartPose.ZERO);
		// Sleeves end at the wrist, over the 9-long sleeve of the arm, so the hand shows.
		CubeDeformation sleeve = g.extend(0.0F, -2.0F, 0.0F);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -4.0F, -2.0F, 4.0F, 12.0F, 4.0F, sleeve),
				PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -4.0F, -2.0F, 4.0F, 12.0F, 4.0F, sleeve),
				PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(0.1F)),
				PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(0.1F)),
				PartPose.offset(2.0F, 12.0F, 0.0F));
		return mesh;
	}

	public static LayerDefinition createBabyLayer() {
		return LayerDefinition.create(createBabyMesh(), 64, 64);
	}

	public static LayerDefinition createBabyNoHatLayer() {
		MeshDefinition mesh = createBabyMesh();
		mesh.getRoot().clearChild("head").clearRecursively();
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static MeshDefinition createMesh(boolean hands) {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
		PartDefinition hat = head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO);
		hat.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
				PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F));
		head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offset(0.0F, -2.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO);
		body.addOrReplaceChild("jacket",
				CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		// Sleeve: the 8-long crossed-arm box drawn 9 long. Hand: 3 of the crossed hands below it, so the arm is 12 long.
		CubeListBuilder rightArm = CubeListBuilder.create().texOffs(44, 22).addBox(-3.0F, -1.5F, -2.0F, 4.0F, 8.0F, 4.0F, SLEEVE);
		CubeListBuilder leftArm = CubeListBuilder.create().mirror().texOffs(44, 22).addBox(-1.0F, -1.5F, -2.0F, 4.0F, 8.0F, 4.0F, SLEEVE);
		if (hands) {
			rightArm.texOffs(44, 38).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F);
			leftArm.texOffs(44, 38).addBox(-1.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F);
		}
		root.addOrReplaceChild("right_arm", rightArm, PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", leftArm, PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
		return mesh;
	}

	/** The vanilla child, with empty arm parts where its hands are so it fits the humanoid model. */
	private static MeshDefinition createBabyMesh() {
		MeshDefinition mesh = BabyVillagerModel.createBodyModel();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-2.0F, 17.5F, -2.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(2.0F, 17.5F, -2.0F));
		return mesh;
	}

	private static ModelLayerLocation armorLayer(String slot) {
		return new ModelLayerLocation(Eam.id("villager"), slot);
	}

	@Override
	public void setupAnim(VillagerFigureRenderState state) {
		super.setupAnim(state);
		if (state.isUnhappy) {
			head.zRot = 0.3F * Mth.sin(0.45F * state.ageInTicks);
			head.xRot = 0.4F;
		} else {
			head.zRot = 0.0F;
		}
	}

	@Override
	public void translateToArms(VillagerFigureRenderState state, PoseStack poseStack) {
		translateToHand(state, HumanoidArm.RIGHT, poseStack);
	}
}
