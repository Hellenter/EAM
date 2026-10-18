package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardLogic.Attack;
import io.github.hellenter.eam.mobs.entity.king.AxeThrower;
import io.github.hellenter.eam.mobs.entity.king.OnslaughtMoves;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic;
import io.github.hellenter.eam.mobs.entity.king.ThrownKingAxe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A pillager guard: a pillager in a round brimmed helmet and an iron cuirass, fighting with an axe like a vindicator.
 * On top of plain swings he has the Pillager King's onslaught with one axe (a rush and a leap for a blow a shield only
 * softens, parried by a hit while he is in the air) and throws his axe, which flies back to his hand. He is a raider:
 * he marches in patrols, guards outposts and comes in raids. Numbers are in {@code data/eam/mobs/pillager_guard.json}.
 */
public class PillagerGuard extends Vindicator implements AxeThrower {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(PillagerGuard.class, EntityDataSerializers.BYTE);
	/** Scales his plain axe blows by {@code stats.melee_damage_multiplier}. */
	private static final Identifier MELEE_DAMAGE = Eam.id("guard_melee_damage");
	/** Attack goals of the vindicator sit at 4 and 5: his moves come first. */
	private static final int MOVES_PRIORITY = 3;
	/** The thrown axe is back in his hand after this many ticks at the latest, even if it got lost. */
	private static final int AXE_BACK_AFTER = 100;

	private GuardAction action = GuardAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private int onslaughtCooldown;
	private int throwCooldown;
	/** The axe out flying, given back when it returns; empty while he holds it. */
	private ItemStack thrownAxe = ItemStack.EMPTY;
	/** Ticks since he threw his axe, or -1 while he holds it. */
	private int axeAway = -1;
	/** Client: the tick count when the current action started. */
	private int clientActionStart;

	public PillagerGuard(EntityType<? extends PillagerGuard> type, Level level) {
		super(type, level);
		xpReward = PillagerGuardModule.settings().stats().xp();
	}

	public static AttributeSupplier.Builder createAttributes() {
		PillagerGuardSettings.Stats stats = PillagerGuardSettings.DEFAULT.stats();
		return Vindicator.createAttributes()
				.add(Attributes.MAX_HEALTH, stats.health())
				.add(Attributes.ARMOR, stats.armor())
				.add(Attributes.MOVEMENT_SPEED, stats.speed())
				.add(Attributes.FOLLOW_RANGE, stats.followRange());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(MOVES_PRIORITY, new GuardMovesGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		applyStats(true);
		return data;
	}

	/** Health, armor, speed and the rest from the data file; {@code full} also heals him to the new maximum. */
	private void applyStats(boolean full) {
		PillagerGuardSettings.Stats stats = PillagerGuardModule.settings().stats();
		setBase(Attributes.MAX_HEALTH, stats.health());
		setBase(Attributes.ARMOR, stats.armor());
		setBase(Attributes.MOVEMENT_SPEED, stats.speed());
		setBase(Attributes.FOLLOW_RANGE, stats.followRange());
		AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) {
			attack.addOrReplacePermanentModifier(new AttributeModifier(MELEE_DAMAGE, stats.meleeDamageMultiplier() - 1.0,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
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

	public GuardAction action() {
		return GuardAction.byId(entityData.get(DATA_ACTION));
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

	/** His moves are posed by the model: arms out of the vindicator's crossed rest while one plays. */
	@Override
	public AbstractIllager.IllagerArmPose getArmPose() {
		return action() != GuardAction.NONE ? AbstractIllager.IllagerArmPose.ATTACKING : super.getArmPose();
	}

	public boolean isBusy() {
		return action != GuardAction.NONE;
	}

	int onslaughtCooldown() {
		return onslaughtCooldown;
	}

	int throwCooldown() {
		return throwCooldown;
	}

	/** He holds his axe: it is not out flying. */
	boolean hasAxe() {
		return axeAway < 0 && !getMainHandItem().isEmpty();
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (onslaughtCooldown > 0) onslaughtCooldown--;
		if (throwCooldown > 0) throwCooldown--;
		if (axeAway >= 0 && ++axeAway > AXE_BACK_AFTER) {
			catchAxe();
		}
		if (action != GuardAction.NONE) {
			tickAction(level);
		}
	}

	void startAttack(Attack attack, LivingEntity target) {
		actionTarget = target;
		PillagerGuardSettings settings = PillagerGuardModule.settings();
		switch (attack) {
			case ONSLAUGHT -> {
				onslaughtCooldown = settings.onslaught().cooldown();
				setAction(GuardAction.ONSLAUGHT);
			}
			case THROW -> {
				throwCooldown = settings.axeThrow().cooldown();
				setAction(GuardAction.THROW);
			}
		}
	}

	private void setAction(GuardAction next) {
		action = next;
		actionTicks = 0;
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		setAction(GuardAction.NONE);
		actionTarget = null;
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= action.length()) {
			endAction();
			return;
		}
		if (action != GuardAction.STAGGER && (target == null || !target.isAlive())) {
			endAction();
			return;
		}
		getNavigation().stop();
		if (target == null || action == GuardAction.STAGGER) {
			return;
		}
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		PillagerGuardSettings settings = PillagerGuardModule.settings();
		switch (action) {
			case ONSLAUGHT -> tickOnslaught(level, t, target, settings.onslaught());
			case THROW -> {
				if (t == GuardAction.THROW_RELEASE) {
					throwAxe(level, target, settings.axeThrow());
				}
			}
			default -> {
			}
		}
	}

	/** The king's onslaught with one axe: the blow at its end goes through a shield, which only softens it. */
	private void tickOnslaught(ServerLevel level, int t, LivingEntity target, PillagerGuardSettings.Onslaught onslaught) {
		if (!OnslaughtMoves.tick(this, level, t, target, onslaught.rushSpeed(), onslaught.reach())) {
			return;
		}
		boolean shielded = OnslaughtMoves.shielded(target, this);
		float damage = PillagerKingLogic.onslaughtDamage(onslaught.damage(), shielded, onslaught.shieldReduction());
		DamageSource source = damageSources().source(PillagerGuardModule.ONSLAUGHT_DAMAGE, this);
		target.damageCooldownTime = 0;
		if (target.hurtServer(level, source, damage)) {
			target.knockback(0.6F, getX() - target.getX(), getZ() - target.getZ(), source, damage);
			setLastHurtMob(target);
		}
		if (shielded) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, getSoundSource(), 1.0F, 0.7F);
		}
	}

	// ---- Axe throw ----

	private void throwAxe(ServerLevel level, LivingEntity target, PillagerGuardSettings.AxeThrow axe) {
		ItemStack held = getMainHandItem();
		if (held.isEmpty()) {
			return;
		}
		thrownAxe = held.copy();
		ThrownKingAxe thrown = new ThrownKingAxe(EamEntities.GUARD_AXE, level, this, thrownAxe.copyWithCount(1), axe.damage(), 0.0F);
		// From his right hand, at the target's chest.
		Vec3 right = Vec3.directionFromRotation(0.0F, getYRot() - 90.0F).scale(0.4);
		thrown.setPos(getX() + right.x, getY(0.7), getZ() + right.z);
		double dx = target.getX() - thrown.getX();
		double dy = target.getY(0.5) - thrown.getY();
		double dz = target.getZ() - thrown.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		thrown.shoot(dx, dy + flat * 0.05, dz, (float) axe.speed(), 1.0F);
		level.addFreshEntity(thrown);
		playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.8F);
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		axeAway = 0;
	}

	/** The thrown axe is back in his hand. */
	@Override
	public void catchAxe() {
		axeAway = -1;
		if (getMainHandItem().isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, thrownAxe.isEmpty() ? new ItemStack(Items.IRON_AXE) : thrownAxe);
		}
		thrownAxe = ItemStack.EMPTY;
	}

	/** His axe leaves shields be. */
	@Override
	public void axeBrokeShield(LivingEntity target) {
	}

	@Override
	public float axeDamageTo(float damage, Entity target) {
		return damage;
	}

	// ---- Damage: the parry ----

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean parry = source.getEntity() instanceof Player player && source.getDirectEntity() == player
				&& PillagerGuardLogic.parries(action, actionTicks);
		if (parry) {
			damage *= PillagerGuardModule.settings().onslaught().parryMultiplier();
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (parry && isAlive()) {
			parried(level, (Player) source.getEntity());
		}
		return hurt;
	}

	/** A player hit him in the air in his onslaught: the leap breaks off and he staggers back, open to hits. */
	private void parried(ServerLevel level, Player player) {
		LivingEntity target = actionTarget;
		setAction(GuardAction.STAGGER);
		actionTarget = target;
		Vec3 away = horizontal(position().subtract(player.position()));
		setDeltaMovement(away.x * 0.4, 0.1, away.z * 0.4);
		level.playSound(null, getX(), getY(0.6), getZ(), EamSounds.FALLEN_LORD_PARRY, getSoundSource(), 1.5F, 1.1F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.6), getZ(), 10, getBbWidth() * 0.4, getBbHeight() * 0.2, getBbWidth() * 0.4, 0.2);
	}

	// ---- Geometry ----

	public double horizontalDistanceTo(Entity other) {
		double dx = other.getX() - getX();
		double dz = other.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
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

	/** Killed while his axe is out flying: it may still drop, as if he held it. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		if (getMainHandItem().isEmpty() && !thrownAxe.isEmpty()) {
			setItemSlot(EquipmentSlot.MAINHAND, thrownAxe);
			thrownAxe = ItemStack.EMPTY;
		}
		super.dropCustomDeathLoot(level, source, killedByPlayer);
	}

	// ---- Spawning like a pillager ----

	/** Spawns wherever a pillager may, light or dark: outposts are out in the open. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	@Override
	public int getMaxSpawnClusterSize() {
		return 1;
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (!thrownAxe.isEmpty()) {
			output.store("ThrownAxe", ItemStack.CODEC, thrownAxe);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		thrownAxe = input.read("ThrownAxe", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		applyStats(false);
		// Saved while his axe was out flying (the axe itself is not kept): give it back.
		if (!thrownAxe.isEmpty()) {
			catchAxe();
		}
	}

	// ---- Sounds: a pillager's voice ----

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
	public SoundEvent getCelebrateSound() {
		return SoundEvents.PILLAGER_CELEBRATE;
	}
}
