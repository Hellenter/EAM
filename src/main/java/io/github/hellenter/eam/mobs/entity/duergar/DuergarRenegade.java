package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A duergar renegade: a living grey dwarf cast out of his clan, who roams the dark of the underworld in a squad of four
 * or five, all in heavy, whole armor. Slow on their short legs but hard to hurt, and with their picks they dig through
 * stone to get at a player who hides in it. Squads never spawn where any light falls. The leader, in his own horned
 * helm and spiked pauldrons, brings his followers along when he spawns; one sees the player, all of them fight.
 * <p>
 * Besides plain hits, each has three attacks with a clear wind-up ({@link DuergarRenegadeAttacks}): a charged blow from
 * over his head that puts a blocking shield on cooldown, a bomb he lights and lobs, and, rarer still, a squad mate he
 * lifts over his head and hurls at the player.
 */
public class DuergarRenegade extends Monster implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DuergarRenegade.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(DuergarRenegade.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(DuergarRenegade.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has. */
	public static final int SKINS = 2;
	/** Squad mates further away than this are not counted. */
	static final double SQUAD_RANGE = 32.0;
	private static final int SQUAD_REFRESH_TICKS = 20;
	/** How often, in a fight, he thinks of a special attack. */
	private static final int DECISION_TICKS = 20;
	/** Lob flight time per block of distance: a bomb, a thrown squad mate. */
	private static final double BOMB_TICKS_PER_BLOCK = 1.4;
	private static final double MATE_TICKS_PER_BLOCK = 1.3;

	/** The squad's leader; {@code null} for a leader himself. */
	private @Nullable UUID leader;
	private List<DuergarRenegade> squad = List.of();
	private int squadRefresh;

	private RenegadeAction action = RenegadeAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	/** The squad mate he is lifting, or the one he was thrown at. */
	private @Nullable DuergarRenegade carried;
	private boolean flightHit;
	private int decisionTicks;
	private int chargeCooldown;
	private int bombCooldown;
	private int throwCooldown;
	/** Client: ticks since the synced action last changed. */
	private int clientActionTicks;

	public DuergarRenegade(EntityType<? extends DuergarRenegade> type, Level level) {
		super(type, level);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		DuergarRenegadeSettings settings = DuergarRenegadeSettings.DEFAULT;
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.MOVEMENT_SPEED, settings.speed())
				.add(Attributes.ATTACK_DAMAGE, settings.attackDamage())
				.add(Attributes.ARMOR_TOUGHNESS, settings.armorToughness())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	private static DuergarRenegadeSettings settings() {
		return DuergarModule.renegadeSettings();
	}

	/** Like any monster, and only in full darkness; one squad at a time in any one stretch of the caves. */
	public static boolean checkSpawnRules(EntityType<DuergarRenegade> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (reason != EntitySpawnReason.NATURAL) {
			return true;
		}
		return DuergarLogic.darkEnough(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
				&& level.getEntitiesOfClass(DuergarRenegade.class, new AABB(pos).inflate(settings().spawnSpacing())).isEmpty();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GEAR, 0);
		builder.define(DATA_SKIN, (byte) 0);
		builder.define(DATA_ACTION, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new BusyGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(4, new FollowSquadLeaderGoal(this));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		// A squad mate's wild swing does not turn him on his own.
		targetSelector.addGoal(1, new HurtByTargetGoal(this, DuergarRenegade.class).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, DuergarVillager.class, true));
	}

	// ---- Spawning and gear ----

	public boolean isLeader() {
		return leader == null;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		DuergarRenegadeSettings settings = settings();
		leader = groupData instanceof Squad squadData ? squadData.leader() : null;
		setBase(Attributes.MAX_HEALTH, isLeader() ? settings.leaderHealth() : settings.health());
		setBase(Attributes.MOVEMENT_SPEED, settings.speed());
		setBase(Attributes.ATTACK_DAMAGE, settings.attackDamage());
		setBase(Attributes.ARMOR_TOUGHNESS, settings.armorToughness());
		setHealth(getMaxHealth());
		if (isLeader()) {
			xpReward = 20;
		}
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData instanceof Squad ? null : groupData);
		RandomSource random = getRandom();
		setGear(DuergarGear.armored(random::nextDouble, isLeader(), settings.baldChance()));
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(isLeader() || random.nextFloat() < 0.35F ? Items.IRON_AXE : Items.IRON_PICKAXE));
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		setDropChance(EquipmentSlot.MAINHAND, settings.weaponDropChance());
		if (isLeader()) {
			int followers = DuergarLogic.followerCount(settings.minFollowers(), settings.maxFollowers(), random.nextDouble());
			for (int i = 0; i < followers; i++) {
				spawnFollower(level, difficulty);
			}
		}
		return data;
	}

	/** Gear comes from {@link #finalizeSpawn}; nothing vanilla on top. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	private void spawnFollower(ServerLevelAccessor level, DifficultyInstance difficulty) {
		DuergarRenegade follower = EamEntities.DUERGAR_RENEGADE.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (follower == null) {
			return;
		}
		Vec3 spot = followerSpot(level, follower, getRandom());
		follower.snapTo(spot.x, spot.y, spot.z, getRandom().nextFloat() * 360.0F, 0.0F);
		follower.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new Squad(getUUID()));
		if (isPersistenceRequired()) {
			follower.setPersistenceRequired();
		}
		level.addFreshEntityWithPassengers(follower);
	}

	/** A free spot on solid ground within three blocks of the leader, or his own. */
	private Vec3 followerSpot(ServerLevelAccessor level, DuergarRenegade follower, RandomSource random) {
		BlockPos centre = blockPosition();
		for (int attempt = 0; attempt < 16; attempt++) {
			BlockPos column = centre.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.above(dy);
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
						&& level.noCollision(follower, follower.getType().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
					return Vec3.atBottomCenterOf(pos);
				}
			}
		}
		return position();
	}

	/** Spawn data for a follower: his leader. */
	record Squad(UUID leader) implements SpawnGroupData {
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	@Override
	public int gear() {
		return entityData.get(DATA_GEAR);
	}

	void setGear(int gear) {
		entityData.set(DATA_GEAR, gear);
	}

	@Override
	public int skin() {
		return Math.floorMod(entityData.get(DATA_SKIN), SKINS);
	}

	/** His armor is part of him: it counts, but it cannot be taken. */
	@Override
	public int getArmorValue() {
		return super.getArmorValue() + DuergarGear.armor(gear()) + (int) settings().extraArmor();
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof DuergarRenegade || super.considersEntityAsAlly(other);
	}

	// ---- Squad ----

	/** The id all members of his squad share: the leader's. */
	UUID squadId() {
		return leader != null ? leader : getUUID();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--squadRefresh <= 0) {
			squadRefresh = SQUAD_REFRESH_TICKS;
			UUID id = squadId();
			squad = level.getEntitiesOfClass(DuergarRenegade.class, getBoundingBox().inflate(SQUAD_RANGE),
					mate -> mate != this && mate.isAlive() && id.equals(mate.squadId()));
			shareTarget();
		}
		if (chargeCooldown > 0) chargeCooldown--;
		if (bombCooldown > 0) bombCooldown--;
		if (throwCooldown > 0) throwCooldown--;
		if (action != RenegadeAction.NONE) {
			tickAction(level);
		} else {
			if (isPassenger() && getVehicle() instanceof DuergarRenegade) {
				// Loaded from a save in a mate's arms: he gets down.
				stopRiding();
			}
			if (--decisionTicks <= 0) {
				decisionTicks = DECISION_TICKS;
				pickAttack();
			}
		}
	}

	/** One sees the player, all of them fight. */
	private void shareTarget() {
		LivingEntity own = getTarget();
		if (own != null && own.isAlive()) {
			return;
		}
		for (DuergarRenegade mate : squad) {
			LivingEntity target = mate.getTarget();
			if (target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target) && canAttack(target)
					&& distanceToSqr(target) < SQUAD_RANGE * SQUAD_RANGE) {
				setTarget(target);
				return;
			}
		}
	}

	/** His leader, if alive and near. */
	@Nullable DuergarRenegade leaderNearby() {
		if (leader == null) {
			return null;
		}
		for (DuergarRenegade mate : squad) {
			if (leader.equals(mate.getUUID())) {
				return mate;
			}
		}
		return null;
	}

	// ---- Special attacks ----

	public RenegadeAction action() {
		return RenegadeAction.byId(entityData.get(DATA_ACTION));
	}

	/** Client: how far into his current action he is, in ticks. */
	public float actionTime(float partialTicks) {
		return clientActionTicks + partialTicks;
	}

	/** Once a second in a fight: the rarest attack first, so the common ones do not crowd it out. */
	private void pickAttack() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || isPassenger() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
			return;
		}
		DuergarRenegadeAttacks attacks = settings().attacks();
		RandomSource random = getRandom();
		double distance = distanceTo(target);
		boolean sees = getSensing().hasLineOfSight(target);
		DuergarRenegadeAttacks.AllyThrow allyThrow = attacks.allyThrow();
		if (throwCooldown <= 0 && sees && onGround() && !isVehicle() && distance >= allyThrow.minDistance()
				&& distance <= allyThrow.maxDistance() && random.nextDouble() < allyThrow.chance()) {
			DuergarRenegade mate = mateToThrow(allyThrow.grabRange());
			if (mate != null) {
				startAllyThrow(mate, target, allyThrow);
				return;
			}
		}
		DuergarRenegadeAttacks.Bomb bomb = attacks.bomb();
		if (bombCooldown <= 0 && sees && distance >= bomb.minDistance() && distance <= bomb.maxDistance()
				&& random.nextDouble() < bomb.chance()) {
			bombCooldown = bomb.cooldownTicks();
			for (DuergarRenegade mate : squad) {
				mate.bombCooldown = Math.max(mate.bombCooldown, bomb.squadCooldownTicks());
			}
			startAction(RenegadeAction.BOMB, target);
			return;
		}
		DuergarRenegadeAttacks.Charge charge = attacks.charge();
		if (chargeCooldown <= 0 && inReach(target, charge.reach()) && random.nextDouble() < charge.chance()) {
			chargeCooldown = charge.cooldownTicks();
			startAction(RenegadeAction.CHARGE, target);
		}
	}

	/** A squad mate beside him, standing free, who can be lifted. */
	private @Nullable DuergarRenegade mateToThrow(double range) {
		DuergarRenegade best = null;
		double bestDistance = range * range;
		for (DuergarRenegade mate : squad) {
			double d = distanceToSqr(mate);
			if (d <= bestDistance && mate.action == RenegadeAction.NONE && mate.onGround() && !mate.isPassenger() && !mate.isVehicle()
					&& hasLineOfSight(mate)) {
				best = mate;
				bestDistance = d;
			}
		}
		return best;
	}

	private void startAllyThrow(DuergarRenegade mate, LivingEntity target, DuergarRenegadeAttacks.AllyThrow settings) {
		if (!mate.startRiding(this, true, false)) {
			return;
		}
		throwCooldown = settings.cooldownTicks();
		for (DuergarRenegade other : squad) {
			other.throwCooldown = Math.max(other.throwCooldown, settings.squadCooldownTicks());
		}
		carried = mate;
		mate.carried = this;
		mate.startAction(RenegadeAction.HELD, target);
		startAction(RenegadeAction.ALLY_THROW, target);
		playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.2F, getVoicePitch());
		playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.7F);
	}

	private void startAction(RenegadeAction next, @Nullable LivingEntity target) {
		action = next;
		actionTicks = 0;
		actionTarget = target;
		flightHit = false;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
		getNavigation().stop();
	}

	private void endAction() {
		if (action == RenegadeAction.ALLY_THROW && carried != null && carried.getVehicle() == this) {
			// Interrupted before the throw: he puts his mate down.
			carried.stopRiding();
			carried.endAction();
		}
		if (action == RenegadeAction.FLYING || action == RenegadeAction.HELD) {
			resetFallDistance();
		}
		action = RenegadeAction.NONE;
		actionTicks = 0;
		actionTarget = null;
		carried = null;
		entityData.set(DATA_ACTION, (byte) RenegadeAction.NONE.ordinal());
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		if (t >= action.length()) {
			endAction();
			return;
		}
		switch (action) {
			case HELD -> {
				if (!(getVehicle() instanceof DuergarRenegade)) {
					endAction();
				}
				return;
			}
			case FLYING -> {
				tickFlying(level, t);
				return;
			}
			default -> {
			}
		}
		LivingEntity target = actionTarget;
		if (target == null || !target.isAlive()) {
			endAction();
			return;
		}
		getNavigation().stop();
		faceTowards(target);
		switch (action) {
			case CHARGE -> tickCharge(level, t, target);
			case BOMB -> tickBomb(level, t, target);
			case ALLY_THROW -> tickAllyThrow(t, target);
			default -> {
			}
		}
	}

	private void faceTowards(LivingEntity target) {
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	// The charged blow: the weapon goes up over his head, then comes down.
	private void tickCharge(ServerLevel level, int t, LivingEntity target) {
		DuergarRenegadeAttacks.Charge settings = settings().attacks().charge();
		if (t == 0) {
			playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.2F, getVoicePitch() * 0.85F);
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.6F);
		} else if (t == RenegadeAction.CHARGE_HIT) {
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
			if (inReach(target, settings.reach())) {
				chargedHit(level, target, settings);
			}
		}
	}

	/** Blocked by a shield, the blow lands on it and puts it on cooldown, as an axe would. */
	private void chargedHit(ServerLevel level, LivingEntity target, DuergarRenegadeAttacks.Charge settings) {
		ItemStack shield = target.getItemBlockingWith();
		BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
		boolean covered = blocks != null
				&& FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ());
		hit(level, target, settings.damageMultiplier(), 0.4F);
		if (covered) {
			blocks.disable(level, target, settings.shieldSeconds(), shield);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.7F);
		}
	}

	private boolean inReach(LivingEntity target, double reach) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz) - target.getBbWidth() * 0.5;
		return horizontal <= reach && Math.abs(target.getY() - getY()) < 2.5 && hasLineOfSight(target);
	}

	// The bomb: out of his belt with a flint and steel, two strikes, the fuse catches, a swing back and the lob.
	private void tickBomb(ServerLevel level, int t, LivingEntity target) {
		if (t == 0) {
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.2F);
		} else if (t == RenegadeAction.BOMB_STRIKE || t == RenegadeAction.BOMB_STRIKE + 3) {
			playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, 0.9F + getRandom().nextFloat() * 0.2F);
		} else if (t == RenegadeAction.BOMB_LIT) {
			playSound(SoundEvents.TNT_PRIMED, 0.7F, 1.5F);
		} else if (t == RenegadeAction.BOMB_THROW) {
			throwBomb(level, target);
		}
	}

	private void throwBomb(ServerLevel level, LivingEntity target) {
		DuergarBomb bomb = new DuergarBomb(level, this, new ItemStack(EamItems.DUERGAR_BOMB));
		bomb.arm(settings().attacks().bomb());
		bomb.setPos(getX(), getY() + getBbHeight() * 0.9, getZ());
		bomb.setDeltaMovement(lob(bomb.position(), target.position(), BOMB_TICKS_PER_BLOCK, bomb.getGravity(), 0.99, 0.99, true));
		level.addFreshEntity(bomb);
		playSound(SoundEvents.WITCH_THROW, 1.0F, 0.6F);
	}

	/**
	 * The start velocity of a lob from {@code from} that comes down on {@code to}: farther lobs fly longer and higher.
	 * {@code dragFirst} and the drags as {@link DuergarLogic#launchSpeed} takes them.
	 */
	private static Vec3 lob(Vec3 from, Vec3 to, double ticksPerBlock, double gravity, double horizontalDrag, double verticalDrag,
			boolean dragFirst) {
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		double distance = Math.sqrt(dx * dx + dz * dz);
		int ticks = DuergarLogic.flightTicks(distance, ticksPerBlock, 10, 30);
		double speed = DuergarLogic.launchSpeed(distance, ticks, 0.0, horizontalDrag, dragFirst);
		double vy = DuergarLogic.launchSpeed(to.y - from.y, ticks, gravity, verticalDrag, dragFirst);
		double scale = distance > 1.0E-4 ? speed / distance : 0.0;
		return new Vec3(dx * scale, vy, dz * scale);
	}

	// The thrown squad mate: lifted over his head, held there a moment, hurled.
	private void tickAllyThrow(int t, LivingEntity target) {
		DuergarRenegade mate = carried;
		if (mate == null || !mate.isAlive() || mate.getVehicle() != this) {
			if (t < RenegadeAction.ALLY_THROW_RELEASE) {
				endAction();
			}
			return;
		}
		if (t == RenegadeAction.ALLY_THROW_RELEASE) {
			mate.stopRiding();
			mate.setPos(getX(), getY() + getBbHeight() + 0.1, getZ());
			Vec3 velocity = lob(mate.position(), target.position(), MATE_TICKS_PER_BLOCK, mate.getGravity(), 0.91, 0.98, false);
			mate.setDeltaMovement(velocity);
			mate.needsSync = true;
			mate.carried = this;
			mate.startAction(RenegadeAction.FLYING, target);
			mate.setYRot((float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F);
			mate.yBodyRot = mate.getYRot();
			carried = null;
			playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
			mate.playSound(SoundEvents.VINDICATOR_HURT, 1.0F, mate.getVoicePitch() * 1.1F);
		}
	}

	/** In the air he hits whoever he flies into; he lands on his feet, unhurt by the fall. */
	private void tickFlying(ServerLevel level, int t) {
		if (t > 2 && (onGround() || isInWater() || isInLava())) {
			endAction();
			return;
		}
		resetFallDistance();
		LivingEntity target = actionTarget;
		if (!flightHit && target != null && target.isAlive() && getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) {
			flightHit = true;
			DuergarRenegadeAttacks.AllyThrow settings = settings().attacks().allyThrow();
			hit(level, target, settings.hitMultiplier(), settings.knockback());
		}
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		if (action == RenegadeAction.FLYING || action == RenegadeAction.HELD) {
			return false;
		}
		return super.causeFallDamage(fallDistance, damageModifier, source);
	}

	/** {@link #doHurtTarget} with the damage scaled: his weapon, its enchantments and their after-effects all apply. */
	private boolean hit(ServerLevel level, LivingEntity target, float multiplier, float extraKnockback) {
		ItemStack weapon = getWeaponItem();
		DamageSource source = weapon.getDamageSource(this);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		damage = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage);
		damage += weapon.getItem().getAttackDamageBonus(target, damage, source);
		damage *= multiplier;
		Vec3 oldMovement = target.getDeltaMovement();
		if (!target.hurtServer(level, source, damage)) {
			return false;
		}
		causeExtraKnockback(target, getKnockback(target, source) + extraKnockback, oldMovement, source, damage, true);
		weapon.hurtEnemy(target, this);
		EnchantmentHelper.doPostAttackEffects(level, target, source);
		setLastHurtMob(target);
		return true;
	}

	/** A squad mate on his shoulders never steers him. */
	@Override
	public boolean canControlVehicle() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			clientActionTicks++;
			if (action() == RenegadeAction.BOMB) {
				bombParticles(clientActionTicks);
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			clientActionTicks = 0;
		}
	}

	/** Client: sparks off the flint and steel, then the burning fuse in his hand. */
	private void bombParticles(int t) {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(yaw);
		double forwardZ = Mth.cos(yaw);
		double rightX = -Mth.cos(yaw);
		double rightZ = -Mth.sin(yaw);
		if (t == RenegadeAction.BOMB_STRIKE || t == RenegadeAction.BOMB_STRIKE + 3) {
			double x = getX() + forwardX * 0.5;
			double z = getZ() + forwardZ * 0.5;
			for (int i = 0; i < 6; i++) {
				level().addParticle(ParticleTypes.SMALL_FLAME, x, getY() + 0.85, z, (getRandom().nextDouble() - 0.5) * 0.1, 0.05,
						(getRandom().nextDouble() - 0.5) * 0.1);
			}
		} else if (t >= RenegadeAction.BOMB_LIT && t < RenegadeAction.BOMB_THROW) {
			double up = t < RenegadeAction.BOMB_SWING ? 1.05 : 1.6;
			double x = getX() + forwardX * 0.35 + rightX * 0.35;
			double z = getZ() + forwardZ * 0.35 + rightZ * 0.35;
			level().addParticle(ParticleTypes.SMOKE, x, getY() + up, z, 0.0, 0.03, 0.0);
			if (t % 2 == 0) {
				level().addParticle(ParticleTypes.SMALL_FLAME, x, getY() + up, z, 0.0, 0.01, 0.0);
			}
		}
	}

	/** Holds his other goals back while he is busy with an action. */
	private final class BusyGoal extends Goal {
		BusyGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return action != RenegadeAction.NONE;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			getNavigation().stop();
		}
	}

	// ---- Sounds: a vindicator's mutter, deeper ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.VINDICATOR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VINDICATOR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VINDICATOR_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.2F, 0.6F);
		super.playStepSound(pos, block);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.65F;
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Gear", gear());
		output.putInt("Skin", skin());
		output.storeNullable("Leader", UUIDUtil.CODEC, leader);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setGear(input.getIntOr("Gear", 0));
		entityData.set(DATA_SKIN, (byte) input.getIntOr("Skin", 0));
		leader = input.read("Leader", UUIDUtil.CODEC).orElse(null);
	}
}
