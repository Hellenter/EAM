package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.boss.ParryPenalty;
import io.github.hellenter.eam.mobs.entity.boss.ParryStreak;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic.Attack;

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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * The Pillager King: a giant two and a half blocks tall in closed black plate trimmed with diamond, an axe in each hand.
 * He cuts once, in a four-cut combo that a hit at its start parries, whirls round once with both axes through any
 * shield after his eyes flash red, spins in a long tornado of weak blockable cuts, and throws his left axe to knock a
 * raised shield out before he goes in. He dashes at players a few blocks off and hops back from those who crowd him,
 * and runs after one he could not reach for a while. A totem of undying saves him once. Numbers are in
 * {@code data/eam/mobs/pillager_king.json}.
 */
public class PillagerKing extends Monster {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(PillagerKing.class, EntityDataSerializers.BYTE);
	private static final Identifier RUN_MODIFIER = Eam.id("pillager_king_run");
	/** Hits further than this flat distance past their reach still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** A dash stops this much short of the hit's reach, so the cut lands even if the target keeps backing off. */
	private static final double DASH_STOP_SHORT = 1.0;
	/** The thrown axe is back in his hand after this many ticks at the latest, even if it got lost. */
	private static final int AXE_BACK_AFTER = 80;
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
	/** Ticks his target has stayed out of his reach. */
	private int outOfReach;
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
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setLeftHanded(false);
		setCanPickUpLoot(false);
		applyStats(true);
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		setItemSlot(EquipmentSlot.MAINHAND, axeStack());
		setItemSlot(EquipmentSlot.OFFHAND, axeStack());
	}

	/** One of his two axes. */
	public static ItemStack axeStack() {
		return new ItemStack(Items.IRON_AXE);
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
		if (axeAway >= 0 && ++axeAway > AXE_BACK_AFTER) {
			catchAxe();
		}
		LivingEntity target = getTarget();
		if (target != null && target.isAlive() && horizontalDistanceTo(target) > settings().reach() + REACH_SLACK) {
			outOfReach++;
		} else {
			outOfReach = 0;
		}
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
			case WHIRL -> {
				cooldowns[attack.ordinal()] = settings.whirl().cooldown();
				setAction(KingAction.WHIRL_WINDUP);
			}
			case TORNADO -> {
				cooldowns[attack.ordinal()] = settings.tornado().cooldown();
				setAction(KingAction.TORNADO);
			}
			case THROW -> {
				cooldowns[attack.ordinal()] = settings.axeThrow().cooldown();
				setAction(KingAction.THROW);
			}
		}
	}

	private void setAction(KingAction next) {
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
			if (action == KingAction.WHIRL_WINDUP) {
				setAction(KingAction.WHIRL);
			} else {
				endAction();
			}
			return;
		}
		boolean spinning = action == KingAction.WHIRL || action == KingAction.TORNADO;
		if (action != KingAction.TOTEM && action != KingAction.STAGGER && !spinning && (target == null || !target.isAlive())) {
			endAction();
			return;
		}
		getNavigation().stop();
		if (target != null && !spinning && action != KingAction.STAGGER) {
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
			case WHIRL_WINDUP -> {
				if (t == 0) {
					level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_DASH_WARNING, getSoundSource(), 2.0F, 0.8F);
				}
			}
			case WHIRL -> {
				for (int hit : KingAction.WHIRL_HITS) {
					if (t == hit) {
						playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F + t * 0.03F);
						PillagerKingSettings.Whirl whirl = settings.whirl();
						hitAround(level, whirl.radius(), whirl.damage(), damageSources().source(PillagerKingModule.WHIRL_DAMAGE, this), 0.6F);
					}
				}
			}
			case TORNADO -> tickTornado(level, t, target, settings);
			case THROW -> {
				if (t == KingAction.THROW_RELEASE && target != null) {
					throwAxe(level, target, settings.axeThrow());
				}
			}
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
				hitAround(level, tornado.radius(), tornado.damage(), damageSources().mobAttack(this), 0.2F);
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

	/** The thrown axe is back in his left hand. */
	void catchAxe() {
		axeAway = -1;
		if (getOffhandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.OFFHAND, axeStack());
		}
	}

	/** His axe knocked out the target's shield: he dashes in and attacks at once. */
	void axeBrokeShield(LivingEntity target) {
		if (target == getTarget()) {
			followUp = true;
			dashCooldown = 0;
		}
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
		if (horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.5 && inFront(target)
				&& hasLineOfSight(target)) {
			strike(level, target, damageSources().mobAttack(this), damage, 0.0F);
		}
	}

	/** Cuts everyone around him that he fights: players, golems and his target, whichever side they are on. */
	private void hitAround(ServerLevel level, double radius, float damage, DamageSource source, float knockback) {
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 1.0, radius),
				e -> e != this && e.isAlive() && (e == actionTarget || e instanceof IronGolem
						|| e instanceof Player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e)))) {
			if (horizontalDistanceTo(victim) <= radius + victim.getBbWidth() * 0.5 && Math.abs(victim.getY() - getY()) < 2.5
					&& hasLineOfSight(victim)) {
				victim.damageCooldownTime = 0;
				strike(level, victim, source, damage, knockback);
			}
		}
	}

	private boolean strike(ServerLevel level, LivingEntity target, DamageSource source, float damage, float knockback) {
		if (!target.hurtServer(level, source, damage)) {
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
		if (parry && isAlive() && action != KingAction.TOTEM) {
			parried(level, (Player) source.getEntity());
		}
		return hurt;
	}

	/** A player hit him as the combo began: it breaks off and he staggers back, open to hits. */
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
		super.actuallyHurt(level, source, damage);
		if (PillagerKingLogic.totemSaves(getHealth(), totemUsed, source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))) {
			useTotem(level);
		}
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
		// Saved mid-throw (the action itself is not saved): give the axe back.
		if (getOffhandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.OFFHAND, axeStack());
		}
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
}
