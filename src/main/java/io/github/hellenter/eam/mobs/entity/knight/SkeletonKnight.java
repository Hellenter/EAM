package io.github.hellenter.eam.mobs.entity.knight;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A crypt skeleton in ancient metal plate with a sword and a shield. He keeps the shield up while he is not attacking,
 * so it must be knocked out with an axe or caught between his attacks; he cuts once, in a three-cut combo, or with a
 * slow overhead strike that goes through a shield and knocks it out of the player's guard. His moves are a small
 * version of the Fallen Lord's.
 */
public class SkeletonKnight extends AbstractSkeleton {
	private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(SkeletonKnight.class, EntityDataSerializers.BYTE);
	/** Whether an axe has knocked his shield out of the guard; the client shows it hanging. */
	private static final EntityDataAccessor<Boolean> DATA_SHIELD_KNOCKED = SynchedEntityData.defineId(SkeletonKnight.class,
			EntityDataSerializers.BOOLEAN);
	/** Hits further than this flat distance past {@link SkeletonKnightSettings#reach()} still land, for a moving target. */
	private static final double REACH_SLACK = 0.3;
	/** A dash stops this much short of the hit's reach, so the cut lands even if the target keeps backing off. */
	private static final double DASH_STOP_SHORT = 1.0;

	private KnightAction action = KnightAction.NONE;
	private int actionTicks;
	private @Nullable LivingEntity actionTarget;
	private int idleTicks;
	private int comboCooldown;
	private int heavyCooldown;
	private int shieldDisabledTicks;
	private int dashCooldown;
	private int dashTicks;
	/** Whether he has already decided how to reach the coming hit. */
	private boolean hitChecked;
	/** The combo ends after the coming cut: it will miss and the dash is not ready. */
	private boolean comboStops;
	/** Client: the tick count when the current action started. */
	private int clientActionStart;
	/** Client: how far the shield is raised into the guard, 0 to 1, now and a tick ago. */
	private float guard;
	private float guardO;
	/** Client: how far the shield hangs knocked out of the guard, 0 to 1, now and a tick ago. */
	private float knocked;
	private float knockedO;

	public SkeletonKnight(EntityType<? extends SkeletonKnight> type, Level level) {
		super(type, level);
		xpReward = 20;
		// Ancient metal has no other source yet: his gear stays with him.
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 50.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	/** His balance numbers; a mob built on him may read its own. */
	protected SkeletonKnightSettings settings() {
		return SkeletonKnightModule.settings();
	}

	/** How far a spear jab ({@link KnightAction#POKE}) reaches; the knight himself carries no spear. */
	protected double pokeReach() {
		return settings().reach() + 0.6;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ACTION, (byte) 0);
		builder.define(DATA_SHIELD_KNOCKED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new KnightFightGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	/** His own goal fights; the skeleton bow and melee goals are never added. */
	@Override
	public void reassessWeaponGoal() {
	}

	@Override
	public boolean canUseNonMeleeWeapon(ItemStack item) {
		return false;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		// A crypt trial spawner may swap these for enchanted ones: see the eam:equipment/crypt/knight tables.
		setItemSlot(EquipmentSlot.HEAD, new ItemStack(EamItems.ANCIENT_METAL_HELMET));
		setItemSlot(EquipmentSlot.CHEST, new ItemStack(EamItems.ANCIENT_METAL_CHESTPLATE));
		setItemSlot(EquipmentSlot.LEGS, new ItemStack(EamItems.ANCIENT_METAL_LEGGINGS));
		setItemSlot(EquipmentSlot.FEET, new ItemStack(EamItems.ANCIENT_METAL_BOOTS));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EamItems.ANCIENT_METAL_SWORD));
		setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		// Cuts with the right arm, shield on the left.
		setLeftHanded(false);
		setCanPickUpLoot(false);
		return data;
	}

	public KnightAction action() {
		return KnightAction.byId(entityData.get(DATA_ACTION));
	}

	/** Client: ticks since the current action started, with the partial tick. */
	public float actionTime(float partialTicks) {
		return tickCount - clientActionStart + partialTicks;
	}

	/** Client: how far the shield is raised into the guard, 0 to 1. */
	public float guard(float partialTicks) {
		return Mth.lerp(partialTicks, guardO, guard);
	}

	/** Client: how far the shield hangs knocked out of the guard by an axe, 0 to 1. */
	public float shieldKnocked(float partialTicks) {
		return Mth.lerp(partialTicks, knockedO, knocked);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ACTION.equals(accessor)) {
			clientActionStart = tickCount;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			guardO = guard;
			guard = Mth.approach(guard, isUsingItem() && getUseItem().has(DataComponents.BLOCKS_ATTACKS) ? 1.0F : 0.0F, 0.25F);
			knockedO = knocked;
			// Knocked out at once, raised back slowly.
			knocked = entityData.get(DATA_SHIELD_KNOCKED) ? Math.min(1.0F, knocked + 0.5F) : Mth.approach(knocked, 0.0F, 0.15F);
		}
	}

	public boolean isBusy() {
		return action != KnightAction.NONE;
	}

	public boolean isIdleFor(int ticks) {
		return action == KnightAction.NONE && idleTicks >= ticks;
	}

	public int comboCooldown() {
		return comboCooldown;
	}

	public int heavyCooldown() {
		return heavyCooldown;
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (comboCooldown > 0) comboCooldown--;
		if (heavyCooldown > 0) heavyCooldown--;
		if (shieldDisabledTicks > 0) shieldDisabledTicks--;
		if (dashCooldown > 0) dashCooldown--;
		entityData.set(DATA_SHIELD_KNOCKED, shieldDisabledTicks > 0);
		if (action == KnightAction.NONE) {
			idleTicks++;
		} else {
			tickAction(level);
		}
		updateShield();
	}

	/** The shield goes up while he faces a target and is not attacking, and down for his attacks. */
	private void updateShield() {
		LivingEntity target = getTarget();
		boolean want = action == KnightAction.NONE && shieldDisabledTicks <= 0 && target != null && target.isAlive()
				&& distanceTo(target) <= settings().shieldRange() && getOffhandItem().has(DataComponents.BLOCKS_ATTACKS);
		if (want && !isUsingItem()) {
			startUsingItem(InteractionHand.OFF_HAND);
		} else if (!want && isUsingItem() && getUseItem().has(DataComponents.BLOCKS_ATTACKS)) {
			stopUsingItem();
		}
	}

	public void startAttack(Attack attack, LivingEntity target) {
		actionTarget = target;
		hitChecked = false;
		comboStops = false;
		SkeletonKnightSettings settings = settings();
		switch (attack) {
			case SLASH -> setAction(KnightAction.SLASH);
			case POKE -> setAction(KnightAction.POKE);
			case COMBO -> {
				comboCooldown = settings.comboCooldown();
				setAction(KnightAction.COMBO);
			}
			case HEAVY -> {
				heavyCooldown = settings.heavyCooldown();
				setAction(KnightAction.HEAVY_WINDUP);
			}
		}
	}

	private void setAction(KnightAction next) {
		action = next;
		actionTicks = 0;
		if (next != KnightAction.NONE) {
			stopUsingItem();
		}
		entityData.set(DATA_ACTION, (byte) next.ordinal());
	}

	private void endAction() {
		setAction(KnightAction.NONE);
		actionTarget = null;
		idleTicks = 0;
		dashTicks = 0;
	}

	private void tickAction(ServerLevel level) {
		int t = actionTicks++;
		LivingEntity target = actionTarget;
		if (t >= action.length()) {
			if (action == KnightAction.HEAVY_WINDUP && target != null && target.isAlive()) {
				setAction(KnightAction.HEAVY_STRIKE);
			} else {
				endAction();
			}
			return;
		}
		if (target == null || !target.isAlive()) {
			endAction();
			return;
		}
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		SkeletonKnightSettings settings = settings();
		closeGap(t, target, settings);
		switch (action) {
			case SLASH -> {
				if (t == KnightAction.SLASH_HIT) {
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F);
					hitIfInReach(level, target, settings.reach(), 1.0F, 0.0F);
					hitDone();
				}
			}
			case POKE -> {
				if (t == KnightAction.POKE_HIT) {
					playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.2F);
					hitIfInReach(level, target, pokeReach(), 1.0F, 0.3F);
					hitDone();
				}
			}
			case COMBO -> tickCombo(level, t, target, settings);
			case HEAVY_WINDUP -> {
				if (t == 0) playSound(SoundEvents.SKELETON_AMBIENT, 1.2F, 0.5F);
			}
			case HEAVY_STRIKE -> {
				if (t == KnightAction.HEAVY_HIT) heavyStrike(level, target, settings);
			}
			default -> {
			}
		}
	}

	private void tickCombo(ServerLevel level, int t, LivingEntity target, SkeletonKnightSettings settings) {
		if (t == 0 && onGround()) {
			// A leap in at the player.
			Vec3 to = horizontal(target.position().subtract(position()));
			double step = Math.min(0.6, horizontalDistanceTo(target) * 0.15);
			setDeltaMovement(to.x * step, 0.3, to.z * step);
		}
		for (int hit : KnightAction.COMBO_HITS) {
			if (t == hit) {
				playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + t * 0.01F);
				// The cuts come faster than the usual half-second of hurt immunity; each one must land.
				target.damageCooldownTime = 0;
				hitIfInReach(level, target, settings.reach() + 0.3, settings.comboHitMultiplier(), 0.0F);
				boolean stops = comboStops;
				hitDone();
				if (stops) {
					// The target got away and there is no dash left to follow: no more cuts into the air.
					endAction();
					return;
				}
			}
		}
		if (t == KnightAction.COMBO_KICK) {
			playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
			target.damageCooldownTime = 0;
			hitIfInReach(level, target, settings.reach(), settings.comboHitMultiplier() * 0.5F, 0.8F);
		}
		if (t == KnightAction.COMBO_BACKSTEP && onGround()) {
			Vec3 away = horizontal(position().subtract(target.position()));
			setDeltaMovement(away.x * settings.backstep(), 0.3, away.z * settings.backstep());
		}
	}

	/** The overhead cut: through a raised shield it lands only in part, and knocks the shield out of the guard. */
	private void heavyStrike(ServerLevel level, LivingEntity target, SkeletonKnightSettings settings) {
		playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
		double reach = settings.reach() + settings.heavyExtraReach();
		hitDone();
		if (!inReach(target, reach)) {
			return;
		}
		float multiplier = settings.heavyDamageMultiplier();
		ItemStack shield = target.getItemBlockingWith();
		BlocksAttacks blocks = shield != null ? shield.get(DataComponents.BLOCKS_ATTACKS) : null;
		if (blocks != null && FallenLordLogic.shieldCovers(target.getYHeadRot(), getX() - target.getX(), getZ() - target.getZ())) {
			multiplier *= settings.heavyBlockedShare();
			blocks.disable(level, target, settings.heavyShieldSeconds(), shield);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.7F);
		}
		hit(level, target, multiplier, 0.6F);
	}

	// ---- Dash ----

	/**
	 * A few ticks before each hit he checks where the target will be. Out of reach, he dashes at it if the dash is ready;
	 * otherwise the hit goes into the air, and a combo ends after that cut.
	 */
	private void closeGap(int t, LivingEntity target, SkeletonKnightSettings settings) {
		SkeletonKnightSettings.Dash dash = settings.dash();
		if (dashTicks > 0) {
			dashTicks--;
			Vec3 to = horizontal(target.position().subtract(position()));
			if (horizontalDistanceTo(target) > Math.max(1.2, hitReach(settings) - DASH_STOP_SHORT)) {
				setDeltaMovement(to.x * dash.speed(), getDeltaMovement().y, to.z * dash.speed());
			} else {
				dashTicks = 0;
			}
			return;
		}
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
		switch (SkeletonKnightLogic.reach(Math.sqrt(dx * dx + dz * dz), hitReach(settings), dashCooldown, dash)) {
			case DASH -> {
				dashTicks = dash.ticks();
				dashCooldown = dash.cooldown();
				playSound(SoundEvents.SKELETON_STEP, 1.5F, 0.6F);
			}
			case MISSES -> comboStops = action == KnightAction.COMBO;
			case HITS -> {
			}
		}
	}

	/** Called on each hit tick: the next hit is checked afresh. */
	private void hitDone() {
		hitChecked = false;
		comboStops = false;
		dashTicks = 0;
	}

	/** Ticks from tick {@code t} of the current action to its next hit, or -1 when no hit is left. */
	private int ticksToHit(int t) {
		return switch (action) {
			case SLASH -> t <= KnightAction.SLASH_HIT ? KnightAction.SLASH_HIT - t : -1;
			case POKE -> t <= KnightAction.POKE_HIT ? KnightAction.POKE_HIT - t : -1;
			case COMBO -> {
				for (int hit : KnightAction.COMBO_HITS) {
					if (hit >= t) yield hit - t;
				}
				yield -1;
			}
			case HEAVY_WINDUP -> action.length() - t + KnightAction.HEAVY_HIT;
			case HEAVY_STRIKE -> t <= KnightAction.HEAVY_HIT ? KnightAction.HEAVY_HIT - t : -1;
			default -> -1;
		};
	}

	/** The reach of the current action's hits. */
	private double hitReach(SkeletonKnightSettings settings) {
		return switch (action) {
			case COMBO -> settings.reach() + 0.3;
			case POKE -> pokeReach();
			case HEAVY_WINDUP, HEAVY_STRIKE -> settings.reach() + settings.heavyExtraReach();
			default -> settings.reach();
		};
	}

	private void hitIfInReach(ServerLevel level, LivingEntity target, double reach, float multiplier, float knockback) {
		if (inReach(target, reach)) {
			hit(level, target, multiplier, knockback);
		}
	}

	private boolean inReach(LivingEntity target, double reach) {
		return horizontalDistanceTo(target) <= reach + REACH_SLACK && Math.abs(target.getY() - getY()) < 2.5 && inFront(target)
				&& hasLineOfSight(target);
	}

	/** {@link #doHurtTarget} with the damage scaled: the sword, its enchantments and their after-effects all apply. */
	private boolean hit(ServerLevel level, LivingEntity target, float multiplier, float extraKnockback) {
		ItemStack weapon = getWeaponItem();
		DamageSource source = weapon.getDamageSource(this);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		damage = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage);
		damage += weapon.getItem().getAttackDamageBonus(target, damage, source);
		damage *= multiplier;
		Vec3 oldMovement = target.getDeltaMovement();
		if (!target.hurtServer(level, source, damage)) {
			return false;
		}
		causeExtraKnockback(target, getKnockback(target, source) + extraKnockback, oldMovement, source, damage, true);
		weapon.hurtEnemy(target, this);
		EnchantmentHelper.doPostAttackEffects(level, target, source);
		setLastHurtMob(target);
		return true;
	}

	// ---- Shield ----

	/**
	 * His shield blocks as soon as it is up, without the vanilla quarter-second delay: he raises it only for the short
	 * pauses between his attacks, and a hit on the raised shield must be blocked (and an axe knock it out).
	 */
	@Override
	public @Nullable ItemStack getItemBlockingWith() {
		return isUsingItem() && getUseItem().has(DataComponents.BLOCKS_ATTACKS) ? getUseItem() : null;
	}

	/** An axe knocks his shield down for a while; any other blocked hit he answers at once. */
	@Override
	protected void blockUsingItem(ServerLevel level, LivingEntity attacker, DamageSource source, float damage, boolean fullyBlocked) {
		super.blockUsingItem(level, attacker, source, damage, fullyBlocked);
		float seconds = attacker.getSecondsToDisableBlocking();
		if (seconds > 0.0F) {
			shieldDisabledTicks = SkeletonKnightLogic.shieldDisableTicks(seconds, settings().shieldDisableMultiplier());
			stopUsingItem();
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BREAK, getSoundSource(), 1.0F, 0.8F);
		} else {
			idleTicks = Math.max(idleTicks, settings().attackPause());
		}
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

	// ---- Sounds ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.SKELETON_STEP;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.8F;
	}
}
