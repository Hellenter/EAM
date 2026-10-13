package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A duergar of a clan village in the underworld: a smith, a miner or the elder. They trade with players (the trade sets
 * {@code eam:duergar/<role>}, datapack data): mithril for jewels, and the elder sells a map to the nearest lair of a
 * boss. They keep near home, run from undead duergar and renegades, and their stock comes back every morning.
 */
public class DuergarVillager extends AbstractVillager implements DuergarLook {
	private static final EntityDataAccessor<Integer> DATA_GEAR = SynchedEntityData.defineId(DuergarVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(DuergarVillager.class, EntityDataSerializers.BYTE);
	/** How many skins the renderer has: one per clan colour of their clothes. */
	public static final int SKINS = 4;
	/** How far from where he was placed or spawned he wanders. */
	private static final int HOME_RADIUS = 24;
	private static final long DAY = 24000L;

	/** What he does in the village, and so what he trades. */
	public enum Role implements StringRepresentable {
		SMITH("smith"),
		MINER("miner"),
		ELDER("elder");

		private final String name;
		private final ResourceKey<TradeSet> trades;

		Role(String name) {
			this.name = name;
			this.trades = ResourceKey.create(Registries.TRADE_SET, Eam.id("duergar/" + name));
		}

		public ResourceKey<TradeSet> trades() {
			return trades;
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
		if (reason != EntitySpawnReason.STRUCTURE && reason != EntitySpawnReason.LOAD) {
			// A village places each duergar with his role; one from an egg gets a random one.
			role = Role.values()[random.nextInt(Role.values().length)];
		}
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
			openTradingScreen(player, getDisplayName(), 1);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		addOffersFromTradeSet(level, getOffers(), role.trades());
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
	public boolean showProgressBar() {
		return false;
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		if (offer.shouldRewardExp()) {
			level().addFreshEntity(new ExperienceOrb(level(), getX(), getY() + 0.5, getZ(), 3 + random.nextInt(4)));
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
		output.putInt("Gear", gear());
		output.putInt("Skin", skin());
		output.putLong("RestockDay", restockDay);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		role = Role.byName(input.getStringOr("Role", Role.MINER.getSerializedName()));
		entityData.set(DATA_GEAR, input.getIntOr("Gear", 0));
		entityData.set(DATA_SKIN, (byte) input.getIntOr("Skin", 0));
		restockDay = input.getLongOr("RestockDay", -1L);
		if (gear() == 0) {
			dress();
		}
	}
}
