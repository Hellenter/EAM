package io.github.hellenter.eam.client.mobs.beetle;

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
 * An ember beetle: a low, broad coal shell over a narrower belly, a small head with two short mandibles and feelers, and
 * three thin legs a side splayed out like a spider's. The shell's cracks glow, drawn by the renderer's glow layer.
 */
public class EmberBeetleModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("ember_beetle"), "main");
	private static final String[] LEGS = {"right_front_leg", "right_middle_leg", "right_hind_leg", "left_front_leg", "left_middle_leg",
			"left_hind_leg"};
	/** How far each pair of legs fans forward (front) or back (hind) from straight out sideways. */
	private static final float[] FAN = {0.5F, 0.0F, -0.5F};
	private static final float LEG_DROOP = 0.75F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart[] legs = new ModelPart[LEGS.length];

	public EmberBeetleModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		for (int i = 0; i < LEGS.length; i++) {
			legs[i] = root.getChild(LEGS[i]);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5F, -5.0F, -6.0F, 9.0F, 3.0F, 12.0F)
				.texOffs(0, 15).addBox(-3.5F, -2.0F, -5.5F, 7.0F, 3.0F, 11.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(42, 0).addBox(-2.5F, -2.0F, -4.0F, 5.0F, 3.0F, 4.0F)
				.texOffs(42, 7).addBox(-2.0F, 0.0F, -6.0F, 1.0F, 1.0F, 2.0F)
				.texOffs(42, 7).mirror().addBox(1.0F, 0.0F, -6.0F, 1.0F, 1.0F, 2.0F).mirror(false), PartPose.offset(0.0F, 19.5F, -6.0F));
		head.addOrReplaceChild("right_feeler", CubeListBuilder.create()
				.texOffs(48, 7).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-1.5F, -1.5F, -3.5F, 0.5F, 0.4F, 0.0F));
		head.addOrReplaceChild("left_feeler", CubeListBuilder.create()
				.texOffs(48, 7).mirror().addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(1.5F, -1.5F, -3.5F, 0.5F, -0.4F, 0.0F));
		CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(42, 12).addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F);
		CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(42, 12).mirror().addBox(0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F);
		for (int i = 0; i < 3; i++) {
			float z = -3.5F + 3.5F * i;
			root.addOrReplaceChild(LEGS[i], rightLeg, PartPose.offset(-3.0F, 20.5F, z));
			root.addOrReplaceChild(LEGS[i + 3], leftLeg, PartPose.offset(3.0F, 20.5F, z));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		// A spider's gait: legs swing in two tripods, front and hind of one side with the middle of the other.
		for (int i = 0; i < 3; i++) {
			float phase = i == 1 ? Mth.PI : 0.0F;
			float swing = Mth.cos(pos * 1.2F + phase) * 0.5F * speed;
			float lift = Math.abs(Mth.sin(pos * 0.6F + phase * 0.5F)) * 0.4F * speed;
			ModelPart right = legs[i];
			ModelPart left = legs[i + 3];
			right.yRot = -FAN[i] + swing;
			left.yRot = FAN[i] + swing;
			right.zRot = -LEG_DROOP + lift;
			left.zRot = LEG_DROOP - lift;
		}
		body.y = 20.0F + Mth.cos(pos * 2.4F) * 0.15F * speed;
	}
}
