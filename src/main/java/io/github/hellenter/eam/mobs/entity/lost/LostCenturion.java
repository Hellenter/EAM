package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Lost centurion: one of the Lost in full plate of ancient metal battered nearly to pieces, a red crest on his helm,
 * a long spear with a torn banner and a shield. He comes with one squad in four, or at the head of his own when
 * spawned alone. He fights with long spear jabs; once he has a player in sight he blows his horn, and more of the Lost
 * come from out of the player's sight: first a pair of heavy cavalrymen, then cavalry or a squad every 30 and then 40
 * seconds. Whoever hits him draws every one of the Lost fighting that one already: they leave their places and go for
 * it. His armor items give his protection and are drawn by his own model, not the armor layer.
 */
public class LostCenturion extends Lost {
	/** The Lost around him are counted this far for the horn's limit. */
	private static final double NEARBY_RANGE = 48.0;
	/** He answers being hit at most this often. */
	private static final int RALLY_INTERVAL = 10;
	/** Not yet counting down to the first call. */
	private static final int UNSET = -1;

	private int hornCalls;
	private int hornCooldown = UNSET;
	private long lastRally;

	public LostCenturion(EntityType<? extends LostCenturion> type, Level level) {
		super(type, level);
		xpReward = 25;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Lost.createAttributes()
				.add(Attributes.MAX_HEALTH, LostCenturionSettings.DEFAULT.health())
				.add(Attributes.ARMOR, LostCenturionSettings.DEFAULT.extraArmor())
				.add(Attributes.FOLLOW_RANGE, 40.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	private static LostCenturionSettings centurion() {
		return LostModule.settings().centurion();
	}

	@Override
	protected SkeletonKnightSettings settings() {
		return centurion().fight();
	}

	@Override
	protected double pokeReach() {
		return centurion().spearReach();
	}

	@Override
	protected LostRole headRole() {
		return LostRole.CENTURION;
	}

	@Override
	protected double roleHealth(LostSettings settings) {
		return settings.centurion().health();
	}

	@Override
	protected float roleDropChance(LostSettings settings) {
		return settings.centurion().dropChance();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		AttributeInstance armor = getAttribute(Attributes.ARMOR);
		if (armor != null) {
			armor.setBaseValue(centurion().extraArmor());
		}
		return data;
	}

	/** Full plate (real ancient metal armor, holed), an iron spear his model draws long and bannered, a battered shield. */
	@Override
	protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
		int look = 1 + random.nextInt(LostGear.WORN.size() - 1);
		setItemSlot(EquipmentSlot.HEAD, LostGear.worn(EamItems.ANCIENT_METAL_HELMET, look));
		setItemSlot(EquipmentSlot.CHEST, LostGear.worn(EamItems.ANCIENT_METAL_CHESTPLATE, look));
		setItemSlot(EquipmentSlot.LEGS, LostGear.worn(EamItems.ANCIENT_METAL_LEGGINGS, look));
		setItemSlot(EquipmentSlot.FEET, LostGear.worn(EamItems.ANCIENT_METAL_BOOTS, look));
		setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
		ItemStack shield = new ItemStack(Items.SHIELD);
		shield.set(DataComponents.ITEM_MODEL, LostGear.SHIELD_MODEL);
		setItemSlot(EquipmentSlot.OFFHAND, shield);
	}

	// ---- Horn ----

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LostCenturionSettings.Horn horn = centurion().horn();
		if (hornCooldown == UNSET) {
			hornCooldown = horn.firstDelay();
		}
		Player player = playerToCall(horn);
		// The first call waits for a player in sight; after it the horn keeps its own time.
		if (hornCooldown > 0 && (hornCalls > 0 || player != null)) {
			hornCooldown--;
		}
		if (hornCooldown <= 0 && player != null && !isBusy()) {
			startAction(KnightAction.HORN, player);
		}
	}

	/** His target, if it is a player he sees and close enough to call the Lost on. */
	private @Nullable Player playerToCall(LostCenturionSettings.Horn horn) {
		return getTarget() instanceof Player player && player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
				&& distanceToSqr(player) <= horn.range() * horn.range() && getSensing().hasLineOfSight(player) ? player : null;
	}

	@Override
	protected boolean tickOwnAction(ServerLevel level, KnightAction action, int t, LivingEntity target) {
		if (action != KnightAction.HORN) {
			return false;
		}
		getNavigation().stop();
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		faceTowards(target);
		if (t == KnightAction.HORN_SOUND) {
			// A goat horn's call, deeper; it carries as far as a goat horn.
			level.playSound(null, getX(), getEyeY(), getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(5), getSoundSource(), 16.0F, 0.75F);
		} else if (t == KnightAction.HORN_CALL) {
			LostCenturionSettings.Horn horn = centurion().horn();
			if (target instanceof Player player) {
				callReinforcements(level, player, horn);
			}
			hornCalls++;
			hornCooldown = LostCenturionLogic.cooldownAfter(hornCalls, horn);
		}
		return true;
	}

	/** Brings cavalry or a squad from somewhere the player cannot see, unless there are enough of the Lost about already. */
	private void callReinforcements(ServerLevel level, Player player, LostCenturionSettings.Horn horn) {
		AABB around = getBoundingBox().inflate(NEARBY_RANGE);
		int nearby = level.getEntitiesOfClass(Lost.class, around, Lost::isAlive).size()
				+ level.getEntitiesOfClass(LostCavalryman.class, around, LostCavalryman::isAlive).size();
		if (nearby >= horn.maxNearby()) {
			return;
		}
		LostCenturionLogic.Call call = LostCenturionLogic.call(hornCalls, getRandom().nextDouble(), horn);
		LostWave wave = call == LostCenturionLogic.Call.CAVALRY ? LostWave.CAVALRY : LostWave.SQUAD;
		// Everyone who came goes for the player: the squad, or both riders.
		for (Mob newcomer : LostReinforcements.bring(level, player, wave, horn.minDistance(), horn.maxDistance(), true, position(), false)) {
			newcomer.setTarget(player);
		}
	}

	// ---- His guard ----

	/** Whoever attacks him, the Lost already fighting that one leave their places and go for it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (source.getEntity() instanceof LivingEntity attacker && attacker != this && attacker.isAlive() && !(attacker instanceof Lost)
				&& !(attacker instanceof LostCavalryman) && level.getGameTime() - lastRally >= RALLY_INTERVAL) {
			lastRally = level.getGameTime();
			LostCenturionSettings.Defend defend = centurion().defend();
			for (Lost mate : level.getEntitiesOfClass(Lost.class, getBoundingBox().inflate(defend.range()),
					mate -> mate != this && mate.isAlive() && mate.getTarget() == attacker)) {
				mate.defendCenturion(attacker, defend.ticks());
			}
		}
		return hurt;
	}

	// ---- Saving ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("HornCalls", hornCalls);
		output.putInt("HornCooldown", hornCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		hornCalls = input.getIntOr("HornCalls", 0);
		hornCooldown = input.getIntOr("HornCooldown", UNSET);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.85F;
	}
}
