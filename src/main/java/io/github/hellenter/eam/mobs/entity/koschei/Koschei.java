package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.boss.BossBuilding;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.boss.BuildingAware;
import io.github.hellenter.eam.mobs.entity.boss.ParryPenalty;
import io.github.hellenter.eam.mobs.entity.boss.ParryStreak;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.draugr.Draugr;
import io.github.hellenter.eam.mobs.entity.lost.LostLogic;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Attack;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.DaggerAttack;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.StaffAttack;
import io.github.hellenter.eam.survival.bleeding.BleedingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Koschei the Deathless: a gaunt bald old man two and a half blocks tall in a spiked crown, dark filigree armor, a fur
 * mantle and mail, a long sword in his left hand. His first phase is a sword duel: his blade turns aside blows from the
 * front and arrows, so only a parry opens him up. He cuts once or twice in quick succession; an orange-glowing blow only a
 * raised shield stops, and is knocked out by it; red-glowing blows a shield only halves, one of them from over the head,
 * parried as it starts down; from four blocks off a lunge, parried just before the stab; a burst of blue force from his
 * right hand that throws the target far back; and from afar, now and then, a bolt of lightning from the sword's point that
 * burns through armor and makes the body jerk. His eyes flicker purple and blue before he vanishes and stands behind the
 * target, and he hops aside and back.
 * <p>
 * Struck down, his sword crumbles into ravens and comes together again as a staff: in his second phase, the lord of the
 * undead, he keeps his distance as Baba Yaga does in hers, vanishes and hops away, shoots blue bolts from the staff that
 * only a shield stops, sweeps one who comes close away with it, and raises draugr out of the ground, faster and frailer
 * than those of the ice caves: with souls thrown over the arena, or round one he lets walk up to him before he vanishes.
 * He also looses a wide snow storm that pushes back and burns those without a shield, three ravens of ice of which one
 * knocks a shield out, now and then a ball of lightning that hunts the target until struck down, and a wall of ice
 * before a target that runs at him.
 * <p>
 * Struck down again, a shell of ice bursts off him, his staff snaps and he rises undead with two daggers: slow, but darting
 * left, right, in and back, round to the target's back, vanishing to stand behind or before it. He cuts three times
 * quickly, strikes an orange blow that knocks a shield out and makes one bleed, leaps with both blades red, dashes with
 * them out ahead, throws them on chains to drag the target in, and feints: a leap at the target and behind it again.
 * Killed in this phase he shrinks away into his needle. Numbers are in {@code data/eam/mobs/koschei.json}.
 */
public class Koschei extends Monster implements BuildingAware {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.BYTE);
	/** The phase he fights in, from 1. */
	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.BYTE);
	/** Id of the entity his lightning holds on, or -1. */
	/** Ids of the entities his lightning holds on, -1 for an empty slot: every player in his sight when he looses it. */
	@SuppressWarnings("unchecked")
	private static final EntityDataAccessor<Integer>[] DATA_BOLT_TARGETS = new EntityDataAccessor[] {
			SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT),
			SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT),
			SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT),
			SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT)
	};
	/** His lunge just knocked a shield out: a hit now parries him. The client lights his eyes yellow. */
	private static final EntityDataAccessor<Boolean> DATA_PARRY_OPEN = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.BOOLEAN);
	/** The overhead blow under way lost its parry: the player kept clicking at him while he raised it. No yellow eyes then. */
	private static final EntityDataAccessor<Boolean> DATA_PARRY_LOST = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.BOOLEAN);
	/** Third phase: stacks of his fury. */
	private static final EntityDataAccessor<Byte> DATA_FURY = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.BYTE);
	/** Third phase: where the heads of his chains are, in the world, while they are out. */
	private static final EntityDataAccessor<Vector3fc> DATA_CHAIN_TIP = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.VECTOR3);
	/** Where the blow he turns aside comes at him, packed by {@link #packGuard}; the client holds the sword there. */
	private static final EntityDataAccessor<Integer> DATA_GUARD = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT);
	/** Hits further than this flat distance past their reach still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** The lunge stops this close to the target and stabs from there. */
	private static final double LUNGE_STOP = 1.8;
	private static final double LUNGE_SPEED = 0.8;
	/** The stab reaches only this far: he has to rush in close for it to go in. */
	private static final double LUNGE_REACH = 2.05;
	/** A hop pushes for this many ticks; the rest of the action is the landing. */
	private static final int HOP_PUSH_TICKS = 3;
	private static final double HOP_LIFT = 0.35;
	/** The bolt breaks off if the target gets this much further than the bolt's range. */
	private static final double BOLT_SLACK = 4.0;
	/** He runs at the target this fast while he winds up the orange blow, and it reaches this much further. */
	private static final double ORANGE_CHASE_SPEED = 0.42;
	private static final double ORANGE_REACH_BONUS = 0.6;
	/** The burst still catches the one he casts at this much further than its range. */
	private static final double FORCE_AIMED_SLACK = 3.0;
	/** How many ravens scatter from where he vanishes. */
	private static final int RAVENS = 28;
	/** He walks in this fast while the sword is raised over his head or drawn back for the red blow. */
	private static final double OVERHEAD_WALK_SPEED = 0.2;
	/** The kick's step in stops this close to the target. */
	private static final double KICK_STOP = 1.4;
	/** A dash round to the target's side or back lands this far from it. */
	private static final double FLANK_DISTANCE = 2.2;
	/** And this far round from where it faces, at least and at most, degrees. */
	private static final float FLANK_MIN_ANGLE = 80.0F;
	private static final float FLANK_MAX_ANGLE = 160.0F;
	private static final double FLANK_LIFT = 0.15;
	/** He counts his draugr this far round him. */
	private static final double THRALL_RANGE = 48.0;
	/** The staff's sweep throws the target off its feet for about this many ticks. */
	private static final int STRIKE_AIR_TICKS = 8;
	private static final double STRIKE_LIFT = 0.3;
	/** A soul is in the air about this long, ticks. */
	private static final int SOUL_FLIGHT = 26;
	/** Killed in his third phase, he shrinks away into his needle over this many ticks. */
	public static final int DEATH_TICKS = 30;
	/** His leaps rise this high over the straight line from where he jumps to where he lands. */
	private static final double LEAP_HEIGHT = 2.5;
	/** The dash with the blades out ahead runs this fast, and drives them in this close to the target. */
	private static final double BLADE_DASH_SPEED = 1.0;
	private static final double BLADE_DASH_STOP = 1.9;
	/** The chains drag the target in until it is this close. */
	private static final double CHAINS_STOP = 2.0;
	/** His dash trail: dark red motes. */
	private static final DustParticleOptions DASH_RED = new DustParticleOptions(0x7A0F1E, 1.2F);

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

	private KoscheiAction action = KoscheiAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private final ParryStreak parryStreak = new ParryStreak();
	private int idleTicks;
	private final int[] cooldowns = new int[Attack.values().length];
	private @Nullable Attack lastAttack;
	/** Times in a row the target stood too close for a lunge he wanted. */
	private int lungeFails;
	private int hopCooldown;
	/** Speed of the hop under way, blocks per tick. */
	private double hopSpeed;
	/** The hop under way is aimed at a spot: it stops there instead of coasting on. */
	private boolean hopAimed;
	private int flankCooldown;
	/** The hop under way is a dash round the target: the blow follows as soon as he lands. */
	private boolean flankStrike;
	/** Ticks his bolt has been ready with a target about and not cast. */
	private int boltHunger;
	/** Whose shield he last knocked out, and the game time it is ready again. */
	private @Nullable UUID shieldDownOf;
	private long shieldBackAt;
	/** Hits a player landed on him while he raised the overhead blow under way. */
	private int windupHits;
	private int teleportCooldown;
	/** Ticks his target has stood out of his reach above him, on a pillar or a ledge. */
	private int highTicks;
	/** Where the teleport under way takes him: room by a target above him, or null to stand behind it. */
	private @Nullable Vec3 teleportSpot;
	/** The phantom ravens he called up that are still about. */
	private final List<KoscheiRaven> flock = new ArrayList<>();
	/** Damage he took lately: when and how much, oldest first. */
	private final ArrayDeque<Hit> recentHits = new ArrayDeque<>();
	/** Where the one who hurt him fast stands: he leaps back from there on his next tick, after the hit's knockback. */
	private @Nullable Vec3 leapFrom;
	/** Which way a hop or the lunge goes. */
	private Vec3 rushDirection = Vec3.ZERO;
	private boolean actionHit;
	/** The bolt has burnt the target already: the convulsions are on. */
	private final List<LivingEntity> boltTargets = new ArrayList<>();
	/** Ids of those the bolt has burnt already: their convulsions are on. */
	private final Set<Integer> boltLanded = new HashSet<>();
	/** The player whose shield his lunge just knocked out, and the game time until which their hit parries him. */
	private @Nullable UUID parryPlayer;
	private long parryUntil;
	private boolean parryPenalized;
	/** Client: the tick count when the current action started. */
	private int clientActionStart;
	/** Second phase: ticks left for each {@link StaffAttack}, by ordinal. */
	private final int[] staffCooldowns = new int[StaffAttack.values().length];
	/** Third phase: ticks left for each {@link DaggerAttack}, by ordinal. */
	private final int[] daggerCooldowns = new int[DaggerAttack.values().length];
	/** Third phase: ticks the target has kept out of his reach. */
	private int outOfReach;
	/** Third phase: ticks since he was last hit or his fury last cooled a stack. */
	private int furyCalm;
	/** Third phase: cuts a shield blocked in a row, and the game time of the last. */
	private int blockedCuts;
	private long lastBlockedCut = Long.MIN_VALUE / 2;
	private int dartCooldown;
	/** Third phase: the pause since his last attack when the dart under way started; a dart does not put off his next blow. */
	private int idleBeforeDart;
	/** Third phase: the teleport under way puts him before the target rather than behind it. */
	private boolean teleportFront;
	/** Third phase: a leap under way, from where to where, from which tick of the action and for how long. */
	private Vec3 flightFrom = Vec3.ZERO;
	private Vec3 flightTo = Vec3.ZERO;
	private int flightStart;
	private int flightTicks;
	/** Third phase: his chains, while they are out. */
	private Vec3 chainTip = Vec3.ZERO;
	private Vec3 chainDirection = Vec3.ZERO;
	private ChainState chainState = ChainState.OUT;
	/** Third phase: where his dash with the blades out ahead was aimed. */
	private Vec3 dashTarget = Vec3.ZERO;
	/** Killed in his third phase: what killed him, for his needle once he has shrunk away into it. */
	private @Nullable DamageSource deathSource;
	/** Client: where he stood the last few ticks, newest first, for the trail his darts leave. */
	private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
	/** Second phase: ticks he still waits with the trap laid; 0 when it is not. */
	private int trapWait;
	/** Second phase: the teleport under way takes him away from the target rather than behind it. */
	private boolean teleportAway;
	/** Second phase: hits taken in a row, and the game time of the last; enough of them and he vanishes. */
	private int hitStreak;
	private long lastHitAt;
	/** His draugr up, counted now and then. */
	private int thrallCount;
	/** His ball of lightning while it is about. */
	private @Nullable KoscheiBallLightning ball;
	/** His ice ravens still in the air. */
	private final List<KoscheiIceRaven> iceRavens = new ArrayList<>();
	/** Ticks left for the row of his shield bearers; 0 when he hides behind none. */
	private int turtleTicks;
	private final List<Draugr> bearers = new ArrayList<>();
	private final List<KoscheiIceCopy> copies = new ArrayList<>();
	/** Times he has drained a draugr's strength in this phase. */
	private int drainsUsed;
	private @Nullable Draugr drained;
	/** Where he stood when he rose for the drain. */
	private double drainGround;

	public Koschei(EntityType<? extends Koschei> type, Level level) {
		super(type, level);
		xpReward = settings().stats().xp();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
		bossEvent.setName(barName());
	}

	public static AttributeSupplier.Builder createAttributes() {
		KoscheiSettings.Stats stats = KoscheiSettings.DEFAULT.stats();
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, stats.health())
				.add(Attributes.ARMOR, stats.armor())
				.add(Attributes.MOVEMENT_SPEED, stats.speed())
				.add(Attributes.FOLLOW_RANGE, stats.followRange())
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.ATTACK_DAMAGE, KoscheiSettings.DEFAULT.phaseOne().strike().damage())
				.add(Attributes.ATTACK_SPEED, stats.attackSpeed())
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	protected KoscheiSettings settings() {
		return KoscheiModule.settings();
	}

	private KoscheiSettings.PhaseOne phaseOne() {
		return settings().phaseOne();
	}

	PhaseTwoSettings phaseTwo() {
		return settings().phaseTwo();
	}

	PhaseThreeSettings phaseThree() {
		return settings().phaseThree();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_PHASE, (byte) 1);
		for (EntityDataAccessor<Integer> slot : DATA_BOLT_TARGETS) {
			builder.define(slot, -1);
		}
		builder.define(DATA_PARRY_OPEN, false);
		builder.define(DATA_PARRY_LOST, false);
		builder.define(DATA_FURY, (byte) 0);
		builder.define(DATA_CHAIN_TIP, new Vector3f());
		builder.define(DATA_GUARD, packGuard(0.6F, 0.0F));
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new KoscheiFightGoal(this));
		goalSelector.addGoal(2, new KoscheiStaffGoal(this));
		goalSelector.addGoal(2, new KoscheiDaggerGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
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
		// His sword is in his left hand, the right is free for the burst of force.
		setLeftHanded(true);
		setCanPickUpLoot(false);
		applyStats(true);
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		// His sword is part of his model.
	}

	private void applyStats(boolean full) {
		KoscheiSettings.Stats stats = settings().stats();
		int phase = phase();
		setBase(Attributes.MAX_HEALTH, switch (phase) {
			case 1 -> stats.health();
			case 2 -> phaseTwo().health();
			default -> phaseThree().health();
		});
		setBase(Attributes.ARMOR, stats.armor());
		setBase(Attributes.MOVEMENT_SPEED, stats.speed() * switch (phase) {
			case 1 -> 1.0;
			case 2 -> phaseTwo().speedMultiplier();
			default -> phaseThree().speedMultiplier();
		});
		setBase(Attributes.FOLLOW_RANGE, stats.followRange());
		setBase(Attributes.ATTACK_SPEED, stats.attackSpeed());
		setHealth(full ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
		xpReward = stats.xp();
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	// ---- State for the goal and the client ----

	public KoscheiAction action() {
		return KoscheiAction.byId(entityData.get(DATA_ACTION));
	}

	/** The phase he fights in: 1 with the sword, 2 with the staff, 3 with the daggers. */
	public int phase() {
		return entityData.get(DATA_PHASE);
	}

	/** Client: what his lightning holds on, or null. */
	public List<Entity> boltTargets() {
		List<Entity> targets = new ArrayList<>();
		for (EntityDataAccessor<Integer> slot : DATA_BOLT_TARGETS) {
			int id = entityData.get(slot);
			Entity entity = id < 0 ? null : level().getEntity(id);
			if (entity != null) {
				targets.add(entity);
			}
		}
		return targets;
	}

	/** Client: whether a hit now parries him, after his lunge knocked a shield out. */
	public boolean parryOpen() {
		return entityData.get(DATA_PARRY_OPEN);
	}

	/** Client: the overhead blow under way can't be parried any more, the player clicked at him through its windup. */
	public boolean parryLost() {
		return entityData.get(DATA_PARRY_LOST);
	}

	/** Client: how high the blow he turns aside comes at him, 0 at his feet to 1 at the top of his head. */
	public float guardHeight() {
		return (entityData.get(DATA_GUARD) / 1000) / 100.0F;
	}

	/** Client: which side the blow he turns aside comes from, -1 his right to 1 his left. */
	public float guardSide() {
		return (entityData.get(DATA_GUARD) % 1000 - 100) / 100.0F;
	}

	private static int packGuard(float height, float side) {
		int h = Math.round(Mth.clamp(height, 0.0F, 1.0F) * 100.0F);
		int s = Math.round(Mth.clamp(side, -1.0F, 1.0F) * 100.0F) + 100;
		return h * 1000 + s;
	}

	/** Client: ticks since the current action started, with the partial tick. */
	public float actionTime(float partialTicks) {
		return tickCount - clientActionStart + partialTicks;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			clientActionStart = tickCount;
		}
	}

	public boolean isBusy() {
		return action != KoscheiAction.NONE;
	}

	/** Ready to attack: no action for one swing of his sword. */
	public boolean readyToAttack() {
		return action == KoscheiAction.NONE && idleTicks >= attackPause();
	}

	private int attackPause() {
		return switch (phase()) {
			case 1 -> settings().stats().attackPause();
			case 2 -> phaseTwo().attackPause();
			default -> {
				PhaseThreeSettings.Rage rage = phaseThree().rage();
				yield KoscheiLogic.furyPause(phaseThree().attackPause(), fury(), rage.pauseCut(), rage.minPause());
			}
		};
	}

	/** Third phase: stacks of his fury, from the hits he took lately; his eyes burn red while he has any. */
	public int fury() {
		return entityData.get(DATA_FURY);
	}

	/** Third phase: notes whether the target is in his reach, for how fast he chases it. */
	void noteReach(boolean inReach) {
		outOfReach = inReach ? 0 : outOfReach + 1;
	}

	/** Third phase: ticks the target has kept out of his reach. */
	int outOfReach() {
		return outOfReach;
	}

	/** Third phase: how many times his speed he runs at the target now, the chase and his fury together. */
	double chaseSpeed() {
		PhaseThreeSettings three = phaseThree();
		PhaseThreeSettings.Chase chase = three.chase();
		return KoscheiLogic.chaseSpeed(outOfReach, chase.after(), chase.ramp(), chase.maxMultiplier()) * (1.0 + fury() * three.rage().speedBonus());
	}

	/** Third phase: the dagger attack he starts now at a target {@code distance} off, or null. */
	@Nullable DaggerAttack chooseDagger(double distance, boolean sees) {
		int room = phaseTwo().thralls().cap() - thrallCount;
		return KoscheiLogic.chooseDagger(distance, sees, room, daggerCooldowns, phaseThree(), random::nextDouble);
	}

	void startDagger(DaggerAttack attack, LivingEntity target) {
		PhaseThreeSettings three = phaseThree();
		actionTarget = target;
		actionHit = false;
		daggerCooldowns[attack.ordinal()] = switch (attack) {
			case COMBO -> 0;
			case ORANGE -> three.orange().cooldown();
			case LEAP -> three.leap().cooldown();
			case DASH -> three.dash().cooldown();
			case CHAINS -> three.chains().cooldown();
			case FEINT -> three.feint().cooldown();
			case RAISE -> three.thralls().cooldown();
		};
		setAction(switch (attack) {
			case COMBO -> KoscheiAction.DAGGER_COMBO;
			case ORANGE -> KoscheiAction.DAGGER_ORANGE;
			case LEAP -> KoscheiAction.DAGGER_LEAP;
			case DASH -> KoscheiAction.DAGGER_DASH;
			case CHAINS -> KoscheiAction.CHAINS;
			case FEINT -> KoscheiAction.DAGGER_FEINT;
			case RAISE -> KoscheiAction.RAISE;
		});
	}

	/** Third phase: whether a dart is ready. */
	boolean dartReady() {
		return dartCooldown <= 0 && onGround();
	}

	/** Third phase: a dart to one side or back from the target, or in at it from {@code in} true. */
	void dart(LivingEntity target, boolean in) {
		Vec3 toward = horizontal(target.position().subtract(position()));
		Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
		double roll = random.nextDouble();
		Vec3 direction = in ? toward.add(side.scale((roll - 0.5) * 0.8)) : roll < 0.4 ? side : roll < 0.8 ? side.scale(-1.0) : toward.scale(-1.0);
		if (!dartReady() || direction.lengthSqr() < 1.0E-6) {
			return;
		}
		PhaseThreeSettings.Moves moves = phaseThree().moves();
		double distance = in ? Math.min(moves.dartDistance(), Math.max(0.0, horizontalDistanceTo(target) - 2.0)) : moves.dartDistance();
		if (distance < 0.5) {
			return;
		}
		dartCooldown = moves.dartCooldown();
		idleBeforeDart = idleTicks;
		hopSpeed = distance / KoscheiAction.DART_PUSH;
		rushDirection = direction.normalize();
		actionTarget = target;
		getNavigation().stop();
		setAction(KoscheiAction.DART);
		playSound(SoundEvents.BREEZE_JUMP, 0.8F, 1.6F + random.nextFloat() * 0.2F);
	}

	/** Third phase: a dash round to the target's back, the blow following as he lands. */
	boolean flankInPhaseThree(LivingEntity target) {
		boolean dashed = flankDash(target);
		if (dashed) {
			flankCooldown = phaseThree().moves().flankCooldown();
			playSound(SoundEvents.BREEZE_JUMP, 0.8F, 1.4F);
		}
		return dashed;
	}

	/** Third phase: whether he vanishes to stand by the target now, ready or not. */
	void startTeleportThree(LivingEntity target) {
		PhaseThreeSettings.Moves moves = phaseThree().moves();
		startTeleport(target);
		teleportCooldown = moves.teleportCooldown();
		teleportFront = random.nextFloat() < moves.frontShare();
	}

	int[] cooldowns() {
		return cooldowns;
	}

	@Nullable
	Attack lastAttack() {
		return lastAttack;
	}

	int lungeFails() {
		return lungeFails;
	}

	void lungeFailed() {
		lungeFails++;
	}

	int hopCooldown() {
		return hopCooldown;
	}

	boolean teleportReady() {
		return teleportCooldown <= 0;
	}

	/** Ticks {@code target} has held a shield up toward him; 0 when it holds none up. */
	int shieldTicks(LivingEntity target) {
		return target.isUsingItem() && target.getUseItem().has(DataComponents.BLOCKS_ATTACKS)
				&& FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ()) ? target.getTicksUsingItem() : 0;
	}

	/** Whether the target's shield is not knocked out: one it holds, or could raise, is not cooling down. */
	static boolean shieldReady(LivingEntity target) {
		if (!(target instanceof Player player)) {
			return true;
		}
		for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
			if (stack.has(DataComponents.BLOCKS_ATTACKS) && player.getCooldowns().isOnCooldown(stack)) {
				return false;
			}
		}
		return true;
	}

	// ---- Client effects ----

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			KoscheiEffects.tick(this);
			trail.addFirst(position());
			while (trail.size() > 6) {
				trail.removeLast();
			}
		}
	}

	/** Client: where he stood the last few ticks, newest first. */
	public List<Vec3> trail() {
		return List.copyOf(trail);
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) cooldowns[i]--;
		}
		if (hopCooldown > 0) hopCooldown--;
		if (flankCooldown > 0) flankCooldown--;
		if (getTarget() != null && cooldowns[Attack.BOLT.ordinal()] <= 0) {
			boltHunger++;
		}
		if (parryPlayer != null && level.getGameTime() > parryUntil) {
			closeParry();
		}
		if (teleportCooldown > 0) teleportCooldown--;
		if (dartCooldown > 0) dartCooldown--;
		for (int i = 0; i < staffCooldowns.length; i++) {
			if (staffCooldowns[i] > 0) staffCooldowns[i]--;
		}
		if (trapWait > 0) trapWait--;
		for (int i = 0; i < daggerCooldowns.length; i++) {
			if (daggerCooldowns[i] > 0) daggerCooldowns[i]--;
		}
		if (fury() > 0 && ++furyCalm >= phaseThree().rage().decay()) {
			// Left alone a while, his fury cools a stack.
			furyCalm = 0;
			entityData.set(DATA_FURY, (byte) (fury() - 1));
		}
		if (phase() >= 2 && tickCount % 10 == 0) {
			thrallCount = countThralls(level);
		}
		if (ball != null && !ball.isAlive()) {
			ball = null;
		}
		iceRavens.removeIf(raven -> !raven.isAlive());
		copies.removeIf(copy -> !copy.isAlive());
		if (!copies.isEmpty()) {
			// No new copies while some stand: the cooldown counts from when the last has shattered.
			staffCooldowns[StaffAttack.ICE_COPIES.ordinal()] = Math.max(staffCooldowns[StaffAttack.ICE_COPIES.ordinal()],
					phaseTwo().iceCopies().cooldown());
		}
		tickTurtle();
		if (!iceRavens.isEmpty()) {
			// No new ravens while some are in the air: the cooldown counts from when the last is gone.
			staffCooldowns[StaffAttack.ICE_RAVENS.ordinal()] = Math.max(staffCooldowns[StaffAttack.ICE_RAVENS.ordinal()],
					phaseTwo().iceRavens().cooldown());
		}
		LivingEntity current = getTarget();
		highTicks = current != null && current.isAlive() && current.getY() - getY() > settings().height().above() ? highTicks + 1 : 0;
		flock.removeIf(raven -> !raven.isAlive());
		if (!flock.isEmpty()) {
			// No new flock while one is about: the cooldown counts from when the last of it is gone.
			cooldowns[Attack.RAVENS.ordinal()] = Math.max(cooldowns[Attack.RAVENS.ordinal()], settings().ravens().cooldown());
		}
		if (leapFrom != null) {
			leapBack(leapFrom);
			leapFrom = null;
		}
		LivingEntity target = getTarget();
		if (target == null && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(settings().stats().regen());
		}
		if (action != KoscheiAction.DRAIN && action != KoscheiAction.DAGGER_LEAP && action != KoscheiAction.DAGGER_FEINT && isNoGravity()) {
			// Only the drain and his leaps hold him up: never left hanging after them, however they ended.
			setNoGravity(false);
		}
		if (action == KoscheiAction.NONE) {
			idleTicks++;
		} else {
			tickAction(level);
		}
		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	public void startAttack(Attack attack, LivingEntity target) {
		KoscheiSettings.PhaseOne one = phaseOne();
		actionTarget = target;
		actionHit = false;
		if (attack != Attack.MAKE_ROOM) {
			lastAttack = attack;
		}
		switch (attack) {
			case STRIKE -> setAction(KoscheiAction.STRIKE);
			case DOUBLE -> {
				cooldowns[attack.ordinal()] = one.doubleStrike().cooldown();
				setAction(KoscheiAction.DOUBLE);
			}
			case ORANGE -> {
				cooldowns[attack.ordinal()] = one.orange().cooldown();
				setAction(KoscheiAction.ORANGE);
			}
			case RED -> {
				cooldowns[attack.ordinal()] = one.red().cooldown();
				setAction(KoscheiAction.RED);
			}
			case OVERHEAD -> {
				cooldowns[attack.ordinal()] = one.overhead().cooldown();
				setAction(KoscheiAction.OVERHEAD);
			}
			case KICK -> {
				cooldowns[attack.ordinal()] = one.kick().cooldown();
				setAction(KoscheiAction.KICK);
			}
			case LUNGE -> {
				lungeFails = 0;
				cooldowns[attack.ordinal()] = one.lunge().cooldown();
				setAction(KoscheiAction.LUNGE);
			}
			case FORCE -> {
				cooldowns[attack.ordinal()] = one.force().cooldown();
				setAction(KoscheiAction.FORCE);
			}
			case BOLT -> {
				cooldowns[attack.ordinal()] = one.bolt().cooldown();
				boltHunger = 0;
				boltLanded.clear();
				clearBolt();
				setAction(KoscheiAction.BOLT);
			}
			case RAVENS -> {
				cooldowns[attack.ordinal()] = settings().ravens().cooldown();
				setAction(KoscheiAction.SUMMON);
			}
			case MAKE_ROOM -> {
				// Room for the lunge he keeps wanting: the burst of force if it is ready, else a hop back.
				lungeFails = 0;
				cooldowns[Attack.LUNGE.ordinal()] = 0;
				if (cooldowns[Attack.FORCE.ordinal()] <= 0) {
					startAttack(Attack.FORCE, target);
				} else {
					hop(horizontal(position().subtract(target.position())), target);
				}
			}
		}
	}

	private void setAction(KoscheiAction next) {
		if (action == KoscheiAction.DRAIN && next != KoscheiAction.DRAIN) {
			// Down from the air.
			setNoGravity(false);
			resetFallDistance();
			drained = null;
		}
		action = next;
		actionTicks = 0;
		windupHits = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
		entityData.set(DATA_PARRY_LOST, false);
	}

	private void endAction() {
		setAction(KoscheiAction.NONE);
		actionTarget = null;
		idleTicks = 0;
		clearBolt();
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= action.length()) {
			KoscheiAction ended = action;
			endAction();
			if (ended == KoscheiAction.STAGGER && phase() == 1 && target != null && target.isAlive()) {
				// Back on his feet after a parry: he throws the one who parried him off.
				actionTarget = target;
				cooldowns[Attack.FORCE.ordinal()] = phaseOne().force().cooldown();
				setAction(KoscheiAction.FORCE);
			} else if (ended == KoscheiAction.DART) {
				idleTicks = idleBeforeDart + KoscheiAction.DART.length();
			} else if (ended == KoscheiAction.TELEPORT || ended == KoscheiAction.HOP && flankStrike) {
				// Behind the target or at its side: the next blow comes at once.
				idleTicks = phase() >= 3 ? attackPause() : settings().stats().attackPause();
			}
			flankStrike = false;
			return;
		}
		boolean free = action == KoscheiAction.STAGGER || action == KoscheiAction.PARRY_ARROW || action == KoscheiAction.BLOCK
				|| action == KoscheiAction.HOP || action == KoscheiAction.TRANSFORM || action == KoscheiAction.DRAIN
				|| action == KoscheiAction.SHATTER || action == KoscheiAction.DART;
		if (!free && (target == null || !target.isAlive())) {
			endAction();
			return;
		}
		if (action != KoscheiAction.HOP && action != KoscheiAction.DART) {
			getNavigation().stop();
		}
		if (target != null && action != KoscheiAction.STAGGER && action != KoscheiAction.HOP && action != KoscheiAction.DART
				&& !(action == KoscheiAction.DAGGER_DASH && actionTicks > KoscheiAction.DASH_GO)) {
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			faceTowards(target);
		}
		KoscheiSettings.PhaseOne one = phaseOne();
		int parryStart = action.parryStart(parryWindow(action));
		if (t == parryStart && parryStart >= 0 && !entityData.get(DATA_PARRY_LOST)) {
			// A bright ping: the moment to parry him.
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 1.5F, 0.9F);
		}
		switch (action) {
			case STRIKE -> {
				if (t == KoscheiAction.STRIKE_HIT) {
					swing(level, target, one.strike().damage(), 1.0F);
				}
			}
			case DOUBLE -> {
				for (int hit : KoscheiAction.DOUBLE_HITS) {
					if (t == hit) {
						swing(level, target, one.doubleStrike().damage(), 1.1F + t * 0.02F);
					}
				}
			}
			case ORANGE -> tickOrange(level, t, target, one.orange());
			case RED -> {
				if (t == 0) {
					redCue(level);
				}
				if (t < KoscheiAction.RED_HIT && target != null) {
					// As with the overhead blow: stepping back does not take the target out of reach.
					walkIn(target, OVERHEAD_WALK_SPEED);
				}
				if (t == KoscheiAction.RED_HIT) {
					cleave(level, target, one.red());
				}
			}
			case OVERHEAD -> {
				if (t == 0) {
					redCue(level);
				}
				if (t < KoscheiAction.OVERHEAD_HIT && target != null) {
					// He keeps walking in with the sword raised, so stepping back does not take the target out of reach.
					walkIn(target, OVERHEAD_WALK_SPEED);
				}
				if (t == KoscheiAction.OVERHEAD_HIT) {
					cleave(level, target, one.overhead());
				}
			}
			case LUNGE -> tickLunge(level, t, target, one.lunge());
			case KICK -> tickKick(level, t, target, one.kick());
			case FORCE -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.7F);
				} else if (t == KoscheiAction.FORCE_RELEASE) {
					burst(level, target, one.force());
				}
			}
			case BOLT -> tickBolt(level, t, target, one.bolt());
			case TELEPORT -> {
				if (t == 0) {
					playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.0F, 0.6F);
				} else if (t == action.length() - 1 && target != null && teleportAway) {
					teleportAway = false;
					teleportAwayFrom(level, target, phaseTwo().teleport().distance());
				} else if (t == action.length() - 1 && target != null && phase() >= 3) {
					if (teleportFront) {
						standBefore(level, target);
					} else {
						teleportBehind(level, target, settings().teleport());
					}
					teleportFront = false;
				} else if (t == action.length() - 1 && target != null) {
					// Looked for again: the target may have come down or moved on up there meanwhile.
					Vec3 spot = teleportSpot != null && target.getY() - getY() > settings().height().above()
							? roomBy(level, target, settings().height().spotRange()) : null;
					if (spot != null) {
						teleportTo(level, spot, target);
					} else {
						teleportBehind(level, target, settings().teleport());
					}
					teleportSpot = null;
				}
			}
			case SUMMON -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.6F);
				} else if (t == KoscheiAction.SUMMON_RELEASE && target != null) {
					summonRavens(level, target, settings().ravens());
				}
			}
			case TRANSFORM -> tickTransform(level, t);
			case STAFF_BOLT -> {
				if (t == 0) {
					level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, getSoundSource(), 1.2F, 1.5F);
				} else if (t == KoscheiAction.STAFF_BOLT_RELEASE && target != null) {
					shootBolt(level, target, phaseTwo().bolt());
				}
			}
			case STAFF_STRIKE -> {
				if (t == 0) {
					playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.2F, 0.6F);
				} else if (t == KoscheiAction.STAFF_STRIKE_HIT && target != null) {
					staffStrike(level, target, phaseTwo().strike());
				}
			}
			case SOULS -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.5F);
				} else if (t == KoscheiAction.SOULS_RELEASE) {
					throwSouls(level, phaseTwo());
				}
			}
			case SNOWSTORM -> tickStorm(level, t, phaseTwo().storm());
			case ICE_RAVENS -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 1.3F);
				} else if (t == KoscheiAction.ICE_RAVENS_RELEASE && target != null) {
					loosIceRavens(level, target, phaseTwo().iceRavens());
				}
			}
			case BALL -> {
				if (t == 0) {
					level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BEACON_ACTIVATE, getSoundSource(), 1.5F, 1.8F);
				} else if (t == KoscheiAction.BALL_RELEASE && target != null) {
					callBall(level, target, phaseTwo().ball());
				}
			}
			case ICE_WALL -> {
				if (t == KoscheiAction.ICE_WALL_RELEASE && target != null) {
					raiseIceWall(level, target, phaseTwo().iceWall());
				}
			}
			case TURTLE -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.45F);
				} else if (t == KoscheiAction.TURTLE_RELEASE && target != null) {
					raiseBearers(level, target, phaseTwo());
				}
			}
			case ICE_COPIES -> {
				if (t == 0) {
					playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.5F, 1.2F);
				} else if (t == KoscheiAction.ICE_COPIES_RELEASE && target != null) {
					makeCopies(level, target, phaseTwo().iceCopies());
				}
			}
			case DRAIN -> tickDrain(level, t, phaseTwo().drain());
			case SHATTER -> tickShatter(level, t);
			case DAGGER_COMBO -> {
				if (t == 0) {
					playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.6F);
				}
				if (target != null && t < KoscheiAction.COMBO_HITS[2]) {
					walkIn(target, OVERHEAD_WALK_SPEED, phaseThree().combo().reach());
				}
				for (int hit : KoscheiAction.COMBO_HITS) {
					if (t == hit) {
						comboCut(level, target, phaseThree().combo(), 1.2F + t * 0.03F);
					}
				}
			}
			case DAGGER_ORANGE -> tickDaggerOrange(level, t, target, phaseThree().orange());
			case DAGGER_LEAP -> tickLeap(level, t, target, phaseThree().leap());
			case DAGGER_DASH -> tickBladeDash(level, t, target, phaseThree().dash());
			case CHAINS -> tickChains(level, t, target, phaseThree().chains());
			case DAGGER_FEINT -> tickFeint(level, t, target, phaseThree().feint());
			case DART -> {
				if (t < KoscheiAction.DART_PUSH && !horizontalCollision) {
					setDeltaMovement(rushDirection.x * hopSpeed, Math.min(getDeltaMovement().y, 0.0), rushDirection.z * hopSpeed);
					dashTrail(level);
				} else if (t == KoscheiAction.DART_PUSH) {
					setDeltaMovement(getDeltaMovement().multiply(0.15, 1.0, 0.15));
				}
			}
			case RAISE -> {
				if (t == 0) {
					playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.4F);
				} else if (t == KoscheiAction.RAISE_RELEASE && target != null) {
					raiseFar(level, target, phaseThree());
				}
			}
			case TRAP -> {
				if (t == 0) {
					playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.0F, 0.5F);
				} else if (t == action.length() - 1 && target != null) {
					springTrap(level, target, phaseTwo());
				}
			}
			case HOP -> {
				if (t < HOP_PUSH_TICKS && !horizontalCollision) {
					double lift = flankStrike ? FLANK_LIFT : HOP_LIFT;
					setDeltaMovement(rushDirection.x * hopSpeed, t == 0 ? lift : getDeltaMovement().y, rushDirection.z * hopSpeed);
				} else if (t == HOP_PUSH_TICKS && hopAimed) {
					// At the spot it was aimed at: no coasting past it.
					setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
				}
			}
			default -> {
			}
		}
	}

	private int parryWindow(KoscheiAction of) {
		KoscheiSettings.PhaseOne one = phaseOne();
		return switch (of) {
			case LUNGE -> one.lunge().parryWindow();
			case OVERHEAD -> one.overhead().parryWindow();
			default -> 0;
		};
	}

	// ---- Blows ----

	/** A plain cut: a raised shield blocks it as it blocks any blow. */
	private void swing(ServerLevel level, @Nullable LivingEntity target, float damage, float pitch) {
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, pitch * 0.8F);
		if (target != null && inReach(target, phaseOne().reach())) {
			target.damageCooldownTime = 0;
			strike(level, target, damageSources().mobAttack(this), damage, 0.4F);
		}
	}

	/** The orange cue: a new rising, ringing sound; the blade's glow and sparks are drawn by the client. */
	private void tickOrange(ServerLevel level, int t, @Nullable LivingEntity target, KoscheiSettings.Orange orange) {
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.KOSCHEI_ORANGE_CUE, getSoundSource(), 2.0F, 1.0F);
		}
		if (target == null) {
			return;
		}
		if (t < KoscheiAction.ORANGE_HIT) {
			// He runs the target down as he winds up: stepping back does not get away from it, only a shield stops it.
			walkIn(target, ORANGE_CHASE_SPEED);
			return;
		}
		if (t != KoscheiAction.ORANGE_HIT) {
			return;
		}
		setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.6F);
		if (!inReach(target, phaseOne().reach() + ORANGE_REACH_BONUS)) {
			return;
		}
		ItemStack shield = blockingShield(target);
		if (shield != null) {
			// The shield takes it all and flies out of the hand.
			knockShieldOut(level, target, shield, orange.shieldSeconds());
			return;
		}
		target.damageCooldownTime = 0;
		strike(level, target, damageSources().mobAttack(this), orange.damage(), 0.6F);
	}

	/** Closes in on the target during a blow's windup, stopping once it is well within his reach. */
	private void walkIn(LivingEntity target, double speed) {
		walkIn(target, speed, phaseOne().reach());
	}

	/** As {@link #walkIn(LivingEntity, double)}, for a blow that reaches {@code reach}. */
	private void walkIn(LivingEntity target, double speed, double reach) {
		if (horizontalDistanceTo(target) > reach - 0.8 && onGround()) {
			Vec3 to = horizontal(target.position().subtract(position()));
			setDeltaMovement(to.x * speed, getDeltaMovement().y, to.z * speed);
		} else {
			setDeltaMovement(getDeltaMovement().multiply(0.3, 1.0, 0.3));
		}
	}

	private void redCue(ServerLevel level) {
		level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_DASH_WARNING, getSoundSource(), 2.0F, 0.7F);
	}

	/** A red blow: a raised shield only takes its share off. */
	private void cleave(ServerLevel level, @Nullable LivingEntity target, KoscheiSettings.Shielded blow) {
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.55F);
		if (target == null || !inReach(target, phaseOne().reach())) {
			return;
		}
		boolean shield = blockingShield(target) != null;
		target.damageCooldownTime = 0;
		strike(level, target, damageSources().source(KoscheiModule.CLEAVE_DAMAGE, this),
				KoscheiLogic.shielded(blow.damage(), shield, blow.shieldReduction()), 0.6F);
		if (shield) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.6F);
		}
	}

	/** Drawn back, a rush in that stops short of the target, the stab, and a hop back if it struck. */
	private void tickLunge(ServerLevel level, int t, @Nullable LivingEntity target, KoscheiSettings.Lunge lunge) {
		if (target == null) {
			return;
		}
		if (t == KoscheiAction.LUNGE_PUSH) {
			rushDirection = horizontal(target.position().subtract(position()));
			playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.2F, 0.7F);
		}
		if (t >= KoscheiAction.LUNGE_PUSH && t < KoscheiAction.LUNGE_RUSH_END) {
			if (horizontalDistanceTo(target) > LUNGE_STOP) {
				rushDirection = horizontal(target.position().subtract(position()));
				setDeltaMovement(rushDirection.x * LUNGE_SPEED, getDeltaMovement().y, rushDirection.z * LUNGE_SPEED);
			} else {
				setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
			}
		} else if (t == KoscheiAction.LUNGE_STAB) {
			setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
			playSound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.7F);
			if (inReach(target, LUNGE_REACH)) {
				ItemStack shield = blockingShield(target);
				if (shield != null && target instanceof Player player) {
					// As the Fallen Lord's thrust: the shield takes the stab and is knocked out, and a quick hit back parries him.
					knockShieldOut(level, target, shield, lunge.shieldSeconds());
					openParry(level, player, lunge.shieldParryWindow());
				} else {
					target.damageCooldownTime = 0;
					actionHit = strike(level, target, damageSources().mobAttack(this), lunge.damage(), 0.5F);
				}
			}
		} else if (t == KoscheiAction.LUNGE_STAB + 2 && actionHit) {
			// Out of reach again after a stab that went in.
			setDeltaMovement(-rushDirection.x * 0.7, 0.3, -rushDirection.z * 0.7);
		}
	}

	/** A step in to the target, then the kick: it knocks a raised shield out and lands, as the Fallen Lord's does. */
	private void tickKick(ServerLevel level, int t, @Nullable LivingEntity target, KoscheiSettings.Kick kick) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2F, 0.7F);
		}
		if (t < KoscheiAction.KICK_HIT) {
			Vec3 to = horizontal(target.position().subtract(position()));
			double speed = horizontalDistanceTo(target) > KICK_STOP ? 0.5 : 0.0;
			setDeltaMovement(to.x * speed, getDeltaMovement().y, to.z * speed);
		} else if (t == KoscheiAction.KICK_HIT) {
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
			if (!inReach(target, kick.reach())) {
				return;
			}
			playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
			ItemStack shield = target.getItemBlockingWith();
			if (shield != null) {
				knockShieldOut(level, target, shield, kick.shieldSeconds());
			}
			target.damageCooldownTime = 0;
			// The boot goes past where the shield was: a shield never takes the kick.
			strike(level, target, damageSources().source(KoscheiModule.CLEAVE_DAMAGE, this), kick.damage(), kick.knockback());
		}
	}

	/** The burst of force from his right hand: everyone in front of him is thrown back, less far behind a shield. */
	private void burst(ServerLevel level, @Nullable LivingEntity target, KoscheiSettings.Force force) {
		if (target != null) {
			// Turned square on the one he casts at, so the burst does not go past them.
			float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(yaw);
			setYBodyRot(yaw);
			setYHeadRot(yaw);
		}
		Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
		Vec3 hand = rightHand();
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), getSoundSource(), 2.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.5F, 0.6F);
		// A cone of blue motes rushing out from the palm.
		for (int i = 0; i < 40; i++) {
			double spread = 0.35;
			Vec3 dir = facing.add((random.nextDouble() - 0.5) * spread * 2.0, (random.nextDouble() - 0.5) * spread, (random.nextDouble() - 0.5) * spread * 2.0)
					.normalize().scale(0.6 + random.nextDouble() * 0.5);
			level.sendParticles(EamParticles.KOSCHEI_FORCE, hand.x, hand.y, hand.z, 0, dir.x, dir.y, dir.z, 1.0);
		}
		double rise = Math.max(2.5, settings().height().forceRise());
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(force.range() + FORCE_AIMED_SLACK, rise, force.range() + FORCE_AIMED_SLACK),
				e -> e != this && e.isAlive() && !(e instanceof ArmorStand) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
			Vec3 to = victim.position().subtract(position());
			Vec3 flat = horizontal(to);
			if (flat == Vec3.ZERO) {
				flat = facing;
			}
			// The one he casts at is caught unless a wall is between them, even a few steps out of range; others in front of him too.
			double up = victim.getY() - getY();
			boolean aimed = victim == target && horizontalDistanceTo(victim) <= force.range() + FORCE_AIMED_SLACK && up > -2.5
					&& up <= rise;
			// Up on a pillar too: the one he casts at is thrown off it.
			boolean inCone = horizontalDistanceTo(victim) <= force.range() + victim.getBbWidth() * 0.5 && flat.dot(facing) >= 0.4
					&& Math.abs(victim.getY() - getY()) < 2.5;
			if (!(aimed || inCone) || !hasLineOfSight(victim)) {
				continue;
			}
			boolean shield = blockingShield(victim) != null;
			double speed = KoscheiLogic.pushSpeed(shield ? force.shieldPush() : force.push(), force.airTicks());
			victim.setDeltaMovement(flat.x * speed, force.lift(), flat.z * speed);
			victim.syncVelocity = true;
			victim.needsSync = true;
			if (shield) {
				level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.5F);
			}
		}
	}

	/** The bolt: sparks gather at the sword's point, then the lightning holds on the target and burns it in pulses. */
	private void tickBolt(ServerLevel level, int t, @Nullable LivingEntity target, KoscheiSettings.Bolt bolt) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BEACON_ACTIVATE, getSoundSource(), 2.0F, 1.6F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, getSoundSource(), 1.5F, 1.2F);
		}
		if (t < KoscheiAction.BOLT_RELEASE) {
			return;
		}
		if (t == KoscheiAction.BOLT_RELEASE) {
			// It leaps to every player in his sight at once, the target first.
			if (holdsBolt(target, bolt) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
				boltTargets.add(target);
			}
			Vec3 facing = Vec3.directionFromRotation(0.0F, yBodyRot);
			for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(bolt.maxRange(), 6.0, bolt.maxRange()),
					p -> p != target && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p))) {
				if (boltTargets.size() >= DATA_BOLT_TARGETS.length) {
					break;
				}
				if (horizontalDistanceTo(player) <= bolt.maxRange() && horizontal(player.position().subtract(position())).dot(facing) >= 0.0
						&& hasLineOfSight(player)) {
					boltTargets.add(player);
				}
			}
			if (boltTargets.isEmpty()) {
				return;
			}
			syncBolt();
			for (LivingEntity struck : boltTargets) {
				level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, getSoundSource(), 1.5F, 1.4F);
			}
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.TRIDENT_THUNDER.value(), getSoundSource(), 0.8F, 1.6F);
		}
		if (boltTargets.isEmpty()) {
			return;
		}
		if (t >= KoscheiAction.BOLT_END) {
			clearBolt();
			return;
		}
		// Each one it loses (out of range, out of sight, dead) is let go.
		if (boltTargets.removeIf(struck -> !struck.isAlive() || !holdsBolt(struck, bolt))) {
			syncBolt();
		}
		boolean pulse = (t - KoscheiAction.BOLT_RELEASE) % bolt.pulse() == 0;
		Vec3 tip = swordTip();
		for (LivingEntity struck : boltTargets) {
			Vec3 shield = shieldAgainstBolt(struck, tip);
			if (shield != null) {
				// A shield raised his way takes the lightning: it still crackles against the shield, but burns no one and the
				// convulsions stop.
				struck.removeEffect(KoscheiModule.CONVULSIONS);
				if (pulse) {
					level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 1.5F);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, shield.x, shield.y, shield.z, 14, 0.2, 0.3, 0.2, 0.25);
				}
				continue;
			}
			if (t % 4 == 0) {
				level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, getSoundSource(), 0.5F,
						1.8F + random.nextFloat() * 0.2F);
			}
			if (!pulse) {
				continue;
			}
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, struck.getX(), struck.getY(0.6), struck.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
			struck.damageCooldownTime = 0;
			strike(level, struck, damageSources().source(KoscheiModule.LIGHTNING_DAMAGE, this),
					KoscheiLogic.boltPulse(bolt.damagePerSecond(), bolt.pulse()), 0.0F);
			if (boltLanded.add(struck.getId())) {
				struck.addEffect(new MobEffectInstance(KoscheiModule.CONVULSIONS, KoscheiAction.BOLT_END - t, 0, false, true, true), this);
			}
		}
	}

	/**
	 * Where {@code struck} holds a shield up to the lightning, or null. Any shield turned his way counts, since the
	 * convulsions keep jerking the view about.
	 */
	private static @Nullable Vec3 shieldAgainstBolt(LivingEntity struck, Vec3 tip) {
		ItemStack shield = struck.getItemBlockingWith();
		if (shield == null || !shield.has(DataComponents.BLOCKS_ATTACKS)) {
			return null;
		}
		Vec3 look = horizontal(struck.getLookAngle());
		if (look.dot(horizontal(tip.subtract(struck.position()))) < 0.0) {
			return null;
		}
		return new Vec3(struck.getX() + look.x * 0.6, struck.getY(0.6), struck.getZ() + look.z * 0.6);
	}

	/** Whether his lightning reaches {@code entity}: in range and in sight. */
	private boolean holdsBolt(LivingEntity entity, KoscheiSettings.Bolt bolt) {
		return horizontalDistanceTo(entity) <= bolt.maxRange() + BOLT_SLACK && hasLineOfSight(entity);
	}

	private void syncBolt() {
		for (int i = 0; i < DATA_BOLT_TARGETS.length; i++) {
			entityData.set(DATA_BOLT_TARGETS[i], i < boltTargets.size() ? boltTargets.get(i).getId() : -1);
		}
	}

	private void clearBolt() {
		boltTargets.clear();
		syncBolt();
	}

	// ---- Second phase ----

	/**
	 * Struck down in his first phase: whatever he was doing breaks off, the boss bar turns green and he rises again with
	 * the staff through {@link KoscheiAction#TRANSFORM}.
	 */
	private void startTransform(ServerLevel level) {
		closeParry();
		clearBolt();
		getNavigation().stop();
		recentHits.clear();
		leapFrom = null;
		entityData.set(DATA_PHASE, (byte) 2);
		applyStats(false);
		bossEvent.setColor(BossEvent.BossBarColor.GREEN);
		bossEvent.setName(barName());
		Arrays.fill(staffCooldowns, 0);
		trapWait = 0;
		drainsUsed = 0;
		LivingEntity target = getTarget();
		setAction(KoscheiAction.TRANSFORM);
		actionTarget = target;
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, getSoundSource(), 2.0F, 0.4F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 2.0F, 0.5F);
	}

	/** The sword crumbles into ravens, they wheel about him and come together as the staff; his health fills meanwhile. */
	private void tickTransform(ServerLevel level, int t) {
		setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
		int length = KoscheiAction.TRANSFORM.length();
		setHealth(KoscheiLogic.transformHealth(t + 1, length, 1.0F, getMaxHealth()));
		Vec3 centre = position().add(0.0, getBbHeight() * 0.6, 0.0);
		if (t == KoscheiAction.TRANSFORM_CRUMBLE) {
			Vec3 tip = swordTip();
			level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.ITEM_BREAK.value(), getSoundSource(), 1.5F, 0.5F);
			ravens(level, position());
		}
		if (t > KoscheiAction.TRANSFORM_CRUMBLE && t < KoscheiAction.TRANSFORM_STAFF && t % 2 == 0) {
			// Ravens and green motes wheel round him.
			double angle = t * 0.6;
			for (int i = 0; i < 3; i++) {
				double a = angle + i * Math.PI * 2.0 / 3.0;
				level.sendParticles(EamParticles.KOSCHEI_RAVEN, centre.x + Math.cos(a) * 1.4, centre.y + (i - 1) * 0.5, centre.z + Math.sin(a) * 1.4, 0,
						-Math.sin(a) * 0.3, 0.05, Math.cos(a) * 0.3, 1.0);
			}
			level.sendParticles(ParticleTypes.SOUL, centre.x, centre.y, centre.z, 3, 0.8, 0.8, 0.8, 0.02);
		}
		if (t == KoscheiAction.TRANSFORM_STAFF) {
			Vec3 hand = raisedHand();
			level.playSound(null, hand.x, hand.y, hand.z, EamSounds.KOSCHEI_RAVENS, getSoundSource(), 2.0F, 0.7F);
			level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.ZOMBIE_VILLAGER_CURE, getSoundSource(), 1.0F, 1.4F);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centre.x, centre.y, centre.z, 30, 0.6, 0.9, 0.6, 0.05);
			warpCloud(level, position(), 0.6);
		}
		if (t == length - 1) {
			setHealth(getMaxHealth());
			// He opens his second phase by raising the dead.
			staffCooldowns[StaffAttack.SOULS.ordinal()] = 0;
			idleTicks = phaseTwo().attackPause();
		}
	}

	// ---- Third phase ----

	/**
	 * Struck down in his second phase: whatever he was doing breaks off, the boss bar turns red, a shell of ice closes round
	 * him and bursts, his staff snaps, and he rises undead with two daggers through {@link KoscheiAction#SHATTER}.
	 */
	private void startShatter(ServerLevel level) {
		getNavigation().stop();
		recentHits.clear();
		leapFrom = null;
		hitStreak = 0;
		trapWait = 0;
		setNoGravity(false);
		entityData.set(DATA_PHASE, (byte) 3);
		entityData.set(DATA_FURY, (byte) 0);
		applyStats(false);
		bossEvent.setColor(BossEvent.BossBarColor.RED);
		bossEvent.setName(barName());
		blockedCuts = 0;
		Arrays.fill(daggerCooldowns, 0);
		// He opens his third phase with the daggers, his draugr later.
		daggerCooldowns[DaggerAttack.RAISE.ordinal()] = phaseThree().thralls().cooldown() / 2;
		daggerCooldowns[DaggerAttack.FEINT.ordinal()] = phaseThree().feint().cooldown() / 3;
		outOfReach = 0;
		LivingEntity target = getTarget();
		setAction(KoscheiAction.SHATTER);
		actionTarget = target;
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GLASS_BREAK, getSoundSource(), 2.0F, 0.4F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITHER_AMBIENT, getSoundSource(), 1.2F, 0.5F);
	}

	/**
	 * Ice gathers round him into a shell, the staff snaps in his hand inside it, and the shell bursts outward, throwing
	 * back those close; his health fills meanwhile.
	 */
	private void tickShatter(ServerLevel level, int t) {
		setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
		int length = KoscheiAction.SHATTER.length();
		setHealth(KoscheiLogic.transformHealth(t + 1, length, 1.0F, getMaxHealth()));
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		double radius = getBbHeight() * 0.62;
		if (t < KoscheiAction.SHATTER_BURST && t % 2 == 0) {
			// The shell closes: more panes each time, until it is whole.
			int panes = 20 + t * 4;
			double golden = Math.PI * (3.0 - Math.sqrt(5.0));
			for (int i = 0; i < panes; i++) {
				double y = 1.0 - (i + 0.5) * 2.0 / panes;
				double ring = Math.sqrt(1.0 - y * y);
				double angle = i * golden + t * 0.05;
				level.sendParticles(EamParticles.KOSCHEI_ICE_FIELD, centre.x + Math.cos(angle) * ring * radius * 0.75, centre.y + y * radius,
						centre.z + Math.sin(angle) * ring * radius * 0.75, 1, 0.0, 0.0, 0.0, 0.0);
			}
			if (t % 6 == 0) {
				level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_BLOCK_RESONATE, getSoundSource(), 1.5F, 0.6F + t * 0.02F);
			}
		}
		if (t == KoscheiAction.SHATTER_STAFF) {
			Vec3 hand = raisedHand();
			level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.ITEM_BREAK.value(), getSoundSource(), 2.0F, 0.4F);
			level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.WOOD_BREAK, getSoundSource(), 2.0F, 0.5F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DARK_OAK_PLANKS.defaultBlockState()), hand.x, hand.y, hand.z, 25,
					0.2, 0.5, 0.2, 0.15);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y + 0.8, hand.z, 20, 0.2, 0.2, 0.2, 0.3);
		}
		if (t == KoscheiAction.SHATTER_BURST) {
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GLASS_BREAK, getSoundSource(), 3.0F, 0.5F);
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_BLOCK_BREAK, getSoundSource(), 3.0F, 0.6F);
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), getSoundSource(), 1.0F, 1.6F);
			// Shards of the shell flung out on all sides, like a broken eggshell.
			for (int i = 0; i < 90; i++) {
				Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.3, random.nextGaussian()).normalize();
				Vec3 at = centre.add(dir.scale(radius * 0.8));
				level.sendParticles(EamParticles.KOSCHEI_ICE_FIELD, at.x, at.y, at.z, 0, dir.x, dir.y, dir.z, 0.5 + random.nextDouble() * 0.5);
			}
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), centre.x, centre.y, centre.z, 120,
					radius * 0.6, radius * 0.6, radius * 0.6, 0.6);
			level.sendParticles(EamParticles.KOSCHEI_SNOWFLAKE, centre.x, centre.y, centre.z, 60, radius * 0.5, radius * 0.5, radius * 0.5, 0.3);
			level.sendParticles(ParticleTypes.SOUL, centre.x, centre.y, centre.z, 20, 0.5, 0.9, 0.5, 0.05);
			for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0),
					e -> e != this && e.isAlive() && !(e instanceof Koschei) && !(e instanceof Draugr draugr && draugr.servedBy(this)))) {
				Vec3 away = horizontal(near.position().subtract(position()));
				double speed = KoscheiLogic.pushSpeed(3.0, STRIKE_AIR_TICKS);
				near.setDeltaMovement(away.x * speed, 0.35, away.z * speed);
				near.syncVelocity = true;
				near.needsSync = true;
			}
		}
		if (t == length - 1) {
			setHealth(getMaxHealth());
			idleTicks = attackPause();
		}
	}

	/**
	 * One of his three quick cuts. A shield blocks it as any blow, but enough of them blocked close together knock it out.
	 */
	private void comboCut(ServerLevel level, @Nullable LivingEntity target, PhaseThreeSettings.Combo combo, float pitch) {
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, pitch);
		if (target == null || !inReach(target, combo.reach())) {
			return;
		}
		ItemStack shield = blockingShield(target);
		target.damageCooldownTime = 0;
		if (shield != null) {
			long now = level.getGameTime();
			blockedCuts = KoscheiLogic.blockedInRow(blockedCuts, lastBlockedCut, now, combo.shieldMemory());
			lastBlockedCut = now;
			if (blockedCuts >= combo.shieldHits()) {
				blockedCuts = 0;
				knockShieldOut(level, target, shield, combo.shieldSeconds());
				return;
			}
		}
		if (strike(level, target, damageSources().mobAttack(this), combo.damage(), 0.2F)) {
			level.playSound(null, target.getX(), target.getY(0.6), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, getSoundSource(), 1.0F, 1.2F);
		}
	}

	/** The orange blow with a dagger: he runs in as the blade flickers, one heavy cut; a shield is knocked out, flesh bleeds. */
	private void tickDaggerOrange(ServerLevel level, int t, @Nullable LivingEntity target, PhaseThreeSettings.Orange orange) {
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.KOSCHEI_ORANGE_CUE, getSoundSource(), 2.0F, 1.2F);
		}
		if (target == null) {
			return;
		}
		double reach = phaseThree().combo().reach();
		if (t < KoscheiAction.DAGGER_ORANGE_HIT) {
			walkIn(target, ORANGE_CHASE_SPEED, reach);
			return;
		}
		if (t != KoscheiAction.DAGGER_ORANGE_HIT) {
			return;
		}
		setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.4F, 0.8F);
		if (!inReach(target, reach + ORANGE_REACH_BONUS)) {
			return;
		}
		ItemStack shield = blockingShield(target);
		if (shield != null) {
			knockShieldOut(level, target, shield, orange.shieldSeconds());
			return;
		}
		target.damageCooldownTime = 0;
		if (strike(level, target, damageSources().mobAttack(this), orange.damage(), 0.4F)) {
			BleedingModule.bleed(target, this, orange.bleedingTicks());
			level.sendParticles(new DustParticleOptions(0x9E0B0B, 1.0F), target.getX(), target.getY(0.6), target.getZ(), 14, 0.25, 0.3, 0.25, 0.0);
		}
	}

	/**
	 * The red leap: crouched with both blades glowing red, a leap at the target, and both brought down as he lands. A
	 * shield raised as he hangs at the top stops it; one held up all along, or raised too late, lets most of it through.
	 */
	private void tickLeap(ServerLevel level, int t, @Nullable LivingEntity target, PhaseThreeSettings.Leap leap) {
		if (t == 0) {
			redCue(level);
		}
		if (target == null) {
			return;
		}
		if (t == KoscheiAction.LEAP_GO) {
			Vec3 back = horizontal(position().subtract(target.position()));
			startFlight(target.position().add(back.scale(1.2)), t, KoscheiAction.LEAP_LAND - KoscheiAction.LEAP_GO);
			playSound(SoundEvents.BREEZE_JUMP, 1.4F, 0.7F);
		}
		if (t == KoscheiAction.LEAP_APEX - leap.blockEarly()) {
			// A bright ping: the moment to raise the shield.
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 1.5F, 1.1F);
		}
		if (t > KoscheiAction.LEAP_GO && t <= KoscheiAction.LEAP_LAND) {
			fly(t);
		}
		if (t != KoscheiAction.LEAP_LAND) {
			return;
		}
		land();
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.5F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 30, 0.8, 0.1, 0.8, 0.15);
		if (!inReach(target, phaseThree().combo().reach() + 0.8)) {
			return;
		}
		ItemStack shield = blockingShield(target);
		target.damageCooldownTime = 0;
		if (shield != null && KoscheiLogic.leapBlockedInTime(target.getTicksUsingItem(), KoscheiAction.LEAP_LAND - KoscheiAction.LEAP_APEX,
				leap.blockEarly(), leap.blockLate())) {
			// Raised in time: the shield takes it all.
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.2F, 0.7F);
			level.sendParticles(EamParticles.KOSCHEI_SPARK, target.getX(), target.getY(0.6), target.getZ(), 16, 0.2, 0.2, 0.2, 0.25);
			return;
		}
		float damage = shield != null ? leap.damage() * leap.shieldShare() : leap.damage();
		strike(level, target, damageSources().source(KoscheiModule.CLEAVE_DAMAGE, this), damage, 0.5F);
		if (shield != null) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 0.8F, 1.3F);
		}
	}

	/** Plans a leap from where he stands to {@code to}, starting on action tick {@code start} and lasting {@code ticks}. */
	private void startFlight(Vec3 to, int start, int ticks) {
		flightFrom = position();
		flightTo = to;
		flightStart = start;
		flightTicks = Math.max(1, ticks);
		setNoGravity(true);
	}

	/** One tick along the leap planned: up in an arc and down on its end. */
	private void fly(int t) {
		double share = Math.clamp((t + 1 - flightStart) / (double) flightTicks, 0.0, 1.0);
		double y = flightFrom.y + KoscheiLogic.leapHeight(share, flightTo.y - flightFrom.y, LEAP_HEIGHT);
		Vec3 at = new Vec3(Mth.lerp(share, flightFrom.x, flightTo.x), y, Mth.lerp(share, flightFrom.z, flightTo.z));
		setNoGravity(true);
		setDeltaMovement(at.subtract(position()));
		resetFallDistance();
	}

	/** Down from a leap. */
	private void land() {
		setNoGravity(false);
		setDeltaMovement(getDeltaMovement().multiply(0.0, 0.0, 0.0));
		resetFallDistance();
	}

	/**
	 * Both blades out ahead, a dash straight at where the target stood, and both driven in point-blank. A shield takes them
	 * and is knocked out, and a quick hit back parries him; his cuts follow at once either way.
	 */
	private void tickBladeDash(ServerLevel level, int t, @Nullable LivingEntity target, PhaseThreeSettings.Dash dash) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 1.3F);
		}
		if (t < KoscheiAction.DASH_GO) {
			return;
		}
		if (t == KoscheiAction.DASH_GO) {
			rushDirection = horizontal(target.position().subtract(position()));
			dashTarget = target.position();
			playSound(SoundEvents.BREEZE_JUMP, 1.4F, 0.9F);
		}
		if (horizontalDistanceTo(target) <= BLADE_DASH_STOP && Math.abs(target.getY() - getY()) < 2.5 && inFront(target)) {
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.9F);
			ItemStack shield = blockingShield(target);
			if (shield != null && target instanceof Player player) {
				knockShieldOut(level, target, shield, dash.shieldSeconds());
				openParry(level, player, dash.parryWindow());
			} else {
				target.damageCooldownTime = 0;
				strike(level, target, damageSources().mobAttack(this), dash.damage(), 0.3F);
			}
			// Straight into his cuts while the target reels.
			setAction(KoscheiAction.DAGGER_COMBO);
			return;
		}
		Vec3 left = dashTarget.subtract(position());
		if (t > KoscheiAction.DASH_GO && (horizontalCollision || horizontal(left).dot(rushDirection) <= 0.0)) {
			setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
			endAction();
			return;
		}
		setDeltaMovement(rushDirection.x * BLADE_DASH_SPEED, getDeltaMovement().y, rushDirection.z * BLADE_DASH_SPEED);
		dashTrail(level);
	}

	/** How his chains go: flying out, coming back empty, or dragging the target in. */
	private enum ChainState { OUT, BACK, PULL }

	/**
	 * Both daggers flung on their chains at the target as it stands: one that steps aside lets them fly by. Caught, it is
	 * dragged in to him and his cuts follow.
	 */
	private void tickChains(ServerLevel level, int t, @Nullable LivingEntity target, PhaseThreeSettings.Chains chains) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.5F, 0.6F);
		}
		if (t < KoscheiAction.CHAINS_THROW) {
			return;
		}
		Vec3 hands = chainHands();
		if (t == KoscheiAction.CHAINS_THROW) {
			chainTip = hands;
			chainDirection = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(hands).normalize();
			chainState = ChainState.OUT;
			playSound(SoundEvents.TRIDENT_THROW.value(), 1.5F, 0.6F);
		}
		switch (chainState) {
			case OUT -> {
				Vec3 next = chainTip.add(chainDirection.scale(chains.speed()));
				if (target.getBoundingBox().inflate(0.35).clip(chainTip, next).isPresent()) {
					// Caught: the hooks bite and the pull begins. A shield takes the bite, not the pull.
					chainState = ChainState.PULL;
					chainTip = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
					target.damageCooldownTime = 0;
					strike(level, target, damageSources().mobAttack(this), chains.hookDamage(), 0.0F);
					level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, getSoundSource(), 1.5F, 0.7F);
					level.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), getSoundSource(), 2.0F, 0.5F);
				} else {
					HitResult wall = level.clip(new ClipContext(chainTip, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
					if (wall.getType() != HitResult.Type.MISS) {
						chainTip = wall.getLocation();
						chainState = ChainState.BACK;
						level.playSound(null, chainTip.x, chainTip.y, chainTip.z, SoundEvents.ANVIL_LAND, getSoundSource(), 0.4F, 1.8F);
					} else {
						chainTip = next;
						if (chainTip.distanceTo(hands) >= chains.range()) {
							chainState = ChainState.BACK;
						}
					}
				}
			}
			case BACK -> {
				Vec3 home = hands.subtract(chainTip);
				double step = chains.speed() * 1.5;
				if (home.length() <= step) {
					endAction();
					return;
				}
				chainTip = chainTip.add(home.normalize().scale(step));
			}
			case PULL -> {
				chainTip = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
				if (horizontalDistanceTo(target) <= CHAINS_STOP || t >= KoscheiAction.CHAINS.length() - 2) {
					target.setDeltaMovement(target.getDeltaMovement().multiply(0.1, 1.0, 0.1));
					syncMotion(target);
					setAction(KoscheiAction.DAGGER_COMBO);
					return;
				}
				Vec3 in = horizontal(position().subtract(target.position()));
				target.setDeltaMovement(in.x * chains.pullSpeed(), Math.max(0.08, target.getDeltaMovement().y), in.z * chains.pullSpeed());
				target.resetFallDistance();
				syncMotion(target);
			}
		}
		entityData.set(DATA_CHAIN_TIP, new Vector3f((float) chainTip.x, (float) chainTip.y, (float) chainTip.z));
	}

	/** Sends the motion just set on {@code entity} to its client, a player's above all. */
	private static void syncMotion(Entity entity) {
		entity.syncVelocity = true;
		entity.needsSync = true;
	}

	/** Where his chains run from: between his fists, held out before his chest. */
	private Vec3 chainHands() {
		return position().add(Vec3.directionFromRotation(0.0F, yBodyRot).scale(0.6)).add(0.0, getBbHeight() * 0.55, 0.0);
	}

	/** Client: where the heads of his chains are, in the world. */
	public Vec3 chainTip() {
		Vector3fc tip = entityData.get(DATA_CHAIN_TIP);
		return new Vec3(tip.x(), tip.y(), tip.z());
	}

	/**
	 * The feint: his eyes flicker and he stands behind the target, leaps at it, vanishes at the top of the leap to stand
	 * behind it again, and cuts. One who turned to meet the leap has to turn back in time.
	 */
	private void tickFeint(ServerLevel level, int t, @Nullable LivingEntity target, PhaseThreeSettings.Feint feint) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.0F, 0.8F);
		}
		if (t == KoscheiAction.FEINT_WARP) {
			standBehind(level, target, 4.5);
		}
		if (t == KoscheiAction.FEINT_GO) {
			Vec3 back = horizontal(position().subtract(target.position()));
			startFlight(target.position().add(back.scale(1.2)), t, (KoscheiAction.FEINT_APEX - KoscheiAction.FEINT_GO) * 2);
			playSound(SoundEvents.BREEZE_JUMP, 1.4F, 0.8F);
		}
		if (t > KoscheiAction.FEINT_GO && t < KoscheiAction.FEINT_APEX) {
			fly(t);
		}
		if (t == KoscheiAction.FEINT_APEX) {
			land();
			standBehind(level, target, 2.2);
		}
		if (t == KoscheiAction.FEINT_HIT) {
			playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.9F);
			if (inReach(target, phaseThree().combo().reach() + 0.3)) {
				target.damageCooldownTime = 0;
				strike(level, target, damageSources().mobAttack(this), feint.damage(), 0.3F);
			}
		}
	}

	/** Vanishes and stands about {@code distance} behind the target, as it faces now; true if there was room. */
	private boolean standBehind(ServerLevel level, LivingEntity target, double distance) {
		for (float angle : KoscheiLogic.teleportAngles(settings().teleport().maxAngle(), random::nextDouble)) {
			Vec3 dir = Vec3.directionFromRotation(0.0F, target.getYHeadRot() + 180.0F + angle);
			Vec3 spot = findStandingSpot(level, target.position().add(dir.scale(distance)));
			if (spot != null) {
				teleportTo(level, spot, target);
				return true;
			}
		}
		return false;
	}

	/** Vanishes and stands right before the target, facing it; behind it if there is no room before it. */
	private void standBefore(ServerLevel level, LivingEntity target) {
		for (float angle : KoscheiLogic.teleportAngles(30.0F, random::nextDouble)) {
			Vec3 dir = Vec3.directionFromRotation(0.0F, target.getYHeadRot() + angle);
			Vec3 spot = findStandingSpot(level, target.position().add(dir.scale(2.0)));
			if (spot != null) {
				teleportTo(level, spot, target);
				return;
			}
		}
		teleportBehind(level, target, settings().teleport());
	}

	/** The trail of a dart or a dash: dark red motes and wisps of souls left where he passes. */
	private void dashTrail(ServerLevel level) {
		level.sendParticles(DASH_RED, getX(), getY(0.5), getZ(), 6, 0.25, getBbHeight() * 0.3, 0.25, 0.0);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.3), getZ(), 1, 0.2, 0.2, 0.2, 0.01);
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.2), getZ(), 2, 0.2, 0.1, 0.2, 0.01);
	}

	/** Souls fly out of him and come down far round the target; his draugr climb out where they land, at their own speed. */
	private void raiseFar(ServerLevel level, LivingEntity target, PhaseThreeSettings three) {
		PhaseThreeSettings.Thralls settings = three.thralls();
		Vec3 chest = position().add(0.0, getBbHeight() * 0.7, 0.0);
		level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.5F, 0.5F);
		level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 2.0F, 0.5F);
		level.sendParticles(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z, 12, 0.3, 0.4, 0.3, 0.05);
		int count = KoscheiLogic.thrallsToRaise(settings.count(), countThralls(level), phaseTwo().thralls().cap());
		double start = random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < count; i++) {
			double angle = start + i * Math.PI * 2.0 / Math.max(1, count) + (random.nextDouble() - 0.5) * 0.6;
			double distance = Mth.lerp(random.nextDouble(), settings.minDistance(), settings.maxDistance());
			Vec3 spot = findStandingSpot(level, target.position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance));
			if (spot != null) {
				throwSoul(level, chest, spot);
			}
		}
	}

	/** A green soul thrown out in an arc from {@code from}, to raise one of his draugr at {@code spot} where it comes down. */
	private void throwSoul(ServerLevel level, Vec3 from, Vec3 spot) {
		Vec3 way = spot.subtract(from);
		double vy = (way.y + KoscheiOrb.SOUL_GRAVITY * SOUL_FLIGHT * (SOUL_FLIGHT + 1) / 2.0) / SOUL_FLIGHT;
		level.addFreshEntity(new KoscheiOrb(level, this, KoscheiOrb.Kind.SOUL, from, new Vec3(way.x / SOUL_FLIGHT, vy, way.z / SOUL_FLIGHT)));
	}

	int[] staffCooldowns() {
		return staffCooldowns;
	}

	int thrallCount() {
		return thrallCount;
	}

	/** Starts one of his staff attacks on {@code target}; the trap is only laid, he waits for the target to come to him. */
	void startStaff(StaffAttack attack, LivingEntity target) {
		PhaseTwoSettings two = phaseTwo();
		actionTarget = target;
		switch (attack) {
			case BOLT -> {
				staffCooldowns[attack.ordinal()] = two.bolt().cooldown();
				setAction(KoscheiAction.STAFF_BOLT);
			}
			case STRIKE -> {
				staffCooldowns[attack.ordinal()] = two.strike().cooldown();
				setAction(KoscheiAction.STAFF_STRIKE);
			}
			case SOULS -> {
				staffCooldowns[attack.ordinal()] = two.souls().cooldown();
				setAction(KoscheiAction.SOULS);
			}
			case TRAP -> {
				staffCooldowns[attack.ordinal()] = two.trap().cooldown();
				trapWait = two.trap().waitTicks();
				actionTarget = null;
			}
			case BALL -> {
				staffCooldowns[attack.ordinal()] = two.ball().cooldown();
				setAction(KoscheiAction.BALL);
			}
			case STORM -> {
				staffCooldowns[attack.ordinal()] = two.storm().cooldown();
				setAction(KoscheiAction.SNOWSTORM);
			}
			case ICE_RAVENS -> {
				staffCooldowns[attack.ordinal()] = two.iceRavens().cooldown();
				setAction(KoscheiAction.ICE_RAVENS);
			}
			case ICE_WALL -> {
				staffCooldowns[attack.ordinal()] = two.iceWall().cooldown();
				setAction(KoscheiAction.ICE_WALL);
			}
			case TURTLE -> {
				staffCooldowns[attack.ordinal()] = two.turtle().cooldown();
				setAction(KoscheiAction.TURTLE);
			}
			case ICE_COPIES -> {
				staffCooldowns[attack.ordinal()] = two.iceCopies().cooldown();
				setAction(KoscheiAction.ICE_COPIES);
			}
			case DRAIN -> {
				if (!(level() instanceof ServerLevel level)) {
					return;
				}
				List<Draugr> near = drainable(level, two.drain());
				if (near.isEmpty()) {
					return;
				}
				staffCooldowns[attack.ordinal()] = two.drain().cooldown();
				drainsUsed++;
				drained = near.get(random.nextInt(near.size()));
				drainGround = getY();
				setAction(KoscheiAction.DRAIN);
			}
		}
	}

	/** What he does next with the staff, or null to keep moving; see {@link KoscheiLogic#chooseStaff}. */
	@Nullable StaffAttack chooseStaff(LivingEntity target, double distance, boolean sees, boolean rushing) {
		PhaseTwoSettings two = phaseTwo();
		boolean canDrain = KoscheiLogic.mayDrain(getHealth(), getMaxHealth(), drainsUsed, two.drain()) && level() instanceof ServerLevel level
				&& !drainable(level, two.drain()).isEmpty();
		KoscheiLogic.StaffSituation now = new KoscheiLogic.StaffSituation(distance, sees, thrallCount, rushing, ballAbout(), !copies.isEmpty(),
				turtling(), canDrain);
		return KoscheiLogic.chooseStaff(now, staffCooldowns, two, random::nextDouble);
	}

	/** Whether he hides behind a row of his shield bearers now. */
	boolean turtling() {
		return turtleTicks > 0 && !bearers.isEmpty();
	}

	/** Keeps his shield bearers in their row between him and the target while it lasts. */
	private void tickTurtle() {
		bearers.removeIf(bearer -> !bearer.isAlive());
		if (turtleTicks <= 0) {
			return;
		}
		turtleTicks--;
		LivingEntity target = getTarget();
		if (bearers.isEmpty() || target == null || !target.isAlive()) {
			if (bearers.isEmpty()) {
				turtleTicks = 0;
			}
			return;
		}
		PhaseTwoSettings.Turtle turtle = phaseTwo().turtle();
		double[][] at = new double[bearers.size()][];
		for (int i = 0; i < at.length; i++) {
			at[i] = new double[] {bearers.get(i).getX(), bearers.get(i).getZ()};
		}
		double[][] row = LostLogic.wallSpots(getX(), getZ(), target.getX(), target.getZ(), at, turtle.spacing(), turtle.frontDistance());
		for (int i = 0; i < row.length; i++) {
			bearers.get(i).holdWall(new Vec3(row[i][0], getY(), row[i][1]), target, turtle.reach());
		}
	}

	/** Draugr with shields climb out of the ground in a row between him and the target. */
	private void raiseBearers(ServerLevel level, LivingEntity target, PhaseTwoSettings two) {
		PhaseTwoSettings.Turtle turtle = two.turtle();
		int count = KoscheiLogic.thrallsToRaise(turtle.count(), countThralls(level), two.thralls().cap());
		Vec3 toTarget = horizontal(target.position().subtract(position()));
		if (toTarget == Vec3.ZERO) {
			toTarget = Vec3.directionFromRotation(0.0F, yBodyRot);
		}
		Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x);
		Vec3 centre = position().add(toTarget.scale(Math.min(turtle.frontDistance(), horizontalDistanceTo(target) * 0.5)));
		bearers.removeIf(bearer -> !bearer.isAlive());
		for (int i = 0; i < count; i++) {
			double offset = (i - (count - 1) * 0.5) * turtle.spacing();
			Draugr bearer = raiseThrall(level, centre.add(side.scale(offset)), true);
			if (bearer != null) {
				bearers.add(bearer);
			}
		}
		if (!bearers.isEmpty()) {
			turtleTicks = turtle.duration();
			level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.SHIELD_BLOCK.value(), getSoundSource(), 1.5F, 0.6F);
		}
	}

	/** His copies of ice stand up at his sides, facing the target. */
	private void makeCopies(ServerLevel level, LivingEntity target, PhaseTwoSettings.IceCopies settings) {
		Vec3 toTarget = horizontal(target.position().subtract(position()));
		if (toTarget == Vec3.ZERO) {
			toTarget = Vec3.directionFromRotation(0.0F, yBodyRot);
		}
		Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x);
		for (int i = 0; i < settings.count(); i++) {
			// Left, right, then further out each pair.
			double offset = settings.distance() * (1 + i / 2) * (i % 2 == 0 ? 1.0 : -1.0);
			Vec3 spot = findStandingSpot(level, position().add(side.scale(offset)));
			if (spot == null) {
				continue;
			}
			KoscheiIceCopy copy = EamEntities.KOSCHEI_ICE_COPY.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (copy == null) {
				continue;
			}
			copy.snapTo(spot.x, spot.y, spot.z, getYRot(), 0.0F);
			copy.summon(this, target, settings);
			level.addFreshEntity(copy);
			copies.add(copy);
			level.sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y + 1.2, spot.z, 30, 0.4, 0.8, 0.4, 0.05);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), spot.x, spot.y + 1.0, spot.z,
					20, 0.4, 0.8, 0.4, 0.1);
		}
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GLASS_PLACE, getSoundSource(), 1.5F, 0.6F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, getSoundSource(), 1.5F, 0.8F);
	}

	/** His draugr he could drain now: in reach and in sight. */
	private List<Draugr> drainable(ServerLevel level, PhaseTwoSettings.Drain drain) {
		return level.getEntitiesOfClass(Draugr.class, getBoundingBox().inflate(drain.range()), draugr -> draugr.isAlive() && draugr.servedBy(this)
				&& draugr.distanceToSqr(this) <= drain.range() * drain.range() && hasLineOfSight(draugr));
	}

	/**
	 * He rises into the air and draws the draugr's strength into himself each second, healing a little less each time,
	 * until the draugr falls or gets away from him.
	 */
	private void tickDrain(ServerLevel level, int t, PhaseTwoSettings.Drain drain) {
		Draugr from = drained;
		LivingEntity target = getTarget();
		// With no one left to fight he comes down, rather than hang there drinking.
		if (from == null || !from.isAlive() || from.distanceToSqr(this) > (drain.range() + 4.0) * (drain.range() + 4.0)
				|| target == null || !target.isAlive()) {
			endAction();
			return;
		}
		setNoGravity(true);
		double wantedY = drainGround + drain.height();
		setDeltaMovement(0.0, Mth.clamp((wantedY - getY()) * 0.2, -0.15, 0.25), 0.0);
		getLookControl().setLookAt(from, 30.0F, 30.0F);
		faceTowards(from);
		from.hold(5);
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_WOLOLO, getSoundSource(), 1.5F, 0.5F);
		}
		Vec3 hand = rightHand();
		Vec3 chest = from.position().add(0.0, from.getBbHeight() * 0.6, 0.0);
		if (t % 2 == 0) {
			// Green motes stream from the draugr to his hand.
			Vec3 way = hand.subtract(chest);
			for (int i = 0; i < 4; i++) {
				double along = random.nextDouble();
				Vec3 at = chest.add(way.scale(along));
				level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 0, way.x, way.y, way.z, 0.04);
			}
			level.sendParticles(ParticleTypes.SOUL, chest.x, chest.y, chest.z, 1, 0.2, 0.3, 0.2, 0.01);
		}
		if (t < KoscheiAction.DRAIN_RISE || (t - KoscheiAction.DRAIN_RISE) % 20 != 0) {
			return;
		}
		int second = (t - KoscheiAction.DRAIN_RISE) / 20;
		from.damageCooldownTime = 0;
		from.hurtServer(level, damageSources().source(KoscheiModule.SORCERY_DAMAGE), drain.damagePerSecond());
		heal(KoscheiLogic.drainHeal(second, drain));
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 1.2F, 0.6F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.6), getZ(), 4, 0.4, 0.5, 0.4, 0.0);
	}

	/** Whether his ball of lightning is about. */
	boolean ballAbout() {
		return ball != null && ball.isAlive();
	}

	/** The ball of lightning breaks from the staff's crystal and drifts after the target. */
	private void callBall(ServerLevel level, LivingEntity target, PhaseTwoSettings.Ball settings) {
		KoscheiBallLightning next = EamEntities.KOSCHEI_BALL_LIGHTNING.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (next == null) {
			return;
		}
		Vec3 at = raisedStaffHead();
		next.snapTo(at.x, at.y - next.getBbHeight() * 0.5, at.z, 0.0F, 0.0F);
		next.summon(this, target, settings);
		level.addFreshEntity(next);
		ball = next;
		level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, getSoundSource(), 1.5F, 1.6F);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.3);
	}

	/**
	 * The blizzard from his hands: everyone in a wide fan in front of him is pushed slowly back, and unless they hold a
	 * shield up to it, burnt a little each second and frozen over as in powder snow.
	 */
	private void tickStorm(ServerLevel level, int t, PhaseTwoSettings.Storm storm) {
		if (t == 0) {
			playSound(SoundEvents.BREEZE_INHALE, 1.5F, 0.6F);
		}
		if (t < KoscheiAction.STORM_START) {
			return;
		}
		if ((t - KoscheiAction.STORM_START) % 20 == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.POWDER_SNOW_STEP, getSoundSource(), 2.0F, 0.5F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), getSoundSource(), 0.8F, 0.4F);
		}
		Vec3 facing = Vec3.directionFromRotation(0.0F, yBodyRot);
		Vec3 hands = position().add(facing.scale(0.9)).add(0.0, getBbHeight() * 0.6, 0.0);
		// A blizzard of small flakes blown out in a wide fan, swirling as they go.
		double reachSpeed = storm.range() / 18.0;
		for (int i = 0; i < 36; i++) {
			float spread = (random.nextFloat() - 0.5F) * 2.0F * storm.angle();
			Vec3 dir = Vec3.directionFromRotation((random.nextFloat() - 0.6F) * 24.0F, yBodyRot + spread);
			double speed = reachSpeed * (0.6 + random.nextDouble() * 0.7);
			level.sendParticles(EamParticles.KOSCHEI_SNOWFLAKE, hands.x + (random.nextDouble() - 0.5) * 0.4, hands.y + (random.nextDouble() - 0.5) * 0.4,
					hands.z + (random.nextDouble() - 0.5) * 0.4, 0, dir.x, dir.y, dir.z, speed);
			if (i % 9 == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, hands.x, hands.y, hands.z, 0, dir.x, dir.y, dir.z, speed * 0.8);
			}
		}
		// A ring of frost spinning round his hands.
		double swirl = t * 0.5;
		for (int i = 0; i < 3; i++) {
			double a = swirl + i * Math.PI * 2.0 / 3.0;
			Vec3 side = Vec3.directionFromRotation(0.0F, yBodyRot - 90.0F);
			Vec3 at = hands.add(side.scale(Math.cos(a) * 0.6)).add(0.0, Math.sin(a) * 0.6, 0.0);
			level.sendParticles(EamParticles.KOSCHEI_ICE_FIELD, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		boolean burn = (t - KoscheiAction.STORM_START) % 20 == 0;
		double push = storm.pushPerSecond() / 20.0;
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(storm.range(), 3.0, storm.range()),
				e -> e != this && e.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e) && !(e instanceof Draugr draugr && draugr.servedBy(this))
						&& !(e instanceof Koschei)
						&& !(e instanceof KoscheiBallLightning) && !(e instanceof KoscheiIceRaven))) {
			if (!KoscheiLogic.inCone(victim.getX() - getX(), victim.getZ() - getZ(), facing.x, facing.z, storm.range(), storm.angle())
					|| !hasLineOfSight(victim)) {
				continue;
			}
			Vec3 away = horizontal(victim.position().subtract(position()));
			Vec3 base = victim instanceof Player ? victim.getKnownMovement() : victim.getDeltaMovement();
			victim.setDeltaMovement(base.x + away.x * push, base.y, base.z + away.z * push);
			victim.syncVelocity = true;
			victim.needsSync = true;
			if (random.nextInt(4) == 0) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY(0.6), victim.getZ(), 2, 0.3, 0.4, 0.3, 0.02);
			}
			boolean shielded = blockingShield(victim) != null;
			if (!shielded && victim.canFreeze()) {
				// Frost over the eyes and stiff limbs, as in powder snow.
				victim.setTicksFrozen(KoscheiLogic.frost(victim.getTicksFrozen(), storm.freezePerTick(), storm.freezeMax()));
			}
			if (!burn) {
				continue;
			}
			if (shielded) {
				level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 0.6F, 1.5F);
				continue;
			}
			victim.damageCooldownTime = 0;
			victim.hurtServer(level, damageSources().source(KoscheiModule.SORCERY_DAMAGE, this), storm.damagePerSecond());
		}
	}

	/** The ravens of ice burst from his raised hand, fanned out, and curve in after the target; the middle one breaks shields. */
	private void loosIceRavens(ServerLevel level, LivingEntity target, PhaseTwoSettings.IceRavens settings) {
		Vec3 hand = raisedHand();
		level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.GLASS_BREAK, getSoundSource(), 1.2F, 0.8F);
		level.playSound(null, hand.x, hand.y, hand.z, EamSounds.KOSCHEI_RAVEN_CAW, getSoundSource(), 1.5F, 1.3F);
		Vec3 facing = horizontal(target.position().subtract(position()));
		if (facing == Vec3.ZERO) {
			facing = Vec3.directionFromRotation(0.0F, yBodyRot);
		}
		Vec3 side = new Vec3(-facing.z, 0.0, facing.x);
		int count = settings.count();
		for (int i = 0; i < count; i++) {
			KoscheiIceRaven raven = EamEntities.KOSCHEI_ICE_RAVEN.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (raven == null) {
				continue;
			}
			double across = count == 1 ? 0.0 : (i / (count - 1.0) - 0.5) * 2.0;
			Vec3 out = facing.scale(0.5).add(side.scale(across * 0.9)).add(0.0, 0.35, 0.0).normalize().scale(settings.speed());
			raven.snapTo(hand.x, hand.y, hand.z, getYRot(), 0.0F);
			raven.setDeltaMovement(out);
			raven.summon(this, target, settings, i == count / 2);
			level.addFreshEntity(raven);
			iceRavens.add(raven);
		}
		level.sendParticles(ParticleTypes.SNOWFLAKE, hand.x, hand.y, hand.z, 20, 0.3, 0.3, 0.3, 0.05);
	}

	/**
	 * A wall of his ice rises across the way of a target running at him, a couple of blocks in front of it: on open air
	 * and plants only, never over anything built.
	 */
	private void raiseIceWall(ServerLevel level, LivingEntity target, PhaseTwoSettings.IceWall wall) {
		Vec3 toHim = horizontal(position().subtract(target.position()));
		if (toHim == Vec3.ZERO) {
			return;
		}
		Vec3 centre = target.position().add(toHim.scale(wall.ahead()));
		Vec3 across = new Vec3(-toHim.z, 0.0, toHim.x);
		BlockPos ground = BlockPos.containing(centre.x, target.getY() + 0.5, centre.z);
		int half = wall.size() / 2;
		boolean any = false;
		for (int w = -half; w < wall.size() - half; w++) {
			for (int h = 0; h < wall.size(); h++) {
				BlockPos pos = BlockPos.containing(centre.x + across.x * w, ground.getY() + h, centre.z + across.z * w);
				BlockState there = level.getBlockState(pos);
				if (!there.canBeReplaced() || !there.getFluidState().isEmpty()) {
					continue;
				}
				level.setBlockAndUpdate(pos, EamBlocks.KOSCHEI_ICE.defaultBlockState());
				level.scheduleTick(pos, EamBlocks.KOSCHEI_ICE, wall.lifetime() + random.nextInt(20));
				level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
				any = true;
			}
		}
		if (any) {
			level.playSound(null, centre.x, ground.getY(), centre.z, SoundEvents.GLASS_PLACE, getSoundSource(), 2.0F, 0.6F);
			level.playSound(null, centre.x, ground.getY(), centre.z, SoundEvents.POWDER_SNOW_PLACE, getSoundSource(), 2.0F, 0.5F);
		}
	}

	/** Where the crystal of his staff is as he holds it up high, in the world. */
	public Vec3 raisedStaffHead() {
		Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
		Vec3 left = Vec3.directionFromRotation(0.0F, yBodyRot - 90.0F);
		return position().add(forward.scale(0.4)).add(left.scale(0.45)).add(0.0, getBbHeight() * 1.45, 0.0);
	}

	/** Whether the trap is laid: he stands still and lets the target come right up to him. */
	boolean trapLaid() {
		return trapWait > 0;
	}

	/** The target came close with the trap laid: a moment by him, then he vanishes and draugr rise round it. */
	void triggerTrap(LivingEntity target) {
		trapWait = 0;
		actionTarget = target;
		setAction(KoscheiAction.TRAP);
	}

	private void springTrap(ServerLevel level, LivingEntity target, PhaseTwoSettings two) {
		teleportAwayFrom(level, target, two.teleport().distance());
		int raise = KoscheiLogic.thrallsToRaise(two.trap().count(), countThralls(level), two.thralls().cap());
		double start = random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < raise; i++) {
			double angle = start + i * Math.PI * 2.0 / raise + (random.nextDouble() - 0.5) * 0.6;
			double distance = two.trap().radius() * (0.6 + random.nextDouble() * 0.4);
			raiseThrall(level, target.position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance));
		}
	}

	/** The blue bolt from the staff's head at where the target stands. */
	private void shootBolt(ServerLevel level, LivingEntity target, PhaseTwoSettings.Bolt bolt) {
		Vec3 from = staffHead();
		Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
		Vec3 velocity = to.subtract(from).normalize().scale(bolt.speed());
		level.addFreshEntity(new KoscheiOrb(level, this, KoscheiOrb.Kind.BOLT, from, velocity));
		level.playSound(null, from.x, from.y, from.z, SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.2F, 1.4F);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THUNDER.value(), getSoundSource(), 0.4F, 2.0F);
	}

	/** A sweep of the staff: the blow a raised shield blocks as any blow, and it throws the target back either way. */
	private void staffStrike(ServerLevel level, LivingEntity target, PhaseTwoSettings.Strike strike) {
		playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3F, 0.6F);
		if (!inReach(target, strike.reach())) {
			return;
		}
		target.damageCooldownTime = 0;
		strike(level, target, damageSources().mobAttack(this), strike.damage(), 0.0F);
		Vec3 away = horizontal(target.position().subtract(position()));
		double speed = KoscheiLogic.pushSpeed(strike.push(), STRIKE_AIR_TICKS);
		target.setDeltaMovement(away.x * speed, STRIKE_LIFT, away.z * speed);
		target.syncVelocity = true;
		target.needsSync = true;
	}

	/** Green souls thrown out in arcs from his raised hands, each to raise a draugr where it comes down. */
	private void throwSouls(ServerLevel level, PhaseTwoSettings two) {
		Vec3 hand = raisedHand();
		level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 2.0F, 0.6F);
		level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.5F, 0.7F);
		int count = KoscheiLogic.thrallsToRaise(two.souls().count(), countThralls(level), two.thralls().cap());
		double start = random.nextDouble() * Math.PI * 2.0;
		for (int i = 0; i < count; i++) {
			double angle = start + i * Math.PI * 2.0 / Math.max(1, count) + (random.nextDouble() - 0.5) * 0.8;
			double distance = two.souls().spread() * (0.4 + random.nextDouble() * 0.6);
			Vec3 spot = findStandingSpot(level, position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance));
			if (spot == null) {
				continue;
			}
			throwSoul(level, hand, spot);
		}
	}

	/**
	 * One of his draugr climbs out of the ground at {@code at}, if there is room for it and he is under his cap; in his
	 * third phase at the speed of its kind.
	 */
	public void raiseThrall(ServerLevel level, Vec3 at) {
		if (phase() >= 3) {
			raiseThrall(level, at, false, 1.0);
		} else {
			raiseThrall(level, at, false);
		}
	}

	/** As {@link #raiseThrall(ServerLevel, Vec3)}; a {@code shieldBearer} rises with a sword and a shield. */
	private @Nullable Draugr raiseThrall(ServerLevel level, Vec3 at, boolean shieldBearer) {
		return raiseThrall(level, at, shieldBearer, phaseTwo().thralls().speedMultiplier());
	}

	/** As {@link #raiseThrall(ServerLevel, Vec3, boolean)}, the draugr {@code speedMultiplier} times as fast as their kind. */
	private @Nullable Draugr raiseThrall(ServerLevel level, Vec3 at, boolean shieldBearer, double speedMultiplier) {
		PhaseTwoSettings.Thralls thralls = phaseTwo().thralls();
		if (countThralls(level) >= thralls.cap()) {
			return null;
		}
		Vec3 spot = findStandingSpot(level, at);
		if (spot == null) {
			return null;
		}
		Draugr draugr = EamEntities.DRAUGR.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (draugr == null) {
			return null;
		}
		draugr.snapTo(spot.x, spot.y, spot.z, random.nextFloat() * 360.0F, 0.0F);
		draugr.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), EntitySpawnReason.MOB_SUMMONED, null);
		if (draugr.getMainHandItem().is(ItemTags.SPEARS)) {
			// His draugr fight with axes and swords only.
			draugr.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? EamItems.RUSTY_IRON_AXE : EamItems.RUSTY_IRON_SWORD));
		}
		if (shieldBearer) {
			draugr.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EamItems.RUSTY_IRON_SWORD));
			draugr.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		}
		draugr.raiseAsThrall(level, this, getTarget(), spot, speedMultiplier, thralls.healthMultiplier());
		level.addFreshEntity(draugr);
		thrallCount++;
		level.sendParticles(ParticleTypes.SCULK_SOUL, spot.x, spot.y + 0.2, spot.z, 12, 0.4, 0.1, 0.4, 0.03);
		level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 1.5F, 0.5F);
		return draugr;
	}

	/** His draugr up now. */
	private int countThralls(ServerLevel level) {
		return level.getEntitiesOfClass(Draugr.class, getBoundingBox().inflate(THRALL_RANGE), draugr -> draugr.isAlive() && draugr.servedBy(this))
				.size();
	}

	/** Vanishes as in his first phase, but stands far off the target instead of behind it. */
	void startTeleportAway(LivingEntity target) {
		startTeleport(target);
		teleportCooldown = phaseTwo().teleport().cooldown();
		teleportAway = true;
	}

	/** Stands about {@code distance} from the target, on the side away from it if there is room, facing it. */
	private void teleportAwayFrom(ServerLevel level, LivingEntity target, double distance) {
		Vec3 away = horizontal(position().subtract(target.position()));
		float base = away == Vec3.ZERO ? random.nextFloat() * 360.0F : (float) (Mth.atan2(away.z, away.x) * Mth.RAD_TO_DEG) - 90.0F;
		for (float angle : KoscheiLogic.teleportAngles(80.0F, random::nextDouble)) {
			Vec3 dir = Vec3.directionFromRotation(0.0F, base + angle);
			Vec3 spot = findStandingSpot(level, target.position().add(dir.scale(distance)));
			if (spot != null) {
				teleportTo(level, spot, target);
				return;
			}
		}
	}

	// ---- Teleport and hops ----

	void startTeleport(LivingEntity target) {
		teleportCooldown = settings().teleport().cooldown();
		teleportSpot = null;
		actionTarget = target;
		setAction(KoscheiAction.TELEPORT);
	}

	/**
	 * A target that has stood out of his reach above him for a while: he vanishes and stands by it if there is room up
	 * there, throws it off with the burst of force, or holds it in the bolt. True if he started one of them.
	 */
	boolean answerHeight(LivingEntity target) {
		KoscheiSettings settings = settings();
		KoscheiSettings.Height height = settings.height();
		double rise = target.getY() - getY();
		if (!readyToAttack() || !KoscheiLogic.outOfReachAbove(rise, highTicks, height) || !(level() instanceof ServerLevel level)) {
			return false;
		}
		Vec3 spot = teleportReady() ? roomBy(level, target, height.spotRange()) : null;
		boolean sees = hasLineOfSight(target);
		boolean forceReaches = sees && rise <= height.forceRise() && horizontalDistanceTo(target) <= phaseOne().force().range() + FORCE_AIMED_SLACK;
		KoscheiLogic.HeightAnswer answer = KoscheiLogic.answerHeight(teleportReady(), spot != null, cooldowns[Attack.FORCE.ordinal()] <= 0,
				forceReaches, cooldowns[Attack.BOLT.ordinal()] <= 0 && !boltBlocked(target), sees && holdsBolt(target, phaseOne().bolt()));
		if (answer == null) {
			return false;
		}
		getMoveControl().strafe(0.0F, 0.0F);
		switch (answer) {
			case TELEPORT -> {
				startTeleport(target);
				teleportSpot = spot;
			}
			case FORCE -> startAttack(Attack.FORCE, target);
			case BOLT -> startAttack(Attack.BOLT, target);
		}
		return true;
	}

	/** Room for him to stand by {@code target} at its height, nearest first, on any side; null when it stands alone up there. */
	private @Nullable Vec3 roomBy(ServerLevel level, LivingEntity target, double range) {
		for (double distance = 1.2; distance <= range; distance += 0.6) {
			for (int i = 0; i < 12; i++) {
				Vec3 dir = Vec3.directionFromRotation(0.0F, target.getYRot() + 180.0F + i * 30.0F);
				Vec3 at = target.position().add(dir.scale(distance));
				BlockPos feet = BlockPos.containing(at.x, target.getY() + 0.5, at.z);
				if (!level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
					continue;
				}
				Vec3 spot = new Vec3(at.x, feet.getY(), at.z);
				AABB box = getDimensions(getPose()).makeBoundingBox(spot);
				if (level.noCollision(this, box) && !level.containsAnyLiquid(box)) {
					return spot;
				}
			}
		}
		return null;
	}

	/** Vanishes, leaving a cloud of purple and blue motes, and stands behind the target, facing it. */
	private void teleportBehind(ServerLevel level, LivingEntity target, KoscheiSettings.Teleport teleport) {
		Vec3 from = position();
		for (float angle : KoscheiLogic.teleportAngles(teleport.maxAngle(), random::nextDouble)) {
			Vec3 dir = Vec3.directionFromRotation(0.0F, target.getYRot() + 180.0F + angle);
			Vec3 spot = findStandingSpot(level, target.position().add(dir.scale(teleport.distance())));
			if (spot == null) {
				continue;
			}
			teleportTo(level, spot, target);
			return;
		}
	}

	/** Vanishes where he stands in a burst of ravens and stands at {@code spot}, facing {@code target}. */
	private void teleportTo(ServerLevel level, Vec3 spot, LivingEntity target) {
		Vec3 from = position();
		warpCloud(level, from, 1.0);
		ravens(level, from);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.0F, 0.6F);
		teleportTo(spot.x, spot.y, spot.z);
		getNavigation().stop();
		faceTowards(target);
		setYHeadRot(getYRot());
		warpCloud(level, spot, 0.4);
		level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.5F, 0.7F);
	}

	/** A flock of ravens bursting out of where he stood, every way and upwards, with a few black feathers left drifting down. */
	private void ravens(ServerLevel level, Vec3 at) {
		level.playSound(null, at.x, at.y, at.z, EamSounds.KOSCHEI_RAVENS, getSoundSource(), 2.0F, 0.9F + random.nextFloat() * 0.2F);
		for (int i = 0; i < RAVENS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double speed = 0.22 + random.nextDouble() * 0.2;
			double y = at.y + 0.3 + random.nextDouble() * getBbHeight() * 0.8;
			level.sendParticles(EamParticles.KOSCHEI_RAVEN, at.x + Math.cos(angle) * 0.3, y, at.z + Math.sin(angle) * 0.3, 0,
					Math.cos(angle) * speed, 0.1 + random.nextDouble() * 0.2, Math.sin(angle) * speed, 1.0);
		}
		level.sendParticles(EamParticles.YAGA_FEATHER, at.x, at.y + getBbHeight() * 0.5, at.z, 10, 0.5, 0.6, 0.5, 0.02);
	}

	/** Phantom ravens burst from his raised hand and go for the target. */
	private void summonRavens(ServerLevel level, LivingEntity target, KoscheiSettings.Ravens settings) {
		Vec3 hand = raisedHand();
		level.playSound(null, hand.x, hand.y, hand.z, EamSounds.KOSCHEI_RAVENS, getSoundSource(), 1.5F, 1.1F);
		level.playSound(null, hand.x, hand.y, hand.z, EamSounds.KOSCHEI_RAVEN_CAW, getSoundSource(), 1.5F, 0.8F);
		level.sendParticles(EamParticles.KOSCHEI_WARP, hand.x, hand.y, hand.z, 24, 0.3, 0.3, 0.3, 0.03);
		for (int i = 0; i < settings.count(); i++) {
			KoscheiRaven raven = EamEntities.KOSCHEI_RAVEN.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (raven == null) {
				continue;
			}
			double angle = (i + random.nextDouble() * 0.5) * Math.PI * 2.0 / settings.count();
			raven.snapTo(hand.x, hand.y, hand.z, getYRot(), 0.0F);
			raven.setDeltaMovement(Math.cos(angle) * 0.35, 0.3 + random.nextDouble() * 0.15, Math.sin(angle) * 0.35);
			raven.summon(this, target, settings);
			level.addFreshEntity(raven);
			flock.add(raven);
		}
		for (int i = 0; i < 8; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double speed = 0.15 + random.nextDouble() * 0.15;
			level.sendParticles(EamParticles.KOSCHEI_RAVEN, hand.x, hand.y, hand.z, 0, Math.cos(angle) * speed, 0.15 + random.nextDouble() * 0.15,
					Math.sin(angle) * speed, 1.0);
		}
	}

	/** Where he can stand near {@code at}: open air for his body over solid ground, a few blocks up or down at most. */
	private @Nullable Vec3 findStandingSpot(ServerLevel level, Vec3 at) {
		for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
			BlockPos feet = BlockPos.containing(at.x, at.y + dy, at.z);
			BlockPos below = feet.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				continue;
			}
			Vec3 spot = new Vec3(at.x, feet.getY(), at.z);
			AABB box = getDimensions(getPose()).makeBoundingBox(spot);
			if (level.noCollision(this, box) && !level.containsAnyLiquid(box)) {
				return spot;
			}
		}
		return null;
	}

	/** The motes he leaves where he stood, or that gather where he appears. */
	private void warpCloud(ServerLevel level, Vec3 at, double amount) {
		level.sendParticles(EamParticles.KOSCHEI_WARP, at.x, at.y + getBbHeight() * 0.5, at.z, (int) (50 * amount), 0.4, getBbHeight() * 0.35,
				0.4, 0.02);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + getBbHeight() * 0.5, at.z, (int) (30 * amount), 0.3, getBbHeight() * 0.3,
				0.3, 0.05);
	}

	/** A short hop the given way, flat. */
	void hop(Vec3 direction, @Nullable LivingEntity target) {
		hop(direction, target, settings().hop().speed());
	}

	private void hop(Vec3 direction, @Nullable LivingEntity target, double speed) {
		hop(direction, target, speed, false);
	}

	private void hop(Vec3 direction, @Nullable LivingEntity target, double speed, boolean aimed) {
		if (!onGround() || direction.lengthSqr() < 1.0E-6) {
			return;
		}
		hopCooldown = settings().hop().cooldown();
		hopSpeed = speed;
		hopAimed = aimed;
		rushDirection = direction.normalize();
		actionTarget = target;
		setAction(KoscheiAction.HOP);
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.6F);
	}

	/** Hops to the left, the right or back from the target. */
	void hopAround(LivingEntity target) {
		Vec3 away = horizontal(position().subtract(target.position()));
		Vec3 side = new Vec3(-away.z, 0.0, away.x);
		double roll = random.nextDouble();
		Vec3 direction = roll < 0.35 ? side : roll < 0.7 ? side.scale(-1.0) : away;
		hop(direction.add(away.scale(0.3)), target);
	}

	/** A short hop in at the target he walks up to, as the Fallen Lord's, landing a couple of blocks short of it. */
	void hopIn(LivingEntity target) {
		KoscheiSettings.Hop hop = settings().hop();
		double length = KoscheiLogic.hopInLength(horizontalDistanceTo(target), hop.inMin(), hop.inMax(), random.nextDouble());
		if (length > 0.0) {
			getNavigation().stop();
			hop(horizontal(target.position().subtract(position())), target, length / HOP_PUSH_TICKS, true);
		}
	}

	/** He knocked {@code target}'s shield out and it is not about to be ready again: the bolt waits. */
	boolean boltBlocked(LivingEntity target) {
		return target.getUUID().equals(shieldDownOf)
				&& KoscheiLogic.boltBlocked(level().getGameTime(), shieldBackAt, phaseOne().bolt().shieldBack());
	}

	/** His bolt has been ready a long while without a cast. */
	boolean boltHungry() {
		return boltHunger >= phaseOne().bolt().hungerTicks();
	}

	boolean flankReady() {
		return flankCooldown <= 0 && onGround();
	}

	/**
	 * A quick low dash round to the target's side or back, where a raised shield does not cover, and the blow comes as soon
	 * as he lands. False when there is no room for it there.
	 */
	boolean flankDash(LivingEntity target) {
		if (!(level() instanceof ServerLevel level) || !flankReady()) {
			return false;
		}
		flankCooldown = settings().hop().flankCooldown();
		float side = random.nextBoolean() ? 1.0F : -1.0F;
		float angle = target.getYHeadRot() + side * Mth.lerp(random.nextFloat(), FLANK_MIN_ANGLE, FLANK_MAX_ANGLE);
		Vec3 spot = findStandingSpot(level, target.position().add(Vec3.directionFromRotation(0.0F, angle).scale(FLANK_DISTANCE)));
		if (spot == null) {
			return false;
		}
		Vec3 way = spot.subtract(position());
		double length = Math.sqrt(way.x * way.x + way.z * way.z);
		Vec3 from = position().add(0.0, 1.0, 0.0);
		if (length < 1.0 || level.clip(new ClipContext(from, spot.add(0.0, 1.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
				.getType() != HitResult.Type.MISS) {
			return false;
		}
		getNavigation().stop();
		hop(horizontal(way), target, length / HOP_PUSH_TICKS, true);
		flankStrike = action == KoscheiAction.HOP;
		return flankStrike;
	}

	private void leapBack(Vec3 from) {
		double speed = settings().hop().burstSpeed();
		Vec3 away = horizontal(position().subtract(from));
		setDeltaMovement(away.x * speed, 0.45, away.z * speed);
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.5F, 0.5F);
	}

	// ---- Hits ----

	private boolean inReach(LivingEntity target, double reach) {
		return horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.5 && inFront(target)
				&& hasLineOfSight(target);
	}

	private boolean strike(ServerLevel level, LivingEntity target, DamageSource source, float damage, float knockback) {
		if (!target.hurtServer(level, source, KoscheiLogic.damageTo(damage, target instanceof Player, settings().mobDamageMultiplier()))) {
			return false;
		}
		if (knockback > 0.0F) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ(), source, damage);
		}
		setLastHurtMob(target);
		return true;
	}

	/** The shield the target holds up against him, or null. */
	private @Nullable ItemStack blockingShield(LivingEntity target) {
		return blockingShieldFrom(target, position());
	}

	/** The shield the target holds up against something coming from {@code from}, or null. */
	private static @Nullable ItemStack blockingShieldFrom(LivingEntity target, Vec3 from) {
		ItemStack shield = target.getItemBlockingWith();
		return shield != null && shield.has(DataComponents.BLOCKS_ATTACKS)
				&& FallenLordLogic.shieldCovers(target.getYHeadRot(), from.x - target.getX(), from.z - target.getZ()) ? shield : null;
	}

	private void knockShieldOut(ServerLevel level, LivingEntity target, ItemStack shield, float seconds) {
		BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks == null || seconds <= 0.0F) {
			return;
		}
		blocks.disable(level, target, seconds, shield);
		shieldDownOf = target.getUUID();
		shieldBackAt = level.getGameTime() + Math.round(seconds * 20.0F);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.8F);
	}

	/** Only his orange blow knocks a shield out. */
	@Override
	public float getSecondsToDisableBlocking() {
		return 0.0F;
	}

	// ---- Arrows ----

	/** Arrows from the front are knocked aside by his sword while he is not in the middle of a blow. */
	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		if (!(projectile instanceof AbstractArrow) || projectile.getOwner() == this || level().isClientSide()) {
			return super.deflection(projectile);
		}
		if (iceField()) {
			return this::iceFieldTurns;
		}
		Vec3 motion = projectile.getDeltaMovement();
		if (!FallenLordLogic.shieldCovers(yBodyRot, -motion.x, -motion.z) || !guarding()
				|| random.nextDouble() >= phaseOne().arrowParryChance()) {
			return super.deflection(projectile);
		}
		return this::parryArrow;
	}

	/** Runs on the server only. The arrow flies off to one side, slowed, and he flicks his sword. */
	private void parryArrow(Projectile projectile, @Nullable Entity by, RandomSource random, Vec3 power) {
		Vec3 motion = projectile.getDeltaMovement();
		Vec3 side = new Vec3(-motion.z, 0.0, motion.x).normalize().scale(random.nextBoolean() ? 1.0 : -1.0);
		projectile.setDeltaMovement(side.scale(motion.length() * 0.3).add(motion.scale(-0.1)).add(0.0, 0.2, 0.0));
		projectile.needsSync = true;
		level().playSound(null, getX(), getY(0.6), getZ(), SoundEvents.ANVIL_LAND, getSoundSource(), 0.35F, 1.8F + random.nextFloat() * 0.2F);
		if (action == KoscheiAction.NONE) {
			actionTarget = getTarget();
			setAction(KoscheiAction.PARRY_ARROW);
		}
	}

	/**
	 * Runs on the server only. The arrow strikes his ice field, unseen till then: a shell of ice panes flashes up round
	 * him for a moment, with a crack and frost where it struck, and the arrow drops away.
	 */
	private void iceFieldTurns(Projectile projectile, @Nullable Entity by, RandomSource random, Vec3 power) {
		Vec3 motion = projectile.getDeltaMovement();
		projectile.setDeltaMovement(motion.scale(-0.12).add(0.0, 0.1, 0.0));
		projectile.needsSync = true;
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		double radius = getBbHeight() * 0.62;
		// The shell: panes spread evenly over a sphere round him (a Fibonacci lattice).
		int panes = 70;
		double golden = Math.PI * (3.0 - Math.sqrt(5.0));
		for (int i = 0; i < panes; i++) {
			double y = 1.0 - (i + 0.5) * 2.0 / panes;
			double ring = Math.sqrt(1.0 - y * y);
			double angle = i * golden;
			level.sendParticles(EamParticles.KOSCHEI_ICE_FIELD, centre.x + Math.cos(angle) * ring * radius * 0.75, centre.y + y * radius,
					centre.z + Math.sin(angle) * ring * radius * 0.75, 1, 0.0, 0.0, 0.0, 0.0);
		}
		Vec3 hit = centre.add(motion.normalize().scale(-radius * 0.8));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), hit.x, hit.y, hit.z, 12,
				0.15, 0.15, 0.15, 0.1);
		level.sendParticles(EamParticles.KOSCHEI_SNOWFLAKE, hit.x, hit.y, hit.z, 10, 0.2, 0.2, 0.2, 0.05);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.AMETHYST_BLOCK_HIT, getSoundSource(), 1.5F, 1.4F + random.nextFloat() * 0.2F);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.GLASS_HIT, getSoundSource(), 1.0F, 1.6F);
	}

	/** Whether his sword is free to turn blows and arrows aside: not while he is thrown off balance, casting or vanishing. */
	private boolean guarding() {
		if (phase() >= 2) {
			// The staff does not turn blows aside.
			return false;
		}
		return switch (action) {
			case STAGGER, FORCE, BOLT -> false;
			default -> true;
		};
	}

	// ---- Damage: block, parry, resistance ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		Entity attacker = source.getEntity();
		if (action == KoscheiAction.TRANSFORM || action == KoscheiAction.SHATTER || attacker instanceof Draugr draugr && draugr.servedBy(this)) {
			// Nothing touches him while he changes, and his own draugr do not hurt him.
			return false;
		}
		if (action == KoscheiAction.DRAIN) {
			// Nothing touches him while he drains a draugr either: kill the draugr to stop it.
			level.playSound(null, getX(), getY(0.6), getZ(), SoundEvents.AMETHYST_BLOCK_HIT, getSoundSource(), 1.5F, 0.6F);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(0.6), getZ(), 6, 0.4, 0.6, 0.4, 0.02);
			return false;
		}
		boolean melee = attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
		if (melee && attacker instanceof Player player && player.getUUID().equals(parryPlayer) && level.getGameTime() <= parryUntil) {
			// The hit back after his lunge knocked the shield out.
			float hit = damage * BossParry.penalty().multiplier(phaseOne().parry().multiplier(), parryPenalized);
			closeParry();
			boolean hurt = super.hurtServer(level, source, resisted(source, hit));
			if (hurt && isAlive()) {
				parried(level, player);
			}
			return hurt;
		}
		if (melee && attacker instanceof Player player) {
			ParryPenalty penalty = BossParry.penalty();
			boolean penalized = parryStreak.penalized(player.getUUID(), level.getGameTime(), penalty);
			int window = penalty.window(parryWindow(action), penalized);
			if (action == KoscheiAction.OVERHEAD && actionTicks - 1 < action.parryStart(window)
					&& KoscheiLogic.windupSpammed(++windupHits, phaseOne().parry().windupHits())) {
				// Clicking at him all through the windup: this blow can't be parried.
				entityData.set(DATA_PARRY_LOST, true);
			}
			if (!entityData.get(DATA_PARRY_LOST) && KoscheiLogic.parries(action, actionTicks - 1, window)) {
				float hit = damage * penalty.multiplier(phaseOne().parry().multiplier(), penalized);
				boolean hurt = super.hurtServer(level, source, resisted(source, hit));
				if (hurt && isAlive()) {
					parried(level, player);
				}
				return hurt;
			}
		}
		if (melee && guarding() && FallenLordLogic.shieldCovers(yBodyRot, attacker.getX() - getX(), attacker.getZ() - getZ())) {
			blockBlow(level, (LivingEntity) attacker);
			return false;
		}
		if (melee && phase() >= 3) {
			// Undead now: Smite bites him as it bites the dead.
			damage += smiteBonus(level, (LivingEntity) attacker);
		}
		return super.hurtServer(level, source, resisted(source, damage));
	}

	private float resisted(DamageSource source, float damage) {
		// Weapon hits and projectiles (as the Fallen Lord counts them), falls and explosions; not fire or magic.
		boolean physical = source.is(FallenLordModule.PHYSICAL) || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_EXPLOSION);
		return KoscheiLogic.resisted(damage, physical, settings().stats().physicalResist());
	}

	/** His sword turns a blow from the front aside: he holds it across where the blow comes, and sparks fly from the steel. */
	private void blockBlow(ServerLevel level, LivingEntity attacker) {
		// Where the attacker aims at him: their line of sight where it passes his middle.
		Vec3 eye = attacker.getEyePosition();
		Vec3 look = attacker.getViewVector(1.0F);
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		Vec3 aim = eye.add(look.scale(Math.max(0.0, centre.subtract(eye).dot(look))));
		Vec3 facing = Vec3.directionFromRotation(0.0F, yBodyRot);
		Vec3 left = Vec3.directionFromRotation(0.0F, yBodyRot - 90.0F);
		float height = (float) Mth.clamp((aim.y - getY()) / getBbHeight(), 0.25, 0.95);
		float side = (float) Mth.clamp(aim.subtract(centre).dot(left) / 0.6, -1.0, 1.0);
		entityData.set(DATA_GUARD, packGuard(height, side));
		Vec3 spot = position().add(facing.scale(0.75)).add(left.scale(side * 0.35)).add(0.0, getBbHeight() * height, 0.0);
		level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ANVIL_LAND, getSoundSource(), 0.5F, 1.5F + random.nextFloat() * 0.2F);
		// Orange sparks off the steel.
		level.sendParticles(EamParticles.KOSCHEI_SPARK, spot.x, spot.y, spot.z, 18, 0.06, 0.06, 0.06, 0.3);
		level.sendParticles(ParticleTypes.SMALL_FLAME, spot.x, spot.y, spot.z, 4, 0.05, 0.05, 0.05, 0.08);
		if (action == KoscheiAction.NONE || action == KoscheiAction.BLOCK) {
			actionTarget = getTarget();
			setAction(KoscheiAction.BLOCK);
		}
	}

	/** His lunge knocked {@code player}'s shield out: their hit within {@code window} ticks parries him, and a bell rings. */
	private void openParry(ServerLevel level, Player player, int window) {
		ParryPenalty penalty = BossParry.penalty();
		parryPlayer = player.getUUID();
		parryPenalized = parryStreak.penalized(parryPlayer, level.getGameTime(), penalty);
		parryUntil = level.getGameTime() + penalty.window(window, parryPenalized);
		entityData.set(DATA_PARRY_OPEN, true);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 1.5F, 1.0F);
	}

	private void closeParry() {
		parryPlayer = null;
		entityData.set(DATA_PARRY_OPEN, false);
	}

	/** A player hit him just before his lunge or overhead blow landed: it breaks off and he staggers, open to hits. */
	private void parried(ServerLevel level, Player player) {
		parryStreak.parried(player.getUUID(), level.getGameTime());
		// As with the Fallen Lord: a shield he knocked out is ready again at once.
		shieldDownOf = null;
		for (ItemStack held : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
			if (held.has(DataComponents.BLOCKS_ATTACKS)) {
				player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(held));
			}
		}
		setAction(KoscheiAction.STAGGER);
		actionTarget = player;
		clearBolt();
		Vec3 away = horizontal(position().subtract(player.position()));
		setDeltaMovement(away.x * 0.3, 0.1, away.z * 0.3);
		level.playSound(null, getX(), getY(0.6), getZ(), EamSounds.FALLEN_LORD_PARRY, getSoundSource(), 2.0F, 0.9F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 14, getBbWidth() * 0.4, getBbHeight() * 0.2, getBbWidth() * 0.4, 0.2);
	}

	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		if (phase() == 1 && getHealth() <= 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			// Struck down in his first phase: he does not die, he changes.
			setHealth(1.0F);
			startTransform(level);
			return;
		}
		if (phase() == 2 && risesAgain() && getHealth() <= 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			// Struck down in his second phase: the last of him, undead and furious.
			setHealth(1.0F);
			startShatter(level);
			return;
		}
		if (isAlive() && phase() <= 2) {
			leapIfHurtFast(level, source, before - getHealth());
		}
		if (isAlive() && phase() == 2) {
			vanishIfHitAgain(level, source);
		}
		if (isAlive() && phase() >= 3 && source.getEntity() instanceof LivingEntity) {
			// He does not back off: each hit makes him strike faster.
			furyCalm = 0;
			entityData.set(DATA_FURY, (byte) Math.min(phaseThree().rage().maxStacks(), fury() + 1));
		}
	}

	/** Second phase: a couple of hits in a row and he vanishes at once, ready or not, to stand far off. */
	private void vanishIfHitAgain(ServerLevel level, DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || !vanishesWhenHit()) {
			return;
		}
		PhaseTwoSettings.Teleport teleport = phaseTwo().teleport();
		long now = level.getGameTime();
		hitStreak = KoscheiLogic.hitStreak(hitStreak, lastHitAt, now, teleport.hitsWindow());
		lastHitAt = now;
		if (teleport.hits() <= 0 || hitStreak < teleport.hits()
				|| action == KoscheiAction.TELEPORT || action == KoscheiAction.DRAIN || action == KoscheiAction.TRANSFORM) {
			return;
		}
		hitStreak = 0;
		if (action != KoscheiAction.NONE) {
			endAction();
		}
		startTeleportAway(attacker);
	}

	/** Whether, struck down in his second phase, he rises again in his third; not his copies in ice. */
	boolean risesAgain() {
		return true;
	}

	/** Whether a couple of hits in a row make him vanish; not his copies in ice. */
	boolean vanishesWhenHit() {
		return true;
	}

	/** Whether his ice field turns arrows aside: all round him in his second phase; not round his copies in ice. */
	boolean iceField() {
		return phase() == 2;
	}

	/** Losing health fast, he breaks off whatever he is doing and leaps back away from who hurts him. */
	private void leapIfHurtFast(ServerLevel level, DamageSource source, float lost) {
		KoscheiSettings.Hop hop = settings().hop();
		long now = level.getGameTime();
		recentHits.addLast(new Hit(now, Math.max(0.0F, lost)));
		while (!recentHits.isEmpty() && now - recentHits.peekFirst().time() > hop.burstWindow()) {
			recentHits.removeFirst();
		}
		float taken = 0.0F;
		for (Hit hit : recentHits) {
			taken += hit.amount();
		}
		// Not out of a stagger: the burst of force that ends it is his answer to a parry.
		if (!KoscheiLogic.burstLeap(taken, getMaxHealth(), hop.burstShare()) || !onGround() || action == KoscheiAction.STAGGER) {
			return;
		}
		recentHits.clear();
		Entity from = source.getEntity() != null ? source.getEntity() : getTarget();
		if (from == null) {
			return;
		}
		if (action != KoscheiAction.NONE) {
			endAction();
		}
		hopCooldown = hop.cooldown();
		leapFrom = from.position();
	}

	private record Hit(long time, float amount) {
	}

	// ---- Geometry ----

	public double horizontalDistanceTo(Entity other) {
		double dx = other.getX() - getX();
		double dz = other.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private boolean inFront(LivingEntity other) {
		Vec3 to = horizontal(other.position().subtract(position()));
		Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
		return to.lengthSqr() < 1.0E-6 || to.dot(facing) >= 0.3;
	}

	private void faceTowards(LivingEntity target) {
		float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 30.0F));
		setYBodyRot(getYRot());
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}

	/** Where the point of his levelled sword is, for a body turned {@code bodyYaw}, relative to his feet. */
	public static Vec3 swordTipOffset(float bodyYaw, float height) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw);
		Vec3 left = Vec3.directionFromRotation(0.0F, bodyYaw - 90.0F);
		return forward.scale(2.1).add(left.scale(0.3)).add(0.0, height * 0.7, 0.0);
	}

	private Vec3 swordTip() {
		return position().add(swordTipOffset(yBodyRot, getBbHeight()));
	}

	/**
	 * Where the crystal at the head of his staff is as he thrusts it out at the target, for a body turned {@code bodyYaw},
	 * relative to his feet.
	 */
	public static Vec3 staffHeadOffset(float bodyYaw, float height) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw);
		Vec3 left = Vec3.directionFromRotation(0.0F, bodyYaw - 90.0F);
		return forward.scale(1.8).add(left.scale(0.4)).add(0.0, height * 1.1, 0.0);
	}

	private Vec3 staffHead() {
		return position().add(staffHeadOffset(yBodyRot, getBbHeight()));
	}

	/** Where his right hand is as he raises it high to call up the ravens, in the world. */
	public Vec3 raisedHand() {
		Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
		Vec3 right = Vec3.directionFromRotation(0.0F, yBodyRot + 90.0F);
		return position().add(forward.scale(0.3)).add(right.scale(0.5)).add(0.0, getBbHeight() + 0.4, 0.0);
	}

	/** Where his right palm is as he thrusts it out, in the world. */
	public Vec3 rightHand() {
		Vec3 forward = Vec3.directionFromRotation(0.0F, yBodyRot);
		Vec3 right = Vec3.directionFromRotation(0.0F, yBodyRot + 90.0F);
		return position().add(forward.scale(1.0)).add(right.scale(0.45)).add(0.0, getBbHeight() * 0.62, 0.0);
	}

	// ---- Boss bar, saving ----

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (showsBossBar()) {
			bossEvent.addPlayer(player);
		}
	}

	/** Whether those who see him see his boss bar. */
	protected boolean showsBossBar() {
		return true;
	}

	private BossEvent.BossBarColor barColor() {
		return switch (phase()) {
			case 1 -> BossEvent.BossBarColor.PURPLE;
			case 2 -> BossEvent.BossBarColor.GREEN;
			default -> BossEvent.BossBarColor.RED;
		};
	}

	/** Undead in his third phase: healing harms him and harming heals him. */
	@Override
	public boolean isInvertedHealAndHarm() {
		return phase() >= 3 || super.isInvertedHealAndHarm();
	}

	/** Undead in his third phase: poison and regeneration do not take. */
	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		if (phase() >= 3 && (effect.is(MobEffects.POISON) || effect.is(MobEffects.REGENERATION))) {
			return false;
		}
		return super.canBeAffected(effect);
	}

	/** Smite's bonus on a blow from {@code attacker}, 2.5 a level, as on any undead. */
	private float smiteBonus(ServerLevel level, LivingEntity attacker) {
		int smite = EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SMITE), attacker.getWeaponItem());
		return smite * 2.5F;
	}

	/** Sets the phase he fights in, for one made straight into his second phase. */
	void setPhase(int phase) {
		entityData.set(DATA_PHASE, (byte) phase);
		bossEvent.setName(barName());
	}

	/** His name on the boss bar: one given him, else the name of the phase he fights in. */
	private Component barName() {
		return hasCustomName() ? getDisplayName() : Component.translatable("entity.eam.koschei.phase_" + Math.clamp(phase(), 1, 3));
	}

	// ---- Death ----

	/** Killed in his third phase, his needle drops only once he has shrunk away into it. */
	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		if (phase() >= 3 && deathTime < DEATH_TICKS) {
			deathSource = source;
			return;
		}
		super.dropAllDeathLoot(level, source);
	}

	/** Killed in his third phase, he shrinks away into his needle, frost and souls drawn in after him. */
	@Override
	protected void tickDeath() {
		if (phase() < 3) {
			super.tickDeath();
			return;
		}
		deathTime++;
		if (!(level() instanceof ServerLevel level) || isRemoved()) {
			return;
		}
		Vec3 heart = position().add(0.0, getBbHeight() * 0.45, 0.0);
		if (deathTime % 2 == 0) {
			for (int i = 0; i < 6; i++) {
				Vec3 from = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(1.2 + random.nextDouble());
				level.sendParticles(EamParticles.KOSCHEI_WARP, heart.x + from.x, heart.y + from.y, heart.z + from.z, 0, -from.x, -from.y, -from.z, 0.12);
			}
			level.sendParticles(ParticleTypes.SOUL, heart.x, heart.y, heart.z, 2, 0.4, 0.5, 0.4, 0.02);
		}
		if (deathTime == 1) {
			level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.WITHER_DEATH, getSoundSource(), 0.8F, 1.6F);
		}
		if (deathTime >= DEATH_TICKS) {
			level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.AMETHYST_BLOCK_BREAK, getSoundSource(), 2.0F, 1.6F);
			level.playSound(null, heart.x, heart.y, heart.z, SoundEvents.TRIDENT_RETURN, getSoundSource(), 2.0F, 0.6F);
			level.sendParticles(ParticleTypes.END_ROD, heart.x, heart.y, heart.z, 30, 0.05, 0.05, 0.05, 0.2);
			level.sendParticles(ParticleTypes.SCULK_SOUL, heart.x, heart.y, heart.z, 20, 0.2, 0.2, 0.2, 0.08);
			if (deathSource != null) {
				dropAllDeathLoot(level, deathSource);
			}
			level.broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		bossEvent.setName(barName());
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	/** His draugr fight at his side. */
	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof Draugr draugr && draugr.servedBy(this) || other instanceof Koschei || super.considersEntityAsAlly(other);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("Phase", entityData.get(DATA_PHASE));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_PHASE, input.getByteOr("Phase", (byte) 1));
		// Saved in the middle of a drain: the drain is not saved, so he comes down.
		setNoGravity(false);
		applyStats(false);
		bossEvent.setName(barName());
		bossEvent.setColor(barColor());
		Arrays.fill(cooldowns, 0);
		Arrays.fill(staffCooldowns, 0);
		Arrays.fill(daggerCooldowns, 0);
	}

	// ---- Sounds ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.EVOKER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.EVOKER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.EVOKER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}

	@Override
	public boolean isFighting() {
		return getTarget() != null && getTarget().isAlive();
	}
}
