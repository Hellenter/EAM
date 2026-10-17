package io.github.hellenter.eam.mobs.vanilla.villager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The village alarm. A grown villager hurt by a monster runs to the nearest bell and strikes it. The bell makes the other
 * villagers hide as in vanilla, and for a while the guards near it gather around the bell and go for the monster that
 * hurt him, and the clerics come out to tend the guards: each walks to the guard who needs him most, heals him and hands
 * him a potion. Every villager hurt near a ringing bell keeps the alarm going. Numbers are in
 * {@code data/eam/villagers/alarm.json}.
 */
public final class VillagerAlarmModule {
	private static final int WALK_REPATH_TICKS = 10;
	private static final double CLERIC_REACH = 2.5;

	private static volatile AlarmSettings settings = AlarmSettings.DEFAULT;
	private static boolean enabled;

	/** Ringing bells by level and bell position. Kept in memory only: an alarm lasts about a minute. */
	private static final Map<ResourceKey<Level>, Map<BlockPos, Alarm>> ALARMS = new HashMap<>();
	/** Villagers running to a bell to ring it. */
	private static final Map<Villager, Run> RUNNERS = new WeakHashMap<>();
	/** When each cleric answering an alarm may tend his next guard. */
	private static final Map<Villager, Long> CLERIC_REST = new WeakHashMap<>();

	private VillagerAlarmModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(AlarmSettingsLoader.ID, new AlarmSettingsLoader());
	}

	public static void init() {
		enabled = true;
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof Villager villager && damageTaken > 0.0F && source.getEntity() instanceof LivingEntity attacker
					&& villager.level() instanceof ServerLevel level) {
				onHurt(villager, attacker, level);
			}
		});
	}

	static void setSettings(AlarmSettings value) {
		settings = value;
	}

	/** Whether the vanilla villager brain runs this tick: not while he runs to the bell or answers its alarm. */
	public static boolean thinks(Villager villager) {
		if (!enabled || !(villager.level() instanceof ServerLevel level)) return true;
		boolean busy = RUNNERS.containsKey(villager) || answers(villager) && alarmNear(level, villager) != null;
		if (busy && villager.isSleeping()) villager.stopSleeping();
		return !busy;
	}

	/** Called every tick a villager thinks on the server: the runner rings, guards gather, clerics tend them. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		AlarmSettings current = settings;
		Run run = RUNNERS.get(villager);
		if (run != null) {
			runToBell(villager, level, run, current);
			return;
		}
		if (!answers(villager)) return;
		Map.Entry<BlockPos, Alarm> alarm = alarmNear(level, villager);
		if (alarm == null) return;
		BlockPos bell = alarm.getKey();
		if (VillagerGuardModule.isGuard(villager)) {
			guard(villager, level, bell, alarm.getValue(), current);
		} else {
			cleric(villager, level, bell, current);
		}
	}

	private static void onHurt(Villager villager, LivingEntity attacker, ServerLevel level) {
		if (villager.isBaby() || !(attacker instanceof Enemy)) return;
		AlarmSettings current = settings;
		long now = level.getGameTime();
		Map.Entry<BlockPos, Alarm> ringing = alarmNear(level, villager);
		if (ringing != null) {
			ringing.getValue().refresh(now + current.alarmTicks(), attacker);
			return;
		}
		if (!VillagerAlarmLogic.raisesAlarm(!villager.isBaby(), VillagerGuardModule.isGuard(villager), true) || RUNNERS.containsKey(villager)) return;
		Optional<BlockPos> bell = level.getPoiManager().findClosest(type -> type.is(PoiTypes.MEETING), villager.blockPosition(),
				current.bellRadius(), PoiManager.Occupancy.ANY);
		if (bell.isEmpty() || runnerTo(bell.get())) return;
		RUNNERS.put(villager, new Run(bell.get().immutable(), now, attacker.getUUID()));
	}

	private static void runToBell(Villager villager, ServerLevel level, Run run, AlarmSettings current) {
		long now = level.getGameTime();
		BlockState state = level.getBlockState(run.bell);
		if (!(state.getBlock() instanceof BellBlock bell) || run.rung == 0 && VillagerAlarmLogic.givesUp(now, run.startedAt, current.runTimeoutTicks())) {
			RUNNERS.remove(villager);
			return;
		}
		Vec3 center = Vec3.atCenterOf(run.bell);
		if (villager.position().distanceToSqr(center) > current.ringReach() * current.ringReach()) {
			if (VillagerRepairLogic.checksNow(now, villager.getId(), WALK_REPATH_TICKS) || villager.getNavigation().isDone()) {
				villager.getNavigation().moveTo(center.x, center.y, center.z, current.runSpeed());
			}
			return;
		}
		villager.getNavigation().stop();
		villager.getLookControl().setLookAt(center);
		if (now < run.nextRing) return;
		Vec3 offset = villager.position().subtract(center);
		bell.attemptToRing(villager, level, run.bell, Direction.getApproximateNearest(offset.x, 0.0, offset.z));
		run.rung++;
		run.nextRing = now + current.ringIntervalTicks();
		Entity attacker = level.getEntity(run.attacker);
		ALARMS.computeIfAbsent(level.dimension(), key -> new HashMap<>())
				.computeIfAbsent(run.bell, key -> new Alarm())
				.refresh(now + current.alarmTicks(), attacker instanceof LivingEntity living ? living : null);
		if (run.rung >= current.rings()) RUNNERS.remove(villager);
	}

	/** A guard goes for the monster that raised the alarm once it is within his reach, and until then gathers by the bell. */
	private static void guard(Villager guard, ServerLevel level, BlockPos bell, Alarm alarm, AlarmSettings current) {
		if (guard.getTarget() != null) return;
		LivingEntity attacker = alarm.attacker(level);
		if (attacker != null && attacker.isAlive() && attacker.blockPosition().closerThan(bell, current.bellRadius())
				&& guard.closerThan(attacker, guard.getAttributeValue(Attributes.FOLLOW_RANGE))) {
			guard.setTarget(attacker);
			return;
		}
		walkTo(guard, level, Vec3.atBottomCenterOf(bell), current.gatherDistance(), current.gatherSpeed());
	}

	/** A cleric walks to the guard near the bell who needs him most, heals him and hands him a potion. */
	private static void cleric(Villager cleric, ServerLevel level, BlockPos bell, AlarmSettings current) {
		long now = level.getGameTime();
		if (now < CLERIC_REST.getOrDefault(cleric, 0L)) return;
		int potionMax = VillagerClericModule.potions(cleric.getInventory()) > 0 ? VillagerClericModule.settings().potionMax() : 0;
		Villager neediest = null;
		float most = 0.0F;
		for (Villager other : level.getEntitiesOfClass(Villager.class, new AABB(bell).inflate(current.bellRadius()),
				other -> other != cleric && other.isAlive() && VillagerGuardModule.isGuard(other))) {
			float need = VillagerAlarmLogic.need(other.getHealth(), other.getMaxHealth(), VillagerClericModule.potions(other.getInventory()), potionMax);
			if (need > most) {
				most = need;
				neediest = other;
			}
		}
		if (neediest == null) {
			walkTo(cleric, level, Vec3.atBottomCenterOf(bell), current.gatherDistance(), current.gatherSpeed());
			return;
		}
		if (cleric.distanceToSqr(neediest) > CLERIC_REACH * CLERIC_REACH) {
			walkTo(cleric, level, neediest.position(), 0.0, current.gatherSpeed());
			return;
		}
		cleric.getNavigation().stop();
		cleric.getLookControl().setLookAt(neediest);
		VillagerClericModule.tendGuard(level, cleric, neediest, current.clericHeal());
		CLERIC_REST.put(cleric, now + current.clericRestTicks());
	}

	private static void walkTo(Villager villager, ServerLevel level, Vec3 to, double distance, double speed) {
		if (!VillagerAlarmLogic.walksToBell(villager.position().distanceToSqr(to), distance)) {
			villager.getNavigation().stop();
			return;
		}
		if (VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), WALK_REPATH_TICKS) || villager.getNavigation().isDone()) {
			villager.getNavigation().moveTo(to.x, to.y, to.z, speed);
		}
	}

	/** Guards answer an alarm, and so do clerics while they tend the village. */
	private static boolean answers(Villager villager) {
		if (villager.isBaby()) return false;
		if (VillagerGuardModule.isGuard(villager)) return true;
		return VillagerClericModule.enabled() && villager.getVillagerData().profession().is(VillagerProfession.CLERIC);
	}

	private static Map.@Nullable Entry<BlockPos, Alarm> alarmNear(ServerLevel level, Villager villager) {
		Map<BlockPos, Alarm> alarms = ALARMS.get(level.dimension());
		if (alarms == null || alarms.isEmpty()) return null;
		long now = level.getGameTime();
		int radius = settings.bellRadius();
		Map.Entry<BlockPos, Alarm> nearest = null;
		double best = Double.MAX_VALUE;
		for (Iterator<Map.Entry<BlockPos, Alarm>> it = alarms.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<BlockPos, Alarm> entry = it.next();
			if (entry.getValue().until < now) {
				it.remove();
				continue;
			}
			double distance = entry.getKey().distToCenterSqr(villager.position());
			if (distance <= (double) radius * radius && distance < best) {
				best = distance;
				nearest = entry;
			}
		}
		return nearest;
	}

	private static boolean runnerTo(BlockPos bell) {
		for (Map.Entry<Villager, Run> entry : RUNNERS.entrySet()) {
			if (entry.getKey().isAlive() && entry.getValue().bell.equals(bell)) return true;
		}
		return false;
	}

	private static final class Run {
		final BlockPos bell;
		final long startedAt;
		final UUID attacker;
		int rung;
		long nextRing;

		Run(BlockPos bell, long startedAt, UUID attacker) {
			this.bell = bell;
			this.startedAt = startedAt;
			this.attacker = attacker;
		}
	}

	private static final class Alarm {
		long until;
		@Nullable UUID attacker;

		void refresh(long until, @Nullable LivingEntity attacker) {
			this.until = Math.max(this.until, until);
			if (attacker != null) this.attacker = attacker.getUUID();
		}

		@Nullable LivingEntity attacker(ServerLevel level) {
			return attacker != null && level.getEntity(attacker) instanceof LivingEntity living ? living : null;
		}
	}
}
