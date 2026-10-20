package io.github.hellenter.eam.mobs.entity.ghosthorse;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * The ghost horse, called from the enchanted horn: a gaunt black horse with its ribs showing and red eyes. It needs no
 * saddle and carries only its master; it is faster and jumps higher than any bred horse, freezes the water under its
 * hooves (and stands on water while it does) and wears horse armor. It heals a little each second once it has gone a few
 * seconds unhurt, and drinks a share of the damage its master deals, past its own health, which then runs off slowly. Killed, it leaves the horn empty. It goes back into the horn when its master
 * goes far, leaves the world or no longer carries the horn. Numbers are in {@code data/eam/mobs/ghost_horse.json}.
 */
public class GhostHorse extends AbstractHorse {
	private static final Identifier OVERFLOW = Eam.id("ghost_horse_overflow");
	/** Still water bears it at the very top of the block, where the ice it leaves will be, so it never stands inside it. */
	private static final VoxelShape WATER_SURFACE = Block.column(16.0, 0.0, 16.0);
	/** How many ticks ahead at its pace it freezes the water. */
	private static final double FROST_LEAD = 5.0;
	private static final ParticleOptions BLACK_MOTE = new DustParticleOptions(0x050407, 1.4F);

	private int lastHurtTick = -10000;
	/** Set while it takes a blow, so the blow does not make it rear. */
	private boolean takingBlow;

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
		if (canSimulateMovement()) {
			climbOutOfSurface();
		}
		if (isInWater()) {
			// Sunk in somehow: it rises back to stand on the water.
			setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.08, 0.0));
		}
		if (level() instanceof ServerLevel level) {
			freezeWater(level);
			if (tickCount % 20 == 0 && isAlive()) {
				everySecond();
				GhostHorseModule.checkTether(level, this);
			}
		} else {
			trail();
		}
	}

	/** At a run a few black motes trail behind it; standing, now and then a soul drifts up from its hooves. */
	private void trail() {
		double dx = getX() - xo;
		double dz = getZ() - zo;
		double speedSqr = dx * dx + dz * dz;
		if (speedSqr > 0.02) {
			int count = speedSqr > 0.1 ? 2 : 1;
			for (int i = 0; i < count; i++) {
				level().addParticle(BLACK_MOTE, getX() - dx * 2.0 + random.nextGaussian() * 0.25, getY() + 0.3 + random.nextDouble() * 0.9,
						getZ() - dz * 2.0 + random.nextGaussian() * 0.25, 0.0, 0.01, 0.0);
			}
		} else if (random.nextInt(12) == 0) {
			level().addParticle(ParticleTypes.SOUL, getRandomX(0.6), getY() + 0.1, getRandomZ(0.6), 0.0, 0.02, 0.0);
		}
	}

	/**
	 * Caught inside the top block of still water or of the ice it froze (it came down into the water, or the ice formed
	 * round its hooves), it steps up onto it. Done where its movement is decided: its rider's client, or the server.
	 */
	private void climbOutOfSurface() {
		BlockPos feet = blockPosition();
		if (getY() <= feet.getY() || !level().getBlockState(feet.above()).getCollisionShape(level(), feet.above()).isEmpty()) {
			return;
		}
		BlockState state = level().getBlockState(feet);
		boolean ice = state.is(Blocks.FROSTED_ICE) || state.is(Blocks.ICE);
		boolean water = state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource()
				&& !level().getFluidState(feet.above()).is(FluidTags.WATER);
		if (ice || water) {
			setPos(getX(), feet.getY() + 1.0, getZ());
			setDeltaMovement(getDeltaMovement().multiply(1.0, 0.0, 1.0));
			setOnGround(true);
		}
	}

	/** It stands on still water as on ground, so it never waits for the ice. */
	@Override
	public boolean canStandOnFluid(FluidState fluid) {
		return fluid.is(FluidTags.WATER);
	}

	@Override
	public VoxelShape getLiquidCollisionShape() {
		return WATER_SURFACE;
	}

	/** Hurt, it keeps all four hooves down: no rearing at a blow. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		takingBlow = true;
		try {
			return super.hurtServer(level, source, damage);
		} finally {
			takingBlow = false;
		}
	}

	@Override
	public void standIfPossible() {
		if (!takingBlow) {
			super.standIfPossible();
		}
	}

	/** A fall hurts it at most {@code max_fall_damage}. */
	@Override
	protected int calculateFallDamage(double fallDistance, float damageModifier) {
		return GhostHorseLogic.fallDamage(super.calculateFallDamage(fallDistance, damageModifier), GhostHorseModule.settings().maxFallDamage());
	}

	/** It heals only once it has gone a while unhurt. */
	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		super.actuallyHurt(level, source, damage);
		lastHurtTick = tickCount;
	}

	/** Heals toward its own health, or lets the overflow from its master's blows run off. */
	private void everySecond() {
		GhostHorseSettings settings = GhostHorseModule.settings();
		float regen = GhostHorseLogic.healsAfter(tickCount - lastHurtTick, settings.regenDelay()) ? settings.regen() : 0.0F;
		float health = GhostHorseLogic.secondLater(getHealth(), settings.health(), regen, settings.overflowDecay());
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

	/**
	 * Frost Walker under its hooves, where it is now and where it will be in a few ticks at this pace: still water around
	 * turns to frosted ice, which melts again on its own. It moves where its rider's client says, so its pace is read from
	 * where it was last tick.
	 */
	private void freezeWater(ServerLevel level) {
		int radius = GhostHorseModule.settings().frostRadius();
		if (radius <= 0) {
			return;
		}
		double dx = getX() - xo;
		double dz = getZ() - zo;
		boolean moving = dx * dx + dz * dz > 1.0E-4;
		if (!moving && tickCount % 10 != 0) {
			return;
		}
		// The block under its hooves: on water it stands a little below the top of the water block.
		int y = Mth.floor(getY() - 0.2);
		freezeDisk(level, getX(), y, getZ(), radius);
		if (moving) {
			freezeDisk(level, getX() + dx * FROST_LEAD, y, getZ() + dz * FROST_LEAD, radius);
		}
	}

	private void freezeDisk(ServerLevel level, double x, int y, double z, int radius) {
		BlockPos center = BlockPos.containing(x, y, z);
		BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
			if (pos.distToCenterSqr(x, pos.getY() + 0.5, z) >= Mth.square(radius)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).is(BlockTags.AIR)
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
