package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarGear;
import io.github.hellenter.eam.mobs.entity.duergar.RenegadeAction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
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
		poseAction(state.action, state.actionTime);
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

	/** A renegade's special attacks, each with a wind-up the player can read. */
	private void poseAction(RenegadeAction action, float t) {
		switch (action) {
			case CHARGE -> {
				// The weapon goes up over his head, then comes down hard.
				if (t < RenegadeAction.CHARGE_HIT) {
					float p = ease(t / RenegadeAction.CHARGE_HIT);
					rightArm.xRot = Mth.lerp(p, rightArm.xRot, -2.9F);
					rightArm.zRot = Mth.lerp(p, rightArm.zRot, -0.15F);
					leftArm.xRot = Mth.lerp(p, leftArm.xRot, -0.6F);
				} else {
					float p = Math.min(1.0F, (t - RenegadeAction.CHARGE_HIT) / 3.0F);
					rightArm.xRot = Mth.lerp(p, -2.9F, -0.4F);
					leftArm.xRot = -0.6F;
				}
			}
			case BOMB -> poseBomb(t);
			case ALLY_THROW -> {
				// Both arms up under his mate, a wobble under the weight, then the heave forward.
				if (t < RenegadeAction.ALLY_THROW_RELEASE) {
					float p = ease(Math.min(1.0F, t / RenegadeAction.ALLY_LIFT));
					float wobble = Mth.sin(t * 0.9F) * 0.06F * p;
					rightArm.xRot = Mth.lerp(p, rightArm.xRot, -3.0F) + wobble;
					leftArm.xRot = Mth.lerp(p, leftArm.xRot, -3.0F) - wobble;
					rightArm.zRot = 0.25F * p;
					leftArm.zRot = -0.25F * p;
				} else {
					float p = Math.min(1.0F, (t - RenegadeAction.ALLY_THROW_RELEASE) / 3.0F);
					rightArm.xRot = Mth.lerp(p, -3.0F, -1.3F);
					leftArm.xRot = Mth.lerp(p, -3.0F, -1.3F);
				}
			}
			case HELD -> {
				// Sitting on his mate's hands, arms flung out.
				float flail = Mth.sin(t * 0.8F) * 0.3F;
				rightArm.xRot = -1.2F + flail;
				leftArm.xRot = -1.2F - flail;
				rightArm.zRot = 0.9F;
				leftArm.zRot = -0.9F;
			}
			case FLYING -> {
				// Fist first at the player.
				rightArm.xRot = -2.8F;
				leftArm.xRot = -2.8F;
				rightArm.zRot = 0.1F;
				leftArm.zRot = -0.1F;
				rightLeg.xRot = 0.4F;
				leftLeg.xRot = 0.6F;
			}
			default -> {
			}
		}
	}

	/** The bomb: both hands to his chest, the flint strikes twice, he holds the hissing bomb, swings back and lobs. */
	private void poseBomb(float t) {
		if (t < RenegadeAction.BOMB_TAKE_OUT) {
			float p = ease(t / RenegadeAction.BOMB_TAKE_OUT);
			rightArm.xRot = Mth.lerp(p, rightArm.xRot, -1.1F);
			leftArm.xRot = Mth.lerp(p, leftArm.xRot, -1.1F);
			rightArm.yRot = -0.45F * p;
			leftArm.yRot = 0.45F * p;
		} else if (t < RenegadeAction.BOMB_LIT) {
			// The flint and steel strikes down across the fuse.
			float strike = t - RenegadeAction.BOMB_STRIKE;
			float flick = strike >= 0.0F && strike < 6.0F ? Math.abs(Mth.sin(strike * Mth.PI / 3.0F)) : 0.0F;
			rightArm.xRot = -1.1F;
			rightArm.yRot = -0.45F;
			leftArm.xRot = -1.35F + flick * 0.45F;
			leftArm.yRot = 0.55F;
		} else if (t < RenegadeAction.BOMB_SWING) {
			// He holds the bomb up, the fuse burning.
			float p = ease(Math.min(1.0F, (t - RenegadeAction.BOMB_LIT) / 4.0F));
			rightArm.xRot = Mth.lerp(p, -1.1F, -1.5F);
			rightArm.yRot = Mth.lerp(p, -0.45F, -0.1F);
			leftArm.xRot = Mth.lerp(p, -1.35F, -0.2F);
			leftArm.yRot = Mth.lerp(p, 0.55F, 0.0F);
		} else if (t < RenegadeAction.BOMB_THROW) {
			// The swing back over his shoulder.
			float p = ease((t - RenegadeAction.BOMB_SWING) / (RenegadeAction.BOMB_THROW - RenegadeAction.BOMB_SWING));
			rightArm.xRot = Mth.lerp(p, -1.5F, -3.1F);
			rightArm.yRot = -0.1F;
			leftArm.xRot = Mth.lerp(p, -0.2F, -0.9F);
		} else {
			// The lob.
			float p = Math.min(1.0F, (t - RenegadeAction.BOMB_THROW) / 3.0F);
			rightArm.xRot = Mth.lerp(p, -3.1F, -0.9F);
			leftArm.xRot = -0.9F;
		}
	}

	private static float ease(float p) {
		float c = Mth.clamp(p, 0.0F, 1.0F);
		return c * c * (3.0F - 2.0F * c);
	}
}
