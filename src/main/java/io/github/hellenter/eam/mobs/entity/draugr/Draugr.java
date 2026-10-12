package io.github.hellenter.eam.mobs.entity.draugr;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A draugr: a warrior long dead, frozen into the floor of the underworld's ice caves with what is left of his armor and
 * an axe, sword or spear. Draugr lie in packs. A sleeper stays frozen until the ice or snow he lies in is broken or a
 * waiting draugr calls; a waiting one rises when a player comes close and wakes the pack around him in waves. Up, they
 * move a little faster than a zombie and hunt the living whether they see them or not; fire and lava hurt them twice
 * as much.
 */
public class Draugr extends Monster {
	private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Draugr.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(Draugr.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_RISE_TICKS = SynchedEntityData.defineId(Draugr.class, EntityDataSerializers.INT);
	/** He lies in a column of ice or snow this many blocks deep, the floor's top block the top of it. */
	public static final int DEPTH = 3;
	/**
	 * His feet are this far under the base of that top block, so the top of his head is just under the floor's surface
	 * and the tips of horns and spires stay inside the block too.
	 */
	public static final double FEET_UNDER_TOP = 1.5;
	/** Woken by his own block breaking, he goes for the nearest player this close. */
	private static final double DUG_OUT_RANGE = 16.0;
	/** Up, he hunts players this far off, seen or not; also how far he paths. */
	private static final double HUNT_RANGE = 64.0;
	/** Out of the floor, he looks this many blocks around and above his spot for room to stand if it is blocked. */
	private static final int ROOM_SEARCH = 2;

	/** Mirrors {@link #DATA_STATE}. */
	private DraugrState state = DraugrState.AWAKE;
	/** Ticks until he starts to rise, once woken; -1 while nothing has woken him. */
	private int wakeDelay = -1;
	private @Nullable UUID wakeTarget;
	/**
	 * Whether his column has held him whole at least once. The cave around a pack is not finished when the pack is laid:
	 * a lake from a neighbouring chunk may still cut into the floor and open a grave. A grave found open before it ever
	 * held him was spoiled that way, not dug out, so he is not there at all rather than standing about on the floor.
	 */
	private boolean sealed;
	private int riseTick;
	private double riseFrom;
	/** Server: he is clawing his way up a wall toward his target above him. */
	private boolean climbing;
	/** Client: the tick count when he began to rise. */
	private int clientRiseStart;

	public Draugr(EntityType<? extends Draugr> type, Level level) {
		super(type, level);
		xpReward = 8;
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, DraugrSettings.DEFAULT.health())
				.add(Attributes.MOVEMENT_SPEED, DraugrSettings.DEFAULT.speed())
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	private static DraugrSettings settings() {
		return DraugrModule.settings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, (byte) DraugrState.AWAKE.ordinal());
		builder.define(DATA_GEAR, 0);
		builder.define(DATA_RISE_TICKS, DraugrSettings.DEFAULT.riseTicks());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new SpearUseGoal<>(this, 1.0, 1.0, 10.0F, 2.0F));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		// A blow from a packmate (a spear thrust through the press) is no reason to turn on him.
		targetSelector.addGoal(1, new HurtByTargetGoal(this, Draugr.class).setAlertOthers());
		// The dead do not need to see the living: they feel them through the ice and rock.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	/**
	 * He walks around the spires of the ice caves like any walking mob. Unlike a spider he does not head straight for his
	 * target when he has no path, or he would climb the nearest spire to the ceiling.
	 */
	@Override
	protected PathNavigation createNavigation(Level level) {
		return new DraugrNavigation(this, level);
	}

	/** Only up toward a target above him: idle or after someone on his own floor, he does not go up the walls. */
	@Override
	public boolean onClimbable() {
		return climbing || super.onClimbable();
	}

	/**
	 * Where to head when his path has run out: his target, if it stands close by on a ledge he could climb to, else
	 * nothing. Right under it he stays put rather than hop about.
	 */
	@Nullable Vec3 ledgeTarget() {
		LivingEntity target = getTarget();
		if (state != DraugrState.AWAKE || target == null) {
			return null;
		}
		double across = horizontalDistanceTo(target);
		DraugrSettings settings = settings();
		return across > getBbWidth() && DraugrLogic.climbs(target.getY() - getY(), across, false, settings.climbHeight(), settings.climbReach())
				? target.position() : null;
	}

	private double horizontalDistanceTo(Entity other) {
		return Math.hypot(other.getX() - getX(), other.getZ() - getZ());
	}

	// ---- Spawning and gear ----

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		DraugrSettings settings = settings();
		setBase(Attributes.MAX_HEALTH, settings.health());
		setBase(Attributes.MOVEMENT_SPEED, settings.speed());
		setHealth(getMaxHealth());
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		RandomSource random = getRandom();
		setGear(DraugrGear.choose(random::nextDouble, settings.pieceWeightArray(), settings.ownPieceChance(), settings.beltChance()));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon(DraugrGear.weighted(random::nextDouble, settings.weaponWeightArray()))));
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		setDropChance(EquipmentSlot.MAINHAND, settings.weaponDropChance());
		return data;
	}

	/** An old axe, sword or spear. */
	private static Item weapon(int index) {
		return switch (index) {
			case 0 -> EamItems.RUSTY_IRON_AXE;
			case 1 -> EamItems.RUSTY_IRON_SWORD;
			default -> Items.IRON_SPEAR;
		};
	}

	private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	public int gear() {
		return entityData.get(DATA_GEAR);
	}

	void setGear(int gear) {
		entityData.set(DATA_GEAR, gear);
	}

	/** His armor is what is left on him: it counts, but it cannot be taken. */
	@Override
	public int getArmorValue() {
		return super.getArmorValue() + DraugrGear.armor(gear());
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

	public DraugrState state() {
		return state;
	}

	public void setState(DraugrState value) {
		entityData.set(DATA_STATE, (byte) value.ordinal());
		applyState(value);
	}

	private void applyState(DraugrState value) {
		state = value;
		noPhysics = value != DraugrState.AWAKE;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_STATE.equals(accessor)) {
			DraugrState previous = state;
			applyState(DraugrState.byId(entityData.get(DATA_STATE)));
			if (state == DraugrState.RISING && previous != DraugrState.RISING) {
				clientRiseStart = tickCount;
			}
		}
	}

	/** Client: how far he has climbed out of the floor, 0 to 1; 1 once up. */
	public float riseProgress(float partialTicks) {
		return switch (state) {
			case SLEEPING, WAITING -> 0.0F;
			case RISING -> Math.min(1.0F, (tickCount - clientRiseStart + partialTicks) / Math.max(1, entityData.get(DATA_RISE_TICKS)));
			case AWAKE -> 1.0F;
		};
	}

	@Override
	public boolean isNoGravity() {
		return state != DraugrState.AWAKE || super.isNoGravity();
	}

	@Override
	protected boolean isImmobile() {
		return state != DraugrState.AWAKE || super.isImmobile();
	}

	@Override
	public boolean isPickable() {
		return !state.buried() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return state == DraugrState.AWAKE && super.isPushable();
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return state == DraugrState.AWAKE && super.canBeCollidedWith(other);
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !state.buried() && super.canBeSeenAsEnemy();
	}

	/** Frozen dead do not freeze. */
	@Override
	public boolean canFreeze() {
		return false;
	}

	/** A pack fights as one. */
	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof Draugr || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return !state.buried() && super.removeWhenFarAway(distanceSquared);
	}

	/**
	 * The ice around a buried draugr takes the blows: break it first. Only what ignores invulnerability, like /kill, gets
	 * through. Climbing out he does not choke in the floor; up, fire and lava hurt him double, and his packmates' blows not
	 * at all.
	 */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (state.buried() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		if (state == DraugrState.RISING && source.is(DamageTypes.IN_WALL)) {
			return false;
		}
		// Draugr do not hurt each other: a spear thrust or a swing at the player that catches a packmate does nothing.
		if (source.getEntity() instanceof Draugr) {
			return false;
		}
		return super.hurtServer(level, source, DraugrLogic.damageTaken(damage, source.is(DamageTypeTags.IS_FIRE), settings().fireMultiplier()));
	}

	// ---- Ticking ----

	/** Buried, he is only a body in the ice: the server checks on him now and then and does nothing else. */
	@Override
	public void tick() {
		if (state.buried() && level() instanceof ServerLevel level) {
			// The level has already counted this tick.
			tickBuried(level);
			return;
		}
		super.tick();
		if (!level().isClientSide()) {
			LivingEntity target = getTarget();
			climbing = state == DraugrState.AWAKE && target != null && horizontalCollision && DraugrLogic.climbs(target.getY() - getY(),
					horizontalDistanceTo(target), climbing, settings().climbHeight(), settings().climbReach());
		}
	}

	@Override
	public void aiStep() {
		if (state == DraugrState.RISING) {
			setDeltaMovement(Vec3.ZERO);
			if (level() instanceof ServerLevel level) {
				tickRising(level);
			}
		}
		super.aiStep();
	}

	private void tickBuried(ServerLevel level) {
		if (wakeDelay >= 0) {
			if (wakeDelay-- == 0) {
				startRising(level);
			}
			return;
		}
		if ((tickCount + getId()) % 10 != 0) {
			return;
		}
		if (!entombed(level)) {
			if (!sealed) {
				discard();
				return;
			}
			// Dug out: he rises at whoever is near.
			Player player = level.getNearestPlayer(getX(), getY() + 1.0, getZ(), DUG_OUT_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
			wakeTarget = player == null ? null : player.getUUID();
			startRising(level);
			return;
		}
		sealed = true;
		if (state == DraugrState.WAITING) {
			Player player = level.getNearestPlayer(getX(), getY() + 1.0, getZ(), settings().waitRadius(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
			if (player != null) {
				wakeTarget = player.getUUID();
				startRising(level);
				DraugrAlarms.raise(level, position(), player);
			}
		}
	}

	/** Whether every block of his column still holds him. */
	private boolean entombed(ServerLevel level) {
		BlockPos feet = blockPosition();
		for (int dy = 0; dy < DEPTH; dy++) {
			if (!level.getBlockState(feet.above(dy)).isSolid()) {
				return false;
			}
		}
		return true;
	}

	/** Woken by a waiting draugr's call: he rises after {@code delay} ticks and goes for {@code player}, if any. */
	void wake(@Nullable Player player, int delay) {
		if (!state.buried() || wakeDelay >= 0) {
			return;
		}
		wakeTarget = player == null ? null : player.getUUID();
		wakeDelay = Math.max(0, delay);
	}

	private void startRising(ServerLevel level) {
		wakeDelay = -1;
		riseTick = 0;
		riseFrom = getY();
		entityData.set(DATA_RISE_TICKS, settings().riseTicks());
		setState(DraugrState.RISING);
		playSound(SoundEvents.HUSK_AMBIENT, 2.0F, 0.45F);
	}

	private void tickRising(ServerLevel level) {
		int total = Math.max(1, entityData.get(DATA_RISE_TICKS));
		riseTick++;
		double t = Math.min(1.0, (double) riseTick / total);
		// Up onto the floor, on the block above the top of his column.
		double riseTo = Mth.floor(riseFrom) + DEPTH;
		setPos(getX(), Mth.lerp(t, riseFrom, riseTo), getZ());
		BlockPos surface = BlockPos.containing(getX(), riseTo - 1.0, getZ());
		BlockState block = level.getBlockState(surface);
		if (block.isAir()) {
			block = Blocks.SNOW_BLOCK.defaultBlockState();
		}
		if (riseTick % 2 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block), getX(), surface.getY() + 1.0, getZ(), 8, 0.35, 0.1, 0.35, 0.1);
		}
		if (riseTick % 8 == 1) {
			level.playSound(null, getX(), surface.getY() + 1.0, getZ(), block.getSoundType().getBreakSound(), SoundSource.HOSTILE, 1.0F, 0.6F);
		}
		if (riseTick >= total) {
			Vec3 spot = roomToStand(level, new Vec3(getX(), riseTo, getZ()));
			setPos(spot.x, spot.y, spot.z);
			setState(DraugrState.AWAKE);
			Player player = wakeTarget == null ? null : level.getPlayerByUUID(wakeTarget);
			if (player == null || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) || player.distanceToSqr(this) > HUNT_RANGE * HUNT_RANGE) {
				player = level.getNearestPlayer(getX(), getY(), getZ(), HUNT_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
			}
			if (player != null) {
				setTarget(player);
				// Start walking at once rather than when the attack goal next looks for a path.
				getNavigation().moveTo(player, 1.0);
			}
			wakeTarget = null;
		}
	}

	/**
	 * Where he can stand on coming out of the floor: {@code spot} if nothing is in the way, else the nearest free spot a
	 * block or two around or above it (something may have been built or grown over his column). If there is none he
	 * stays at {@code spot}.
	 */
	private Vec3 roomToStand(ServerLevel level, Vec3 spot) {
		@Nullable Vec3 best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dy = 0; dy <= ROOM_SEARCH; dy++) {
			for (int dx = -ROOM_SEARCH; dx <= ROOM_SEARCH; dx++) {
				for (int dz = -ROOM_SEARCH; dz <= ROOM_SEARCH; dz++) {
					Vec3 at = spot.add(dx, dy, dz);
					double distance = dx * dx + dy * dy + dz * dz;
					if (distance < bestDistance && level.noCollision(this, getDimensions(getPose()).makeBoundingBox(at))
							&& !level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z)).getCollisionShape(level,
							BlockPos.containing(at.x, at.y - 0.5, at.z)).isEmpty()) {
						best = at;
						bestDistance = distance;
					}
				}
			}
		}
		return best == null ? spot : best;
	}

	// ---- Sounds ----

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return state.buried() ? null : SoundEvents.HUSK_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HUSK_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HUSK_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.HUSK_STEP, 0.15F, 0.8F);
	}

	/** Deeper and slower than a husk's. */
	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("DraugrState", state.serializedName());
		output.putInt("Gear", gear());
		output.putInt("WakeDelay", wakeDelay);
		output.putInt("RiseTick", riseTick);
		output.putDouble("RiseFrom", riseFrom);
		output.putBoolean("Sealed", sealed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setState(DraugrState.byName(input.getStringOr("DraugrState", "awake"), DraugrState.AWAKE));
		setGear(input.getIntOr("Gear", 0));
		wakeDelay = input.getIntOr("WakeDelay", -1);
		riseTick = input.getIntOr("RiseTick", 0);
		riseFrom = input.getDoubleOr("RiseFrom", getY());
		sealed = input.getBooleanOr("Sealed", false);
	}
}
