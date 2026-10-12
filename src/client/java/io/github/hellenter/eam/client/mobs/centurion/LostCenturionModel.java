package io.github.hellenter.eam.client.mobs.centurion;

import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.client.mobs.lost.LostModel;

import net.minecraft.client.model.geom.ModelPart;

/** The centurion's body: one of the Lost, raising his horn ({@link CenturionPoses}). */
public class LostCenturionModel extends LostModel {
	public LostCenturionModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		CenturionPoses.apply(this, state);
	}
}
