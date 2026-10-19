package io.github.hellenter.eam.mobs.entity.leshy;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * A root the leshy calls out of the ground. It waits its turn in the line, then the ground churns where it will come up
 * for the warning time; then it bursts out, strikes whoever stands on it once, holds them a moment, and sinks back.
 * Never saved.
 */
public class LeshyRoot extends Entity {
	private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(LeshyRoot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_WARNING = SynchedEntityData.defineId(LeshyRoot.class, EntityDataSerializers.INT);
	/** Ticks it stands up out of the ground after it bursts, rising and sinking back included. */
	public static final int UP_TIME = 22;

	private @Nullable LivingEntity owner;
	private float damage;
	private int hold;
	private boolean struck;

	public LeshyRoot(EntityType<? extends LeshyRoot> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** A root for {@code owner} that waits {@code delay} ticks, churns the ground for {@code warning} and then bursts. */
	static LeshyRoot create(Level level, LivingEntity owner, double x, double y, double z, int delay, int warning, float damage, int hold) {
		LeshyRoot root = new LeshyRoot(EamEntities.LESHY_ROOT, level);
		root.owner = owner;
		root.damage = damage;
		root.hold = hold;
		root.entityData.set(DATA_DELAY, delay);
		root.entityData.set(DATA_WARNING, warning);
		root.setPos(x, y, z);
		root.setYRot(level.getRandom().nextFloat() * 360.0F);
		return root;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_DELAY, 0);
		builder.define(DATA_WARNING, 20);
	}

	/** The tick it bursts out of the ground. */
	public int burstTick() {
		return entityData.get(DATA_DELAY) + entityData.get(DATA_WARNING);
	}

	/** The tick the ground starts to churn. */
	public int warningTick() {
		return entityData.get(DATA_DELAY);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		int burst = burstTick();
		if (tickCount >= warningTick() && tickCount < burst && tickCount % 2 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState(level)), getX(), getY() + 0.05, getZ(), 4, 0.25, 0.02,
					0.25, 0.05);
		}
		if (tickCount == warningTick()) {
			level.playSound(null, getX(), getY(), getZ(), EamSounds.LESHY_ROOTS, SoundSource.HOSTILE, 0.5F, 0.6F + random.nextFloat() * 0.2F);
		}
		if (tickCount == burst) {
			level.playSound(null, getX(), getY(), getZ(), EamSounds.LESHY_ROOTS, SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState(level)), getX(), getY() + 0.3, getZ(), 12, 0.3, 0.3,
					0.3, 0.1);
		}
		if (!struck && tickCount >= burst && tickCount <= burst + 4) {
			strike(level);
		}
		if (tickCount >= burst + UP_TIME) {
			discard();
		}
	}

	private BlockState groundState(ServerLevel level) {
		BlockState state = level.getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
		return state.isAir() ? Blocks.ROOTED_DIRT.defaultBlockState() : state;
	}

	/** Whoever stands on it as it bursts is struck once and held. */
	private void strike(ServerLevel level) {
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2, 0.0, 0.2))) {
			if (living == owner || !living.isAlive() || LeshyModule.isLeshysWolf(living) || living instanceof Leshy) {
				continue;
			}
			DamageSource source = owner != null ? damageSources().mobAttack(owner) : damageSources().magic();
			if (living.hurtServer(level, source, damage)) {
				struck = true;
				if (hold > 0) {
					living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, hold, 3), owner);
				}
			}
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
