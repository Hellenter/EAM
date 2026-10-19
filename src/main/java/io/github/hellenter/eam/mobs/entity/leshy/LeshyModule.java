package io.github.hellenter.eam.mobs.entity.leshy;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The leshy, master of the deep woods ({@link Leshy}). Numbers are in {@code data/eam/mobs/leshy.json}; where it spawns
 * is the biome tag {@code eam:has_leshies}, what grows over a player's path the block tag {@code eam:leshy_overgrowth},
 * and what buys it off the item tag {@code eam:leshy_offerings}.
 */
public final class LeshyModule {
	public static final TagKey<Biome> SPAWNS_IN = TagKey.create(Registries.BIOME, Eam.id("has_leshies"));
	public static final TagKey<Block> OVERGROWTH = TagKey.create(Registries.BLOCK, Eam.id("leshy_overgrowth"));
	public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, Eam.id("leshy_offerings"));
	/** Spawn weight among the biome's monsters (zombies and skeletons have about 100); the spawn rules thin them a lot more. */
	private static final int SPAWN_WEIGHT = 4;
	private static final int BEWILDERED_COLOR = 0x3E5A2A;

	/** Led astray by a leshy: compasses spin and the player's own marker on maps whirls. */
	public static final Holder<MobEffect> BEWILDERED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Eam.id("bewildered"),
			new BewilderedEffect(BEWILDERED_COLOR));
	/** Game time until which a player who laid an offering is left alone by every leshy. */
	public static final AttachmentType<Long> PEACE = AttachmentRegistry.create(Eam.id("leshy_peace"),
			builder -> builder.persistent(Codec.LONG).copyOnDeath());

	private static volatile LeshySettings settings = LeshySettings.DEFAULT;

	private LeshyModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.LESHY, Leshy.createAttributes());
		SpawnPlacements.register(EamEntities.LESHY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Leshy::checkSpawnRules);
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(LeshySettingsLoader.ID, new LeshySettingsLoader());
		// The wolves it called are spirits of the wood: any found when a chunk loads have lost their leshy.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) -> !(fromDisk && isLeshysWolf(entity)));
	}

	/** Natural spawning in forests and taiga; the mob itself exists whatever the config says. */
	public static void init() {
		BiomeModifications.addSpawn(BiomeSelectors.tag(SPAWNS_IN), MobCategory.MONSTER, EamEntities.LESHY, SPAWN_WEIGHT, 1, 1);
	}

	static LeshySettings settings() {
		return settings;
	}

	static void setSettings(LeshySettings value) {
		settings = value;
	}

	/** Whether {@code entity} is one of the wolves a leshy called. */
	public static boolean isLeshysWolf(Entity entity) {
		return entity instanceof Wolf && entity.entityTags().contains(Leshy.WOLF_TAG);
	}

	static boolean atPeace(Player player) {
		Long until = player.getAttached(PEACE);
		return until != null && LeshyLogic.atPeace(player.level().getGameTime(), until);
	}

	static void makePeace(Player player, long until) {
		player.setAttached(PEACE, until);
	}

	static void breakPeace(Player player) {
		player.removeAttached(PEACE);
	}

	/** Whether {@code entity} is led astray, for the compass and the map. */
	public static boolean bewildered(Entity entity) {
		return entity instanceof LivingEntity living && living.hasEffect(BEWILDERED);
	}

	/**
	 * The facing a map shows for {@code player}: their own, or, while they are led astray, a heading that jumps about
	 * every half second, as maps do in the Nether.
	 */
	public static float mapYaw(Player player, float yaw) {
		if (!bewildered(player)) {
			return yaw;
		}
		return LeshyLogic.spinningYaw(player.level().getGameTime(), player.getId());
	}
}
