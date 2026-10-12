package io.github.hellenter.eam.mobs.entity.drowned;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrGear;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * A drowned soldier: a bony corpse long drowned in a bog, pulled out by Baba Yaga. Built like a draugr and wearing the
 * same kinds of armor pieces ({@link DraugrGear}), but more of them (three to five), rusted, torn and silted; a little
 * more health than a draugr; a rusted sword or axe. It neither burns in the sun nor drops its gear. Not spawned in the
 * world on its own: Baba Yaga raises it, or a spawn egg.
 */
public class DrownedSoldier extends Monster {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DrownedSoldier.class, EntityDataSerializers.INT);
	/** Weights of wearing one to five armor pieces: never fewer than three. */
	private static final double[] PIECE_WEIGHTS = {0.0, 0.0, 1.0, 2.0, 2.0};
	private static final double OWN_PIECE_CHANCE = 0.6;
	private static final double BELT_CHANCE = 0.6;

	public DrownedSoldier(EntityType<? extends DrownedSoldier> type, Level level) {
		super(type, level);
		xpReward = 8;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			setDropChance(slot, 0.0F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 26.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 1.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		setCanPickUpLoot(false);
		equip(level.getRandom());
		return data;
	}

	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		// Gear is fixed, set in finalizeSpawn.
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GEAR, 0);
	}

	public int gear() {
		return entityData.get(DATA_GEAR);
	}

	/** His armor is what is left on him: it counts, but it cannot be taken. */
	@Override
	public int getArmorValue() {
		return super.getArmorValue() + DraugrGear.armor(gear());
	}

	/** Three to five rusted pieces of armor and a rusted sword or axe. */
	private void equip(RandomSource random) {
		entityData.set(DATA_GEAR, DraugrGear.choose(random::nextDouble, PIECE_WEIGHTS, OWN_PIECE_CHANCE, BELT_CHANCE));
		Item weapon = random.nextFloat() < 0.65F ? EamItems.RUSTY_IRON_SWORD : EamItems.RUSTY_IRON_AXE;
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Gear", gear());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_GEAR, input.getIntOr("Gear", 0));
	}

	// ---- Sounds: a drowned's gurgle, lower ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.DROWNED_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DROWNED_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DROWNED_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}
}
