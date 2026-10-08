package io.github.hellenter.eam.content.ground;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.slingshot.SlingshotLogic;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown pebble: flies like a snowball, hurts for half a heart and falls to the ground as an item.
 * Shot from a slingshot it hurts more, less the farther it flew.
 */
public class ThrownPebble extends ThrowableItemProjectile {
	/** Half a heart. */
	public static final float DAMAGE = 1.0F;

	private static final byte HIT_EVENT = 3;

	/** Draw power of the slingshot that shot it; 0 when thrown by hand. */
	private float slingshotPower;
	/** Where the slingshot shot it from, for damage falloff. */
	private Vec3 shotFrom = Vec3.ZERO;

	public ThrownPebble(EntityType<? extends ThrownPebble> type, Level level) {
		super(type, level);
	}

	public ThrownPebble(Level level, LivingEntity owner, ItemStack itemStack) {
		super(EamEntities.PEBBLE, owner, level, itemStack);
	}

	/** Marks this pebble as shot from a slingshot drawn to the given power, from where it is now. */
	public void fromSlingshot(float power) {
		slingshotPower = power;
		shotFrom = position();
	}

	@Override
	protected Item getDefaultItem() {
		return EamItems.PEBBLE;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == HIT_EVENT && !getItem().isEmpty()) {
			ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(getItem()));
			for (int i = 0; i < 8; i++) {
				level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			float damage = slingshotPower > 0.0F
					? SlingshotLogic.damage(slingshotPower, shotFrom.distanceTo(hitResult.getEntity().position()))
					: DAMAGE;
			hitResult.getEntity().hurtServer(serverLevel, damageSources().thrown(this, getOwner()), damage);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.broadcastEntityEvent(this, HIT_EVENT);
			if (!(getOwner() instanceof Player player && player.hasInfiniteMaterials())) {
				spawnAtLocation(serverLevel, getItem());
			}
			discard();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (slingshotPower > 0.0F) {
			output.putFloat("SlingshotPower", slingshotPower);
			output.store("ShotFrom", Vec3.CODEC, shotFrom);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		slingshotPower = input.getFloatOr("SlingshotPower", 0.0F);
		shotFrom = input.read("ShotFrom", Vec3.CODEC).orElse(Vec3.ZERO);
	}
}
