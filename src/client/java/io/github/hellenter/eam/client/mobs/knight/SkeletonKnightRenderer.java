package io.github.hellenter.eam.client.mobs.knight;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnight;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The skeleton knight has the Fallen Lord's build and moves: his model and animations (the knight's actions play the
 * Lord's ones of the same name), with the knight's own armor ({@link SkeletonKnightGearLayer}). The ancient metal armor
 * items he wears give his stats and are not drawn.
 */
public class SkeletonKnightRenderer extends HumanoidMobRenderer<SkeletonKnight, FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

	public SkeletonKnightRenderer(EntityRendererProvider.Context context) {
		super(context, new FallenLordModel(context.bakeLayer(FallenLordModel.LAYER)), 0.6F);
		addLayer(new SkeletonKnightGearLayer(this, context.getModelSet()));
	}

	@Override
	public Identifier getTextureLocation(FallenLordRenderState state) {
		return TEXTURE;
	}

	@Override
	public FallenLordRenderState createRenderState() {
		return new FallenLordRenderState();
	}

	@Override
	public void extractRenderState(SkeletonKnight knight, FallenLordRenderState state, float partialTicks) {
		super.extractRenderState(knight, state, partialTicks);
		state.isAggressive = false;
		state.isShaking = false;
		state.isHoldingBow = false;
		// Phase two: the Lord's sword animations.
		state.phase = 2;
		state.action = knight.action().animation();
		state.actionTime = knight.actionTime(partialTicks);
		state.guard = knight.guard(partialTicks);
		state.shieldUp = state.guard > 0.0F;
	}

	/** The shield is not a vanilla block pose: {@link FallenLordModel} raises it into the guard. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(SkeletonKnight knight, HumanoidArm arm) {
		return knight.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
