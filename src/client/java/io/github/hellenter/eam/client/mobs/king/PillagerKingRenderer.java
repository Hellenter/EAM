package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.king.KingAction;
import io.github.hellenter.eam.mobs.entity.king.PillagerKing;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

/**
 * The Pillager King, two and a half blocks tall, a long netherite axe in each hand. He spins round in the tornado, his
 * eyes glow red in the slit of the helm before the charge and yellow while he can be parried, and he holds up a totem
 * when it saves him.
 */
public class PillagerKingRenderer extends HumanoidMobRenderer<PillagerKing, PillagerKingRenderState, PillagerKingModel> {
	/** The model is 2 blocks tall. */
	private static final float SCALE = 2.5F / 2.0F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/pillager_king/pillager_king.png");
	private static final RenderType RED_EYES = RenderTypes.eyes(Eam.id("textures/entity/pillager_king/pillager_king_eyes.png"));
	private static final RenderType YELLOW_EYES = RenderTypes.eyes(Eam.id("textures/entity/pillager_king/pillager_king_parry_eyes.png"));
	/** Turns of the tornado: it speeds up over {@link #TORNADO_RAMP} ticks, spins, and slows down to end facing ahead. */
	private static final float TORNADO_RAMP = 6.0F;
	private static final float TORNADO_TURNS = 3.0F;

	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack totem;

	public PillagerKingRenderer(EntityRendererProvider.Context context) {
		super(context, new PillagerKingModel(context.bakeLayer(PillagerKingModel.LAYER)), 0.5F * SCALE);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(PillagerKingRenderState state) {
		return TEXTURE;
	}

	@Override
	public PillagerKingRenderState createRenderState() {
		return new PillagerKingRenderState();
	}

	@Override
	public void extractRenderState(PillagerKing king, PillagerKingRenderState state, float partialTicks) {
		super.extractRenderState(king, state, partialTicks);
		state.action = king.action();
		state.actionTime = king.actionTime(partialTicks);
		float t = state.actionTime;
		state.eyes = switch (state.action) {
			case CHARGE_WINDUP -> PillagerKingRenderState.Eyes.RED;
			case COMBO -> t < KingAction.COMBO_PARRY_END ? PillagerKingRenderState.Eyes.YELLOW : PillagerKingRenderState.Eyes.NONE;
			case ONSLAUGHT -> t >= KingAction.ONSLAUGHT_PARRY_START && t < KingAction.ONSLAUGHT_PARRY_END
					? PillagerKingRenderState.Eyes.YELLOW : PillagerKingRenderState.Eyes.NONE;
			default -> PillagerKingRenderState.Eyes.NONE;
		};
		state.bodyRot += spin(state.action, t);
		state.spinSpeed = Math.abs(spin(state.action, t + 0.5F) - spin(state.action, t - 0.5F));
		state.turnRate = Mth.wrapDegrees(king.yBodyRot - king.yBodyRotO);
		state.fallSpeed = (float) (king.getY() - king.yo);
		double dx = king.getX() - king.xo;
		double dz = king.getZ() - king.zo;
		state.dash = Math.clamp(((float) Math.sqrt(dx * dx + dz * dz) - 0.25F) / 0.35F, 0.0F, 1.0F);
		if (state.action == KingAction.TOTEM) {
			if (totem == null) {
				totem = new ItemStack(Items.TOTEM_OF_UNDYING);
			}
			itemModelResolver.updateForLiving(state.leftHandItemState, totem, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, king);
			state.leftHandItemStack = totem;
		}
	}

	/** Degrees he has turned on the spot so far in a tornado. */
	static float spin(KingAction action, float t) {
		return switch (action) {
			case TORNADO -> {
				float start = KingAction.TORNADO_SPIN_UP;
				float end = action.length();
				// Full speed for the middle part, ramps at both ends: the turns add up to whole circles.
				float full = 360.0F * TORNADO_TURNS / (end - start - TORNADO_RAMP);
				float x = Math.clamp(t, start, end) - start;
				float length = end - start;
				float angle;
				if (x < TORNADO_RAMP) {
					angle = full * x * x / (2.0F * TORNADO_RAMP);
				} else if (x < length - TORNADO_RAMP) {
					angle = full * (TORNADO_RAMP / 2.0F + x - TORNADO_RAMP);
				} else {
					float left = length - x;
					angle = 360.0F * TORNADO_TURNS - full * left * left / (2.0F * TORNADO_RAMP);
				}
				yield angle;
			}
			default -> 0.0F;
		};
	}

	@Override
	protected void scale(PillagerKingRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** Both hands hold an axe; his attacks are posed by the model. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(PillagerKing king, HumanoidArm arm) {
		return king.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}

	/** Eyes glowing in the slit of the helm, whatever the light around. */
	private static final class Eyes extends RenderLayer<PillagerKingRenderState, PillagerKingModel> {
		Eyes(RenderLayerParent<PillagerKingRenderState, PillagerKingModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, PillagerKingRenderState state, float yRot,
				float xRot) {
			if (state.eyes == PillagerKingRenderState.Eyes.NONE) {
				return;
			}
			RenderType type = state.eyes == PillagerKingRenderState.Eyes.RED ? RED_EYES : YELLOW_EYES;
			collector.order(1).submitModel(getParentModel(), state, poseStack, type, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
