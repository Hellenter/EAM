package io.github.hellenter.eam.mobs.entity.lurker;

import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerLogic.Launch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A withered, mummified body in a rotten monk's robe that lives in swamps. It lies hidden inside a mud block until a
 * player comes close, then bursts out and leaps at them, swipes three times fast and leaps away again. It moves on
 * all fours and jumps far and high (5 up, 7 along by default) to close or open the distance. Undead: it burns in
 * daylight, and dives back into the ground to put the fire out or when it has no one to hunt.
 */
public class SwampLurker extends Monster {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(SwampLurker.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> DATA_HIDDEN = SynchedEntityData.defineId(SwampLurker.class, EntityDataSerializers.BOOLEAN);
	/** Hidden, it fits inside the block it lies in. */
	private static final EntityDimensions HIDDEN_DIMENSIONS = EntityDimensions.fixed(0.8F, 0.8F).withEyeHeight(0.4F);
	/** Swipes land this much past {@link SwampLurkerSettings#reach()}, for a moving target. */
	private static final double REACH_SLACK = 0.4;
	/** A leap in lands this far short of the target, so the first swipe reaches it. */
	private static final double LAND_SHORT = 1.0;

	private LurkerAction action = LurkerAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	/** Whether the current leap goes at the target, so a combo follows the landing. */
	private boolean leapingIn;
	private int attackCooldown;
	private int leapCooldown;
	private int noTargetTicks;
	private int rehideCooldown;
	/** Mirrors {@link #DATA_HIDDEN}, which is not there yet when the constructor asks for the dimensions. */
	private boolean hidden;
	/** Client: the tick count when the current action started. */
	private int clientActionStart;

	public SwampLurker(EntityType<? extends SwampLurker> type, Level level) {
		super(type, level);
		xpReward = 10;
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	private static SwampLurkerSettings settings() {
		return SwampLurkerModule.settings();
	}

	/**
	 * Natural spawns need ground it can burrow into and no other lurker nearby; light does not matter, since a lurker
	 * spawned in daylight hides at once.
	 */
	public static boolean checkSpawnRules(EntityType<SwampLurker> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		if (!level.getBlockState(pos.below()).is(SwampLurkerModule.BURROWS_IN)) {
			return false;
		}
		return level.getEntitiesOfClass(SwampLurker.class, new AABB(pos).inflate(settings().spawnSpacing())).isEmpty();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_HIDDEN, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new SwampLurkerFightGoal(this));
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
			// It spawns lying in wait.
			BlockPos below = blockPosition().below();
			if (canBurrowInto(level, below) && level.getLevel().getGameRules().get(GameRules.MOB_GRIEFING)) {
				hideIn(level, below, false);
			}
		}
		return data;
	}

	// ---- Equipment: none ----

	/** Its withered hands hold nothing and it wears no armor. */
	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return false;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	// ---- State ----

	public LurkerAction action() {
		return LurkerAction.byId(entityData.get(DATA_ACTION));
	}

	public boolean isHidden() {
		return hidden;
	}

	/** Client: ticks since the current action started, with the partial tick. */
	public float actionTime(float partialTicks) {
		return tickCount - clientActionStart + partialTicks;
	}

	boolean isBusy() {
		return action != LurkerAction.NONE;
	}

	int attackCooldown() {
		return attackCooldown;
	}

	int leapCooldown() {
		return leapCooldown;
	}

	/** It can push off for a leap from the ground or out of water. */
	boolean canLeap() {
		return onGround() || isInWater();
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			clientActionStart = tickCount;
		} else if (DATA_HIDDEN.equals(accessor)) {
			applyHidden(entityData.get(DATA_HIDDEN));
		}
	}

	private void setHidden(boolean value) {
		entityData.set(DATA_HIDDEN, value);
		applyHidden(value);
	}

	private void applyHidden(boolean value) {
		hidden = value;
		noPhysics = value;
		refreshDimensions();
	}

	@Override
	public boolean isNoGravity() {
		return hidden || super.isNoGravity();
	}

	/** Light does not keep it from spawning or walking anywhere: in daylight it simply hides. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return hidden ? HIDDEN_DIMENSIONS : super.getDefaultDimensions(pose);
	}

	// ---- Hidden: untouchable, waiting ----

	/** Hidden, it does nothing but wait for a player to come close. */
	@Override
	protected boolean isImmobile() {
		return hidden || super.isImmobile();
	}

	@Override
	public boolean isPickable() {
		return !hidden && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return !hidden && super.isPushable();
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return !hidden && super.canBeCollidedWith(other);
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !hidden && super.canBeSeenAsEnemy();
	}

	/** The mud around it takes the blows; dig it out first. Only what ignores invulnerability, like /kill, gets through. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (hidden && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** It lands from its own leaps unhurt; only a fall longer than its highest leap hurts. */
	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return super.causeFallDamage(Math.max(0.0, fallDistance - settings().leap().maxHeight()), damageModifier, source);
	}

	@Override
	public void aiStep() {
		if (hidden) {
			setDeltaMovement(Vec3.ZERO);
			if (level() instanceof ServerLevel level) {
				tickHidden(level);
			}
		}
		super.aiStep();
	}

	private void tickHidden(ServerLevel level) {
		// The AI does not run while it hides, nor does the clock that lets an idle monster despawn.
		noActionTime++;
		if (rehideCooldown > 0) {
			rehideCooldown--;
		}
		BlockPos pos = blockPosition();
		if (!level.getBlockState(pos).is(Blocks.MUD)) {
			// Dug out.
			emerge(level, null, false);
			return;
		}
		if (rehideCooldown > 0 || (tickCount + getId()) % 4 != 0) {
			return;
		}
		SwampLurkerSettings settings = settings();
		double range = Math.max(settings.emergeRange(), settings.sneakEmergeRange());
		Player player = level.getNearestPlayer(getX(), getY() + 0.5, getZ(), range, candidate -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(candidate)
				&& SwampLurkerLogic.emerges(Math.sqrt(candidate.distanceToSqr(getX(), getY() + 0.5, getZ())), candidate.isShiftKeyDown(), settings));
		if (player != null) {
			emerge(level, player, true);
		}
	}

	/** Bursts out of its mud, at {@code player} if there is one. */
	private void emerge(ServerLevel level, @Nullable Player player, boolean breakBlock) {
		BlockPos pos = blockPosition();
		if (breakBlock) {
			level.destroyBlock(pos, false, this);
		}
		setHidden(false);
		// Out of the hole first: its walls would stop the leap dead. Unless something was built on top of it.
		if (!level.getBlockState(pos.above()).isSolid()) {
			setPos(getX(), pos.getY() + 1.0, getZ());
		}
		noTargetTicks = 0;
		playSound(SoundEvents.DROWNED_AMBIENT, 1.5F, 0.6F);
		if (player != null && player.isAlive()) {
			setTarget(player);
			leapAt(player);
		} else {
			setDeltaMovement(0.0, 0.5, 0.0);
		}
	}

	static boolean canBurrowInto(ServerLevelAccessor level, BlockPos pos) {
		return level.getBlockState(pos).is(SwampLurkerModule.BURROWS_IN) && !level.getBlockState(pos.above()).isSolid();
	}

	/** Lies down inside the block at {@code pos}, which turns to mud. */
	private void hideIn(ServerLevelAccessor level, BlockPos pos, boolean effects) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.MUD)) {
			level.setBlock(pos, Blocks.MUD.defaultBlockState(), 3);
		}
		if (effects) {
			level.levelEvent(2001, pos, Block.getId(Blocks.MUD.defaultBlockState()));
		}
		getNavigation().stop();
		setPos(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
		setDeltaMovement(Vec3.ZERO);
		clearFire();
		setTarget(null);
		setHidden(true);
		rehideCooldown = settings().rehideCooldown();
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (attackCooldown > 0) attackCooldown--;
		if (leapCooldown > 0) leapCooldown--;
		LivingEntity target = getTarget();
		noTargetTicks = target != null && target.isAlive() ? 0 : noTargetTicks + 1;
		if (action == LurkerAction.NONE) {
			if (wantsToBurrow(level)) {
				startBurrow();
			}
		} else {
			tickAction(level);
		}
	}

	/** With no one to hunt for a while, or burning in the sun, it dives into the ground under it. */
	private boolean wantsToBurrow(ServerLevel level) {
		if (!onGround() || !level.getGameRules().get(GameRules.MOB_GRIEFING) || !canBurrowInto(level, blockPosition().below())) {
			return false;
		}
		boolean sunBurnt = isOnFire() && level.isBrightOutside() && level.canSeeSky(blockPosition());
		return sunBurnt || noTargetTicks >= settings().hideAfter();
	}

	private void startBurrow() {
		getNavigation().stop();
		getMoveControl().strafe(0.0F, 0.0F);
		setAction(LurkerAction.BURROW);
		playSound(SoundEvents.MUD_BREAK, 1.0F, 0.8F);
	}

	void startCombo(LivingEntity target) {
		actionTarget = target;
		getNavigation().stop();
		getMoveControl().strafe(0.0F, 0.0F);
		setAction(LurkerAction.COMBO);
		playSound(SoundEvents.DROWNED_AMBIENT, 1.0F, 1.2F);
	}

	/** A leap that lands just short of {@code target}. */
	void leapAt(LivingEntity target) {
		SwampLurkerSettings.Leap leap = settings().leap();
		double distance = Math.max(0.0, horizontalDistanceTo(target) - LAND_SHORT);
		double height = target.getY() - getY();
		Launch launch = SwampLurkerLogic.leap(distance, height, SwampLurkerLogic.arcFor(distance), leap.maxHeight(), leap.maxDistance());
		jump(horizontal(target.position().subtract(position())), launch, target, true);
	}

	/** A low leap away from {@code target}, to about the retreat distance from it, a little to one side. */
	void leapAway(LivingEntity target) {
		SwampLurkerSettings.Leap leap = settings().leap();
		double distance = Math.clamp(leap.retreatDistance() - horizontalDistanceTo(target), 2.5, leap.maxDistance());
		Vec3 away = horizontal(position().subtract(target.position()));
		if (away == Vec3.ZERO) {
			away = Vec3.directionFromRotation(0.0F, getYRot() + 180.0F);
		}
		away = away.yRot((getRandom().nextFloat() - 0.5F) * 1.2F);
		Launch launch = SwampLurkerLogic.leap(distance, 0.0, 1.2, leap.maxHeight(), leap.maxDistance());
		jump(away, launch, target, false);
	}

	private void jump(Vec3 direction, Launch launch, LivingEntity target, boolean in) {
		actionTarget = target;
		leapingIn = in;
		leapCooldown = settings().leap().cooldown();
		getNavigation().stop();
		getMoveControl().strafe(0.0F, 0.0F);
		faceTowards(target);
		setDeltaMovement(direction.x * launch.horizontal(), launch.vertical(), direction.z * launch.horizontal());
		needsSync = true;
		setAction(LurkerAction.LEAP);
		playSound(in ? SoundEvents.DROWNED_AMBIENT : SoundEvents.HUSK_STEP, 1.0F, in ? 0.8F : 0.6F);
	}

	private void setAction(LurkerAction next) {
		action = next;
		actionTicks = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		setAction(LurkerAction.NONE);
		actionTarget = null;
		leapingIn = false;
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		switch (action) {
			case LEAP -> {
				getNavigation().stop();
				if (t > 2 && (onGround() || isInWater()) || t >= action.length()) {
					landed(target);
				}
			}
			case COMBO -> tickCombo(level, t, target);
			case BURROW -> {
				getNavigation().stop();
				BlockPos below = blockPosition().below();
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)), getX(), getY() + 0.1, getZ(),
						4, 0.3, 0.05, 0.3, 0.1);
				if (t >= action.length()) {
					endAction();
					if (canBurrowInto(level, below)) {
						hideIn(level, below, true);
					}
				}
			}
			case NONE -> {
			}
		}
	}

	private void landed(@Nullable LivingEntity target) {
		boolean in = leapingIn;
		endAction();
		if (in && target != null && target.isAlive() && inReach(target, settings().reach() + 0.6)) {
			startCombo(target);
		}
	}

	private void tickCombo(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (t >= action.length()) {
			attackCooldown = settings().comboCooldown();
			endAction();
			return;
		}
		if (target == null || !target.isAlive()) {
			attackCooldown = settings().comboCooldown();
			endAction();
			return;
		}
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		for (int hit : LurkerAction.COMBO_HITS) {
			if (t == hit - 2 && onGround() && horizontalDistanceTo(target) > 1.0) {
				// A scramble forward into each swipe.
				Vec3 to = horizontal(target.position().subtract(position()));
				setDeltaMovement(to.x * 0.3, getDeltaMovement().y, to.z * 0.3);
			}
			if (t == hit) {
				playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.2F + hit * 0.02F);
				if (inReach(target, settings().reach())) {
					// The swipes come faster than the usual half-second of hurt immunity; each one must land.
					target.damageCooldownTime = 0;
					swipe(level, target);
				}
			}
		}
	}

	private void swipe(ServerLevel level, LivingEntity target) {
		DamageSource source = damageSources().mobAttack(this);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * settings().hitMultiplier();
		if (target.hurtServer(level, source, damage)) {
			setLastHurtMob(target);
		}
	}

	private boolean inReach(LivingEntity target, double reach) {
		return horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.0 && hasLineOfSight(target);
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Hidden", hidden);
		output.putInt("RehideCooldown", rehideCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setHidden(input.getBooleanOr("Hidden", false));
		rehideCooldown = input.getIntOr("RehideCooldown", 0);
	}

	// ---- Geometry ----

	double horizontalDistanceTo(Entity other) {
		double dx = other.getX() - getX();
		double dz = other.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private void faceTowards(LivingEntity target) {
		float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 40.0F));
		setYBodyRot(getYRot());
		setYHeadRot(getYRot());
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}

	// ---- Sounds ----

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return hidden ? null : SoundEvents.DROWNED_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DROWNED_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DROWNED_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.HUSK_STEP, 0.15F, 1.0F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}
}
