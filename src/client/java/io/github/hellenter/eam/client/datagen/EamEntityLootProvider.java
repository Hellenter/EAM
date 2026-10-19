package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderGetter;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Extra death loot added to vanilla mobs by {@code MobDropsModule}, and our own mobs' loot. */
public class EamEntityLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;

	public EamEntityLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.ENTITY);
		this.registries = registries;
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		var enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		// A cow's leather roll (0-2, Looting adds 0-1 per level) that happens one kill in three: a third as much leather.
		output.accept(MobDropsModule.SMALL_LEATHER_LOOT, LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.when(LootItemRandomChanceCondition.randomChance(1.0F / 3.0F))
				.add(LootItem.lootTableItem(Items.LEATHER)
						.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
						.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The skeleton knight: bones like a skeleton, his ancient metal gear never drops.
		output.accept(EamEntities.SKELETON_KNIGHT.getDefaultLootTable().orElseThrow(), LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(Items.BONE)
						.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))
						.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The Fallen Lord: his black talisman, always exactly one; Looting adds nothing. And 1-2 pieces of his mithril
		// armor, well enchanted but battered: always a helmet or chestplate, half the time leggings or boots too.
		output.accept(EamEntities.FALLEN_LORD.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.BLACK_TALISMAN)))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(lordArmor(enchantments, EamItems.MITHRIL_HELMET))
						.add(lordArmor(enchantments, EamItems.MITHRIL_CHESTPLATE)))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.when(LootItemRandomChanceCondition.randomChance(0.5F))
						.add(lordArmor(enchantments, EamItems.MITHRIL_LEGGINGS))
						.add(lordArmor(enchantments, EamItems.MITHRIL_BOOTS))));

		// The Lost: half husk, half skeleton. Their worn gear drops by the mob's own drop chances; their centurion's too.
		for (EntityType<?> lost : List.of(EamEntities.LOST, EamEntities.LOST_CENTURION, EamEntities.LOST_WALL_COMMANDER)) {
			output.accept(lost.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
					.withPool(LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(Items.BONE)
									.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
									.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
					.withPool(LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
									.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
									.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));
		}

		// The heavy cavalryman: like the Lost; his worn plate and weapons drop by the mob's own drop chances.
		output.accept(EamEntities.LOST_CAVALRYMAN.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BONE)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// His horse: bones and dried flesh; the barding is part of it and stays on it.
		output.accept(EamEntities.LOST_WARHORSE.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BONE)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// Koschei drops only experience until his last phase is built.
		// His needle, always.
		output.accept(EamEntities.KOSCHEI.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool().add(LootItem.lootTableItem(EamItems.KOSCHEI_NEEDLE))));
		output.accept(EamEntities.KOSCHEI_RAVEN.getDefaultLootTable().orElseThrow(), LootTable.lootTable());
		output.accept(EamEntities.KOSCHEI_BALL_LIGHTNING.getDefaultLootTable().orElseThrow(), LootTable.lootTable());
		output.accept(EamEntities.KOSCHEI_ICE_RAVEN.getDefaultLootTable().orElseThrow(), LootTable.lootTable());
		output.accept(EamEntities.KOSCHEI_ICE_COPY.getDefaultLootTable().orElseThrow(), LootTable.lootTable());
		// The Pillager King: his helm, a trophy that keeps raids away; a map to the nearest crypt; a little of his treasure.
		output.accept(EamEntities.PILLAGER_KING.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.PILLAGER_KING_HELM)))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.CRYPT_MAP)))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.EMERALD)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.DIAMOND)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))));

		// The elite crossbowman: a few bolts from his quiver; his crossbow drops by the mob's own drop chance, like a pillager's.
		output.accept(EamEntities.ELITE_CROSSBOWMAN.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ARROW)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 3)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// A pillager guard: a vindicator's emerald for a player's kill, and a few iron nuggets from his battered cuirass.
		output.accept(EamEntities.PILLAGER_GUARD.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.when(LootItemKilledByPlayerCondition.killedByPlayer())
						.add(LootItem.lootTableItem(Items.EMERALD)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.IRON_NUGGET)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The undead warlock: like the Lost, plus a rare piece of the chainmail from under his robe.
		output.accept(EamEntities.UNDEAD_WARLOCK.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BONE)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.when(LootItemKilledByPlayerCondition.killedByPlayer())
						.when(LootItemRandomChanceCondition.randomChance(0.08F))
						.add(LootItem.lootTableItem(Items.CHAINMAIL_CHESTPLATE)
								.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.2F, 0.6F))))));

		// Baba Yaga: always a Mending book, and the ash she burnt to. Her grave map is added in code, as it remembers
		// where she died.
		output.accept(EamEntities.BABA_YAGA.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BOOK)
								.apply(new SetEnchantmentsFunction.Builder().withEnchantment(enchantments.getOrThrow(Enchantments.MENDING),
										ContextIntProviders.exactly(1)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.ASH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(3, 5))))));

		// The drowned soldier: rotten flesh, and bog weed.
		output.accept(EamEntities.DROWNED_SOLDIER.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.KELP)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The upyr: rotten flesh, and now and then a bone.
		output.accept(EamEntities.UPYR.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BONE)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// Wargs: wolf meat, more from the leader.
		output.accept(EamEntities.WARG.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.WOLF_MEAT)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));
		output.accept(EamEntities.WARG_LEADER.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.WOLF_MEAT)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// Underground boars: pork like a pig, cooked if it burned, and a cow's leather.
		output.accept(EamEntities.BOAR.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.PORKCHOP)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))
								.apply(SmeltItemFunction.smelted().when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
										EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true)))))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.LEATHER)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The swamp lurker: rotten flesh, and threads from its robe.
		output.accept(EamEntities.SWAMP_LURKER.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.STRING)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The leshy: a tine of its antlers, sticks and moss.
		output.accept(EamEntities.LESHY.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(EamItems.LESHY_ANTLER)))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.STICK)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.MOSS_BLOCK)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The wight: rotten flesh, and string from its rope bindings.
		output.accept(EamEntities.WIGHT.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.STRING)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The zombie duergar: rotten flesh, now and then an iron nugget from his pockets.
		output.accept(EamEntities.ZOMBIE_DUERGAR.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.IRON_NUGGET)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(-2, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The duergar renegade: iron nuggets knocked off his armor, now and then a bit of leather.
		output.accept(EamEntities.DUERGAR_RENEGADE.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.IRON_NUGGET)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 4)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.LEATHER)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(-1, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The draugr: rotten flesh and a bone or two.
		output.accept(EamEntities.DRAUGR.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F)))))
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.add(LootItem.lootTableItem(Items.BONE)
								.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 1)))
								.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));

		// The vampire bat: its heart, one kill in four; each Looting level adds 11.33%.
		output.accept(EamEntities.VAMPIRE_BAT.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ContextIntProviders.exactly(1))
						.when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(enchantments, 0.25F, 0.1133F))
						.add(LootItem.lootTableItem(EamItems.VAMPIRE_HEART))));

		// The small spiders: a string four times in ten, as in MITE.
		for (EntityType<?> spider : List.of(EamEntities.WOOD_SPIDER, EamEntities.BLACK_WIDOW)) {
			output.accept(spider.getDefaultLootTable().orElseThrow(), LootTable.lootTable()
					.withPool(LootPool.lootPool()
							.setRolls(ContextIntProviders.exactly(1))
							.when(LootItemRandomChanceCondition.randomChance(0.4F))
							.add(LootItem.lootTableItem(Items.STRING)
									.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));
		}
	}

	/** A piece of the Lord's armor: enchanted like on a table with 25-35 levels, 40-70% of its durability left. */
	private static LootPoolEntryContainer.Builder<?> lordArmor(HolderGetter<Enchantment> enchantments, ItemLike armor) {
		return LootItem.lootTableItem(armor)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.4F, 0.7F)))
				.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(25, 35))
						.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE)));
	}
}
