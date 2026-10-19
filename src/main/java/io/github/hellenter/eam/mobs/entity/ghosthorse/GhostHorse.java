package io.github.hellenter.eam.mobs.entity.ghosthorse;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * The ghost horse, called from the enchanted horn: a gaunt black horse with its ribs showing and red eyes. It needs no
 * saddle and carries only its master; it is faster and jumps higher than any bred horse, freezes the water under its
 * hooves and wears horse armor. It heals a little each second and drinks a share of the damage its master deals, past its
 * own health, which then runs off slowly. Killed, it leaves the horn empty. It goes back into the horn when its master
 * goes far, leaves the world or no longer carries the horn. Numbers are in {@code data/eam/mobs/ghost_horse.json}.
 */
public class GhostHorse extends AbstractHorse {
	private static final Identifier OVERFLOW = Eam.id("ghost_horse_overflow");

	public GhostHorse(EntityType<? extends GhostHorse> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		GhostHorseSettings settings = GhostHorseSettings.DEFAULT;
		return createBaseHorseAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.MOVEMENT_SPEED, GhostHorseLogic.speed(settings.speedBonus()))
				.add(Attributes.JUMP_STRENGTH, GhostHorseLogic.jumpStrength(settings.jumpBonus()));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		// Never a foal.
		return super.finalizeSpawn(level, difficulty, reason, new AgeableMob.AgeableMobGroupData(false));
	}

	@Override
	protected void randomizeAttributes(RandomSource random) {
		applySettings();
	}

	/** Its health, speed and jump from the numbers file, as it is called. */
	void applySettings() {
		GhostHorseSettings settings = GhostHorseModule.settings();
		setBase(Attributes.MAX_HEALTH, settings.health());
		setBase(Attributes.MOVEMENT_SPEED, GhostHorseLogic.speed(settings.speedBonus()));
		setBase(Attributes.JUMP_STRENGTH, GhostHorseLogic.jumpStrength(settings.jumpBonus()));
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Puts on the armor it wore in the horn; it falls off when the horse is killed. */
	void wearArmor(ItemStack armor) {
		setItemSlotAndDropWhenKilled(EquipmentSlot.BODY, armor);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new FollowMasterGoal(this));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public boolean isMaster(@Nullable LivingEntity entity) {
		return entity != null && getOwnerReference() != null && getOwnerReference().matches(entity);
	}

	/** Its master, when he is in this world. */
	public @Nullable Player master() {
		return getOwnerReference() != null && getOwnerReference().getEntity(level(), LivingEntity.class) instanceof Player player ? player : null;
	}

	/** It needs no saddle: its master rides it bareback. */
	@Override
	public boolean isSaddled() {
		return true;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return slot != EquipmentSlot.SADDLE && super.canUseSlot(slot);
	}

	/** Only its master mounts it, opens its armor slot or puts armor on it. */
	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isMaster(player)) {
			return InteractionResult.PASS;
		}
		return super.mobInteract(player, hand);
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
	public boolean canMate(Animal partner) {
		return false;
	}

	@Override
	public boolean canAgeUp() {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			if (onGround()) {
				freezeWater(level);
			}
			if (tickCount % 20 == 0 && isAlive()) {
				everySecond();
				GhostHorseModule.checkTether(level, this);
			}
		} else if (random.nextInt(3) == 0) {
			// A wisp of cold smoke trails from its hooves.
			level().addParticle(random.nextInt(4) == 0 ? ParticleTypes.SOUL : ParticleTypes.SMOKE, getRandomX(0.6), getY() + 0.1,
					getRandomZ(0.6), 0.0, 0.02, 0.0);
		}
	}

	/** Heals toward its own health, or lets the overflow from its master's blows run off. */
	private void everySecond() {
		GhostHorseSettings settings = GhostHorseModule.settings();
		float health = GhostHorseLogic.secondLater(getHealth(), settings.health(), settings.regen(), settings.overflowDecay());
		setOverflowHealth(health, settings.health());
		if (health != getHealth()) {
			setHealth(health);
		}
	}

	/** Its master dealt {@code damage}: a share of it heals the horse, past its own health. */
	void drink(float damage) {
		GhostHorseSettings settings = GhostHorseModule.settings();
		float health = GhostHorseLogic.afterMasterHit(getHealth(), settings.overflowHealth(), damage, settings.lifesteal());
		if (health > getHealth()) {
			setOverflowHealth(health, settings.health());
			setHealth(health);
		}
	}

	/** Its max health is its own plus whatever overflow it holds now, so the overflow shows on its master's hearts. */
	private void setOverflowHealth(float health, float base) {
		AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		float overflow = Math.max(0.0F, health - base);
		AttributeModifier current = maxHealth.getModifier(OVERFLOW);
		if (overflow <= 0.0F) {
			if (current != null) {
				maxHealth.removeModifier(OVERFLOW);
			}
		} else if (current == null || current.amount() != overflow) {
			maxHealth.addOrUpdateTransientModifier(new AttributeModifier(OVERFLOW, overflow, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/** Frost Walker under its hooves: still water around turns to frosted ice, which melts again on its own. */
	private void freezeWater(ServerLevel level) {
		int radius = GhostHorseModule.settings().frostRadius();
		if (radius <= 0 || (getDeltaMovement().horizontalDistanceSqr() < 1.0E-6 && tickCount % 10 != 0)) {
			return;
		}
		BlockPos center = blockPosition().below();
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
			if (pos.distToCenterSqr(getX(), pos.getY() + 0.5, getZ()) >= Mth.square(radius)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
					&& level.getBlockState(pos.above()).is(BlockTags.AIR)
					&& level.isUnobstructed(ice, pos, CollisionContext.empty())
					&& level.setBlockAndUpdate(pos, ice)) {
				level.gameEvent(this, GameEvent.BLOCK_PLACE, pos);
			}
		}
	}

	/** Killed, it leaves its horn empty. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			GhostHorseModule.horseDied(level, this);
		}
	}

	/** Called back into the horn: a puff of souls where it stood. */
	void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 24, 0.5, 0.7, 0.5, 0.03);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 12, 0.5, 0.6, 0.5, 0.01);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 1.5F, 0.7F);
		ejectPassengers();
		discard();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HORSE_HURT;
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
		return super.getVoicePitch() * 0.7F;
	}

	/** Riderless, it trots after its master when he walks off, and stops a few blocks short of him. */
	private static final class FollowMasterGoal extends Goal {
		private final GhostHorse horse;
		private @Nullable Player master;
		private int repath;

		FollowMasterGoal(GhostHorse horse) {
			this.horse = horse;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (horse.isVehicle()) {
				return false;
			}
			Player player = horse.master();
			double distance = GhostHorseModule.settings().followDistance();
			if (player == null || player.isSpectator() || player.isPassenger() || horse.distanceToSqr(player) < distance * distance) {
				return false;
			}
			master = player;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return master != null && master.isAlive() && !horse.isVehicle() && !horse.getNavigation().isDone()
					&& horse.distanceToSqr(master) > 9.0;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void stop() {
			master = null;
			horse.getNavigation().stop();
		}

		@Override
		public void tick() {
			if (master == null) {
				return;
			}
			horse.getLookControl().setLookAt(master, 10.0F, horse.getMaxHeadXRot());
			if (--repath <= 0) {
				repath = 10;
				horse.getNavigation().moveTo(master, horse.distanceToSqr(master) > 144.0 ? 1.6 : 1.1);
			}
		}
	}
}
