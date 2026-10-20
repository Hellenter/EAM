package io.github.hellenter.eam.client.mobs.ghosthorse;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.ghosthorse.GhostHorse;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
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
 * The ghost horse: a horse's frame, its hide stretched over ribs, spine and hips, a bare skull for a head with a hanging
 * jaw, a thin rat's tail and a ragged mane. Its eyes glow red whatever the light, and it wears horse armor like a horse.
 */
public class GhostHorseRenderer extends AbstractHorseRenderer<GhostHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("ghost_horse"), "main");
	private static final Identifier TEXTURE = Eam.id("textures/entity/ghost_horse/ghost_horse.png");
	/** A living horse's model scale; vanilla horse armor is drawn at it. */
	private static final float LIVING_HORSE_SCALE = 1.1F;
	/** Its hitbox is 1.85 blocks tall where a horse's is 1.6. */
	private static final float SCALE = 1.85F / 1.6F;
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/ghost_horse/ghost_horse_eyes.png"));

	public GhostHorseRenderer(EntityRendererProvider.Context context) {
		super(context, new HorseModel(context.bakeLayer(LAYER)), new HorseModel(context.bakeLayer(LAYER)));
		shadowRadius *= SCALE;
		addLayer(new Eyes(this));
		addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY,
				state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ModelLayers.HORSE_ARMOR)), null, 2));
	}

	@Override
	public Identifier getTextureLocation(EquineRenderState state) {
		return TEXTURE;
	}

	/** A quarter block taller than a horse, armor and all. */
	@Override
	protected void scale(EquineRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public EquineRenderState createRenderState() {
		return new EquineRenderState();
	}

	/**
	 * The vanilla horse's boxes and poses at a living horse's scale, so horse armor fits it as it fits a horse, with a
	 * hanging lower jaw and a thin rat's tail; texture 128x128 over the 64x64 layout, two texels to a pixel, painted by
	 * {@code tools/ghost-horse/ghost_horse_textures.py}.
	 */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = AbstractEquineModel.createBodyMesh(CubeDeformation.NONE);
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.getChild("body");
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(42, 36).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 14.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, -5.0F, 2.0F, (float) (Math.PI / 6), 0.0F, 0.0F));
		root.getChild("head_parts").addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(26, 0)
				.addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -6.0F, -2.5F, 0.3F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64).apply(MeshTransformer.scaling(LIVING_HORSE_SCALE));
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
