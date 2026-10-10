package io.github.hellenter.eam.mobs.entity.wight;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A gaunt, hunched body in rags with long clawed arms. It picks one player and stalks them from a distance, invisible
 * the whole time except for the moment it strikes. Now and then, when the player is busy (in a crafting table or chest,
 * eating, drinking), it darts in for a couple of blows and runs. A player under a third of their health is hunted down:
 * pass after pass from behind. It runs 1.7 times as fast as a sprinting player, so it cannot be caught; but fire undoes
 * it: burning, it shows itself, stops attacking, staggers about groaning looking for water and takes double damage.
 * The sun does not hurt it.
 */
public class Wight extends Monster {
	/**
	 * A mob's walk speed goes with the square of this attribute (it is both the speed and the walk input), a player's
	 * with its own: 0.465² ≈ 1.7 × a sprinting player's 0.13 × 0.98.
	 */
	static final double MOVEMENT_SPEED = 0.465;
	private static final double FOLLOW_RANGE = 48.0;
	/** A fresh wight watches a while before its first raid. */
	private static final int FIRST_RAID_DELAY = 600;

	private int raidCooldown = FIRST_RAID_DELAY;
	private int huntCooldown;
	/** Ticks since its last blow; it turns invisible again once this reaches the recloak delay. */
	private int sinceStrike = Integer.MAX_VALUE / 2;
	private int groanTimer;
	private boolean fleeRequested;
	private @Nullable Player stalked;

	public Wight(EntityType<? extends Wight> type, Level level) {
		super(type, level);
		xpReward = 15;
		setCanPickUpLoot(false);
		// It does not mind stepping through fire, which is how it gets caught.
		setPathfindingMalus(PathType.FIRE, 0.0F);
		setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	static WightSettings settings() {
		return WightModule.settings();
	}

	/**
	 * Natural spawns: only from the configured day of the world, only now and then, and never more wights in the level
	 * than there are players in it. Light does not matter: it does not mind the sun.
	 */
	public static boolean checkSpawnRules(EntityType<Wight> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		WightSettings settings = settings();
		ServerLevel server = level.getLevel();
		int day = BloodMoonLogic.dayNumber(server.getServer().overworld().getOverworldClockTime());
		if (!WightLogic.oldEnough(day, settings.minDay()) || random.nextFloat() >= settings.spawnChance()) {
			return false;
		}
		return server.getEntities(EamEntities.WIGHT, wight -> wight.isAlive()).size() < server.players().size();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new WightStalkGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		cloak();
		return data;
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

	/** It roams and spawns in any light. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	// ---- The stalked player ----

	@Nullable Player stalked() {
		return stalked;
	}

	private void updateStalked(ServerLevel level) {
		double giveUp = settings().giveUpDistance();
		if (stalked != null && (!stalked.isAlive() || stalked.isRemoved() || stalked.level() != level
				|| !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(stalked) || stalked.distanceToSqr(this) > giveUp * giveUp)) {
			stalked = null;
		}
		if (stalked == null && (tickCount + getId()) % 20 == 0) {
			stalked = level.getNearestPlayer(getX(), getY(), getZ(), FOLLOW_RANGE, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		}
	}

	/** Whether {@code player} has their hands full: a crafting table, chest or other menu open, or eating or drinking. */
	static boolean busy(Player player) {
		if (player.containerMenu != player.inventoryMenu) {
			return true;
		}
		if (!player.isUsingItem()) {
			return false;
		}
		ItemUseAnimation animation = player.getUseItem().getUseAnimation();
		return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
	}

	// ---- Cooldowns, for the goal ----

	int raidCooldown() {
		return raidCooldown;
	}

	int huntCooldown() {
		return huntCooldown;
	}

	void raided() {
		raidCooldown = settings().raidCooldown();
	}

	void huntPassDone() {
		huntCooldown = settings().huntCooldown();
	}

	/** True once after it was hurt, so the goal makes it run. */
	boolean takeFleeRequest() {
		boolean value = fleeRequested;
		fleeRequested = false;
		return value;
	}

	// ---- Invisibility ----

	private void cloak() {
		if (!hasEffect(MobEffects.INVISIBILITY)) {
			addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
	}

	private void decloak() {
		if (hasEffect(MobEffects.INVISIBILITY)) {
			removeEffect(MobEffects.INVISIBILITY);
		}
	}

	// ---- Server tick ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (raidCooldown > 0) raidCooldown--;
		if (huntCooldown > 0) huntCooldown--;
		if (sinceStrike < Integer.MAX_VALUE / 2) sinceStrike++;
		updateStalked(level);
		WightSettings settings = settings();
		if (isOnFire()) {
			decloak();
			if (--groanTimer <= 0) {
				groanTimer = settings.groanInterval() + getRandom().nextInt(settings.groanInterval() / 2 + 1);
				playSound(EamSounds.WIGHT_GROAN, 1.6F, getVoicePitch());
			}
		} else {
			groanTimer = 0;
			if (sinceStrike >= settings.recloakDelay()) {
				cloak();
			}
		}
	}

	/** A blow with its claws; it shows itself for it. */
	void strike(ServerLevel level, Player target) {
		decloak();
		sinceStrike = 0;
		swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
		playSound(EamSounds.WIGHT_ATTACK, 1.0F, getVoicePitch());
		doHurtTarget(level, target);
	}

	/** Fire is its bane: while it burns every blow hurts it more. Any hurt sends it running for a while. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		float taken = WightLogic.damageTaken(damage, isOnFire(), settings().burningDamage());
		boolean hurt = super.hurtServer(level, source, taken);
		if (hurt && !isOnFire()) {
			fleeRequested = true;
		}
		return hurt;
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RaidCooldown", raidCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		raidCooldown = input.getIntOr("RaidCooldown", FIRST_RAID_DELAY);
	}

	// ---- Sounds ----

	/** Unseen, it keeps quiet but for a rare rasping breath. */
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return EamSounds.WIGHT_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return EamSounds.WIGHT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return EamSounds.WIGHT_DEATH;
	}
}
