package io.github.hellenter.eam.mobs.entity.warlock;

import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.bat.VampireBat;
import io.github.hellenter.eam.mobs.entity.bat.VampireBatModule;
import io.github.hellenter.eam.mobs.entity.lost.CommanderTargetGoal;
import io.github.hellenter.eam.mobs.entity.lost.Lost;
import io.github.hellenter.eam.mobs.entity.lost.LostCommander;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The undead warlock: one of the Lost who kept his mind, in a black torn robe and a long cloak over chainmail. He walks
 * with big squads of the Lost and fights from a distance: red homing missiles, a gust that throws a player who came
 * too close back, Speed or Strength for the Lost around him, and now and then a flock of vampire bats. Numbers are in
 * {@code data/eam/mobs/undead_warlock.json}.
 */
public class UndeadWarlock extends Monster {
	private static final EntityDataAccessor<Byte> DATA_SPELL = SynchedEntityData.defineId(UndeadWarlock.class, EntityDataSerializers.BYTE);
	/** Squad mates and allies further away than this are not counted. */
	static final double SQUAD_RANGE = 32.0;
	private static final int SHARE_INTERVAL = 20;
	private static final DustParticleOptions RED = new DustParticleOptions(MagicMissile.COLOR, 1.0F);

	private final int[] cooldowns = new int[WarlockSpell.values().length];
	/** The leader of the squad he walks with; {@code null} on his own. */
	private @Nullable UUID leader;
	/** Ticks into the current spell, counted on both sides. */
	private int castTick;
	private int combatTicks;
	private int shareRefresh;

	public UndeadWarlock(EntityType<? extends UndeadWarlock> type, Level level) {
		super(type, level);
		xpReward = 20;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, UndeadWarlockSettings.DEFAULT.health())
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				// The chainmail under the robe.
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SPELL, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new WarlockCastGoal(this));
		goalSelector.addGoal(4, new WarlockFollowLeaderGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new CommanderTargetGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	/** He never turns on a player who carries the commander's horn. */
	@Override
	public boolean canAttack(LivingEntity target) {
		return super.canAttack(target) && !LostCommander.commands(target);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(UndeadWarlockModule.settings().health());
			setHealth(getMaxHealth());
		}
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	/** Joins the squad of this leader. */
	void follow(Lost squadLeader) {
		leader = squadLeader.getUUID();
	}

	/** The leader of his squad, if alive and near. */
	@Nullable Lost leaderNearby() {
		if (leader == null || !(level() instanceof ServerLevel level)) {
			return null;
		}
		return level.getEntity(leader) instanceof Lost lost && lost.isAlive() && distanceToSqr(lost) < SQUAD_RANGE * SQUAD_RANGE ? lost : null;
	}

	// ---- Spells ----

	public WarlockSpell spell() {
		return WarlockSpell.byId(entityData.get(DATA_SPELL));
	}

	/** Ticks into the current spell, with the partial tick for smooth gestures. */
	public float castTime(float partialTicks) {
		return spell() == WarlockSpell.NONE ? 0.0F : castTick + partialTicks;
	}

	int castTick() {
		return castTick;
	}

	void startSpell(WarlockSpell spell) {
		entityData.set(DATA_SPELL, (byte) spell.ordinal());
		castTick = 0;
		SoundEvent sound = switch (spell) {
			case MISSILES -> SoundEvents.EVOKER_PREPARE_ATTACK;
			case WIND -> SoundEvents.BREEZE_INHALE;
			case BUFF -> SoundEvents.EVOKER_PREPARE_WOLOLO;
			case BATS -> SoundEvents.EVOKER_PREPARE_SUMMON;
			case NONE -> null;
		};
		if (sound != null) {
			playSound(sound, 1.0F, 0.75F);
		}
	}

	void endSpell(int cooldown, int gap) {
		WarlockSpell spell = spell();
		cooldowns[spell.ordinal()] = cooldown;
		for (WarlockSpell other : WarlockSpell.values()) {
			cooldowns[other.ordinal()] = Math.max(cooldowns[other.ordinal()], gap);
		}
		entityData.set(DATA_SPELL, (byte) 0);
		castTick = 0;
	}

	/** Which spells are off cooldown, by ordinal. */
	boolean[] readySpells() {
		boolean[] ready = new boolean[cooldowns.length];
		for (int i = 0; i < cooldowns.length; i++) {
			ready[i] = cooldowns[i] <= 0;
		}
		// The bats come only once the fight has gone on a while.
		ready[WarlockSpell.BATS.ordinal()] &= combatTicks >= UndeadWarlockModule.settings().bats().firstDelay();
		return ready;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_SPELL.equals(accessor)) {
			castTick = 0;
		}
	}

	@Override
	public void tick() {
		super.tick();
		WarlockSpell spell = spell();
		if (spell == WarlockSpell.NONE) {
			return;
		}
		castTick++;
		if (level().isClientSide()) {
			castParticles(spell);
		}
	}

	/** Glowing hands while he casts, in the colour of the spell. */
	private void castParticles(WarlockSpell spell) {
		ParticleOptions particle = switch (spell) {
			case MISSILES -> RED;
			case WIND -> ParticleTypes.SMALL_GUST;
			case BUFF -> ParticleTypes.SOUL_FIRE_FLAME;
			case BATS -> ParticleTypes.SMOKE;
			case NONE -> null;
		};
		if (particle == null || (spell == WarlockSpell.WIND && random.nextInt(3) != 0)) {
			return;
		}
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(yaw);
		double forwardZ = Mth.cos(yaw);
		for (int side = -1; side <= 1; side += 2) {
			if (spell == WarlockSpell.MISSILES && side < 0) {
				continue;
			}
			double x = getX() + forwardX * 0.6 - forwardZ * 0.4 * side;
			double z = getZ() + forwardZ * 0.6 + forwardX * 0.4 * side;
			double y = getY() + (spell == WarlockSpell.BUFF || spell == WarlockSpell.BATS ? 2.4 : 1.5);
			level().addParticle(particle, x + random.nextGaussian() * 0.08, y + random.nextGaussian() * 0.08, z + random.nextGaussian() * 0.08,
					0.0, spell == WarlockSpell.BUFF ? 0.03 : 0.0, 0.0);
		}
	}

	/** The point in front of his right hand, where his missiles start. */
	Vec3 castingHand() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double forwardX = -Mth.sin(yaw);
		double forwardZ = Mth.cos(yaw);
		return new Vec3(getX() + forwardX * 0.8 + forwardZ * 0.35, getY() + 1.6, getZ() + forwardZ * 0.8 - forwardX * 0.35);
	}

	/** Missile {@code index} of {@code count}, fanned out sideways and upward, curving in on the target. */
	void fireMissile(LivingEntity target, int index, int count, UndeadWarlockSettings.Missiles settings) {
		Vec3 from = castingHand();
		Vec3 toTarget = target.getBoundingBox().getCenter().subtract(from).normalize();
		float spread = (index - (count - 1) / 2.0F) * 35.0F * Mth.DEG_TO_RAD;
		Vec3 direction = toTarget.yRot(spread).add(0.0, 0.5, 0.0).normalize().scale(settings.speed());
		MagicMissile missile = new MagicMissile(level(), this, target, from, direction, settings);
		level().addFreshEntity(missile);
		playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.3F + random.nextFloat() * 0.2F);
	}

	/** A gust in front of him: everything not undead in it is thrown back and a little up. */
	void gust(ServerLevel level, UndeadWarlockSettings.Wind wind) {
		Vec3 look = getLookAngle();
		Vec3 centre = position().add(0.0, 1.0, 0.0);
		List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(wind.reach()),
				entity -> entity != this && entity.isAlive() && !isUndeadAlly(entity) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
						&& !LostCommander.commands(entity)
						&& entity.distanceToSqr(this) <= wind.reach() * wind.reach());
		for (LivingEntity entity : caught) {
			double dx = entity.getX() - getX();
			double dz = entity.getZ() - getZ();
			if (!UndeadWarlockLogic.inGust(dx, dz, look.x, look.z)) {
				continue;
			}
			Vec3 away = new Vec3(dx, 0.0, dz);
			away = away.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0.0, look.z) : away;
			double resist = 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			if (wind.damage() > 0.0F) {
				entity.hurtServer(level, damageSources().mobAttack(this), wind.damage());
			}
			Vec3 push = away.normalize().scale(wind.push() * resist).add(0.0, wind.lift() * resist, 0.0);
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.2).add(push));
			entity.needsSync = true;
			entity.syncVelocity = true;
		}
		// The wave rolls out in front of him.
		for (int step = 1; step <= 4; step++) {
			Vec3 at = centre.add(look.x * step * 1.5, 0.0, look.z * step * 1.5);
			level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.SMALL_GUST, at.x, at.y, at.z, 6, 0.6 * step * 0.5, 0.4, 0.6 * step * 0.5, 0.0);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.2F, 0.7F);
	}

	/** The Lost near him that have neither Speed nor Strength. */
	List<Lost> unbuffedLost(ServerLevel level, double radius) {
		return level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(radius),
				lost -> lost.isAlive() && !lost.hasEffect(MobEffects.SPEED) && !lost.hasEffect(MobEffects.STRENGTH));
	}

	/** Speed I for Lost far from the target, so they close in, Strength I for those already fighting. */
	void buff(ServerLevel level, LivingEntity target, UndeadWarlockSettings.Buff buff) {
		List<Lost> allies = level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(buff.radius()), Lost::isAlive);
		if (allies.isEmpty()) {
			return;
		}
		double distance = 0.0;
		for (Lost lost : allies) {
			distance += lost.distanceTo(target);
		}
		boolean haste = UndeadWarlockLogic.haste(distance / allies.size());
		for (Lost lost : allies) {
			lost.addEffect(new MobEffectInstance(haste ? MobEffects.SPEED : MobEffects.STRENGTH, buff.duration(), 0), this);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, lost.getX(), lost.getY() + 1.2, lost.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 1.0F, 0.7F);
	}

	/** Vampire bats near him, already flying. */
	int batsNearby(ServerLevel level) {
		return level.getEntitiesOfClass(VampireBat.class, getBoundingBox().inflate(SQUAD_RANGE)).size();
	}

	/** A flock of vampire bats bursts out of the dark above him and goes for the target. */
	void summonBats(ServerLevel level, LivingEntity target, UndeadWarlockSettings.Bats bats) {
		int count = UndeadWarlockLogic.flockSize(bats.min(), bats.max(), random.nextDouble());
		Vec3 above = position().add(0.0, getBbHeight() + 0.8, 0.0);
		int spawned = VampireBatModule.summon(level, above, target, count);
		if (spawned > 0) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, above.x, above.y, above.z, 20, 0.6, 0.4, 0.6, 0.02);
			level.playSound(null, above.x, above.y, above.z, EamSounds.VAMPIRE_BAT_SWARM, SoundSource.HOSTILE, 1.2F, 1.0F);
		}
	}

	private static boolean isUndeadAlly(LivingEntity entity) {
		return entity instanceof Lost || entity instanceof UndeadWarlock || entity instanceof VampireBat;
	}

	// ---- Squad ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		LivingEntity target = getTarget();
		combatTicks = target != null && target.isAlive() ? combatTicks + 1 : 0;
		if (--shareRefresh <= 0) {
			shareRefresh = SHARE_INTERVAL;
			shareTarget(level);
		}
	}

	/** He fights whom the Lost around him fight, and they fight whom he fights. */
	private void shareTarget(ServerLevel level) {
		List<Lost> near = level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(SQUAD_RANGE), Lost::isAlive);
		LivingEntity own = getTarget();
		if (own == null || !own.isAlive()) {
			for (Lost lost : near) {
				LivingEntity target = lost.getTarget();
				if (target != null && target.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(target)) {
					setTarget(target);
					return;
				}
			}
			return;
		}
		if (EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(own)) {
			for (Lost lost : near) {
				LivingEntity target = lost.getTarget();
				if (target == null || !target.isAlive()) {
					lost.setTarget(own);
				}
			}
		}
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.storeNullable("Leader", UUIDUtil.CODEC, leader);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		leader = input.read("Leader", UUIDUtil.CODEC).orElse(null);
	}

	// ---- Sounds: a husk's, deep and hoarse ----

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
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}
}
