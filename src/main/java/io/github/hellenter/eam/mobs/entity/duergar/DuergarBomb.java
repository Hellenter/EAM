package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A duergar bomb with its fuse lit. It lands and lies hissing until the fuse burns down, then bursts: full damage at its
 * centre, less the farther away, none past the radius, and only through open air. It breaks no blocks; a renegade's
 * bomb does not hurt renegades.
 */
public class DuergarBomb extends ThrowableItemProjectile {
	private int fuse = DuergarRenegadeAttacks.Bomb.DEFAULT.fuseTicks();
	private float damage = DuergarRenegadeAttacks.Bomb.DEFAULT.damage();
	private double radius = DuergarRenegadeAttacks.Bomb.DEFAULT.radius();
	private boolean hitSomeone;

	public DuergarBomb(EntityType<? extends DuergarBomb> type, Level level) {
		super(type, level);
	}

	public DuergarBomb(Level level, LivingEntity owner, ItemStack stack) {
		super(EamEntities.DUERGAR_BOMB, owner, level, stack);
	}

	/** Sets the fuse and the blast from the renegade's numbers. */
	void arm(DuergarRenegadeAttacks.Bomb settings) {
		fuse = settings.fuseTicks();
		damage = settings.damage();
		radius = settings.radius();
	}

	@Override
	protected Item getDefaultItem() {
		return EamItems.DUERGAR_BOMB;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			// The burning fuse on top.
			Vec3 top = position().add(0.0, getBbHeight() + 0.1, 0.0);
			level().addParticle(ParticleTypes.SMOKE, top.x, top.y, top.z, 0.0, 0.03, 0.0);
			if (tickCount % 2 == 0) {
				level().addParticle(ParticleTypes.SMALL_FLAME, top.x, top.y, top.z, 0.0, 0.01, 0.0);
			}
			return;
		}
		if (isNoGravity() && level().noCollision(this, getBoundingBox().move(0.0, -0.1, 0.0))) {
			// The floor under it is gone: it falls again.
			setNoGravity(false);
		}
		if (tickCount % 10 == 0) {
			playSound(SoundEvents.TNT_PRIMED, 0.25F, 1.6F);
		}
		if (--fuse <= 0 && level() instanceof ServerLevel level) {
			burst(level);
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (hitSomeone || getOwner() instanceof DuergarRenegade && (entity instanceof DuergarRenegade || entity instanceof DuergarRamRider)) {
			return false;
		}
		return super.canHitEntity(entity);
	}

	/** It strikes someone and drops at their feet. */
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		hitSomeone = true;
		Vec3 motion = getDeltaMovement();
		setDeltaMovement(0.0, Math.min(0.0, motion.y), 0.0);
	}

	/** On a floor it stops; off a wall or a ceiling it bounces and falls. */
	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Direction face = hit.getDirection();
		Vec3 motion = getDeltaMovement();
		Vec3 at = hit.getLocation();
		if (face == Direction.UP) {
			setPos(at.x, at.y, at.z);
			setDeltaMovement(Vec3.ZERO);
			setNoGravity(true);
			if (motion.lengthSqr() > 0.01) {
				playSound(SoundEvents.CHAIN_HIT, 0.8F, 0.6F);
			}
			return;
		}
		setPos(at.x + face.getStepX() * 0.1, at.y + face.getStepY() * 0.1 - (face == Direction.DOWN ? getBbHeight() : 0.0),
				at.z + face.getStepZ() * 0.1);
		setDeltaMovement(switch (face.getAxis()) {
			case X -> new Vec3(-motion.x * 0.3, motion.y, motion.z * 0.3);
			case Z -> new Vec3(motion.x * 0.3, motion.y, -motion.z * 0.3);
			case Y -> new Vec3(motion.x * 0.3, 0.0, motion.z * 0.3);
		});
	}

	private void burst(ServerLevel level) {
		Vec3 centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
		Entity owner = getOwner();
		DamageSource source = damageSources().explosion(this, owner);
		AABB area = new AABB(centre, centre).inflate(radius);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
			if (owner instanceof DuergarRenegade && (living instanceof DuergarRenegade || living instanceof DuergarRamRider)) {
				continue;
			}
			double distance = Math.sqrt(living.getBoundingBox().distanceToSqr(centre));
			float amount = DuergarLogic.blastDamage(damage, distance, radius) * ServerExplosion.getSeenPercent(centre, living);
			if (amount <= 0.0F) {
				continue;
			}
			living.hurtServer(level, source, amount);
			Vec3 push = living.position().subtract(centre);
			if (push.lengthSqr() > 1.0E-4) {
				double strength = 0.8 * amount / damage * (1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
				living.push(push.normalize().scale(strength).add(0.0, 0.2 * strength, 0.0));
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 2, 0.4, 0.3, 0.4, 0.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y, centre.z, 16, 0.6, 0.4, 0.6, 0.02);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.1F);
		level.gameEvent(this, GameEvent.EXPLODE, centre);
		discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Fuse", fuse);
		output.putFloat("Damage", damage);
		output.putDouble("Radius", radius);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		fuse = input.getIntOr("Fuse", fuse);
		damage = input.getFloatOr("Damage", damage);
		radius = input.getDoubleOr("Radius", radius);
	}
}
