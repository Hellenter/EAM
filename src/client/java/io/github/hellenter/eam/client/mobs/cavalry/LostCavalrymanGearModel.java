package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The heavy cavalryman's plate: the skeleton knight's armor model (closed great helm, breastplate, pauldrons, leg
 * plates, greaves, cape) in his own holed ancient metal texture, bending with his body. In the saddle his cape does not
 * hang straight down through the horse: the top rows fall short of the horse's back and the bottom one folds back to
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
			// The top two rows end a few pixels above the saddle (his hips, y 12); the bottom row and the torn strips then
			// lie nearly level (1.45 rad from the vertical in all, the tips still above the back) and lift just past level
			// at the gallop.
			capeRow(0).xRot = 0.3F + 0.1F * state.mountSpeed + flutter + 1.5F * Math.max(0.0F, -body.xRot);
			capeRow(1).xRot = 0.05F + 0.05F * state.mountSpeed;
			capeRow(2).xRot = 1.1F + 0.1F * state.mountSpeed + flutter;
			for (int i = 0; i < 3; i++) {
				capeStrip(i).xRot = 0.05F * state.mountSpeed + Mth.sin(state.ageInTicks * 0.6F - 1.7F * i) * 0.1F * state.mountSpeed;
				capeStrip(i).zRot = 0.0F;
			}
			for (int i = 0; i < 3; i++) {
				capeRow(i).zRot = 0.0F;
			}
		}
	}
}
