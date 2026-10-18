package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.boss.BossBuilding;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.boss.BuildingAware;
import io.github.hellenter.eam.mobs.entity.boss.DizzyStars;
import io.github.hellenter.eam.mobs.entity.boss.ParryPenalty;
import io.github.hellenter.eam.mobs.entity.boss.ParryStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic.Attack;
import io.github.hellenter.eam.mobs.vanilla.feud.PillagerFeudModule;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * The Pillager King: a giant two and a half blocks tall in closed black plate, a long netherite axe in each hand.
 * He cuts once, in a four-cut combo that a hit at its start parries, in an onslaught (a rush and a leap with both axes
 * for a blow a shield only softens, parried by a hit while he is in the air), spins in a long tornado of weak blockable
 * cuts, throws his left axe to knock a raised shield out or round the target into its back, and from afar charges after
 * his eyes flash red for three cuts, the first knocking a raised shield out, and a kick, slower after the first cut; a
 * charge into a wall or a pillar leaves him stunned for a moment. He knocks arrows from the front aside. He dashes at players a few blocks off and hops back from those who crowd him,
 * and runs after one he could not reach for a while. A totem of undying saves him once. Numbers are in
 * {@code data/eam/mobs/pillager_king.json}.
 */
public class PillagerKing extends Monster implements AxeThrower, BuildingAware {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(PillagerKing.class, EntityDataSerializers.BYTE);
	private static final Identifier RUN_MODIFIER = Eam.id("pillager_king_run");
	/** Slower after the first cut of a charge, to the end of it. */
	private static final Identifier CHARGE_SLOW_MODIFIER = Eam.id("pillager_king_charge_slow");
	/** The look of his axes, named by the item_model component. */
	public static final Identifier AXE_MODEL = Eam.id("pillager_king_axe");
	/** Hits further than this flat distance past their reach still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** A dash stops this much short of the hit's reach, so the cut lands even if the target keeps backing off. */
	private static final double DASH_STOP_SHORT = 1.0;
	/** The thrown axe is back in his hand after this many ticks at the latest, even if it got lost. */
	private static final int AXE_BACK_AFTER = 100;
	/** How far his kick reaches, flat from his centre. */
	private static final double KICK_REACH = 2.4;
	/** The charge reaches the target this close. */
	private static final double CHARGE_CONTACT = 2.0;
	/** How far ahead of him a wall is looked for when the charge is blocked. */
	private static final double WALL_PROBE = 0.25;
	/** The combo ends with a hop back on this tick, if the hop is ready. */
	private static final int COMBO_HOP = 32;

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

	private KingAction action = KingAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	/** Who parried him and when: a quick second parry is harder. */
	private final ParryStreak parryStreak = new ParryStreak();
	private int idleTicks;
	private final int[] cooldowns = new int[Attack.values().length];
	private int dashCooldown;
	private int dashTicks;
	private @Nullable LivingEntity dashTarget;
	private int hopCooldown;
	/** Damage he took lately: when and how much, oldest first. */
	private final ArrayDeque<Hit> recentHits = new ArrayDeque<>();
	/** Where the one who hurt him fast stands: he leaps back from there on his next tick, after the hit's knockback. */
	private @Nullable Vec3 leapFrom;
	/** Ticks his target has stayed out of his reach. */
	private int outOfReach;
	/** Ticks his target has stood out of his reach above him, on a pillar or a ledge. */
	private int highTicks;
	private int climbCooldown;
	private int chopCooldown;
	/** Where a climb started, and which way the wall is. */
	private double climbFrom;
	private Vec3 climbDirection = Vec3.ZERO;
	/** He came off a wall he climbed: no fall damage on landing. */
	private boolean fellFromClimb;
	/** Whether he has already decided how to reach the coming hit. */
	private boolean hitChecked;
	/** His axe knocked the target's shield out: the next attack starts at once. */
	private boolean followUp;
	/** Ticks since he threw his left axe, or -1 while he holds it. */
	private int axeAway = -1;
	private boolean totemUsed;
	/** Client: the tick count when the current action started. */
	private int clientActionStart;

	public PillagerKing(EntityType<? extends PillagerKing> type, Level level) {
		super(type, level);
		xpReward = settings().stats().xp();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		PillagerKingSettings.Stats stats = PillagerKingSettings.DEFAULT.stats();
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, stats.health())
				.add(Attributes.ARMOR, stats.armor())
				.add(Attributes.MOVEMENT_SPEED, stats.speed())
				.add(Attributes.FOLLOW_RANGE, stats.followRange())
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.ATTACK_DAMAGE, PillagerKingSettings.DEFAULT.slash().damage())
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	protected PillagerKingSettings settings() {
		return PillagerKingModule.settings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new KingFightGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
		// The monsters that go for pillagers, he goes for too; a player in sight still comes first.
		targetSelector.addGoal(PillagerFeudModule.TARGET_PRIORITY, new PillagerFeudModule.FeudTargetGoal(this, PillagerFeudModule.ATTACKERS));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setLeftHanded(false);
		setCanPickUpLoot(false);
		applyStats(true);
		equipAxes();
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		// Gear is fixed, set in finalizeSpawn.
	}

	/** An axe in each hand that is empty. */
	private void equipAxes() {
		if (getMainHandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, axeStack());
		}
		if (getOffhandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.OFFHAND, axeStack());
		}
	}

	/** One of his two axes: a netherite axe with a long haft. */
	public static ItemStack axeStack() {
		ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
		axe.set(DataComponents.ITEM_MODEL, AXE_MODEL);
		return axe;
	}

	/** Health, armor, speed and the rest from the data file; {@code full} also heals him to the new maximum. */
	private void applyStats(boolean full) {
		PillagerKingSettings.Stats stats = settings().stats();
		setBase(Attributes.MAX_HEALTH, stats.health());
		setBase(Attributes.ARMOR, stats.armor());
		setBase(Attributes.MOVEMENT_SPEED, stats.speed());
		setBase(Attributes.FOLLOW_RANGE, stats.followRange());
		setHealth(full ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
		xpReward = stats.xp();
	}

	private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	// ---- State for the goal and the client ----

	public KingAction action() {
		return KingAction.byId(entityData.get(DATA_ACTION));
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
		return action != KingAction.NONE;
	}

	/** Ready to attack: no action for the attack pause, or at once after his axe broke a shield. */
	public boolean readyToAttack() {
		return action == KingAction.NONE && (followUp || idleTicks >= settings().attackPause());
	}

	int[] cooldowns() {
		return cooldowns;
	}

	int dashCooldown() {
		return dashCooldown;
	}

	int hopCooldown() {
		return hopCooldown;
	}

	int outOfReach() {
		return outOfReach;
	}

	boolean followUp() {
		return followUp;
	}

	// ---- A target above him ----

	/**
	 * A target that has stood out of his reach above him for a while: cuts its pillar of placed blocks down, throws the
	 * boomerang up at it or climbs the wall in front of him. True if he started one of them.
	 */
	boolean answerHeight(LivingEntity target) {
		PillagerKingSettings settings = settings();
		PillagerKingSettings.Height height = settings.height();
		if (!readyToAttack() || !PillagerKingLogic.outOfReachAbove(target.getY() - getY(), highTicks, height)) {
			return false;
		}
		PillagerKingLogic.HeightAnswer answer = PillagerKingLogic.answerHeight(BossBuilding.anyPlacedIn(level(), pillarFoot(target)),
				horizontalDistanceTo(target), chopCooldown <= 0, cooldowns[Attack.BOOMERANG.ordinal()] <= 0 && axeAway < 0,
				hasLineOfSight(target), distanceTo(target), climbCooldown <= 0 && onGround(), wallAhead(towards(target)), settings);
		if (answer == null) {
			return false;
		}
		getMoveControl().strafe(0.0F, 0.0F);
		switch (answer) {
			case CHOP -> {
				chopCooldown = height.chopCooldown();
				actionTarget = target;
				setAction(KingAction.CHOP);
			}
			case BOOMERANG -> startAttack(Attack.BOOMERANG, target);
			case CLIMB -> {
				climbCooldown = height.climbCooldown();
				climbFrom = getY();
				climbDirection = towards(target);
				actionTarget = target;
				setAction(KingAction.CLIMB);
			}
		}
		return true;
	}

	/** Where the foot of the target's pillar would be: under it and a block round, from his feet up to his chest. */
	private AABB pillarFoot(LivingEntity target) {
		AABB under = target.getBoundingBox();
		return new AABB(under.minX - 1.0, getY(), under.minZ - 1.0, under.maxX + 1.0, Math.min(getY() + 2.0, target.getY()), under.maxZ + 1.0);
	}

	/** Whether a block stands against him on the side {@code direction} points to, above a step. */
	private boolean wallAhead(Vec3 direction) {
		if (direction == Vec3.ZERO) {
			return false;
		}
		AABB box = getBoundingBox().move(direction.x * 0.3, 0.0, direction.z * 0.3);
		box = new AABB(box.minX, box.minY + 1.1, box.minZ, box.maxX, box.maxY - 0.2, box.maxZ);
		return !level().noBlockCollision(this, box);
	}

	private Vec3 towards(LivingEntity target) {
		return horizontal(target.position().subtract(position()));
	}

	/**
	 * Up the wall with his axes hooked in, pressed to it. Over its top he cuts at the target if it is in reach, else steps
	 * onto it; past {@code climb_max} he lets go.
	 */
	private void tickClimb(ServerLevel level, int t, LivingEntity target, PillagerKingSettings settings) {
		PillagerKingSettings.Height height = settings.height();
		Vec3 toTarget = towards(target);
		if (toTarget != Vec3.ZERO) {
			climbDirection = toTarget;
		}
		fallDistance = 0.0;
		if (getY() - climbFrom > height.climbMax()) {
			letGo();
			return;
		}
		if (wallAhead(climbDirection)) {
			setDeltaMovement(climbDirection.x * 0.05, height.climbSpeed(), climbDirection.z * 0.05);
			if (t % 6 == 0) {
				// An axe biting into the wall, right and left in turn.
				playSound(SoundEvents.AXE_STRIP.value(), 1.0F, t % 12 == 0 ? 0.6F : 0.7F);
				BlockPos wall = BlockPos.containing(position().add(climbDirection.scale(getBbWidth() * 0.5 + 0.3)).add(0.0, getBbHeight() * 0.8, 0.0));
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(wall)), wall.getX() + 0.5,
						wall.getY() + 0.5, wall.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
			}
			return;
		}
		if (t < 3) {
			// Nothing to climb after all.
			endAction();
			return;
		}
		// Over the top.
		setDeltaMovement(climbDirection.x * 0.25, 0.3, climbDirection.z * 0.25);
		if (horizontalDistanceTo(target) <= settings.reach() + 0.5 && Math.abs(target.getY() - getY()) < 2.5) {
			LivingEntity cut = target;
			setAction(KingAction.TOP_CUT);
			actionTarget = cut;
		} else {
			endAction();
		}
	}

	/** Comes off the wall he climbs. */
	private void letGo() {
		setDeltaMovement(-climbDirection.x * 0.2, 0.0, -climbDirection.z * 0.2);
		endAction();
	}

	/** The cut at the top of a climb: it throws the target off its ledge. */
	private void tickTopCut(ServerLevel level, int t, LivingEntity target, PillagerKingSettings settings) {
		if (t < KingAction.TOP_CUT_HIT) {
			setDeltaMovement(climbDirection.x * 0.15, getDeltaMovement().y, climbDirection.z * 0.15);
			return;
		}
		if (t != KingAction.TOP_CUT_HIT) {
			return;
		}
		PillagerKingSettings.Height height = settings.height();
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.7F);
		if (horizontalDistanceTo(target) <= settings.reach() + 0.8 && Math.abs(target.getY() - getY()) < 2.5 && hasLineOfSight(target)) {
			target.damageCooldownTime = 0;
			strike(level, target, damageSources().mobAttack(this), height.topCutDamage(), 0.0F);
			Vec3 away = towards(target);
			target.push(away.x * height.topCutPush(), 0.35, away.z * height.topCutPush());
			target.syncVelocity = true;
			target.needsSync = true;
		}
	}

	/** Both axes swung low into the foot of the target's pillar: the placed blocks there break, and what stood on them falls. */
	private void tickChop(ServerLevel level, int t, LivingEntity target) {
		if (t < KingAction.CHOP_HIT && horizontalDistanceTo(target) > 1.6) {
			Vec3 to = towards(target);
			setDeltaMovement(to.x * 0.15, getDeltaMovement().y, to.z * 0.15);
		}
		if (t != KingAction.CHOP_HIT) {
			return;
		}
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5F, 0.6F);
		if (BossBuilding.breakPlacedIn(level, pillarFoot(target), this, true) > 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, getSoundSource(), 1.0F, 0.7F);
		}
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		if (fellFromClimb) {
			fellFromClimb = false;
			return false;
		}
		return super.causeFallDamage(fallDistance, damageModifier, source);
	}

	boolean isDashing() {
		return dashTicks > 0;
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) cooldowns[i]--;
		}
		if (dashCooldown > 0) dashCooldown--;
		if (hopCooldown > 0) hopCooldown--;
		if (climbCooldown > 0) climbCooldown--;
		if (chopCooldown > 0) chopCooldown--;
		if (fellFromClimb && onGround() && action != KingAction.CLIMB) {
			// Down safe: the next fall hurts again.
			fellFromClimb = false;
		}
		if (leapFrom != null) {
			leapBack(leapFrom);
			leapFrom = null;
		}
		if (axeAway >= 0 && ++axeAway > AXE_BACK_AFTER) {
			catchAxe();
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && horizontalDistanceTo(target) > settings().reach() + REACH_SLACK) {
			outOfReach++;
		} else {
			outOfReach = 0;
		}
		highTicks = target != null && target.isAlive() && target.getY() - getY() > settings().height().above() ? highTicks + 1 : 0;
		updateRunning();
		if (target == null && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(settings().stats().regen());
		}
		if (action == KingAction.NONE) {
			idleTicks++;
			tickDash(null);
		} else {
			tickAction(level);
		}
		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	/** Runs (faster walking) after a target he could not reach for a while, until it is back in reach. */
	private void updateRunning() {
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		PillagerKingSettings.Run run = settings().run();
		if (PillagerKingLogic.runs(outOfReach, run) && action == KingAction.NONE) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(RUN_MODIFIER, PillagerKingLogic.speedModifier(run.bonus()),
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			setSprinting(true);
		} else if (speed.hasModifier(RUN_MODIFIER)) {
			speed.removeModifier(RUN_MODIFIER);
			setSprinting(false);
		}
	}

	public void startAttack(Attack attack, LivingEntity target) {
		actionTarget = target;
		hitChecked = false;
		followUp = false;
		outOfReach = 0;
		PillagerKingSettings settings = settings();
		switch (attack) {
			case SLASH -> setAction(KingAction.SLASH);
			case COMBO -> {
				cooldowns[attack.ordinal()] = settings.combo().cooldown();
				setAction(KingAction.COMBO);
			}
			case ONSLAUGHT -> {
				cooldowns[attack.ordinal()] = settings.onslaught().cooldown();
				setAction(KingAction.ONSLAUGHT);
			}
			case CHARGE -> {
				cooldowns[attack.ordinal()] = settings.charge().cooldown();
				setAction(KingAction.CHARGE_WINDUP);
			}
			case BOOMERANG -> {
				cooldowns[attack.ordinal()] = settings.boomerang().cooldown();
				// He has one axe to throw: no plain throw right after.
				cooldowns[Attack.THROW.ordinal()] = Math.max(cooldowns[Attack.THROW.ordinal()], 60);
				setAction(KingAction.BOOMERANG);
			}
			case TORNADO -> {
				cooldowns[attack.ordinal()] = settings.tornado().cooldown();
				setAction(KingAction.TORNADO);
			}
			case THROW -> {
				cooldowns[attack.ordinal()] = settings.axeThrow().cooldown();
				cooldowns[Attack.BOOMERANG.ordinal()] = Math.max(cooldowns[Attack.BOOMERANG.ordinal()], 60);
				setAction(KingAction.THROW);
			}
		}
	}

	private void setAction(KingAction next) {
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && speed.hasModifier(CHARGE_SLOW_MODIFIER)) {
			speed.removeModifier(CHARGE_SLOW_MODIFIER);
		}
		if (action == KingAction.CLIMB && next != KingAction.CLIMB) {
			// Off the wall, however it ended: no fall damage on landing.
			fellFromClimb = true;
		}
		action = next;
		actionTicks = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		setAction(KingAction.NONE);
		actionTarget = null;
		idleTicks = 0;
		dashTicks = 0;
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= action.length()) {
			if (action == KingAction.CHARGE_WINDUP) {
				setAction(KingAction.CHARGE);
			} else {
				endAction();
			}
			return;
		}
		boolean spinning = action == KingAction.TORNADO;
		boolean dazed = action == KingAction.STAGGER || action == KingAction.STUNNED;
		if (action != KingAction.TOTEM && !dazed && action != KingAction.PARRY_ARROW && !spinning
				&& (target == null || !target.isAlive())) {
			endAction();
			return;
		}
		getNavigation().stop();
		if (target != null && !spinning && !dazed) {
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			faceTowards(target);
		}
		PillagerKingSettings settings = settings();
		switch (action) {
			case SLASH -> {
				closeGap(t, target, settings);
				if (t == KingAction.SLASH_HIT) {
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
					hitIfInReach(level, target, settings.reach(), settings.slash().damage());
					hitDone();
				}
			}
			case COMBO -> tickCombo(level, t, target, settings);
			case ONSLAUGHT -> tickOnslaught(level, t, target, settings.onslaught(), settings.reach());
			case CHARGE_WINDUP -> {
				if (t == 0) {
					level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_DASH_WARNING, getSoundSource(), 2.0F, 0.8F);
				}
			}
			case CHARGE -> tickCharge(level, t, target, settings.charge());
			case CHARGE_HITS -> tickChargeHits(level, t, target, settings);
			case TORNADO -> tickTornado(level, t, target, settings);
			case STUNNED -> {
				setDeltaMovement(getDeltaMovement().multiply(0.5, 1.0, 0.5));
				DizzyStars.show(level, this);
			}
			case THROW -> {
				if (t == KingAction.THROW_RELEASE && target != null) {
					throwAxe(level, target, settings.axeThrow());
				}
			}
			case BOOMERANG -> {
				if (t == KingAction.THROW_RELEASE && target != null) {
					throwBoomerang(level, target, settings.boomerang());
				}
			}
			case CLIMB -> tickClimb(level, t, target, settings);
			case TOP_CUT -> tickTopCut(level, t, target, settings);
			case CHOP -> tickChop(level, t, target);
			default -> {
			}
		}
	}

	private void tickCombo(ServerLevel level, int t, @Nullable LivingEntity target, PillagerKingSettings settings) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			// A bell: the moment to parry him.
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 1.5F, 0.8F);
		}
		closeGap(t, target, settings);
		for (int hit : KingAction.COMBO_HITS) {
			if (t == hit) {
				playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.75F + t * 0.01F);
				// The cuts come faster than the usual half-second of hurt immunity; each one must land.
				target.damageCooldownTime = 0;
				hitIfInReach(level, target, settings.reach() + 0.3, settings.combo().damage());
				hitDone();
			}
		}
		if (t == COMBO_HOP) {
			hopBack(target, settings.hop());
		}
	}

	/**
	 * The onslaught: a rush up to the target, a leap at it with both axes raised and
	 * one blow of both that a shield only softens.
	 */
	private void tickOnslaught(ServerLevel level, int t, @Nullable LivingEntity target, PillagerKingSettings.Onslaught onslaught,
			double reach) {
		if (target == null || !OnslaughtMoves.tick(this, level, t, target, onslaught.rushSpeed(), reach)) {
			return;
		}
		boolean shielded = OnslaughtMoves.shielded(target, this);
		float damage = PillagerKingLogic.onslaughtDamage(onslaught.damage(), shielded, onslaught.shieldReduction());
		target.damageCooldownTime = 0;
		strike(level, target, damageSources().source(PillagerKingModule.ONSLAUGHT_DAMAGE, this), damage, 0.8F);
		if (shielded) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.6F);
		}
	}

	/** A kick in front of him: a little damage and a hard push back. */
	private void kick(ServerLevel level, LivingEntity target, float damage, double push) {
		if (horizontalDistanceTo(target) > KICK_REACH + REACH_SLACK || Math.abs(target.getY() - getY()) > 2.0 || !inFront(target)) {
			return;
		}
		playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.6F);
		target.damageCooldownTime = 0;
		strike(level, target, damageSources().mobAttack(this), damage, 0.0F);
		Vec3 away = horizontal(target.position().subtract(position()));
		target.push(away.x * push, 0.35, away.z * push);
	}

	/** Runs at the target; on reaching it, goes into the cuts. Running into a wall or a pillar on the way stuns him. */
	private void tickCharge(ServerLevel level, int t, @Nullable LivingEntity target, PillagerKingSettings.Charge charge) {
		if (target == null) {
			return;
		}
		if (t > 0 && hitWall()) {
			if (!smashPlacedWall(level)) {
				stun(level);
				return;
			}
		}
		if (horizontalDistanceTo(target) > CHARGE_CONTACT) {
			Vec3 to = horizontal(target.position().subtract(position()));
			setDeltaMovement(to.x * charge.speed(), getDeltaMovement().y, to.z * charge.speed());
			if (tickCount % 3 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 2, 0.3, 0.0, 0.3, 0.01);
			}
			return;
		}
		setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
		setAction(KingAction.CHARGE_HITS);
	}

	/**
	 * Whether the last step of his charge ran into something taller than a step: a block against him above his knees, on
	 * the side he runs to.
	 */
	private boolean hitWall() {
		if (!horizontalCollision) {
			return false;
		}
		return !level().noBlockCollision(this, wallProbe());
	}

	/** The space just ahead of his run, from above his knees to below his crown. */
	private AABB wallProbe() {
		Vec3 ahead = horizontal(getDeltaMovement());
		if (ahead.lengthSqr() < 1.0E-4) {
			ahead = horizontal(Vec3.directionFromRotation(0.0F, getYRot()));
		}
		AABB box = getBoundingBox().move(ahead.x * WALL_PROBE, 0.0, ahead.z * WALL_PROBE);
		return new AABB(box.minX, box.minY + 1.0, box.minZ, box.maxX, box.maxY - 0.2, box.maxZ);
	}

	/**
	 * The wall his charge ran into was put up by players around him: he smashes through it and runs on, slowed. False
	 * for any other wall.
	 */
	private boolean smashPlacedWall(ServerLevel level) {
		AABB ahead = wallProbe();
		if (!BossBuilding.mayBreak(level) || !BossBuilding.onlyPlacedIn(level, ahead)) {
			return false;
		}
		BossBuilding.breakPlacedIn(level, ahead, this, false);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, getSoundSource(), 1.2F, 0.7F);
		setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
		return true;
	}

	/** He slammed into a wall: a thud, sparks off his helm, and he stands dazed for a moment. */
	private void stun(ServerLevel level) {
		LivingEntity target = actionTarget;
		setAction(KingAction.STUNNED);
		actionTarget = target;
		setDeltaMovement(getDeltaMovement().multiply(-0.3, 1.0, -0.3));
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ANVIL_LAND, getSoundSource(), 1.2F, 0.6F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.5F, 0.5F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 16, getBbWidth() * 0.5, 0.3, getBbWidth() * 0.5, 0.3);
	}

	/** Three quick cuts after the charge, then a kick; slower after the first cut. */
	private void tickChargeHits(ServerLevel level, int t, @Nullable LivingEntity target, PillagerKingSettings settings) {
		if (target == null) {
			return;
		}
		PillagerKingSettings.Charge charge = settings.charge();
		boolean slowed = t > KingAction.CHARGE_CUTS[0];
		if (t == KingAction.CHARGE_CUTS[0] + 1) {
			AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null) {
				speed.addOrUpdateTransientModifier(new AttributeModifier(CHARGE_SLOW_MODIFIER, -charge.slowAfterHit(),
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
		// He keeps on the target as it backs off.
		if (t < KingAction.CHARGE_KICK && horizontalDistanceTo(target) > 1.8) {
			Vec3 to = horizontal(target.position().subtract(position()));
			double follow = 0.35 * (slowed ? 1.0 - charge.slowAfterHit() : 1.0);
			setDeltaMovement(to.x * follow, getDeltaMovement().y, to.z * follow);
		}
		for (int cut : KingAction.CHARGE_CUTS) {
			if (t == cut) {
				playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + t * 0.02F);
				target.damageCooldownTime = 0;
				// The first cut knocks a raised shield out.
				hitIfInReach(level, target, settings.reach(), charge.damage(), cut == KingAction.CHARGE_CUTS[0] ? charge.shieldSeconds() : 0.0F);
			}
		}
		if (t == KingAction.CHARGE_KICK) {
			kick(level, target, charge.kickDamage(), charge.kickPush());
		}
	}

	private void tickTornado(ServerLevel level, int t, @Nullable LivingEntity target, PillagerKingSettings settings) {
		PillagerKingSettings.Tornado tornado = settings.tornado();
		if (t == 0) {
			playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 0.6F);
		}
		if (t >= KingAction.TORNADO_SPIN_UP && target != null && target.isAlive() && horizontalDistanceTo(target) > 1.2) {
			Vec3 to = horizontal(target.position().subtract(position()));
			setDeltaMovement(to.x * tornado.drift(), getDeltaMovement().y, to.z * tornado.drift());
		}
		for (int hit : KingAction.TORNADO_HITS) {
			if (t == hit) {
				playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F + t * 0.01F);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(0.5), getZ(), 3, tornado.radius() * 0.4, 0.2,
						tornado.radius() * 0.4, 0.0);
				// The second cut knocks a raised shield out.
				hitAround(level, tornado.radius(), tornado.damage(), damageSources().mobAttack(this), 0.2F,
						hit == KingAction.TORNADO_HITS[1] ? tornado.shieldSeconds() : 0.0F);
			}
		}
	}

	// ---- Axe throw ----

	private void throwAxe(ServerLevel level, LivingEntity target, PillagerKingSettings.AxeThrow axe) {
		ThrownKingAxe thrown = new ThrownKingAxe(level, this, axe.damage(), axe.shieldSeconds());
		// From his left hand, at the target's chest.
		Vec3 left = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F).scale(0.6);
		thrown.setPos(getX() + left.x, getY(0.75), getZ() + left.z);
		double dx = target.getX() - thrown.getX();
		double dy = target.getY(0.5) - thrown.getY();
		double dz = target.getZ() - thrown.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		thrown.shoot(dx, dy + flat * 0.05, dz, (float) axe.speed(), 0.5F);
		level.addFreshEntity(thrown);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.6F);
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		axeAway = 0;
	}

	/** The left axe thrown round the target: it curves past it and strikes it in the back. */
	private void throwBoomerang(ServerLevel level, LivingEntity target, PillagerKingSettings.Boomerang boomerang) {
		Vec3 left = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F).scale(0.6);
		ThrownKingAxe thrown = ThrownKingAxe.boomerang(level, this, target, boomerang.damage(), boomerang.speed(), random.nextBoolean());
		thrown.setPos(getX() + left.x, getY(0.75), getZ() + left.z);
		level.addFreshEntity(thrown);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.5F);
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		axeAway = 0;
	}

	/** The thrown axe is back in his left hand. */
	@Override
	public void catchAxe() {
		axeAway = -1;
		if (getOffhandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.OFFHAND, axeStack());
		}
	}

	/** His axe knocked out the target's shield: he dashes in and attacks at once. */
	@Override
	public void axeBrokeShield(LivingEntity target) {
		if (target == getTarget()) {
			followUp = true;
			dashCooldown = 0;
		}
	}

	/** A player takes his thrown axe as it is, anything else many times over. */
	@Override
	public float axeDamageTo(float damage, Entity target) {
		return PillagerKingLogic.damageTo(damage, target instanceof Player, settings().mobDamageMultiplier());
	}

	// ---- Dash and hop ----

	/** Dashes at a target a few blocks off; the attack follows once he is in reach. */
	void dashAt(LivingEntity target) {
		PillagerKingSettings.Dash dash = settings().dash();
		dashTarget = target;
		dashTicks = dash.ticks();
		dashCooldown = dash.cooldown();
		playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 0.6F);
	}

	/**
	 * Moves him along a dash; {@code reach} is how close he wants to get, or null for the plain reach. Returns whether
	 * he is dashing.
	 */
	private boolean tickDash(@Nullable Double reach) {
		if (dashTicks <= 0) {
			return false;
		}
		LivingEntity target = dashTarget != null ? dashTarget : actionTarget;
		dashTicks--;
		if (target == null || !target.isAlive()) {
			dashTicks = 0;
			return false;
		}
		double stop = Math.max(1.2, (reach != null ? reach : settings().reach()) - DASH_STOP_SHORT);
		if (horizontalDistanceTo(target) > stop) {
			Vec3 to = horizontal(target.position().subtract(position()));
			double speed = settings().dash().speed();
			setDeltaMovement(to.x * speed, getDeltaMovement().y, to.z * speed);
			faceTowards(target);
			return true;
		}
		dashTicks = 0;
		return false;
	}

	/** Hops back from a target that crowds him. */
	void hopBack(LivingEntity target, PillagerKingSettings.Hop hop) {
		if (!onGround() || hopCooldown > 0) {
			return;
		}
		hopCooldown = hop.cooldown();
		Vec3 away = horizontal(position().subtract(target.position()));
		setDeltaMovement(away.x * hop.speed(), 0.35, away.z * hop.speed());
		playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.5F);
	}

	/**
	 * A few ticks before each hit he checks where the target will be. Out of reach, he dashes at it if the dash is ready;
	 * otherwise the hit goes into the air.
	 */
	private void closeGap(int t, @Nullable LivingEntity target, PillagerKingSettings settings) {
		if (target == null) {
			return;
		}
		dashTarget = target;
		double reach = hitReach(settings);
		if (tickDash(reach)) {
			return;
		}
		PillagerKingSettings.Dash dash = settings.dash();
		int toHit = ticksToHit(t);
		if (hitChecked || toHit < 0 || toHit > dash.ticks()) {
			return;
		}
		hitChecked = true;
		// Where the target will be on the hit tick if it keeps moving the way it moved this tick.
		Vec3 motion = target.position().subtract(target.xo, target.yo, target.zo);
		double step = Math.min(motion.horizontalDistance(), 0.6);
		Vec3 flat = horizontal(motion);
		double dx = target.getX() + flat.x * step * toHit - getX();
		double dz = target.getZ() + flat.z * step * toHit - getZ();
		if (PillagerKingLogic.reach(Math.sqrt(dx * dx + dz * dz), reach, dashCooldown, dash) == PillagerKingLogic.Reach.DASH) {
			dashAt(target);
		}
	}

	/** Called on each hit tick: the next hit is checked afresh. */
	private void hitDone() {
		hitChecked = false;
		dashTicks = 0;
	}

	/** Ticks from tick {@code t} of the current action to its next hit, or -1 when no hit is left. */
	private int ticksToHit(int t) {
		return switch (action) {
			case SLASH -> t <= KingAction.SLASH_HIT ? KingAction.SLASH_HIT - t : -1;
			case COMBO -> {
				for (int hit : KingAction.COMBO_HITS) {
					if (hit >= t) yield hit - t;
				}
				yield -1;
			}
			default -> -1;
		};
	}

	private double hitReach(PillagerKingSettings settings) {
		return action == KingAction.COMBO ? settings.reach() + 0.3 : settings.reach();
	}

	// ---- Hits ----

	private void hitIfInReach(ServerLevel level, LivingEntity target, double reach, float damage) {
		hitIfInReach(level, target, reach, damage, 0.0F);
	}

	/** As {@link #hitIfInReach(ServerLevel, LivingEntity, double, float)}; a shield that blocks it is knocked out for {@code shieldSeconds}. */
	private void hitIfInReach(ServerLevel level, LivingEntity target, double reach, float damage, float shieldSeconds) {
		if (horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.5 && inFront(target)
				&& hasLineOfSight(target)) {
			ItemStack shield = blockingShield(target);
			strike(level, target, damageSources().mobAttack(this), damage, 0.0F);
			knockShieldOut(level, target, shield, shieldSeconds);
		}
	}

	/** The shield the target holds up against him, or null. */
	private @Nullable ItemStack blockingShield(LivingEntity target) {
		ItemStack shield = target.getItemBlockingWith();
		return shield != null && shield.has(DataComponents.BLOCKS_ATTACKS)
				&& FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ()) ? shield : null;
	}

	/** Knocks a shield that blocked his blow out of the target's hands for a while. */
	private void knockShieldOut(ServerLevel level, LivingEntity target, @Nullable ItemStack shield, float seconds) {
		BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
		if (blocks == null || seconds <= 0.0F) {
			return;
		}
		blocks.disable(level, target, seconds, shield);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.8F);
	}

	/** Cuts everything alive around him: players, golems, monsters, animals, villagers. A shield that blocks it is knocked out for {@code shieldSeconds}. */
	private void hitAround(ServerLevel level, double radius, float damage, DamageSource source, float knockback, float shieldSeconds) {
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 1.0, radius),
				e -> e != this && e.isAlive() && !(e instanceof ArmorStand) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e))) {
			if (horizontalDistanceTo(victim) <= radius + victim.getBbWidth() * 0.5 && Math.abs(victim.getY() - getY()) < 2.5
					&& hasLineOfSight(victim)) {
				victim.damageCooldownTime = 0;
				ItemStack shield = blockingShield(victim);
				strike(level, victim, source, damage, knockback);
				knockShieldOut(level, victim, shield, shieldSeconds);
			}
		}
	}

	private boolean strike(ServerLevel level, LivingEntity target, DamageSource source, float damage, float knockback) {
		if (!target.hurtServer(level, source,
				PillagerKingLogic.damageTo(damage, target instanceof Player, settings().mobDamageMultiplier()))) {
			return false;
		}
		if (knockback > 0.0F) {
			target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ(), source, damage);
		}
		setLastHurtMob(target);
		return true;
	}

	/** Only his thrown axe knocks a shield out; the cuts of his axes don't. */
	@Override
	public float getSecondsToDisableBlocking() {
		return 0.0F;
	}

	// ---- Arrows ----

	/** Arrows from the front are knocked aside by an axe while he is not in the middle of an attack. */
	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		if (!(projectile instanceof AbstractArrow) || projectile instanceof ThrownKingAxe || projectile.getOwner() == this
				|| level().isClientSide()) {
			return super.deflection(projectile);
		}
		Vec3 motion = projectile.getDeltaMovement();
		if (!FallenLordLogic.shieldCovers(yBodyRot, -motion.x, -motion.z) || getMainHandItem().isEmpty()
				|| action != KingAction.NONE && action != KingAction.PARRY_ARROW
				|| random.nextDouble() >= settings().arrowParry().chance()) {
			return super.deflection(projectile);
		}
		return this::parryArrow;
	}

	/** Runs on the server only. The arrow flies off to one side, slowed, and he flicks an axe. */
	private void parryArrow(Projectile projectile, @Nullable Entity by, RandomSource random, Vec3 power) {
		Vec3 motion = projectile.getDeltaMovement();
		Vec3 side = new Vec3(-motion.z, 0.0, motion.x).normalize().scale(random.nextBoolean() ? 1.0 : -1.0);
		projectile.setDeltaMovement(side.scale(motion.length() * 0.3).add(motion.scale(-0.1)).add(0.0, 0.2, 0.0));
		projectile.needsSync = true;
		level().playSound(null, getX(), getY(0.6), getZ(), SoundEvents.ANVIL_LAND, getSoundSource(), 0.35F, 1.8F + random.nextFloat() * 0.2F);
		LivingEntity target = getTarget();
		setAction(KingAction.PARRY_ARROW);
		actionTarget = target;
	}

	// ---- Damage: parry and totem ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (action == KingAction.TOTEM && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		ParryPenalty penalty = BossParry.penalty();
		boolean penalized = source.getEntity() instanceof Player hitter
				&& parryStreak.penalized(hitter.getUUID(), level.getGameTime(), penalty);
		boolean parry = source.getEntity() instanceof Player player && source.getDirectEntity() == player
				&& PillagerKingLogic.parries(action, actionTicks, penalty.window(KingAction.COMBO_PARRY_END, penalized));
		if (parry) {
			damage *= penalty.multiplier(settings().combo().parryMultiplier(), penalized);
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (parry && hurt && isAlive() && action != KingAction.TOTEM) {
			parried(level, (Player) source.getEntity());
		} else if (hurt && isAlive() && action == KingAction.CLIMB && settings().height().stunHit()) {
			// Hit on the wall: he comes off it and lands dazed.
			LivingEntity target = actionTarget;
			Vec3 away = climbDirection.scale(-0.2);
			stun(level);
			actionTarget = target;
			setDeltaMovement(away.x, getDeltaMovement().y, away.z);
		}
		return hurt;
	}

	/** A player hit him as the combo began or as he leapt in the onslaught: it breaks off and he staggers back, open to hits. */
	private void parried(ServerLevel level, Player player) {
		parryStreak.parried(player.getUUID(), level.getGameTime());
		LivingEntity target = actionTarget;
		setAction(KingAction.STAGGER);
		actionTarget = target;
		dashTicks = 0;
		Vec3 away = horizontal(position().subtract(player.position()));
		setDeltaMovement(away.x * 0.3, 0.1, away.z * 0.3);
		level.playSound(null, getX(), getY(0.6), getZ(), EamSounds.FALLEN_LORD_PARRY, getSoundSource(), 2.0F, 0.9F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 14, getBbWidth() * 0.4, getBbHeight() * 0.2, getBbWidth() * 0.4, 0.2);
	}

	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float damage) {
		float before = getHealth();
		super.actuallyHurt(level, source, damage);
		if (PillagerKingLogic.totemSaves(getHealth(), totemUsed, source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) {
			useTotem(level);
		} else if (isAlive()) {
			leapIfHurtFast(level, source, before - getHealth());
		}
	}

	/** Losing health fast, he breaks off whatever he is doing and leaps back away from who hurts him. */
	private void leapIfHurtFast(ServerLevel level, DamageSource source, float lost) {
		PillagerKingSettings.Hop hop = settings().hop();
		long now = level.getGameTime();
		recentHits.addLast(new Hit(now, Math.max(0.0F, lost)));
		while (!recentHits.isEmpty() && now - recentHits.peekFirst().time() > hop.burstWindow()) {
			recentHits.removeFirst();
		}
		float taken = 0.0F;
		for (Hit hit : recentHits) {
			taken += hit.amount();
		}
		if (!PillagerKingLogic.burstLeap(taken, getMaxHealth(), hop.burstShare()) || !onGround() || action == KingAction.TOTEM
				|| action == KingAction.STUNNED) {
			return;
		}
		recentHits.clear();
		Entity from = source.getEntity() != null ? source.getEntity() : getTarget();
		if (from == null) {
			return;
		}
		if (action != KingAction.NONE) {
			endAction();
		}
		hopCooldown = hop.cooldown();
		leapFrom = from.position();
	}

	private void leapBack(Vec3 from) {
		double speed = settings().hop().burstSpeed();
		Vec3 away = horizontal(position().subtract(from));
		setDeltaMovement(away.x * speed, 0.45, away.z * speed);
		playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 0.5F);
	}

	private record Hit(long time, float amount) {
	}

	/** His totem of undying breaks instead of him: he is healed and stands up again. */
	private void useTotem(ServerLevel level) {
		totemUsed = true;
		setHealth(settings().totem().heal());
		removeAllEffects();
		level.broadcastEntityEvent(this, (byte) 35);
		LivingEntity target = actionTarget != null ? actionTarget : getTarget();
		setAction(KingAction.TOTEM);
		actionTarget = target;
		dashTicks = 0;
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
	public void setCustomName(net.minecraft.network.chat.@Nullable Component name) {
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
		output.putBoolean("TotemUsed", totemUsed);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		totemUsed = input.getBooleanOr("TotemUsed", false);
		applyStats(false);
		bossEvent.setName(getDisplayName());
		// Saved mid-throw (the action itself is not saved), or spawned before he got both axes: give them back.
		equipAxes();
		Arrays.fill(cooldowns, 0);
	}

	// ---- Sounds ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PILLAGER_DEATH;
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
