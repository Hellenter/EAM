package io.github.hellenter.eam.mobs.entity.duergar;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A duergar of a clan village in the underworld. Like a villager he has a profession ({@link DuergarProfession}) and
 * levels up from novice to master as players trade with him, each level adding new offers (datapack trade sets); only
 * a duergar runecarver sells the Mending book. The village templates place a smith, a miner or the elder; each takes
 * a profession of his role on spawning. They keep near home, run from undead duergar and renegades, and their stock
 * comes back every morning.
 */
public class DuergarVillager extends AbstractVillager implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DuergarVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(DuergarVillager.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_PROFESSION =
			SynchedEntityData.defineId(DuergarVillager.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has: one per clan colour of their clothes. */
	public static final int SKINS = 4;
	/** How far from where he was placed or spawned he wanders. */
	private static final int HOME_RADIUS = 24;
	private static final long DAY = 24000L;

	/** His place in the village: the house templates name it, and it decides his clothes and his professions. */
	public enum Role implements StringRepresentable {
		SMITH("smith"),
		MINER("miner"),
		ELDER("elder");

		private final String name;

		Role(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		static Role byName(String name) {
			for (Role role : values()) {
				if (role.name.equals(name)) {
					return role;
				}
			}
			return MINER;
		}
	}

	private Role role = Role.MINER;
	private DuergarProfession profession = DuergarProfession.MINER;
	/** From 1 (novice) to 5 (master), as a villager's. */
	private int tradeLevel = 1;
	private int tradeXp;
	/** The day his stock was last refilled. */
	private long restockDay = -1L;

	public DuergarVillager(EntityType<? extends DuergarVillager> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.4)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GEAR, 0);
		builder.define(DATA_SKIN, (byte) 0);
		builder.define(DATA_PROFESSION, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, ZombieDuergar.class, 8.0F, 0.6, 0.7));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, DuergarRenegade.class, 12.0F, 0.6, 0.7));
		goalSelector.addGoal(1, new PanicGoal(this, 0.7));
		goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.5));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.45));
		goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		RandomSource random = getRandom();
		if (reason == EntitySpawnReason.STRUCTURE || reason == EntitySpawnReason.LOAD) {
			// A village places each duergar with his role; he takes one of its professions.
			setProfession(DuergarProfession.pick(role, random.nextDouble()));
		} else {
			// One from an egg gets any profession but the elder's.
			List<DuergarProfession> professions = new ArrayList<>(List.of(DuergarProfession.values()));
			professions.remove(DuergarProfession.ELDER);
			setProfession(professions.get(random.nextInt(professions.size())));
		}
		offers = null;
		tradeLevel = 1;
		tradeXp = 0;
		dress();
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
		setHomeTo(blockPosition(), HOME_RADIUS);
		setPersistenceRequired();
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	private void dress() {
		RandomSource random = getRandom();
		entityData.set(DATA_GEAR, DuergarGear.homespun(random::nextDouble, role));
	}

	public Role role() {
		return role;
	}

	public DuergarProfession profession() {
		return DuergarProfession.values()[Math.floorMod(entityData.get(DATA_PROFESSION), DuergarProfession.values().length)];
	}

	private void setProfession(DuergarProfession value) {
		profession = value;
		role = value.role();
		entityData.set(DATA_PROFESSION, (byte) value.ordinal());
	}

	/** "Duergar Runecarver" and the like, as a villager is named by his profession. */
	@Override
	protected Component getTypeName() {
		return Component.translatable(getType().getDescriptionId() + "." + profession().getSerializedName());
	}

	@Override
	public int gear() {
		return entityData.get(DATA_GEAR);
	}

	@Override
	public int skin() {
		return Math.floorMod(entityData.get(DATA_SKIN), SKINS);
	}

	// ---- Trading ----

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isAlive() || isTrading() || isBaby()) {
			return super.mobInteract(player, hand);
		}
		if (hand == InteractionHand.MAIN_HAND) {
			player.awardStat(Stats.TALKED_TO_VILLAGER);
		}
		if (!level().isClientSide()) {
			if (getOffers().isEmpty()) {
				return InteractionResult.CONSUME;
			}
			setTradingPlayer(player);
			openTradingScreen(player, getDisplayName(), tradeLevel);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		addOffersFromTradeSet(level, getOffers(), profession.trades(tradeLevel));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long day = level.getOverworldClockTime() / DAY;
		if (day != restockDay && !isTrading()) {
			if (restockDay >= 0 && offers != null) {
				for (MerchantOffer offer : offers) {
					offer.resetUses();
				}
			}
			restockDay = day;
		}
	}

	@Override
	public int getVillagerXp() {
		return tradeXp;
	}

	@Override
	public boolean showProgressBar() {
		return profession.maxLevel() > 1;
	}

	/** As a villager: each trade adds to his experience, and a new level brings new offers. */
	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		int orbXp = 3 + random.nextInt(4);
		tradeXp += offer.getXp();
		if (DuergarLogic.levelsUp(tradeLevel, profession.maxLevel(), tradeXp) && level() instanceof ServerLevel serverLevel) {
			tradeLevel++;
			updateTrades(serverLevel);
			addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
			orbXp += 5;
		}
		if (offer.shouldRewardExp()) {
			level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), orbXp));
		}
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	// ---- Sounds: a villager's grumble, deeper ----

	@Override
	protected SoundEvent getAmbientSound() {
		return isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
		return validTrade ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.VILLAGER_YES;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.6F;
	}

	// ---- Save ----

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Role", role.getSerializedName());
		output.putString("Profession", profession.getSerializedName());
		output.putInt("TradeLevel", tradeLevel);
		output.putInt("TradeXp", tradeXp);
		output.putInt("Gear", gear());
		output.putInt("Skin", skin());
		output.putLong("RestockDay", restockDay);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		role = Role.byName(input.getStringOr("Role", Role.MINER.getSerializedName()));
		Optional<String> saved = input.getString("Profession");
		if (saved.isPresent()) {
			setProfession(DuergarProfession.byName(saved.get()));
			tradeLevel = Math.clamp(input.getIntOr("TradeLevel", 1), 1, profession.maxLevel());
			tradeXp = input.getIntOr("TradeXp", 0);
		} else {
			// Placed by a template (only his role is written there), or saved before duergar had professions:
			// he takes a profession of his role, and his old offers make way for its trades.
			setProfession(DuergarProfession.pick(role, getRandom().nextDouble()));
			offers = null;
			tradeLevel = 1;
			tradeXp = 0;
		}
		entityData.set(DATA_GEAR, input.getIntOr("Gear", 0));
		entityData.set(DATA_SKIN, (byte) input.getIntOr("Skin", 0));
		restockDay = input.getLongOr("RestockDay", -1L);
		if (gear() == 0) {
			dress();
		}
	}
}
