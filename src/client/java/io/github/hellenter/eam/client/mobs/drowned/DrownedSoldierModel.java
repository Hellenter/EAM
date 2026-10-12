package io.github.hellenter.eam.client.mobs.drowned;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrGear;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.List;

/**
 * A drowned soldier: built like a draugr but bonier, bone showing through rotted flesh, weed in his hair, in torn and
 * rusted pieces of armor silted brown and green from the bog. Every piece of every slot is in the mesh
 * ({@link DrownedSoldierGearMesh}); only the ones he wears are shown.
 */
public class DrownedSoldierModel extends HumanoidModel<DrownedSoldierRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("drowned_soldier"), "main");

	/** [slot][variant - 1] -> the parts of that piece. */
	private final List<List<List<ModelPart>>> pieces = new ArrayList<>();

	public DrownedSoldierModel(ModelPart root) {
		super(root);
		hat.visible = false;
		for (String[][][] slot : DrownedSoldierGearMesh.PIECES) {
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
		DrownedSoldierGearMesh.add(mesh.getRoot());
		return LayerDefinition.create(mesh, DrownedSoldierGearMesh.TEXTURE_WIDTH, DrownedSoldierGearMesh.TEXTURE_HEIGHT);
	}

	/** His arms are centred on their pivots like a skeleton's, so the held item moves in the way it does for one. */
	@Override
	public void translateToHand(DrownedSoldierRenderState state, HumanoidArm arm, PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		float offset = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		ModelPart part = getArm(arm);
		part.x += offset;
		part.translateAndRotate(poseStack);
		part.x -= offset;
	}

	@Override
	public void setupAnim(DrownedSoldierRenderState state) {
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
		// Waterlogged: the head lolls forward and a little aside.
		head.xRot += 0.2F;
		head.zRot += 0.08F;
	}
}
