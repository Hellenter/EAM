package io.github.hellenter.eam.content.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A knife: weak, but a sneaking hit in the target's back deals +50% damage ({@link BackstabLogic}), and animals killed
 * with it drop more ({@link KnifeModule}).
 */
public class KnifeItem extends Item {
	public KnifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public float getAttackDamageBonus(Entity victim, float damage, DamageSource damageSource) {
		if (!(damageSource.getEntity() instanceof LivingEntity attacker)) {
			return 0.0F;
		}
		float victimYaw = victim instanceof LivingEntity living ? living.yBodyRot : victim.getYRot();
		boolean behind = BackstabLogic.isBehind(victimYaw, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
		return BackstabLogic.bonusDamage(damage, attacker.isShiftKeyDown(), behind);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		long percent = Math.round((KnifeModule.settings().animalLootMultiplier() - 1.0) * 100.0);
		builder.accept(Component.translatable("item.eam.knife.animal_loot", percent).withStyle(ChatFormatting.BLUE));
	}
}
