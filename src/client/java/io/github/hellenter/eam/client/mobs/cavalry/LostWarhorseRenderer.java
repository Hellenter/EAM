package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.cavalry.LostWarhorse;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.EnumSet;

/**
 * The heavy cavalryman's horse: the vanilla horse model with a dried-up hide over bare bones, and its barding over it
 * on a slightly larger copy of the model: a chanfron with a spike and eye holes, plates down the neck, a breastplate,
 * flank and rump plates over chainmail and a chainmail skirt hanging along its flanks.
 */
public class LostWarhorseRenderer extends AbstractHorseRenderer<LostWarhorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
	public static final ModelLayerLocation BARDING = new ModelLayerLocation(Eam.id("lost_warhorse"), "barding");
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost_cavalry/lost_warhorse.png");
	private static final Identifier BARDING_TEXTURE = Eam.id("textures/entity/lost_cavalry/lost_warhorse_barding.png");

	public LostWarhorseRenderer(EntityRendererProvider.Context context) {
		super(context, new HorseModel(context.bakeLayer(ModelLayers.ZOMBIE_HORSE)), new BabyHorseModel(context.bakeLayer(ModelLayers.ZOMBIE_HORSE_BABY)));
		addLayer(new BardingLayer(this, new HorseModel(context.bakeLayer(BARDING))));
	}

	@Override
	public Identifier getTextureLocation(EquineRenderState state) {
		return TEXTURE;
	}

	@Override
	public EquineRenderState createRenderState() {
		return new EquineRenderState();
	}

	/**
	 * The barding: the parts of the horse model a little larger (texture 128x64, the left half in the horse layout), the
	 * body plates a little larger still, a spike on the chanfron and a chainmail skirt on both flanks (the right half).
	 */
	public static LayerDefinition createBardingLayer() {
		CubeDeformation g = new CubeDeformation(0.35F);
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 32).addBox(-5.0F, -8.0F, -17.0F, 10.0F, 10.0F, 22.0F, new CubeDeformation(0.6F)),
				PartPose.offset(0.0F, 11.0F, 5.0F));
		body.addOrReplaceChild("skirt", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-6.0F, 1.0F, -15.0F, 12.0F, 7.0F, 20.0F, EnumSet.of(Direction.EAST, Direction.WEST)), PartPose.ZERO);
		body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition headParts = root.addOrReplaceChild("head_parts",
				CubeListBuilder.create().texOffs(0, 35).addBox(-2.05F, -6.0F, -2.0F, 4.0F, 12.0F, 7.0F, new CubeDeformation(0.45F)),
				PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition head = headParts.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 13).addBox(-3.0F, -11.0F, -2.0F, 6.0F, 5.0F, 7.0F, g), PartPose.ZERO);
		head.addOrReplaceChild("spike", CubeListBuilder.create()
				.texOffs(64, 30).addBox(-0.5F, -11.5F, -5.0F, 1.0F, 1.0F, 3.0F), PartPose.rotation(0.25F, 0.0F, 0.0F));
		headParts.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(56, 36).addBox(-1.0F, -11.0F, 5.01F, 2.0F, 16.0F, 2.0F, g),
				PartPose.ZERO);
		headParts.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, -11.0F, -7.0F, 4.0F, 5.0F, 5.0F, g),
				PartPose.ZERO);
		CubeDeformation leg = new CubeDeformation(0.3F);
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(48, 21).mirror()
				.addBox(-3.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, leg), PartPose.offset(4.0F, 14.0F, 7.0F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(48, 21)
				.addBox(-1.0F, -1.01F, -1.0F, 4.0F, 11.0F, 4.0F, leg), PartPose.offset(-4.0F, 14.0F, 7.0F));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(48, 21).mirror()
				.addBox(-3.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, leg), PartPose.offset(4.0F, 14.0F, -10.0F));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(48, 21)
				.addBox(-1.0F, -1.01F, -1.9F, 4.0F, 11.0F, 4.0F, leg), PartPose.offset(-4.0F, 14.0F, -10.0F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(19, 16)
				.addBox(0.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(19, 16)
				.addBox(-2.55F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)), PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static class BardingLayer extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {
		private final HorseModel model;

		BardingLayer(RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer, HorseModel model) {
			super(renderer);
			this.model = model;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EquineRenderState state, float yRot, float xRot) {
			coloredCutoutModelCopyLayerRender(model, BARDING_TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
		}
	}
}
