package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.vanilla.crypt.CryptSpawners;
import io.github.hellenter.eam.mobs.vanilla.crypt.CryptSpawners.Spawner;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Trial spawner configs of the crypt ({@link CryptSpawners}). The swordsman and archer spawn data carry an extra field,
 * which makes the spawner skip the skeleton's own spawn setup (a bow, random vanilla armor): all their gear comes from
 * the crypt's equipment tables. The knight sets up his own gear, which the tables then replace with enchanted pieces.
 */
public class EamCryptSpawnerProvider extends FabricDynamicRegistryProvider {
	public EamCryptSpawnerProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<TrialSpawnerConfig> context) {
		Consumer<CompoundTag> noSpawnSetup = tag -> tag.putBoolean("CanPickUpLoot", false);
		Consumer<CompoundTag> ancientArrows = tag -> {
			// Held in the off hand, the arrows are what the skeleton shoots; they can't be picked up.
			CompoundTag arrow = new CompoundTag();
			arrow.putString("id", "eam:ancient_metal_arrow");
			arrow.putInt("count", 1);
			CompoundTag equipment = new CompoundTag();
			equipment.put("offhand", arrow);
			tag.put("equipment", equipment);
			CompoundTag dropChances = new CompoundTag();
			dropChances.putFloat("offhand", 0.0F);
			tag.put("drop_chances", dropChances);
		};

		register(context, CryptSpawners.SWORDSMAN,
				skeletons().spawnPotentialsDefinition(spawn(EntityTypes.SKELETON, noSpawnSetup, CryptSpawners.SWORDSMAN.gear())),
				skeletons().spawnPotentialsDefinition(spawn(EntityTypes.SKELETON, noSpawnSetup, CryptSpawners.SWORDSMAN.ominousGear())));
		register(context, CryptSpawners.ARCHER,
				skeletons().spawnPotentialsDefinition(spawn(EntityTypes.SKELETON, ancientArrows, CryptSpawners.ARCHER.gear())),
				skeletons().spawnPotentialsDefinition(spawn(EntityTypes.SKELETON, ancientArrows, CryptSpawners.ARCHER.ominousGear())));
		// Only a few knights: 2 per spawner, 3 when ominous, however many players come.
		register(context, CryptSpawners.KNIGHT,
				knights(2.0F, 1.0F).spawnPotentialsDefinition(spawn(EamEntities.SKELETON_KNIGHT, tag -> {}, CryptSpawners.KNIGHT.gear())),
				knights(3.0F, 2.0F).spawnPotentialsDefinition(spawn(EamEntities.SKELETON_KNIGHT, tag -> {}, CryptSpawners.KNIGHT.ominousGear())));
	}

	/** Like vanilla trial chamber skeletons: 6 (+2 per extra player), 3 at once. */
	private static TrialSpawnerConfig.Builder skeletons() {
		return TrialSpawnerConfig.builder().spawnRange(CryptSpawners.SPAWN_RANGE).simultaneousMobs(3.0F).simultaneousMobsAddedPerPlayer(0.5F).ticksBetweenSpawn(20);
	}

	private static TrialSpawnerConfig.Builder knights(float total, float simultaneous) {
		return TrialSpawnerConfig.builder()
				.spawnRange(CryptSpawners.SPAWN_RANGE)
				.totalMobs(total)
				.totalMobsAddedPerPlayer(0.0F)
				.simultaneousMobs(simultaneous)
				.simultaneousMobsAddedPerPlayer(0.0F)
				.ticksBetweenSpawn(60);
	}

	private static void register(BootstrapContext<TrialSpawnerConfig> context, Spawner spawner, TrialSpawnerConfig.Builder normal,
			TrialSpawnerConfig.Builder ominous) {
		context.register(spawner.normal(), normal.build());
		context.register(spawner.ominous(), ominous.lootTablesToEject(WeightedList.<ResourceKey<LootTable>>builder()
				.add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
				.add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
				.build()).build());
	}

	private static WeightedList<SpawnData> spawn(EntityType<?> type, Consumer<CompoundTag> extra, ResourceKey<LootTable> gear) {
		CompoundTag tag = new CompoundTag();
		tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
		extra.accept(tag);
		return WeightedList.of(new SpawnData(tag, Optional.empty(), Optional.of(new EquipmentTable(gear, 0.0F))));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		var configs = registries.lookupOrThrow(Registries.TRIAL_SPAWNER_CONFIG);
		for (Spawner spawner : new Spawner[] {CryptSpawners.SWORDSMAN, CryptSpawners.ARCHER, CryptSpawners.KNIGHT}) {
			entries.add(configs, spawner.normal());
			entries.add(configs, spawner.ominous());
		}
	}

	@Override
	public String getName() {
		return "EAM crypt trial spawners";
	}
}
