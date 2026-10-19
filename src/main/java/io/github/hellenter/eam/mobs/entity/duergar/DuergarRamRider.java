package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.tools.ThrownTomahawk;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic;
import io.github.hellenter.eam.mobs.entity.crossbowman.NetShot;
import io.github.hellenter.eam.mobs.entity.ram.CaveRam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
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
 * A ram rider: a duergar outcast in light gear on a cave ram. They roam the dark underworld in a squad of three or four,
 * each on his own ram; the leader wears a horned helm. They ride at the player and cut at him with tomahawks from the
 * saddle, throw tomahawks from a distance (each has spares on his belt) and now and then cast a net over him, the same
 * net the elite crossbowman shoots. With his ram killed a rider fights on foot.
 */
public class DuergarRamRider extends Monster implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DuergarRamRider.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(DuergarRamRider.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(DuergarRamRider.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has: the renegades' own, outcasts both. */
	public static final int SKINS = DuergarRenegade.SKINS;
	private static final int SQUAD_REFRESH_TICKS = 20;
	/** How often, in a fight, he thinks of a throw. */
	private static final int DECISION_TICKS = 20;
	/** Room a rider on his ram needs above the ground. */
	private static final double MOUNTED_HEIGHT = 2.6;
	/** A target further above or below him than this is out of his tomahawk's reach. */
	private static final double MELEE_HEIGHT = 2.5;

	/** The squad's leader; {@code null} for a leader himself. */
	private @Nullable UUID leader;
	private List<DuergarRamRider> squad = List.of();
	private int squadRefresh;

	private RenegadeAction action = RenegadeAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private int decisionTicks;
	private int tomahawkCooldown;
	private int netCooldown;
	/** Client: ticks since the synced action last changed. */
	private int clientActionTicks;

	public DuergarRamRider(EntityType<? extends DuergarRamRider> type, Level level) {
		super(type, level);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		RamRiderSettings settings = RamRiderSettings.DEFAULT;
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.MOVEMENT_SPEED, settings.speed())
				.add(Attributes.ATTACK_DAMAGE, settings.attackDamage())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	private static RamRiderSettings settings() {
		return DuergarModule.riderSettings();
	}

	/**
	 * Like any monster, and only in full darkness, with room for a ram under him; one squad of riders or renegades at a
	 * time in any one stretch of the caves.
	 */
	public static boolean checkSpawnRules(EntityType<DuergarRamRider> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (reason != EntitySpawnReason.NATURAL) {
			return true;
		}
		AABB around = new AABB(pos).inflate(settings().spawnSpacing());
		return DuergarLogic.darkEnough(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos))
				&& level.noCollision(mountedBox(pos))
				&& level.getEntitiesOfClass(DuergarRamRider.class, around).isEmpty()
				&& level.getEntitiesOfClass(DuergarRenegade.class, around).isEmpty();
	}

	/** The room a ram with its rider takes standing at {@code pos}. */
	private static AABB mountedBox(BlockPos pos) {
		EntityType<?> ram = EamEntities.CAVE_RAM;
		return ram.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5).setMaxY(pos.getY() + MOUNTED_HEIGHT);
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
		// Mounted he rides at the player as fast as a ram charges; unhorsed he walks.
		goalSelector.addGoal(3, new MeleeAttackGoal(this, settings().mountedSpeed(), true) {
			@Override
			public boolean canUse() {
				return ram() != null && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return ram() != null && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
			@Override
			public boolean canUse() {
				return ram() == null && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return ram() == null && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(4, new FollowLeaderGoal());
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		// A squad mate's wild swing does not turn him on his own; a renegade's neither, an outcast as he is.
		targetSelector.addGoal(1, new HurtByTargetGoal(this, DuergarRamRider.class, DuergarRenegade.class).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, DuergarVillager.class, true));
	}

	/** His ram, while he is in the saddle. */
	@Nullable CaveRam ram() {
		return getVehicle() instanceof CaveRam ram && ram.isAlive() ? ram : null;
	}

	// ---- Spawning and gear ----

	public boolean isLeader() {
		return leader == null;
	}

	/** He always comes on his ram; a leader brings his riders, each on his own. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		RamRiderSettings settings = settings();
		leader = groupData instanceof Squad squadData ? squadData.leader() : null;
		setBase(Attributes.MAX_HEALTH, isLeader() ? settings.leaderHealth() : settings.health());
		setBase(Attributes.MOVEMENT_SPEED, settings.speed());
		setBase(Attributes.ATTACK_DAMAGE, settings.attackDamage());
		setHealth(getMaxHealth());
		if (isLeader()) {
			xpReward = 20;
		}
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData instanceof Squad ? null : groupData);
		RandomSource random = getRandom();
		setGear(DuergarGear.rider(random::nextDouble, isLeader(), settings.baldChance()));
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EamItems.TOMAHAWK));
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		setDropChance(EquipmentSlot.MAINHAND, settings.tomahawkDropChance());
		mountRam(level, difficulty);
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

	/** A grown ram where he stands, and he gets on it. */
	private void mountRam(ServerLevelAccessor level, DifficultyInstance difficulty) {
		CaveRam ram = EamEntities.CAVE_RAM.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (ram == null) {
			return;
		}
		ram.snapTo(getX(), getY(), getZ(), getYRot(), 0.0F);
		ram.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new AgeableMob.AgeableMobGroupData(false));
		if (isPersistenceRequired()) {
			ram.setPersistenceRequired();
		}
		level.addFreshEntity(ram);
		startRiding(ram, true, false);
	}

	private void spawnFollower(ServerLevelAccessor level, DifficultyInstance difficulty) {
		DuergarRamRider follower = EamEntities.DUERGAR_RAM_RIDER.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (follower == null) {
			return;
		}
		Vec3 spot = followerSpot(level, getRandom());
		follower.snapTo(spot.x, spot.y, spot.z, getRandom().nextFloat() * 360.0F, 0.0F);
		if (isPersistenceRequired()) {
			follower.setPersistenceRequired();
		}
		follower.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new Squad(getUUID()));
		level.addFreshEntityWithPassengers(follower);
	}

	/** A free spot for a ram and its rider on solid ground within four blocks of the leader, or his own. */
	private Vec3 followerSpot(ServerLevelAccessor level, RandomSource random) {
		BlockPos centre = blockPosition();
		for (int attempt = 0; attempt < 16; attempt++) {
			BlockPos column = centre.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.above(dy);
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
						&& level.noCollision(mountedBox(pos))) {
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
		return other instanceof DuergarRamRider || other instanceof DuergarRenegade || other instanceof CaveRam && other.isVehicle()
				|| super.considersEntityAsAlly(other);
	}

	/** A blow struck from a charging ram throws the player back harder. */
	@Override
	protected float getKnockback(Entity target, DamageSource damageSource) {
		float knockback = super.getKnockback(target, damageSource);
		return ram() != null ? knockback + settings().mountedKnockback() : knockback;
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
			squad = level.getEntitiesOfClass(DuergarRamRider.class, getBoundingBox().inflate(DuergarRenegade.SQUAD_RANGE),
					mate -> mate != this && mate.isAlive() && id.equals(mate.squadId()));
			shareTarget();
		}
		if (tomahawkCooldown > 0) tomahawkCooldown--;
		if (netCooldown > 0) netCooldown--;
		if (action != RenegadeAction.NONE) {
			tickAction(level);
		} else if (--decisionTicks <= 0) {
			decisionTicks = DECISION_TICKS;
			pickAttack();
		}
	}

	/** One sees the player, all of them ride at him. */
	private void shareTarget() {
		LivingEntity own = getTarget();
		if (own != null && own.isAlive()) {
			return;
		}
		double range = DuergarRenegade.SQUAD_RANGE;
		for (DuergarRamRider mate : squad) {
			LivingEntity target = mate.getTarget();
			if (target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target) && canAttack(target)
					&& distanceToSqr(target) < range * range) {
				setTarget(target);
				return;
			}
		}
	}

	/** His leader, if alive and near. */
	private @Nullable DuergarRamRider leaderNearby() {
		if (leader == null) {
			return null;
		}
		for (DuergarRamRider mate : squad) {
			if (leader.equals(mate.getUUID())) {
				return mate;
			}
		}
		return null;
	}

	// ---- Throws ----

	@Override
	public RenegadeAction action() {
		return RenegadeAction.byId(entityData.get(DATA_ACTION));
	}

	@Override
	public float actionTime(float partialTicks) {
		return clientActionTicks + partialTicks;
	}

	/** Once a second in a fight: the net first, as it is rarer, then the tomahawk. */
	private void pickAttack() {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target) || !getSensing().hasLineOfSight(target)) {
			return;
		}
		RamRiderSettings settings = settings();
		RandomSource random = getRandom();
		double distance = distanceTo(target);
		// A player up on a ledge or a pillar he cannot reach gets thrown at, however close he stands.
		boolean outOfReach = Math.abs(target.getY() - getY()) > MELEE_HEIGHT;
		RamRiderSettings.Net net = settings.net();
		if (netCooldown <= 0 && (distance >= net.minDistance() || outOfReach) && distance <= net.maxDistance()
				&& random.nextDouble() < net.chance()) {
			netCooldown = net.cooldownTicks();
			for (DuergarRamRider mate : squad) {
				mate.netCooldown = Math.max(mate.netCooldown, net.squadCooldownTicks());
			}
			startAction(RenegadeAction.NET_THROW, target);
			return;
		}
		RamRiderSettings.Tomahawk tomahawk = settings.tomahawk();
		if (tomahawkCooldown <= 0 && (distance >= tomahawk.minDistance() || outOfReach) && distance <= tomahawk.maxDistance()
				&& random.nextDouble() < tomahawk.chance()) {
			tomahawkCooldown = tomahawk.cooldownTicks();
			startAction(RenegadeAction.TOMAHAWK_THROW, target);
		}
	}

	private void startAction(RenegadeAction next, @Nullable LivingEntity target) {
		action = next;
		actionTicks = 0;
		actionTarget = target;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		action = RenegadeAction.NONE;
		actionTicks = 0;
		actionTarget = null;
		entityData.set(DATA_ACTION, (byte) RenegadeAction.NONE.ordinal());
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= action.length() || target == null || !target.isAlive()) {
			endAction();
			return;
		}
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		switch (action) {
			case TOMAHAWK_THROW -> {
				if (t == 0) {
					playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.0F, getVoicePitch());
				} else if (t == RenegadeAction.TOMAHAWK_RELEASE) {
					throwTomahawk(level, target);
				}
			}
			case NET_THROW -> {
				if (t % 6 == 0 && t < RenegadeAction.NET_RELEASE) {
					playSound(SoundEvents.WOOL_PLACE, 0.6F, 1.4F);
				} else if (t == RenegadeAction.NET_RELEASE) {
					castNet(level, target);
				}
			}
			default -> endAction();
		}
	}

	/** A spare tomahawk off his belt: the one in his hand stays there. */
	private void throwTomahawk(ServerLevel level, LivingEntity target) {
		RamRiderSettings.Tomahawk settings = settings().tomahawk();
		ThrownTomahawk tomahawk = new ThrownTomahawk(level, this, new ItemStack(EamItems.TOMAHAWK), settings.damage());
		tomahawk.pickup = AbstractArrow.Pickup.DISALLOWED;
		aim(tomahawk, target, settings.speed(), 1.5F, EliteCrossbowmanLogic.ARROW_GRAVITY);
		level.addFreshEntity(tomahawk);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.3F);
	}

	private void castNet(ServerLevel level, LivingEntity target) {
		NetShot net = new NetShot(level, this);
		aim(net, target, settings().net().speed(), 2.0F, EliteCrossbowmanLogic.THROWN_GRAVITY);
		level.addFreshEntity(net);
		playSound(SoundEvents.WITCH_THROW, 1.0F, 0.7F);
	}

	private void aim(Projectile projectile, LivingEntity target, float speed, float inaccuracy, double gravity) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		double dy = EliteCrossbowmanLogic.aimHeight(flat, target.getY(1.0 / 3.0) - projectile.getY(), speed, gravity);
		projectile.shoot(dx, dy, dz, speed, inaccuracy);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			clientActionTicks++;
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			clientActionTicks = 0;
		}
	}

	/** On foot he stands still to throw; in the saddle he throws as he rides. */
	private final class BusyGoal extends Goal {
		BusyGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return action != RenegadeAction.NONE && ram() == null;
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

	/** Out of a fight, a rider keeps up with his leader. */
	private final class FollowLeaderGoal extends Goal {
		private static final double START_DISTANCE = 9.0;
		private static final double STOP_DISTANCE = 4.0;
		private static final int REPATH_INTERVAL = 10;

		private @Nullable DuergarRamRider leaderToFollow;
		private int repath;

		FollowLeaderGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (getTarget() != null) {
				return false;
			}
			leaderToFollow = leaderNearby();
			return leaderToFollow != null && distanceToSqr(leaderToFollow) > START_DISTANCE * START_DISTANCE;
		}

		@Override
		public boolean canContinueToUse() {
			return getTarget() == null && leaderToFollow != null && leaderToFollow.isAlive()
					&& distanceToSqr(leaderToFollow) > STOP_DISTANCE * STOP_DISTANCE;
		}

		@Override
		public void start() {
			repath = 0;
		}

		@Override
		public void stop() {
			leaderToFollow = null;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			if (leaderToFollow != null && --repath <= 0) {
				repath = REPATH_INTERVAL;
				getNavigation().moveTo(leaderToFollow, 1.0);
			}
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
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.15F, 0.8F);
		super.playStepSound(pos, block);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
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
