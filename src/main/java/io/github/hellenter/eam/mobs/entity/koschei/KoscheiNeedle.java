package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Koschei's needle, used: it flies a little way out of the hand, stops and hangs spinning in the air in a pillar of light
 * that reaches up into the sky and down through the ground, wound round with a spiral running down it, then bursts.
 * After the burst a chain of blasts drives straight down the light's path, one under another, through everything to the
 * bedrock, through the underworld's deep bedrock too. Whoever used it has the time it spins to get away. Not saved:
 * unloaded, it is gone.
 */
public class KoscheiNeedle extends Entity implements ItemSupplier {
	/** How far down under it the chain of blasts has gone, in blocks; the light reaches down that far after the burst. */
	private static final EntityDataAccessor<Integer> DATA_DEPTH = SynchedEntityData.defineId(KoscheiNeedle.class, EntityDataSerializers.INT);
	/** It has burst: the needle is gone, only the light stays while the blasts go down it. */
	private static final EntityDataAccessor<Boolean> DATA_BURST = SynchedEntityData.defineId(KoscheiNeedle.class, EntityDataSerializers.BOOLEAN);
	/** It slows to a stop in the air over this many ticks after leaving the hand. */
	public static final int FLIGHT_TICKS = 10;

	/** The next layer the chain clears, as a block height. */
	private int nextLayer;
	/** The block height where it burst. */
	private int burstY;
	private int nextBlast;

	public KoscheiNeedle(EntityType<? extends KoscheiNeedle> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public KoscheiNeedle(Level level, Vec3 at, Vec3 velocity) {
		this(EamEntities.KOSCHEI_NEEDLE, level);
		setPos(at.x, at.y, at.z);
		setDeltaMovement(velocity);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_DEPTH, 0);
		builder.define(DATA_BURST, false);
	}

	/** Client: how far down under it the blasts have gone. */
	public int depth() {
		return entityData.get(DATA_DEPTH);
	}

	/** Client: it has burst, and only its light is left. */
	public boolean burst() {
		return entityData.get(DATA_BURST);
	}

	/** Client: ticks since it was used, for the spin. */
	public int age() {
		return tickCount;
	}

	@Override
	public ItemStack getItem() {
		return EamItems.KOSCHEI_NEEDLE.getDefaultInstance();
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 v = getDeltaMovement();
		if (tickCount < FLIGHT_TICKS) {
			setPos(position().add(v));
			setDeltaMovement(v.scale(0.75));
		} else {
			setDeltaMovement(Vec3.ZERO);
		}
		if (level() instanceof ServerLevel level) {
			serverTick(level);
		} else if (tickCount >= FLIGHT_TICKS && !burst()) {
			clientEffects();
		}
	}

	private void serverTick(ServerLevel level) {
		KoscheiSettings.Needle settings = KoscheiModule.settings().needle();
		int burstAt = FLIGHT_TICKS + settings.fuse();
		if (tickCount == FLIGHT_TICKS) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 3.0F, 0.6F);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.NEUTRAL, 2.0F, 1.4F);
		}
		if (tickCount > FLIGHT_TICKS && tickCount < burstAt && tickCount % 10 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.NEUTRAL, 2.0F, 1.0F + (tickCount - FLIGHT_TICKS) / 60.0F);
		}
		if (tickCount == burstAt) {
			entityData.set(DATA_BURST, true);
			burstY = BlockPos.containing(position()).getY();
			nextLayer = burstY;
			nextBlast = tickCount + settings.chainInterval();
			level.explode(this, getX(), getY(), getZ(), settings.explosionPower(), Level.ExplosionInteraction.TNT);
		} else if (tickCount > burstAt && tickCount >= nextBlast) {
			nextBlast = tickCount + settings.chainInterval();
			if (!blastDown(level, settings)) {
				discard();
			}
		}
	}

	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return true;
	}

	/**
	 * The next blast of the chain: clears the shaft one step further down and goes off at its bottom. False once the
	 * chain has reached plain bedrock or the floor of the world.
	 */
	private boolean blastDown(ServerLevel level, KoscheiSettings.Needle settings) {
		int radius = settings.shaftRadius();
		int x0 = BlockPos.containing(position()).getX();
		int z0 = BlockPos.containing(position()).getZ();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean blocked = false;
		int bottom = nextLayer;
		for (int i = 0; i < settings.chainStep() && !blocked; i++) {
			if (nextLayer < level.getMinY()) {
				blocked = true;
				break;
			}
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					pos.set(x0 + dx, nextLayer, z0 + dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					if (!KoscheiLogic.needlePierces(state.getDestroySpeed(level, pos), state.is(EamBlocks.DEEP_BEDROCK))) {
						// Plain bedrock: the chain ends here.
						blocked = true;
						continue;
					}
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			bottom = nextLayer;
			if (!blocked) {
				nextLayer--;
			}
		}
		if (bottom < level.getMinY()) {
			return false;
		}
		Vec3 at = new Vec3(x0 + 0.5, bottom + 0.5, z0 + 0.5);
		entityData.set(DATA_DEPTH, (int) Math.ceil(getY()) - bottom);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 20, 0.6, 0.6, 0.6, 0.15);
		level.explode(this, at.x, at.y, at.z, settings.chainPower(), Level.ExplosionInteraction.TNT);
		return !blocked;
	}

	private void clientEffects() {
		Level level = level();
		double angle = tickCount * 0.6;
		for (int i = 0; i < 2; i++) {
			double a = angle + i * Math.PI;
			level.addParticle(ParticleTypes.END_ROD, getX() + Math.cos(a) * 0.6, getY() + (random.nextDouble() - 0.5) * 0.6, getZ() + Math.sin(a) * 0.6,
					0.0, 0.0, 0.0);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3,
					(random.nextDouble() - 0.5) * 0.3);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 65536.0;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
