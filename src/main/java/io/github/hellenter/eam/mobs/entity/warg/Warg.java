package io.github.hellenter.eam.mobs.entity.warg;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A warg, a big shaggy wolf of the snowy taiga, or the leader of a warg pack ({@link EamEntities#WARG_LEADER}, bigger
 * and darker). The leader picks the player to hunt and runs the hunt: the pack circles the player and, one at a time,
 * the warg furthest behind the player's back darts in for a bite and drops back into the ring. A warg the player turns
 * to face on its way in backs off. Kill the leader and the rest of the pack runs and keeps away from players after.
 * A warg without a pack, or a leader who lost his, fights like a plain wolf. Numbers are in
 * {@code data/eam/mobs/warg.json}.
 */
public class Warg extends Monster {
	/** Ticks a warg waits for its unloaded or missing leader before it goes its own way. */
	private static final int LEADER_PATIENCE = 200;
	/** A lone warg joins a leader this close. */
	private static final double JOIN_RANGE = 24.0;
	/** Pack members further than this from their leader are not counted in the hunt. */
	private static final double PACK_RANGE = 32.0;
	/** Without a hunt, a member keeps within this of its leader. */
	private static final double FOLLOW_DISTANCE = 8.0;

	/** A member's leader; {@code null} for a leader himself and for a lone warg. */
	private @Nullable UUID leader;
	private @Nullable Warg leaderCache;
	private int leaderMissing;
	/** Its leader is dead: it runs, then keeps away from players for good. */
	private boolean broken;
	private int fleeTicks;

	// A leader's hunt.
	private final List<Warg> pack = new ArrayList<>();
	private @Nullable LivingEntity packTarget;
	private @Nullable Warg striker;
	private int strikeTicks;
	private int strikeCooldown;
	private double ringAngle;

	public Warg(EntityType<? extends Warg> type, Level level) {
		super(type, level);
		xpReward = isLeader() ? 15 : 5;
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 14.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 2.5)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public static AttributeSupplier.Builder createLeaderAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.33)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	static WargSettings settings() {
		return WargModule.settings();
	}

	public boolean isLeader() {
		return getType() == EamEntities.WARG_LEADER;
	}

	/** Lost its leader and running, or done running and keeping away. */
	public boolean isBroken() {
		return broken;
	}

	// ---- Spawning ----

	/**
	 * Natural spawns of a leader (his pack comes with him): only at night, under the open sky and away from torches, now
	 * and then, and never close to another pack.
	 */
	public static boolean checkLeaderSpawnRules(EntityType<Warg> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		WargSettings settings = settings();
		if (!level.getLevel().isDarkOutside() || !level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK, pos) > 0
				|| random.nextFloat() >= settings.spawnChance()) {
			return false;
		}
		return level.getEntitiesOfClass(Warg.class, new AABB(pos).inflate(settings.packSpacing()), Warg::isLeader).isEmpty();
	}

	/** A leader brings his pack, whether he spawned naturally, from his egg or by a command; a member gets its leader. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		if (groupData instanceof Pack packData) {
			leader = packData.leader();
			return super.finalizeSpawn(level, difficulty, reason, null);
		}
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (isLeader()) {
			WargSettings settings = settings();
			int count = WargLogic.packSize(settings.packMin(), settings.packMax(), getRandom().nextDouble());
			for (int i = 0; i < count; i++) {
				spawnMember(level, difficulty);
			}
		}
		return data;
	}

	private void spawnMember(ServerLevelAccessor level, DifficultyInstance difficulty) {
		Warg member = EamEntities.WARG.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (member == null) {
			return;
		}
		Vec3 spot = memberSpot(level, member, getRandom());
		member.snapTo(spot.x, spot.y, spot.z, getRandom().nextFloat() * 360.0F, 0.0F);
		member.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new Pack(getUUID()));
		if (isPersistenceRequired()) {
			member.setPersistenceRequired();
		}
		level.addFreshEntityWithPassengers(member);
	}

	/** A free spot on solid ground within three blocks of the leader, or his own. */
	private Vec3 memberSpot(ServerLevelAccessor level, Warg member, RandomSource random) {
		BlockPos centre = blockPosition();
		for (int attempt = 0; attempt < 16; attempt++) {
			BlockPos column = centre.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.above(dy);
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
						&& level.noCollision(member, member.getType().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
					return Vec3.atBottomCenterOf(pos);
				}
			}
		}
		return position();
	}

	/** Spawn data for a pack member: its leader. */
	record Pack(UUID leader) implements SpawnGroupData {
	}

	// ---- Goals ----

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 24.0F, 1.3, 1.6) {
			@Override
			public boolean canUse() {
				return broken && super.canUse();
			}
		});
		goalSelector.addGoal(2, new WargPackGoal(this));
		// Without a pack: a plain wolf's charge.
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true) {
			@Override
			public boolean canUse() {
				return fightsAlone() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return fightsAlone() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new FollowLeaderGoal());
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return fightsAlone() && super.canUse();
			}
		});
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			public boolean canUse() {
				return fightsAlone() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return fightsAlone() && super.canContinueToUse();
			}
		});
	}

	/** Not in a pack of two or more and not broken: it fights on its own, like a wolf. */
	boolean fightsAlone() {
		if (broken) {
			return false;
		}
		Warg head = packLeader();
		return head == null || head.pack.isEmpty();
	}

	/** A member stays near its leader while the pack is not hunting. */
	private final class FollowLeaderGoal extends Goal {
		private int recalc;

		FollowLeaderGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			Warg head = isLeader() || broken ? null : packLeader();
			return head != null && head.packTarget == null && distanceToSqr(head) > FOLLOW_DISTANCE * FOLLOW_DISTANCE;
		}

		@Override
		public boolean canContinueToUse() {
			Warg head = packLeader();
			return head != null && head.packTarget == null && distanceToSqr(head) > 9.0 && !getNavigation().isDone();
		}

		@Override
		public void start() {
			recalc = 0;
		}

		@Override
		public void tick() {
			Warg head = packLeader();
			if (head != null && --recalc <= 0) {
				recalc = 10;
				getNavigation().moveTo(head, 1.0);
			}
		}
	}

	// ---- The pack ----

	/** The pack's leader: itself for a leader, its leader for a member while it is around, or {@code null}. */
	@Nullable Warg packLeader() {
		if (isLeader()) {
			return broken ? null : this;
		}
		return leaderCache != null && leaderCache.isAlive() ? leaderCache : null;
	}

	/** The player the pack is hunting, as its leader sees it. */
	@Nullable LivingEntity packTarget() {
		Warg head = packLeader();
		return head != null ? head.packTarget : null;
	}

	/** Whether this is the warg whose turn it is to strike. */
	boolean isStriker() {
		Warg head = packLeader();
		return head != null && head.striker == this;
	}

	/** Where in the ring around {@code target} this warg circles. */
	Vec3 ringSpot(LivingEntity target) {
		Warg head = packLeader();
		if (head == null) {
			return target.position();
		}
		WargSettings settings = settings();
		List<Warg> ring = head.ring();
		int index = Math.max(0, ring.indexOf(this));
		double angle = WargLogic.slotAngle(head.ringAngle, index, ring.size());
		double radius = settings.ringRadius() + (isLeader() ? settings.leaderRing() : 0.0);
		return new Vec3(target.getX() + Math.cos(angle) * radius, target.getY(), target.getZ() + Math.sin(angle) * radius);
	}

	/** The leader and his members, in a fixed order. */
	private List<Warg> ring() {
		List<Warg> ring = new ArrayList<>(pack.size() + 1);
		ring.add(this);
		ring.addAll(pack);
		return ring;
	}

	/** Its strike is over, bitten or called off; the next one waits a while. */
	void endStrike(boolean bit) {
		Warg head = packLeader();
		if (head != null && head.striker == this) {
			head.striker = null;
			int interval = settings().strikeInterval();
			head.strikeCooldown = bit ? interval : interval / 2;
		}
	}

	/** A bite at the end of its dash. */
	void bite(ServerLevel level, LivingEntity target) {
		swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
		doHurtTarget(level, target);
		endStrike(true);
	}

	/** Someone hurt one of the pack: the hunt turns on them. */
	private void alert(LivingEntity attacker) {
		if (attacker instanceof Player player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
			packTarget = player;
		}
	}

	/** Its leader is dead: off it runs, and it never hunts again. */
	private void panic() {
		broken = true;
		fleeTicks = settings().fleeTicks();
		leader = null;
		leaderCache = null;
		setTarget(null);
		setAggressive(false);
		getNavigation().stop();
		playSound(sounds().whineSound().value(), getSoundVolume(), getVoicePitch());
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (fleeTicks > 0) {
			fleeTicks--;
		}
		if (broken) {
			return;
		}
		if (isLeader()) {
			leadPack(level);
		} else {
			findLeader(level);
		}
	}

	private void findLeader(ServerLevel level) {
		if ((tickCount + getId()) % 20 != 0) {
			return;
		}
		if (leader != null) {
			Entity found = level.getEntity(leader);
			if (found instanceof Warg head && head.isAlive() && head.isLeader() && !head.broken) {
				leaderCache = head;
				leaderMissing = 0;
			} else {
				leaderCache = null;
				leaderMissing += 20;
				if (leaderMissing >= LEADER_PATIENCE) {
					leader = null;
				}
			}
			return;
		}
		// A warg on its own joins the nearest pack around.
		Warg head = level.getNearestEntity(Warg.class, TargetingConditions.forNonCombat()
				.ignoreLineOfSight().selector((entity, l) -> entity instanceof Warg warg && warg.isLeader() && !warg.broken),
				this, getX(), getY(), getZ(), getBoundingBox().inflate(JOIN_RANGE));
		if (head != null) {
			leader = head.getUUID();
			leaderCache = head;
		}
	}

	private void leadPack(ServerLevel level) {
		WargSettings settings = settings();
		if ((tickCount + getId()) % 10 == 0) {
			pack.clear();
			UUID id = getUUID();
			pack.addAll(level.getEntitiesOfClass(Warg.class, getBoundingBox().inflate(PACK_RANGE),
					warg -> warg != this && warg.isAlive() && !warg.broken && id.equals(warg.leader)));
			pack.sort(Comparator.comparingInt(Entity::getId));
		}
		pack.removeIf(warg -> !warg.isAlive());
		double drop = settings.aggroRange() * 2.0;
		if (packTarget != null && (!packTarget.isAlive() || packTarget.level() != level || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(packTarget)
				|| packTarget.distanceToSqr(this) > drop * drop)) {
			packTarget = null;
		}
		if (packTarget == null && (tickCount + getId()) % 10 == 0) {
			Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), settings.aggroRange(), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
			if (nearest != null && hasLineOfSight(nearest)) {
				packTarget = nearest;
			}
		}
		ringAngle += Math.toRadians(settings.ringSpin()) / 20.0;
		if (striker != null) {
			strikeTicks++;
			if (!striker.isAlive() || striker.broken || packTarget == null || strikeTicks > settings.strikeTimeout()) {
				striker.endStrike(false);
				striker = null;
			}
		}
		if (strikeCooldown > 0) {
			strikeCooldown--;
		}
		if (striker == null && strikeCooldown <= 0 && packTarget != null && !pack.isEmpty()) {
			pickStriker(packTarget, settings);
		}
	}

	/** Of the wargs already circling, the one furthest behind the target's back gets to strike, if the target is not watching it. */
	private void pickStriker(LivingEntity target, WargSettings settings) {
		List<Warg> ring = ring();
		double[] angles = new double[ring.size()];
		Vec3 look = target.getViewVector(1.0F);
		double ready = settings.ringRadius() + settings.leaderRing() + 3.0;
		for (int i = 0; i < ring.size(); i++) {
			Warg warg = ring.get(i);
			angles[i] = warg.distanceToSqr(target) <= ready * ready && warg.hasLineOfSight(target)
					? WargLogic.offAngle(look.x, look.z, warg.getX() - target.getX(), warg.getZ() - target.getZ())
					: -1.0;
		}
		int index = WargLogic.pickStriker(angles, settings.seenAngle());
		if (index >= 0) {
			striker = ring.get(index);
			strikeTicks = 0;
		}
	}

	/** Whether {@code target} is looking this warg's way. */
	boolean watchedBy(LivingEntity target) {
		Vec3 look = target.getViewVector(1.0F);
		return WargLogic.watched(WargLogic.offAngle(look.x, look.z, getX() - target.getX(), getZ() - target.getZ()), settings().seenAngle());
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && !broken && source.getEntity() instanceof LivingEntity attacker) {
			Warg head = packLeader();
			if (head != null && head.packTarget == null) {
				head.alert(attacker);
			}
		}
		return hurt;
	}

	/** The leader's death breaks the pack. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && isLeader() && !broken) {
			UUID id = getUUID();
			for (Warg warg : level.getEntitiesOfClass(Warg.class, getBoundingBox().inflate(PACK_RANGE * 2.0),
					warg -> warg != this && warg.isAlive() && id.equals(warg.leader))) {
				warg.panic();
			}
		}
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Leader", UUIDUtil.CODEC, leader);
		output.putBoolean("Broken", broken);
		output.putInt("FleeTicks", fleeTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		leader = input.read("Leader", UUIDUtil.CODEC).orElse(null);
		broken = input.getBooleanOr("Broken", false);
		fleeTicks = input.getIntOr("FleeTicks", 0);
	}

	// ---- Sounds: a wolf's, deeper ----

	private WolfSoundVariant.WolfSoundSet sounds() {
		return SoundEvents.WOLF_SOUNDS.get(isLeader() ? WolfSoundVariants.SoundSet.BIG : WolfSoundVariants.SoundSet.ANGRY).adultSounds();
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return (isAggressive() || packTarget() != null ? sounds().growlSound() : sounds().ambientSound()).value();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sounds().hurtSound().value();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sounds().deathSound().value();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(sounds().stepSound().value(), 0.15F, 1.0F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * (isLeader() ? 0.7F : 0.85F);
	}
}
