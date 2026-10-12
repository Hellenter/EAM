package io.github.hellenter.eam.client.mobs.crossbowman;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowman;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

/**
 * The elite crossbowman: the vanilla pillager under a gambeson with a steel cuirass, a leather apron with a belt,
 * tall boots (a slightly larger copy of the illager model) and a round kettle helmet. Before his charged shot his
 * crossbow turns red and glows.
 */
public class EliteCrossbowmanRenderer extends IllagerRenderer<EliteCrossbowman, EliteCrossbowmanRenderState> {
	public static final ModelLayerLocation OUTFIT = new ModelLayerLocation(Eam.id("elite_crossbowman"), "outfit");
	private static final Identifier PILLAGER = Identifier.withDefaultNamespace("textures/entity/illager/pillager.png");
	private static final Identifier OUTFIT_TEXTURE = Eam.id("textures/entity/elite_crossbowman/elite_crossbowman_outfit.png");
	/** The look of his crossbow while it glows red, named by the item_model component. */
	public static final Identifier RED_CROSSBOW = Eam.id("red_crossbow");

	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack redCrossbow;

	public EliteCrossbowmanRenderer(EntityRendererProvider.Context context) {
		super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
		addLayer(new Outfit(this, new IllagerModel<>(context.bakeLayer(OUTFIT))));
		addLayer(new KettleHelmetLayer<>(this, context.bakeLayer(KettleHelmetLayer.ILLAGER)));
		addLayer(new GlowingItemInHandLayer(this));
	}

	@Override
	public Identifier getTextureLocation(EliteCrossbowmanRenderState state) {
		return PILLAGER;
	}

	@Override
	public EliteCrossbowmanRenderState createRenderState() {
		return new EliteCrossbowmanRenderState();
	}

	@Override
	public void extractRenderState(EliteCrossbowman mob, EliteCrossbowmanRenderState state, float partialTicks) {
		super.extractRenderState(mob, state, partialTicks);
		state.redCharging = mob.isRedCharging();
		if (state.redCharging) {
			if (redCrossbow == null) {
				redCrossbow = new ItemStack(Items.CROSSBOW);
				redCrossbow.set(DataComponents.ITEM_MODEL, RED_CROSSBOW);
			}
			boolean right = mob.getMainHandItem().is(Items.CROSSBOW) == (mob.getMainArm() == HumanoidArm.RIGHT);
			if (right) {
				itemModelResolver.updateForLiving(state.rightHandItemState, redCrossbow, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, mob);
				state.rightHandItemStack = redCrossbow;
			} else {
				itemModelResolver.updateForLiving(state.leftHandItemState, redCrossbow, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, mob);
				state.leftHandItemStack = redCrossbow;
			}
		}
	}

	/**
	 * His outfit on the illager model a little larger: the cuirass over the body, the apron on the long coat, the
	 * gambeson sleeves and the boots. Texture 64x64 in the illager layout.
	 */
	public static LayerDefinition createOutfitLayer() {
		CubeDeformation g = new CubeDeformation(0.3F);
		// The gambeson sleeves sit close on the arm, like a shirt's.
		CubeDeformation sleeve = new CubeDeformation(0.1F);
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F, g)
				.texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.75F)), PartPose.ZERO);
		PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create()
						.texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, sleeve)
						.texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, sleeve),
				PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
		arms.addOrReplaceChild("left_shoulder", CubeListBuilder.create().texOffs(44, 22).mirror()
				.addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, sleeve), PartPose.ZERO);
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 22)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 22).mirror()
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g), PartPose.offset(2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 46)
				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, sleeve), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 46).mirror()
				.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, sleeve), PartPose.offset(5.0F, 2.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	/** The outfit, posed like the body under it. */
	private static final class Outfit extends RenderLayer<EliteCrossbowmanRenderState, IllagerModel<EliteCrossbowmanRenderState>> {
		private final IllagerModel<EliteCrossbowmanRenderState> model;

		Outfit(RenderLayerParent<EliteCrossbowmanRenderState, IllagerModel<EliteCrossbowmanRenderState>> renderer,
				IllagerModel<EliteCrossbowmanRenderState> model) {
			super(renderer);
			this.model = model;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EliteCrossbowmanRenderState state, float yRot,
				float xRot) {
			coloredCutoutModelCopyLayerRender(model, OUTFIT_TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
		}
	}

	/** His crossbow, lit up whatever the light around while it glows red. */
	private static final class GlowingItemInHandLayer extends ItemInHandLayer<EliteCrossbowmanRenderState, IllagerModel<EliteCrossbowmanRenderState>> {
		GlowingItemInHandLayer(RenderLayerParent<EliteCrossbowmanRenderState, IllagerModel<EliteCrossbowmanRenderState>> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EliteCrossbowmanRenderState state, float yRot,
				float xRot) {
			super.submit(poseStack, collector, state.redCharging ? LightCoordsUtil.FULL_BRIGHT : lightCoords, state, yRot, xRot);
		}
	}
}
