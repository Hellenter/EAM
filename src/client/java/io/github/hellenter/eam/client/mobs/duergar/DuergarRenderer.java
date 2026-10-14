package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarLook;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarRenegade;
import io.github.hellenter.eam.mobs.entity.duergar.RenegadeAction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Any kind of duergar: the shared model with the kind's own textures {@code textures/entity/duergar/<kind>_<skin>.png};
 * the eyes ({@code <kind>_eyes.png}) glow whatever the light.
 */
public class DuergarRenderer<T extends Mob & DuergarLook> extends HumanoidMobRenderer<T, DuergarRenderState, DuergarModel> {
	private final Identifier[] textures;
	private final RenderType eyes;
	private final ItemStack bomb = new ItemStack(EamItems.DUERGAR_BOMB);
	private final ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);

	public DuergarRenderer(EntityRendererProvider.Context context, String kind, int skins) {
		super(context, new DuergarModel(context.bakeLayer(DuergarModel.LAYER)), 0.5F);
		textures = new Identifier[skins];
		for (int i = 0; i < skins; i++) {
			textures[i] = Eam.id("textures/entity/duergar/" + kind + "_" + i + ".png");
		}
		eyes = RenderTypes.eyes(Eam.id("textures/entity/duergar/" + kind + "_eyes.png"));
		addLayer(new Eyes(this, eyes));
	}

	@Override
	public Identifier getTextureLocation(DuergarRenderState state) {
		return textures[Math.floorMod(state.skin, textures.length)];
	}

	@Override
	public DuergarRenderState createRenderState() {
		return new DuergarRenderState();
	}

	@Override
	public void extractRenderState(T duergar, DuergarRenderState state, float partialTicks) {
		super.extractRenderState(duergar, state, partialTicks);
		state.gear = duergar.gear();
		state.skin = duergar.skin();
		state.action = RenegadeAction.NONE;
		if (duergar instanceof DuergarRenegade renegade) {
			state.action = renegade.action();
			state.actionTime = renegade.actionTime(partialTicks);
			if (state.action == RenegadeAction.BOMB && state.actionTime < RenegadeAction.BOMB_THROW) {
				// The bomb in his right hand, the flint and steel in his left until the fuse catches.
				ItemStack flint = state.actionTime < RenegadeAction.BOMB_LIT + 2 ? flintAndSteel : ItemStack.EMPTY;
				itemModelResolver.updateForLiving(state.rightHandItemState, bomb, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, duergar);
				itemModelResolver.updateForLiving(state.leftHandItemState, flint, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, duergar);
				state.rightHandItemStack = bomb;
				state.leftHandItemStack = flint;
			} else if (state.action == RenegadeAction.ALLY_THROW && state.actionTime < RenegadeAction.ALLY_THROW_RELEASE) {
				// Both hands under his mate: the weapon goes on his belt.
				itemModelResolver.updateForLiving(state.rightHandItemState, ItemStack.EMPTY, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, duergar);
				state.rightHandItemStack = ItemStack.EMPTY;
			}
		}
	}

	/** The two eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<DuergarRenderState, DuergarModel> {
		private final RenderType type;

		Eyes(RenderLayerParent<DuergarRenderState, DuergarModel> renderer, RenderType type) {
			super(renderer);
			this.type = type;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, DuergarRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, type, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
