package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.Set;

/**
 * The Fallen Lord's own armor over the skeleton: a barbute with a T-shaped face opening, a smooth mithril breastplate,
 * pauldrons, chainmail bracers on the forearms, leg plates, greaves and a torn red cape, plus the bones of his limbs.
 * In {@code fallen_lord_gear.png} (128x64) the top left holds the two limb bones (upper and lower segment), then the
 * vanilla armor layout; the top right the leggings layout; the bottom the helmet, bracers and cape.
 */
public class FallenLordGearModel extends FallenLordModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("fallen_lord"), "gear");
	private static final CubeDeformation OUTER = new CubeDeformation(1.0F);
	private static final CubeDeformation INNER = new CubeDeformation(0.5F);

	protected final ModelPart cape;
	protected final ModelPart capeLower;

	public FallenLordGearModel(ModelPart root) {
		super(root);
		cape = body.getChild("cape");
		capeLower = cape.getChild("cape_lower");
	}

	public static LayerDefinition createLayer() {
		return LayerDefinition.create(createMesh(), 128, 64).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	/** His armor's parts, unscaled, for a mob that wears it with parts of its own added. */
	public static MeshDefinition createMesh() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Shell down to the neck, its flared rim, a cap rounding the dome, the crest over it and the nasal down the face.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F)
				.texOffs(40, 32).addBox(-5.0F, 1.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.5F, 0.0F, 0.5F))
				.texOffs(90, 44).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 1.0F, 8.0F)
				.texOffs(84, 32).addBox(-0.5F, -11.0F, -4.0F, 1.0F, 1.0F, 8.0F)
				.texOffs(108, 32).addBox(-0.5F, -9.0F, -6.0F, 1.0F, 6.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

		// Breastplate, then the belt and tassets on the smaller leggings box under it.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, OUTER)
				.texOffs(80, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, INNER), PartPose.ZERO);
		// The cape: two hanging panels, only their broad faces drawn; the texture tears the outline.
		Set<Direction> sheet = EnumSet.of(Direction.NORTH, Direction.SOUTH);
		PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(0, 52).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 10.0F, 1.0F, sheet), PartPose.offset(0.0F, -0.5F, 3.1F));
		cape.addOrReplaceChild("cape_lower", CubeListBuilder.create()
				.texOffs(60, 51).addBox(-7.0F, 0.0F, 0.0F, 14.0F, 12.0F, 1.0F, sheet), PartPose.offset(0.0F, 10.0F, 0.0F));

		// Bones of the limbs (two shared textures, upper and lower segment), then the armor over them: pauldrons on the
		// vanilla armor arm box, chainmail around the forearm, leg plates on the thighs, greaves on the shins.
		CubeDeformation bracer = new CubeDeformation(0.5F);
		CubeDeformation legPlates = new CubeDeformation(0.4F);
		CubeDeformation greaves = new CubeDeformation(0.9F);
		addLimbs(root,
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER),
				CubeListBuilder.create()
						.texOffs(12, 0).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(48, 52).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, bracer),
				CubeListBuilder.create().mirror()
						.texOffs(0, 0).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, OUTER),
				CubeListBuilder.create().mirror()
						.texOffs(12, 0).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(48, 52).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, bracer),
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(64, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legPlates),
				CubeListBuilder.create()
						.texOffs(12, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, greaves),
				CubeListBuilder.create().mirror()
						.texOffs(0, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(64, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, legPlates),
				CubeListBuilder.create().mirror()
						.texOffs(12, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
						.texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, greaves));
		return mesh;
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float sway = Mth.sin(state.ageInTicks * 0.07F) * 0.03F;
		float wind = switch (state.action) {
			case DASH, SWORD_DASH -> 0.7F;
			case RETREAT -> 0.5F * Mth.sin((float) Math.PI * Mth.clamp(state.actionTime / FallenLordAction.RETREAT.length(), 0.0F, 1.0F));
			case LUNGE -> 0.4F * Mth.sin((float) Math.PI * Mth.clamp(state.actionTime / FallenLordAction.LUNGE.length(), 0.0F, 1.0F));
			case WHIRL -> 0.9F * Mth.sin((float) Math.PI * Mth.clamp(state.actionTime / FallenLordAction.WHIRL.length(), 0.0F, 1.0F));
			default -> 0.0F;
		};
		// Hangs a little off his back and lifts as he moves; when he leans back it swings out, not into his legs.
		cape.xRot = 0.08F + 0.9F * walk + wind + sway + 1.5F * Math.max(0.0F, -body.xRot);
		capeLower.xRot = 0.06F + 0.35F * walk + 0.5F * wind + sway;
	}
}
