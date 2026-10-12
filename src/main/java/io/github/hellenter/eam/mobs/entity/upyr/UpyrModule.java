package io.github.hellenter.eam.mobs.entity.upyr;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.world.structures.VillageModule;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * The upyr, a bloodsucking corpse that crawls out at night by abandoned villages, and the two wards against
 * bloodsuckers: eaten garlic ({@link #GARLIC}) makes upyrs and vampire bats hit the player for less, and black blood
 * ({@link #BLACK_BLOOD}) hurts them each time they hurt the player. Numbers are in {@code data/eam/mobs/upyr.json}.
 */
public final class UpyrModule {
	private static final int GARLIC_COLOR = 0xE8E2C8;
	private static final int BLACK_BLOOD_COLOR = 0x3A0A12;

	/** From eating garlic: upyrs and vampire bats hit for less and upyrs will not drink. */
	public static final Holder<MobEffect> GARLIC = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("garlic"),
			new WardEffect(GARLIC_COLOR));
	/** From the black blood potion: upyrs and vampire bats that hurt the drinker get hurt in turn. */
	public static final Holder<MobEffect> BLACK_BLOOD = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("black_blood"),
			new WardEffect(BLACK_BLOOD_COLOR));

	private static final Identifier GARLIC_MODIFIER = Eam.id("garlic_ward");
	/** Tries to find a dark spot in the village for an upyr before giving up until the next check. */
	private static final int PLACE_ATTEMPTS = 16;
	/** Upyrs this close to the player count toward the limit. */
	private static final double NEARBY = 48.0;
	/** An upyr may also crawl out on the outskirts, this far around the village. */
	private static final int OUTSKIRTS = 16;

	private static volatile UpyrSettings settings = UpyrSettings.DEFAULT;

	private UpyrModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.UPYR, Upyr.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(UpyrSettingsLoader.ID, new UpyrSettingsLoader());
	}

	/** Spawning by abandoned villages; the upyr and the wards exist whatever the config says. */
	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(UpyrModule::tick);
	}

	static UpyrSettings settings() {
		return settings;
	}

	static void setSettings(UpyrSettings value) {
		settings = value;
	}

	/**
	 * A bloodsucker's blow at {@code target}, done by {@code attack} (the vanilla one): weaker when the target ate garlic,
	 * and paid back in the attacker's own blood when the target has black blood in them.
	 *
	 * @return whether the target was hurt
	 */
	public static boolean wardedAttack(Mob attacker, ServerLevel level, Entity target, BooleanSupplier attack) {
		UpyrSettings current = settings;
		boolean garlic = hasGarlic(target);
		AttributeInstance damage = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
		if (garlic && damage != null) {
			damage.addOrUpdateTransientModifier(new AttributeModifier(GARLIC_MODIFIER,
					UpyrLogic.wardedDamage(1.0F, true, current.garlicDamageFactor()) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		boolean hurt;
		try {
			hurt = attack.getAsBoolean();
		} finally {
			if (garlic && damage != null) {
				damage.removeModifier(GARLIC_MODIFIER);
			}
		}
		if (hurt && target instanceof LivingEntity living && living.hasEffect(BLACK_BLOOD) && current.blackBloodDamage() > 0.0F) {
			// Thorns credit the drinker with the hurt, so a bloodsucker killed this way is their kill.
			attacker.hurtServer(level, level.damageSources().thorns(target), current.blackBloodDamage());
		}
		return hurt;
	}

	static boolean hasGarlic(Entity target) {
		return target instanceof LivingEntity living && living.hasEffect(GARLIC);
	}

	// ---- Spawning ----

	private static void tick(ServerLevel level) {
		UpyrSettings current = settings;
		if (level.dimension() != Level.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
				|| !level.getGameRules().get(GameRules.SPAWN_MONSTERS) || level.getGameTime() % current.checkInterval() != 0 || !level.isDarkOutside()) {
			return;
		}
		Optional<HolderSet.Named<Structure>> villages = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(StructureTags.VILLAGE);
		if (villages.isEmpty()) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
				continue;
			}
			int nearby = level.getEntitiesOfClass(Upyr.class, player.getBoundingBox().inflate(NEARBY)).size();
			if (!UpyrLogic.spawnDue(nearby, current.maxNearby(), level.getRandom().nextFloat(), current.spawnChance())) {
				continue;
			}
			BoundingBox village = abandonedVillageAround(level, player.blockPosition(), villages.get());
			if (village != null) {
				spawn(level, player, village, current);
			}
		}
	}

	/** The box of the abandoned village the player is in or on the outskirts of, or null. */
	private static @Nullable BoundingBox abandonedVillageAround(ServerLevel level, BlockPos pos, HolderSet<Structure> villages) {
		for (BlockPos probe : new BlockPos[] {pos, pos.offset(OUTSKIRTS, 0, 0), pos.offset(-OUTSKIRTS, 0, 0), pos.offset(0, 0, OUTSKIRTS),
				pos.offset(0, 0, -OUTSKIRTS)}) {
			StructureStart start = level.structureManager().getStructureAt(probe, villages);
			if (start.isValid() && VillageModule.isAbandoned(start)) {
				return start.getBoundingBox().inflatedBy(OUTSKIRTS);
			}
		}
		return null;
	}

	private static void spawn(ServerLevel level, ServerPlayer player, BoundingBox village, UpyrSettings current) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < PLACE_ATTEMPTS; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = UpyrLogic.spawnDistance(current.minDistance(), current.maxDistance(), random.nextFloat());
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			// Checked before the heightmap is read, so no chunk gets loaded for it.
			if (!village.isInside(new BlockPos(x, village.minY(), z)) || !level.isPositionEntityTicking(new BlockPos(x, player.getBlockY(), z))) {
				continue;
			}
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			if (level.getMaxLocalRawBrightness(pos) > 7 || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(),
							Direction.UP)) {
				continue;
			}
			Upyr upyr = EamEntities.UPYR.create(level, EntitySpawnReason.EVENT);
			if (upyr == null) {
				return;
			}
			upyr.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(upyr) || level.containsAnyLiquid(upyr.getBoundingBox())) {
				upyr.discard();
				continue;
			}
			upyr.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
			level.addFreshEntity(upyr);
			return;
		}
	}
}
