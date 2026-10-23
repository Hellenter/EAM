package io.github.hellenter.eam.mobs.entity.shambler;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A dead man overgrown with mushrooms: mycelium all over the body, caps on the head and shoulders. Left alone it stands
 * stock still and silent among the mushrooms, a stalk under a cap like any other, until a player comes within a few
 * blocks; then it stands up and shambles after them. Every blow that lands on it puffs out a cloud of spores that makes
 * everything round it sick, so it is best fought from afar or hit and left. Fire is its bane: it burns long, panics and
 * takes double damage.
 * <p>
 * The smoldering shambler of the burnt forest is the same creature charred: embers glow in its cracks, a blow on it
 * throws sparks that set everything round it on fire, and its own blows do too. Fire does not hurt it, but water and
 * snowballs do.
 */
public class MushroomShambler extends Monster {
	private static final EntityDataAccessor<Boolean> DATA_DISGUISED = SynchedEntityData.defineId(MushroomShambler.class,
			EntityDataSerializers.BOOLEAN);
	/** How fast the client poses it between standing (0) and crouched into a mushroom (1), per tick. */
	private static final float POSE_SPEED = 0.12F;
	private static final int WAKE_CHECK_INTERVAL = 5;

	/** Ticks awake with nothing to fight; it settles down again once this reaches the disguise delay. */
	private int idleTicks;
	private boolean wasOnFire;
	/** Client: how far it is crouched into a mushroom now and a tick ago. */
	private float disguise;
	private float oDisguise;

	public MushroomShambler(EntityType<? extends MushroomShambler> type, Level level) {
		super(type, level);
		xpReward = 5;
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				// Slower than a zombie: it shambles.
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	static ShamblerSettings settings() {
		return ShamblerModule.settings();
	}

	/** Natural spawns: in dim places only (the mushroom forests glow in patches), and only now and then. */
	public static boolean checkSpawnRules(EntityType<MushroomShambler> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason) || EntitySpawnReason.ignoresLightRequirements(reason)) {
			return true;
		}
		ShamblerSettings settings = settings();
		return level.getBrightness(LightLayer.BLOCK, pos) <= settings.maxSpawnLight() && random.nextFloat() < settings.spawnChance();
	}

	/** The charred kind from the burnt forest. */
	public boolean smoldering() {
		return getType() == EamEntities.SMOLDERING_SHAMBLER;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DISGUISED, false);
	}

	public boolean disguised() {
		return entityData.get(DATA_DISGUISED);
	}

	private void setDisguised(boolean value) {
		if (value == disguised()) {
			return;
		}
		entityData.set(DATA_DISGUISED, value);
		idleTicks = 0;
		if (value) {
			setTarget(null);
			getNavigation().stop();
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
		}
		updateControlFlags();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		// A plain one on fire runs about in a panic, looking for water.
		goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
			@Override
			protected boolean shouldPanic() {
				return mob.isOnFire() && !smoldering();
			}
		});
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Disguised, it neither moves, turns its head, nor picks a target: it is a mushroom. */
	@Override
	protected void updateControlFlags() {
		super.updateControlFlags();
		if (disguised()) {
			goalSelector.disableControlFlag(Goal.Flag.MOVE);
			goalSelector.disableControlFlag(Goal.Flag.LOOK);
			goalSelector.disableControlFlag(Goal.Flag.JUMP);
			targetSelector.disableControlFlag(Goal.Flag.TARGET);
		} else {
			targetSelector.enableControlFlag(Goal.Flag.TARGET);
		}
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		setDisguised(true);
		disguise = 1.0F;
		oDisguise = 1.0F;
		return data;
	}

	/** It roams and spawns whatever the light. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		ShamblerSettings settings = settings();
		boolean onFire = isOnFire();
		if (onFire && !wasOnFire && !smoldering()) {
			setRemainingFireTicks(ShamblerLogic.fireTicks(getRemainingFireTicks(), settings.burnTicks()));
		}
		wasOnFire = onFire;
		if ((tickCount + getId()) % WAKE_CHECK_INTERVAL != 0) {
			return;
		}
		Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), settings.wakeDistance() * 2.0 + 1.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		double nearestSqr = nearest == null ? Double.POSITIVE_INFINITY : nearest.distanceToSqr(this);
		if (disguised()) {
			if (nearest != null && ShamblerLogic.wakes(nearestSqr, settings.wakeDistance())) {
				wake(nearest);
			}
			return;
		}
		idleTicks = getTarget() == null ? idleTicks + WAKE_CHECK_INTERVAL : 0;
		if (ShamblerLogic.settles(idleTicks, settings.disguiseDelay(), onFire, nearestSqr, settings.wakeDistance())) {
			setDisguised(true);
		}
	}

	/** Stands up out of its disguise and goes for {@code target}, if any. */
	private void wake(@Nullable LivingEntity target) {
		if (!disguised()) {
			return;
		}
		setDisguised(false);
		playSound(EamSounds.SHAMBLER_WAKE, 1.0F, getVoicePitch());
		if (target != null && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
			setTarget(target);
		}
	}

	/**
	 * Every blow that lands wakes it and puffs out spores, or sparks from a smoldering one. A plain one takes more while it
	 * burns; a smoldering one is hurt by snowballs.
	 */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		ShamblerSettings settings = settings();
		float taken = ShamblerLogic.damageTaken(damage, isOnFire(), smoldering(), settings.burningDamage());
		if (smoldering() && source.getDirectEntity() instanceof Snowball) {
			taken = Math.max(taken, settings.snowballDamage());
		}
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && source.getEntity() != null) {
			wake(source.getEntity() instanceof LivingEntity living ? living : null);
			Vec3 chest = position().add(0.0, getBbHeight() * 0.6, 0.0);
			if (smoldering()) {
				ShamblerSpores.sparks(level, chest);
			} else {
				ShamblerSpores.spores(level, chest, this);
			}
		}
		return hurt;
	}

	/** A smoldering one's blows set the target alight. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && smoldering() && !target.fireImmune()) {
			target.igniteForTicks(settings().sparkFireTicks());
		}
		return hurt;
	}

	/** Water and rain put out a smoldering one, which hurts it. */
	@Override
	public boolean isSensitiveToWater() {
		return smoldering();
	}

	// ---- Client ----

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			oDisguise = disguise;
			disguise = Mth.approach(disguise, disguised() ? 1.0F : 0.0F, POSE_SPEED);
			ambientParticles();
		}
	}

	/** How far it is crouched into a mushroom: 0 standing, 1 a mushroom. */
	public float disguiseAmount(float partialTicks) {
		return Mth.lerp(partialTicks, oDisguise, disguise);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		// One that first shows up disguised is a mushroom at once, not one crouching down.
		if (DATA_DISGUISED.equals(accessor) && tickCount < 2) {
			disguise = disguised() ? 1.0F : 0.0F;
			oDisguise = disguise;
		}
	}

	/** Embers rising off a smoldering one now and then; a spore drifting off a plain one that is up and about. */
	private void ambientParticles() {
		RandomSource random = getRandom();
		if (smoldering()) {
			if (random.nextInt(6) == 0) {
				level().addParticle(EamParticles.SHAMBLER_EMBER, getRandomX(0.5), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.5),
						0.0, 0.02, 0.0);
			}
		} else if (!disguised() && random.nextInt(12) == 0) {
			level().addParticle(EamParticles.SHAMBLER_SPORE, getRandomX(0.6), getY() + getBbHeight() * (0.6 + random.nextDouble() * 0.4),
					getRandomZ(0.6), 0.0, 0.0, 0.0);
		}
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Disguised", disguised());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setDisguised(input.getBooleanOr("Disguised", false));
	}

	// ---- Sounds ----

	/** A mushroom makes no sound. */
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return disguised() ? null : EamSounds.SHAMBLER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return EamSounds.SHAMBLER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return EamSounds.SHAMBLER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.NYLIUM_STEP, 0.4F, 0.8F);
	}
}
