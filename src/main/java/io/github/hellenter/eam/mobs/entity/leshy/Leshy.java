package io.github.hellenter.eam.mobs.entity.leshy;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.leshy.LeshyLogic.Move;
import io.github.hellenter.eam.mobs.entity.leshy.LeshyLogic.Point;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The leshy, master of the deep woods: a towering shape of bark, roots and moss under a stag's skull with great antlers,
 * long arms ending in twig claws, eyes glowing in the dark of the skull. It does not come at a player at once. First it
 * leads them astray: it keeps among the trees out of reach, compasses and maps go wild near it, steps sound from the
 * wrong side and the path behind them grows over with bushes. A player who finds it, hits it, or lingers too long gets
 * the fight: claws, lines of roots bursting out of the ground, wolves called from the forest, and a flight as a flock of
 * crows from one tree to another. While it stands by a tree no blow hurts it: it has to be lured into the open, or set
 * on fire, which also burns it twice as hard. It need not be fought at all: bread laid on a stump buys a few days of
 * peace, and it leaves. Numbers live in {@code data/eam/mobs/leshy.json}.
 */
public class Leshy extends Monster {
	/** What it is doing, for its animations. */
	public enum Action { NONE, ROOTS, FLIGHT, HOWL, EAT }

	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(Leshy.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> DATA_ANGRY = SynchedEntityData.defineId(Leshy.class, EntityDataSerializers.BOOLEAN);
	private static final double MOVEMENT_SPEED = 0.28;
	private static final double FOLLOW_RANGE = 48.0;
	/** Lengths of its actions, and the tick in each at which it acts. */
	public static final int ROOTS_TIME = 26;
	public static final int ROOTS_AT = 12;
	public static final int HOWL_TIME = 30;
	public static final int HOWL_AT = 14;
	public static final int EAT_TIME = 40;
	private static final double ROOT_SPACING = 1.25;
	private static final int MAX_ROOTS = 16;
	/** Steps in one false walk, and ticks between them. */
	private static final int STEPS = 4;
	private static final int STEP_GAP = 7;
	/** Scoreboard tag on the wolves it calls, so they can be told apart after a reload. */
	public static final String WOLF_TAG = "eam.leshy_wolf";

	private enum Mood { HAUNT, FIGHT, LEAVE }

	private Mood mood = Mood.HAUNT;
	private @Nullable Player haunted;
	private int hauntTicks;
	private int hitCooldown;
	private int rootCooldown = 60;
	private int wolvesCooldown;
	private int flyCooldown;
	private boolean fledHurt;
	private int actionTicks;
	private @Nullable Vec3 rootTarget;
	private final List<Wolf> wolves = new ArrayList<>();
	private int wolvesAge;
	private @Nullable Vec3 stepAt;
	private int stepsLeft;
	private int stepTimer;
	private int footstepTimer = 200;
	private int overgrowthTimer = 100;
	private @Nullable ItemEntity offering;
	private boolean besideTree;
	/** It was fighting when saved: it turns on the first player it finds again. */
	private boolean angryOnLoad;
	/** Client: the tick its current action started, for the animations. */
	private int actionStart;

	public Leshy(EntityType<? extends Leshy> type, Level level) {
		super(type, level);
		xpReward = 40;
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		LeshySettings.Combat combat = LeshySettings.DEFAULT.combat();
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, combat.health())
				.add(Attributes.ARMOR, combat.armor())
				.add(Attributes.ATTACK_DAMAGE, combat.damage())
				.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
				.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	static LeshySettings settings() {
		return LeshyModule.settings();
	}

	/**
	 * Natural spawns: from the configured day of the world on, only in the regions that are a leshy's wood, among the
	 * trees, now and then, far from any other leshy and never more of them than players in the level.
	 */
	public static boolean checkSpawnRules(EntityType<Leshy> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		LeshySettings.Spawn spawn = settings().spawn();
		ServerLevel server = level.getLevel();
		int day = BloodMoonLogic.dayNumber(server.getServer().overworld().getOverworldClockTime());
		if (!LeshyLogic.oldEnough(day, spawn.minDay()) || random.nextFloat() >= spawn.spawnChance()
				|| !LeshyLogic.leshyWood(server.getSeed(), pos.getX(), pos.getZ(), spawn.regionSize(), spawn.regionChance())
				|| !logNear(level, pos, spawn.treeRadius(), 0, 3)) {
			return false;
		}
		List<? extends Leshy> all = server.getEntities(EamEntities.LESHY, Entity::isAlive);
		double spacing = spawn.spacing();
		return all.size() < server.players().size() && all.stream().noneMatch(leshy -> leshy.distanceToSqr(Vec3.atCenterOf(pos)) < spacing * spacing);
	}

	/** Whether a log lies within {@code radius} sideways of {@code pos}, from {@code low} to {@code high} blocks above it. */
	static boolean logNear(LevelReader level, BlockPos pos, int radius, int low, int high) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dy = low; dy <= high; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (level.getBlockState(at.setWithOffset(pos, dx, dy, dz)).is(BlockTags.LOGS)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_ANGRY, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new LeshyGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		applySettings();
		setCanPickUpLoot(false);
		return data;
	}

	private void applySettings() {
		LeshySettings.Combat combat = settings().combat();
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(combat.health());
		getAttribute(Attributes.ARMOR).setBaseValue(combat.armor());
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(combat.damage());
		setHealth(getMaxHealth());
	}

	// ---- Equipment: none ----

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return false;
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	/** It walks and spawns in any light, and the sun does not hurt it. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	// ---- State, for the goal and the renderer ----

	public Action action() {
		int id = entityData.get(DATA_ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.NONE;
	}

	private void setAction(Action action, int ticks) {
		entityData.set(DATA_ACTION, (byte) action.ordinal());
		actionTicks = ticks;
	}

	/** Ticks since its current action started, on the client. */
	public int actionAge() {
		return tickCount - actionStart;
	}

	/** Whether it fights, rather than haunts. */
	public boolean isAngry() {
		return entityData.get(DATA_ANGRY);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			actionStart = tickCount;
		}
	}

	boolean busy() {
		return action() != Action.NONE;
	}

	boolean fighting() {
		return mood == Mood.FIGHT;
	}

	boolean leaving() {
		return mood == Mood.LEAVE;
	}

	@Nullable Player haunted() {
		return haunted;
	}

	@Nullable ItemEntity offering() {
		return offering;
	}

	boolean canHit() {
		return hitCooldown <= 0;
	}

	// ---- Who it haunts, and the fight ----

	private boolean fair(Player player) {
		return player.isAlive() && !player.isRemoved() && player.level() == level() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
				&& !LeshyModule.atPeace(player);
	}

	private void updateHaunted(ServerLevel level) {
		double giveUp = settings().haunt().giveUpDistance();
		if (haunted != null && (!fair(haunted) || haunted.distanceToSqr(this) > giveUp * giveUp)) {
			haunted = null;
			hauntTicks = 0;
			if (mood == Mood.FIGHT && getTarget() instanceof Player) {
				calmDown();
			}
		}
		if (haunted == null && (tickCount + getId()) % 20 == 0) {
			Player nearest = level.getNearestPlayer(getX(), getY(), getZ(), FOLLOW_RANGE, entity -> entity instanceof Player p && fair(p));
			if (nearest != null) {
				haunted = nearest;
				hauntTicks = 0;
				if (angryOnLoad) {
					anger(nearest);
				}
			}
			angryOnLoad = false;
		}
	}

	/** It turns on {@code target}: the haunting is over, the fight begins. */
	void anger(LivingEntity target) {
		setTarget(target);
		if (mood != Mood.FIGHT) {
			mood = Mood.FIGHT;
			entityData.set(DATA_ANGRY, true);
			playSound(EamSounds.LESHY_ROAR, 2.0F, getVoicePitch());
		}
	}

	private void calmDown() {
		mood = Mood.HAUNT;
		entityData.set(DATA_ANGRY, false);
		setTarget(null);
		hauntTicks = 0;
		fledHurt = false;
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (hitCooldown > 0) hitCooldown--;
		if (rootCooldown > 0) rootCooldown--;
		if (wolvesCooldown > 0) wolvesCooldown--;
		if (flyCooldown > 0) flyCooldown--;
		LeshySettings settings = settings();
		if (tickCount % 5 == 0) {
			besideTree = logNear(level, blockPosition(), settings.combat().treeRadius(), 0, 2);
		}
		if (besideTree && !isOnFire() && settings.combat().treeRegen() > 0 && tickCount % settings.combat().treeRegen() == 0) {
			heal(1.0F);
		}
		tickWolves(level, settings.combat().wolves());
		if (busy()) {
			tickAction(level, settings);
			return;
		}
		if (mood == Mood.LEAVE) {
			// Leaving when it was saved: it is gone.
			discard();
			return;
		}
		updateHaunted(level);
		if (tickCount % 20 == 0) {
			bewilder(level, settings.haunt());
			lookForOffering(level, settings.offering());
		}
		if (mood == Mood.HAUNT) {
			tickHaunt(level, settings.haunt());
		} else {
			tickFight(level, settings);
		}
	}

	private void tickHaunt(ServerLevel level, LeshySettings.Haunt haunt) {
		Player player = haunted;
		if (player == null) {
			return;
		}
		hauntTicks++;
		double distance = Math.sqrt(distanceToSqr(player));
		if (distance < haunt.noticeDistance() || hauntTicks >= haunt.haunt()) {
			anger(player);
			return;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			tickFootsteps(serverPlayer, haunt);
		}
		if (--overgrowthTimer <= 0) {
			overgrowthTimer = haunt.overgrowthInterval() / 2 + random.nextInt(haunt.overgrowthInterval() + 1);
			overgrow(level, player);
		}
	}

	private void tickFight(ServerLevel level, LeshySettings settings) {
		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || target instanceof Player player && !fair(player)) {
			calmDown();
			return;
		}
		if (tickCount % 10 != 0) {
			return;
		}
		LeshySettings.Combat combat = settings.combat();
		double distance = Math.sqrt(distanceToSqr(target));
		boolean fleeHurt = !fledHurt && !besideTree && getHealth() < getMaxHealth() * combat.teleport().health();
		Move move = LeshyLogic.move(distance, fleeHurt, flyCooldown, combat.teleport().distance(), wolvesCooldown, wolves.size(),
				combat.wolves().count(), rootCooldown, combat.rootMinRange(), combat.rootMaxRange());
		switch (move) {
			case FLY -> {
				if (fleeHurt) fledHurt = true;
				startFlight(level, combat.teleport().duration());
			}
			case WOLVES -> {
				setAction(Action.HOWL, HOWL_TIME);
				wolvesCooldown = combat.wolves().cooldown();
				playSound(EamSounds.LESHY_ROAR, 2.0F, 0.8F);
			}
			case ROOTS -> {
				if (getSensing().hasLineOfSight(target)) {
					setAction(Action.ROOTS, ROOTS_TIME);
					rootCooldown = combat.rootCooldown();
					rootTarget = null;
				}
			}
			case MELEE -> {
			}
		}
	}

	private void tickAction(ServerLevel level, LeshySettings settings) {
		getNavigation().stop();
		Action action = action();
		int elapsed = actionLength(action) - actionTicks;
		LivingEntity target = getTarget();
		switch (action) {
			case ROOTS -> {
				if (target != null) {
					getLookControl().setLookAt(target, 30.0F, 30.0F);
					if (elapsed == ROOTS_AT - 4) {
						// Where the target stood as it plunged its hands in: the line runs there.
						rootTarget = target.position();
					}
				}
				if (elapsed == ROOTS_AT && rootTarget != null) {
					callRoots(level, rootTarget, settings.combat());
				}
			}
			case HOWL -> {
				if (elapsed == HOWL_AT && target != null) {
					callWolves(level, target, settings.combat().wolves());
				}
			}
			case FLIGHT -> {
				if (elapsed % 3 == 0) {
					level.sendParticles(EamParticles.KOSCHEI_RAVEN, getX(), getY() + 1.5, getZ(), 0, (random.nextDouble() - 0.5) * 0.4, 0.1,
							(random.nextDouble() - 0.5) * 0.4, 1.0);
				}
			}
			case EAT -> {
				if (elapsed % 6 == 0 && offering != null) {
					playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.6F);
					level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, offering.getItem().getItem()), getX(), getY() + 2.2, getZ(), 6, 0.2,
							0.1, 0.2, 0.05);
				}
			}
			case NONE -> {
			}
		}
		if (--actionTicks <= 0) {
			entityData.set(DATA_ACTION, (byte) Action.NONE.ordinal());
			if (action == Action.FLIGHT) {
				endFlight(level);
			} else if (action == Action.EAT) {
				accept(level, settings.offering());
			}
		}
	}

	private static int actionLength(Action action) {
		return switch (action) {
			case ROOTS -> ROOTS_TIME;
			case HOWL -> HOWL_TIME;
			case EAT -> EAT_TIME;
			case FLIGHT -> settings().combat().teleport().duration();
			case NONE -> 0;
		};
	}

	/** A blow with its claws. */
	void strike(ServerLevel level, LivingEntity target) {
		hitCooldown = settings().combat().hitInterval();
		swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
		playSound(EamSounds.LESHY_ATTACK, 1.2F, getVoicePitch());
		doHurtTarget(level, target);
	}

	double reach() {
		return settings().combat().reach();
	}

	// ---- Leading astray ----

	/** Everyone near enough is led astray: compasses and maps spin. */
	private void bewilder(ServerLevel level, LeshySettings.Haunt haunt) {
		double radius = haunt.bewilderRadius();
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), this::fair)) {
			if (player.distanceToSqr(this) <= radius * radius) {
				player.addEffect(new MobEffectInstance(LeshyModule.BEWILDERED, haunt.bewilderDuration(), 0, true, false, true), this);
			}
		}
	}

	/** Now and then a few steps sound near the player, from the side away from it; only they hear them. */
	private void tickFootsteps(ServerPlayer player, LeshySettings.Haunt haunt) {
		if (stepsLeft > 0 && stepAt != null) {
			if (--stepTimer <= 0) {
				stepTimer = STEP_GAP + random.nextInt(3);
				stepsLeft--;
				boolean twig = random.nextInt(4) == 0;
				SoundEvent sound = twig ? SoundEvents.WOOD_BREAK : SoundEvents.GRASS_STEP;
				// Each step a little closer, as if someone walked up behind a tree.
				Vec3 at = stepAt.add((player.getX() - stepAt.x) * 0.06 * (STEPS - stepsLeft), 0.0, (player.getZ() - stepAt.z) * 0.06 * (STEPS - stepsLeft));
				player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT, at.x, at.y,
						at.z, twig ? 0.5F : 0.8F, 0.8F + random.nextFloat() * 0.2F, random.nextLong()));
			}
			return;
		}
		if (--footstepTimer <= 0) {
			footstepTimer = haunt.footstepInterval() / 2 + random.nextInt(haunt.footstepInterval() + 1);
			Point point = LeshyLogic.decoy(player.getX(), player.getZ(), getX(), getZ(), 6.0 + random.nextDouble() * 3.0,
					(random.nextFloat() - 0.5F) * 80.0F);
			stepAt = new Vec3(point.x(), player.getY(), point.z());
			stepsLeft = STEPS;
			stepTimer = 0;
			if (random.nextInt(3) == 0) {
				player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(EamSounds.LESHY_CROWS), SoundSource.AMBIENT,
						point.x(), player.getY() + 6.0, point.z(), 0.7F, 0.9F + random.nextFloat() * 0.2F, random.nextLong()));
			}
		}
	}

	/** The path behind the player grows over: a bush comes up on the grass a few blocks back, out of their sight. */
	private void overgrow(ServerLevel level, Player player) {
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		Point point = LeshyLogic.behind(player.getX(), player.getZ(), player.getYRot(), 3.0 + random.nextDouble() * 3.0,
				(random.nextFloat() - 0.5F) * 100.0F);
		BlockPos column = BlockPos.containing(point.x(), player.getY(), point.z());
		for (int dy = 2; dy >= -3; dy--) {
			BlockPos pos = column.above(dy);
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
				continue;
			}
			Holder<Block> grown = level.registryAccess().lookupOrThrow(Registries.BLOCK).getRandomElementOf(LeshyModule.OVERGROWTH, random)
					.orElse(null);
			if (grown == null) {
				return;
			}
			BlockState state = grown.value().defaultBlockState();
			if (state.canSurvive(level, pos)) {
				level.setBlockAndUpdate(pos, state);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), pos.getX() + 0.5,
						pos.getY() + 0.3, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);
			}
			return;
		}
	}

	// ---- Roots, wolves and the flight ----

	/** A line of roots from its feet to {@code at} and a little past, bursting one after the other. */
	private void callRoots(ServerLevel level, Vec3 at, LeshySettings.Combat combat) {
		double dx = at.x - getX();
		double dz = at.z - getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance < 1.0E-3) {
			return;
		}
		int count = LeshyLogic.rootCount(distance, ROOT_SPACING, MAX_ROOTS);
		double stepX = dx / distance * ROOT_SPACING;
		double stepZ = dz / distance * ROOT_SPACING;
		double startY = Math.max(getY(), at.y);
		for (int i = 0; i < count; i++) {
			double x = getX() + stepX * (i + 1);
			double z = getZ() + stepZ * (i + 1);
			Double y = groundY(level, x, startY, z);
			if (y != null) {
				level.addFreshEntity(LeshyRoot.create(level, this, x, y, z, i * 2, combat.rootWarning(), combat.rootDamage(), combat.rootHold()));
			}
		}
	}

	/** The top of the ground under ({@code x}, {@code z}), from a little above {@code fromY} down a few blocks. */
	private static @Nullable Double groundY(ServerLevel level, double x, double fromY, double z) {
		BlockPos.MutableBlockPos at = BlockPos.containing(x, fromY + 2.0, z).mutable();
		for (int i = 0; i < 7; i++, at.move(0, -1, 0)) {
			BlockState below = level.getBlockState(at.below());
			if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && below.isFaceSturdy(level, at.below(), Direction.UP)) {
				return (double) at.getY();
			}
		}
		return null;
	}

	/** Wolves come out of the trees around it and go for its target. */
	private void callWolves(ServerLevel level, LivingEntity target, LeshySettings.Wolves settings) {
		dismissWolves(level);
		wolvesAge = 0;
		for (int i = 0; i < settings.count(); i++) {
			Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (wolf == null) {
				continue;
			}
			double angle = random.nextDouble() * Math.PI * 2.0;
			double x = getX() + Math.cos(angle) * 3.0;
			double z = getZ() + Math.sin(angle) * 3.0;
			Double y = groundY(level, x, getY(), z);
			wolf.snapTo(x, y != null ? y : getY(), z, random.nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(wolf)) {
				wolf.snapTo(getX(), getY(), getZ(), random.nextFloat() * 360.0F, 0.0F);
			}
			wolf.finalizeSpawn(level, level.getCurrentDifficultyAt(wolf.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
			wolf.addTag(WOLF_TAG);
			wolf.setPersistentAngerTarget(EntityReference.of(target));
			wolf.startPersistentAngerTimer();
			wolf.setTarget(target);
			level.addFreshEntity(wolf);
			leaves(level, wolf.position());
			wolves.add(wolf);
		}
	}

	private void tickWolves(ServerLevel level, LeshySettings.Wolves settings) {
		wolves.removeIf(wolf -> !wolf.isAlive());
		if (wolves.isEmpty()) {
			return;
		}
		if (++wolvesAge >= settings.lifetime() || mood != Mood.FIGHT) {
			dismissWolves(level);
			return;
		}
		LivingEntity target = getTarget();
		if (target != null) {
			for (Wolf wolf : wolves) {
				if (wolf.getTarget() != target) {
					wolf.setPersistentAngerTarget(EntityReference.of(target));
					wolf.setTarget(target);
				}
			}
		}
	}

	/** Its wolves melt back into the forest in a swirl of leaves. */
	private void dismissWolves(ServerLevel level) {
		for (Wolf wolf : wolves) {
			if (wolf.isAlive()) {
				leaves(level, wolf.position());
				wolf.discard();
			}
		}
		wolves.clear();
	}

	private static void leaves(ServerLevel level, Vec3 at) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), at.x, at.y + 0.5, at.z, 20, 0.4,
				0.4, 0.4, 0.05);
	}

	/** It breaks up into a flock of crows and is gone. */
	void startFlight(ServerLevel level, int duration) {
		flyCooldown = settings().combat().teleport().cooldown();
		setAction(Action.FLIGHT, duration);
		getNavigation().stop();
		setInvisible(true);
		playSound(EamSounds.LESHY_CROWS, 2.0F, 1.0F);
		for (int i = 0; i < 14; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			level.sendParticles(EamParticles.KOSCHEI_RAVEN, getX() + Math.cos(a) * 0.5, getY() + 0.5 + random.nextDouble() * 2.2,
					getZ() + Math.sin(a) * 0.5, 0, Math.cos(a) * 0.5, 0.15 + random.nextDouble() * 0.2, Math.sin(a) * 0.5, 1.0);
		}
	}

	/** The crows come down by a tree near its target and it stands up out of them; leaving, it is simply gone. */
	private void endFlight(ServerLevel level) {
		setInvisible(false);
		if (mood == Mood.LEAVE) {
			dismissWolves(level);
			discard();
			return;
		}
		LivingEntity target = getTarget() != null ? getTarget() : haunted;
		if (target != null) {
			Vec3 spot = landing(level, target);
			if (spot != null) {
				teleportTo(spot.x, spot.y, spot.z);
				getNavigation().stop();
			}
		}
		playSound(EamSounds.LESHY_CROWS, 2.0F, 0.85F);
		for (int i = 0; i < 14; i++) {
			double a = random.nextDouble() * Math.PI * 2.0;
			double r = 2.5;
			level.sendParticles(EamParticles.KOSCHEI_RAVEN, getX() + Math.cos(a) * r, getY() + 0.5 + random.nextDouble() * 2.2, getZ() + Math.sin(a) * r,
					0, -Math.cos(a) * 0.4, 0.05, -Math.sin(a) * 0.4, 1.0);
		}
	}

	/** A free spot 5 to 11 blocks from the target, next to a tree where it can find one. */
	private @Nullable Vec3 landing(ServerLevel level, LivingEntity target) {
		Vec3 fallback = null;
		for (int i = 0; i < 24; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 5.0 + random.nextDouble() * 6.0;
			double x = target.getX() + Math.cos(angle) * distance;
			double z = target.getZ() + Math.sin(angle) * distance;
			Double y = groundY(level, x, target.getY() + 2.0, z);
			if (y == null) {
				continue;
			}
			Vec3 at = new Vec3(x, y, z);
			if (!level.noCollision(this, getDimensions(getPose()).makeBoundingBox(at)) || !level.getFluidState(BlockPos.containing(at)).isEmpty()) {
				continue;
			}
			if (logNear(level, BlockPos.containing(at), 2, 0, 2)) {
				return at;
			}
			if (fallback == null) {
				fallback = at;
			}
		}
		return fallback;
	}

	// ---- The offering ----

	/** Bread (or whatever the tag holds) lying on a stump near it, enough of it in one stack. */
	private void lookForOffering(ServerLevel level, LeshySettings.Offering settings) {
		if (offering != null && (!offering.isAlive() || !isOffering(level, offering, settings))) {
			offering = null;
		}
		if (offering != null) {
			return;
		}
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(settings.search()),
				item -> isOffering(level, item, settings));
		offering = items.isEmpty() ? null : items.getFirst();
	}

	private static boolean isOffering(ServerLevel level, ItemEntity item, LeshySettings.Offering settings) {
		ItemStack stack = item.getItem();
		return stack.is(LeshyModule.OFFERINGS) && stack.getCount() >= settings.count() && item.onGround()
				&& level.getBlockState(BlockPos.containing(item.getX(), item.getY() - 0.1, item.getZ())).is(BlockTags.LOGS);
	}

	/** The goal brought it to the offering: it eats. */
	void eatOffering() {
		if (offering != null) {
			setAction(Action.EAT, EAT_TIME);
		}
	}

	/** It has eaten: whoever laid the bread is left in peace, and it goes. */
	private void accept(ServerLevel level, LeshySettings.Offering settings) {
		ItemEntity item = offering;
		offering = null;
		if (item == null || !item.isAlive() || item.getItem().getCount() < settings.count()) {
			return;
		}
		ItemStack rest = item.getItem().copyWithCount(item.getItem().getCount() - settings.count());
		if (rest.isEmpty()) {
			item.discard();
		} else {
			item.setItem(rest);
		}
		long until = level.getGameTime() + settings.peaceDays() * 24000L;
		Entity thrower = item.getOwner();
		if (thrower instanceof Player player) {
			LeshyModule.makePeace(player, until);
		}
		if (haunted != null && haunted != thrower) {
			LeshyModule.makePeace(haunted, until);
		}
		LivingEntity target = getTarget();
		if (target instanceof Player player && player != thrower) {
			LeshyModule.makePeace(player, until);
		}
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 2.5, getZ(), 12, 0.6, 0.6, 0.6, 0.0);
		playSound(EamSounds.LESHY_AMBIENT, 2.0F, 1.2F);
		mood = Mood.LEAVE;
		entityData.set(DATA_ANGRY, false);
		setTarget(null);
		dismissWolves(level);
		startFlight(level, settings().combat().teleport().duration());
	}

	// ---- Damage ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (action() == Action.FLIGHT) {
			return false;
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof Player player && mood != Mood.LEAVE) {
			// Striking it breaks any peace bought from it.
			LeshyModule.breakPeace(player);
			if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
				haunted = player;
				anger(player);
			}
		} else if (attacker instanceof LivingEntity living && living != this && !LeshyModule.isLeshysWolf(living) && mood == Mood.HAUNT) {
			anger(living);
		}
		LeshySettings.Combat combat = settings().combat();
		if (LeshyLogic.shielded(besideTree, isOnFire()) && !source.is(DamageTypeTags.IS_FIRE)) {
			// The tree takes the blow: a dull knock and a spray of bark.
			playSound(EamSounds.LESHY_SHIELD, 1.2F, 0.7F + random.nextFloat() * 0.2F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), getX(), getY() + 1.5, getZ(), 12,
					0.4, 0.6, 0.4, 0.05);
			return false;
		}
		return super.hurtServer(level, source, LeshyLogic.damageTaken(damage, isOnFire(), combat.fireDamage()));
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel level) {
			dismissWolves(level);
		}
		super.remove(reason);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			leaves(level, position().add(0.0, 1.0, 0.0));
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), getX(), getY() + 1.5, getZ(), 30,
					0.5, 1.0, 0.5, 0.1);
		}
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Angry", mood == Mood.FIGHT);
		output.putBoolean("Leaving", mood == Mood.LEAVE);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		// Its target is not saved: an angry leshy turns on the nearest player again.
		mood = input.getBooleanOr("Leaving", false) ? Mood.LEAVE : Mood.HAUNT;
		angryOnLoad = mood == Mood.HAUNT && input.getBooleanOr("Angry", false);
	}

	// ---- Sounds ----

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return EamSounds.LESHY_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected float getSoundVolume() {
		return 1.4F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return EamSounds.LESHY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return EamSounds.LESHY_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.9F + random.nextFloat() * 0.15F;
	}
}
