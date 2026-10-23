package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;
import io.github.hellenter.eam.world.structures.village.DuergarVillage;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The Lost straggler: one of the Lost who fell behind his squad and wanders the stone wastes alone, bareheaded, in a
 * torn breastplate or none, with a rusty sword and no shield. Weak on his own, out of a fight he joins a squad he comes
 * across. A player who takes too long to kill him pays for it: he drives his sword into the floor, and the nearest squad
 * of the Lost comes for the player, or a new one comes from out of sight.
 */
public class LostStraggler extends Lost {
	/** How often, out of a fight, he looks for a squad to join. */
	private static final int JOIN_INTERVAL = 40;

	private int fightTicks;
	private boolean called;
	private int joinCheck;

	public LostStraggler(EntityType<? extends LostStraggler> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Lost.createAttributes()
				.add(Attributes.MAX_HEALTH, LostStragglerSettings.DEFAULT.health())
				.add(Attributes.ATTACK_DAMAGE, LostStragglerSettings.DEFAULT.attackDamage());
	}

	private static LostStragglerSettings straggler() {
		return LostModule.settings().straggler();
	}

	/** A dark spot in the stone wastes, out of duergar villages, with no other straggler near and few about. */
	public static boolean checkStragglerSpawnRules(EntityType<LostStraggler> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (reason != EntitySpawnReason.NATURAL) {
			return true;
		}
		LostStragglerSettings settings = straggler();
		AABB spot = new AABB(pos);
		int tooClose = level.getEntitiesOfClass(LostStraggler.class, spot.inflate(settings.spawnSpacing())).size();
		int around = level.getEntitiesOfClass(LostStraggler.class, spot.inflate(settings.spawnRange())).size();
		return LostStragglerLogic.spawnAllowed(tooClose, around, settings.maxStragglers())
				&& !level.getLevel().structureManager().getStructureWithPieceAt(pos, DuergarVillage.VILLAGES).isValid();
	}

	@Override
	protected SkeletonKnightSettings settings() {
		return straggler().fight();
	}

	@Override
	protected LostRole headRole() {
		return LostRole.STRAGGLER;
	}

	@Override
	protected double roleHealth(LostSettings settings) {
		return settings.straggler().health();
	}

	@Override
	protected float roleDropChance(LostSettings settings) {
		return settings.straggler().dropChance();
	}

	/** He always comes alone. */
	@Override
	protected boolean comesAlone(EntitySpawnReason reason) {
		return true;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.setBaseValue(straggler().attackDamage());
		}
		return data;
	}

	/** No helmet or leggings, his torn breastplate and his boots if he kept them, a rusty sword. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		LostStragglerSettings settings = straggler();
		if (random.nextFloat() < settings.chestChance()) {
			setItemSlot(EquipmentSlot.CHEST, LostGear.worn(EamItems.ANCIENT_METAL_CHESTPLATE, LostGear.TORN));
		}
		if (random.nextFloat() < settings.bootsChance()) {
			setItemSlot(EquipmentSlot.FEET, LostGear.worn(EamItems.ANCIENT_METAL_BOOTS, LostGear.TORN));
		}
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(EamItems.RUSTY_IRON_SWORD));
	}

	// ---- Joining a squad, calling one ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LostStragglerSettings settings = straggler();
		Player player = playerFought(settings);
		fightTicks = LostStragglerLogic.fightTicks(fightTicks, player != null);
		if (player != null && !isBusy() && onGround() && LostStragglerLogic.callDue(fightTicks, settings.callAfter(), called, inSquad())) {
			startAction(KnightAction.STRAGGLER_CALL, player);
		}
		if (getTarget() == null && !inSquad() && --joinCheck <= 0) {
			joinCheck = JOIN_INTERVAL;
			Lost head = nearestSquad(level, settings.joinRange());
			if (head != null) {
				joinSquad(head);
			}
		}
	}

	/** His target, if it is a player close enough for the fight to count. */
	private @Nullable Player playerFought(LostStragglerSettings settings) {
		return getTarget() instanceof Player player && player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
				&& distanceToSqr(player) <= settings.fightRange() * settings.fightRange() ? player : null;
	}

	@Override
	protected boolean tickOwnAction(ServerLevel level, KnightAction action, int t, LivingEntity target) {
		if (action != KnightAction.STRAGGLER_CALL) {
			return false;
		}
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		if (t == KnightAction.STRAGGLER_CALL_HIT) {
			strikeFloor(level);
			if (target instanceof Player player) {
				call(level, player);
			}
			called = true;
		}
		return true;
	}

	/** His sword rings on the stone in front of him, chips and rust flying; a low, quiet horn answers far off. */
	private void strikeFloor(ServerLevel level) {
		float yaw = getYRot() * Mth.DEG_TO_RAD;
		Vec3 point = new Vec3(getX() - Mth.sin(yaw) * 1.3, getY() + 0.1, getZ() + Mth.cos(yaw) * 1.3);
		level.playSound(null, point.x, point.y, point.z, SoundEvents.ANVIL_LAND, getSoundSource(), 0.7F, 0.55F);
		level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5), getSoundSource(), 3.0F, 0.6F);
		level.sendParticles(EamParticles.STRAGGLER_RUBBLE, point.x, point.y, point.z, 16, 0.25, 0.05, 0.25, 0.18);
		level.sendParticles(EamParticles.UNDERWORLD_DUST, point.x, point.y + 0.2, point.z, 10, 0.4, 0.15, 0.4, 0.02);
	}

	/**
	 * The nearest squad of the Lost within hearing comes for the player. Without one, a new squad comes from out of the
	 * player's sight, unless there are enough of the Lost about already; the caves around the player then rest as after
	 * any squad.
	 */
	private void call(ServerLevel level, Player player) {
		LostStragglerSettings settings = straggler();
		Lost head = nearestSquad(level, settings.squadRange());
		if (head != null) {
			UUID squad = head.squadId();
			for (Lost member : level.getEntitiesOfClass(Lost.class, head.getBoundingBox().inflate(SQUAD_RANGE),
					member -> member.isAlive() && squad.equals(member.squadId()))) {
				member.setTarget(player);
			}
			return;
		}
		AABB around = getBoundingBox().inflate(settings.squadRange());
		int nearby = level.getEntitiesOfClass(Lost.class, around, Lost::isAlive).size()
				+ level.getEntitiesOfClass(LostCavalryman.class, around, LostCavalryman::isAlive).size();
		if (nearby >= settings.maxNearby()) {
			return;
		}
		List<Mob> arrived = LostReinforcements.bring(level, player, LostWave.SQUAD, settings.minDistance(), settings.maxDistance(), true,
				position(), false);
		for (Mob newcomer : arrived) {
			newcomer.setTarget(player);
		}
		if (!arrived.isEmpty()) {
			LostSpawnCooldowns.start(level, player.blockPosition(), LostModule.settings().squad(), getRandom());
		}
	}

	/** The nearest one of the Lost heading a squad within {@code range}, or {@code null}. */
	private @Nullable Lost nearestSquad(ServerLevel level, double range) {
		Lost nearest = null;
		double best = Double.MAX_VALUE;
		for (Lost head : level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(range),
				lost -> lost != this && lost.isAlive() && lost.headsSquad())) {
			double distance = distanceToSqr(head);
			if (distance < best) {
				best = distance;
				nearest = head;
			}
		}
		return nearest;
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("FightTicks", fightTicks);
		output.putBoolean("Called", called);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		fightTicks = input.getIntOr("FightTicks", 0);
		called = input.getBooleanOr("Called", false);
	}
}
