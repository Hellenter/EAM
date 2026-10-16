package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.boss.ParryPenalty;
import io.github.hellenter.eam.mobs.entity.boss.ParryStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.Attack;

import net.minecraft.core.BlockPos;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

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
 * target, and he hops aside and back. Phases two and three are not built yet: for now he dies at the end of the first.
 * Numbers are in {@code data/eam/mobs/koschei.json}.
 */
public class Koschei extends Monster {
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
	/** Where the blow he turns aside comes at him, packed by {@link #packGuard}; the client holds the sword there. */
	private static final EntityDataAccessor<Integer> DATA_GUARD = SynchedEntityData.defineId(Koschei.class, EntityDataSerializers.INT);
	/** Hits further than this flat distance past their reach still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** The lunge stops this close to the target and stabs from there. */
	private static final double LUNGE_STOP = 1.8;
	private static final double LUNGE_SPEED = 0.8;
	private static final double LUNGE_REACH = 2.8;
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
	/** He walks in this fast while the sword is raised over his head. */
	private static final double OVERHEAD_WALK_SPEED = 0.2;
	/** The kick's step in stops this close to the target. */
	private static final double KICK_STOP = 1.4;

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
	private int teleportCooldown;
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

	public Koschei(EntityType<? extends Koschei> type, Level level) {
		super(type, level);
		xpReward = settings().stats().xp();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
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

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_PHASE, (byte) 1);
		for (EntityDataAccessor<Integer> slot : DATA_BOLT_TARGETS) {
			builder.define(slot, -1);
		}
		builder.define(DATA_PARRY_OPEN, false);
		builder.define(DATA_GUARD, packGuard(0.6F, 0.0F));
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new KoscheiFightGoal(this));
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
		setBase(Attributes.MAX_HEALTH, stats.health());
		setBase(Attributes.ARMOR, stats.armor());
		setBase(Attributes.MOVEMENT_SPEED, stats.speed());
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

	/** The phase he fights in: 1 for now. */
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
		return action == KoscheiAction.NONE && idleTicks >= settings().stats().attackPause();
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
		}
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) cooldowns[i]--;
		}
		if (hopCooldown > 0) hopCooldown--;
		if (parryPlayer != null && level.getGameTime() > parryUntil) {
			closeParry();
		}
		if (teleportCooldown > 0) teleportCooldown--;
		if (leapFrom != null) {
			leapBack(leapFrom);
			leapFrom = null;
		}
		LivingEntity target = getTarget();
		if (target == null && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(settings().stats().regen());
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
				boltLanded.clear();
				clearBolt();
				setAction(KoscheiAction.BOLT);
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
		action = next;
		actionTicks = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
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
			if (ended == KoscheiAction.STAGGER && target != null && target.isAlive()) {
				// Back on his feet after a parry: he throws the one who parried him off.
				actionTarget = target;
				cooldowns[Attack.FORCE.ordinal()] = phaseOne().force().cooldown();
				setAction(KoscheiAction.FORCE);
			} else if (ended == KoscheiAction.TELEPORT) {
				// Behind the target: the next blow comes at once.
				idleTicks = settings().stats().attackPause();
			}
			return;
		}
		boolean free = action == KoscheiAction.STAGGER || action == KoscheiAction.PARRY_ARROW || action == KoscheiAction.BLOCK
				|| action == KoscheiAction.HOP;
		if (!free && (target == null || !target.isAlive())) {
			endAction();
			return;
		}
		if (action != KoscheiAction.HOP) {
			getNavigation().stop();
		}
		if (target != null && action != KoscheiAction.STAGGER && action != KoscheiAction.HOP) {
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			faceTowards(target);
		}
		KoscheiSettings.PhaseOne one = phaseOne();
		int parryStart = action.parryStart(parryWindow(action));
		if (t == parryStart && parryStart >= 0) {
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
				} else if (t == action.length() - 1 && target != null) {
					teleportBehind(level, target, settings().teleport());
				}
			}
			case HOP -> {
				if (t < HOP_PUSH_TICKS && !horizontalCollision) {
					double speed = settings().hop().speed();
					setDeltaMovement(rushDirection.x * speed, t == 0 ? HOP_LIFT : getDeltaMovement().y, rushDirection.z * speed);
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
		if (horizontalDistanceTo(target) > phaseOne().reach() - 0.8 && onGround()) {
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
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(force.range() + FORCE_AIMED_SLACK, 2.5, force.range() + FORCE_AIMED_SLACK),
				e -> e != this && e.isAlive() && !(e instanceof ArmorStand) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
			Vec3 to = victim.position().subtract(position());
			Vec3 flat = horizontal(to);
			if (flat == Vec3.ZERO) {
				flat = facing;
			}
			// The one he casts at is caught unless a wall is between them, even a few steps out of range; others in front of him too.
			boolean aimed = victim == target && horizontalDistanceTo(victim) <= force.range() + FORCE_AIMED_SLACK;
			boolean inCone = horizontalDistanceTo(victim) <= force.range() + victim.getBbWidth() * 0.5 && flat.dot(facing) >= 0.4;
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
			if (holdsBolt(target, bolt)) {
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
			if (t % 4 == 0) {
				level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, getSoundSource(), 0.5F,
						1.8F + random.nextFloat() * 0.2F);
			}
			if (!pulse) {
				continue;
			}
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, struck.getX(), struck.getY(0.6), struck.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
			if (blockingShieldFrom(struck, tip) != null) {
				// The shield takes the lightning.
				level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 0.8F, 1.5F);
				continue;
			}
			struck.damageCooldownTime = 0;
			strike(level, struck, damageSources().source(KoscheiModule.LIGHTNING_DAMAGE, this),
					KoscheiLogic.boltPulse(bolt.damagePerSecond(), bolt.pulse()), 0.0F);
			if (boltLanded.add(struck.getId())) {
				struck.addEffect(new MobEffectInstance(KoscheiModule.CONVULSIONS, KoscheiAction.BOLT_END - t, 0, false, true, true), this);
			}
		}
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

	// ---- Teleport and hops ----

	void startTeleport(LivingEntity target) {
		teleportCooldown = settings().teleport().cooldown();
		actionTarget = target;
		setAction(KoscheiAction.TELEPORT);
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
			warpCloud(level, from, 1.0);
			level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.5F, 0.6F);
			teleportTo(spot.x, spot.y, spot.z);
			getNavigation().stop();
			faceTowards(target);
			setYHeadRot(getYRot());
			warpCloud(level, spot, 0.4);
			level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.5F, 0.7F);
			return;
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
		if (!onGround() || direction.lengthSqr() < 1.0E-6) {
			return;
		}
		hopCooldown = settings().hop().cooldown();
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

	/** Whether his sword is free to turn blows and arrows aside: not while he is thrown off balance, casting or vanishing. */
	private boolean guarding() {
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
		boolean melee = attacker instanceof LivingEntity && source.getDirectEntity() == attacker;
		if (melee && attacker instanceof Player player && player.getUUID().equals(parryPlayer) && level.getGameTime() <= parryUntil) {
			// The hit back after his lunge knocked the shield out.
			float hit = damage * BossParry.penalty().multiplier(phaseOne().parry().multiplier(), parryPenalized);
			closeParry();
			boolean hurt = super.hurtServer(level, source, resisted(source, hit));
			if (isAlive()) {
				parried(level, player);
			}
			return hurt;
		}
		if (melee && attacker instanceof Player player) {
			ParryPenalty penalty = BossParry.penalty();
			boolean penalized = parryStreak.penalized(player.getUUID(), level.getGameTime(), penalty);
			if (KoscheiLogic.parries(action, actionTicks - 1, penalty.window(parryWindow(action), penalized))) {
				float hit = damage * penalty.multiplier(phaseOne().parry().multiplier(), penalized);
				boolean hurt = super.hurtServer(level, source, resisted(source, hit));
				if (isAlive()) {
					parried(level, player);
				}
				return hurt;
			}
		}
		if (melee && guarding() && FallenLordLogic.shieldCovers(yBodyRot, attacker.getX() - getX(), attacker.getZ() - getZ())) {
			blockBlow(level, (LivingEntity) attacker);
			return false;
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
		if (isAlive()) {
			leapIfHurtFast(level, source, before - getHealth());
		}
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
		bossEvent.setName(getDisplayName());
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
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
		applyStats(false);
		bossEvent.setName(getDisplayName());
		Arrays.fill(cooldowns, 0);
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
}
