package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarGear;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.List;

/**
 * A duergar: a grey dwarf, short and broad, bald or nearly so, with a great beard. Every beard, haircut and piece of
 * gear is in the mesh ({@link DuergarGearMesh}); only the ones he wears are shown. Every kind of duergar uses this model
 * with its own texture.
 */
public class DuergarModel extends HumanoidModel<DuergarRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("duergar"), "main");

	/** [slot][variant - 1] -> the parts of that piece. */
	private final List<List<List<ModelPart>>> pieces = new ArrayList<>();

	public DuergarModel(ModelPart root) {
		super(root);
		hat.visible = false;
		for (String[][][] slot : DuergarGearMesh.PIECES) {
			List<List<ModelPart>> variants = new ArrayList<>();
			for (String[][] piece : slot) {
				List<ModelPart> parts = new ArrayList<>();
				for (String[] part : piece) {
					parts.add(root.getChild(part[0]).getChild(part[1]));
				}
				variants.add(parts);
			}
			pieces.add(variants);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		DuergarGearMesh.add(mesh.getRoot());
		return LayerDefinition.create(mesh, DuergarGearMesh.TEXTURE_WIDTH, DuergarGearMesh.TEXTURE_HEIGHT);
	}

	/** His arms are centred on their pivots, so the held item sits in the middle of his fist. */
	@Override
	public void translateToHand(DuergarRenderState state, HumanoidArm arm, PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		float offset = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		ModelPart part = getArm(arm);
		part.x += offset;
		part.translateAndRotate(poseStack);
		part.x -= offset;
		// The item sits where a vanilla arm's fist is, ten pixels below the shoulder; his fist is three pixels higher.
		poseStack.translate(0.0F, -3.0F / 16.0F, 0.0F);
	}

	@Override
	public void setupAnim(DuergarRenderState state) {
		super.setupAnim(state);
		DuergarGear.Slot[] slots = DuergarGear.Slot.values();
		for (int s = 0; s < pieces.size(); s++) {
			int worn = DuergarGear.piece(state.gear, slots[s]);
			List<List<ModelPart>> variants = pieces.get(s);
			for (int v = 0; v < variants.size(); v++) {
				for (ModelPart part : variants.get(v)) {
					part.visible = worn == v + 1;
				}
			}
		}
	}
}
