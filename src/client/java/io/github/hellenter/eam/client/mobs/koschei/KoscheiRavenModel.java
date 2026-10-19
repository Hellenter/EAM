package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A raven in flight: a slim body, a small head with a heavy beak, a fanned tail, and long wings in two parts that beat
 * deep, the tips trailing the inner wing. Drawn see-through.
 */
public class KoscheiRavenModel extends EntityModel<KoscheiRavenRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("koschei_raven"), "main");

	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart leftWing;
	private final ModelPart leftWingTip;
	private final ModelPart rightWing;
	private final ModelPart rightWingTip;

	public KoscheiRavenModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		ModelPart body = root.getChild("body");
		head = body.getChild("head");
		tail = body.getChild("tail");
		leftWing = body.getChild("left_wing");
		leftWingTip = leftWing.getChild("left_wing_tip");
		rightWing = body.getChild("right_wing");
		rightWingTip = rightWing.getChild("right_wing_tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 6.0F),
				PartPose.offset(0.0F, 20.0F, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, -0.8F, -3.0F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(18, 4).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, 0.2F, -2.0F, 0.15F, 0.0F, 0.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 9).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, -0.1F, 0.0F, 0.0F));
		PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 14).addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 5.0F),
				PartPose.offset(1.5F, -1.5F, -2.0F));
		leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().texOffs(0, 20).addBox(0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 4.0F),
				PartPose.offset(3.0F, 0.0F, 0.0F));
		PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 14).mirror()
				.addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 5.0F), PartPose.offset(-1.5F, -1.5F, -2.0F));
		rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().texOffs(0, 20).mirror()
				.addBox(-4.0F, 0.0F, 0.0F, 4.0F, 1.0F, 4.0F), PartPose.offset(-3.0F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(KoscheiRavenRenderState state) {
		super.setupAnim(state);
		// A deep beat, the tips lagging behind the inner wing.
		float beat = Mth.cos(state.flap);
		float lag = Mth.cos(state.flap - 0.9F);
		leftWing.zRot = beat * 0.75F - 0.1F;
		leftWingTip.zRot = lag * 0.5F;
		rightWing.zRot = -leftWing.zRot;
		rightWingTip.zRot = -leftWingTip.zRot;
		tail.xRot = -0.1F + Mth.cos(state.flap * 0.5F) * 0.08F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * -0.3F;
	}
}
