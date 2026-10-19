package io.github.hellenter.eam.mobs.entity.boar;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An underground boar: a brown wild pig with tusks, a bit bigger than a pig and as tough as a cow. It grazes the
 * underworld's lake caves and forests and leaves players alone. Hit one and it goes for you, and the grown boars of its
 * herd nearby join in; piglets only run. It gives up when you get away. Breeds and is tempted with {@link BoarModule#FOOD}.
 */
public class Boar extends Animal {
	public Boar(EntityType<? extends Boar> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ATTACK_DAMAGE, BoarSettings.DEFAULT.damage())
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Natural spawns: on solid ground, and only now and then so a herd does not turn up behind every corner. */
	public static boolean checkBoarSpawnRules(EntityType<Boar> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		return reason != EntitySpawnReason.NATURAL || random.nextFloat() < BoarModule.settings().spawnChance();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.25) {
			@Override
			public boolean canUse() {
				// Grown boars fight back; only piglets, and boars on fire, run.
				return (isBaby() || isOnFire()) && super.canUse();
			}
		});
		goalSelector.addGoal(2, new MeleeAttackGoal(this, BoarModule.settings().chaseSpeed(), true) {
			@Override
			public boolean canUse() {
				return !isBaby() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isBaby() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// Datapacks may change the damage, so it is read on every hit.
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BoarModule.settings().damage());
		return super.doHurtTarget(level, target);
	}

	@Override
	protected float getKnockback(Entity target, DamageSource damageSource) {
		return BoarModule.settings().knockback();
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target != null && getTarget() == null && !isBaby()) {
			playSound(SoundEvents.HOGLIN_ANGRY, 1.0F, getVoicePitch());
		}
		super.setTarget(target);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(BoarModule.FOOD);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return EamEntities.BOAR.create(level, EntitySpawnReason.BREEDING);
	}

	// Hoglin grunts, a little higher: a boar is smaller than a hoglin.

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HOGLIN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HOGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HOGLIN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		playSound(SoundEvents.HOGLIN_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void playAttackSound() {
		playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, getVoicePitch());
	}

	@Override
	protected float getSoundVolume() {
		return 0.8F;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.15F;
	}
}
