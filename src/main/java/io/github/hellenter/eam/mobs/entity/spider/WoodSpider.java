package io.github.hellenter.eam.mobs.entity.spider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * MITE's wood spider: a small bark-brown spider that lives in tree crowns. It has a third of a spider's health and a
 * weak bite that poisons for a moment. Unlike the vanilla spider it stays hostile by day, but in bright light it only
 * notices a player half as far away. It climbs trunks, sits on the leaves and drops onto whoever walks below.
 */
public class WoodSpider extends Spider {
	public WoodSpider(EntityType<? extends WoodSpider> type, Level level) {
		super(type, level);
		xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Spider.createAttributes()
				.add(Attributes.MAX_HEALTH, 6.0)
				.add(Attributes.ATTACK_DAMAGE, 1.0);
	}

	/** Its numbers; the black widow has its own. */
	protected SpiderSettings settings() {
		return SpiderModule.woodSpider();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
			@Override
			protected double getFollowDistance() {
				return WoodSpiderLogic.followDistance(super.getFollowDistance(), mob.getLightLevelDependentMagicValue(),
						settings().brightRange());
			}
		});
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (!super.doHurtTarget(level, target)) {
			return false;
		}
		int ticks = WoodSpiderLogic.poisonTicks(settings().poisonTicks(), level.getDifficulty().getId());
		if (ticks > 0 && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0), this);
		}
		return true;
	}

	/** No skeleton riders and no random potion effects: it is a small early-game pest. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData groupData) {
		return groupData;
	}

	@Override
	public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
		return vehicle.getBbWidth() <= getBbWidth() ? new Vec3(0.0, 0.1875 * getScale(), 0.0) : super.getVehicleAttachmentPoint(vehicle);
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.2F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}

	/**
	 * Natural spawns: in the dark like any monster, or now and then by day when it stands on leaves; next to a tree if
	 * its settings ask for one, and never in a crowd of its own kind. Spawners and eggs ignore all that.
	 */
	public static boolean checkSpawnRules(EntityType<? extends WoodSpider> type, ServerLevelAccessor level, EntitySpawnReason reason,
			BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.ignoresLightRequirements(reason)) {
			return true;
		}
		SpiderSettings settings = SpiderModule.settings(type);
		if (random.nextFloat() >= settings.spawnChance()) {
			return false;
		}
		boolean onLeaves = level.getBlockState(pos.below()).is(BlockTags.LEAVES);
		if (!WoodSpiderLogic.lightAllows(Monster.isDarkEnoughToSpawn(level, pos, random), onLeaves, random.nextFloat(),
				settings.daySpawnChance())) {
			return false;
		}
		if (settings.needsTrees() && !nextToTree(level, pos, onLeaves)) {
			return false;
		}
		return level.getEntitiesOfClass(WoodSpider.class, new AABB(pos).inflate(16.0), spider -> spider.getType() == type).size()
				< settings.maxNearby();
	}

	/** A log within 4 blocks across and 6 up or down, and leaves under its feet or above it. */
	private static boolean nextToTree(LevelReader level, BlockPos pos, boolean onLeaves) {
		boolean log = false;
		boolean leaves = onLeaves;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dy = -6; dy <= 6; dy++) {
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					BlockState state = level.getBlockState(cursor.setWithOffset(pos, dx, dy, dz));
					log |= state.is(BlockTags.LOGS);
					leaves |= dy > 0 && state.is(BlockTags.LEAVES);
					if (log && leaves) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
