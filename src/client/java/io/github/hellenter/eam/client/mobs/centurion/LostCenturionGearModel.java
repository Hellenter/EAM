package io.github.hellenter.eam.client.mobs.centurion;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
 * The centurion's gear: the skeleton knight's plate in his own battered ancient metal texture, without the cape, a
 * tall red crest along the helm and a goat horn at his belt; in his right hand a long spear with a leaf-shaped head and
 * a torn banner under it, which flutters; while he blows the horn it is in his left hand at his mouth. The spear
 * replaces the iron spear item he holds, which is not drawn. His parts sit in the free top rows of
 * {@code lost_centurion_gear.png}, which otherwise has the knight's layout.
 */
public class LostCenturionGearModel extends FallenLordGearModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("lost_centurion"), "gear");
	/** Where the spear's grip sits on the forearm: in the fist at its end. */
	private static final float GRIP = 5.5F;

	private final ModelPart flag;
	private final ModelPart flagLower;
	private final ModelPart heldHorn;
	private final ModelPart beltHorn;

	public LostCenturionGearModel(ModelPart root) {
		super(root);
		ModelPart spear = rightForearm.getChild("spear");
		flag = spear.getChild("flag");
		flagLower = flag.getChild("flag_lower");
		heldHorn = leftForearm.getChild("horn");
		beltHorn = body.getChild("belt_horn");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = createMesh();
		PartDefinition root = mesh.getRoot();
		// The crest along the top of the helm, its tail falling down the back.
		root.getChild("head").addOrReplaceChild("crest", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0F, -14.0F, -4.5F, 2.0F, 4.0F, 9.0F)
				.texOffs(24, 0).addBox(-1.0F, -12.0F, 4.5F, 2.0F, 4.0F, 2.0F), PartPose.ZERO);

		// The spear, built along its own y axis and turned a quarter so it points ahead with the forearm hanging, as a
		// held spear item does: the head at -y, the butt at +y; its own +z is up.
		PartDefinition spear = root.getChild("right_arm").getChild("right_forearm").addOrReplaceChild("spear", CubeListBuilder.create()
				.texOffs(124, 0).addBox(-0.5F, -25.0F, -0.5F, 1.0F, 38.0F, 1.0F)
				.texOffs(50, 0).addBox(-1.0F, -26.0F, -1.0F, 2.0F, 2.0F, 2.0F)
				.texOffs(34, 0).addBox(-0.5F, -32.0F, -1.5F, 1.0F, 7.0F, 3.0F)
				.texOffs(44, 0).addBox(-0.5F, -34.0F, -0.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(60, 0).addBox(-1.0F, 13.0F, -1.0F, 2.0F, 1.0F, 2.0F),
				PartPose.offsetAndRotation(-0.5F, GRIP, 0.0F, Mth.HALF_PI, 0.0F, 0.0F));
		// The banner hangs from the shaft under the head: two flat panels, only their faces drawn, the texture tears them.
		Set<Direction> sheet = EnumSet.of(Direction.EAST, Direction.WEST);
		spear.addOrReplaceChild("flag", CubeListBuilder.create()
				.texOffs(98, 0).addBox(0.0F, 0.0F, -5.0F, 0.0F, 14.0F, 5.0F, sheet), PartPose.offset(0.0F, -23.0F, 0.0F))
				.addOrReplaceChild("flag_lower", CubeListBuilder.create()
						.texOffs(108, 0).addBox(0.0F, 0.0F, -7.0F, 0.0F, 14.0F, 7.0F, sheet), PartPose.offset(0.0F, 0.0F, -5.0F));

		// The horn, mouthpiece to bell along its y axis: at his mouth in the left hand, or hanging at his belt.
		CubeListBuilder horn = CubeListBuilder.create()
				.texOffs(70, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(76, 0).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(86, 0).addBox(-1.5F, 6.0F, -1.5F, 3.0F, 3.0F, 3.0F);
		// The mouthpiece at his lips with the arm in the horn pose (CenturionPoses), the bell up and out ahead of him.
		root.getChild("left_arm").getChild("left_forearm").addOrReplaceChild("horn", horn,
				PartPose.offsetAndRotation(0.6F, 6.7F, -2.0F, 1.11F, 0.0F, -1.09F));
		root.getChild("body").addOrReplaceChild("belt_horn", horn,
				PartPose.offsetAndRotation(5.3F, 9.0F, -0.5F, 0.15F, 0.0F, -0.35F));
		return LayerDefinition.create(mesh, 128, 64).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		CenturionPoses.apply(this, state);
		cape.visible = false;
		boolean blowing = CenturionPoses.horn(state) > 0.5F;
		heldHorn.visible = blowing;
		beltHorn.visible = !blowing;
		// The banner streams sideways off the shaft and ripples, harder as he walks; the lower panel trails the upper.
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float time = state.ageInTicks * (0.2F + 0.15F * walk);
		flag.yRot = Mth.sin(time) * (0.15F + 0.15F * walk);
		flagLower.yRot = Mth.sin(time - 1.1F) * (0.25F + 0.25F * walk);
	}
}
