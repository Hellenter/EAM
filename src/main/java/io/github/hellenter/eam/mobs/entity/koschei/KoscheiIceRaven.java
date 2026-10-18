package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.draugr.Draugr;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlockLogic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A raven of ice Koschei looses in his second phase: it leaves his hand at an angle and curves in after the target like the
 * undead warlock's missiles, and shatters on it, hurting it and slowing it a level more. A raised shield turned its way
 * takes it, except from the one with a dark heart, which knocks the shield out instead. An arrow or a blow brings one
 * down on its way. It flies over the ground and walls, never into them. Never saved.
 */
public class KoscheiIceRaven extends Monster {
	private static final EntityDataAccessor<Boolean> DATA_BREAKER = SynchedEntityData.defineId(KoscheiIceRaven.class,
			EntityDataSerializers.BOOLEAN);
	/** How close its box has to come to the target's box to strike. */
	private static final double REACH = 0.2;
	/** Slowness goes no higher than this level. */
	private static final int MAX_SLOWNESS = 4;

	private @Nullable Koschei owner;
	private PhaseTwoSettings.IceRavens settings = PhaseTwoSettings.IceRavens.DEFAULT;

	public KoscheiIceRaven(EntityType<? extends KoscheiIceRaven> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, PhaseTwoSettings.IceRavens.DEFAULT.health())
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREAKER, false);
	}

	/** Sets it on {@code target} for {@code owner}; a {@code breaker} knocks a shield out. */
	void summon(Koschei owner, LivingEntity target, PhaseTwoSettings.IceRavens settings, boolean breaker) {
		this.owner = owner;
		this.settings = settings;
		setTarget(target);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(settings.health());
		setHealth(settings.health());
		entityData.set(DATA_BREAKER, breaker);
	}

	/** Whether it knocks a shield out rather than shattering on it. */
	public boolean breaker() {
		return entityData.get(DATA_BREAKER);
	}

	@Override
	public void tick() {
		noPhysics = true;
		super.tick();
		noPhysics = false;
		setNoGravity(true);
		if (level().isClientSide() && random.nextInt(2) == 0) {
			level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(0.5), getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = getTarget();
		if (owner == null || !owner.isAlive() || tickCount > settings.lifetime() || target == null || !target.isAlive()
				|| target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			shatter(level);
			return;
		}
		Vec3 to = target.getBoundingBox().getCenter().subtract(position().add(0.0, getBbHeight() * 0.5, 0.0));
		Vec3 v = getDeltaMovement();
		double[] next = UndeadWarlockLogic.steer(v.x, v.y, v.z, to.x, to.y, to.z, settings.speed(), settings.turn());
		setDeltaMovement(next[0], next[1], next[2]);
		if (!level.noCollision(this, getBoundingBox().move(getDeltaMovement()).inflate(0.1))) {
			// Never into the ground or a wall: it rises over them instead.
			setDeltaMovement(next[0] * 0.6, Math.max(next[1], settings.speed() * 0.6), next[2] * 0.6);
		}
		face(getDeltaMovement());
		if (getBoundingBox().inflate(REACH).intersects(target.getBoundingBox())) {
			strike(level, target);
		}
	}

	private void face(Vec3 v) {
		if (v.horizontalDistanceSqr() > 1.0E-4) {
			float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
			setYRot(yaw);
			yBodyRot = yaw;
			setYHeadRot(yaw);
		}
		setXRot((float) (-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG) * 0.6F);
	}

	private void strike(ServerLevel level, LivingEntity target) {
		Vec3 v = getDeltaMovement();
		ItemStack shield = target.getItemBlockingWith();
		if (shield != null && shield.has(DataComponents.BLOCKS_ATTACKS) && FallenLordLogic.shieldCovers(target.getYHeadRot(), -v.x, -v.z)) {
			if (breaker()) {
				BlocksAttacks blocks = shield.get(DataComponents.BLOCKS_ATTACKS);
				if (blocks != null) {
					blocks.disable(level, target, settings.shieldSeconds(), shield);
				}
				level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.0F, 1.2F);
			} else {
				level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 1.4F);
			}
			shatter(level);
			return;
		}
		target.damageCooldownTime = 0;
		if (target.hurtServer(level, damageSources().source(KoscheiModule.SORCERY_DAMAGE, this, owner), settings.damage())) {
			MobEffectInstance slow = target.getEffect(MobEffects.SLOWNESS);
			int level0 = KoscheiLogic.stackedSlowness(slow == null ? -1 : slow.getAmplifier(), MAX_SLOWNESS);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(settings.slownessSeconds() * 20.0F), level0), owner);
		}
		shatter(level);
	}

	/** Bursts into shards of ice. */
	private void shatter(ServerLevel level) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX(), getY(0.5), getZ(), 16,
				0.2, 0.2, 0.2, 0.1);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(0.5), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.4F + random.nextFloat() * 0.3F);
		discard();
	}

	/** Koschei, his draugr and his other sorcery do not bring it down. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker instanceof Koschei || attacker instanceof Draugr draugr && owner != null && draugr.servedBy(owner)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected void tickDeath() {
		if (level() instanceof ServerLevel level) {
			shatter(level);
		}
	}

	@Override
	public void travel(Vec3 input) {
		travelFlying(input, 0.0F);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GLASS_HIT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return null;
	}
}
