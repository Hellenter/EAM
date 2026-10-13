package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.fallenlord.ThrownLordSpear;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnight;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;
import io.github.hellenter.eam.mobs.entity.lost.CommanderTargetGoal;
import io.github.hellenter.eam.mobs.entity.lost.Lost;
import io.github.hellenter.eam.mobs.entity.lost.LostCommander;
import io.github.hellenter.eam.mobs.entity.lost.LostGear;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * A heavy Lost cavalryman: one of the Lost, a dried-up corpse half husk and half skeleton, in full plate of ancient
 * metal holed through, a closed helm, a lance and a shield, on an armored undead horse. They come in pairs. Mounted, he
 * rides round the player and charges now and then with the lance couched: the strike stuns a shield, and his partner
 * follows a moment later to strike past it. He throws his lance at a player he cannot ride down, and takes another from
 * the saddle. With his horse killed he fights on with axe and shield, the skeleton knight's way with moves of his own;
 * his partner, still mounted, covers him and charges more often.
 */
public class LostCavalryman extends SkeletonKnight {
	/** His partner further away than this is not counted. */
	private static final double PAIR_RANGE = 48.0;
	private static final int PAIR_REFRESH_TICKS = 20;
	/** After a throw he takes the next lance from the saddle this many ticks later. */
	private static final int REARM_TICKS = 24;
	/** Hits further than the reach by this much still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** After a lance strike he gallops on past the target for at most this many ticks before turning. */
	private static final int RIDE_THROUGH_TICKS = 14;

	/** The pair's first rider; {@code null} for that rider himself. */
	private @Nullable UUID pairLeader;
	private @Nullable LostCavalryman partner;
	private int pairRefresh;
	/** His horse is dead: he fights on foot with his axe. */
	private boolean onFoot;
	/** Ticks he has been off his horse without being unhorsed yet; a moment's gap while loading is not a fall. */
	private int offHorseTicks;
	private int chargeCooldown;
	private int throwCooldown;
	private int sparesLeft = LostCavalrySettings.Throwing.DEFAULT.spares();
	private int rearmTicks;
	private int doubleCooldown;
	private int sweepCooldown;
	private int sprintTicks;
	private int sprintCooldown;
	/** Ticks until he charges after his partner, or -1. */
	private int followUpTicks = -1;
	private boolean lanceStruck;
	private int struckAt;
	private @Nullable Vec3 rideThrough;

	public LostCavalryman(EntityType<? extends LostCavalryman> type, Level level) {
		super(type, level);
		xpReward = 20;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, LostCavalrySettings.Body.DEFAULT.riderHealth())
				.add(Attributes.MOVEMENT_SPEED, LostCavalryLogic.speedOfSkeletonWalk(LostCavalrySettings.Speed.DEFAULT.foot()))
				.add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected SkeletonKnightSettings settings() {
		return LostCavalryModule.settings().axe();
	}

	static LostCavalrySettings cavalry() {
		return LostCavalryModule.settings();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new CavalryMountedGoal(this));
		goalSelector.addGoal(2, new CavalryFootGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new CommanderTargetGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, Lost.class, LostCavalryman.class, UndeadWarlock.class).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	/** He never turns on a player who carries the commander's horn. */
	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && !LostCommander.commands(target);
	}

	public boolean isOnFoot() {
		return onFoot;
	}

	/** His horse, while he is in the saddle. */
	@Nullable LostWarhorse horse() {
		return !onFoot && getVehicle() instanceof LostWarhorse horse && horse.isAlive() ? horse : null;
	}

	// ---- Spawning ----

	/**
	 * Spawned on his own (by a command or an egg) he comes on his horse and brings his partner, on his own horse; the
	 * partner gets the pair's id from the {@link Pair} data.
	 */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		LostCavalrySettings settings = cavalry();
		pairLeader = groupData instanceof Pair pair ? pair.leader() : null;
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(settings.body().riderHealth());
			setHealth(getMaxHealth());
		}
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(LostCavalryLogic.speedOfSkeletonWalk(settings.speed().foot()));
		}
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData instanceof Pair ? null : groupData);
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, settings.body().gearDropChance());
		}
		sparesLeft = settings.throwing().spares();
		if (!isPassenger()) {
			mountNewHorse(level, difficulty);
		}
		if (pairLeader == null) {
			spawnPartner(level, difficulty);
		}
		return data;
	}

	/** Full holed plate (real ancient metal armor, drawn by his own gear layer), a lance and a battered shield. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		setItemSlot(EquipmentSlot.HEAD, LostGear.worn(EamItems.ANCIENT_METAL_HELMET, 0));
		setItemSlot(EquipmentSlot.CHEST, LostGear.worn(EamItems.ANCIENT_METAL_CHESTPLATE, 0));
		setItemSlot(EquipmentSlot.LEGS, LostGear.worn(EamItems.ANCIENT_METAL_LEGGINGS, 0));
		setItemSlot(EquipmentSlot.FEET, LostGear.worn(EamItems.ANCIENT_METAL_BOOTS, 0));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		ItemStack shield = new ItemStack(Items.SHIELD);
		shield.set(DataComponents.ITEM_MODEL, LostGear.SHIELD_MODEL);
		setItemSlot(EquipmentSlot.OFFHAND, shield);
	}

	private void mountNewHorse(ServerLevelAccessor level, DifficultyInstance difficulty) {
		LostWarhorse horse = EamEntities.LOST_WARHORSE.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (horse == null) {
			takeAxe();
			return;
		}
		horse.snapTo(getX(), getY(), getZ(), getYRot(), 0.0F);
		horse.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, null);
		if (isPersistenceRequired()) {
			horse.setPersistenceRequired();
		}
		level.addFreshEntity(horse);
		startRiding(horse, true, false);
	}

	private void spawnPartner(ServerLevelAccessor level, DifficultyInstance difficulty) {
		LostCavalryman partner = EamEntities.LOST_CAVALRYMAN.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (partner == null) {
			return;
		}
		Vec3 spot = partnerSpot(level);
		partner.snapTo(spot.x, spot.y, spot.z, getYRot(), 0.0F);
		if (isPersistenceRequired()) {
			partner.setPersistenceRequired();
		}
		partner.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, new Pair(getUUID()));
		level.addFreshEntityWithPassengers(partner);
	}

	/** A free spot for a horse on solid ground three or four blocks to his side, or his own. */
	private Vec3 partnerSpot(ServerLevelAccessor level) {
		Vec3 side = Vec3.directionFromRotation(0.0F, getYRot() + 90.0F);
		EntityType<?> horse = EamEntities.LOST_WARHORSE;
		for (double step : new double[] {3.5, -3.5, 5.0, -5.0, 2.5, -2.5}) {
			BlockPos column = BlockPos.containing(getX() + side.x * step, getY(), getZ() + side.z * step);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.above(dy);
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
						&& level.noCollision(horse.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
					return Vec3.atBottomCenterOf(pos);
				}
			}
		}
		return position();
	}

	/** Spawn data for the second rider of a pair: the first one's id. */
	record Pair(UUID leader) implements SpawnGroupData {
	}

	// ---- Pair ----

	/** The id both riders of a pair share: the first one's. */
	UUID pairId() {
		return pairLeader != null ? pairLeader : getUUID();
	}

	@Nullable LostCavalryman partner() {
		return partner != null && partner.isAlive() ? partner : null;
	}

	/** He is mounted and his partner fights on foot: he covers him, charging more often and riding closer. */
	boolean covering() {
		LostCavalryman mate = partner();
		return !onFoot && mate != null && mate.onFoot;
	}

	private void refreshPartner(ServerLevel level) {
		UUID id = pairId();
		partner = null;
		for (LostCavalryman mate : level.getEntitiesOfClass(LostCavalryman.class, getBoundingBox().inflate(PAIR_RANGE),
				mate -> mate != this && mate.isAlive() && id.equals(mate.pairId()))) {
			partner = mate;
			break;
		}
		LostCavalryman mate = partner();
		if (mate == null) {
			return;
		}
		// Whoever goes for his unhorsed partner is his target first.
		if (covering() && mate.getLastHurtByMob() instanceof Player attacker && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(attacker)
				&& attacker.isAlive() && distanceToSqr(attacker) < PAIR_RANGE * PAIR_RANGE) {
			setTarget(attacker);
			return;
		}
		LivingEntity own = getTarget();
		LivingEntity theirs = mate.getTarget();
		if ((own == null || !own.isAlive()) && theirs != null && theirs.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(theirs)
				&& distanceToSqr(theirs) < PAIR_RANGE * PAIR_RANGE) {
			setTarget(theirs);
		}
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (chargeCooldown > 0) chargeCooldown--;
		if (throwCooldown > 0) throwCooldown--;
		if (doubleCooldown > 0) doubleCooldown--;
		if (sweepCooldown > 0) sweepCooldown--;
		if (sprintCooldown > 0) sprintCooldown--;
		if (followUpTicks > 0) followUpTicks--;
		if (sprintTicks > 0 && --sprintTicks == 0) {
			sprintCooldown = cavalry().speed().sprintCooldown();
		}
		setSprinting(sprintTicks > 0);
		if (--pairRefresh <= 0) {
			pairRefresh = PAIR_REFRESH_TICKS;
			refreshPartner(level);
		}
		if (!onFoot) {
			offHorseTicks = isPassenger() ? 0 : offHorseTicks + 1;
			if (offHorseTicks > 2) {
				unhorsed();
			} else if (rearmTicks > 0 && --rearmTicks == 0) {
				setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
				playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.8F, 0.8F);
			}
		}
	}

	/** Thrown from his dead horse: he gets up with his axe, and his partner rides in to cover him. */
	private void unhorsed() {
		LivingEntity target = getTarget();
		endAction();
		takeAxe();
		playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.2F, 0.7F);
		if (target != null && target.isAlive()) {
			startAction(KnightAction.DISMOUNT, target);
		}
		LostCavalryman mate = partner();
		if (mate != null && !mate.onFoot) {
			mate.chargeCooldown = Math.min(mate.chargeCooldown, 20);
		}
	}

	private void takeAxe() {
		onFoot = true;
		rearmTicks = 0;
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EamItems.ANCIENT_METAL_AXE));
		setDropChance(EquipmentSlot.MAINHAND, cavalry().body().gearDropChance());
	}

	// ---- Mounted ----

	int chargeCooldown() {
		return chargeCooldown;
	}

	int throwCooldown() {
		return throwCooldown;
	}

	int sparesLeft() {
		return sparesLeft;
	}

	/** Whether he has a lance in hand: not while he takes the next one from the saddle. */
	boolean hasLance() {
		return getMainHandItem().is(Items.IRON_SPEAR);
	}

	/** His partner's charge is about to pass, or has just stunned a shield: time to follow. */
	boolean followUpDue() {
		return followUpTicks == 0;
	}

	/** Lowers the lance and gallops at the target; his partner follows a moment later. */
	void startCharge(LivingEntity target) {
		LostCavalrySettings.Lance lance = cavalry().lance();
		chargeCooldown = lance.maxTicks() + LostCavalryLogic.chargeCooldown(lance, covering());
		followUpTicks = -1;
		lanceStruck = false;
		rideThrough = null;
		startAction(KnightAction.LANCE_CHARGE, target);
		LostWarhorse horse = horse();
		if (horse != null) {
			horse.playSound(SoundEvents.ZOMBIE_HORSE_ANGRY, 1.5F, 0.7F);
		}
		LostCavalryman mate = partner();
		if (mate != null) {
			mate.followCharge(target, lance.followUpDelay());
		}
	}

	/** Charges after his partner in {@code delay} ticks, if he is mounted and free. */
	private void followCharge(LivingEntity target, int delay) {
		if (onFoot || isBusy() || followUpTicks >= 0 || !hasLance()) {
			return;
		}
		setTarget(target);
		followUpTicks = Math.max(1, delay);
	}

	void startThrow(LivingEntity target) {
		startAction(KnightAction.LANCE_THROW, target);
	}

	@Override
	protected boolean tickOwnAction(ServerLevel level, KnightAction action, int t, LivingEntity target) {
		switch (action) {
			case LANCE_CHARGE -> tickCharge(level, t, target);
			case LANCE_THROW -> tickThrow(level, t, target);
			case AXE_DOUBLE -> tickDouble(level, t, target);
			case AXE_SWEEP -> tickSweep(level, t, target);
			case DISMOUNT -> getNavigation().stop();
			default -> {
				return false;
			}
		}
		return true;
	}

	private void tickCharge(ServerLevel level, int t, LivingEntity target) {
		LostCavalrySettings.Lance lance = cavalry().lance();
		LostWarhorse horse = horse();
		if (horse == null) {
			endAction();
			return;
		}
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (lanceStruck) {
			// Gallops on past before wheeling round.
			if (rideThrough == null || t - struckAt > RIDE_THROUGH_TICKS || horse.position().distanceToSqr(rideThrough) < 2.0) {
				endAction();
				return;
			}
			getNavigation().stop();
			getMoveControl().setWantedPosition(rideThrough.x, rideThrough.y, rideThrough.z, 1.0);
			return;
		}
		double distance = horizontalDistanceTo(target);
		Vec3 to = horizontal(target.position().subtract(horse.position()));
		if (distance > 7.0) {
			if (t % 5 == 0 || getNavigation().isDone()) {
				getNavigation().moveTo(target, 1.0);
			}
		} else {
			// Close in, he aims past the target so the horse does not slow down as it gets there.
			Vec3 past = target.position().add(to.scale(4.0));
			getNavigation().stop();
			getMoveControl().setWantedPosition(past.x, target.getY(), past.z, 1.0);
		}
		if (t > 2 && distance <= lance.reach() + REACH_SLACK && Math.abs(target.getY() - horse.getY()) < 3.0
				&& ahead(horse, target, 0.3) && hasLineOfSight(target)) {
			lanceStrike(level, target, lance, to);
			lanceStruck = true;
			struckAt = t;
			Vec3 heading = Vec3.directionFromRotation(0.0F, horse.getYRot());
			rideThrough = horse.position().add(heading.scale(8.0));
		} else if (t >= lance.maxTicks()) {
			endAction();
		}
	}

	/** The couched lance at the gallop: a covering shield takes most of it and is stunned, and his partner comes on. */
	private void lanceStrike(ServerLevel level, LivingEntity target, LostCavalrySettings.Lance lance, Vec3 to) {
		playSound(SoundEvents.TRIDENT_HIT, 1.3F, 0.6F);
		ItemStack shield = target.getItemBlockingWith();
		BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
		boolean blocked = blocks != null && FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ());
		if (blocked) {
			blocks.disable(level, target, lance.shieldSeconds(), shield);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.2F, 0.6F);
			LostCavalryman mate = partner();
			if (mate != null && mate.followUpTicks < 0) {
				mate.followCharge(target, 1);
			}
		}
		float damage = LostCavalryLogic.lanceDamage(lance, blocked);
		DamageSource source = damageSources().source(LostCavalryModule.LANCE_DAMAGE, this);
		if (damage > 0.0F && target.hurtServer(level, source, damage)) {
			target.knockback(1.2, -to.x, -to.z, source, damage, true);
			setLastHurtMob(target);
		}
	}

	private void tickThrow(ServerLevel level, int t, LivingEntity target) {
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		LostWarhorse horse = horse();
		if (horse != null) {
			// The horse halts and turns him to face the target.
			float yaw = (float) (Mth.atan2(target.getZ() - horse.getZ(), target.getX() - horse.getX()) * Mth.RAD_TO_DEG) - 90.0F;
			horse.setYRot(Mth.rotateIfNecessary(horse.getYRot(), yaw, 15.0F));
			horse.setYBodyRot(horse.getYRot());
		}
		if (t == KnightAction.LANCE_THROW_RELEASE && hasLance()) {
			LostCavalrySettings.Throwing throwing = cavalry().throwing();
			ThrownLordSpear spear = ThrownLordSpear.plain(level, this, throwing.damage());
			double dx = target.getX() - getX();
			double dz = target.getZ() - getZ();
			double dy = target.getY(0.5) - spear.getY();
			double distance = Math.sqrt(dx * dx + dz * dz);
			spear.shoot(dx, dy + distance * 0.12, dz, 2.0F, 3.0F);
			level.addFreshEntity(spear);
			playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.6F);
			setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			rearmTicks = REARM_TICKS;
			sparesLeft--;
			throwCooldown = throwing.cooldown();
			chargeCooldown = Math.max(chargeCooldown, REARM_TICKS + 10);
		}
	}

	/** Whether {@code target} is ahead of {@code facer}, within the cone given by the cosine {@code minDot}. */
	private static boolean ahead(Entity facer, Entity target, double minDot) {
		Vec3 to = horizontal(target.position().subtract(facer.position()));
		return to.lengthSqr() < 1.0E-6 || to.dot(Vec3.directionFromRotation(0.0F, facer.getYRot())) >= minDot;
	}

	// ---- On foot ----

	int doubleCooldown() {
		return doubleCooldown;
	}

	int sweepCooldown() {
		return sweepCooldown;
	}

	int sprintCooldownLeft() {
		return sprintCooldown;
	}

	boolean isSprintingNow() {
		return sprintTicks > 0;
	}

	void startSprint() {
		sprintTicks = cavalry().speed().sprintTicks();
		playSound(SoundEvents.HUSK_AMBIENT, 1.2F, 0.6F);
	}

	void startDouble(LivingEntity target) {
		doubleCooldown = cavalry().moves().doubleCooldown();
		startAction(KnightAction.AXE_DOUBLE, target);
	}

	void startSweep(LivingEntity target) {
		sweepCooldown = cavalry().moves().sweepCooldown();
		startAction(KnightAction.AXE_SWEEP, target);
	}

	/** Two quick cuts, right then left, each a quarter weaker than a plain one. */
	private void tickDouble(ServerLevel level, int t, LivingEntity target) {
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		for (int hit : KnightAction.AXE_DOUBLE_HITS) {
			if (t == hit) {
				playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, hit == KnightAction.AXE_DOUBLE_HITS[0] ? 1.1F : 1.3F);
				// The second cut comes sooner than the usual half-second of hurt immunity; it must land.
				target.damageCooldownTime = 0;
				hitIfInReach(level, target, settings().reach() + 0.3, cavalry().moves().doubleMultiplier(), 0.0F);
			}
		}
	}

	/** The axe drawn far back in a crouch, then one full turn: everyone around is hit, round any shield, which stays up. */
	private void tickSweep(ServerLevel level, int t, LivingEntity target) {
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (t < KnightAction.AXE_SWEEP_WINDUP) {
			faceTowards(target);
		}
		if (t == 0) {
			playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.3F, 0.6F);
		}
		if (t != KnightAction.AXE_SWEEP_HIT) {
			return;
		}
		LostCavalrySettings.Moves moves = cavalry().moves();
		double radius = moves.sweepRadius();
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3F, 0.6F);
		for (int i = 0; i < 6; i++) {
			double angle = i * Math.PI / 3.0;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(angle) * radius * 0.6, getY(0.5), getZ() + Math.sin(angle) * radius * 0.6,
					1, 0.0, 0.0, 0.0, 0.0);
		}
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * moves.sweepMultiplier();
		DamageSource source = damageSources().source(LostCavalryModule.SWEEP_DAMAGE, this);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 1.0, radius),
				victim -> victim != this && victim.isAlive() && sweepHits(victim))) {
			if (horizontalDistanceTo(victim) > radius + victim.getBbWidth() / 2.0 || Math.abs(victim.getY() - getY()) > 2.5) {
				continue;
			}
			Vec3 away = horizontal(victim.position().subtract(position()));
			if (victim.hurtServer(level, source, damage)) {
				victim.knockback(0.6, -away.x, -away.z, source, damage, true);
				setLastHurtMob(victim);
			}
		}
	}

	private boolean sweepHits(LivingEntity victim) {
		return victim == getTarget() || victim instanceof IronGolem
				|| victim instanceof Player player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && !LostCommander.commands(player);
	}

	/** Whether {@code target} has a shield it can raise: in hand, and not knocked out. */
	static boolean shieldReady(LivingEntity target) {
		if (target.isBlocking()) {
			return true;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = target.getItemInHand(hand);
			if (stack.has(DataComponents.BLOCKS_ATTACKS) && !(target instanceof Player player && player.getCooldowns().isOnCooldown(stack))) {
				return true;
			}
		}
		return false;
	}

	/** Starts a knight's attack of the matching kind. */
	void startFootAttack(LostCavalryLogic.FootAttack attack, LivingEntity target) {
		switch (attack) {
			case SLASH -> startAttack(SkeletonKnightLogic.Attack.SLASH, target);
			case HEAVY -> startAttack(SkeletonKnightLogic.Attack.HEAVY, target);
			case DOUBLE -> startDouble(target);
			case SWEEP -> startSweep(target);
		}
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("PairLeader", UUIDUtil.CODEC, pairLeader);
		output.putBoolean("OnFoot", onFoot);
		output.putInt("SpareLances", sparesLeft);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		pairLeader = input.read("PairLeader", UUIDUtil.CODEC).orElse(null);
		onFoot = input.getBooleanOr("OnFoot", false);
		sparesLeft = input.getIntOr("SpareLances", cavalry().throwing().spares());
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

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}
}
