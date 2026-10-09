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
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boss of the crypt. Phase one: spear and shield, a cautious duel. Phase two: shield dropped, sword drawn, aggressive.
 * Each phase has its own health; the boss bar shows the current one. Attacks run here as {@link FallenLordAction}s;
 * {@link FallenLordFightGoal} moves him and picks them with {@link FallenLordLogic}.
 */
public class FallenLord extends Monster {
	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BYTE);
	/** Bumped on every new action, so clients restart the animation even when the same action repeats. */
	private static final EntityDataAccessor<Integer> DATA_ACTION_SEQ = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_SHIELD_UP = SynchedEntityData.defineId(FallenLord.class, EntityDataSerializers.BOOLEAN);

	/** Hits land this far beyond the nominal reach, so a target backing off in the swing still gets hit. */
	private static final double REACH_SLACK = 0.6;
	private static final double DASH_SPEED = 0.9;
	private static final double SWORD_DASH_SPEED = 1.0;
	/** A blocked sword dash still pushes the player back a block, as a hit does. */
	private static final double SWORD_DASH_PUSH = 0.4;
	/** The leap back before drinking: this fast for this many ticks, about six blocks. */
	private static final double RETREAT_SPEED = 0.75;
	private static final int RETREAT_TICKS = 8;
	/** Swirls of the regeneration potion while he drinks. */
	private static final int REGEN_COLOR = 0xCD5CAB;
	/** A wither skeleton's pace. */
	private static final double MOVEMENT_SPEED = 0.25;
	/** A tenth bigger than the wither skeleton he is modelled on. */
	private static final double SCALE = 1.1;
	/** Without the spear back by then (lost in a wall, unloaded) he gets a new one. */
	private static final int SPEAR_LOST_TICKS = 120;
	/** His health and speed bonuses for the players around him. */
	private static final Identifier PLAYERS_BONUS = Eam.id("fallen_lord_players");
	/** He counts the players around him this often. */
	private static final int PLAYER_COUNT_INTERVAL = 20;
	/** How fast the shield arm comes up into the guard and drops, per tick. */
	private static final float GUARD_SPEED = 0.2F;

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
	private int idleTicks;
	private int shieldDisabledTicks;
	private HitStreak hitStreak = HitStreak.NONE;
	private boolean swipePending;
	private int ticksOutOfReach;
	private int spearAwayTicks = -1;
	/** The spear in flight that will come back; an older one that turns up late is ignored. */
	private @Nullable UUID thrownSpear;
	private int drinksThisPhase;
	private int drinkCooldown;
	private float healedThisDrink;
	/** The leap back ends in a drink. */
	private boolean drinkAfterRetreat;
	/** Dashes he has followed the first one with. */
	private int dashesChained;
	/** Set while the sword dash strikes: a shield that blocks it is knocked out at once. */
	private boolean breaksShields;
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

	public FallenLord(EntityType<? extends FallenLord> type, Level level) {
		super(type, level);
		xpReward = 100;
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
				.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
				.add(Attributes.SCALE, SCALE)
				.add(Attributes.FOLLOW_RANGE, 32.0)
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
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new FallenLordFightGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		// His animations swing the weapon with the right arm.
		setLeftHanded(false);
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

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			guardO = guard;
			guard = Mth.approach(guard, isShieldUp() && getOffhandItem().is(Items.SHIELD) ? 1.0F : 0.0F, GUARD_SPEED);
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
			setAction(FallenLordAction.RETREAT, getTarget());
		}
	}

	private boolean canLeapBack() {
		return action != FallenLordAction.TRANSITION && action != FallenLordAction.RETREAT && action != FallenLordAction.DRINK
				&& action != FallenLordAction.COUNTER;
	}

	void startAttack(Attack attack, LivingEntity target) {
		lastAttack = attack;
		cooldowns[attack.ordinal()] = FallenLordLogic.cooldown(attack, settings());
		dashesChained = 0;
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
		};
		setAction(next, target);
	}

	private void setAction(FallenLordAction next, @Nullable LivingEntity target) {
		action = next;
		actionTicks = 0;
		actionTarget = target;
		actionHit = false;
		getNavigation().stop();
		// The duel strafe would otherwise keep sliding him sideways.
		getMoveControl().strafe(0.0F, 0.0F);
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
		if (shieldDisabledTicks > 0 && --shieldDisabledTicks == 0) {
			hitStreak = HitStreak.NONE;
			swipePending = false;
		}
		if (tickCount % PLAYER_COUNT_INTERVAL == 0) {
			countPlayers(level);
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

		LivingEntity target = getTarget();
		ticksOutOfReach = target != null && horizontalDistanceTo(target) > FallenLordLogic.CLOSE ? ticksOutOfReach + 1 : 0;

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
		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget != null && actionTarget.isAlive() ? actionTarget : null;
		if (target != null && action != FallenLordAction.DASH && action != FallenLordAction.SWORD_DASH && action != FallenLordAction.WHIRL) {
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
					setAction(FallenLordAction.DASH, target);
					playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 0.8F);
					return;
				}
			}
			case DASH -> {
				Vec3 left = dashTarget.subtract(position());
				if (t > 0 && (horizontalCollision || horizontal(left).dot(rushDirection) <= 0.0 || left.horizontalDistance() < 0.6)
						|| t >= action.length() - 1) {
					finishDash(target);
					return;
				}
				setDeltaMovement(rushDirection.x * DASH_SPEED, getDeltaMovement().y, rushDirection.z * DASH_SPEED);
				for (LivingEntity victim : victimsWithin(FallenLordLogic.DASH_HIT_RADIUS)) {
					if (!actionHit) {
						actionHit = strike(level, victim, damageSources().source(FallenLordModule.DASH_DAMAGE, this), one.dashDamage(), 1.0F);
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
					setDeltaMovement(rushDirection.x * RETREAT_SPEED, 0.3, rushDirection.z * RETREAT_SPEED);
					playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.2F, 0.9F);
				} else if (t < RETREAT_TICKS && !horizontalCollision) {
					setDeltaMovement(rushDirection.x * RETREAT_SPEED, getDeltaMovement().y, rushDirection.z * RETREAT_SPEED);
				}
				if (t == action.length() - 1) {
					if (drinkAfterRetreat) {
						drinkAfterRetreat = false;
						setAction(FallenLordAction.DRINK, target);
					} else {
						endAction();
					}
					return;
				}
			}
			case DRINK -> {
				if (tickDrink(level, t)) {
					endAction();
					return;
				}
			}
			case COUNTER -> {
				if (t == 0 && target != null) {
					// Turns on the spot to whoever hit him.
					float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
					setYRot(yaw);
					setYBodyRot(yaw);
					setYHeadRot(yaw);
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
			case PARRY, NONE -> {
			}
		}
		if (actionTicks >= action.length()) {
			endAction();
		}
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
		if (t == 4 && target != null) {
			rushDirection = horizontal(target.position().subtract(position()));
		}
		if (t >= 4 && t < 10) {
			setDeltaMovement(rushDirection.x * 0.8, getDeltaMovement().y, rushDirection.z * 0.8);
			if (!actionHit && target != null && horizontalDistanceTo(target) <= 2.2) {
				actionHit = true;
				strike(level, target, damageSources().mobAttack(this), two.lungeDamage(), 0.5F);
				actionTicks = 10;
			}
		} else if (t == 10) {
			// Hop back out of reach.
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
		if (action == FallenLordAction.TRANSITION && !getMainHandItem().is(EamItems.MITHRIL_SWORD)) {
			setItemSlot(EquipmentSlot.MAINHAND, swordStack());
		}
		setAction(FallenLordAction.NONE, null);
	}

	private void throwSpear(ServerLevel level, LivingEntity target, float damage, boolean returning) {
		ThrownLordSpear spear = new ThrownLordSpear(level, this, damage * damageMultiplier(), returning);
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
		if (!target.hurtServer(level, source, scaled)) {
			return false;
		}
		if (knockback > 0.0F) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ(), source, scaled);
		}
		setLastHurtMob(target);
		return true;
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
	}

	/** Sets the players bonus on an attribute; true when it changed. */
	private boolean setBonus(Holder<Attribute> attribute, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) {
			return false;
		}
		AttributeModifier current = instance.getModifier(PLAYERS_BONUS);
		if (current == null ? amount == 0.0 : current.amount() == amount) {
			return false;
		}
		if (amount == 0.0) {
			instance.removeModifier(PLAYERS_BONUS);
		} else {
			instance.addOrReplacePermanentModifier(new AttributeModifier(PLAYERS_BONUS, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		return true;
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

	private void faceTowards(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 30.0F));
		setYBodyRot(getYRot());
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
			entityData.set(DATA_SHIELD_UP, false);
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
		if (phase() == 2 && (now == FallenLordAction.NONE || now == FallenLordAction.PARRY) && getMainHandItem().is(EamItems.MITHRIL_SWORD)) {
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
		swipePending = false;
		drinksThisPhase = 0;
		drinkAfterRetreat = false;
		dashesChained = 0;
		wounds.clear();
		spearAwayTicks = -1;
		thrownSpear = null;
		Arrays.fill(cooldowns, 0);
		// No combo or whirl right after the change: he opens with plain hits and lunges.
		cooldowns[Attack.COMBO.ordinal()] = 60;
		cooldowns[Attack.WHIRL.ordinal()] = 100;
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int phase = Mth.clamp(input.getByteOr("Phase", (byte) 1), 1, 2);
		entityData.set(DATA_PHASE, (byte) phase);
		shieldDisabledTicks = input.getIntOr("ShieldDisabled", 0);
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
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(MOVEMENT_SPEED);
		}
		AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			boolean full = getHealth() >= maxHealth.getValue();
			maxHealth.setBaseValue(phase == 2 ? settings().phaseTwo().health() : settings().phaseOne().health());
			setHealth(full ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
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
