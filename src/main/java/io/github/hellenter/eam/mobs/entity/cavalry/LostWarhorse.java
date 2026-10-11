package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * A heavy cavalryman's horse: a dried-up undead horse, half husk and half skeleton, in plate barding over chainmail.
 * The barding takes {@code horse_armor} of weapon and projectile damage off; killing the horse unhorses its rider. Its
 * rider steers it; nobody else can ride or tame it.
 */
public class LostWarhorse extends AbstractHorse {
	public LostWarhorse(EntityType<? extends LostWarhorse> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes()
				.add(Attributes.MAX_HEALTH, LostCavalrySettings.Body.DEFAULT.horseHealth())
				.add(Attributes.MOVEMENT_SPEED, LostCavalryLogic.speedOfPlayerWalk(LostCavalrySettings.Speed.DEFAULT.gallop()))
				.add(Attributes.JUMP_STRENGTH, 0.6)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		// Never a foal.
		return super.finalizeSpawn(level, difficulty, reason, new AgeableMob.AgeableMobGroupData(false));
	}

	@Override
	protected void randomizeAttributes(RandomSource random) {
		LostCavalrySettings settings = LostCavalryModule.settings();
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(settings.body().horseHealth());
			setHealth(getMaxHealth());
		}
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(LostCavalryLogic.speedOfPlayerWalk(settings.speed().gallop()));
		}
	}

	@Override
	protected void addBehaviourGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		// Riderless it shies from blows like any horse; under its rider it does not.
		goalSelector.addGoal(1, new PanicGoal(this, 1.2) {
			@Override
			protected boolean shouldPanic() {
				return !isMobControlled() && super.shouldPanic();
			}
		});
	}

	/** Its rider steers it, and it does not shy from blows while he does. */
	@Override
	public boolean isMobControlled() {
		return getFirstPassenger() instanceof Mob;
	}

	/** It carries its rider only: no player can mount, feed or tame it. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public boolean canFallInLove() {
		return false;
	}

	@Override
	public boolean canAgeUp() {
		return false;
	}

	@Override
	protected boolean canPerformRearing() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	/** With its rider it stays while he does; riderless it despawns like any horse. */
	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !isVehicle();
	}

	/** The barding takes most of a weapon's or an arrow's force. */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		if (source.is(FallenLordModule.PHYSICAL)) {
			damage *= 1.0F - LostCavalryModule.settings().body().horseArmor();
		}
		super.actuallyHurt(level, source, damage);
	}

	/** Its rider is thrown off as it falls, not when the body vanishes a second later. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		ejectPassengers();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ZOMBIE_HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_HORSE_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getAngrySound() {
		return SoundEvents.ZOMBIE_HORSE_ANGRY;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.8F;
	}
}
