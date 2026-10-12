package io.github.hellenter.eam.client.mobs.draugr;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrGear;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.List;

/**
 * A draugr: a gaunt, mummified man, thinner than a zombie but not yet a skeleton, with long lank hair, bared teeth and
 * the cold blue gleam of his eyes, in rags and whatever pieces of armor he still wears. Every piece of every slot is in
 * the mesh ({@link DraugrGearMesh}); only the ones he wears are shown. Climbing out of the floor he reaches up with both arms.
 */
public class DraugrModel extends HumanoidModel<DraugrRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("draugr"), "main");

	/** [slot][variant - 1] -> the parts of that piece. */
	private final List<List<List<ModelPart>>> pieces = new ArrayList<>();

	public DraugrModel(ModelPart root) {
		super(root);
		hat.visible = false;
		for (String[][][] slot : DraugrGearMesh.PIECES) {
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
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		DraugrGearMesh.add(mesh.getRoot());
		return LayerDefinition.create(mesh, DraugrGearMesh.TEXTURE_WIDTH, DraugrGearMesh.TEXTURE_HEIGHT);
	}

	/** His arms are centred on their pivots like a skeleton's, so the held item moves in the way it does for one. */
	@Override
	public void translateToHand(DraugrRenderState state, HumanoidArm arm, PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		float offset = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		ModelPart part = getArm(arm);
		part.x += offset;
		part.translateAndRotate(poseStack);
		part.x -= offset;
	}

	@Override
	public void setupAnim(DraugrRenderState state) {
		super.setupAnim(state);
		DraugrGear.Slot[] slots = DraugrGear.Slot.values();
		for (int s = 0; s < pieces.size(); s++) {
			int worn = DraugrGear.piece(state.gear, slots[s]);
			List<List<ModelPart>> variants = pieces.get(s);
			for (int v = 0; v < variants.size(); v++) {
				for (ModelPart part : variants.get(v)) {
					part.visible = worn == v + 1;
				}
			}
		}
		// A dead man's stoop: the head hangs forward a little.
		head.xRot += 0.15F;
		if (state.rise < 1.0F) {
			// Buried he stands stiff with arms at his sides; rising, he reaches up out of the floor, then pulls himself free.
			float reach = state.rise <= 0.0F ? 0.0F : Mth.sin(Mth.clamp(state.rise * 1.25F, 0.0F, 1.0F) * Mth.PI);
			float arms = -2.7F * reach;
			rightArm.xRot = arms - 0.1F;
			leftArm.xRot = arms + 0.1F;
			rightArm.zRot = 0.15F * reach;
			leftArm.zRot = -0.15F * reach;
			rightArm.yRot = 0.0F;
			leftArm.yRot = 0.0F;
			rightLeg.xRot = 0.0F;
			leftLeg.xRot = 0.0F;
			head.xRot = state.rise <= 0.0F ? 0.25F : -0.5F * reach;
		}
	}
}
