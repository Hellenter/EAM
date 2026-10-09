package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The shield the Fallen Lord drops when phase two starts. It falls and lies flat at full size, facing up, instead of
 * spinning like a dropped item; nobody can pick it up, and it is gone after as long as a dropped item would last.
 */
public class LordShield extends Entity {
	private static final int LIFETIME = 6000;

	private int age;

	public LordShield(EntityType<? extends LordShield> type, Level level) {
		super(type, level);
	}

	public LordShield(Level level, double x, double y, double z, float yaw) {
		this(EamEntities.LORD_SHIELD, level);
		setPos(x, y, z);
		setYRot(yaw);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected double getDefaultGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		super.tick();
		applyGravity();
		move(MoverType.SELF, getDeltaMovement());
		double friction = onGround() ? 0.5 : 0.98;
		setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
		if (!level().isClientSide() && ++age >= LIFETIME) {
			discard();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		age = input.getIntOr("Age", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("Age", age);
	}
}
