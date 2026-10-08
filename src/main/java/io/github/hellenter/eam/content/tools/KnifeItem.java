package io.github.hellenter.eam.content.tools;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/** A knife: weak, but a sneaking hit in the target's back deals +50% damage ({@link BackstabLogic}). */
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
}
