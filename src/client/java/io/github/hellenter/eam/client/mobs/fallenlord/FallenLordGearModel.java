package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Fallen Lord's own armor over the skeleton: a barbute with a T-shaped face opening, a smooth mithril breastplate,
 * pauldrons, chainmail bracers on the forearms, leg plates, greaves and a torn red cloak ({@link LordCloak}), plus the
 * bones of his limbs. In {@code fallen_lord_gear.png} (128x64) the top left holds the two limb bones (upper and lower
 * segment) with the cloak to their right, then the vanilla armor layout; the right the leggings layout; the bottom the
 * helmet and bracers.
 */
public class FallenLordGearModel extends FallenLordModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("fallen_lord"), "gear");
	private static final CubeDeformation OUTER = new CubeDeformation(1.0F);
	private static final CubeDeformation INNER = new CubeDeformation(0.5F);

	protected final ModelPart cape;
	private final LordCloak cloak;

	public FallenLordGearModel(ModelPart root) {
		super(root);
		cloak = new LordCloak(body);
		cape = cloak.top();
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
		LordCloak.addTo(body);

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
		cloak.animate(state, this);
	}

	/** Row {@code index} of the cloak, top (0) to bottom (2), each hanging from the one above. */
	protected ModelPart capeRow(int index) {
		return cloak.row(index);
	}

	/** Torn strip {@code index} of the cloak's hem, left (0) to right (2), hanging from the bottom row. */
	protected ModelPart capeStrip(int index) {
		return cloak.strip(index);
	}
}
