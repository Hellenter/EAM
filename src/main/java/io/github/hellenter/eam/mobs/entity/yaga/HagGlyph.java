package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Glyph;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A glowing glyph Baba Yaga lays on the ground, or the acid puddle her bolt leaves. Inside a glyph a player is cursed and
 * monsters are helped: the blinding glyph blinds and weakens the player and speeds monsters up; the draining glyph slows
 * the player and makes them hungry and heals monsters; the glyph of harm strikes once after a delay, through armor, and
 * heals every monster inside. A glyph lies grey and harmless for a moment after it appears, then lights up; its effects
 * last only while one stands inside and are taken away the moment one steps out. The acid puddle burns the player through
 * armor every second. A glyph can also be the small copy she shows over her head as she casts it, which does nothing.
 * Never saved: a reload clears them.
 */
public class HagGlyph extends Entity {
	private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(HagGlyph.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(HagGlyph.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(HagGlyph.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FOLLOW = SynchedEntityData.defineId(HagGlyph.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_INACTIVE = SynchedEntityData.defineId(HagGlyph.class, EntityDataSerializers.INT);
	/** Effects are given out this often. */
	private static final int PULSE = 10;
	/** The glyph of harm flares this long after it strikes, then goes. */
	private static final int HARM_FLASH = 10;
	/** The small copy floats this high over her head. */
	private static final double PREVIEW_HEIGHT = 0.9;

	private @Nullable Entity owner;
	/** Who was inside at the last tick, and so carries this glyph's effects. */
	private final Set<LivingEntity> affected = new HashSet<>();

	public HagGlyph(EntityType<? extends HagGlyph> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** A glyph of {@code kind} on the ground at its position, lasting {@code lifetime} ticks. */
	public static HagGlyph create(Level level, @Nullable Entity owner, Glyph kind, double radius, int lifetime) {
		HagGlyph glyph = new HagGlyph(EamEntities.HAG_GLYPH, level);
		glyph.owner = owner;
		glyph.entityData.set(DATA_KIND, (byte) kind.ordinal());
		glyph.entityData.set(DATA_RADIUS, (float) radius);
		glyph.entityData.set(DATA_LIFETIME, lifetime);
		return glyph;
	}

	/** The small copy of a glyph over {@code owner}'s head, for a moment; it does nothing. */
	public static HagGlyph preview(Level level, Entity owner, Glyph kind) {
		HagGlyph glyph = create(level, owner, kind, 0.6, YagaAction.GLYPH_PREVIEW);
		glyph.entityData.set(DATA_FOLLOW, owner.getId());
		glyph.setPos(owner.getX(), owner.getY() + owner.getBbHeight() + PREVIEW_HEIGHT, owner.getZ());
		return glyph;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_KIND, (byte) 0);
		builder.define(DATA_RADIUS, 1.0F);
		builder.define(DATA_LIFETIME, 100);
		builder.define(DATA_FOLLOW, -1);
		builder.define(DATA_INACTIVE, 0);
	}

	/** Ticks it lies grey and does nothing after it appears. */
	public void setInactiveTicks(int ticks) {
		entityData.set(DATA_INACTIVE, ticks);
	}

	public int inactiveTicks() {
		return entityData.get(DATA_INACTIVE);
	}

	/** Whether it has lit up yet. */
	public boolean isActive() {
		return tickCount >= inactiveTicks();
	}

	public Glyph kind() {
		int id = entityData.get(DATA_KIND);
		return id >= 0 && id < Glyph.values().length ? Glyph.values()[id] : Glyph.BLIND;
	}

	public float radius() {
		return entityData.get(DATA_RADIUS);
	}

	public int lifetime() {
		return entityData.get(DATA_LIFETIME);
	}

	/** Whether this is the small copy over her head. */
	public boolean isPreview() {
		return entityData.get(DATA_FOLLOW) >= 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (isPreview()) {
			Entity followed = level().getEntity(entityData.get(DATA_FOLLOW));
			if (followed != null) {
				setPos(followed.getX(), followed.getY() + followed.getBbHeight() + PREVIEW_HEIGHT, followed.getZ());
			}
		}
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Glyph kind = kind();
		int lifetime = lifetime();
		if (isPreview()) {
			if (tickCount >= lifetime) {
				discard();
			}
			return;
		}
		if (kind == Glyph.HARM) {
			if (tickCount == lifetime) {
				strike(level);
			} else if (tickCount >= lifetime + HARM_FLASH) {
				discard();
			}
			return;
		}
		if (tickCount >= lifetime) {
			release();
			discard();
			return;
		}
		if (kind == Glyph.ACID) {
			tickAcid(level);
		} else if (isActive()) {
			tickCurse(level, kind);
		}
	}

	/**
	 * The blinding and draining glyphs: whoever steps in is cursed (or helped) at once and again every pulse while they
	 * stay; whoever steps out loses what the glyph gave them straight away.
	 */
	private void tickCurse(ServerLevel level, Glyph kind) {
		List<LivingEntity> inside = inside(level);
		for (Iterator<LivingEntity> it = affected.iterator(); it.hasNext(); ) {
			LivingEntity living = it.next();
			if (!inside.contains(living)) {
				release(living, kind);
				it.remove();
			}
		}
		boolean pulse = tickCount % PULSE == 0;
		for (LivingEntity living : inside) {
			if ((affected.add(living) || pulse) && (living instanceof Player || helps(living))) {
				apply(level, living, kind);
			}
		}
		if (pulse) {
			motes(level, kind);
		}
	}

	/** Takes this glyph's effects off everyone still inside, as it fades. */
	private void release() {
		for (LivingEntity living : affected) {
			release(living, kind());
		}
		affected.clear();
	}

	/**
	 * Takes away the effects this glyph gave {@code living}: only an effect still as weak and as short as the glyph's own,
	 * so a stronger or longer one from elsewhere (a flask's weakness) stays.
	 */
	private void release(LivingEntity living, Glyph kind) {
		if (!living.isAlive()) {
			return;
		}
		int ticks = settings().effectTicks();
		for (Holder<MobEffect> effect : effectsOf(living, kind)) {
			MobEffectInstance instance = living.getEffect(effect);
			if (instance != null && instance.getAmplifier() == 0 && instance.getDuration() <= ticks) {
				living.removeEffect(effect);
			}
		}
	}

	private static List<Holder<MobEffect>> effectsOf(LivingEntity living, Glyph kind) {
		if (living instanceof Player) {
			return kind == Glyph.BLIND ? List.of(MobEffects.BLINDNESS, MobEffects.WEAKNESS) : List.of(MobEffects.SLOWNESS, MobEffects.HUNGER);
		}
		return kind == Glyph.BLIND ? List.of(MobEffects.SPEED) : List.of(MobEffects.REGENERATION);
	}

	private BabaYagaSettings.Glyphs settings() {
		return BabaYagaModule.settings().glyphs();
	}

	/** A curse on a player inside, a boon for a monster. */
	private void apply(ServerLevel level, LivingEntity living, Glyph kind) {
		BabaYagaSettings.Glyphs glyphs = settings();
		int ticks = glyphs.effectTicks();
		if (living instanceof Player) {
			if (kind == Glyph.BLIND) {
				living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0), owner);
				living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), owner);
			} else {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 0), owner);
				living.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 0), owner);
			}
		} else if (kind == Glyph.BLIND) {
			living.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0), owner);
		} else {
			drain(level, living, glyphs);
		}
	}

	/** Motes rising off the glyph, in its colour. */
	private void motes(ServerLevel level, Glyph kind) {
		DustParticleOptions mote = new DustParticleOptions(YagaGlow.of(kind).color, 1.2F);
		double r = radius();
		for (int i = 0; i < 6; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double d = Math.sqrt(random.nextDouble()) * r;
			level.sendParticles(mote, getX() + Math.cos(angle) * d, getY() + 0.15, getZ() + Math.sin(angle) * d, 1, 0.0, 0.2, 0.0, 0.0);
		}
	}

	/**
	 * Regeneration for a monster that takes it; one that does not (the undead shrug off regeneration and poison alike)
	 * is healed straight away by the same amount.
	 */
	private void drain(ServerLevel level, LivingEntity living, BabaYagaSettings.Glyphs glyphs) {
		MobEffectInstance regeneration = new MobEffectInstance(MobEffects.REGENERATION, glyphs.effectTicks(), 0);
		if (living.canBeAffected(regeneration)) {
			living.addEffect(regeneration, owner);
			return;
		}
		if (living.getHealth() < living.getMaxHealth()) {
			living.heal(glyphs.drainHeal() * PULSE / 20.0F);
			level.sendParticles(ParticleTypes.HEART, living.getX(), living.getY() + living.getBbHeight() + 0.3, living.getZ(), 1, 0.2, 0.1,
					0.2, 0.0);
		}
	}

	/** The glyph of harm goes off: players inside are struck through armor, monsters are healed. */
	private void strike(ServerLevel level) {
		BabaYagaSettings.Glyphs glyphs = settings();
		DamageSource source = damageSources().source(BabaYagaModule.CURSE_DAMAGE, this, owner);
		for (LivingEntity living : inside(level)) {
			if (living instanceof Player) {
				living.hurtServer(level, source, glyphs.damage());
			} else if (helps(living)) {
				living.heal(glyphs.mobHeal());
				level.sendParticles(ParticleTypes.HEART, living.getX(), living.getY() + living.getBbHeight() + 0.3, living.getZ(), 3, 0.3, 0.2,
						0.3, 0.0);
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.6F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5F, 0.5F);
		double r = radius();
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 80, r * 0.5, 0.2, r * 0.5, 0.05);
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.2, getZ(), 12, r * 0.4, 0.1, r * 0.4, 0.0);
		level.sendParticles(new DustParticleOptions(YagaGlow.HARM.color, 2.0F), getX(), getY() + 0.5, getZ(), 40, r * 0.5, 0.6, r * 0.5, 0.0);
	}

	/** The acid puddle: bubbles, and a burn through armor every second for a player standing in it. */
	private void tickAcid(ServerLevel level) {
		double r = radius();
		if (tickCount % 3 == 0) {
			level.sendParticles(new DustParticleOptions(YagaGlow.ACID.color, 1.0F), getX(), getY() + 0.1, getZ(), 4, r * 0.4, 0.05, r * 0.4, 0.0);
			level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 0.1, getZ(), 2, r * 0.4, 0.02, r * 0.4, 0.0);
		}
		if (tickCount % 20 != 0) {
			return;
		}
		DamageSource source = damageSources().source(BabaYagaModule.CURSE_DAMAGE, this, owner);
		float damage = BabaYagaModule.settings().acid().poolDamage();
		for (LivingEntity living : inside(level)) {
			if (living instanceof Player) {
				living.hurtServer(level, source, damage);
				level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 0.5F, 1.6F);
			}
		}
	}

	/** Who stands in the glyph: within its radius, flat, and not far above or below it. Creative players are left alone. */
	private List<LivingEntity> inside(ServerLevel level) {
		double r = radius();
		AABB box = new AABB(getX() - r, getY() - 1.5, getZ() - r, getX() + r, getY() + 2.5, getZ() + r);
		return level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive() && !(living instanceof BabaYaga)
				&& EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && living.distanceToSqr(getX(), living.getY(), getZ()) <= r * r);
	}

	/** Monsters are helped by her glyphs; not other bosses, not animals. */
	private static boolean helps(LivingEntity living) {
		return living instanceof Enemy && living.isAlive();
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
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128.0 * 128.0;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
