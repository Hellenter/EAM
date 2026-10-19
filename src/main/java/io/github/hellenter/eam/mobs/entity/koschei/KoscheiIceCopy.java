package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.mobs.entity.draugr.Draugr;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiLogic.StaffAttack;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

/**
 * A copy of Koschei in ice, two of which stand up at his sides in his second phase: it moves as he does with the staff,
 * keeping its distance, but shoots only the bolt; it is frail and shatters on its own after a while, or when he is gone.
 * No boss bar, no experience, never saved.
 */
public class KoscheiIceCopy extends Koschei {
	private @Nullable Koschei owner;
	private PhaseTwoSettings.IceCopies copySettings = PhaseTwoSettings.IceCopies.DEFAULT;

	public KoscheiIceCopy(EntityType<? extends KoscheiIceCopy> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Koschei.createAttributes().add(Attributes.MAX_HEALTH, PhaseTwoSettings.IceCopies.DEFAULT.health());
	}

	/** Makes it a copy of {@code owner} in his second phase, after {@code target}. */
	void summon(Koschei owner, @Nullable LivingEntity target, PhaseTwoSettings.IceCopies settings) {
		this.owner = owner;
		this.copySettings = settings;
		setLeftHanded(true);
		setCanPickUpLoot(false);
		setPhase(2);
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(settings.health());
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(owner.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
		setHealth(settings.health());
		xpReward = 0;
		setTarget(target);
		// Its first bolt comes a moment after it stands up, not at once.
		staffCooldowns()[StaffAttack.BOLT.ordinal()] = settings.boltCooldown() / 2;
	}

	@Override
	protected boolean showsBossBar() {
		return false;
	}

	@Override
	boolean teleportReady() {
		return false;
	}

	@Override
	boolean vanishesWhenHit() {
		return false;
	}

	/** Frail as it is, an arrow breaks it. */
	@Override
	boolean iceField() {
		return false;
	}

	/** Only the bolt. */
	@Override
	@Nullable StaffAttack chooseStaff(LivingEntity target, double distance, boolean sees, boolean rushing) {
		return sees && staffCooldowns()[StaffAttack.BOLT.ordinal()] <= 0 && distance <= phaseTwo().bolt().maxRange() ? StaffAttack.BOLT : null;
	}

	@Override
	void startStaff(StaffAttack attack, LivingEntity target) {
		super.startStaff(attack, target);
		staffCooldowns()[StaffAttack.BOLT.ordinal()] = copySettings.boltCooldown();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (owner == null || !owner.isAlive() || tickCount > copySettings.lifetime()) {
			shatter(level);
		}
	}

	/** Bursts into shards of ice. */
	private void shatter(ServerLevel level) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX(), getY(0.5), getZ(), 60,
				0.4, 0.9, 0.4, 0.15);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(0.5), getZ(), 20, 0.4, 0.9, 0.4, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, getSoundSource(), 1.5F, 0.6F);
		discard();
	}

	/** His draugr and he himself do not break it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker instanceof Koschei || attacker instanceof Draugr draugr && owner != null && draugr.servedBy(owner)) {
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		return owner != null && other instanceof Draugr draugr && draugr.servedBy(owner) || super.considersEntityAsAlly(other);
	}

	@Override
	protected void tickDeath() {
		if (level() instanceof ServerLevel level) {
			shatter(level);
		}
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	/** It does not count as him for the bosses' building rules. */
	@Override
	public boolean isFighting() {
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
