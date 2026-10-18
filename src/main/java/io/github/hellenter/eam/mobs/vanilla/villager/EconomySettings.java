package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Balance numbers for the village economy, read from {@code data/eam/villagers/economy.json} so datapacks can override
 * them. Amounts are per half a day ({@code period_ticks}).
 *
 * @param periodTicks      how often, in ticks, each villager brings his share to the elder and eats from the store
 * @param upkeep           provisions each villager, children and guards too, eats each time
 * @param breedCost        provisions a child costs; villagers do not breed while the store has less
 * @param villagersPerGuard grown villagers per guard the elder appoints
 * @param guardCheckTicks  how often, in ticks, the elder checks he has guards enough
 * @param villagersPerGolem villagers per iron golem the village may order (one at least)
 * @param levelBonus       extra share per trade level past the first
 * @param componentBonus   extra share per component used up
 * @param componentsMax    components a villager uses up at most each time
 * @param farmRadius       how far, in blocks, from his job site a farmer's fields reach
 * @param professions      what each profession brings
 * @param toolTiers        the share multiplier of a tool by the tier of its material, named by the blocks it cannot mine
 * @param untieredTool     the multiplier of a tool with no tier, such as shears or a fishing rod
 * @param orders           what the elder can order with the stock, by id
 * @param builders         villagers who build one building at a time at most
 * @param ticksPerBlock    ticks a builder takes for each block
 */
public record EconomySettings(int periodTicks, double upkeep, double breedCost, int villagersPerGuard, int guardCheckTicks,
		int villagersPerGolem, double levelBonus, double componentBonus, int componentsMax, int farmRadius,
		Map<Identifier, Yield> professions, Map<TagKey<Block>, Double> toolTiers, double untieredTool, Map<String, Order> orders,
		int builders, int ticksPerBlock) {

	/**
	 * What a villager of one profession brings.
	 *
	 * @param resource    the kind he brings
	 * @param base        how much he brings with no tool, level or components
	 * @param perFarmland how much more for each farmland block of his fields
	 * @param components  what he uses up for a bigger share
	 */
	public record Yield(VillageResource resource, double base, double perFarmland, Optional<TagKey<Item>> components) {
		public static final Codec<Yield> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				VillageResource.CODEC.fieldOf("resource").forGetter(Yield::resource),
				Codec.doubleRange(0.0, 1000.0).fieldOf("base").forGetter(Yield::base),
				Codec.doubleRange(0.0, 100.0).optionalFieldOf("per_farmland", 0.0).forGetter(Yield::perFarmland),
				TagKey.codec(Registries.ITEM).optionalFieldOf("components").forGetter(Yield::components)
		).apply(instance, Yield::new));
	}

	/**
	 * Something the elder can order.
	 *
	 * @param cost      what it takes from the stock
	 * @param templates for a blueprint, the structure templates of the building by the village's villager type (its
	 *                  style, such as {@code plains} or {@code desert}), one picked at random, or {@code default} for
	 *                  any other type; empty for anything else
	 */
	public record Order(Map<VillageResource, Double> cost, Map<String, List<Identifier>> templates) {
		public static final Codec<Order> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.unboundedMap(VillageResource.CODEC, Codec.doubleRange(0.0, 100_000.0)).fieldOf("cost").forGetter(Order::cost),
				Codec.unboundedMap(Codec.STRING, Identifier.CODEC.listOf()).optionalFieldOf("templates", Map.of()).forGetter(Order::templates)
		).apply(instance, Order::new));

		public boolean blueprint() {
			return !templates.isEmpty();
		}
	}

	/** The id of the iron golem order. */
	public static final String IRON_GOLEM = "iron_golem";

	public static final EconomySettings DEFAULT = new EconomySettings(12_000, 0.25, 6.0, 8, 600, 8, 0.25, 0.15, 4, 16,
			defaultProfessions(), defaultToolTiers(), 1.5, defaultOrders(), 3, 12);

	public static final Codec<EconomySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(200, 1_000_000).fieldOf("period_ticks").orElse(DEFAULT.periodTicks).forGetter(EconomySettings::periodTicks),
			Codec.doubleRange(0.0, 100.0).fieldOf("upkeep").orElse(DEFAULT.upkeep).forGetter(EconomySettings::upkeep),
			Codec.doubleRange(0.0, 10_000.0).fieldOf("breed_cost").orElse(DEFAULT.breedCost).forGetter(EconomySettings::breedCost),
			Codec.intRange(0, 1000).fieldOf("villagers_per_guard").orElse(DEFAULT.villagersPerGuard).forGetter(EconomySettings::villagersPerGuard),
			Codec.intRange(20, 1_000_000).fieldOf("guard_check_ticks").orElse(DEFAULT.guardCheckTicks).forGetter(EconomySettings::guardCheckTicks),
			Codec.intRange(0, 1000).fieldOf("villagers_per_golem").orElse(DEFAULT.villagersPerGolem).forGetter(EconomySettings::villagersPerGolem),
			Codec.doubleRange(0.0, 10.0).fieldOf("level_bonus").orElse(DEFAULT.levelBonus).forGetter(EconomySettings::levelBonus),
			Codec.doubleRange(0.0, 10.0).fieldOf("component_bonus").orElse(DEFAULT.componentBonus).forGetter(EconomySettings::componentBonus),
			Codec.intRange(0, 64).fieldOf("components_max").orElse(DEFAULT.componentsMax).forGetter(EconomySettings::componentsMax),
			Codec.intRange(1, 48).fieldOf("farm_radius").orElse(DEFAULT.farmRadius).forGetter(EconomySettings::farmRadius),
			Codec.unboundedMap(Identifier.CODEC, Yield.CODEC).fieldOf("professions").orElse(DEFAULT.professions).forGetter(EconomySettings::professions),
			Codec.unboundedMap(TagKey.codec(Registries.BLOCK), Codec.doubleRange(0.0, 100.0)).fieldOf("tool_tiers").orElse(DEFAULT.toolTiers)
					.forGetter(EconomySettings::toolTiers),
			Codec.doubleRange(0.0, 100.0).fieldOf("untiered_tool").orElse(DEFAULT.untieredTool).forGetter(EconomySettings::untieredTool),
			Codec.unboundedMap(Codec.STRING, Order.CODEC).fieldOf("orders").orElse(DEFAULT.orders).forGetter(EconomySettings::orders),
			Codec.intRange(1, 16).fieldOf("builders").orElse(DEFAULT.builders).forGetter(EconomySettings::builders),
			Codec.intRange(1, 1200).fieldOf("ticks_per_block").orElse(DEFAULT.ticksPerBlock).forGetter(EconomySettings::ticksPerBlock)
	).apply(instance, EconomySettings::new));

	private static Map<Identifier, Yield> defaultProfessions() {
		Map<Identifier, Yield> map = new LinkedHashMap<>();
		map.put(VillagerProfession.FARMER.identifier(), new Yield(VillageResource.PROVISIONS, 0.5, 0.025, component("farmer")));
		map.put(VillagerProfession.FISHERMAN.identifier(), new Yield(VillageResource.PROVISIONS, 1.5, 0.0, component("fisherman")));
		map.put(VillagerProfession.BUTCHER.identifier(), new Yield(VillageResource.PROVISIONS, 1.0, 0.0, component("butcher")));
		map.put(VillagerProfession.MASON.identifier(), new Yield(VillageResource.STONE, 3.0, 0.0, component("mason")));
		map.put(VillagerProfession.ARMORER.identifier(), new Yield(VillageResource.METAL, 1.0, 0.0, component("armorer")));
		map.put(VillagerProfession.TOOLSMITH.identifier(), new Yield(VillageResource.METAL, 1.0, 0.0, component("toolsmith")));
		map.put(VillagerProfession.WEAPONSMITH.identifier(), new Yield(VillageResource.METAL, 1.0, 0.0, component("weaponsmith")));
		map.put(VillagerProfession.FLETCHER.identifier(), new Yield(VillageResource.WOOD, 3.0, 0.0, component("fletcher")));
		map.put(VillagerProfession.NITWIT.identifier(), new Yield(VillageResource.WOOD, 1.5, 0.0, Optional.empty()));
		map.put(VillagerProfession.NONE.identifier(), new Yield(VillageResource.WOOD, 1.5, 0.0, Optional.empty()));
		map.put(VillagerProfession.SHEPHERD.identifier(), new Yield(VillageResource.MONEY, 1.5, 0.0, component("shepherd")));
		map.put(VillagerProfession.LEATHERWORKER.identifier(), new Yield(VillageResource.MONEY, 2.0, 0.0, component("leatherworker")));
		map.put(VillagerProfession.LIBRARIAN.identifier(), new Yield(VillageResource.MONEY, 2.0, 0.0, component("librarian")));
		map.put(VillagerProfession.CLERIC.identifier(), new Yield(VillageResource.MONEY, 1.5, 0.0, Optional.empty()));
		map.put(VillagerProfession.CARTOGRAPHER.identifier(), new Yield(VillageResource.MONEY, 1.5, 0.0, Optional.empty()));
		return Map.copyOf(map);
	}

	/** The components tag of a profession, {@code eam:village_components/<profession>}. */
	public static TagKey<Item> componentsTag(String profession) {
		return TagKey.create(Registries.ITEM, io.github.hellenter.eam.Eam.id("village_components/" + profession));
	}

	private static Optional<TagKey<Item>> component(String profession) {
		return Optional.of(componentsTag(profession));
	}

	private static Map<TagKey<Block>, Double> defaultToolTiers() {
		return Map.of(
				BlockTags.INCORRECT_FOR_WOODEN_TOOL, 1.0,
				BlockTags.INCORRECT_FOR_GOLD_TOOL, 1.25,
				BlockTags.INCORRECT_FOR_STONE_TOOL, 1.5,
				BlockTags.INCORRECT_FOR_COPPER_TOOL, 1.5,
				BlockTags.INCORRECT_FOR_IRON_TOOL, 2.0,
				BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3.0,
				BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4.0);
	}

	private static Map<String, Order> defaultOrders() {
		return Map.of(
				IRON_GOLEM, new Order(Map.of(VillageResource.METAL, 16.0, VillageResource.MONEY, 6.0, VillageResource.PROVISIONS, 4.0), Map.of()),
				"house_small", new Order(Map.of(VillageResource.WOOD, 20.0, VillageResource.STONE, 10.0, VillageResource.MONEY, 4.0),
						houses("small_house", Map.of("plains", 8, "desert", 8, "savanna", 8, "snowy", 8, "taiga", 5))),
				"house_medium", new Order(Map.of(VillageResource.WOOD, 40.0, VillageResource.STONE, 20.0, VillageResource.METAL, 2.0,
						VillageResource.MONEY, 8.0), houses("medium_house", Map.of("plains", 2, "desert", 2, "savanna", 2, "snowy", 3, "taiga", 4))),
				"house_big", new Order(Map.of(VillageResource.WOOD, 64.0, VillageResource.STONE, 36.0, VillageResource.METAL, 4.0,
						VillageResource.MONEY, 16.0), houses("big_house", Map.of("plains", 1))),
				"wall", new Order(Map.of(VillageResource.WOOD, 16.0, VillageResource.STONE, 6.0), defense("wall")),
				"gate", new Order(Map.of(VillageResource.WOOD, 18.0, VillageResource.STONE, 6.0, VillageResource.METAL, 1.0), defense("gate")),
				"tower", new Order(Map.of(VillageResource.STONE, 48.0, VillageResource.WOOD, 12.0, VillageResource.METAL, 2.0,
						VillageResource.MONEY, 4.0), defense("tower")));
	}

	/** Defences in the pillager outpost's style, the same in every village: {@code eam:village/<kind>}. */
	private static Map<String, List<Identifier>> defense(String kind) {
		return Map.of("default", List.of(Eam.id("village/" + kind)));
	}

	/**
	 * Vanilla village houses of one kind, {@code village/<style>/houses/<style>_<kind>_<n>} for each style's count; the
	 * snowy style is the {@code snow} villager type, and plains houses stand for every type without its own.
	 */
	private static Map<String, List<Identifier>> houses(String kind, Map<String, Integer> counts) {
		Map<String, List<Identifier>> map = new LinkedHashMap<>();
		counts.forEach((style, count) -> {
			List<Identifier> templates = new java.util.ArrayList<>();
			for (int i = 1; i <= count; i++) {
				templates.add(Identifier.withDefaultNamespace("village/" + style + "/houses/" + style + "_" + kind + "_" + i));
			}
			map.put(style.equals("snowy") ? "snow" : style, List.copyOf(templates));
		});
		map.put("default", map.get("plains"));
		return Map.copyOf(map);
	}
}
