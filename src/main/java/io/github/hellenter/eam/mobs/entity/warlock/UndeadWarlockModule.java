package io.github.hellenter.eam.mobs.entity.warlock;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.lost.Lost;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The undead warlock: a caster who walks with squads of the Lost ({@code squad_chance} of squads, 10% by default) and
 * later will guard monuments. He does not spawn on his own. Numbers are in {@code data/eam/mobs/undead_warlock.json}.
 */
public final class UndeadWarlockModule {
	private static volatile UndeadWarlockSettings settings = UndeadWarlockSettings.DEFAULT;

	private UndeadWarlockModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.UNDEAD_WARLOCK, UndeadWarlock.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(UndeadWarlockSettingsLoader.ID, new UndeadWarlockSettingsLoader());
	}

	static UndeadWarlockSettings settings() {
		return settings;
	}

	static void setSettings(UndeadWarlockSettings value) {
		settings = value;
	}

	/** Called as a squad of the Lost spawns: now and then a warlock comes along, a few steps behind the leader. */
	public static void maybeJoinSquad(ServerLevelAccessor level, DifficultyInstance difficulty, Lost leader) {
		RandomSource random = leader.getRandom();
		if (random.nextFloat() >= settings.squadChance()) {
			return;
		}
		UndeadWarlock warlock = EamEntities.UNDEAD_WARLOCK.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);
		if (warlock == null) {
			return;
		}
		Vec3 spot = spotNear(level, leader, warlock, random);
		warlock.snapTo(spot.x, spot.y, spot.z, random.nextFloat() * 360.0F, 0.0F);
		warlock.finalizeSpawn(level, difficulty, EntitySpawnReason.MOB_SUMMONED, null);
		warlock.follow(leader);
		if (leader.isPersistenceRequired()) {
			warlock.setPersistenceRequired();
		}
		level.addFreshEntityWithPassengers(warlock);
	}

	/** A free spot on solid ground two to four blocks from the leader, or his own. */
	private static Vec3 spotNear(ServerLevelAccessor level, Lost leader, UndeadWarlock warlock, RandomSource random) {
		BlockPos centre = leader.blockPosition();
		for (int attempt = 0; attempt < 16; attempt++) {
			BlockPos column = centre.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.above(dy);
				BlockPos below = pos.below();
				if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(pos).isEmpty()
						&& level.noCollision(warlock, warlock.getType().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
					return Vec3.atBottomCenterOf(pos);
				}
			}
		}
		return leader.position();
	}
}
