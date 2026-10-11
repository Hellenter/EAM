package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.client.mobs.lost.LostModel;

import net.minecraft.client.model.geom.ModelPart;

/** The heavy cavalryman's body: one of the Lost, with his seat in the saddle and his own moves ({@link CavalryPoses}). */
public class LostCavalrymanModel extends LostModel {
	public LostCavalrymanModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		CavalryPoses.apply(this, state);
	}
}
