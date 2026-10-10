package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Attack;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.BlockStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.HitStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Wound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Boss of the crypt. Phase one: spear and shield, a cautious duel. Phase two: shield dropped, sword drawn, aggressive.
 * Each phase has its own health; the boss bar shows the current one. Attacks run here as {@link FallenLordAction}s;
 * {@link FallenLordFightGoal} moves him and picks them with {@link FallenLordLogic}.
 */
public class FallenLord extends Monster {
	/**
	 * His death, in ticks since it began (shared with the renderer): he sinks to his knees, the bones inside the armor
	 * burn to ash ({@link #ASH_START} to {@link #ASH_END}), the empty armor falls to the ground
	 * ({@link #COLLAPSE_START} to {@link #COLLAPSE_END}) and lies there until {@link #DEATH_TICKS}.
	 */
	public static final int ASH_START = 6;
	public static final int ASH_END = 30;
	public static final int COLLAPSE_START = 24;
	public static final int COLLAPSE_END = 44;
	public static final int DEATH_TICKS = 100;
	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BYTE);
	/** Bumped on every new action, so clients restart the animation even when the same action repeats. */
	private static final EntityDataAccessor<Integer> DATA_ACTION_SEQ = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_SHIELD_UP = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BOOLEAN);
	/** Whether an axe has knocked his shield out of the guard; the client shows it hanging. */
	private static final EntityDataAccessor<Boolean> DATA_SHIELD_KNOCKED = SynchedEntityData.defineId(FallenLord.class,
			EntityDataSerializers.BOOLEAN);

	/** Hits land this far beyond the nominal reach, so a target backing off in the swing still gets hit. */
	private static final double REACH_SLACK = 0.6;
	private static final double DASH_SPEED = 0.9;
	private static final double SWORD_DASH_SPEED = 1.0;
	/** A blocked sword dash still pushes the player back a block, as a hit does. */
	private static final double SWORD_DASH_PUSH = 0.4;
	/** A leap back pushes him for this many ticks, at the speed that covers its distance. */
	private static final int RETREAT_TICKS = 8;
	/** The lunge's run in lasts this many ticks. */
	private static final int LUNGE_RUSH_TICKS = 6;
	/** The leap back after a burst of damage. */
	private static final double BURST_LEAP = 6.0;
	/** Swirls of the regeneration potion while he drinks. */
	private static final int REGEN_COLOR = 0xCD5CAB;
	/** The hop's push lasts this many ticks; the rest of {@link FallenLordAction#HOP} is the landing. */
	private static final int HOP_PUSH_TICKS = 5;
	private static final double HOP_LIFT = 0.3;
	/** Fastest he darts round the player in the backstab, blocks per tick. */
	private static final double BACKSTAB_MAX_SPEED = 1.4;
	/** He looks for a player to turn on, when fighting something else, this often. */
	private static final int PLAYER_PRIORITY_INTERVAL = 10;
	/** A tenth bigger than the wither skeleton he is modelled on. */
	private static final double SCALE = 1.1;
	/** Without the spear back by then (lost in a wall, unloaded) he gets a new one. */
	private static final int SPEAR_LOST_TICKS = 60;
	/** His health and speed bonuses for the players around him. */
	private static final Identifier PLAYERS_BONUS = Eam.id("fallen_lord_players");
	private static final Identifier SPRINT_BONUS = Eam.id("fallen_lord_sprint");
	/** He counts the players around him this often. */
	private static final int PLAYER_COUNT_INTERVAL = 20;
	/** How fast the shield arm comes up into the guard and drops, per tick. */
	private static final float GUARD_SPEED = 0.2F;

	/** Phase two actions that start an attack, and those that end one: counted for his leap back. */
	private static final Set<FallenLordAction> ATTACK_STARTS = EnumSet.of(FallenLordAction.SLASH, FallenLordAction.COMBO,
			FallenLordAction.LUNGE, FallenLordAction.WHIRL, FallenLordAction.SWORD_DASH_WINDUP, FallenLordAction.BACKSTAB,
			FallenLordAction.HEAVY_WINDUP);
	/** Attacks he breaks off at once when their target dies, instead of finishing them at nothing. */
	private static final Set<FallenLordAction> BROKEN_BY_KILL = EnumSet.of(FallenLordAction.POKE, FallenLordAction.DASH_WINDUP,
			FallenLordAction.SWIPE, FallenLordAction.SLASH, FallenLordAction.COMBO, FallenLordAction.LUNGE,
			FallenLordAction.SWORD_DASH_WINDUP, FallenLordAction.BACKSTAB, FallenLordAction.HEAVY_WINDUP, FallenLordAction.HEAVY_STRIKE,
			FallenLordAction.COUNTER, FallenLordAction.HOP);
	private static final Set<FallenLordAction> ATTACK_ENDS = EnumSet.of(FallenLordAction.SLASH, FallenLordAction.COMBO,
			FallenLordAction.LUNGE, FallenLordAction.WHIRL, FallenLordAction.SWORD_DASH, FallenLordAction.BACKSTAB,
			FallenLordAction.HEAVY_STRIKE);

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

	private final int[] cooldowns = new int[Attack.values().length];
	private @Nullable Attack lastAttack;
	private FallenLordAction action = FallenLordAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private Vec3 dashTarget = Vec3.ZERO;
	private Vec3 rushDirection = Vec3.ZERO;
	private boolean actionHit;
	/** Everyone the current spear dash has struck: each is hit once. */
	private final Set<UUID> dashHits = new HashSet<>();
	/** Stretches of the current dash's run filled by blocks it smashed, to tell how deep it has gone. */
	private final List<FallenLordLogic.Span> dashSpans = new ArrayList<>();
	/** Set once the current dash has smashed as deep as it can: it smashes nothing more and stops at the next block. */
	private boolean dashSpent;
	private int idleTicks;
	private int shieldDisabledTicks;
	private HitStreak hitStreak = HitStreak.NONE;
	private boolean swipePending;
	private int ticksOutOfReach;
	/** Ticks the target has kept out of his weapon's reach; past the sprint threshold he sprints until a hit lands. */
	private int chaseTicks;
	private boolean sprinting;
	private int spearAwayTicks = -1;
	/** The spear in flight that will come back; an older one that turns up late is ignored. */
	private @Nullable UUID thrownSpear;
	private int drinksThisPhase;
	private int drinkCooldown;
	private float healedThisDrink;
	/** The leap back ends in a drink. */
	private boolean drinkAfterRetreat;
	/** Or in his fencing stance. */
	private boolean stanceAfterRetreat;
	/** Blocks per tick of the current leap back's push. */
	private double retreatSpeed;
	/** Phase two attacks in a row since he last leapt back. */
	private int attacksInRow;
	/** His recent hits on each player, with the health each took, for the breather. */
	private final Map<UUID, List<Wound>> blows = new HashMap<>();
	private int breatherCooldown;
	/** Ticks left of a jump over an obstacle, while he is pushed on over it, and before he can jump the next. */
	private int climbTicks;
	private int climbCooldown;
	private Vec3 climbDirection = Vec3.ZERO;
	/** Climbing a bank out of water: he rises straight up until this height, then is pushed over it. */
	private double waterClimbTop = Double.NaN;
	/** Ticks he has been pushing against a wall he cannot jump; past {@link #STUCK_TICKS} he breaks through it. */
	private int stuckTicks;
	/** His boss room: inside it he breaks every block weaker than obsidian. Null for a lord spawned elsewhere. */
	private @Nullable BoundingBox arena;
	/** Ticks before he can leap up at a player again. */
	private int leapUpCooldown;
	/** Where the current leap up sprang from. */
	private Vec3 leapUpStart = Vec3.ZERO;
	/** Hits on one player that make him take a breather; picked anew after each, 0 until picked. */
	private int breatherHits;
	private boolean breatherPending;
	/** Stance: the target's distance and health when it began. */
	private double stanceDistance;
	private float stanceHealth;
	/** Spear dash: where the run started and how long it is. */
	private Vec3 dashStart = Vec3.ZERO;
	private double dashLength;
	/** Dashes he has followed the first one with. */
	private int dashesChained;
	/** Set while the sword dash strikes: a shield that blocks it is knocked out at once. */
	private boolean breaksShields;
	/** The hop goes at the target, not away. */
	private boolean hopTowards;
	private double hopSpeed;
	/** His shield was just knocked out: he hops back from the player once to make room. */
	private boolean hopBackPending;
	/** Ticks since one of his hits last landed. */
	private int ticksSinceHit;
	/** Backstab: where he passes the player's side and where he ends up behind them, relative to the player. */
	private Vec3 backstabSide = Vec3.ZERO;
	private Vec3 backstabBack = Vec3.ZERO;
	/** Health lost lately, for the leap back after a burst of damage. */
	private final List<Wound> wounds = new ArrayList<>();
	/** Players around him at the last count, at least one. */
	private int players = 1;
	/** His hits each player's shield has blocked lately. */
	private final Map<UUID, BlockStreak> blockStreaks = new HashMap<>();

	/** Client: the tick count when the current action started. */
	private int clientActionStart;
	/** Client: how far the shield is raised into his guard, 0 to 1, now and a tick ago. */
	private float guard;
	private float guardO;
	/** Client: how far the shield hangs knocked out of the guard, 0 to 1, now and a tick ago. */
	private float knocked;
	private float knockedO;

	public FallenLord(EntityType<? extends FallenLord> type, Level level) {
		super(type, level);
		// His experience bursts from his chest when he dies: see dropExperience.
		xpReward = 0;
		setPersistenceRequired();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
		// The hitbox follows the scale attribute only when told to; its base value never changes.
		refreshDimensions();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, FallenLordSettings.DEFAULT.phaseOne().health())
				.add(Attributes.MOVEMENT_SPEED, FallenLordSettings.DEFAULT.movement().speed())
				.add(Attributes.SCALE, SCALE)
				.add(Attributes.FOLLOW_RANGE, FallenLordSettings.DEFAULT.movement().followRange())
				.add(Attributes.ATTACK_DAMAGE, FallenLordSettings.DEFAULT.phaseTwo().slashDamage())
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	private static FallenLordSettings settings() {
		return FallenLordModule.settings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE, (byte) 1);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_ACTION_SEQ, 0);
		builder.define(DATA_SHIELD_UP, true);
		builder.define(DATA_SHIELD_KNOCKED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new FallenLordFightGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
				(mob, level) -> isUndeadFoe(mob) && distanceTo(mob) <= settings().undead().radius()));
	}

	/** Undead he cuts down when they come close; never another lord. */
	static boolean isUndeadFoe(LivingEntity entity) {
		return !(entity instanceof Player) && !(entity instanceof FallenLord) && entity.is(EntityTypeTags.UNDEAD);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		// His animations swing the weapon with the right arm.
		setLeftHanded(false);
		applyMovement();
		setMaxHealthTo(settings().phaseOne().health());
		equipPhaseOne();
		bossEvent.setName(bossName());
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		// Gear is fixed, set in finalizeSpawn.
	}

	private void equipPhaseOne() {
		setItemSlot(EquipmentSlot.MAINHAND, spearStack());
		setItemSlot(EquipmentSlot.OFFHAND, shieldStack());
	}

	/** His spear: an iron spear that looks mithril. His armor is part of his model, not equipment. */
	public static ItemStack spearStack() {
		ItemStack spear = Items.IRON_SPEAR.getDefaultInstance();
		spear.set(DataComponents.ITEM_MODEL, Eam.id("fallen_lord_spear"));
		return spear;
	}

	/** His shield: a shield that looks mithril. */
	public static ItemStack shieldStack() {
		ItemStack shield = Items.SHIELD.getDefaultInstance();
		shield.set(DataComponents.ITEM_MODEL, Eam.id("fallen_lord_shield"));
		return shield;
	}

	private static ItemStack swordStack() {
		return new ItemStack(EamItems.MITHRIL_SWORD);
	}

	/** Sets the phase's health and heals him to it; the bonus for the players around still applies on top. */
	private void setMaxHealthTo(float health) {
		AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.setBaseValue(health);
		}
		setHealth(getMaxHealth());
	}

	// ---- State shared with the client ----

	public int phase() {
		return entityData.get(DATA_PHASE);
	}

	public FallenLordAction action() {
		return FallenLordAction.byId(entityData.get(DATA_ACTION));
	}

	public boolean isShieldUp() {
		return entityData.get(DATA_SHIELD_UP);
	}

	/** Client: ticks since the current action started. */
	public float actionTime(float partialTicks) {
		return tickCount - clientActionStart + partialTicks;
	}

	/** Client: how far the shield is raised into his guard, 0 to 1. */
	public float guard(float partialTicks) {
		return Mth.lerp(partialTicks, guardO, guard);
	}

	/** Client: how far the shield hangs knocked out of the guard by an axe, 0 to 1. */
	public float shieldKnocked(float partialTicks) {
		return Mth.lerp(partialTicks, knockedO, knocked);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			guardO = guard;
			guard = Mth.approach(guard, isShieldUp() && getOffhandItem().is(Items.SHIELD) ? 1.0F : 0.0F, GUARD_SPEED);
			knockedO = knocked;
			// Knocked out at once, raised back slowly.
			knocked = entityData.get(DATA_SHIELD_KNOCKED) ? Math.min(1.0F, knocked + 0.5F) : Mth.approach(knocked, 0.0F, 0.15F);
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION_SEQ.equals(accessor)) {
			clientActionStart = tickCount;
		}
	}

	// ---- Combat state used by the goal ----

	boolean isBusy() {
		return action != FallenLordAction.NONE;
	}

	boolean hasSpear() {
		return getMainHandItem().is(Items.IRON_SPEAR);
	}

	int[] cooldowns() {
		return cooldowns;
	}

	@Nullable Attack lastAttack() {
		return lastAttack;
	}

	int ticksOutOfReach() {
		return ticksOutOfReach;
	}

	int ticksSinceHit() {
		return ticksSinceHit;
	}

	boolean hopBackPending() {
		return hopBackPending;
	}

	/** Phase two: the target stands out of his reach above him, on a pillar or a ledge, but close enough to leap at. */
	boolean canLeapUp(LivingEntity target) {
		return phase() == 2 && leapUpCooldown <= 0 && onGround()
				&& FallenLordLogic.shouldLeapUp(target.getY() - getY(), horizontalDistanceTo(target), settings().later().leapUp());
	}

	/** The highest wall he jumps over on the move; a single block he steps up with the plain jump. */
	private static final int CLIMB_MAX = 3;
	/** He does not bother jumping a wall to reach a target closer than this. */
	private static final double CLIMB_FROM = 1.5;
	/** Ticks he is pushed on over the wall, and between two such jumps. */
	private static final int CLIMB_TICKS = 15;
	private static final int CLIMB_COOLDOWN = 20;
	/** Push forward over the wall, blocks per tick. */
	private static final double CLIMB_PUSH = 0.25;
	/** Out of water he climbs a bank up to this high above his feet, rising this fast. */
	private static final int WATER_CLIMB_MAX = 4;
	private static final double WATER_CLIMB_RISE = 0.4;
	private static final int WATER_CLIMB_TICKS = 25;
	private static final int STUCK_TICKS = 20;

	/**
	 * A wall two or three blocks high right in front of him on his way, with room on top of it and over his head: jumps
	 * up onto or over it. His way is the next point of his path, or straight at the target when he has none, as when
	 * the wall cuts them off.
	 */
	void tryClimb(LivingEntity target) {
		boolean swimming = isInWater();
		if (climbCooldown > 0 || climbTicks > 0 || !onGround() && !swimming || isBusy() || horizontalDistanceTo(target) < CLIMB_FROM) {
			return;
		}
		Path path = getNavigation().getPath();
		Vec3 goal = path != null && !path.isDone() ? Vec3.atBottomCenterOf(path.getNextNodePos()) : target.position();
		Vec3 direction = horizontal(goal.subtract(position()));
		if (direction == Vec3.ZERO) {
			return;
		}
		if (swimming) {
			// In water: up the bank, however low, and out.
			int height = obstacleHeight(direction, WATER_CLIMB_MAX);
			if (height >= 1) {
				climbDirection = direction;
				climbTicks = WATER_CLIMB_TICKS;
				climbCooldown = CLIMB_COOLDOWN;
				waterClimbTop = Math.floor(getY() + 0.5) + height + 0.1;
				playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.6F);
			}
			return;
		}
		int height = obstacleHeight(direction, CLIMB_MAX);
		if (height < 2) {
			return;
		}
		waterClimbTop = Double.NaN;
		climbDirection = direction;
		climbTicks = CLIMB_TICKS;
		climbCooldown = CLIMB_COOLDOWN;
		setDeltaMovement(0.0, FallenLordLogic.jumpSpeed(height + 0.2), 0.0);
		playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.6F);
	}

	/** Blocks of wall right in front of him, 0 when there is none or it is too high or there is no room to jump it. */
	private int obstacleHeight(Vec3 direction, int max) {
		double ahead = getBbWidth() * 0.5 + 0.5;
		BlockPos front = BlockPos.containing(getX() + direction.x * ahead, getY() + 0.5, getZ() + direction.z * ahead);
		int height = 0;
		while (height <= max && blocks(front.above(height))) {
			height++;
		}
		if (height == 0 || height > max) {
			return 0;
		}
		int tall = Mth.ceil(getBbHeight());
		BlockPos feet = BlockPos.containing(getX(), getY() + 0.5, getZ());
		for (int i = 0; i < tall; i++) {
			if (blocks(front.above(height + i)) || blocks(feet.above(tall + i))) {
				return 0;
			}
		}
		return height;
	}

	private boolean blocks(BlockPos pos) {
		return !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
	}

	/** Pushes him on over the wall he jumped while he is in the air. */
	private void tickClimb() {
		if (climbCooldown > 0) {
			climbCooldown--;
		}
		if (climbTicks <= 0) {
			return;
		}
		climbTicks--;
		if (isBusy()) {
			climbTicks = 0;
			waterClimbTop = Double.NaN;
			return;
		}
		if (!Double.isNaN(waterClimbTop)) {
			// Out of water: straight up along the bank, then over its edge.
			if (getY() < waterClimbTop) {
				setDeltaMovement(climbDirection.x * 0.05, WATER_CLIMB_RISE, climbDirection.z * 0.05);
				resetFallDistance();
				return;
			}
			waterClimbTop = Double.NaN;
			climbTicks = Math.min(climbTicks, CLIMB_TICKS / 2);
		} else if (onGround() && climbTicks < CLIMB_TICKS - 2) {
			climbTicks = 0;
			return;
		}
		setDeltaMovement(climbDirection.x * CLIMB_PUSH, getDeltaMovement().y, climbDirection.z * CLIMB_PUSH);
	}

	/**
	 * Pushing against a wall he can neither jump nor climb: after a moment he breaks the blocks in his way that he may
	 * break. The floor he stands on stays.
	 */
	void breakThrough(LivingEntity target) {
		if (!horizontalCollision || isBusy() || climbTicks > 0 || horizontalDistanceTo(target) <= FallenLordLogic.SWORD_REACH
				|| !(level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			stuckTicks = 0;
			return;
		}
		if (++stuckTicks < STUCK_TICKS) {
			return;
		}
		stuckTicks = 0;
		Path path = getNavigation().getPath();
		Vec3 goal = path != null && !path.isDone() ? Vec3.atBottomCenterOf(path.getNextNodePos()) : target.position();
		Vec3 direction = horizontal(goal.subtract(position()));
		if (direction == Vec3.ZERO) {
			return;
		}
		// Shrunk a little so a wall he only brushes along his side is not taken.
		AABB box = getBoundingBox().deflate(0.05, 0.0, 0.05);
		AABB ahead = new AABB(box.minX, box.minY + 0.1, box.minZ, box.maxX, box.maxY, box.maxZ)
				.expandTowards(direction.x * 0.8, 0.0, direction.z * 0.8);
		smashWithin(level, ahead);
	}

	/** His boss room, where he breaks every block weaker than obsidian; null outside a crypt. */
	public @Nullable BoundingBox getArena() {
		return arena;
	}

	/** Sets his boss room; the crypt sets it when he rises from his sarcophagus. */
	public void setArena(@Nullable BoundingBox arena) {
		this.arena = arena;
	}

	/** Whether his dashes and break-throughs take this block: the smash tag anywhere, anything weaker than obsidian in his room. */
	private boolean canSmash(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getCollisionShape(level, pos).isEmpty()) {
			return false;
		}
		if (state.is(FallenLordModule.DASH_SMASHES)) {
			return true;
		}
		return arena != null && arena.isInside(pos) && FallenLordLogic.breakableInArena(state.getDestroySpeed(level, pos));
	}

	private void smashWithin(ServerLevel level, AABB area) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
			if (canSmash(level, pos, level.getBlockState(pos))) {
				level.destroyBlock(pos, true, this);
			}
		}
	}

	void startLeapUp(LivingEntity target) {
		leapUpCooldown = settings().later().leapUp().cooldown();
		setAction(FallenLordAction.LEAP_UP, target);
	}

	boolean isIdleFor(int ticks) {
		return idleTicks >= ticks;
	}

	/** Players that can be hit, within {@code radius} of the boss and in his sight. */
	List<Player> playersWithin(double radius) {
		return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius, 2.0, radius),
				player -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && player.isAlive() && horizontalDistanceTo(player) <= radius
						&& hasLineOfSight(player));
	}

	/** What an area attack hits: nearby players, plus his current target if that is some other mob. */
	private List<LivingEntity> victimsWithin(double radius) {
		List<LivingEntity> victims = new ArrayList<>(playersWithin(radius));
		LivingEntity target = getTarget();
		if (target != null && !(target instanceof Player) && target.isAlive() && horizontalDistanceTo(target) <= radius
				&& Math.abs(target.getY() - getY()) < 2.5 && hasLineOfSight(target)) {
			victims.add(target);
		}
		return victims;
	}

	double horizontalDistanceTo(LivingEntity other) {
		double dx = other.getX() - getX();
		double dz = other.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	boolean wantsToDrink() {
		return FallenLordLogic.shouldDrink(getHealth(), getMaxHealth(), drinksThisPhase, drinkCooldown, settings());
	}

	/** Leaps back from the target, then drinks. */
	void startDrinking(@Nullable LivingEntity target) {
		drinksThisPhase++;
		drinkCooldown = settings().regen().cooldown();
		healedThisDrink = 0.0F;
		drinkAfterRetreat = true;
		stanceAfterRetreat = false;
		retreatSpeed = settings().regen().retreat() / RETREAT_TICKS;
		setAction(FallenLordAction.RETREAT, target);
	}

	/** Lost a lot of health quickly: leaps back with all his cooldowns reset, sometimes to drink. */
	private void leapBack() {
		wounds.clear();
		Arrays.fill(cooldowns, 0);
		drinkCooldown = 0;
		shieldDisabledTicks = 0;
		hitStreak = HitStreak.NONE;
		swipePending = false;
		FallenLordSettings.Regen regen = settings().regen();
		if (drinksThisPhase < regen.usesPerPhase() && regen.maxHeal() > 0.0F && getHealth() < getMaxHealth()
				&& random.nextDouble() < settings().burst().drinkChance()) {
			startDrinking(getTarget());
		} else {
			drinkAfterRetreat = false;
			stanceAfterRetreat = false;
			retreatSpeed = BURST_LEAP / RETREAT_TICKS;
			setAction(FallenLordAction.RETREAT, getTarget());
		}
	}

	/** Phase two: leaps back from the target and takes his fencing stance there. */
	private void leapIntoStance(double distance, LivingEntity target) {
		attacksInRow = 0;
		drinkAfterRetreat = false;
		stanceAfterRetreat = true;
		retreatSpeed = distance / RETREAT_TICKS;
		setAction(FallenLordAction.RETREAT, target);
	}

	/** Hurt one player a lot in a short time: he backs off into his stance so the fight is not over at once. */
	private void takeBreather() {
		FallenLordSettings.Breather breather = settings().later().breather();
		breatherCooldown = breather.cooldown();
		breatherHits = 0;
		blows.clear();
		LivingEntity target = getTarget();
		if (target != null && target.isAlive()) {
			leapIntoStance(breather.distance(), target);
		}
	}

	/** After an attack in phase two, maybe leaps back from a player; likelier with every attack in a row. */
	private void maybeLeapBack() {
		FallenLordSettings.LeapBack leapBack = settings().later().leapBack();
		LivingEntity target = getTarget();
		if (target instanceof Player && target.isAlive() && horizontalDistanceTo(target) <= leapBack.maxRange() && hasLineOfSight(target)
				&& random.nextDouble() < FallenLordLogic.leapBackChance(attacksInRow, leapBack)) {
			leapIntoStance(leapBack.distance(), target);
		}
	}

	private boolean canLeapBack() {
		return action != FallenLordAction.TRANSITION && action != FallenLordAction.RETREAT && action != FallenLordAction.DRINK
				&& action != FallenLordAction.COUNTER;
	}

	void startAttack(Attack attack, LivingEntity target) {
		lastAttack = attack;
		// Against undead his attacks are free.
		boolean free = isUndeadFoe(target) && settings().undead().freeAbilities();
		cooldowns[attack.ordinal()] = free ? 0 : FallenLordLogic.cooldown(attack, settings());
		dashesChained = 0;
		if (attack == Attack.HOP || attack == Attack.HOP_BACK) {
			startHop(attack == Attack.HOP, target);
			return;
		}
		FallenLordAction next = switch (attack) {
			case POKE -> FallenLordAction.POKE;
			case DASH -> FallenLordAction.DASH_WINDUP;
			case THROW -> FallenLordAction.THROW;
			case SWIPE -> FallenLordAction.SWIPE;
			case SLASH -> FallenLordAction.SLASH;
			case COMBO -> FallenLordAction.COMBO;
			case LUNGE -> FallenLordAction.LUNGE;
			case WHIRL -> FallenLordAction.WHIRL;
			case SWORD_DASH -> FallenLordAction.SWORD_DASH_WINDUP;
			case BACKSTAB -> FallenLordAction.BACKSTAB;
			case HEAVY -> FallenLordAction.HEAVY_WINDUP;
			case HOP, HOP_BACK -> throw new IllegalStateException();
		};
		setAction(next, target);
	}

	private void startHop(boolean towards, LivingEntity target) {
		hopTowards = towards;
		if (!towards) {
			hopBackPending = false;
		}
		double length = FallenLordLogic.hopLength(towards, horizontalDistanceTo(target), settings().movement(), random.nextDouble());
		hopSpeed = length / HOP_PUSH_TICKS;
		Vec3 to = horizontal(target.position().subtract(position()));
		rushDirection = towards ? to : to.reverse();
		setAction(FallenLordAction.HOP, target);
	}

	private void setAction(FallenLordAction next, @Nullable LivingEntity target) {
		action = next;
		actionTicks = 0;
		actionTarget = target;
		actionHit = false;
		dashHits.clear();
		dashSpans.clear();
		dashSpent = false;
		getNavigation().stop();
		// The duel strafe would otherwise keep sliding him sideways.
		getMoveControl().strafe(0.0F, 0.0F);
		if (ATTACK_STARTS.contains(next)) {
			attacksInRow++;
		}
		entityData.set(DATA_ACTION, (byte) next.ordinal());
		entityData.set(DATA_ACTION_SEQ, entityData.get(DATA_ACTION_SEQ) + 1);
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		if (drinkCooldown > 0) {
			drinkCooldown--;
		}
		if (breatherCooldown > 0) {
			breatherCooldown--;
		}
		if (leapUpCooldown > 0) {
			leapUpCooldown--;
		}
		tickClimb();
		if (shieldDisabledTicks > 0 && --shieldDisabledTicks == 0) {
			hitStreak = HitStreak.NONE;
			swipePending = false;
			hopBackPending = false;
		}
		if (tickCount % PLAYER_COUNT_INTERVAL == 0) {
			countPlayers(level);
			regenUnseen(level);
		}
		if (spearAwayTicks >= 0 && ++spearAwayTicks > SPEAR_LOST_TICKS) {
			if (thrownSpear != null && level.getEntity(thrownSpear) instanceof ThrownLordSpear lost) {
				lost.discard();
			}
			rearm();
		}

		long now = level.getGameTime();
		FallenLordSettings.Burst burst = settings().burst();
		wounds.removeIf(wound -> now - wound.tick() > burst.window());
		if (!wounds.isEmpty() && canLeapBack() && FallenLordLogic.lostQuickly(wounds, now, getMaxHealth(), burst)) {
			leapBack();
		}

		if (breatherPending) {
			breatherPending = false;
			if (phase() == 2 && canLeapBack() && action != FallenLordAction.STANCE) {
				takeBreather();
			}
		}

		LivingEntity target = getTarget();
		ticksSinceHit++;
		if (tickCount % PLAYER_PRIORITY_INTERVAL == 0 && target != null && !(target instanceof Player)) {
			// A player always comes first.
			Player player = level.getNearestPlayer(TargetingConditions.forCombat().range(getAttributeValue(Attributes.FOLLOW_RANGE)), this);
			if (player != null) {
				setTarget(player);
				target = player;
			}
		}
		ticksOutOfReach = target != null && horizontalDistanceTo(target) > FallenLordLogic.CLOSE ? ticksOutOfReach + 1 : 0;
		tickSprint(target);

		if (isBusy()) {
			idleTicks = 0;
			tickAction(level);
		} else {
			idleTicks++;
			if (swipePending) {
				swipePending = false;
				if (phase() == 1 && hasSpear() && target != null && horizontalDistanceTo(target) <= FallenLordLogic.SWORD_REACH + 1.5) {
					startAttack(Attack.SWIPE, target);
				}
			}
		}

		entityData.set(DATA_SHIELD_UP, phase() == 1 && shieldDisabledTicks <= 0 && !getOffhandItem().isEmpty()
				&& action != FallenLordAction.DASH && action != FallenLordAction.SWIPE && action != FallenLordAction.RETREAT
				&& action != FallenLordAction.DRINK && action != FallenLordAction.COUNTER);
		entityData.set(DATA_SHIELD_KNOCKED, phase() == 1 && shieldDisabledTicks > 0 && !getOffhandItem().isEmpty());
		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget != null && actionTarget.isAlive() ? actionTarget : null;
		if (actionTarget != null && target == null && BROKEN_BY_KILL.contains(action)) {
			// The target is dead: no swinging at nothing, straight on to the next one.
			endAction();
			findNewTarget(level);
			idleTicks = Integer.MAX_VALUE / 2;
			return;
		}
		if (target != null && action != FallenLordAction.DASH && action != FallenLordAction.SWORD_DASH && action != FallenLordAction.WHIRL
				&& action != FallenLordAction.HEAVY_STRIKE) {
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			faceTowards(target);
		}
		FallenLordSettings.PhaseOne one = settings().phaseOne();
		FallenLordSettings.PhaseTwo two = settings().phaseTwo();
		switch (action) {
			case POKE -> {
				if (t == 6 && target != null) {
					hitIfInReach(level, target, FallenLordLogic.POKE_REACH, one.pokeDamage(), one.pokeKnockback());
				}
			}
			case DASH_WINDUP -> {
				if (t == 0) {
					// The cue that the unblockable dash is coming, with the eye flash.
					playSound(EamSounds.FALLEN_LORD_DASH_WARNING, 2.0F, 1.0F);
				}
				if (t == action.length() - 1) {
					if (target == null) {
						break;
					}
					// He runs through where the player stands at this moment and on past it; stepping aside dodges it.
					rushDirection = horizontal(target.position().subtract(position()));
					dashTarget = target.position().add(rushDirection.scale(FallenLordLogic.DASH_OVERSHOOT));
					dashStart = position();
					dashLength = Math.max(1.0, horizontal2(dashTarget.subtract(dashStart)));
					setAction(FallenLordAction.DASH, target);
					playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 0.8F);
					return;
				}
			}
			case DASH -> {
				smashAhead(level, DASH_SPEED);
				Vec3 left = dashTarget.subtract(position());
				if (t > 0 && (horizontalCollision || horizontal(left).dot(rushDirection) <= 0.0 || left.horizontalDistance() < 0.6)
						|| t >= action.length() - 1) {
					finishDash(target);
					return;
				}
				setDeltaMovement(rushDirection.x * DASH_SPEED, getDeltaMovement().y, rushDirection.z * DASH_SPEED);
				// Weaker point-blank, stronger and wider towards the end of the run.
				FallenLordSettings.SpearDash spearDash = settings().later().spearDash();
				double progress = horizontal2(position().subtract(dashStart)) / dashLength;
				double reach = FallenLordLogic.DASH_HIT_RADIUS * FallenLordLogic.alongDash(spearDash.startReach(), spearDash.endReach(), progress);
				float damage = one.dashDamage() * (float) FallenLordLogic.alongDash(spearDash.startDamage(), spearDash.endDamage(), progress);
				// Everyone in the way is hit, each once per dash.
				for (LivingEntity victim : victimsWithin(reach)) {
					if (!dashHits.contains(victim.getUUID())
							&& strike(level, victim, damageSources().source(FallenLordModule.DASH_DAMAGE, this), damage, 1.0F)) {
						dashHits.add(victim.getUUID());
						actionHit = true;
					}
				}
			}
			case THROW -> {
				if (t == 8 && target != null && hasSpear()) {
					throwSpear(level, target, one.throwDamage(), true);
				}
			}
			case SWIPE -> {
				if (t == 0) {
					shieldDisabledTicks = 0;
					hitStreak = HitStreak.NONE;
				}
				if (t == 7) {
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
					for (LivingEntity victim : victimsWithin(FallenLordLogic.SWORD_REACH + REACH_SLACK)) {
						if (inFront(victim, 0.0)) {
							strike(level, victim, damageSources().mobAttack(this), one.swipeDamage(), one.swipeKnockback());
						}
					}
				}
			}
			case TRANSITION -> tickTransition(level, t, target);
			case SLASH -> {
				if (t == 5 && target != null) {
					hitIfInReach(level, target, FallenLordLogic.SWORD_REACH, two.slashDamage(), 0.4F);
				}
			}
			case COMBO -> tickCombo(level, t, target, two);
			case LUNGE -> tickLunge(level, t, target, two);
			case WHIRL -> {
				if (t == 8) {
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
					for (LivingEntity victim : victimsWithin(two.whirlRadius())) {
						strike(level, victim, damageSources().mobAttack(this), two.slashDamage() * two.whirlMultiplier(), two.whirlKnockback());
					}
				}
			}
			case RETREAT -> {
				if (t == 0) {
					rushDirection = target != null ? horizontal(position().subtract(target.position()))
							: Vec3.directionFromRotation(0.0F, getYRot()).reverse();
					setDeltaMovement(rushDirection.x * retreatSpeed, 0.3, rushDirection.z * retreatSpeed);
					playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.2F, 0.9F);
				} else if (t < RETREAT_TICKS && !horizontalCollision) {
					setDeltaMovement(rushDirection.x * retreatSpeed, getDeltaMovement().y, rushDirection.z * retreatSpeed);
				} else if (t == RETREAT_TICKS) {
					// He lands where the leap was meant to take him instead of sliding on.
					setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
				}
				if (t == action.length() - 1) {
					if (drinkAfterRetreat) {
						drinkAfterRetreat = false;
						setAction(FallenLordAction.DRINK, target);
					} else if (stanceAfterRetreat && target != null && phase() == 2) {
						stanceAfterRetreat = false;
						setAction(FallenLordAction.STANCE, target);
					} else {
						stanceAfterRetreat = false;
						endAction();
					}
					return;
				}
			}
			case DRINK -> {
				if (target != null && horizontalDistanceTo(target) < settings().regen().keepAway()) {
					// Backs away from the player as he drinks.
					getMoveControl().strafe(-0.8F, 0.0F);
				}
				if (tickDrink(level, t)) {
					endAction();
					return;
				}
			}
			case COUNTER -> {
				if (t == 0 && target != null) {
					// Turns on the spot to whoever hit him.
					turnTo(target);
				}
				if (t == 3) {
					FallenLordSettings.Regen regen = settings().regen();
					playSound(phase() == 1 ? SoundEvents.SHIELD_BLOCK.value() : SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
					for (LivingEntity victim : victimsWithin(FallenLordLogic.COUNTER_REACH + REACH_SLACK)) {
						if (inFront(victim, 0.0)) {
							// The answer must land even right after another hit.
							victim.damageCooldownTime = 0;
							strike(level, victim, damageSources().mobAttack(this), regen.counterDamage(), regen.counterKnockback());
						}
					}
				}
			}
			case SWORD_DASH_WINDUP -> {
				if (t == 0) {
					// No red eyes: this one can be blocked. The cue is the sword drawn back with a rasp of metal.
					playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 1.4F);
				}
				if (t == action.length() - 1) {
					if (target == null) {
						break;
					}
					// Straight at where the player stands now; stepping aside dodges it.
					rushDirection = horizontal(target.position().subtract(position()));
					dashTarget = target.position();
					setAction(FallenLordAction.SWORD_DASH, target);
					playSound(SoundEvents.LUNGE_2.value(), 1.5F, 0.8F);
					return;
				}
			}
			case SWORD_DASH -> {
				if (tickSwordDash(level, t, target)) {
					return;
				}
			}
			case HOP -> {
				if (tickHop(t, target)) {
					return;
				}
			}
			case BACKSTAB -> tickBackstab(level, t, target);
			case HEAVY_WINDUP -> {
				if (t == 0) {
					// The same cue as the spear dash: get out of reach.
					playSound(EamSounds.FALLEN_LORD_DASH_WARNING, 2.0F, 1.0F);
					playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.5F, 0.5F);
				} else if (t == FallenLordAction.HEAVY_GLOW) {
					// The blade catches fire along its edge.
					playSound(SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F);
				}
				if (t == action.length() - 1) {
					setAction(FallenLordAction.HEAVY_STRIKE, target);
					return;
				}
			}
			case HEAVY_STRIKE -> {
				if (t == FallenLordAction.HEAVY_HIT && target != null && heavyStrike(level, target)) {
					return;
				}
			}
			case STANCE -> {
				if (tickStance(t, target)) {
					return;
				}
			}
			case LEAP_UP -> {
				if (tickLeapUp(level, t, target)) {
					return;
				}
			}
			case PARRY, NONE -> {
			}
		}
		if (actionTicks >= action.length()) {
			endAction();
		}
	}

	/** Ticks from springing off to the top of the leap up. */
	private static final int LEAP_UP_RISE = FallenLordAction.LEAP_UP_CUT - FallenLordAction.LEAP_UP_LAUNCH;
	/** At the top of the leap up he is this far short of the player, along the ground, so he lands in front of them. */
	private static final double LEAP_UP_SHORT = 0.7;

	/**
	 * One tick of the leap up at a player standing too high; true once he has landed. He crouches, then is steered
	 * along a fixed path to beside the player's feet, so the cut at the top always comes on the same tick; after it he
	 * falls.
	 */
	private boolean tickLeapUp(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (t < FallenLordAction.LEAP_UP_LAUNCH) {
			if (t == 0) {
				playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.5F, 0.6F);
			}
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
			return false;
		}
		if (t == FallenLordAction.LEAP_UP_LAUNCH) {
			leapUpStart = position();
			playSound(SoundEvents.GOAT_LONG_JUMP, 1.5F, 0.6F);
		}
		if (t < FallenLordAction.LEAP_UP_CUT) {
			if (target == null) {
				return false;
			}
			Vec3 to = horizontal(target.position().subtract(leapUpStart));
			double flat = Math.max(0.0, horizontalDistanceTo(leapUpStart, target) - LEAP_UP_SHORT);
			int done = t - FallenLordAction.LEAP_UP_LAUNCH + 1;
			double share = (double) done / LEAP_UP_RISE;
			Vec3 next = leapUpStart.add(to.scale(flat * share))
					.add(0.0, (target.getY() + 0.3 - leapUpStart.y) * FallenLordLogic.leapUpHeight(done, LEAP_UP_RISE), 0.0);
			setDeltaMovement(next.subtract(position()));
			resetFallDistance();
			return false;
		}
		if (t == FallenLordAction.LEAP_UP_CUT) {
			setDeltaMovement(0.0, 0.0, 0.0);
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
			FallenLordSettings.LeapUp leapUp = settings().later().leapUp();
			if (target != null && distanceTo(target) <= FallenLordLogic.SWORD_REACH + 0.5 && hasLineOfSight(target)) {
				strike(level, target, damageSources().mobAttack(this), leapUp.damage(), leapUp.knockback());
			}
			return false;
		}
		setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
		if (onGround()) {
			endAction();
			return true;
		}
		return false;
	}

	private static double horizontalDistanceTo(Vec3 from, Entity other) {
		double dx = other.getX() - from.x;
		double dz = other.getZ() - from.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/**
	 * One tick of the fencing stance; true when it has handed over to another action. A player who steps into reach
	 * gets a riposte; one who heals or runs is attacked again.
	 */
	private boolean tickStance(int t, @Nullable LivingEntity target) {
		if (target == null) {
			endAction();
			return true;
		}
		FallenLordSettings.Stance stance = settings().later().stance();
		double distance = horizontalDistanceTo(target);
		if (t == 0) {
			stanceDistance = distance;
			stanceHealth = target.getHealth();
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2F, 1.2F);
		}
		if (FallenLordLogic.stanceApproached(distance, stanceDistance, stance) && hasLineOfSight(target)) {
			// Coming at him: a lunging stab.
			startAttack(Attack.LUNGE, target);
			return true;
		}
		boolean eating = target.isUsingItem() && target.getUseItem().has(DataComponents.CONSUMABLE);
		if (t >= stance.duration() || FallenLordLogic.stanceBroken(distance, stanceDistance, target.getHealth(), stanceHealth, eating, stance)) {
			// Passive: healing, running or waiting it out. A dash at once, or straight back into the fight.
			if (FallenLordLogic.dashFromStance(distance) && hasLineOfSight(target)) {
				startAttack(Attack.SWORD_DASH, target);
				return true;
			}
			endAction();
			idleTicks = Integer.MAX_VALUE / 2;
			return true;
		}
		return false;
	}

	/** One tick of a hop; true when it has handed over to another action. */
	private boolean tickHop(int t, @Nullable LivingEntity target) {
		if (t == 0) {
			setDeltaMovement(rushDirection.x * hopSpeed, HOP_LIFT, rushDirection.z * hopSpeed);
			playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 0.6F);
		} else if (t < HOP_PUSH_TICKS && !horizontalCollision) {
			setDeltaMovement(rushDirection.x * hopSpeed, getDeltaMovement().y, rushDirection.z * hopSpeed);
		}
		if (t == action.length() - 1 && !hopTowards && target != null) {
			// Room made: the spear or a dash follows at once.
			Attack next = FallenLordLogic.afterHopBack(phase(), hasSpear(), cooldowns);
			if (next != null) {
				startAttack(next, target);
				return true;
			}
		}
		return false;
	}

	/** Phase two: darts round the player's side to their back, where a raised shield does not cover, and cuts. */
	private void tickBackstab(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			double[] path = FallenLordLogic.backstabPath(target.getYRot(), getX() - target.getX(), getZ() - target.getZ());
			backstabSide = new Vec3(path[0], 0.0, path[1]);
			backstabBack = new Vec3(path[2], 0.0, path[3]);
			playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.2F, 0.8F);
		} else if (t >= FallenLordAction.BACKSTAB_DART && t < FallenLordAction.BACKSTAB_CUT) {
			// Follows the player as they move, round the side they faced when he started.
			boolean toSide = t < FallenLordAction.BACKSTAB_BACK;
			Vec3 point = target.position().add(toSide ? backstabSide : backstabBack);
			int left = (toSide ? FallenLordAction.BACKSTAB_BACK : FallenLordAction.BACKSTAB_CUT) - t;
			Vec3 step = new Vec3(point.x - getX(), 0.0, point.z - getZ()).scale(1.0 / left);
			if (step.length() > BACKSTAB_MAX_SPEED) {
				step = step.normalize().scale(BACKSTAB_MAX_SPEED);
			}
			setDeltaMovement(step.x, getDeltaMovement().y, step.z);
			if (t == FallenLordAction.BACKSTAB_DART) {
				playSound(SoundEvents.LUNGE_1.value(), 1.2F, 1.1F);
			}
		} else if (t == FallenLordAction.BACKSTAB_CUT) {
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
			turnTo(target);
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.9F);
			hitIfInReach(level, target, FallenLordLogic.SWORD_REACH, settings().backstab().damage(), 0.4F);
		}
	}

	/**
	 * The overhead cut after the red-bladed windup. A shield takes only part of it and is knocked out. True when it
	 * landed and he went on into the combo.
	 */
	private boolean heavyStrike(ServerLevel level, LivingEntity target) {
		FallenLordSettings.HeavyStrike heavy = settings().heavyStrike();
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.5F);
		double reach = FallenLordLogic.SWORD_REACH + heavy.extraReach() + REACH_SLACK;
		if (horizontalDistanceTo(target) > reach || Math.abs(target.getY() - getY()) >= 2.5 || !inFront(target, 0.3) || !hasLineOfSight(target)) {
			return false;
		}
		float damage = settings().phaseTwo().slashDamage() * heavy.damageMultiplier();
		ItemStack shield = target.getItemBlockingWith();
		boolean blocked = shield != null && target.isBlocking()
				&& FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ());
		if (blocked) {
			// Through the shield: part of the blow lands, and the shield is knocked out of the guard.
			damage *= heavy.blockedShare();
			knockShieldOut(level, target, heavy.shieldSeconds());
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.7F);
		}
		if (!strike(level, target, damageSources().source(FallenLordModule.HEAVY_DAMAGE, this), damage, 0.6F)) {
			return false;
		}
		lastAttack = Attack.COMBO;
		if (!(isUndeadFoe(target) && settings().undead().freeAbilities())) {
			cooldowns[Attack.COMBO.ordinal()] = FallenLordLogic.cooldown(Attack.COMBO, settings());
		}
		setAction(FallenLordAction.COMBO, target);
		return true;
	}

	/** A dash that hit goes straight into the next one, from where he ended up behind the player. */
	private void finishDash(@Nullable LivingEntity target) {
		if (actionHit && target != null && dashesChained < settings().phaseOne().dashChain()) {
			dashesChained++;
			setAction(FallenLordAction.DASH_WINDUP, target);
		} else {
			endAction();
		}
	}

	/**
	 * A dash smashes weak blocks (tag {@link FallenLordModule#DASH_SMASHES}, or anything weaker than obsidian in his room) in his way before he reaches them, so it is
	 * not stopped by them. The floor he runs on stays. Only so deep, though: once the blocks in his path would take the
	 * dash past {@link FallenLordSettings.DashBreak#maxDepth()}, he smashes nothing more and the next block stops him.
	 */
	private void smashAhead(ServerLevel level, double speed) {
		if (dashSpent || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		AABB box = getBoundingBox();
		AABB ahead = new AABB(box.minX, box.minY + 0.1, box.minZ, box.maxX, box.maxY, box.maxZ)
				.expandTowards(rushDirection.x * (speed + 0.5), 0.0, rushDirection.z * (speed + 0.5));
		List<BlockPos> smashable = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(ahead.minX, ahead.minY, ahead.minZ),
				BlockPos.containing(ahead.maxX, ahead.maxY, ahead.maxZ))) {
			if (canSmash(level, pos, level.getBlockState(pos))) {
				smashable.add(pos.immutable());
			}
		}
		// Nearest first, so the depth limit keeps the far blocks standing.
		smashable.sort(Comparator.comparingDouble(this::alongRush));
		double maxDepth = settings().later().dashBreak().maxDepth();
		double halfWidth = getBbWidth() / 2.0;
		for (BlockPos pos : smashable) {
			Vec3 offset = Vec3.atCenterOf(pos).subtract(position());
			double sideways = offset.x * rushDirection.z - offset.z * rushDirection.x;
			if (FallenLordLogic.inDashPath(sideways, halfWidth)) {
				// Measured from the world origin, which stays put while he runs: the direction does not change mid-dash.
				Vec3 centre = Vec3.atCenterOf(pos);
				double along = centre.x * rushDirection.x + centre.z * rushDirection.z;
				dashSpans.add(FallenLordLogic.blockSpan(along, rushDirection.x, rushDirection.z));
				if (FallenLordLogic.dashDepth(dashSpans) > maxDepth + 1.0E-6) {
					dashSpans.removeLast();
					dashSpent = true;
					return;
				}
			}
			level.destroyBlock(pos, true, this);
		}
	}

	/** How far ahead along the dash a block's centre lies, from where he stands. */
	private double alongRush(BlockPos pos) {
		Vec3 offset = Vec3.atCenterOf(pos).subtract(position());
		return offset.x * rushDirection.x + offset.z * rushDirection.z;
	}

	/** One tick of the sword dash; true when it has handed over to another action. */
	private boolean tickSwordDash(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target != null && horizontalDistanceTo(target) <= FallenLordLogic.SWORD_DASH_STOP + 0.3 && Math.abs(target.getY() - getY()) < 2.5) {
			// Point-blank: he stops dead and cuts. A shield takes the cut but is knocked out; the player is pushed back either way.
			setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
			breaksShields = true;
			boolean hurt = strike(level, target, damageSources().mobAttack(this), settings().swordDash().damage(), 0.0F);
			breaksShields = false;
			if (!hurt) {
				target.knockback(SWORD_DASH_PUSH, getX() - target.getX(), getZ() - target.getZ(), damageSources().mobAttack(this), 0.0F);
				// A blocked hit does not send the player their new motion by itself.
				target.syncVelocity = true;
			}
			// Straight into the combo while the player reels.
			lastAttack = Attack.COMBO;
			cooldowns[Attack.COMBO.ordinal()] = FallenLordLogic.cooldown(Attack.COMBO, settings());
			setAction(FallenLordAction.COMBO, target);
			return true;
		}
		smashAhead(level, SWORD_DASH_SPEED);
		Vec3 left = dashTarget.subtract(position());
		if (t > 0 && (horizontalCollision || horizontal(left).dot(rushDirection) <= 0.0)) {
			endAction();
			return true;
		}
		setDeltaMovement(rushDirection.x * SWORD_DASH_SPEED, getDeltaMovement().y, rushDirection.z * SWORD_DASH_SPEED);
		return false;
	}

	private void tickCombo(ServerLevel level, int t, @Nullable LivingEntity target, FallenLordSettings.PhaseTwo two) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			// Leap at the player.
			Vec3 to = horizontal(target.position().subtract(position()));
			double leap = Math.min(1.0, horizontalDistanceTo(target) * 0.16);
			setDeltaMovement(to.x * leap, 0.35, to.z * leap);
			playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.2F, 0.7F);
		} else if (t > 6 && t < 30 && horizontalDistanceTo(target) > 1.8 && onGround()) {
			// Keeps pressing in between the slashes.
			Vec3 to = horizontal(target.position().subtract(position()));
			setDeltaMovement(to.x * 0.2, getDeltaMovement().y, to.z * 0.2);
		}
		if (t == 10 || t == 16 || t == 22) {
			playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + t * 0.01F);
			// The hits come faster than the usual half-second of hurt immunity; each one must land.
			target.damageCooldownTime = 0;
			hitIfInReach(level, target, FallenLordLogic.SWORD_REACH + 0.4, two.comboHitDamage(), 0.1F);
		} else if (t == 30) {
			playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
			target.damageCooldownTime = 0;
			hitIfInReach(level, target, FallenLordLogic.SWORD_REACH, two.comboKickDamage(), two.comboKnockback());
		}
	}

	private void tickLunge(ServerLevel level, int t, @Nullable LivingEntity target, FallenLordSettings.PhaseTwo two) {
		int push = FallenLordAction.LUNGE_PUSH;
		if (t == push && target != null) {
			rushDirection = horizontal(target.position().subtract(position()));
		}
		if (t >= push && t < push + LUNGE_RUSH_TICKS) {
			setDeltaMovement(rushDirection.x * 0.8, getDeltaMovement().y, rushDirection.z * 0.8);
			if (!actionHit && target != null && horizontalDistanceTo(target) <= 2.2) {
				actionHit = true;
				strike(level, target, damageSources().mobAttack(this), two.lungeDamage(), 0.5F);
				actionTicks = push + LUNGE_RUSH_TICKS;
			}
		} else if (t == push + LUNGE_RUSH_TICKS && actionHit) {
			// Hop back out of reach after a hit; after a miss he stays on the player.
			setDeltaMovement(-rushDirection.x * 0.7, 0.3, -rushDirection.z * 0.7);
		}
	}

	private void tickTransition(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (t == 0) {
			ItemStack shield = getOffhandItem();
			if (!shield.isEmpty()) {
				LordShield dropped = new LordShield(level, getX(), getY() + 1.0, getZ(), getYRot() + 30.0F);
				Vec3 side = Vec3.directionFromRotation(0.0F, getYRot() - 90.0F).scale(0.25);
				dropped.setDeltaMovement(side.x, 0.25, side.z);
				level.addFreshEntity(dropped);
				setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			}
			playSound(SoundEvents.WITHER_SKELETON_HURT, 2.0F, 0.5F);
		} else if (t == 14) {
			if (hasSpear()) {
				if (target != null) {
					throwSpear(level, target, settings().phaseOne().throwDamage(), false);
				} else {
					setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				}
			}
		} else if (t == 30) {
			setItemSlot(EquipmentSlot.MAINHAND, swordStack());
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.7F);
		}
	}

	/** One tick of the drink; true when it is done. */
	private boolean tickDrink(ServerLevel level, int t) {
		FallenLordSettings.Regen regen = settings().regen();
		if (t % 8 == 0) {
			playSound(SoundEvents.GENERIC_DRINK.value(), 1.0F, 0.6F);
		}
		if (t % 4 == 0) {
			level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, REGEN_COLOR), getX(), getY(0.6), getZ(), 2,
					getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 1.0);
		}
		float amount = Math.min(regen.healPerSecond() / 20.0F, regen.maxHeal() - healedThisDrink);
		if (amount > 0.0F) {
			heal(amount);
			healedThisDrink += amount;
		}
		if (healedThisDrink >= regen.maxHeal() || getHealth() >= getMaxHealth()) {
			playSound(SoundEvents.PLAYER_BURP, 1.0F, 0.6F);
			return true;
		}
		return false;
	}

	private void endAction() {
		FallenLordAction ended = action;
		if (ended == FallenLordAction.TRANSITION && !getMainHandItem().is(EamItems.MITHRIL_SWORD)) {
			setItemSlot(EquipmentSlot.MAINHAND, swordStack());
		}
		setAction(FallenLordAction.NONE, null);
		if (phase() == 2 && ATTACK_ENDS.contains(ended)) {
			maybeLeapBack();
		}
	}

	private void throwSpear(ServerLevel level, LivingEntity target, float damage, boolean returning) {
		float undead = isUndeadFoe(target) ? settings().undead().damageMultiplier() : 1.0F;
		ThrownLordSpear spear = new ThrownLordSpear(level, this, damage * damageMultiplier() * undead, returning);
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double dy = target.getY(0.5) - spear.getY();
		double distance = Math.sqrt(dx * dx + dz * dz);
		spear.shoot(dx, dy + distance * 0.12, dz, 2.0F, 2.0F);
		level.addFreshEntity(spear);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.7F);
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		spearAwayTicks = returning ? 0 : -1;
		thrownSpear = returning ? spear.getUUID() : null;
	}

	/** A thrown spear came back to his hand; only the one he is waiting for counts. */
	void catchSpear(ThrownLordSpear spear) {
		if (spear.getUUID().equals(thrownSpear)) {
			rearm();
		}
	}

	private void rearm() {
		spearAwayTicks = -1;
		thrownSpear = null;
		if (phase() == 1 && getMainHandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, spearStack());
		}
	}

	private void hitIfInReach(ServerLevel level, LivingEntity target, double reach, float damage, float knockback) {
		if (horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.5 && inFront(target, 0.3)
				&& hasLineOfSight(target)) {
			strike(level, target, damageSources().mobAttack(this), damage, knockback);
		}
	}

	private boolean strike(ServerLevel level, LivingEntity target, DamageSource source, float damage, float knockback) {
		float scaled = damage * damageMultiplier();
		if (isUndeadFoe(target)) {
			scaled *= settings().undead().damageMultiplier();
		}
		float before = target.getHealth() + target.getAbsorptionAmount();
		if (!target.hurtServer(level, source, scaled)) {
			return false;
		}
		ticksSinceHit = 0;
		chaseTicks = 0;
		sprinting = false;
		if (phase() == 2 && target instanceof Player) {
			noteBlow(level, target, before - target.getHealth() - target.getAbsorptionAmount());
		}
		if (knockback > 0.0F) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ(), source, scaled);
		}
		setLastHurtMob(target);
		return true;
	}

	/** With no player in sight he regenerates a little every second. */
	private void regenUnseen(ServerLevel level) {
		float regen = settings().later().vampirism().unseenRegen();
		if (regen <= 0.0F || getHealth() >= getMaxHealth() || action == FallenLordAction.TRANSITION) {
			return;
		}
		double range = getAttributeValue(Attributes.FOLLOW_RANGE);
		boolean seen = !level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
				player -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && player.isAlive() && distanceToSqr(player) <= range * range
						&& hasLineOfSight(player)).isEmpty();
		if (!seen) {
			heal(regen * PLAYER_COUNT_INTERVAL / 20.0F);
		}
	}

	/** A player he killed gives him back a share of the phase's health. */
	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
		boolean result = super.killedEntity(level, victim, source);
		if (victim == getTarget()) {
			findNewTarget(level);
		}
		double share = settings().later().vampirism().killHeal();
		if (victim instanceof Player && share > 0.0 && isAlive()) {
			heal((float) (getMaxHealth() * share));
			level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0x8B0000), getX(), getY(0.6), getZ(), 30,
					getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 1.0);
			playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 2.0F, 0.4F);
		}
		return result;
	}

	/**
	 * Right after a kill he turns on the next foe he can see, without the target goals' usual delay: the nearest
	 * player, otherwise the nearest undead within his reach for them.
	 */
	private void findNewTarget(ServerLevel level) {
		LivingEntity next = level.getNearestPlayer(TargetingConditions.forCombat().range(getAttributeValue(Attributes.FOLLOW_RANGE)), this);
		if (next == null || !hasLineOfSight(next)) {
			double radius = settings().undead().radius();
			next = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(radius),
							mob -> mob.isAlive() && isUndeadFoe(mob) && distanceTo(mob) <= radius && hasLineOfSight(mob)).stream()
					.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		}
		setTarget(next);
	}

	/** Counts a hit on a player for the breather. */
	private void noteBlow(ServerLevel level, LivingEntity player, float lost) {
		FallenLordSettings.Breather breather = settings().later().breather();
		long now = level.getGameTime();
		List<Wound> list = blows.computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>());
		list.removeIf(blow -> now - blow.tick() > breather.window());
		list.add(new Wound(now, Math.max(0.0F, lost)));
		if (breatherHits <= 0) {
			breatherHits = FallenLordLogic.breatherHits(breather, random.nextDouble());
		}
		if (breatherCooldown <= 0 && FallenLordLogic.needsBreather(list, now, breatherHits, breather)) {
			breatherPending = true;
		}
	}

	// ---- Players around: his strength and their shields ----

	/** All his damage is scaled by the number of players around him. */
	private float damageMultiplier() {
		return (float) Math.max(0.0, 1.0 + FallenLordLogic.scaling(players, settings().players()).damage());
	}

	/** Counts the players around him and sets his health and speed for them; his health keeps its share. */
	private void countPlayers(ServerLevel level) {
		FallenLordSettings.PlayerScaling scaling = settings().players();
		double radius = scaling.radius();
		players = Math.max(1, level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius),
				player -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && player.isAlive() && distanceToSqr(player) <= radius * radius).size());
		FallenLordSettings.PlayerScaling.Level bonus = FallenLordLogic.scaling(players, scaling);
		float share = getHealth() / getMaxHealth();
		if (setBonus(Attributes.MAX_HEALTH, bonus.health())) {
			setHealth(share * getMaxHealth());
		}
		setBonus(Attributes.MOVEMENT_SPEED, bonus.speed());
		long now = level.getGameTime();
		blockStreaks.values().removeIf(streak -> now - streak.lastTick() > settings().shieldBreak().window());
		int window = settings().later().breather().window();
		blows.values().removeIf(list -> list.isEmpty() || now - list.getLast().tick() > window);
	}

	/** Sets the players bonus on an attribute; true when it changed. */
	private boolean setBonus(Holder<Attribute> attribute, double amount) {
		return setBonus(attribute, PLAYERS_BONUS, amount);
	}

	/** Sets a bonus share of the base value on an attribute; true when it changed. */
	private boolean setBonus(Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) {
			return false;
		}
		AttributeModifier current = instance.getModifier(id);
		if (current == null ? amount == 0.0 : current.amount() == amount) {
			return false;
		}
		if (amount == 0.0) {
			instance.removeModifier(id);
		} else {
			instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		return true;
	}

	/** A target he has not caught for a while: he sprints after them until his first hit lands. */
	private void tickSprint(@Nullable LivingEntity target) {
		FallenLordSettings.Sprint sprint = settings().later().sprint();
		if (target == null) {
			chaseTicks = 0;
			sprinting = false;
		} else if (!sprinting) {
			chaseTicks = FallenLordLogic.chaseTicks(chaseTicks, horizontalDistanceTo(target), phase(), isBusy());
			sprinting = chaseTicks >= sprint.after();
		}
		setBonus(Attributes.MOVEMENT_SPEED, SPRINT_BONUS, sprinting ? sprint.speed() : 0.0);
	}

	/** A player's shield blocked his hit: after a few such hits he knocks the shield out, the way an axe does. */
	@Override
	protected void blockedByItem(LivingEntity defender, DamageSource source, float damage, boolean fullyBlocked) {
		super.blockedByItem(defender, source, damage, fullyBlocked);
		if (!(defender instanceof Player) || !(level() instanceof ServerLevel level)) {
			return;
		}
		if (breaksShields) {
			blockStreaks.remove(defender.getUUID());
			knockShieldOut(level, defender, settings().swordDash().shieldSeconds());
			return;
		}
		FallenLordSettings.ShieldBreak shieldBreak = settings().shieldBreak();
		BlockStreak streak = FallenLordLogic.addBlock(blockStreaks.getOrDefault(defender.getUUID(), BlockStreak.NONE), level.getGameTime(),
				shieldBreak, random.nextDouble());
		if (!FallenLordLogic.breaksShield(streak)) {
			blockStreaks.put(defender.getUUID(), streak);
			return;
		}
		blockStreaks.remove(defender.getUUID());
		knockShieldOut(level, defender, shieldBreak.seconds());
	}

	private static void knockShieldOut(ServerLevel level, LivingEntity defender, float seconds) {
		ItemStack shield = defender.getItemBlockingWith();
		BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
		if (blocks != null) {
			blocks.disable(level, defender, seconds, shield);
		}
	}

	private boolean inFront(LivingEntity other, double minDot) {
		Vec3 to = horizontal(other.position().subtract(position()));
		Vec3 facing = Vec3.directionFromRotation(0.0F, getYRot());
		return to.lengthSqr() < 1.0E-6 || to.dot(facing) >= minDot;
	}

	/** Turns on the spot to face {@code target} at once. */
	private void turnTo(LivingEntity target) {
		float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	private void faceTowards(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 30.0F));
		setYBodyRot(getYRot());
	}

	private static double horizontal2(Vec3 vector) {
		return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}

	// ---- Damage: shield and phases ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (action == FallenLordAction.TRANSITION && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		if (isShieldUp() && source.getDirectEntity() instanceof LivingEntity attacker && source.getEntity() == attacker
				&& !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.BYPASSES_SHIELD) && !source.is(DamageTypes.THORNS)
				&& FallenLordLogic.shieldCovers(yBodyRot, attacker.getX() - getX(), attacker.getZ() - getZ())) {
			blockWithShield(level, attacker);
			return false;
		}
		if (source.is(FallenLordModule.PHYSICAL)) {
			damage *= (float) (1.0 - settings().physicalResistance());
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && action == FallenLordAction.DRINK && source.getEntity() instanceof Player player) {
			// A player's hit stops the drink, and he hits back at once.
			setAction(FallenLordAction.COUNTER, player);
			return true;
		}
		if (hurt && action == FallenLordAction.STANCE && source.getEntity() instanceof LivingEntity attacker) {
			// Hit in the stance: a riposte if they are in reach, otherwise he goes after them.
			if (horizontalDistanceTo(attacker) <= FallenLordLogic.SWORD_REACH + 1.0) {
				startAttack(Attack.SLASH, attacker);
			} else {
				endAction();
			}
			return true;
		}
		if (hurt && phase() == 1 && shieldDisabledTicks > 0 && source.getEntity() instanceof Player) {
			hitStreak = FallenLordLogic.addHit(hitStreak, level.getGameTime(), settings().phaseOne().swipeWindow());
			if (FallenLordLogic.shouldSwipe(hitStreak, settings())) {
				hitStreak = HitStreak.NONE;
				swipePending = true;
			}
		}
		return hurt;
	}

	private void blockWithShield(ServerLevel level, LivingEntity attacker) {
		float seconds = attacker.getSecondsToDisableBlocking();
		if (seconds > 0.0F) {
			shieldDisabledTicks = FallenLordLogic.shieldDisableTicks(seconds, settings().phaseOne().shieldDisableMultiplier());
			hitStreak = HitStreak.NONE;
			hopBackPending = true;
			entityData.set(DATA_SHIELD_UP, false);
			entityData.set(DATA_SHIELD_KNOCKED, true);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.7F);
		} else {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
		}
	}

	/**
	 * Arrows from the front glance off the raised shield, or in phase two he knocks them aside with the sword when he
	 * is not busy attacking.
	 */
	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		if (!(projectile instanceof AbstractArrow) || projectile.getOwner() == this) {
			return super.deflection(projectile);
		}
		Vec3 motion = projectile.getDeltaMovement();
		if (!FallenLordLogic.shieldCovers(yBodyRot, -motion.x, -motion.z)) {
			return super.deflection(projectile);
		}
		if (isShieldUp()) {
			return this::deflectWithShield;
		}
		FallenLordAction now = action();
		if (phase() == 2 && (now == FallenLordAction.NONE || now == FallenLordAction.PARRY || now == FallenLordAction.STANCE)
				&& getMainHandItem().is(EamItems.MITHRIL_SWORD)) {
			return this::parryWithSword;
		}
		return super.deflection(projectile);
	}

	/** Runs on the server only. */
	private void deflectWithShield(Projectile projectile, @Nullable Entity by, RandomSource random, Vec3 power) {
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
		ProjectileDeflection.REVERSE.deflect(projectile, by, random, power);
	}

	/** Runs on the server only. The arrow flies off to one side, slowed, and he flicks the sword. */
	private void parryWithSword(Projectile projectile, @Nullable Entity by, RandomSource random, Vec3 power) {
		Vec3 motion = projectile.getDeltaMovement();
		Vec3 side = new Vec3(-motion.z, 0.0, motion.x).normalize().scale(random.nextBoolean() ? 1.0 : -1.0);
		projectile.setDeltaMovement(side.scale(motion.length() * 0.3).add(motion.scale(-0.1)).add(0.0, 0.2, 0.0));
		projectile.needsSync = true;
		level().playSound(null, getX(), getY(0.6), getZ(), SoundEvents.ANVIL_LAND, getSoundSource(), 0.35F, 1.8F + random.nextFloat() * 0.2F);
		setAction(FallenLordAction.PARRY, getTarget());
	}

	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		if (before > getHealth()) {
			wounds.add(new Wound(level.getGameTime(), before - getHealth()));
		}
		// /kill and the void still kill him outright.
		if (phase() == 1 && getHealth() <= 0.0F && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			startPhaseTwo();
		}
	}

	private void startPhaseTwo() {
		entityData.set(DATA_PHASE, (byte) 2);
		setMaxHealthTo(settings().phaseTwo().health());
		shieldDisabledTicks = 0;
		hopBackPending = false;
		swipePending = false;
		drinksThisPhase = 0;
		drinkAfterRetreat = false;
		stanceAfterRetreat = false;
		attacksInRow = 0;
		blows.clear();
		dashesChained = 0;
		wounds.clear();
		spearAwayTicks = -1;
		thrownSpear = null;
		Arrays.fill(cooldowns, 0);
		// No combo or whirl right after the change: he opens with plain hits and lunges.
		cooldowns[Attack.COMBO.ordinal()] = 60;
		cooldowns[Attack.WHIRL.ordinal()] = 100;
		cooldowns[Attack.HEAVY.ordinal()] = 80;
		lastAttack = null;
		bossEvent.setColor(BossEvent.BossBarColor.RED);
		bossEvent.setName(bossName());
		setAction(FallenLordAction.TRANSITION, getTarget());
	}

	@Override
	public int getArmorValue() {
		// The worn armor is for looks; protection comes from the data file.
		return settings().armor();
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	/** Orbs fly about this many times their starting speed before they settle. */
	private static final double ORB_TRAVEL = 13.0;

	/**
	 * Experience bursts from his chest in two spheres, scaled by the players around him. Dropped under the same
	 * conditions as any mob's experience.
	 */
	@Override
	protected void dropExperience(ServerLevel level, @Nullable Entity killer) {
		if (wasExperienceConsumed() || lastHurtByPlayerMemoryTime <= 0 || !shouldDropExperience() || !level.getGameRules().get(GameRules.MOB_DROPS)) {
			return;
		}
		FallenLordSettings.Death death = settings().death();
		int xp = FallenLordLogic.deathXp(players, death);
		int[] values = FallenLordLogic.orbValues(xp, FallenLordLogic.orbCount(xp, death));
		int inner = FallenLordLogic.innerOrbs(values.length, death);
		Vec3 chest = new Vec3(getX(), getY(0.65), getZ());
		for (int i = 0; i < values.length; i++) {
			double radius = i < inner ? death.innerRadius() : death.outerRadius();
			// Out and a little up in any direction, most orbs near the edge of their sphere.
			double yaw = random.nextDouble() * Math.PI * 2.0;
			double pitch = 0.25 + random.nextDouble() * 0.75;
			double speed = radius / ORB_TRAVEL * (0.55 + 0.45 * Math.sqrt(random.nextDouble()));
			Vec3 motion = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch)).scale(speed);
			ExperienceOrb orb = new ExperienceOrb(level, chest.x, chest.y, chest.z, values[i]);
			// The constructor gives it a random nudge; the burst decides instead.
			orb.setDeltaMovement(motion);
			level.addFreshEntity(orb);
		}
		level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.PLAYER_LEVELUP, getSoundSource(), 1.0F, 0.6F);
	}

	/** The long death: ash, falling armor; then the armor crumbles away too. */
	@Override
	protected void tickDeath() {
		deathTime++;
		if (level() instanceof ServerLevel level) {
			deathEffects(level, deathTime);
			if (deathTime >= DEATH_TICKS && !isRemoved()) {
				level.broadcastEntityEvent(this, (byte) 60);
				remove(RemovalReason.KILLED);
			}
		}
	}

	private void deathEffects(ServerLevel level, int t) {
		double w = getBbWidth() * 0.3;
		if (t >= ASH_START && t < ASH_END) {
			// Ash pours out of the gaps of the armor as the bones burn away.
			level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(0.6), getZ(), 12, w, getBbHeight() * 0.3, w, 0.02);
			level.sendParticles(ParticleTypes.ASH, getX(), getY(0.5), getZ(), 6, w, getBbHeight() * 0.3, w, 0.0);
			if (t % 3 == 0) {
				level.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.85), getZ(), 3, w * 0.5, 0.1, w * 0.5, 0.01);
			}
			if (t % 6 == 0) {
				playSound(SoundEvents.SAND_BREAK, 0.8F, 0.6F);
			}
		}
		if (t == ASH_END) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 12, w, getBbHeight() * 0.25, w, 0.02);
		}
		if (t == COLLAPSE_START + 8 || t == COLLAPSE_END - 4) {
			// The empty armor clatters to the ground.
			playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 0.6F);
			playSound(SoundEvents.CHAIN_FALL, 1.0F, 0.7F);
		}
		if (t == DEATH_TICKS - 1) {
			level.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(0.1), getZ(), 40, getBbWidth() * 0.6, 0.1, getBbWidth() * 0.6, 0.02);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		// No loot yet: it comes with the crypt treasury.
	}

	// ---- Boss bar ----

	private Component bossName() {
		return Component.translatable("entity.eam.fallen_lord.phase", getDisplayName(), phase());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		bossEvent.setName(bossName());
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("Phase", (byte) phase());
		output.putInt("ShieldDisabled", shieldDisabledTicks);
		output.putInt("Drinks", drinksThisPhase);
		output.putInt("DrinkCooldown", drinkCooldown);
		output.storeNullable("Arena", BoundingBox.CODEC, arena);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int phase = Mth.clamp(input.getByteOr("Phase", (byte) 1), 1, 2);
		entityData.set(DATA_PHASE, (byte) phase);
		shieldDisabledTicks = input.getIntOr("ShieldDisabled", 0);
		arena = input.read("Arena", BoundingBox.CODEC).orElse(null);
		drinksThisPhase = input.getIntOr("Drinks", 0);
		drinkCooldown = input.getIntOr("DrinkCooldown", 0);
		applyBaseStats(phase);
		bossEvent.setColor(phase == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE);
		bossEvent.setName(bossName());
		// Older saves wore rusty iron armor; his armor is drawn by his model now.
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			setItemSlot(slot, ItemStack.EMPTY);
		}
		// Saved mid-throw or mid-change (the action itself is not saved): give back what he should hold.
		if (phase == 2) {
			setItemSlot(EquipmentSlot.MAINHAND, swordStack());
			setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		} else {
			if (getMainHandItem().isEmpty() || getMainHandItem().is(Items.IRON_SPEAR)) {
				setItemSlot(EquipmentSlot.MAINHAND, spearStack());
			}
			// He only puts the shield down when phase two starts; this also arms a lord summoned with NBT.
			if (getOffhandItem().isEmpty() || getOffhandItem().is(Items.SHIELD)) {
				setItemSlot(EquipmentSlot.OFFHAND, shieldStack());
			}
		}
	}

	/** Saved lords keep the size, pace and health they were spawned with; bring them to the current ones. */
	private void applyBaseStats(int phase) {
		setLeftHanded(false);
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(SCALE);
		}
		applyMovement();
		AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			boolean full = getHealth() >= maxHealth.getValue();
			maxHealth.setBaseValue(phase == 2 ? settings().phaseTwo().health() : settings().phaseOne().health());
			setHealth(full ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
		}
	}

	/** His pace and sight from the data file. */
	private void applyMovement() {
		FallenLordSettings.Movement movement = settings().movement();
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(movement.speed());
		}
		AttributeInstance followRange = getAttribute(Attributes.FOLLOW_RANGE);
		if (followRange != null) {
			followRange.setBaseValue(movement.followRange());
		}
	}

	// ---- Sounds ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_SKELETON_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SKELETON_STEP, 0.2F, 0.7F);
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}
}
