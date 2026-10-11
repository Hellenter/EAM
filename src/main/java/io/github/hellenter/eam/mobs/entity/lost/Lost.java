package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnight;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlockModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * One of the Lost: a dried-up corpse, half husk and half skeleton, in worn ancient metal. They roam in squads. The
 * leader, with the best gear and always a sword and a shield, presses the player into melee; swordsmen and spearmen
 * with shields stand between the player and the squad's archers, who keep their distance and shoot. Sword and spear
 * fighting is the skeleton knight's, with the Lost's own numbers.
 */
public class Lost extends SkeletonKnight {
	/** Squad mates further away than this are not counted. */
	static final double SQUAD_RANGE = 32.0;
	private static final int SQUAD_REFRESH_TICKS = 20;
	/** Sideways spots of the shield bearers in front of the archers, by their place in the squad. */
	private static final double[] GUARD_SIDES = {0.0, 1.5, -1.5, 3.0, -3.0};

	private LostRole role = LostRole.SWORDSMAN;
	/** The squad's leader; {@code null} for a leader himself. */
	private @Nullable UUID leader;
	private List<Lost> squad = List.of();
	private int squadRefresh;

	public Lost(EntityType<? extends Lost> type, Level level) {
		super(type, level);
		xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, LostSettings.Squad.DEFAULT.followerHealth())
				// As fast as a skeleton.
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
	}

	public static boolean checkSpawnRules(EntityType<Lost> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		// One squad at a time in any one stretch of the caves.
		return reason != EntitySpawnReason.NATURAL
				|| level.getEntitiesOfClass(Lost.class, new AABB(pos).inflate(LostModule.settings().squad().spawnSpacing())).isEmpty();
	}

	@Override
	protected SkeletonKnightSettings settings() {
		return role == LostRole.LEADER ? LostModule.settings().leader() : LostModule.settings().follower();
	}

	@Override
	protected double pokeReach() {
		return LostModule.settings().spearReach();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new LostFightGoal(this));
		goalSelector.addGoal(2, new LostBowGoal(this));
		goalSelector.addGoal(4, new LostFollowLeaderGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	public LostRole role() {
		return role;
	}

	@Override
	public boolean canUseNonMeleeWeapon(ItemStack item) {
		return role == LostRole.ARCHER && item.is(Items.BOW);
	}

	// ---- Spawning ----

	/**
	 * Spawned on his own (naturally, by a command or an egg) he leads a squad and brings his followers along; a follower
	 * gets his role and leader from the {@link Squad} data.
	 */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		LostSettings settings = LostModule.settings();
		if (groupData instanceof Squad squadData) {
			role = squadData.role();
			leader = squadData.leader();
		} else {
			role = LostRole.LEADER;
			leader = null;
		}
		boolean isLeader = role == LostRole.LEADER;
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(isLeader ? settings.squad().leaderHealth() : settings.squad().followerHealth());
			setHealth(getMaxHealth());
		}
		if (isLeader) {
			xpReward = 15;
		}
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData instanceof Squad ? null : groupData);
		float drop = isLeader ? settings.squad().leaderDropChance() : settings.squad().followerDropChance();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, drop);
		}
		if (isLeader) {
			spawnFollowers(level, difficulty, settings);
		}
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		LostGear.equip(this, role, random);
	}

	private void spawnFollowers(ServerLevelAccessor level, DifficultyInstance difficulty, LostSettings settings) {
		RandomSource random = getRandom();
		int count = LostLogic.followerCount(settings.squad().minFollowers(), settings.squad().maxFollowers(), random.nextDouble());
		for (LostRole followerRole : LostLogic.followerRoles(count, random::nextDouble)) {
			Lost follower = EamEntities.LOST.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
			if (follower == null) {
				continue;
			}
			Vec3 spot = followerSpot(level, follower, random);
			follower.snapTo(spot.x, spot.y, spot.z, random.nextFloat() * 360.0F, 0.0F);
			follower.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new Squad(followerRole, getUUID()));
			if (isPersistenceRequired()) {
				follower.setPersistenceRequired();
			}
			level.addFreshEntityWithPassengers(follower);
		}
		UndeadWarlockModule.maybeJoinSquad(level, difficulty, this);
	}

	/** A free spot on solid ground within three blocks of the leader, or his own. */
	private Vec3 followerSpot(ServerLevelAccessor level, Lost follower, RandomSource random) {
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

	/** Spawn data for a follower: his role and his leader. */
	record Squad(LostRole role, UUID leader) implements SpawnGroupData {
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
			squad = level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(SQUAD_RANGE),
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
		for (Lost mate : squad) {
			LivingEntity target = mate.getTarget();
			if (target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)
					&& distanceToSqr(target) < SQUAD_RANGE * SQUAD_RANGE) {
				setTarget(target);
				return;
			}
		}
	}

	/** His leader, if alive and near. */
	@Nullable Lost leaderNearby() {
		if (leader == null) {
			return null;
		}
		for (Lost mate : squad) {
			if (mate.isAlive() && leader.equals(mate.getUUID())) {
				return mate;
			}
		}
		return null;
	}

	/** The middle of the squad's living archers, or {@code null} without any. */
	@Nullable Vec3 archersCentre() {
		double x = 0.0;
		double z = 0.0;
		double y = 0.0;
		int archers = 0;
		for (Lost mate : squad) {
			if (mate.isAlive() && mate.role == LostRole.ARCHER) {
				x += mate.getX();
				y += mate.getY();
				z += mate.getZ();
				archers++;
			}
		}
		return archers == 0 ? null : new Vec3(x / archers, y / archers, z / archers);
	}

	/** How far sideways he stands in front of the archers, so the shield bearers spread out in a line. */
	double guardSide() {
		List<Lost> guards = new ArrayList<>();
		guards.add(this);
		for (Lost mate : squad) {
			if (mate.isAlive() && mate.role != LostRole.LEADER && mate.role.shielded()) {
				guards.add(mate);
			}
		}
		guards.sort(Comparator.comparingInt(Lost::getId));
		return GUARD_SIDES[guards.indexOf(this) % GUARD_SIDES.length];
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("LostRole", role.ordinal());
		output.storeNullable("Leader", UUIDUtil.CODEC, leader);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		role = LostRole.byId(input.getIntOr("LostRole", LostRole.SWORDSMAN.ordinal()));
		leader = input.read("Leader", UUIDUtil.CODEC).orElse(null);
	}

	// ---- Sounds: a husk's, lower ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HUSK_AMBIENT;
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
	protected SoundEvent getStepSound() {
		return SoundEvents.HUSK_STEP;
	}
}
