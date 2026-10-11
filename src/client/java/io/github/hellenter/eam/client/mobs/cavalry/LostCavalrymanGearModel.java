package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The heavy cavalryman's plate: the skeleton knight's armor model (closed great helm, breastplate, pauldrons, leg
 * plates, greaves, cape) in his own holed ancient metal texture, bending with his body. In the saddle his cape does not
 * hang straight down through the horse: the upper panel falls short of the horse's back and the lower one folds back to
 * lie over it, lifting to stream out behind him at the gallop.
 */
public class LostCavalrymanGearModel extends FallenLordGearModel {
	public LostCavalrymanGearModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		CavalryPoses.apply(this, state);
		if (state.isPassenger) {
			float flutter = Mth.sin(state.ageInTicks * 0.6F) * 0.08F * state.mountSpeed;
			// The upper panel ends a few pixels above the saddle (his hips, y 12); the lower one then lies nearly level
			// (1.45 rad from the vertical in all, its tip still above the back) and lifts just past level at the gallop.
			cape.xRot = 0.35F + 0.15F * state.mountSpeed + flutter + 1.5F * Math.max(0.0F, -body.xRot);
			capeLower.xRot = 1.1F + 0.15F * state.mountSpeed + flutter;
		}
	}
}
