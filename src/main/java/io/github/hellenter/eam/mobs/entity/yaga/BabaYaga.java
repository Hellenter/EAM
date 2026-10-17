package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.boss.ParryPenalty;
import io.github.hellenter.eam.mobs.entity.boss.ParryStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Attack;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Buff;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Creature;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Glyph;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.GroupKind;
import io.github.hellenter.eam.mobs.entity.yaga.grave.YagaGraveMapItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Baba Yaga, the swamp witch. Phase one: she circles high over the player in her mortar, untouchable by blade, arrow or
 * spell, and fights through others: groups of swamp creatures raised around the player, in sub-waves a while apart, poison flasks,
 * spells that strengthen one group (absorption, speed or strength, picked by what the group needs) and glyphs on the
 * ground that curse the player and help her creatures. Only once a wave is dead does she dive at the player with her
 * staff; just before it strikes she lights up, and a hit then parries her for a fixed share of her health. A dive that
 * misses the player is flown again: no new wave rises until one reaches them. While she fights, a thunderstorm of her own
 * rages within a hundred blocks of her; once she has seen no player for three minutes it ends and she goes back where
 * she started. The fifth
 * parry bursts her mortar and she drops to the ground: phase two. On foot she keeps behind one group raised in front of
 * her and casts on it, runs from a player who comes close, escapes into the dark after a couple of hits to come back
 * behind them with more creatures, shoots acid bolts (through a shield), purple bolts that tie the player down with
 * threads and a fire beam (a shield blocks it), blinks behind the player for a parryable flurry of claws, and eats one
 * of her own creatures to heal. Numbers are in {@code data/eam/mobs/baba_yaga.json}.
 */
public class BabaYaga extends Monster {
	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(BabaYaga.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(BabaYaga.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_GLOW = SynchedEntityData.defineId(BabaYaga.class, EntityDataSerializers.BYTE);
	/** Where her fire beam hits, from her feet; zero while it is not burning. */
	private static final EntityDataAccessor<Vector3fc> DATA_BEAM = SynchedEntityData.defineId(BabaYaga.class, EntityDataSerializers.VECTOR3);
	/** Set while she waits in her hut, unseen, for a player to come up to its door. */
	private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(BabaYaga.class, EntityDataSerializers.BOOLEAN);
	private static final Identifier FOLLOW_MODIFIER = Eam.id("baba_yaga_minion_follow");
	private static final Identifier HEALTH_MODIFIER = Eam.id("baba_yaga_minion_health");

	private static final EntityDimensions IN_MORTAR = EntityDimensions.scalable(0.95F, 2.1F).withEyeHeight(1.85F);
	private static final EntityDimensions ON_FOOT = EntityDimensions.scalable(0.6F, 1.75F).withEyeHeight(1.5F);
	/** Phase one: least ticks between two of her casts, so they don't all come at once. */
	private static final int CAST_GAP = 25;
	/** Phase one: after the dive she raises the next wave this long after she is back up. */
	private static final int NEXT_WAVE_DELAY = 20;
	/** How far her claws reach, flat from her centre. */
	private static final double CLAW_REACH = 2.6;
	/** She eats a creature from this close. */
	private static final double EAT_REACH = 1.9;
	/** The fire beam deals its damage this often. */
	private static final int BEAM_EVERY = 5;
	/** Icons over a strengthened creature: how many go round in each ring, and how fast (radians a tick). */
	private static final int ICONS = 3;
	private static final double ICON_TURN = 0.08;
	/** A save from before her storm was her own kept the world's weather this long at most; older ones are stale. */
	private static final int LEGACY_STORM_TICKS = 1200;
	private static final DustParticleOptions MORTAR_DUST = new DustParticleOptions(0x5A4632, 1.4F);

	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(random), getDisplayName(),
			BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

	private YagaAction action = YagaAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private final ParryStreak parryStreak = new ParryStreak();
	/** Set while a parry's fixed damage goes through, so armor and effects don't change it. */
	private boolean exactHit;
	/** What killed her, kept until she has burnt to ash and drops her loot. */
	private @Nullable DamageSource deathSource;
	private int deathPlayerMemory;

	// Phase one
	private int potionCooldown;
	private int spellCooldown;
	private int glyphCooldown;
	private int castGap;
	/** Ticks until she raises the next wave, or -1 while one is out. */
	private int nextWave = -1;
	/** The later sub-waves of the current wave's groups, still to rise, and ticks until the next of them do. */
	private final List<LaterPart> laterParts = new ArrayList<>();
	private int laterPartsTimer;
	private float waveAngle;
	private GroupKind waveFirstKind = GroupKind.LURKERS;
	private int clearedTicks;
	/** Whether her last dive reached the player (a blow, blocked or not, or a parry); without that no new wave rises. */
	private boolean diveLanded;
	private float orbitAngle;
	private int climbTicks;
	private @Nullable Glyph lastGlyph;
	private int pendingGlyph = -1;

	// Phase two
	private final int[] attackCooldowns = new int[Attack.values().length];
	private int summonCooldown;
	/** Ticks she can't raise creatures for after reaching the cap of living ones. */
	private int summonLockout;
	private int escapeCooldown;
	private int eatCooldown;
	private int idleTicks;
	private int repath;
	private final ArrayDeque<Long> recentHits = new ArrayDeque<>();
	private boolean escapeRequested;
	/** The escape after a parry: she blinks away at once, without running off first. */
	private boolean escapeAtOnce;
	private @Nullable Mob eatTarget;
	private Vec3 beamAim = Vec3.ZERO;
	private @Nullable Vec3 escapeTo;

	// Her thunderstorm, around her alone: from her first fight until she has lost the players for good
	private boolean stormActive;
	/** Ticks since she last saw a player. */
	private int unseenTicks;
	/** Where she started; she goes back there once she has lost the players. */
	private @Nullable BlockPos home;
	private boolean returningHome;
	/** Her hut's door, which she bursts out of once a player comes up to it. */
	private @Nullable BlockPos hutDoor;
	/** Loaded from a save made while her storm was still the whole world's weather: that weather goes back once. */
	private @Nullable LegacyWeather legacyWeather;

	private record LegacyWeather(int clear, int rain, int thunder, boolean raining, boolean thundering) {
	}

	private final List<Group> groups = new ArrayList<>();
	/** Set on load: her creatures are saved with their chunks, but her groups are not, so she takes them back. */
	private boolean adoptMinions;
	private final List<SpellBolt> spells = new ArrayList<>();
	/** Client: the tick count when the current action started. */
	private int clientActionStart;

	public BabaYaga(EntityType<? extends BabaYaga> type, Level level) {
		super(type, level);
		xpReward = settings().stats().xp();
		nextWave = settings().summon().firstDelay();
		setNoGravity(true);
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		BabaYagaSettings.Stats stats = BabaYagaSettings.DEFAULT.stats();
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, stats.phaseOneHealth())
				.add(Attributes.ARMOR, stats.armor())
				.add(Attributes.MOVEMENT_SPEED, stats.speed())
				.add(Attributes.FOLLOW_RANGE, stats.followRange())
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				.add(Attributes.ATTACK_DAMAGE, BabaYagaSettings.DEFAULT.claws().damage())
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	private static BabaYagaSettings settings() {
		return BabaYagaModule.settings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE, (byte) 1);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_GLOW, (byte) 0);
		builder.define(DATA_BEAM, new Vector3f());
		builder.define(DATA_WAITING, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this) {
			@Override
			public boolean canUse() {
				return phase() == 2 && super.canUse();
			}
		});
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		applyStats(true);
		// Spawned on the ground, she rises into the air first.
		setPos(getX(), getY() + 0.5, getZ());
		return data;
	}

	/** Health of the current phase, armor, speed and the rest from the data file; {@code full} heals her to the top. */
	private void applyStats(boolean full) {
		BabaYagaSettings.Stats stats = settings().stats();
		setBase(Attributes.MAX_HEALTH, phase() == 1 ? stats.phaseOneHealth() : stats.phaseTwoHealth());
		setBase(Attributes.ARMOR, stats.armor());
		setBase(Attributes.MOVEMENT_SPEED, stats.speed());
		setBase(Attributes.FOLLOW_RANGE, stats.followRange());
		setHealth(full ? getMaxHealth() : Math.min(getHealth(), getMaxHealth()));
		xpReward = stats.xp();
	}

	private void setBase(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	// ---- State for the client ----

	public int phase() {
		return entityData.get(DATA_PHASE);
	}

	public YagaAction action() {
		return YagaAction.byId(entityData.get(DATA_ACTION));
	}

	public YagaGlow glow() {
		return YagaGlow.byId(entityData.get(DATA_GLOW));
	}

	/** Where her fire beam hits, from her feet, or null while it is not burning. */
	public @Nullable Vec3 beamEnd() {
		Vector3fc end = entityData.get(DATA_BEAM);
		return end.lengthSquared() < 1.0E-6F ? null : new Vec3(end);
	}

	/** In her mortar, in the air. */
	public boolean inMortar() {
		return phase() == 1;
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
		if (DATA_PHASE.equals(accessor)) {
			refreshDimensions();
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return phase() == 1 ? IN_MORTAR : ON_FOOT;
	}

	private void setGlow(YagaGlow glow) {
		if (glow() != glow) {
			entityData.set(DATA_GLOW, (byte) glow.ordinal());
		}
	}

	// ---- Movement ----

	/** In her mortar she flies wherever she is steered, with nothing pulling her down. */
	@Override
	public void travel(Vec3 input) {
		if (phase() == 1 && action != YagaAction.SHATTER) {
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.91));
			return;
		}
		super.travel(input);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}

	/** Flies towards {@code to} at up to {@code speed} blocks a tick. */
	private void flyTowards(Vec3 to, double speed) {
		Vec3 delta = to.subtract(position());
		double length = delta.length();
		Vec3 velocity = length > speed ? delta.scale(speed / length) : delta;
		if (horizontalCollision) {
			// Something in the way: over it.
			climbTicks = 20;
		}
		if (climbTicks > 0) {
			climbTicks--;
			velocity = new Vec3(velocity.x * 0.5, Math.max(velocity.y, speed * 0.8), velocity.z * 0.5);
		}
		setDeltaMovement(velocity);
	}

	/** Where she circles over the player: around them, up high, kept under any roof over their head. */
	private Vec3 orbitPoint(LivingEntity target, double radius) {
		BabaYagaSettings.Flight flight = settings().flight();
		double angle = Math.toRadians(orbitAngle);
		double x = target.getX() + Math.cos(angle) * radius;
		double z = target.getZ() + Math.sin(angle) * radius;
		double y = target.getY() + ceilingClearance(target, flight.altitude());
		return new Vec3(x, y, z);
	}

	/** How high she can fly over {@code target}, up to {@code altitude}: two blocks under the first roof above them. */
	private double ceilingClearance(LivingEntity target, double altitude) {
		Vec3 from = target.getEyePosition();
		Vec3 to = from.add(0.0, altitude, 0.0);
		var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		double room = hit.getLocation().y - target.getY() - getBbHeight() - 0.5;
		return Mth.clamp(room, 1.0, altitude);
	}

	private void faceTowards(Entity target) {
		float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
		setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 30.0F));
		setYBodyRot(getYRot());
		setYHeadRot(getYRot());
	}

	public double horizontalDistanceTo(Entity other) {
		double dx = other.getX() - getX();
		double dz = other.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}

	/** Where her staff's head is, for spells and bolts. */
	private Vec3 staffTip() {
		return position().add(staffTipOffset(yBodyRot, getBbHeight(), phase() == 1));
	}

	/** Her staff's head from her feet, for her body turned {@code bodyRot}; the client draws the fire beam from it. */
	public static Vec3 staffTipOffset(float bodyRot, float height, boolean inMortar) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyRot);
		Vec3 right = Vec3.directionFromRotation(0.0F, bodyRot + 90.0F);
		return new Vec3(0.0, height + (inMortar ? 0.2 : 0.35), 0.0).add(forward.scale(0.5)).add(right.scale(-0.45));
	}

	// ---- Waiting in her hut ----

	/**
	 * She waits in her hut: unseen, untouchable and still, until a player comes within
	 * {@link BabaYagaSettings.Storm#hutWakeRadius()} of {@code door}. Then the door bursts open and she is out, laughing.
	 */
	public void waitInHut(BlockPos door) {
		hutDoor = door.immutable();
		setWaiting(true);
	}

	/** Whether she is still waiting in her hut, unseen. */
	public boolean waiting() {
		return entityData.get(DATA_WAITING);
	}

	private void setWaiting(boolean waiting) {
		entityData.set(DATA_WAITING, waiting);
		bossEvent.setVisible(!waiting);
	}

	@Override
	public void aiStep() {
		if (!waiting()) {
			super.aiStep();
			return;
		}
		setDeltaMovement(Vec3.ZERO);
		if (level() instanceof ServerLevel level && tickCount % 5 == 0) {
			Player player = playerAtDoor(level);
			if (player != null) {
				burstOut(level, player);
			}
		}
	}

	/** A player close enough to her hut's door to wake her; spectators don't. */
	private @Nullable Player playerAtDoor(ServerLevel level) {
		Vec3 door = hutDoor != null ? Vec3.atCenterOf(hutDoor) : position();
		for (Player player : level.players()) {
			if (player.isAlive() && !player.isSpectator()
					&& BabaYagaLogic.wakesAtHut(player.distanceToSqr(door), settings().storm())) {
				return player;
			}
		}
		return null;
	}

	/** The door flies open and she is there in a cloud of ink, cackling; a player she can fight is her first target. */
	private void burstOut(ServerLevel level, Player player) {
		setWaiting(false);
		if (hutDoor != null) {
			BlockState door = level.getBlockState(hutDoor);
			level.playSound(null, hutDoor, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.8F);
			if (door.getBlock() instanceof DoorBlock doorBlock) {
				doorBlock.setOpen(this, level, door, hutDoor, true);
			}
			level.sendParticles(ParticleTypes.CLOUD, hutDoor.getX() + 0.5, hutDoor.getY() + 1.0, hutDoor.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.05);
		}
		YagaFx.blink(level, this);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_CELEBRATE, getSoundSource(), 4.0F, 0.6F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_AMBIENT, getSoundSource(), 3.0F, 0.5F);
		if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
			setTarget(player);
		}
	}

	@Override
	public boolean isPickable() {
		return !waiting() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return !waiting() && super.isPushable();
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return !waiting() && super.canBeSeenAsEnemy();
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		BabaYagaModule.track(this);
		if (potionCooldown > 0) potionCooldown--;
		if (spellCooldown > 0) spellCooldown--;
		if (glyphCooldown > 0) glyphCooldown--;
		if (castGap > 0) castGap--;
		if (summonCooldown > 0) summonCooldown--;
		if (escapeCooldown > 0) escapeCooldown--;
		if (eatCooldown > 0) eatCooldown--;
		for (int i = 0; i < attackCooldowns.length; i++) {
			if (attackCooldowns[i] > 0) attackCooldowns[i]--;
		}
		setNoGravity(phase() == 1 && action != YagaAction.SHATTER);
		if (adoptMinions) {
			adoptMinions = false;
			adoptMinions(level);
		}
		tickGroups(level);
		tickSpells(level);
		if (phase() == 2) {
			summonLockout = BabaYagaLogic.summonLockout(aliveMinions(), summonLockout, settings().phaseTwo());
		}

		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || target.level() != level || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target))) {
			setTarget(null);
			target = null;
		}
		tickStorm(level, target);
		tickWaveGroups(level, target);
		if (action != YagaAction.NONE) {
			tickAction(level);
		} else if (target == null) {
			setGlow(YagaGlow.NONE);
			if (returningHome && home != null) {
				tickReturn();
			} else if (phase() == 1) {
				// Nobody to fight: she hangs in the air, bobbing.
				setDeltaMovement(0.0, Mth.sin(tickCount * 0.1F) * 0.02, 0.0);
			} else {
				getNavigation().stop();
			}
		} else if (phase() == 1) {
			tickPhaseOne(level, target);
		} else {
			tickPhaseTwo(level, target);
		}
		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	// ---- The storm ----

	/**
	 * Her thunderstorm: it starts with her first fight and lasts until she has seen no player for
	 * {@link BabaYagaSettings.Storm#lostTicks()}; then she lets her target go and goes back where she started. Only players
	 * within {@link BabaYagaSettings.Storm#radius()} of her see its rain (the module tells them), and lightning flashes
	 * strike around her. The world's own weather is left alone.
	 */
	private void tickStorm(ServerLevel level, @Nullable LivingEntity target) {
		if (home == null) {
			home = blockPosition();
		}
		if (legacyWeather != null) {
			restoreLegacyWeather(level, legacyWeather);
			legacyWeather = null;
		}
		BabaYagaSettings.Storm storm = settings().storm();
		if (tickCount % 20 == 0) {
			unseenTicks = seesPlayer(level) ? 0 : unseenTicks + 20;
		}
		if (unseenTicks >= storm.lostTicks()) {
			if (stormActive) {
				stormActive = false;
				returningHome = !atHome(2.0);
			}
			// Players she does not see are no longer hers to chase.
			if (target != null && !hasLineOfSight(target)) {
				setTarget(null);
				target = null;
			}
		}
		if (target != null) {
			stormActive = true;
			returningHome = false;
		}
		if (stormActive && level.canHaveWeather() && random.nextInt(storm.lightningOdds()) == 0) {
			strikeLightning(level, storm.lightningReach());
		}
	}

	/** Whether she sees a player she could fight, within her follow range. */
	private boolean seesPlayer(ServerLevel level) {
		double range = getAttributeValue(Attributes.FOLLOW_RANGE);
		for (Player player : level.players()) {
			if (player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && distanceToSqr(player) <= range * range
					&& hasLineOfSight(player)) {
				return true;
			}
		}
		return false;
	}

	/** A lightning flash and thunderclap somewhere around her; it neither burns nor hurts. */
	private void strikeLightning(ServerLevel level, double reach) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = Math.sqrt(random.nextDouble()) * reach;
		int x = Mth.floor(getX() + Math.cos(angle) * distance);
		int z = Mth.floor(getZ() + Math.sin(angle) * distance);
		if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
			return;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(x + 0.5, y, z + 0.5);
			level.addFreshEntity(bolt);
		}
	}

	/** Whether her storm is up: the module shows its rain to the players near her. */
	public boolean stormActive() {
		return stormActive && isAlive() && !isRemoved();
	}

	private boolean atHome(double within) {
		return home == null || position().distanceToSqr(Vec3.atBottomCenterOf(home)) <= within * within;
	}

	/** Nobody to fight and she lost the players: back to where she started, flying or on foot. */
	private void tickReturn() {
		Vec3 to = Vec3.atBottomCenterOf(home);
		if (atHome(phase() == 1 ? 0.75 : 1.5)) {
			returningHome = false;
			getNavigation().stop();
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		if (phase() == 1) {
			flyTowards(to, settings().flight().speed());
			Vec3 delta = to.subtract(position());
			float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(Mth.rotateIfNecessary(getYRot(), yaw, 30.0F));
			setYBodyRot(getYRot());
			setYHeadRot(getYRot());
		} else if (tickCount % 20 == 0 || getNavigation().isDone()) {
			getNavigation().moveTo(to.x, to.y, to.z, 1.0);
		}
	}

	/** A save from before her storm was her own may have left the world thundering: the weather from before goes back. */
	private static void restoreLegacyWeather(ServerLevel level, LegacyWeather saved) {
		WeatherData weather = level.getServer().getWeatherData();
		weather.setClearWeatherTime(saved.clear());
		weather.setRainTime(saved.rain());
		weather.setThunderTime(saved.thunder());
		weather.setRaining(saved.raining());
		weather.setThundering(saved.thundering());
	}

	// ---- Phase one ----

	private void tickPhaseOne(ServerLevel level, LivingEntity target) {
		BabaYagaSettings settings = settings();
		setGlow(YagaGlow.NONE);
		orbitAngle = (orbitAngle + settings.flight().orbitTurn()) % 360.0F;
		flyTowards(orbitPoint(target, settings.flight().orbitRadius()), settings.flight().speed());
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);

		boolean waveOut = nextWave < 0;
		if (!waveOut) {
			if (--nextWave <= 0) {
				startAction(YagaAction.SUMMON, target);
			}
			return;
		}
		if (aliveMinions() == 0 && laterParts.isEmpty()) {
			if (++clearedTicks >= settings.swoop().delay()) {
				startAction(YagaAction.SWOOP, target);
			}
			return;
		}
		clearedTicks = 0;
		if (castGap > 0) {
			return;
		}
		double distance = distanceTo(target);
		boolean sees = getSensing().hasLineOfSight(target);
		if (glyphCooldown <= 0 && sees) {
			startAction(YagaAction.GLYPH, target);
		} else if (spellCooldown <= 0 && groupToBuff(target) >= 0) {
			startAction(YagaAction.SPELL, target);
		} else if (potionCooldown <= 0 && sees && distance <= settings.potion().range()) {
			startAction(YagaAction.THROW, target);
		}
	}

	// ---- Phase two ----

	private void tickPhaseTwo(ServerLevel level, LivingEntity target) {
		BabaYagaSettings settings = settings();
		setGlow(YagaGlow.NONE);
		if (escapeRequested) {
			escapeRequested = false;
			startAction(YagaAction.ESCAPE, target);
			return;
		}
		idleTicks++;
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		keepDistance(target, settings.phaseTwo());
		if (idleTicks < settings.phaseTwo().attackPause()) {
			return;
		}
		Mob meal = nearestMinion(settings.eat().range());
		if (BabaYagaLogic.eats(getHealth(), eatCooldown, meal != null, settings.eat())) {
			eatTarget = meal;
			startAction(YagaAction.EAT_RUN, target);
			return;
		}
		if (summonCooldown <= 0 && BabaYagaLogic.maySummon(aliveMinions(), summonLockout, settings.phaseTwo())) {
			startAction(YagaAction.SUMMON, target);
			return;
		}
		if (spellCooldown <= 0 && groupToBuff(target) >= 0) {
			startAction(YagaAction.SPELL, target);
			return;
		}
		double distance = horizontalDistanceTo(target);
		boolean sees = getSensing().hasLineOfSight(target);
		Attack attack = BabaYagaLogic.chooseAttack(distance, sees, HagThread.isTied(target), attackCooldowns, settings,
				random::nextDouble);
		if (attack == null) {
			return;
		}
		switch (attack) {
			case ACID -> {
				attackCooldowns[attack.ordinal()] = settings.acid().cooldown();
				startAction(YagaAction.ACID, target);
			}
			case THREADS -> {
				attackCooldowns[attack.ordinal()] = settings.threads().cooldown();
				startAction(YagaAction.THREADS, target);
			}
			case BEAM -> {
				attackCooldowns[attack.ordinal()] = settings.beam().cooldown();
				startAction(YagaAction.BEAM, target);
			}
			case CLAWS -> {
				attackCooldowns[attack.ordinal()] = settings.claws().cooldown();
				startAction(YagaAction.CLAWS, target);
			}
		}
	}

	/**
	 * On foot she stays on the second line: she runs from a player who comes close, steps back from one a bit too near,
	 * walks up to one too far or out of sight, and otherwise stands still behind her creatures.
	 */
	private void keepDistance(LivingEntity target, BabaYagaSettings.PhaseTwo two) {
		double distance = horizontalDistanceTo(target);
		boolean sees = getSensing().hasLineOfSight(target);
		boolean repathNow = --repath <= 0 || getNavigation().isDone();
		if (distance < two.minDistance()) {
			if (repathNow) {
				repath = 10;
				Vec3 away = DefaultRandomPos.getPosAway(this, 12, 6, target.position());
				if (away != null) {
					getNavigation().moveTo(away.x, away.y, away.z, distance < two.fleeDistance() ? two.fleeSpeed() : 1.0);
				}
			}
		} else if (distance > two.maxDistance() || !sees) {
			if (repathNow) {
				repath = 10;
				getNavigation().moveTo(target, 1.0);
			}
		} else {
			getNavigation().stop();
			faceTowards(target);
		}
	}

	// ---- Actions ----

	private void startAction(YagaAction next, LivingEntity target) {
		actionTarget = target;
		setAction(next);
		getNavigation().stop();
	}

	private void setAction(YagaAction next) {
		if (action == YagaAction.CLAWS && next != YagaAction.CLAWS) {
			releaseMinions();
		}
		if (next != YagaAction.ESCAPE) {
			escapeAtOnce = false;
		}
		if (next != YagaAction.BEAM) {
			entityData.set(DATA_BEAM, new Vector3f());
		}
		action = next;
		actionTicks = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		setAction(YagaAction.NONE);
		actionTarget = null;
		idleTicks = 0;
		setGlow(YagaGlow.NONE);
	}

	/** Length of the current action in ticks. */
	private int actionLength() {
		BabaYagaSettings settings = settings();
		return switch (action) {
			case SWOOP -> settings.swoop().strikeTick() + 6;
			case RETREAT -> settings.swoop().retreatTicks();
			case BEAM -> settings.beam().windup() + settings.beam().ticks();
			case CLAWS -> settings.claws().lunge() + (settings.claws().hits() - 1) * settings.claws().gap() + 8;
			case EAT -> settings.eat().ticks() + 6;
			case ESCAPE -> settings.escape().runTicks() + 1;
			default -> action.length();
		};
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= actionLength()) {
			YagaAction ended = action;
			endAction();
			afterAction(ended);
			return;
		}
		boolean needsTarget = action != YagaAction.SHATTER && action != YagaAction.STAGGER && action != YagaAction.RETREAT;
		if (needsTarget && (target == null || !target.isAlive() || target.level() != level)) {
			YagaAction ended = action;
			endAction();
			afterAction(ended);
			return;
		}
		if (phase() == 1 && action != YagaAction.SWOOP && action != YagaAction.RETREAT && action != YagaAction.STAGGER
				&& action != YagaAction.SHATTER && target != null) {
			// Casting in the air, she keeps circling slowly.
			BabaYagaSettings.Flight flight = settings().flight();
			flyTowards(orbitPoint(target, flight.orbitRadius()), flight.speed() * 0.5);
		}
		if (target != null && action != YagaAction.ESCAPE && action != YagaAction.SHATTER) {
			getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (action != YagaAction.STAGGER) {
				faceTowards(target);
			}
		}
		switch (action) {
			case SUMMON -> tickSummon(level, t, target);
			case THROW -> {
				if (t == 0) {
					// A loud cackle and the clink of glass: a flask is coming.
					level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_AMBIENT, getSoundSource(), 3.0F, 0.6F);
					level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BOTTLE_FILL, getSoundSource(), 3.0F, 0.7F);
				}
				if (t == YagaAction.THROW_RELEASE && target != null) {
					throwFlask(level, target);
				}
			}
			case SPELL -> tickSpell(level, t, target);
			case GLYPH -> tickGlyph(level, t, target);
			case SWOOP -> tickSwoop(level, t, target);
			case RETREAT -> tickRetreat(target);
			case STAGGER -> setDeltaMovement(getDeltaMovement().multiply(0.85, phase() == 1 ? 0.85 : 1.0, 0.85));
			case SHATTER -> tickShatter(level, t);
			case ACID, THREADS -> tickBolt(level, t, target);
			case BEAM -> tickBeam(level, t, target);
			case CLAWS -> tickClaws(level, t, target);
			case EAT_RUN -> tickEatRun(target);
			case EAT -> tickEat(level, t);
			case ESCAPE -> tickEscape(level, t, target);
			default -> {
			}
		}
	}

	/** What comes after an action that ran its course. */
	private void afterAction(YagaAction ended) {
		switch (ended) {
			case SWOOP, STAGGER -> {
				if (phase() == 1) {
					setAction(YagaAction.RETREAT);
				}
			}
			case RETREAT -> {
				if (diveLanded) {
					nextWave = NEXT_WAVE_DELAY;
				} else {
					// The dive missed: she comes down again before she raises anyone.
					clearedTicks = 0;
				}
				diveLanded = false;
			}
			case EAT_RUN -> {
				if (eatTarget != null && eatTarget.isAlive() && distanceTo(eatTarget) <= EAT_REACH + 0.5) {
					setAction(YagaAction.EAT);
				} else {
					eatTarget = null;
				}
			}
			default -> {
			}
		}
	}

	/**
	 * After a reload her creatures still stand about, but she has forgotten them: she gathers them into one group again,
	 * so they glow, wither and count toward her cap, and a phase-one wave still out is not raised a second time.
	 */
	private void adoptMinions(ServerLevel level) {
		Group group = new Group();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(BabaYagaModule.SUN_SHELTER_RADIUS),
				mob -> mob.isAlive() && BabaYagaModule.isMinion(mob)
						&& groups.stream().noneMatch(known -> known.members.contains(mob.getUUID())))) {
			group.add(mob);
		}
		if (group.members.isEmpty()) {
			return;
		}
		group.raisedAt = level.getGameTime();
		group.wave = phase() == 1;
		groups.add(group);
		if (phase() == 1) {
			nextWave = -1;
		}
	}

	// ---- Summoning ----

	private void tickSummon(ServerLevel level, int t, @Nullable LivingEntity target) {
		setGlow(YagaGlow.SUMMON);
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, getSoundSource(), 2.0F, 0.7F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_CELEBRATE, getSoundSource(), 1.5F, 0.7F);
		}
		if (t % 2 == 0) {
			Vec3 tip = staffTip();
			level.sendParticles(new DustParticleOptions(YagaGlow.SUMMON.color, 1.2F), tip.x, tip.y, tip.z, 3, 0.15, 0.15, 0.15, 0.0);
			YagaFx.summonCharge(level, tip);
		}
		if (t != YagaAction.SUMMON_RAISE || target == null) {
			return;
		}
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 2.0F, 0.6F);
		if (phase() == 1) {
			raiseWave(level, target);
		} else {
			raiseInFront(level, target);
		}
	}

	/**
	 * Phase one: every group of the wave is rolled at once, each a good way off on its own side, and rises in sub-waves:
	 * the first of every group at once, the next ones together a while apart in the same spots. Lurker and wight groups
	 * take turns, so a wave of two has one of each. More players in the fight make more groups and sub-waves.
	 */
	private void raiseWave(ServerLevel level, LivingEntity target) {
		BabaYagaSettings settings = settings();
		BabaYagaSettings.Summon summon = settings.summon();
		groups.removeIf(group -> group.alive(level) == 0);
		waveAngle = random.nextFloat() * 360.0F;
		waveFirstKind = random.nextBoolean() ? GroupKind.LURKERS : GroupKind.WIGHTS;
		laterParts.clear();
		List<Integer> shape = BabaYagaLogic.waveShape(playersInFight(level), summon, settings.crowd());
		List<List<List<Creature>>> wave = new ArrayList<>(shape.size());
		for (int g = 0; g < shape.size(); g++) {
			wave.add(BabaYagaLogic.waveParts(BabaYagaLogic.waveGroupKind(g, waveFirstKind), shape.get(g), summon, random::nextDouble));
		}
		wave = BabaYagaLogic.capWights(wave, summon.maxWights());
		for (int g = 0; g < wave.size(); g++) {
			Vec3 center = waveGroupCenter(level, target, g, wave.size());
			if (center == null) {
				continue;
			}
			List<List<Creature>> parts = wave.get(g);
			raiseGroup(level, target, center, parts.get(0));
			for (int p = 1; p < parts.size(); p++) {
				if (!parts.get(p).isEmpty()) {
					laterParts.add(new LaterPart(center, parts.get(p), p));
				}
			}
		}
		laterPartsTimer = summon.halfDelay();
		nextWave = -1;
		clearedTicks = 0;
		glyphCooldown = Math.max(glyphCooldown, settings.glyphs().firstDelay());
		spellCooldown = Math.max(spellCooldown, 60);
		potionCooldown = Math.max(potionCooldown, 40);
	}

	/** Phase one: raises the wave's next sub-waves when their time comes, whatever she is busy with. */
	private void tickWaveGroups(ServerLevel level, @Nullable LivingEntity target) {
		if (laterParts.isEmpty() || phase() != 1 || --laterPartsTimer > 0) {
			return;
		}
		if (target == null) {
			laterPartsTimer = 1;
			return;
		}
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 2.0F, 0.6F);
		Vec3 tip = staffTip();
		level.sendParticles(new DustParticleOptions(YagaGlow.SUMMON.color, 1.4F), tip.x, tip.y, tip.z, 20, 0.3, 0.3, 0.3, 0.0);
		int next = Integer.MAX_VALUE;
		for (LaterPart part : laterParts) {
			next = Math.min(next, part.round());
		}
		for (LaterPart part : laterParts) {
			if (part.round() == next) {
				raiseGroup(level, target, part.center(), part.creatures());
			}
		}
		int raised = next;
		laterParts.removeIf(part -> part.round() == raised);
		laterPartsTimer = settings().summon().halfDelay();
	}

	/** Players in the fight: alive, not in creative or spectator mode, and near her. */
	private int playersInFight(ServerLevel level) {
		double radius = settings().crowd().radius();
		int count = 0;
		for (Player player : level.players()) {
			if (player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && distanceToSqr(player) <= radius * radius) {
				count++;
			}
		}
		return Math.max(1, count);
	}

	private @Nullable Vec3 waveGroupCenter(ServerLevel level, LivingEntity target, int g, int count) {
		BabaYagaSettings.Summon summon = settings().summon();
		double angle = Math.toRadians(waveAngle + 360.0 * g / count + (random.nextDouble() - 0.5) * 50.0);
		Vec3 center = null;
		for (int attempt = 0; attempt < 6 && center == null; attempt++) {
			double distance = Mth.lerp(random.nextDouble(), summon.minRadius(), Math.max(summon.minRadius(), summon.maxRadius()));
			distance *= 1.0 - attempt * 0.12;
			center = ground(level, target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance,
					EamEntities.DROWNED_SOLDIER);
		}
		if (center == null) {
			center = ground(level, target.getX() + Math.cos(angle) * 5.0, target.getY(), target.getZ() + Math.sin(angle) * 5.0,
					EamEntities.DROWNED_SOLDIER);
		}
		return center;
	}

	/** A sub-wave of a wave group still to rise, where, and its turn: 1 for the second sub-wave, 2 for the third. */
	private record LaterPart(Vec3 center, List<Creature> creatures, int round) {
	}

	/** Phase two: one group between her and the player. */
	private void raiseInFront(ServerLevel level, LivingEntity target) {
		BabaYagaSettings settings = settings();
		groups.removeIf(group -> group.alive(level) == 0);
		Vec3 toward = horizontal(target.position().subtract(position()));
		Vec3 spot = position().add(toward.scale(Math.min(5.0, horizontalDistanceTo(target) * 0.5)));
		Vec3 center = ground(level, spot.x, getY(), spot.z, EamEntities.DROWNED_SOLDIER);
		int size = BabaYagaLogic.underCap(BabaYagaLogic.groupSize(settings.phaseTwo().minSize(), settings.phaseTwo().maxSize(),
				random::nextDouble), aliveMinions(), settings.phaseTwo());
		if (center != null && size > 0) {
			raiseGroup(level, target, center, size);
		}
		summonCooldown = settings.phaseTwo().summonCooldown();
		summonLockout = BabaYagaLogic.summonLockout(aliveMinions(), summonLockout, settings.phaseTwo());
	}

	/** A few creatures right by the player, after her escape. */
	private void raiseNear(ServerLevel level, LivingEntity target, int count) {
		Vec3 center = null;
		for (int attempt = 0; attempt < 6 && center == null; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			center = ground(level, target.getX() + Math.cos(angle) * 4.0, target.getY(), target.getZ() + Math.sin(angle) * 4.0,
					EamEntities.DROWNED_SOLDIER);
		}
		if (center != null && count > 0) {
			raiseGroup(level, target, center, count);
		}
	}

	/**
	 * A group of {@code size} creatures mixed by the weights, as in phase two: no more lurkers or wights than her phase-two
	 * limits, counting those of hers already alive.
	 */
	private void raiseGroup(ServerLevel level, LivingEntity target, Vec3 center, int size) {
		BabaYagaSettings.Summon summon = settings().summon();
		BabaYagaSettings.PhaseTwo two = settings().phaseTwo();
		List<Creature> creatures = new ArrayList<>(size);
		int lurkers = aliveMinions(EamEntities.SWAMP_LURKER);
		int wights = aliveMinions(EamEntities.WIGHT);
		for (int i = 0; i < size; i++) {
			Creature creature = BabaYagaLogic.creature(summon.lurkerWeight(), summon.wightWeight(), summon.soldierWeight(), lurkers,
					two.maxLurkers(), wights, two.maxWights(), random::nextDouble);
			if (creature == Creature.LURKER) {
				lurkers++;
			} else if (creature == Creature.WIGHT) {
				wights++;
			}
			creatures.add(creature);
		}
		raiseGroup(level, target, center, creatures);
	}

	private void raiseGroup(ServerLevel level, LivingEntity target, Vec3 center, List<Creature> creatures) {
		Group group = new Group();
		int players = playersInFight(level);
		for (Creature creature : creatures) {
			EntityType<? extends Mob> type = switch (creature) {
				case LURKER -> EamEntities.SWAMP_LURKER;
				case WIGHT -> EamEntities.WIGHT;
				case SOLDIER -> EamEntities.DROWNED_SOLDIER;
			};
			double angle = random.nextDouble() * Math.PI * 2.0;
			double spread = 0.8 + random.nextDouble() * 1.8;
			Vec3 at = ground(level, center.x + Math.cos(angle) * spread, center.y, center.z + Math.sin(angle) * spread, type);
			Mob mob = raise(level, type, at != null ? at : center, target, BabaYagaLogic.healthMultiplier(creature, players,
					settings().summon(), settings().crowd()));
			if (mob != null) {
				group.add(mob);
			}
		}
		if (!group.members.isEmpty()) {
			group.raisedAt = level.getGameTime();
			group.wave = phase() == 1;
			groups.add(group);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.MUD_BREAK, getSoundSource(), 2.0F, 0.6F);
			level.playSound(null, center.x, center.y, center.z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 2.0F, 0.6F);
		}
	}

	/** One creature, risen out of the ground: it is hers, knows where the player is and goes for them at once. */
	private @Nullable Mob raise(ServerLevel level, EntityType<? extends Mob> type, Vec3 at, LivingEntity target, double healthMultiplier) {
		Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (mob == null) {
			return null;
		}
		mob.snapTo(at.x, at.y, at.z, random.nextFloat() * 360.0F, 0.0F);
		// Tagged first: a wight raised by her never cloaks, a lurker never hides.
		mob.addTag(BabaYagaModule.MINION_TAG);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.MOB_SUMMONED, null);
		// Her creatures may be weaker than their wild kin, but rise at full health.
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			if (healthMultiplier != 1.0) {
				health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER, healthMultiplier - 1.0,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (follow != null) {
			follow.addOrReplacePermanentModifier(new AttributeModifier(FOLLOW_MODIFIER,
					Math.max(0.0, settings().summon().followRange() - follow.getBaseValue()), AttributeModifier.Operation.ADD_VALUE));
		}
		mob.setTarget(target);
		level.addFreshEntityWithPassengers(mob);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), at.x, at.y + 0.2, at.z, 20, 0.4,
				0.1, 0.4, 0.1);
		level.sendParticles(new DustParticleOptions(YagaGlow.SUMMON.color, 1.3F), at.x, at.y + 1.0, at.z, 12, 0.3, 0.6, 0.3, 0.0);
		level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 4, 0.3, 0.4, 0.3, 0.02);
		YagaFx.graveRise(level, at);
		return mob;
	}

	/**
	 * A spot to stand on near {@code (x, y, z)}: searched up and down a few blocks for solid ground with room above it.
	 * Null if there is none.
	 */
	private static @Nullable Vec3 ground(ServerLevel level, double x, double y, double z, EntityType<?> type) {
		BlockPos start = BlockPos.containing(x, y, z);
		for (int i = 0; i <= 16; i++) {
			int dy = (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2);
			BlockPos pos = start.offset(0, dy, 0);
			BlockPos below = pos.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || level.getBlockState(pos).getFluidState().is(
					net.minecraft.tags.FluidTags.LAVA)) {
				continue;
			}
			Vec3 at = new Vec3(x, pos.getY(), z);
			if (level.noCollision(type.getSpawnAABB(at.x, at.y, at.z))) {
				return at;
			}
		}
		return null;
	}

	// ---- Her creatures ----

	/** Prunes dead creatures, notes which groups the player hurt, and shows each spell over the creatures that carry it. */
	private void tickGroups(ServerLevel level) {
		if (tickCount % 5 != 0) {
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Group> it = groups.iterator(); it.hasNext(); ) {
			Group group = it.next();
			if (group.update(level, now) == 0) {
				it.remove();
			} else if (group.wave) {
				outlive(level, group, now);
			}
		}
		showSpells(level);
	}

	/**
	 * Phase one: a wave lingers no longer than she allows. A group still alive a while after it rose starts to glow, and
	 * a while later its creatures wither away over a few seconds, so the next wave is not held up forever.
	 */
	private void outlive(ServerLevel level, Group group, long now) {
		BabaYagaSettings.Summon summon = settings().summon();
		long age = now - group.raisedAt;
		if (age < summon.glowAfter()) {
			return;
		}
		boolean fading = age >= summon.fadeAfter();
		for (Mob mob : group.living) {
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
			if (!fading) {
				continue;
			}
			if (age >= summon.fadeAfter() + summon.fadeTicks()) {
				mob.kill(level);
				continue;
			}
			// Every update takes its share of the full health; absorption goes first.
			mob.setAbsorptionAmount(0.0F);
			mob.setHealth(Math.max(0.5F, mob.getHealth() - mob.getMaxHealth() * 5.0F / summon.fadeTicks()));
			level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 3, 0.25, 0.3, 0.25, 0.01);
			level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 1, 0.2, 0.3, 0.2, 0.01);
		}
	}

	private int aliveMinions() {
		int alive = 0;
		for (Group group : groups) {
			alive += group.living.size();
		}
		return alive;
	}

	private int aliveMinions(EntityType<?> type) {
		int alive = 0;
		for (Group group : groups) {
			for (Mob mob : group.living) {
				if (mob.getType() == type) {
					alive++;
				}
			}
		}
		return alive;
	}

	private @Nullable Mob nearestMinion(double range) {
		Mob best = null;
		for (Group group : groups) {
			for (Mob mob : group.living) {
				if (mob.isAlive() && distanceTo(mob) <= range && (best == null || distanceTo(mob) < distanceTo(best))) {
					best = mob;
				}
			}
		}
		return best;
	}

	/**
	 * A ring of icons circling over the head of each of her creatures for each of her spells it carries: yellow swords
	 * for strength, red hearts for absorption, pale blue bolts for speed. Each icon lives a few ticks sliding along the
	 * ring, and the next one is sent where it leaves off.
	 */
	private void showSpells(ServerLevel level) {
		double spin = tickCount * ICON_TURN;
		for (Group group : groups) {
			for (Mob mob : group.living) {
				int ring = 0;
				for (Buff buff : Buff.values()) {
					if (!mob.hasEffect(effectOf(buff))) {
						continue;
					}
					SimpleParticleType icon = iconOf(buff);
					double y = mob.getY() + mob.getBbHeight() + 0.4 + ring * 0.32;
					double radius = Math.max(0.4, mob.getBbWidth() * 0.6);
					// Rings over one another turn opposite ways; the icons keep up with the creature as it moves.
					double mx = mob.getX() - mob.xo;
					double mz = mob.getZ() - mob.zo;
					double turn = ring % 2 == 0 ? ICON_TURN : -ICON_TURN;
					for (int i = 0; i < ICONS; i++) {
						double angle = (ring % 2 == 0 ? spin : -spin) + i * Math.PI * 2.0 / ICONS;
						double x = mob.getX() + Math.cos(angle) * radius;
						double z = mob.getZ() + Math.sin(angle) * radius;
						// Count 0: the "velocity" is the icon's step along the ring each tick.
						level.sendParticles(icon, x, y, z, 0, mx - Math.sin(angle) * radius * turn, 0.0,
								mz + Math.cos(angle) * radius * turn, 1.0);
					}
					ring++;
				}
			}
		}
	}

	private static SimpleParticleType iconOf(Buff buff) {
		return switch (buff) {
			case ABSORPTION -> EamParticles.YAGA_ABSORPTION;
			case SPEED -> EamParticles.YAGA_SPEED;
			case STRENGTH -> EamParticles.YAGA_STRENGTH;
		};
	}

	private static Holder<MobEffect> effectOf(Buff buff) {
		return switch (buff) {
			case ABSORPTION -> MobEffects.ABSORPTION;
			case SPEED -> MobEffects.SPEED;
			case STRENGTH -> MobEffects.STRENGTH;
		};
	}

	private int levelOf(Buff buff) {
		BabaYagaSettings.Spell spell = settings().spell();
		return switch (buff) {
			case ABSORPTION -> spell.absorptionLevel();
			case SPEED -> spell.speedLevel();
			case STRENGTH -> spell.strengthLevel();
		};
	}

	/** The group her next spell goes to, or -1. */
	private int groupToBuff(LivingEntity target) {
		BabaYagaSettings.Spell spell = settings().spell();
		long now = level().getGameTime();
		int n = groups.size();
		int[] alive = new int[n];
		double[] distance = new double[n];
		boolean[] hurt = new boolean[n];
		boolean[] has = new boolean[n];
		for (int i = 0; i < n; i++) {
			Group group = groups.get(i);
			alive[i] = group.living.size();
			if (alive[i] == 0) {
				continue;
			}
			distance[i] = group.center().distanceTo(target.position());
			hurt[i] = now - group.lastHurt <= spell.hurtMemory();
			Buff buff = BabaYagaLogic.buffFor(distance[i], hurt[i], spell);
			has[i] = group.living.getFirst().hasEffect(effectOf(buff));
		}
		return BabaYagaLogic.groupToBuff(alive, distance, hurt, has);
	}

	// ---- Spells ----

	private void tickSpell(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		int index = groupToBuff(target);
		if (index < 0) {
			// Nobody left worth the spell.
			endAction();
			return;
		}
		Group group = groups.get(index);
		BabaYagaSettings.Spell spell = settings().spell();
		Buff buff = BabaYagaLogic.buffFor(group.center().distanceTo(target.position()), level.getGameTime() - group.lastHurt <= spell.hurtMemory(),
				spell);
		setGlow(YagaGlow.of(buff));
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, getSoundSource(), 1.5F, 0.8F);
		}
		if (t < YagaAction.SPELL_RELEASE && t % 2 == 0) {
			YagaFx.runes(level, staffTip(), YagaGlow.of(buff).color, 1, 0.3);
		}
		if (t == YagaAction.SPELL_RELEASE) {
			spells.add(new SpellBolt(staffTip(), group, buff, spell.flightTicks()));
			spellCooldown = spell.cooldown();
			castGap = CAST_GAP;
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.5F, 1.2F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, getSoundSource(), 2.0F, 0.6F);
		}
	}

	/** Moves her spells along their arcs, a trail of sparks; on arrival the group gets the effect. */
	private void tickSpells(ServerLevel level) {
		for (Iterator<SpellBolt> it = spells.iterator(); it.hasNext(); ) {
			SpellBolt bolt = it.next();
			bolt.age++;
			Vec3 to = bolt.group.living.isEmpty() ? bolt.lastTo : bolt.group.center().add(0.0, 1.0, 0.0);
			bolt.lastTo = to;
			double s = Math.min(1.0, bolt.age / (double) bolt.ticks);
			double height = Math.max(2.0, bolt.from.distanceTo(to) * 0.25);
			Vec3 at = bolt.from.lerp(to, s).add(0.0, BabaYagaLogic.arc(s, height), 0.0);
			int color = YagaGlow.of(bolt.buff).color;
			level.sendParticles(new DustParticleOptions(color, 1.6F), at.x, at.y, at.z, 3, 0.08, 0.08, 0.08, 0.0);
			YagaFx.runes(level, at, color, 2, 0.12);
			if (bolt.age < bolt.ticks) {
				continue;
			}
			it.remove();
			BabaYagaSettings.Spell spell = settings().spell();
			for (Mob mob : bolt.group.living) {
				if (mob.isAlive()) {
					mob.addEffect(new MobEffectInstance(effectOf(bolt.buff), spell.duration(), levelOf(bolt.buff)), this);
				}
			}
			level.sendParticles(new DustParticleOptions(color, 2.0F), to.x, to.y, to.z, 30, 1.2, 0.6, 1.2, 0.0);
			YagaFx.runeRing(level, to, color);
			level.playSound(null, to.x, to.y, to.z, SoundEvents.BREWING_STAND_BREW, getSoundSource(), 1.5F, 0.8F);
		}
	}

	// ---- Flasks ----

	private void throwFlask(ServerLevel level, LivingEntity target) {
		BabaYagaSettings.Potion potion = settings().potion();
		HagPotion flask = new HagPotion(level, this);
		Vec3 from = position().add(0.0, getBbHeight() * 0.85, 0.0);
		flask.setPos(from);
		// At where the player stands now, in a slow arc: whoever moves off in time is not caught.
		double flat = horizontalDistanceTo(target);
		int ticks = Mth.clamp((int) (flat / 0.5), potion.flightTicks(), potion.flightTicks() + 20);
		Vec3 aim = target.position();
		double dx = aim.x - from.x;
		double dy = aim.y - from.y;
		double dz = aim.z - from.z;
		double g = 0.05;
		// Air drag slows it a little each tick; the throw makes up for it.
		double drag = Math.pow(0.99, ticks * 0.5);
		flask.setDeltaMovement(dx / ticks / drag, (dy + 0.5 * g * ticks * ticks) / ticks / drag, dz / ticks / drag);
		level.addFreshEntity(flask);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, getSoundSource(), 3.0F, 0.8F);
		// The spot it will burst on shows as a ring of green mist on the ground.
		DustParticleOptions mist = new DustParticleOptions(HagPotion.COLOR, 1.2F);
		for (int i = 0; i < 24; i++) {
			double angle = i * Math.PI * 2.0 / 24.0;
			level.sendParticles(mist, aim.x + Math.cos(angle) * potion.radius(), aim.y + 0.1, aim.z + Math.sin(angle) * potion.radius(), 1, 0.05,
					0.05, 0.05, 0.0);
		}
		potionCooldown = potion.cooldown();
		castGap = CAST_GAP;
	}

	// ---- Glyphs ----

	private void tickGlyph(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		if (t == 0) {
			Glyph kind = Glyph.values()[random.nextInt(3)];
			if (kind == lastGlyph) {
				kind = Glyph.values()[(kind.ordinal() + 1 + random.nextInt(2)) % 3];
			}
			pendingGlyph = kind.ordinal();
			level.addFreshEntity(HagGlyph.preview(level, this, kind));
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, getSoundSource(), 2.0F, 0.6F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, getSoundSource(), 2.0F, 0.5F);
		}
		if (pendingGlyph < 0) {
			return;
		}
		Glyph kind = Glyph.values()[pendingGlyph];
		setGlow(YagaGlow.of(kind));
		if (t != YagaAction.GLYPH_PLACE) {
			return;
		}
		BabaYagaSettings.Glyphs glyphs = settings().glyphs();
		Vec3 at = ground(level, target.getX(), target.getY(), target.getZ(), EamEntities.HAG_GLYPH);
		if (at == null) {
			at = target.position();
		}
		boolean harm = kind == Glyph.HARM;
		HagGlyph glyph = HagGlyph.create(level, this, kind, harm ? glyphs.damageRadius() : glyphs.radius(),
				harm ? glyphs.damageDelay() : glyphs.duration());
		glyph.setInactiveTicks(glyphs.inactiveTicks());
		glyph.setPos(at.x, at.y, at.z);
		level.addFreshEntity(glyph);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 2.0F, harm ? 0.5F : 0.8F);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.ENCHANTMENT_TABLE_USE, getSoundSource(), 2.0F, 0.5F);
		lastGlyph = kind;
		pendingGlyph = -1;
		glyphCooldown = glyphs.cooldown();
		castGap = CAST_GAP;
	}

	// ---- The dive ----

	/**
	 * She dives at the player, timed to reach them on the strike tick. The parry window is the last few ticks before it:
	 * she glows exactly while a hit parries (a repeat parry's shorter window included), and a bell rings as it opens. The
	 * dive aims her low, and her staff reaches below her feet, so it strikes a player standing on the ground.
	 */
	private void tickSwoop(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		BabaYagaSettings.Swoop swoop = settings().swoop();
		int strike = swoop.strikeTick();
		if (t == 0) {
			diveLanded = false;
			// A shriek as the dive starts, well before the parry window: the staff is coming.
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GHAST_WARN, getSoundSource(), 3.0F, 0.7F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_CELEBRATE, getSoundSource(), 2.0F, 0.6F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.PHANTOM_SWOOP, getSoundSource(), 2.0F, 0.7F);
		}
		int parryWindow = parryWindow(level, swoop.parryWindow(), target);
		boolean window = BabaYagaLogic.parries(t, strike, parryWindow);
		setGlow(window ? YagaGlow.PARRY : YagaGlow.NONE);
		if (t == strike - parryWindow) {
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 2.0F, 1.1F);
		}
		if (t < strike) {
			// Aimed so her staff, not her mortar, meets the player: at their chest, a little short of them.
			Vec3 away = horizontal(position().subtract(target.position()));
			Vec3 aim = target.position().add(away.scale(swoop.reach() * 0.6))
					.add(0.0, target.getBbHeight() * 0.6 - getBbHeight() * 0.7 - swoop.strikeDrop(), 0.0);
			double distance = aim.distanceTo(position());
			Vec3 to = aim.subtract(position());
			double speed = BabaYagaLogic.closingSpeed(distance, strike - t, swoop.speed());
			setDeltaMovement(distance > 1.0E-4 ? to.scale(speed / distance) : Vec3.ZERO);
			YagaFx.diveTrail(level, this);
			if (window && t % 2 == 0) {
				level.sendParticles(new DustParticleOptions(YagaGlow.PARRY.color, 1.2F), getX(), getY(0.6), getZ(), 4, 0.4, 0.5, 0.4, 0.0);
			}
			return;
		}
		setDeltaMovement(getDeltaMovement().scale(0.5));
		if (t == strike) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, getSoundSource(), 1.5F, 0.7F);
			YagaFx.diveStrike(level, this);
			if (position().subtract(0.0, swoop.strikeDrop(), 0.0).distanceTo(target.position()) <= swoop.reach() + target.getBbWidth() * 0.5 + 0.5) {
				diveLanded = true;
				target.damageCooldownTime = 0;
				if (target.hurtServer(level, damageSources().mobAttack(this), swoop.damage())) {
					Vec3 push = horizontal(target.position().subtract(position()));
					target.push(push.x * 0.6, 0.3, push.z * 0.6);
				}
			}
		}
	}

	/** Back up to her circle after the dive. */
	private void tickRetreat(@Nullable LivingEntity target) {
		setGlow(YagaGlow.NONE);
		BabaYagaSettings.Flight flight = settings().flight();
		LivingEntity around = target != null ? target : getTarget();
		if (around == null) {
			setDeltaMovement(getDeltaMovement().scale(0.8).add(0.0, 0.05, 0.0));
			return;
		}
		flyTowards(orbitPoint(around, flight.orbitRadius()), flight.speed() * 1.4);
	}

	// ---- Phase change ----

	/** Her mortar bursts, she drops to the ground and gets up on foot at full health: phase two. */
	private void startShatter(ServerLevel level) {
		entityData.set(DATA_PHASE, (byte) 2);
		refreshDimensions();
		setNoGravity(false);
		applyStats(true);
		bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
		bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
		bossEvent.setProgress(1.0F);
		spells.clear();
		laterParts.clear();
		LivingEntity target = getTarget();
		setAction(YagaAction.SHATTER);
		actionTarget = target;
		setGlow(YagaGlow.NONE);
		setDeltaMovement(0.0, 0.1, 0.0);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, getSoundSource(), 2.0F, 0.6F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_HURT, getSoundSource(), 2.0F, 0.6F);
		BlockParticleOption planks = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		level.sendParticles(planks, getX(), getY() + 0.6, getZ(), 120, 0.6, 0.5, 0.6, 0.3);
		level.sendParticles(MORTAR_DUST, getX(), getY() + 0.6, getZ(), 40, 0.8, 0.5, 0.8, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 20, 0.6, 0.4, 0.6, 0.05);
		// Phase two starts with her group raised at once; her casts wait a moment.
		summonCooldown = 0;
		idleTicks = 0;
		escapeCooldown = 60;
		eatCooldown = Math.max(eatCooldown, 200);
		for (Attack attack : Attack.values()) {
			attackCooldowns[attack.ordinal()] = Math.max(attackCooldowns[attack.ordinal()], 60);
		}
	}

	/** She falls; a puff of dust where she lands. */
	private void tickShatter(ServerLevel level, int t) {
		if (onGround() && t % 6 == 0 && t < 24) {
			level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 6, 0.4, 0.0, 0.4, 0.02);
		}
	}

	// ---- Phase two attacks ----

	/** An acid or purple bolt from her staff, straight at where the player stands. */
	private void tickBolt(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		boolean acid = action == YagaAction.ACID;
		setGlow(acid ? YagaGlow.ACID : YagaGlow.THREADS);
		if (t == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), acid ? SoundEvents.EVOKER_PREPARE_ATTACK : SoundEvents.ILLUSIONER_PREPARE_MIRROR,
					getSoundSource(), 1.5F, acid ? 1.3F : 0.8F);
		}
		if (t < YagaAction.BOLT_RELEASE && t % 2 == 0) {
			Vec3 tip = staffTip();
			level.sendParticles(new DustParticleOptions(glow().color, 1.0F), tip.x, tip.y, tip.z, 2, 0.1, 0.1, 0.1, 0.0);
			if (acid) {
				YagaFx.acidCharge(level, tip);
			} else {
				YagaFx.threadCharge(level, tip);
			}
		}
		if (t != YagaAction.BOLT_RELEASE) {
			return;
		}
		Vec3 from = staffTip();
		Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
		double speed = acid ? settings().acid().speed() : settings().threads().speed();
		Vec3 velocity = to.subtract(from).normalize().scale(speed);
		level.addFreshEntity(new HagBolt(level, this, acid ? HagBolt.Kind.ACID : HagBolt.Kind.THREADS, from, velocity));
		level.playSound(null, getX(), getEyeY(), getZ(), acid ? SoundEvents.BLAZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL, getSoundSource(), 1.5F,
				acid ? 1.6F : 0.7F);
		if (!acid) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.SPIDER_AMBIENT, getSoundSource(), 1.5F, 0.6F);
		}
	}

	/**
	 * The fire beam: she aims for a moment, then a beam from her staff burns for a while, turning after the player only so
	 * fast. It goes through armor and sets them alight; a shield held towards her blocks it.
	 */
	private void tickBeam(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		BabaYagaSettings.Beam beam = settings().beam();
		setGlow(YagaGlow.BEAM);
		Vec3 from = staffTip();
		Vec3 wanted = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(from).normalize();
		if (t == 0 || beamAim.lengthSqr() < 1.0E-6) {
			beamAim = wanted;
		} else {
			beamAim = turnTowards(beamAim, wanted, beam.turn());
		}
		if (t == 0) {
			// A loud cue as she starts to aim: the beam is coming, get behind cover or raise a shield.
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BEACON_ACTIVATE, getSoundSource(), 3.0F, 0.7F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, getSoundSource(), 3.0F, 0.6F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BLAZE_AMBIENT, getSoundSource(), 1.5F, 0.6F);
		}
		if (t < beam.windup()) {
			level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 2, 0.08, 0.08, 0.08, 0.01);
			YagaFx.emberCharge(level, from, random);
			return;
		}
		if (t == beam.windup()) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.FIRECHARGE_USE, getSoundSource(), 2.0F, 0.6F);
		}
		if ((t - beam.windup()) % 10 == 0) {
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.BLAZE_BURN, getSoundSource(), 1.5F, 0.7F);
		}
		Vec3 end = from.add(beamAim.scale(beam.range()));
		var blockHit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		end = blockHit.getLocation();
		// The beam itself is drawn by the client like a beacon's; a few flames flicker along it.
		entityData.set(DATA_BEAM, end.subtract(position()).toVector3f());
		double length = from.distanceTo(end);
		for (double d = 0.5; d < length; d += 1.5) {
			if (random.nextInt(4) == 0) {
				Vec3 at = from.add(beamAim.scale(d));
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.005);
			} else if (random.nextInt(3) == 0) {
				YagaFx.emberAlong(level, from.add(beamAim.scale(d)));
			}
		}
		// Where it hits: fire, embers and smoke.
		Vec3 back = end.subtract(beamAim.scale(0.15));
		level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z, 6, 0.2, 0.2, 0.2, 0.04);
		level.sendParticles(ParticleTypes.SMALL_FLAME, back.x, back.y, back.z, 4, 0.25, 0.25, 0.25, 0.06);
		level.sendParticles(ParticleTypes.LAVA, back.x, back.y, back.z, 2, 0.15, 0.15, 0.15, 0.0);
		YagaFx.emberHit(level, back);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, back.y + 0.2, back.z, 2, 0.2, 0.2, 0.2, 0.02);
		if (t % 3 == 0) {
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, back.x, back.y + 0.3, back.z, 1, 0.1, 0.1, 0.1, 0.01);
		}
		if ((t - beam.windup()) % BEAM_EVERY != 0) {
			return;
		}
		Vec3 stop = end;
		for (Player player : level.getEntitiesOfClass(Player.class, new AABB(from, stop).inflate(1.0),
				p -> p.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p))) {
			if (player.getBoundingBox().inflate(0.3).clip(from, stop).isEmpty()) {
				continue;
			}
			if (player.isBlocking() && FallenLordLogic.shieldCovers(player.getYHeadRot(), getX() - player.getX(), getZ() - player.getZ())) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(), getSoundSource(), 0.8F, 1.3F);
				continue;
			}
			player.damageCooldownTime = 0;
			player.hurtServer(level, damageSources().source(BabaYagaModule.BEAM_DAMAGE, this), BabaYagaLogic.beamTickDamage(beam, BEAM_EVERY));
			player.igniteForTicks(beam.fireTicks());
		}
	}

	/** {@code from} turned towards {@code to} by at most {@code degrees}. */
	private static Vec3 turnTowards(Vec3 from, Vec3 to, float degrees) {
		double cos = Mth.clamp(from.dot(to), -1.0, 1.0);
		double angle = Math.acos(cos);
		double max = Math.toRadians(degrees);
		if (angle <= max || angle < 1.0E-5) {
			return to;
		}
		double s = max / angle;
		return from.scale(1.0 - s).add(to.scale(s)).normalize();
	}

	/**
	 * The claws: she blinds the player, blinks behind their back and lunges, a run of rakes that knock a raised shield
	 * out with the first. A hit just before the first rake parries her.
	 */
	private void tickClaws(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		BabaYagaSettings.Claws claws = settings().claws();
		if (t == 0) {
			target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, claws.blindTicks(), 0), this);
			holdMinions();
			Vec3 spot = null;
			for (int attempt = 0; attempt < 6 && spot == null; attempt++) {
				float turn = (random.nextFloat() * 2.0F - 1.0F) * claws.angle();
				double[] offset = BabaYagaLogic.behind(target.getYHeadRot(), turn, claws.distance() * (1.0 - attempt * 0.1));
				spot = ground(level, target.getX() + offset[0], target.getY(), target.getZ() + offset[1], getType());
			}
			if (spot == null) {
				endAction();
				return;
			}
			blink(level, spot);
			if (claws.glowTicks() > 0) {
				addEffect(new MobEffectInstance(MobEffects.GLOWING, claws.glowTicks(), 0, false, false));
			}
			// A screech from behind the player's back as she lands: the claws are coming, turn around.
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.FOX_SCREECH, getSoundSource(), 3.0F, 0.7F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_CELEBRATE, getSoundSource(), 2.0F, 1.2F);
		}
		int lunge = claws.lunge();
		int parryWindow = parryWindow(level, claws.parryWindow(), target);
		boolean window = BabaYagaLogic.parries(t, lunge, parryWindow);
		setGlow(window ? YagaGlow.PARRY : YagaGlow.NONE);
		if (t == lunge - parryWindow) {
			level.playSound(null, getX(), getEyeY(), getZ(), EamSounds.FALLEN_LORD_PARRY_WINDOW, getSoundSource(), 2.0F, 1.2F);
		}
		double distance = horizontalDistanceTo(target);
		Vec3 to = horizontal(target.position().subtract(position()));
		if (t < lunge) {
			double speed = BabaYagaLogic.closingSpeed(distance - 1.3, lunge - t, 1.2);
			setDeltaMovement(to.x * speed, getDeltaMovement().y, to.z * speed);
			return;
		}
		int since = t - lunge;
		if (distance > 1.6) {
			setDeltaMovement(to.x * 0.4, getDeltaMovement().y, to.z * 0.4);
		}
		if (since % claws.gap() != 0 || since / claws.gap() >= claws.hits()) {
			return;
		}
		boolean first = since == 0;
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.3F + since * 0.02F);
		if (distance > CLAW_REACH + target.getBbWidth() * 0.5 || Math.abs(target.getY() - getY()) > 2.5) {
			YagaFx.clawRake(level, this, target, false);
			return;
		}
		ItemStack shield = target.getItemBlockingWith();
		target.damageCooldownTime = 0;
		boolean hurt = target.hurtServer(level, damageSources().mobAttack(this), claws.damage());
		YagaFx.clawRake(level, this, target, hurt);
		if (first && shield != null) {
			BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
			if (blocks != null) {
				blocks.disable(level, target, claws.shieldSeconds(), shield);
				level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK.value(), getSoundSource(), 1.0F, 0.8F);
			}
		}
	}

	/** While her claws are out, her creatures hang back: slowness III until the attack is over. */
	private void holdMinions() {
		for (Group group : groups) {
			for (Mob mob : group.living) {
				mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, actionLength() + 1, 2, false, true), this);
			}
		}
	}

	/** The claw attack is over: her creatures move freely again. */
	private void releaseMinions() {
		for (Group group : groups) {
			for (Mob mob : group.living) {
				MobEffectInstance slowness = mob.getEffect(MobEffects.SLOWNESS);
				if (slowness != null && slowness.getAmplifier() == 2) {
					mob.removeEffect(MobEffects.SLOWNESS);
				}
			}
		}
	}

	/** Gone in a puff of smoke, back in another. */
	private void blink(ServerLevel level, Vec3 to) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		YagaFx.blink(level, this);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.5F, 0.7F);
		teleportTo(to.x, to.y, to.z);
		getNavigation().stop();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY(0.5), getZ(), 12, 0.3, 0.6, 0.3, 0.0);
		YagaFx.blink(level, this);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.5F, 0.7F);
	}

	/** Runs to the creature she means to eat. */
	private void tickEatRun(@Nullable LivingEntity target) {
		Mob meal = eatTarget;
		if (meal == null || !meal.isAlive()) {
			eatTarget = null;
			eatCooldown = Math.max(eatCooldown, 100);
			endAction();
			return;
		}
		getLookControl().setLookAt(meal, 30.0F, 30.0F);
		faceTowards(meal);
		if (distanceTo(meal) <= EAT_REACH) {
			getNavigation().stop();
			setAction(YagaAction.EAT);
			return;
		}
		if (actionTicks % 5 == 1) {
			getNavigation().moveTo(meal, settings().phaseTwo().fleeSpeed() * 1.2);
		}
	}

	/** Holds the creature and eats it: it is gone, she is healed. */
	private void tickEat(ServerLevel level, int t) {
		Mob meal = eatTarget;
		BabaYagaSettings.Eat eat = settings().eat();
		if (meal == null || !meal.isAlive()) {
			eatTarget = null;
			endAction();
			return;
		}
		getNavigation().stop();
		faceTowards(meal);
		// Held fast while she eats it: its own steps stopped every tick, nothing saved on it.
		meal.getNavigation().stop();
		meal.setDeltaMovement(Vec3.ZERO);
		if (t % 4 == 0) {
			playSound(SoundEvents.GENERIC_EAT.value(), 1.2F, 0.6F + random.nextFloat() * 0.2F);
			level.sendParticles(new DustParticleOptions(0x5C0F0F, 1.2F), meal.getX(), meal.getY(0.6), meal.getZ(), 6, 0.25, 0.3, 0.25, 0.0);
			YagaFx.feed(level, meal);
		}
		if (t != eat.ticks()) {
			return;
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), meal.getX(), meal.getY(0.5), meal.getZ(),
				30, 0.3, 0.4, 0.3, 0.1);
		meal.discard();
		eatTarget = null;
		heal(eat.heal());
		eatCooldown = eat.cooldown();
		playSound(SoundEvents.PLAYER_BURP, 1.5F, 0.6F);
		level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 6, 0.4, 0.2, 0.4, 0.0);
	}

	/**
	 * Hit a couple of times, she blinds the player and runs off into the dark at a great speed, then blinks behind
	 * their back, a long way off, and raises a few creatures next to them.
	 */
	private void tickEscape(ServerLevel level, int t, @Nullable LivingEntity target) {
		if (target == null) {
			return;
		}
		BabaYagaSettings.Escape escape = settings().escape();
		if (t == 0) {
			target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, escape.blindTicks(), 0), this);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, getSoundSource(), 2.0F, 0.6F);
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.WITCH_CELEBRATE, getSoundSource(), 2.0F, 0.8F);
			escapeTo = darkestAway(target);
			escapeCooldown = escape.cooldown();
			recentHits.clear();
		}
		if (t < escape.runTicks() && !escapeAtOnce) {
			if (escapeTo != null && (t % 5 == 0 || getNavigation().isDone())) {
				getNavigation().moveTo(escapeTo.x, escapeTo.y, escapeTo.z, escape.runSpeed());
			}
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.4), getZ(), 2, 0.2, 0.3, 0.2, 0.0);
			return;
		}
		getNavigation().stop();
		Vec3 spot = null;
		for (int attempt = 0; attempt < 5 && spot == null; attempt++) {
			double[] offset = BabaYagaLogic.behind(target.getYHeadRot(), (random.nextFloat() - 0.5F) * 40.0F, escape.distance() * (1.0 - attempt * 0.15));
			spot = ground(level, target.getX() + offset[0], target.getY(), target.getZ() + offset[1], getType());
		}
		if (spot != null) {
			blink(level, spot);
		}
		int count = escape.minSpawn() + (escape.maxSpawn() > escape.minSpawn() ? random.nextInt(escape.maxSpawn() - escape.minSpawn() + 1) : 0);
		// Her cap of living creatures holds here too.
		BabaYagaSettings.PhaseTwo two = settings().phaseTwo();
		groups.removeIf(group -> group.alive(level) == 0);
		int alive = aliveMinions();
		raiseNear(level, target, BabaYagaLogic.maySummon(alive, summonLockout, two) ? BabaYagaLogic.underCap(count, alive, two) : 0);
		summonLockout = BabaYagaLogic.summonLockout(aliveMinions(), summonLockout, two);
		escapeTo = null;
		if (escapeAtOnce) {
			escapeAtOnce = false;
			endAction();
		}
	}

	/** Of a few spots away from the player, the darkest. */
	private @Nullable Vec3 darkestAway(LivingEntity target) {
		Vec3 best = null;
		int bestLight = Integer.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			Vec3 spot = DefaultRandomPos.getPosAway(this, 16, 6, target.position());
			if (spot == null) {
				continue;
			}
			int light = level().getMaxLocalRawBrightness(BlockPos.containing(spot));
			if (light < bestLight) {
				bestLight = light;
				best = spot;
			}
		}
		return best;
	}

	// ---- Damage ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (waiting() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (action == YagaAction.SHATTER) {
			return false;
		}
		ParryPenalty penalty = BossParry.penalty();
		Player player = source.getEntity() instanceof Player p && source.getDirectEntity() == p ? p : null;
		boolean penalized = player != null && parryStreak.penalized(player.getUUID(), level.getGameTime(), penalty);
		// A hit between ticks answers the tick just run, the one whose glow the player sees.
		int shownTick = actionTicks - 1;
		if (phase() == 1) {
			BabaYagaSettings.Swoop swoop = settings().swoop();
			if (player != null && action == YagaAction.SWOOP
					&& BabaYagaLogic.parries(shownTick, swoop.strikeTick(), penalty.window(swoop.parryWindow(), penalized))) {
				return parryInAir(level, player, source, swoop);
			}
			// Nothing gets through to her in her mortar.
			level.playSound(null, getX(), getY(0.5), getZ(), SoundEvents.AMETHYST_BLOCK_HIT, getSoundSource(), 1.0F, 0.6F);
			level.sendParticles(ParticleTypes.WITCH, getX(), getY(0.5), getZ(), 6, 0.4, 0.5, 0.4, 0.0);
			return false;
		}
		BabaYagaSettings.Claws claws = settings().claws();
		boolean parry = player != null && action == YagaAction.CLAWS
				&& BabaYagaLogic.parries(shownTick, claws.lunge(), penalty.window(claws.parryWindow(), penalized));
		if (parry) {
			damage *= penalty.multiplier(claws.parryMultiplier(), penalized);
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (!hurt || !isAlive()) {
			return hurt;
		}
		if (parry) {
			parried(level, player);
			return true;
		}
		if (source.getEntity() instanceof Player) {
			noteHit(level);
		}
		return true;
	}

	/** The parry window against {@code target}: shorter for a player who parried her a moment ago, as in hurtServer. */
	private int parryWindow(ServerLevel level, int base, @Nullable LivingEntity target) {
		ParryPenalty penalty = BossParry.penalty();
		boolean penalized = target instanceof Player player && parryStreak.penalized(player.getUUID(), level.getGameTime(), penalty);
		return penalty.window(base, penalized);
	}

	/** A parry in phase one: a fixed share of her health, whatever the weapon; the last one breaks her mortar. */
	private boolean parryInAir(ServerLevel level, Player player, DamageSource source, BabaYagaSettings.Swoop swoop) {
		damageCooldownTime = 0;
		exactHit = true;
		boolean hurt;
		try {
			hurt = super.hurtServer(level, source, swoop.parryDamage());
		} finally {
			exactHit = false;
		}
		if (hurt && isAlive() && phase() == 1) {
			parried(level, player);
		}
		return hurt;
	}

	/** She is thrown back and breaks off. */
	private void parried(ServerLevel level, Player player) {
		parryStreak.parried(player.getUUID(), level.getGameTime());
		if (phase() == 1) {
			diveLanded = true;
		}
		LivingEntity target = actionTarget != null ? actionTarget : getTarget();
		if (phase() == 2 && target != null) {
			// On foot a parry makes her blink away at once.
			setAction(YagaAction.ESCAPE);
			escapeAtOnce = true;
			actionTarget = target;
			getNavigation().stop();
			level.playSound(null, getX(), getY(0.6), getZ(), EamSounds.FALLEN_LORD_PARRY, getSoundSource(), 2.0F, 1.0F);
			level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 16, 0.4, 0.4, 0.4, 0.2);
			return;
		}
		setAction(YagaAction.STAGGER);
		actionTarget = target;
		setGlow(YagaGlow.NONE);
		Vec3 away = horizontal(position().subtract(player.position()));
		setDeltaMovement(away.x * 0.8, phase() == 1 ? 0.5 : 0.2, away.z * 0.8);
		level.playSound(null, getX(), getY(0.6), getZ(), EamSounds.FALLEN_LORD_PARRY, getSoundSource(), 2.0F, 1.0F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 16, 0.4, 0.4, 0.4, 0.2);
		level.sendParticles(new DustParticleOptions(YagaGlow.PARRY.color, 1.4F), getX(), getY(0.6), getZ(), 12, 0.4, 0.4, 0.4, 0.0);
	}

	/** Phase two: a player's hit; a couple of them close together make her escape. */
	private void noteHit(ServerLevel level) {
		BabaYagaSettings.Escape escape = settings().escape();
		long now = level.getGameTime();
		recentHits.addLast(now);
		while (!recentHits.isEmpty() && now - recentHits.peekFirst() > escape.window()) {
			recentHits.removeFirst();
		}
		if (BabaYagaLogic.escapes(recentHits.size(), escape, escapeCooldown) && action != YagaAction.ESCAPE) {
			if (action != YagaAction.NONE) {
				endAction();
				eatTarget = null;
			}
			escapeRequested = true;
		}
	}

	@Override
	protected float getDamageAfterArmorAbsorb(DamageSource source, float damage) {
		return exactHit ? damage : super.getDamageAfterArmorAbsorb(source, damage);
	}

	@Override
	protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
		return exactHit ? damage : super.getDamageAfterMagicAbsorb(source, damage);
	}

	/** Phase one does not end in death: the last parry breaks her mortar instead. */
	@Override
	public void die(DamageSource source) {
		if (phase() == 1 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && level() instanceof ServerLevel level) {
			startShatter(level);
			return;
		}
		super.die(source);
		bossEvent.setProgress(0.0F);
	}

	/** Her long death ({@link YagaDeath}): burning, convulsions, then a heap of ash her loot drops from. */
	@Override
	protected void tickDeath() {
		deathTime++;
		if (level() instanceof ServerLevel level && !isRemoved()) {
			YagaDeath.tick(this, level, deathTime);
			if (deathTime >= YagaDeath.DEATH_TICKS) {
				// Killed by a player counts as at the moment she died, however long she took to burn.
				lastHurtByPlayerMemoryTime = Math.max(lastHurtByPlayerMemoryTime, deathPlayerMemory);
				super.dropAllDeathLoot(level, deathSource != null ? deathSource : damageSources().generic());
				level.broadcastEntityEvent(this, (byte) 60);
				remove(RemovalReason.KILLED);
			}
		}
	}

	/** Her loot waits for her to burn to ash: {@link #tickDeath()} drops it. */
	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		deathSource = source;
		deathPlayerMemory = lastHurtByPlayerMemoryTime;
	}

	/** Her map, remembering where she died; the Mending book and the ash come from the loot table. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		spawnAtLocation(level, YagaGraveMapItem.forDeathAt(new ItemStack(EamItems.YAGA_GRAVE_MAP), GlobalPos.of(level.dimension(), blockPosition())));
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		// In her mortar nothing touches her, poison and the like included.
		return phase() != 1 && super.canBeAffected(effect);
	}

	@Override
	public boolean fireImmune() {
		return phase() == 1 || super.fireImmune();
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
		output.putByte("Phase", (byte) phase());
		output.putBoolean("Storm", stormActive);
		output.putInt("Unseen", unseenTicks);
		output.putBoolean("ReturningHome", returningHome);
		if (home != null) {
			output.putLong("Home", home.asLong());
		}
		output.putBoolean("WaitingInHut", waiting());
		if (hutDoor != null) {
			output.putLong("HutDoor", hutDoor.asLong());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int phase = input.getByteOr("Phase", (byte) 1) == 2 ? 2 : 1;
		entityData.set(DATA_PHASE, (byte) phase);
		refreshDimensions();
		setNoGravity(phase == 1);
		applyStats(false);
		if (phase == 2) {
			bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
			bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
		}
		bossEvent.setName(getDisplayName());
		stormActive = input.getBooleanOr("Storm", false);
		unseenTicks = input.getIntOr("Unseen", 0);
		returningHome = input.getBooleanOr("ReturningHome", false);
		home = input.getLong("Home").map(BlockPos::of).orElse(null);
		hutDoor = input.getLong("HutDoor").map(BlockPos::of).orElse(null);
		setWaiting(input.getBooleanOr("WaitingInHut", false));
		// Saved while her storm was the whole world's weather, recently enough that it still thunders: put the old back.
		long stormSavedAt = input.getLongOr("StormSavedAt", level().getGameTime());
		if (input.getBooleanOr("StormOwner", false) && level().getGameTime() - stormSavedAt <= LEGACY_STORM_TICKS) {
			legacyWeather = new LegacyWeather(input.getIntOr("WeatherClear", 0), input.getIntOr("WeatherRain", 0),
					input.getIntOr("WeatherThunder", 0), input.getBooleanOr("WeatherRaining", false),
					input.getBooleanOr("WeatherThundering", false));
		}
		// A wave in progress is not saved: she raises a fresh one.
		nextWave = settings().summon().firstDelay();
		laterParts.clear();
		adoptMinions = true;
	}

	// ---- Sounds: a witch's voice, older ----

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return waiting() ? null : SoundEvents.WITCH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITCH_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		// Phase one's "death" is her mortar breaking, which makes its own noise.
		return phase() == 1 ? null : SoundEvents.WITCH_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}

	// ---- Groups and spells in flight ----

	/** One group of creatures raised together, which her spells treat as one. */
	private static final class Group {
		final List<UUID> members = new ArrayList<>();
		/** The members alive at the last update. */
		final List<Mob> living = new ArrayList<>();
		final Map<UUID, Float> lastHealth = new HashMap<>();
		long lastHurt = Long.MIN_VALUE / 2;
		/** Game time it rose, and whether it is part of a phase-one wave (those wither if they linger). */
		long raisedAt;
		boolean wave;

		void add(Mob mob) {
			members.add(mob.getUUID());
			living.add(mob);
			lastHealth.put(mob.getUUID(), mob.getHealth() + mob.getAbsorptionAmount());
		}

		/** Refreshes who is alive and whether the player hurt any of them; returns how many are alive. */
		int update(ServerLevel level, long now) {
			living.clear();
			for (Iterator<UUID> it = members.iterator(); it.hasNext(); ) {
				UUID id = it.next();
				if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
					living.add(mob);
					float health = mob.getHealth() + mob.getAbsorptionAmount();
					Float before = lastHealth.put(id, health);
					if (before != null && health < before - 0.01F && mob.getLastHurtByMob() instanceof Player) {
						lastHurt = now;
					}
				} else {
					it.remove();
					lastHealth.remove(id);
				}
			}
			return living.size();
		}

		int alive(ServerLevel level) {
			return update(level, level.getGameTime());
		}

		Vec3 center() {
			if (living.isEmpty()) {
				return Vec3.ZERO;
			}
			double x = 0.0;
			double y = 0.0;
			double z = 0.0;
			for (Mob mob : living) {
				x += mob.getX();
				y += mob.getY();
				z += mob.getZ();
			}
			return new Vec3(x / living.size(), y / living.size(), z / living.size());
		}
	}

	/** A spell flying from her staff to a group in an arc of sparks. */
	private static final class SpellBolt {
		final Vec3 from;
		final Group group;
		final Buff buff;
		final int ticks;
		int age;
		Vec3 lastTo;

		SpellBolt(Vec3 from, Group group, Buff buff, int ticks) {
			this.from = from;
			this.group = group;
			this.buff = buff;
			this.ticks = ticks;
			this.lastTo = group.center().add(0.0, 1.0, 0.0);
		}
	}
}
