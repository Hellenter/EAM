package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.longbow.LongbowLogic;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lost longbowman: one of the Lost with a longbow. He sees three times as far as a squad archer and keeps far back;
 * he draws twice as long, but his arrows fly twice as fast and hurt two and a half times as much. He shoots the weakest
 * player in sight, and every other arrow at an unmarked player is a black marking arrow: the player it hits takes more
 * damage for a while and the Lost go for him first. A player who comes too close gets a quick volley of weak arrows
 * that pushes him back. Now and then, with a clear warning, he looses a very fast red arrow that goes through a raised
 * shield. He comes with some squads, or alone from his egg; his arrows fly through the Lost.
 */
public class LostLongbowman extends Lost {
	/** Arrow speed of a squad archer's shot, as a skeleton's. */
	private static final float ARCHER_SPEED = 1.6F;
	/** Spread of the red power arrow: aimed with care. */
	private static final float RED_INACCURACY = 0.3F;

	/** His shots since the last marking arrow; full at first, so his first arrow at a player marks him. */
	private int shotsSinceMark = Integer.MAX_VALUE / 2;
	private int retarget;

	public LostLongbowman(EntityType<? extends LostLongbowman> type, Level level) {
		super(type, level);
		xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Lost.createAttributes()
				.add(Attributes.MAX_HEALTH, LongbowmanSettings.DEFAULT.health())
				.add(Attributes.FOLLOW_RANGE, LongbowmanSettings.DEFAULT.followRange());
	}

	static LongbowmanSettings longbowman() {
		return LostModule.settings().longbowman();
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(1, new LongbowBarrageGoal(this));
		goalSelector.addGoal(1, new LongbowPowerShotGoal(this));
		goalSelector.addGoal(2, new LostBowGoal(this, LostRole.LONGBOWMAN, EamItems.LONGBOW, LostLongbowman::archerNumbers));
	}

	/** His keeping of distance and his draw, in the squad archer's terms. */
	private static LostSettings.Archer archerNumbers() {
		LongbowmanSettings settings = longbowman();
		return new LostSettings.Archer(settings.minRange(), settings.maxRange(), settings.drawTicks(), settings.cooldown(), settings.aimTicks());
	}

	@Override
	protected LostRole headRole() {
		return LostRole.LONGBOWMAN;
	}

	@Override
	protected double roleHealth(LostSettings settings) {
		return settings.longbowman().health();
	}

	/** Spawned on his own he comes alone; with a squad he is one of its followers. */
	@Override
	protected boolean comesAlone(EntitySpawnReason reason) {
		return true;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		AttributeInstance range = getAttribute(Attributes.FOLLOW_RANGE);
		if (range != null) {
			range.setBaseValue(longbowman().followRange());
		}
		setDropChance(EquipmentSlot.MAINHAND, longbowman().dropChance());
		return data;
	}

	@Override
	public boolean canUseNonMeleeWeapon(ItemStack item) {
		return item.is(EamItems.LONGBOW);
	}

	ItemStack longbow() {
		return getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, EamItems.LONGBOW));
	}

	// ---- Targets ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--retarget <= 0) {
			retarget = longbowman().retargetTicks();
			pickTarget(level);
		}
	}

	/** Of the players he sees, a marked one first, then the one with the least health. He keeps any other target. */
	private void pickTarget(ServerLevel level) {
		LivingEntity current = getTarget();
		if (current != null && current.isAlive() && !(current instanceof Player)) {
			return;
		}
		double range = getAttributeValue(Attributes.FOLLOW_RANGE);
		List<LongbowmanLogic.Candidate<Player>> candidates = new ArrayList<>();
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
				player -> player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) && canAttack(player))) {
			double distanceSqr = distanceToSqr(player);
			if (distanceSqr <= range * range && getSensing().hasLineOfSight(player)) {
				candidates.add(new LongbowmanLogic.Candidate<>(player, LostMark.marked(player), player.getHealth() + player.getAbsorptionAmount(),
						distanceSqr));
			}
		}
		Player best = LongbowmanLogic.pick(candidates);
		if (best != null && best != current) {
			setTarget(best);
		}
	}

	// ---- Shooting ----

	/** One longbow arrow, aimed to drop onto the target; every so often a marking arrow at a player. */
	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		LongbowmanSettings settings = longbowman();
		boolean marking = LongbowmanLogic.marks(shotsSinceMark, settings.mark().every(), target instanceof Player && !LostMark.marked(target));
		shotsSinceMark = marking ? 0 : shotsSinceMark + 1;
		LostArrow arrow = new LostArrow(level, this, longbow());
		if (marking) {
			arrow.setMarking(Math.round(settings.mark().seconds() * 20.0F));
		}
		double base = squadArcherDamage(level, power);
		arrow.setBaseDamage(LongbowLogic.baseDamage(base, settings.damageMultiplier(), settings.speedMultiplier()));
		float speed = ARCHER_SPEED * settings.speedMultiplier() * Math.max(power, 0.1F);
		aim(arrow, target, speed, settings.inaccuracy());
		level.addFreshEntity(arrow);
		playSound(SoundEvents.SKELETON_SHOOT, 1.2F, 0.6F / (getRandom().nextFloat() * 0.3F + 0.8F));
	}

	/** A squad archer's base arrow damage, as a skeleton's; the speed multiplies it on hit. */
	private double squadArcherDamage(ServerLevel level, float power) {
		return power * 2.0 + getRandom().triangle(level.getDifficulty().getId() * 0.11, 0.57425);
	}

	/** The warned power shot: a very fast red arrow that goes through a raised shield. */
	void shootRed(ServerLevel level, LivingEntity target, LongbowmanSettings.PowerShot shot) {
		LongbowmanSettings settings = longbowman();
		LostArrow arrow = new LostArrow(level, this, longbow());
		arrow.setRed(LongbowmanLogic.redDamage(squadArcherDamage(level, 1.0F), ARCHER_SPEED, settings.damageMultiplier(), shot.damageMultiplier()),
				shot.shieldMultiplier(), shot.shieldSeconds());
		aim(arrow, target, shot.speed(), RED_INACCURACY);
		level.addFreshEntity(arrow);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.6F, 0.6F);
		playSound(SoundEvents.SKELETON_SHOOT, 1.6F, 0.5F);
	}

	/** A quick weak arrow of the volley that drives back a player who came too close. */
	void shootBarrage(ServerLevel level, LivingEntity target, LongbowmanSettings.Barrage barrage) {
		LostArrow arrow = new LostArrow(level, this, longbow());
		arrow.setBaseDamage(EliteCrossbowmanLogic.baseDamage(barrage.damage(), barrage.speed()));
		arrow.setVolley(barrage.knockback());
		aim(arrow, target, barrage.speed(), barrage.inaccuracy());
		level.addFreshEntity(arrow);
		playSound(SoundEvents.SKELETON_SHOOT, 0.8F, 1.3F / (getRandom().nextFloat() * 0.3F + 0.85F));
	}

	private void aim(LostArrow arrow, LivingEntity target, float speed, float inaccuracy) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		double dy = EliteCrossbowmanLogic.aimHeight(flat, target.getY(1.0 / 3.0) - arrow.getY(), speed, EliteCrossbowmanLogic.ARROW_GRAVITY);
		arrow.shoot(dx, dy, dz, speed, inaccuracy);
	}
}
