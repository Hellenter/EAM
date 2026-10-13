package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The commander of a shield wall: one of the Lost in a leader's gear with a crossbow. He comes with four spearmen who
 * lock their shields in a row between him and the enemy; he keeps behind the row and shoots over it. Several shield
 * walls near each other close up into one longer row. His bolts fly through the Lost.
 */
public class LostWallCommander extends Lost {
	public LostWallCommander(EntityType<? extends LostWallCommander> type, Level level) {
		super(type, level);
		xpReward = 15;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(2, new LostCrossbowGoal(this));
	}

	@Override
	protected LostRole headRole() {
		return LostRole.WALL_COMMANDER;
	}

	@Override
	protected double roleHealth(LostSettings settings) {
		return settings.squad().leaderHealth();
	}

	@Override
	protected float roleDropChance(LostSettings settings) {
		return settings.squad().leaderDropChance();
	}

	@Override
	protected List<LostRole> followerRoles(LostSettings settings, RandomSource random) {
		return Collections.nCopies(settings.shieldWall().spearmen(), LostRole.WALL_SPEARMAN);
	}

	@Override
	protected boolean bringsCompanions() {
		return false;
	}

	@Override
	public boolean canUseNonMeleeWeapon(ItemStack item) {
		return item.is(Items.CROSSBOW);
	}

	ItemStack crossbow() {
		return getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.CROSSBOW));
	}

	/** Shows the crossbow loaded or empty: the model draws the bolt in it. */
	void setLoaded(boolean loaded) {
		ItemStack crossbow = crossbow();
		if (crossbow.is(Items.CROSSBOW)) {
			crossbow.set(DataComponents.CHARGED_PROJECTILES, loaded ? ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW)))
					: ChargedProjectiles.EMPTY);
		}
	}

	/** Killed while aiming, he drops his crossbow empty: the bolt in it was only for show. */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		setLoaded(false);
		super.dropCustomDeathLoot(level, source, killedByPlayer);
	}

	/** One bolt at {@code target}, aimed to drop onto it; it passes through the Lost in its way. */
	void shoot(ServerLevel level, LivingEntity target, ShieldWallSettings.Crossbow settings) {
		ItemStack crossbow = crossbow();
		Bolt bolt = new Bolt(level, this, crossbow.isEmpty() ? null : crossbow);
		bolt.setSoundEvent(SoundEvents.CROSSBOW_HIT);
		bolt.setBaseDamage(EliteCrossbowmanLogic.baseDamage(settings.damage(), settings.speed()));
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		double dy = EliteCrossbowmanLogic.aimHeight(flat, target.getY(1.0 / 3.0) - bolt.getY(), settings.speed(),
				EliteCrossbowmanLogic.ARROW_GRAVITY);
		bolt.shoot(dx, dy, dz, settings.speed(), settings.inaccuracy());
		level.addFreshEntity(bolt);
		playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
	}

	/**
	 * A plain arrow that does not hit the Lost, so he can shoot over and past his own row. Saved, it is a plain arrow
	 * again; by then it has landed.
	 */
	private static final class Bolt extends Arrow {
		Bolt(Level level, LivingEntity owner, @Nullable ItemStack weapon) {
			super(level, owner, new ItemStack(Items.ARROW), weapon);
		}

		@Override
		protected boolean canHitEntity(Entity entity) {
			return !LostCommander.isKin(entity) && super.canHitEntity(entity);
		}
	}
}
