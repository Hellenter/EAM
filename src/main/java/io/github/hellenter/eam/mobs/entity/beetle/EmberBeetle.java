package io.github.hellenter.eam.mobs.entity.beetle;

import io.github.hellenter.eam.content.EamParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * An ember beetle: a chicken-sized beetle with a shell of coal split by glowing red cracks, from the underworld's burnt
 * forest and lava caves. Small swarms of them wander about eating coal: lumps lying on the ground and the fuel out of
 * furnaces and ovens left near them. It leaves players alone unless one comes very close or hits one of the swarm;
 * its bite burns. The shell shrugs off cuts but cracks under axes, the mace and hammers, and water hurts it. When it dies
 * it bursts in sparks and sets the ground under it alight for a moment, and gives up the coal it ate.
 */
public class EmberBeetle extends Monster {
	/** The fuel slot of every furnace, smoker, blast furnace and EAM oven. */
	static final int FURNACE_FUEL_SLOT = 1;

	/** Coal it has eaten and carries in its belly. */
	private int eaten;

	public EmberBeetle(EntityType<? extends EmberBeetle> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		EmberBeetleSettings settings = EmberBeetleSettings.DEFAULT;
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.ARMOR, settings.armor())
				.add(Attributes.MOVEMENT_SPEED, settings.speed())
				.add(Attributes.ATTACK_DAMAGE, settings.damage())
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	/** Natural spawns: on solid ground in any light, lava glow included, and only now and then. */
	public static boolean checkEmberBeetleSpawnRules(EntityType<EmberBeetle> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		return reason != EntitySpawnReason.NATURAL || random.nextFloat() < EmberBeetleModule.settings().spawnChance();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		EmberBeetleSettings settings = EmberBeetleModule.settings();
		setBase(Attributes.MAX_HEALTH, settings.health());
		setBase(Attributes.ARMOR, settings.armor());
		setBase(Attributes.MOVEMENT_SPEED, settings.speed());
		setHealth(getMaxHealth());
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(4, new EatCoalGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> EmberBeetleLogic.notices(distanceToSqr(target), EmberBeetleModule.settings().noticeRange())));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && random.nextInt(6) == 0) {
			// Sparks rising off the cracks in its shell.
			level().addParticle(EamParticles.EMBER_BEETLE_SPARK, getRandomX(0.4), getY() + getBbHeight() * 0.8, getRandomZ(0.4),
					(random.nextDouble() - 0.5) * 0.01, 0.015 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
		}
	}

	/** Its belly holds more coal, and it is not hungry until it has room again. */
	boolean hungry() {
		return getTarget() == null && EmberBeetleLogic.hungry(eaten, EmberBeetleModule.settings().maxEaten());
	}

	/** Swallows one coal: heals, keeps it in its belly and crunches it in a spray of black crumbs. */
	void eatCoal(ServerLevel level) {
		EmberBeetleSettings settings = EmberBeetleModule.settings();
		eaten = EmberBeetleLogic.afterEating(eaten, settings.maxEaten());
		heal(settings.eatHeal());
		playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 0.6F + random.nextFloat() * 0.2F);
		level.sendParticles(EamParticles.EMBER_BEETLE_CRUMB, getX(), getY() + 0.2, getZ(), 8, 0.2, 0.05, 0.2, 0.05);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		EmberBeetleSettings settings = EmberBeetleModule.settings();
		// Datapacks may change the damage, so it is read on every bite.
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(settings.damage());
		boolean hit = super.doHurtTarget(level, target);
		if (hit && settings.burnSeconds() > 0) {
			target.igniteForSeconds(settings.burnSeconds());
		}
		return hit;
	}

	/** Crushing weapons crack the coal shell. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		ItemStack weapon = source.getWeaponItem();
		boolean crushing = weapon != null && weapon.is(EmberBeetleModule.CRUSHES_SHELLS);
		return super.hurtServer(level, source, EmberBeetleLogic.shellDamage(damage, crushing, EmberBeetleModule.settings().crushMultiplier()));
	}

	/** Water quenches it, as it does a blaze. */
	@Override
	public boolean isSensitiveToWater() {
		return true;
	}

	/** Bursts in sparks and sets the ground under it alight for a moment. */
	@Override
	public void die(DamageSource source) {
		boolean wasAlive = !isRemoved() && !dead;
		super.die(source);
		if (!wasAlive || !(level() instanceof ServerLevel level)) {
			return;
		}
		level.sendParticles(EamParticles.EMBER_BEETLE_SPARK, getX(), getY() + 0.3, getZ(), 24, 0.25, 0.15, 0.25, 0.12);
		playSound(SoundEvents.FIRECHARGE_USE, 0.7F, 1.4F + random.nextFloat() * 0.3F);
		BlockPos pos = blockPosition();
		if (EmberBeetleModule.settings().deathFire() && level.getGameRules().get(GameRules.MOB_GRIEFING)
				&& BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
			level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
		}
	}

	/** The coal it ate comes out with it. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (eaten > 0) {
			spawnAtLocation(level, new ItemStack(Items.COAL, eaten));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Eaten", eaten);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		eaten = input.getIntOr("Eaten", 0);
	}

	// A silverfish's clicks, lower: a beetle is bigger and its shell is hard.

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 0.7F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.6F;
	}
}
