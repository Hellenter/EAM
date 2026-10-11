package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The heavy cavalryman's plate: the skeleton knight's armor model (closed great helm, breastplate, pauldrons, leg
 * plates, greaves, cape) in his own holed ancient metal texture, bending with his body. His cape streams out behind
 * him at the gallop.
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
			cape.xRot = 0.15F + 1.0F * state.mountSpeed + flutter + 1.5F * Math.max(0.0F, -body.xRot);
			capeLower.xRot = 0.1F + 0.4F * state.mountSpeed + flutter;
		}
	}
}
