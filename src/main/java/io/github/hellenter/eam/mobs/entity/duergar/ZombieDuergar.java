package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A zombie duergar: a grey dwarf miner who died in the old mines of the underworld and still shambles down their
 * tunnels, in whatever battered helm, torn mail or miner's leather he died in and with his rusted pick, axe or shovel.
 * Short, slow and not very strong, but rarely alone. His outfit, beard and hair are drawn by the model from one packed
 * int ({@link DuergarGear}); each worn piece adds a little armor. Three skins: grey, green-rotten and dried.
 */
public class ZombieDuergar extends Monster implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(ZombieDuergar.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(ZombieDuergar.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has. */
	public static final int SKINS = 3;

	public ZombieDuergar(EntityType<? extends ZombieDuergar> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		ZombieDuergarSettings settings = ZombieDuergarSettings.DEFAULT;
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.MOVEMENT_SPEED, settings.speed())
				.add(Attributes.ATTACK_DAMAGE, settings.attackDamage())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	private static ZombieDuergarSettings settings() {
		return DuergarModule.zombieSettings();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GEAR, 0);
		builder.define(DATA_SKIN, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, ZombieDuergar.class).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, DuergarVillager.class, true));
	}

	// ---- Spawning and gear ----

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		ZombieDuergarSettings settings = settings();
		setBase(Attributes.MAX_HEALTH, settings.health());
		setBase(Attributes.MOVEMENT_SPEED, settings.speed());
		setBase(Attributes.ATTACK_DAMAGE, settings.attackDamage());
		setHealth(getMaxHealth());
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		RandomSource random = getRandom();
		setGear(DuergarGear.scavenged(random::nextDouble, settings.pieceWeightArray(), settings.beltChance(), settings.baldChance()));
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
		setItemSlot(EquipmentSlot.MAINHAND, tool(DuergarGear.weighted(random::nextDouble, settings.weaponWeightArray())));
		setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		setDropChance(EquipmentSlot.MAINHAND, settings.weaponDropChance());
		return data;
	}

	/** A rusted pickaxe, axe or shovel, or nothing. */
	private static ItemStack tool(int index) {
		return switch (index) {
			case 0 -> new ItemStack(EamItems.RUSTY_IRON_PICKAXE);
			case 1 -> new ItemStack(EamItems.RUSTY_IRON_AXE);
			case 2 -> new ItemStack(EamItems.RUSTY_IRON_SHOVEL);
			default -> ItemStack.EMPTY;
		};
	}

	/** Gear comes from {@link #finalizeSpawn}; nothing vanilla on top. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
	}

	private void setBase(Holder<Attribute> attribute, double value) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	@Override
	public int gear() {
		return entityData.get(DATA_GEAR);
	}

	void setGear(int gear) {
		entityData.set(DATA_GEAR, gear);
	}

	@Override
	public int skin() {
		return Math.floorMod(entityData.get(DATA_SKIN), SKINS);
	}

	/** His armor is what is left on him: it counts, but it cannot be taken. */
	@Override
	public int getArmorValue() {
		return super.getArmorValue() + DuergarGear.armor(gear());
	}

	@Override
	public boolean canPickUpLoot() {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
		return false;
	}

	/** The dead miners shamble about together. */
	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return other instanceof ZombieDuergar || super.considersEntityAsAlly(other);
	}

	// ---- Sounds: a zombie's, lower ----

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ZOMBIE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ZOMBIE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.75F;
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Gear", gear());
		output.putInt("Skin", skin());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setGear(input.getIntOr("Gear", 0));
		entityData.set(DATA_SKIN, (byte) input.getIntOr("Skin", 0));
	}
}
