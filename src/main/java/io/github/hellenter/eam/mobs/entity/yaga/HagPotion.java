package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.content.EamEntities;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Baba Yaga's poison flask: it bursts wider than a splash potion and leaves whoever it reaches poisoned and sick.
 * Her own creatures are spared. A player who hits or raises a shield just before it reaches them sends it back: it flies
 * straight at her and bursts on her, stunning her for a while without hurting her.
 */
public class HagPotion extends ThrowableItemProjectile {
	/** The murky green of her brew. */
	public static final int COLOR = 0x4E7A1E;
	private static final EntityDataAccessor<Boolean> DATA_REFLECTED = SynchedEntityData.defineId(HagPotion.class, EntityDataSerializers.BOOLEAN);
	/** A flask sent back that has not reached her after this many ticks bursts in the air. */
	private static final int REFLECTED_LIFE = 100;
	/** How close to a player's chest the flask bursts on them, roughly. */
	private static final double BURST_SHORT = 1.0;

	private int reflectedTicks;

	public HagPotion(EntityType<? extends HagPotion> type, Level level) {
		super(type, level);
	}

	public HagPotion(Level level, LivingEntity owner) {
		super(EamEntities.HAG_POTION, owner, level, flask());
	}

	/** A splash potion bottle with her brew's colour, for the look of the flask in flight. */
	private static ItemStack flask() {
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(COLOR), List.of(), Optional.empty()));
		return stack;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_REFLECTED, false);
	}

	public boolean reflected() {
		return entityData.get(DATA_REFLECTED);
	}

	@Override
	public void tick() {
		if (!reflected() && level() instanceof ServerLevel level) {
			tryReflect(level);
		}
		if (!reflected()) {
			super.tick();
			return;
		}
		// Sent back: straight at her, through anything in the way.
		if (level() instanceof ServerLevel level && !flyBack(level)) {
			return;
		}
		setPos(position().add(getDeltaMovement()));
		updateRotation();
	}

	/** Sends the flask back if a player near its path hit or raised a shield just in time, facing it. */
	private void tryReflect(ServerLevel level) {
		if (!(getOwner() instanceof BabaYaga yaga) || !yaga.isAlive()) {
			return;
		}
		Vec3 motion = getDeltaMovement();
		double speed = motion.length();
		BabaYagaSettings.Potion potion = BabaYagaModule.settings().potion();
		int window = potion.reflectWindow();
		if (speed < 0.05 || window <= 0) {
			return;
		}
		for (Player player : level.players()) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}
			Vec3 to = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0).subtract(position());
			double distance = to.length();
			if (distance > speed * (window + 1) + BURST_SHORT + 1.0) {
				continue;
			}
			double closing = motion.dot(to) / Math.max(distance, 1.0E-3);
			if (closing <= 0.0) {
				continue;
			}
			double ticksToReach = Math.max(0.0, distance - BURST_SHORT) / closing;
			int acted = actedAgo(player);
			Vec3 look = position().subtract(player.getEyePosition());
			boolean facing = look.lengthSqr() < 1.0E-4 || player.getViewVector(1.0F).dot(look.normalize()) > 0.0;
			if (facing && BabaYagaLogic.reflects(ticksToReach, acted, window)) {
				reflect(level, yaga, shieldRaisedTicks(player) == acted, potion.reflectSpeed());
				return;
			}
		}
	}

	/** Ticks since the player last swung or raised a shield, whichever was later, or -1 for neither. */
	private static int actedAgo(Player player) {
		int shield = shieldRaisedTicks(player);
		int swing = -1;
		LivingEntity.SwingDescription current = player.getCurrentSwing();
		if (current != null) {
			swing = Math.round(player.getSwingAnimation(1.0F) * current.durationTicks());
		}
		if (shield < 0) {
			return swing;
		}
		return swing < 0 ? shield : Math.min(shield, swing);
	}

	/** Ticks the player has had a shield up, or -1 if they are not raising one. */
	private static int shieldRaisedTicks(Player player) {
		return player.isUsingItem() && player.getUseItem().has(DataComponents.BLOCKS_ATTACKS) ? player.getTicksUsingItem() : -1;
	}

	private void reflect(ServerLevel level, BabaYaga yaga, boolean shield, double speed) {
		entityData.set(DATA_REFLECTED, true);
		setNoGravity(true);
		reflectedTicks = 0;
		setDeltaMovement(towards(yaga).scale(speed));
		needsSync = true;
		if (shield) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.3F);
		} else {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 1.2F);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0F, 1.6F);
		level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.15, 0.15, 0.15, 0.3);
	}

	/**
	 * Steers the flask sent back at her; bursts it on her, or in the air if she is gone or it flew too long. Returns
	 * whether it is still flying.
	 */
	private boolean flyBack(ServerLevel level) {
		Entity owner = getOwner();
		if (!(owner instanceof BabaYaga yaga) || !yaga.isAlive() || ++reflectedTicks > REFLECTED_LIFE) {
			burstFx(level, 1.5);
			discard();
			return false;
		}
		double speed = BabaYagaModule.settings().potion().reflectSpeed();
		Vec3 centre = yaga.position().add(0.0, yaga.getBbHeight() * 0.6, 0.0);
		if (position().distanceTo(centre) <= speed + yaga.getBbWidth() * 0.5 + 0.3) {
			setPos(centre);
			burstFx(level, 1.5);
			yaga.dazeByFlask(level);
			discard();
			return false;
		}
		setDeltaMovement(towards(yaga).scale(speed));
		needsSync = true;
		return true;
	}

	private Vec3 towards(BabaYaga yaga) {
		return yaga.position().add(0.0, yaga.getBbHeight() * 0.6, 0.0).subtract(position()).normalize();
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPLASH_POTION;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof BabaYaga) && !BabaYagaModule.isMinion(entity);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		BabaYagaSettings.Potion potion = BabaYagaModule.settings().potion();
		double radius = potion.radius();
		AABB box = getBoundingBox().inflate(radius, radius * 0.5, radius);
		Entity owner = getOwner();
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e.isAffectedByPotions() && !(e instanceof BabaYaga) && !BabaYagaModule.isMinion(e))) {
			double distance = Math.sqrt(distanceToSqr(living));
			if (distance > radius) {
				continue;
			}
			if (potion.poisonTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.POISON, potion.poisonTicks(), 0), owner);
			}
			if (potion.nauseaTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, potion.nauseaTicks(), potion.nauseaLevel()), owner);
			}
			if (potion.weaknessTicks() > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, potion.weaknessTicks(), potion.weaknessLevel()), owner);
			}
		}
		burstFx(level, radius);
		discard();
	}

	/** The flask bursting: a splash, a ring of green mist as wide as it reaches and her fumes. */
	private void burstFx(ServerLevel level, double radius) {
		level.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, blockPosition(), COLOR);
		// The burst is wider than a potion's: a ring of green mist shows how far it reached.
		DustParticleOptions mist = new DustParticleOptions(COLOR, 1.5F);
		for (int i = 0; i < 32; i++) {
			double angle = i * Math.PI * 2.0 / 32.0;
			level.sendParticles(mist, getX() + Math.cos(angle) * radius, getY() + 0.3, getZ() + Math.sin(angle) * radius, 1, 0.1, 0.2, 0.1, 0.0);
		}
		level.levelEvent(LevelEvent.SOUND_SPELL_POTION_SPLASH, blockPosition(), 0);
		YagaFx.toxicBurst(level, position(), radius, random);
	}
}
