package io.github.hellenter.eam.client.mobs.villager;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws villagers as {@link VillagerFigureModel}: arms apart, the item in hand, and armor shaped to the villager's head,
 * robe and arms. Replaces the vanilla villager renderer; clothes, hats and the
 * profession badges are vanilla's.
 */
public class VillagerFigureRenderer extends HumanoidMobRenderer<Villager, VillagerFigureRenderState, VillagerFigureModel> {
	private static final Identifier VILLAGER = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
	private static final Identifier VILLAGER_BABY = Identifier.withDefaultNamespace("textures/entity/villager/villager_baby.png");

	public VillagerFigureRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerFigureModel(context.bakeLayer(VillagerFigureModel.LAYER)),
				new VillagerFigureModel(context.bakeLayer(VillagerFigureModel.BABY_LAYER)), 0.5F, VillagerRenderer.CUSTOM_HEAD_TRANSFORMS);
		addLayer(new HumanoidArmorLayer<>(this,
				ArmorModelSet.bake(VillagerFigureModel.ARMOR, context.getModelSet(), root -> new HumanoidModel<VillagerFigureRenderState>(root)),
				context.getEquipmentRenderer()));
		addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager",
				new VillagerFigureModel(context.bakeLayer(VillagerFigureModel.NO_HAT_LAYER)),
				new VillagerFigureModel(context.bakeLayer(VillagerFigureModel.BABY_NO_HAT_LAYER))));
	}

	@Override
	public Identifier getTextureLocation(VillagerFigureRenderState state) {
		return state.isBaby ? VILLAGER_BABY : VILLAGER;
	}

	@Override
	protected float getShadowRadius(VillagerFigureRenderState state) {
		float radius = super.getShadowRadius(state);
		return state.isBaby ? radius * 0.5F : radius;
	}

	@Override
	public VillagerFigureRenderState createRenderState() {
		return new VillagerFigureRenderState();
	}

	@Override
	public void extractRenderState(Villager villager, VillagerFigureRenderState state, float partialTicks) {
		super.extractRenderState(villager, state, partialTicks);
		state.isUnhappy = villager.getUnhappyCounter() > 0;
		state.villagerData = villager.getVillagerData();
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(Villager villager, HumanoidArm arm) {
		ItemStack item = villager.getItemHeldByArm(arm);
		if (item.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
		if (usesSpearPose(item, arm, villager)) return HumanoidModel.ArmPose.SPEAR;
		if (villager.isUsingItem() && usedArm(villager) == arm) {
			if (item.is(Items.BOW)) return HumanoidModel.ArmPose.BOW_AND_ARROW;
			if (item.is(Items.CROSSBOW)) return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
		}
		return HumanoidModel.ArmPose.ITEM;
	}

	private static HumanoidArm usedArm(Villager villager) {
		return villager.getUsedItemHand() == InteractionHand.MAIN_HAND ? villager.getMainArm() : villager.getMainArm().getOpposite();
	}
}
