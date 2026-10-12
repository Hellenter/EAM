package io.github.hellenter.eam.client.mobs.crossbowman;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * A round iron kettle helmet with a wide brim and a ridge over the crown, worn on any model with an 8 pixels wide head.
 * Each kind of head registers its own layer with {@link #createLayer(float)}: the helmet sits on the top of the head,
 * which is 10 pixels up for illagers and 8 for humanoids. It is left off while the mob wears something on its head,
 * such as a raid captain's banner. Texture {@code textures/entity/elite_crossbowman/kettle_helmet.png}, 64x32.
 */
public class KettleHelmetLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S> & HeadedModel> extends RenderLayer<S, M> {
	/** On an illager's head, 10 pixels tall. */
	public static final ModelLayerLocation ILLAGER = new ModelLayerLocation(Eam.id("kettle_helmet"), "illager");
	public static final Identifier TEXTURE = Eam.id("textures/entity/elite_crossbowman/kettle_helmet.png");
	/** The top of an illager's head, in pixels above its neck. */
	public static final float ILLAGER_HEAD_TOP = -10.0F;

	private final ModelPart helmet;

	public KettleHelmetLayer(RenderLayerParent<S, M> renderer, ModelPart helmet) {
		super(renderer);
		this.helmet = helmet;
	}

	/** The helmet on a head whose top is {@code top} pixels down from the head's pivot (negative: up). */
	public static LayerDefinition createLayer(float top) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, top - 0.6F, -4.5F, 9.0F, 4.0F, 9.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(36, 0).addBox(-3.5F, top - 1.6F, -3.5F, 7.0F, 1.0F, 7.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(36, 8).addBox(-0.5F, top - 2.1F, -4.0F, 1.0F, 1.0F, 8.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("brim", CubeListBuilder.create().texOffs(0, 13).addBox(-6.5F, top + 3.0F, -6.5F, 13.0F, 1.0F, 13.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		if (state.isInvisible || !state.headItem.isEmpty() || state.wornHeadType != null) {
			return;
		}
		poseStack.pushPose();
		M parent = getParentModel();
		parent.root().translateAndRotate(poseStack);
		parent.translateToHead(poseStack);
		collector.submitModelPart(helmet, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), null);
		poseStack.popPose();
	}
}
