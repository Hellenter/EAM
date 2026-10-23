package io.github.hellenter.eam.mobs.entity.frozen;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The frozen corpse: a traveller who froze to death in the underworld's ice caves and walks again, frostbitten under a
 * crust of ice with icicles hanging off its arms. It shambles slowly and its cold touch slows whoever it hits, deeper
 * with each hit that lands before the last one wears off. Rime keeps flaking off it as it walks.
 */
public class FrozenCorpse extends Monster {
	public FrozenCorpse(EntityType<? extends FrozenCorpse> type, Level level) {
		super(type, level);
		xpReward = 6;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 18.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	public static boolean checkSpawnRules(EntityType<FrozenCorpse> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		return reason != EntitySpawnReason.NATURAL || random.nextFloat() < FrozenCorpseModule.settings().spawnChance();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** A hit that lands chills the victim: slowness, one level deeper than what the last hit left. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity victim) {
			chill(level, victim);
		}
		return hurt;
	}

	private void chill(ServerLevel level, LivingEntity victim) {
		FrozenCorpseSettings settings = FrozenCorpseModule.settings();
		if (settings.slownessTicks() <= 0) {
			return;
		}
		MobEffectInstance current = victim.getEffect(MobEffects.SLOWNESS);
		int amplifier = FrozenCorpseLogic.nextLevel(current == null ? -1 : current.getAmplifier(), settings.maxSlownessLevel());
		victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, settings.slownessTicks(), amplifier), this);
		playSound(SoundEvents.PLAYER_HURT_FREEZE, 0.7F, 0.9F + random.nextFloat() * 0.2F);
		level.sendParticles(EamParticles.FROST_SHARD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
				10 + amplifier * 4, victim.getBbWidth() * 0.35, victim.getBbHeight() * 0.25, victim.getBbWidth() * 0.35, 0.08);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Rime flaking off as it walks; looks only.
		if (level().isClientSide() && random.nextInt(walkAnimation.isMoving() ? 4 : 12) == 0) {
			level().addParticle(EamParticles.FROST_RIME, getRandomX(0.5), getY() + random.nextDouble() * getBbHeight() * 0.9,
					getRandomZ(0.5), 0.0, -0.01, 0.0);
		}
	}

	// ---- Sounds: a zombie's voice, low and hoarse, and the crackle of ice when hurt ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		playSound(SoundEvents.GLASS_HIT, 0.6F, 1.4F);
		return SoundEvents.ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		playSound(SoundEvents.GLASS_BREAK, 0.7F, 0.8F);
		return SoundEvents.ZOMBIE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}
}
