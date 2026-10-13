package io.github.hellenter.eam.client.mobs.warg;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A warg: bigger and rangier than a wolf, long in the body and the legs. A shaggy ruff over the shoulders and chest, a
 * ridge of bristling hair along the back, a long snout with fur on the cheeks, heavy-thighed hind legs and a bushy
 * tail. Hunting, it carries its head low and its tail straight out. The leader is the same body, drawn bigger.
 */
public class WargModel extends EntityModel<WargRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("warg"), "main");
	private static final float HEAD_Y = 8.5F;
	private static final float TAIL_REST = 0.65F;
	private static final float TAIL_ANGRY = 1.35F;

	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

	public WargModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
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
				.texOffs(0, 0).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 8.0F, 14.0F), PartPose.offset(0.0F, 11.0F, 1.0F));
		body.addOrReplaceChild("ridge", CubeListBuilder.create()
				.texOffs(34, 22).addBox(-1.5F, -5.5F, -5.0F, 3.0F, 2.0F, 10.0F), PartPose.ZERO);
		root.addOrReplaceChild("mane", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-5.0F, -5.5F, -4.0F, 10.0F, 11.0F, 7.0F), PartPose.offset(0.0F, 10.0F, -4.5F));
		root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 40).addBox(-3.5F, -3.5F, -5.0F, 7.0F, 7.0F, 5.0F)
				.texOffs(24, 40).addBox(-2.0F, 0.0F, -10.0F, 4.0F, 3.0F, 5.0F)
				.texOffs(42, 40).addBox(-3.5F, -6.0F, -2.0F, 2.0F, 3.0F, 1.0F)
				.texOffs(42, 40).mirror().addBox(1.5F, -6.0F, -2.0F, 2.0F, 3.0F, 1.0F).mirror(false)
				.texOffs(0, 52).addBox(-4.5F, -1.0F, -3.0F, 9.0F, 4.0F, 3.0F), PartPose.offset(0.0F, HEAD_Y, -8.0F));
		root.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(24, 48).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 8.0F, TAIL_REST, 0.0F, 0.0F));
		CubeListBuilder frontLeg = CubeListBuilder.create().texOffs(48, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F);
		root.addOrReplaceChild("right_front_leg", frontLeg, PartPose.offset(-2.5F, 15.0F, -4.5F));
		root.addOrReplaceChild("left_front_leg", frontLeg, PartPose.offset(2.5F, 15.0F, -4.5F));
		// Hind legs with heavy, furred thighs.
		CubeListBuilder hindLeg = CubeListBuilder.create()
				.texOffs(48, 52).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F)
				.texOffs(44, 0).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 6.0F, 5.0F);
		root.addOrReplaceChild("right_hind_leg", hindLeg, PartPose.offset(-2.5F, 15.0F, 5.0F));
		root.addOrReplaceChild("left_hind_leg", hindLeg, PartPose.offset(2.5F, 15.0F, 5.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WargRenderState state) {
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
		if (state.angry) {
			// Head low between the shoulders, tail straight out behind.
			head.y = HEAD_Y + 1.5F;
			head.xRot += 0.3F;
			tail.xRot = TAIL_ANGRY;
			tail.yRot = Mth.cos(state.ageInTicks * 0.5F) * 0.06F;
		} else {
			tail.xRot = TAIL_REST + Mth.sin(state.ageInTicks * 0.08F) * 0.06F;
			tail.yRot = Mth.cos(pos * 0.6662F) * 0.5F * speed;
		}
	}
}
