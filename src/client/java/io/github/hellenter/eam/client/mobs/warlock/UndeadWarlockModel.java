package io.github.hellenter.eam.client.mobs.warlock;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.warlock.WarlockSpell;

import net.minecraft.client.model.EntityModel;
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
 * The undead warlock: one of the Lost, thin-limbed and dried up, in a black robe torn at the hem and the wide sleeves,
 * chainmail showing at the chest and arms, a hood with only his eyes in it and a long tattered cloak hanging from a
 * mantle over the shoulders. The cloak and the skirt of the robe sway as he walks. Casting, he points his right hand
 * at the target for the missiles, thrusts both palms out for the gust, raises both arms for the buff and spreads them
 * wide for the bats.
 */
public class UndeadWarlockModel extends EntityModel<UndeadWarlockRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("undead_warlock"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart skirt;
	private final ModelPart cloak;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public UndeadWarlockModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		skirt = body.getChild("skirt");
		cloak = body.getChild("cloak");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.ZERO);
		// The hood, open in front, a little forward of the face so it stays in shadow.
		head.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(32, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.7F)), PartPose.offset(0.0F, -0.2F, -0.4F));
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
				.addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				// The mantle over the shoulders the cloak hangs from.
				.texOffs(44, 32).addBox(-5.0F, -0.5F, -3.0F, 10.0F, 3.0F, 6.0F), PartPose.ZERO);
		body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 32).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 11.0F, 5.0F),
				PartPose.offset(0.0F, 10.0F, 0.0F));
		body.addOrReplaceChild("cloak", CubeListBuilder.create().texOffs(64, 0).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 22.0F, 1.0F),
				PartPose.offset(0.0F, 0.5F, 2.8F));
		CubeListBuilder arm = CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
				.texOffs(28, 32).addBox(-2.5F, -2.5F, -2.0F, 4.0F, 7.0F, 4.0F);
		CubeListBuilder leftArm = CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 12.0F, 3.0F)
				.texOffs(28, 32).addBox(-1.5F, -2.5F, -2.0F, 4.0F, 7.0F, 4.0F);
		root.addOrReplaceChild("right_arm", arm, PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", leftArm, PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
				PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
				PartPose.offset(1.9F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(UndeadWarlockRenderState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float swing = Mth.cos(walkPos * 0.6662F);
		float age = state.ageInTicks;

		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		rightLeg.xRot = swing * 1.0F * walk;
		leftLeg.xRot = -swing * 1.0F * walk;
		// Arms hang a little forward, swaying slowly; they swing less than a living man's.
		rightArm.xRot = -0.12F - swing * 0.45F * walk;
		leftArm.xRot = -0.12F + swing * 0.45F * walk;
		rightArm.yRot = 0.0F;
		leftArm.yRot = 0.0F;
		rightArm.zRot = 0.06F + Mth.cos(age * 0.07F) * 0.03F;
		leftArm.zRot = -0.06F - Mth.cos(age * 0.07F) * 0.03F;
		// The skirt follows the stride, the cloak streams back as he walks.
		skirt.xRot = Math.abs(swing) * 0.12F * walk;
		cloak.xRot = 0.06F + walk * 0.55F + Mth.sin(age * 0.09F) * 0.03F + Math.abs(Mth.sin(walkPos * 0.6662F)) * 0.1F * walk;
		cloak.zRot = Mth.sin(age * 0.05F) * 0.02F;

		WarlockSpell spell = state.spell;
		if (spell == WarlockSpell.NONE) {
			return;
		}
		float t = state.castTime;
		float up = smooth(t / Math.max(1, spell.release()));
		// A tremor in the hands while the spell gathers.
		float tremor = Mth.sin(t * 2.3F) * 0.04F * (1.0F - up * 0.5F);
		switch (spell) {
			case MISSILES -> {
				// The right hand points at the target, the left hangs back.
				rightArm.xRot = Mth.lerp(up, -0.12F, -1.55F) + tremor + head.xRot * up;
				rightArm.yRot = head.yRot * up;
				leftArm.xRot = Mth.lerp(up, -0.12F, 0.35F);
				leftArm.zRot = -0.25F * up;
				float recoil = t >= spell.release() ? Math.max(0.0F, Mth.cos((t - spell.release()) * Mth.PI / 2.0F)) * 0.15F : 0.0F;
				rightArm.xRot += recoil;
			}
			case WIND -> {
				// Both arms drawn back, then thrust out, palms first.
				float thrust = t < spell.release() ? 0.0F : smooth((t - spell.release()) / 3.0F);
				float arms = Mth.lerp(up, -0.12F, 0.5F) + thrust * -2.1F;
				rightArm.xRot = arms + tremor;
				leftArm.xRot = arms - tremor;
				rightArm.yRot = -0.25F * thrust;
				leftArm.yRot = 0.25F * thrust;
				cloak.xRot += 0.35F * thrust;
			}
			case BUFF -> {
				rightArm.xRot = Mth.lerp(up, -0.12F, -2.85F) + tremor;
				leftArm.xRot = Mth.lerp(up, -0.12F, -2.85F) - tremor;
				rightArm.zRot = 0.25F * up;
				leftArm.zRot = -0.25F * up;
				head.xRot -= 0.35F * up;
			}
			case BATS -> {
				rightArm.xRot = Mth.lerp(up, -0.12F, -2.0F) + tremor;
				leftArm.xRot = Mth.lerp(up, -0.12F, -2.0F) - tremor;
				rightArm.zRot = 1.05F * up;
				leftArm.zRot = -1.05F * up;
				head.xRot -= 0.6F * up;
				cloak.xRot += 0.2F * up;
			}
			case NONE -> {
			}
		}
	}

	private static float smooth(float x) {
		float c = Mth.clamp(x, 0.0F, 1.0F);
		return c * c * (3.0F - 2.0F * c);
	}
}
