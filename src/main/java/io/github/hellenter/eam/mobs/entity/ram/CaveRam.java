package io.github.hellenter.eam.mobs.entity.ram;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A cave ram: a big, shaggy wild sheep with heavy horns curled round its ears, a grazer of the underworld's rocky and
 * frozen caves. It behaves like a boar: it leaves players alone, but hit one and it and the grown rams of its herd go
 * for you, faster than a boar and with a butt that throws you back much harder. Lambs only run. Duergar outcasts ride
 * them; under a rider a ram goes where he steers it.
 */
public class CaveRam extends Animal {
	/** Brought by a rider: it despawns like a monster even after he is gone, until it is bred or leashed. */
	private boolean mount;

	public CaveRam(EntityType<? extends CaveRam> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		CaveRamSettings settings = CaveRamSettings.DEFAULT;
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, settings.health())
				.add(Attributes.MOVEMENT_SPEED, settings.speed())
				.add(Attributes.ATTACK_DAMAGE, settings.damage())
				.add(Attributes.FOLLOW_RANGE, 16.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	/** Natural spawns: on solid ground, and only now and then so a herd does not turn up behind every corner. */
	public static boolean checkCaveRamSpawnRules(EntityType<CaveRam> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		return reason != EntitySpawnReason.NATURAL || random.nextFloat() < CaveRamModule.settings().spawnChance();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		CaveRamSettings settings = CaveRamModule.settings();
		setBase(Attributes.MAX_HEALTH, settings.health());
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
		goalSelector.addGoal(1, new PanicGoal(this, 1.25) {
			@Override
			public boolean canUse() {
				// Grown rams fight back; only lambs, and rams on fire, run.
				return (isBaby() || isOnFire()) && super.canUse();
			}
		});
		goalSelector.addGoal(2, new MeleeAttackGoal(this, CaveRamModule.settings().chaseSpeed(), true) {
			@Override
			public boolean canUse() {
				return !isBaby() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isBaby() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new BreedGoal(this, 1.0));
		goalSelector.addGoal(4, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	/**
	 * Underworld caves are dark and mostly bare rock: the vanilla animal value (grass, else light) would be negative
	 * almost everywhere, which stops spawning and makes rams crowd lit spots. Grazing ground is still preferred.
	 */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) ? 10.0F : 0.0F;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// Datapacks may change the damage, so it is read on every butt.
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(CaveRamModule.settings().damage());
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, getVoicePitch());
		}
		return hit;
	}

	@Override
	protected float getKnockback(Entity target, DamageSource damageSource) {
		return CaveRamModule.settings().knockback();
	}

	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target != null && getTarget() == null && !isBaby() && !isVehicle()) {
			playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0F, getVoicePitch());
		}
		super.setTarget(target);
	}

	/** A blow to a ridden ram turns its rider on whoever struck it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && getFirstPassenger() instanceof Mob rider && source.getEntity() instanceof LivingEntity attacker && attacker != rider
				&& rider.getTarget() == null && rider.canAttack(attacker)) {
			rider.setTarget(attacker);
		}
		return hurt;
	}

	/**
	 * A ram carrying a duergar despawns like any monster, as the Lost warhorse does: a rider never despawns while he
	 * rides, so the pair goes with the ram. Wild rams stay, as any animal.
	 */
	@Override
	public boolean removeWhenFarAway(double distSqr) {
		if (getFirstPassenger() instanceof Mob rider) {
			return !rider.isPersistenceRequired();
		}
		return mount && !isLeashed();
	}

	/** Marks a ram that came under a rider, so it does not stay behind for good when he is gone. */
	public void setMount() {
		mount = true;
	}

	@Override
	public void setInLove(@Nullable Player player) {
		super.setInLove(player);
		mount = false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Mount", mount);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		mount = input.getBooleanOr("Mount", false);
	}

	/** Its rider is thrown off as it falls, not when the body vanishes a second later. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		ejectPassengers();
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(CaveRamModule.FOOD);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return EamEntities.CAVE_RAM.create(level, EntitySpawnReason.BREEDING);
	}

	// A sheep's bleat, deeper: a ram is bigger than a sheep.

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SHEEP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SHEEP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SHEEP_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockState) {
		playSound(SoundEvents.SHEEP_STEP, 0.2F, 0.8F);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}
}
