package io.github.hellenter.eam.client.mobs.ghosthorse;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.ghosthorse.GhostHorse;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;

/**
 * The ghost horse: a gaunt horse, narrower in the body, neck and legs than a living one, with a bare skull for a head, a
 * thin rat's tail and a ragged mane (texture 64x64, laid out for these boxes, not the vanilla horse's). Its eyes glow red
 * whatever the light, and it wears horse armor like a horse.
 */
public class GhostHorseRenderer extends AbstractHorseRenderer<GhostHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("ghost_horse"), "main");
	private static final Identifier TEXTURE = Eam.id("textures/entity/ghost_horse/ghost_horse.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/ghost_horse/ghost_horse_eyes.png"));

	public GhostHorseRenderer(EntityRendererProvider.Context context) {
		super(context, new HorseModel(context.bakeLayer(LAYER)), new HorseModel(context.bakeLayer(LAYER)));
		addLayer(new Eyes(this));
		addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY,
				state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
	}

	@Override
	public Identifier getTextureLocation(EquineRenderState state) {
		return TEXTURE;
	}

	@Override
	public EquineRenderState createRenderState() {
		return new EquineRenderState();
	}

	/** The horse's parts and poses, thinner; the boxes match {@code tools/ghost-horse/ghost_horse_textures.py}. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 33).addBox(-4.0F, -8.0F, -17.0F, 8.0F, 9.0F, 22.0F), PartPose.offset(0.0F, 11.0F, 5.0F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 4).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 14.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition headParts = root.addOrReplaceChild("head_parts",
				CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -6.0F, -1.5F, 3.0F, 12.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, 4.0F, -12.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		PartDefinition head = headParts.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -11.0F, -2.0F, 5.0F, 5.0F, 7.0F), PartPose.ZERO);
		headParts.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -11.0F, 4.51F, 1.0F, 16.0F, 2.0F),
				PartPose.ZERO);
		headParts.addOrReplaceChild("upper_mouth", CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -11.0F, -7.0F, 3.0F, 4.0F, 5.0F),
				PartPose.ZERO);
		headParts.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, -7.0F, -6.0F, 2.0F, 1.0F, 4.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(50, 0).addBox(0.5F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(-2.5F, -13.0F, 4.0F, 2.0F, 3.0F, 1.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(26, 18).mirror()
				.addBox(-1.5F, -2.01F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(2.5F, 14.0F, 7.0F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(26, 18)
				.addBox(-1.5F, -2.01F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-2.5F, 14.0F, 7.0F));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(26, 18).mirror()
				.addBox(-1.5F, -2.01F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(2.5F, 14.0F, -10.0F));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(26, 18)
				.addBox(-1.5F, -2.01F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-2.5F, 14.0F, -10.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** The red eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {
		Eyes(RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EquineRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
