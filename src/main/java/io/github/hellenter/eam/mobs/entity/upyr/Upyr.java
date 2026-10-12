package io.github.hellenter.eam.mobs.entity.upyr;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The upyr: a dead villager risen from the graves of an abandoned village, grey-skinned in its burial shirt, mouth
 * dark with old blood. It crawls out at night ({@link UpyrModule}), burns in the sun like a zombie and fights with its
 * teeth: a bite drinks the victim's blood, leaving them weakened and healing the upyr by what it took. It fears
 * garlic: a player who ate some is bitten for less and not drunk from at all; black blood hurts it back.
 */
public class Upyr extends Monster {
	/** Dark red drops of blood over the victim when the upyr drinks. */
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0x7A0A0A, 1.0F);

	public Upyr(EntityType<? extends Upyr> type, Level level) {
		super(type, level);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 26.0)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** A bite, weakened by garlic and paid back by black blood; when it draws blood the upyr drinks. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		float before = target instanceof LivingEntity living ? living.getHealth() : 0.0F;
		boolean hurt = UpyrModule.wardedAttack(this, level, target, () -> super.doHurtTarget(level, target));
		if (hurt && target instanceof LivingEntity living && isAlive()) {
			drink(level, living, before);
		}
		return hurt;
	}

	private void drink(ServerLevel level, LivingEntity victim, float healthBefore) {
		UpyrSettings settings = UpyrModule.settings();
		boolean garlic = UpyrModule.hasGarlic(victim);
		float drunk = UpyrLogic.drunk(healthBefore, victim.getHealth(), settings.healFactor(), garlic);
		if (garlic) {
			// It recoils from the smell instead of drinking.
			playSound(SoundEvents.ZOMBIE_HURT, 0.6F, 0.6F);
			return;
		}
		if (settings.weaknessTicks() > 0) {
			victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, settings.weaknessTicks(), settings.weaknessLevel()), this);
		}
		if (drunk > 0.0F) {
			heal(drunk);
		}
		playSound(SoundEvents.GENERIC_DRINK.value(), 0.8F, 0.6F + random.nextFloat() * 0.1F);
		level.sendParticles(BLOOD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.7, victim.getZ(), 8,
				victim.getBbWidth() * 0.3, victim.getBbHeight() * 0.15, victim.getBbWidth() * 0.3, 0.0);
	}

	// ---- Sounds: a zombie's voice, lower and slower ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ZOMBIE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}
}
