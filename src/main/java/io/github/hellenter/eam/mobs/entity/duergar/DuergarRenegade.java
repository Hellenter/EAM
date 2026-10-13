package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A duergar renegade: a living grey dwarf cast out of his clan, who roams the dark of the underworld in a squad of four
 * or five, all in heavy, whole armor. Slow on their short legs but hard to hurt, and with their picks they dig through
 * stone to get at a player who hides in it. Squads never spawn where any light falls. The leader, in his own horned
 * helm and spiked pauldrons, brings his followers along when he spawns; one sees the player, all of them fight.
 */
public class DuergarRenegade extends Monster implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DuergarRenegade.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(DuergarRenegade.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has. */
	public static final int SKINS = 2;
	/** Squad mates further away than this are not counted. */
	static final double SQUAD_RANGE = 32.0;
	private static final int SQUAD_REFRESH_TICKS = 20;

	/** The squad's leader; {@code null} for a leader himself. */
	private @Nullable UUID leader;
	private List<DuergarRenegade> squad = List.of();
	private int squadRefresh;

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
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
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
