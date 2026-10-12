package io.github.hellenter.eam.mobs.entity.crossbowman;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An elite crossbowman: a pillager in a cuirass and a round helmet. Besides a plain bolt he shoots a burst of three
 * weaker bolts, a red bolt after his crossbow glows red for a moment (twice the damage and speed, through a shield at
 * half damage) and, rarely, a net that leaves a sticky net under a player. He keeps his distance and backs away from
 * those who come close. Numbers are in {@code data/eam/mobs/elite_crossbowman.json}.
 */
public class EliteCrossbowman extends Pillager {
	/** His crossbow glows red: the charged shot is coming. */
	private static final EntityDataAccessor<Boolean> DATA_RED_CHARGE = SynchedEntityData.defineId(EliteCrossbowman.class,
			EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions RED_SPARK = new DustParticleOptions(0xFF2A10, 1.0F);

	public EliteCrossbowman(EntityType<? extends EliteCrossbowman> type, Level level) {
		super(type, level);
		xpReward = settings().stats().xp();
	}

	public static AttributeSupplier.Builder createAttributes() {
		EliteCrossbowmanSettings.Stats stats = EliteCrossbowmanSettings.DEFAULT.stats();
		return Monster.createMonsterAttributes()
				.add(Attributes.MOVEMENT_SPEED, stats.speed())
				.add(Attributes.MAX_HEALTH, stats.health())
				.add(Attributes.ARMOR, stats.armor())
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.FOLLOW_RANGE, stats.followRange());
	}

	static EliteCrossbowmanSettings settings() {
		return EliteCrossbowmanModule.settings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_RED_CHARGE, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// His own shooting replaces the vanilla crossbow goal, at its priority.
		goalSelector.removeAllGoals(goal -> goal instanceof RangedCrossbowAttackGoal<?>);
		goalSelector.addGoal(3, new EliteCrossbowGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		EliteCrossbowmanSettings.Stats stats = settings().stats();
		setBaseValue(Attributes.MAX_HEALTH, stats.health());
		setBaseValue(Attributes.ARMOR, stats.armor());
		setBaseValue(Attributes.MOVEMENT_SPEED, stats.speed());
		setBaseValue(Attributes.FOLLOW_RANGE, stats.followRange());
		setHealth(getMaxHealth());
		xpReward = stats.xp();
		return data;
	}

	private void setBaseValue(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	public boolean isRedCharging() {
		return entityData.get(DATA_RED_CHARGE);
	}

	void setRedCharging(boolean red) {
		entityData.set(DATA_RED_CHARGE, red);
	}

	/** The crossbow in his hands, or an empty stack. */
	ItemStack crossbow() {
		return getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.CROSSBOW));
	}

	/** Shows the crossbow loaded or empty: the model draws the bolt in it. */
	void setLoaded(boolean loaded) {
		ItemStack crossbow = crossbow();
		if (crossbow.is(Items.CROSSBOW)) {
			crossbow.set(DataComponents.CHARGED_PROJECTILES, loaded ? ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW)))
					: ChargedProjectiles.EMPTY);
		}
	}

	/** Red sparks round the crossbow while the charged shot winds up. */
	void sparkle(ServerLevel level) {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double x = getX() - Mth.sin(yaw) * 0.6;
		double z = getZ() + Mth.cos(yaw) * 0.6;
		level.sendParticles(RED_SPARK, x, getY() + 1.3, z, 2, 0.15, 0.1, 0.15, 0.0);
	}

	/** A plain bolt: the normal shot and each one of a burst. */
	void shootBolt(ServerLevel level, LivingEntity target, float damage, float speed, float inaccuracy) {
		ItemStack crossbow = crossbow();
		Arrow arrow = new Arrow(level, this, new ItemStack(Items.ARROW), crossbow.isEmpty() ? null : crossbow);
		arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
		arrow.setBaseDamage(EliteCrossbowmanLogic.baseDamage(damage, speed));
		aim(arrow, target, speed, inaccuracy, EliteCrossbowmanLogic.ARROW_GRAVITY);
		level.addFreshEntity(arrow);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
	}

	/** The red bolt: fast, hard, and through a shield at a cut. */
	void shootRedBolt(ServerLevel level, LivingEntity target, float damage, float speed, float shieldMultiplier) {
		ItemStack crossbow = crossbow();
		RedBolt bolt = new RedBolt(level, this, crossbow.isEmpty() ? null : crossbow, damage, shieldMultiplier);
		aim(bolt, target, speed, 1.0F, EliteCrossbowmanLogic.ARROW_GRAVITY);
		level.addFreshEntity(bolt);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.2F, 0.6F);
		playSound(SoundEvents.FIRECHARGE_USE, 0.6F, 1.6F);
	}

	/** A net: it leaves a sticky net under the one it hits. */
	void shootNet(ServerLevel level, LivingEntity target, float speed) {
		NetShot net = new NetShot(level, this);
		aim(net, target, speed, 2.0F, EliteCrossbowmanLogic.THROWN_GRAVITY);
		level.addFreshEntity(net);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.7F);
	}

	private void aim(Projectile projectile, LivingEntity target, float speed, float inaccuracy, double gravity) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		double dy = target.getY(1.0 / 3.0) - projectile.getY() + flat * EliteCrossbowmanLogic.dropCompensation(speed, gravity);
		projectile.shoot(dx, dy, dz, speed, inaccuracy);
	}
}
