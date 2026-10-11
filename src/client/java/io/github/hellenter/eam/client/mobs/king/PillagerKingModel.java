package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.king.KingAction;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Pillager King in closed black plate: a great helm with an eye slit and a crest, a broad breastplate over a skirt
 * of plates, big pauldrons and gauntlet cuffs; diamond trim runs along the edges (painted in the texture). He holds an
 * axe in each hand. His attacks are posed here from the action and the ticks into it; the turns of the whirl and the
 * tornado are the renderer's.
 */
public class PillagerKingModel extends HumanoidModel<PillagerKingRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("pillager_king"), "main");
	/** Arms straight out to the sides. */
	private static final float ARMS_OUT = 1.45F;

	public PillagerKingModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation plate = new CubeDeformation(0.25F);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, plate), PartPose.ZERO);
		// The crest along the top of the helm.
		head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -10.0F, -4.0F, 2.0F, 2.0F, 8.0F),
				PartPose.ZERO);
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
				.addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F)
				// The skirt of plates over the hips.
				.texOffs(0, 34).addBox(-5.0F, 10.0F, -3.0F, 10.0F, 4.0F, 6.0F), PartPose.ZERO);
		root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 16)
				.addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, plate)
				.texOffs(64, 0).addBox(-4.0F, -3.0F, -3.0F, 6.0F, 4.0F, 6.0F)
				.texOffs(92, 16).addBox(-3.5F, 5.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offset(-5.5F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 16)
				.addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, plate)
				.texOffs(88, 0).addBox(-2.0F, -3.0F, -3.0F, 6.0F, 4.0F, 6.0F)
				.texOffs(92, 24).addBox(-1.5F, 5.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offset(5.5F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, plate), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(76, 16)
				.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, plate), PartPose.offset(2.0F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(PillagerKingRenderState state) {
		super.setupAnim(state);
		float t = state.actionTime;
		float restR = rightArm.xRot;
		float restL = leftArm.xRot;
		float restRz = rightArm.zRot;
		float restLz = leftArm.zRot;
		switch (state.action) {
			case SLASH -> {
				// Raised high, then one cut down across the body.
				rightArm.xRot = keys(t, new float[] {0, 5, 7, 10, 14}, new float[] {restR, -2.8F, -0.3F, -0.3F, restR});
				rightArm.yRot = keys(t, new float[] {0, 5, 7, 10, 14}, new float[] {0.0F, -0.3F, 0.45F, 0.3F, 0.0F});
				body.yRot = keys(t, new float[] {0, 5, 7, 10, 14}, new float[] {0.0F, -0.25F, 0.35F, 0.3F, 0.0F});
			}
			case COMBO -> {
				// Both axes up, then four cuts, right and left in turn.
				rightArm.xRot = keys(t, new float[] {0, 7, 8, 10, 13, 20, 22, 25, 32},
						new float[] {restR, -2.6F, -2.6F, -0.2F, -0.6F, -2.6F, -0.2F, -0.4F, restR});
				leftArm.xRot = keys(t, new float[] {0, 7, 14, 16, 19, 26, 28, 31, 34},
						new float[] {restL, -2.6F, -2.6F, -0.2F, -0.6F, -2.6F, -0.2F, -0.4F, restL});
				body.yRot = keys(t, new float[] {0, 8, 10, 16, 22, 28, 34}, new float[] {0.0F, 0.0F, 0.3F, -0.3F, 0.3F, -0.3F, 0.0F});
				rightArm.yRot = body.yRot;
				leftArm.yRot = body.yRot;
			}
			case WHIRL_WINDUP -> {
				// Axes spread wide, the body wound up for the turn, feet apart.
				armsOut(keys(t, new float[] {0, 6, 16}, new float[] {0.0F, 1.0F, 1.0F}), restR, restL, restRz, restLz);
				body.yRot = keys(t, new float[] {0, 6, 16}, new float[] {0.0F, -0.6F, -0.75F});
				stance(keys(t, new float[] {0, 6, 16}, new float[] {0.0F, 1.0F, 1.0F}));
			}
			case WHIRL -> {
				armsOut(keys(t, new float[] {0, 9, 12}, new float[] {1.0F, 1.0F, 0.0F}), restR, restL, restRz, restLz);
				stance(keys(t, new float[] {0, 9, 12}, new float[] {1.0F, 1.0F, 0.0F}));
			}
			case TORNADO -> {
				float out = keys(t, new float[] {0, 6, 38, 42}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				armsOut(out, restR, restL, restRz, restLz);
				stance(out);
				// The axes tip up and down as he spins.
				rightArm.xRot += Mth.sin(t * 1.3F) * 0.25F * out;
				leftArm.xRot -= Mth.sin(t * 1.3F) * 0.25F * out;
			}
			case THROW -> {
				// The left axe drawn back over the shoulder and thrown.
				leftArm.xRot = keys(t, new float[] {0, 6, 8, 10, 16}, new float[] {restL, -2.9F, -1.3F, -0.9F, restL});
				body.yRot = keys(t, new float[] {0, 6, 8, 10, 16}, new float[] {0.0F, 0.45F, -0.35F, -0.3F, 0.0F});
				leftArm.yRot = body.yRot;
			}
			case STAGGER -> {
				// Thrown off balance: arms flung out, leaning back.
				float off = keys(t, new float[] {0, 3, 18, 24}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				rightArm.xRot = Mth.lerp(off, restR, -1.1F);
				leftArm.xRot = Mth.lerp(off, restL, -0.9F);
				rightArm.zRot = Mth.lerp(off, restRz, 0.9F);
				leftArm.zRot = Mth.lerp(off, restLz, -0.9F);
				body.xRot = -0.2F * off;
				head.xRot -= 0.45F * off;
			}
			case TOTEM -> {
				// The totem held high in the left hand.
				float up = keys(t, new float[] {0, 4, 24, 30}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				leftArm.xRot = Mth.lerp(up, restL, -2.8F);
				leftArm.zRot = Mth.lerp(up, restLz, -0.2F);
				rightArm.zRot = Mth.lerp(up, restRz, 0.6F);
				head.xRot = Mth.lerp(up, head.xRot, -0.5F);
			}
			case NONE -> {
			}
		}
	}

	/** Both arms raised straight out to the sides by {@code amount}, the axes pointing ahead. */
	private void armsOut(float amount, float restR, float restL, float restRz, float restLz) {
		rightArm.xRot = Mth.lerp(amount, restR, 0.0F);
		leftArm.xRot = Mth.lerp(amount, restL, 0.0F);
		rightArm.zRot = Mth.lerp(amount, restRz, ARMS_OUT);
		leftArm.zRot = Mth.lerp(amount, restLz, -ARMS_OUT);
	}

	/** Feet set apart by {@code amount}. */
	private void stance(float amount) {
		rightLeg.zRot += 0.2F * amount;
		leftLeg.zRot -= 0.2F * amount;
	}

	/** The value at {@code t} of a curve through the given keys, eased between them. */
	private static float keys(float t, float[] times, float[] values) {
		if (t <= times[0]) {
			return values[0];
		}
		for (int i = 1; i < times.length; i++) {
			if (t <= times[i]) {
				float x = (t - times[i - 1]) / (times[i] - times[i - 1]);
				float s = x * x * (3.0F - 2.0F * x);
				return Mth.lerp(s, values[i - 1], values[i]);
			}
		}
		return values[values.length - 1];
	}
}
