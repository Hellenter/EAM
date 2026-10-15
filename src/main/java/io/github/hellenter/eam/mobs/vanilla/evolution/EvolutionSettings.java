package io.github.hellenter.eam.mobs.vanilla.evolution;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;

/**
 * Monster evolution numbers, read from {@code data/eam/mobs/evolution.json} so datapacks can override them.
 *
 * @param pieceChance chance of each armor slot to be filled once a mob's armor roll came up; one piece is always worn
 * @param phases      the phases in the order they begin
 * @param tiers       named sets of gear the phases roll for
 */
public record EvolutionSettings(float pieceChance, List<Phase> phases, Map<String, Tier> tiers) {
	public static final EvolutionSettings DEFAULT = new EvolutionSettings(0.6F, List.of(), Map.of());

	/**
	 * One phase of evolution.
	 *
	 * @param day                day count (0 = the first day) the phase begins on
	 * @param armor              the armor roll: one tier at most, each with its own chance
	 * @param weapons            the weapon roll, separate from the armor roll
	 * @param enchantedBowChance chance a mob holding a plain bow gets it enchanted
	 */
	public record Phase(int day, List<Roll> armor, List<Roll> weapons, float enchantedBowChance) {
		static final Codec<Phase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("day").forGetter(Phase::day),
				Roll.CODEC.listOf().optionalFieldOf("armor", List.of()).forGetter(Phase::armor),
				Roll.CODEC.listOf().optionalFieldOf("weapons", List.of()).forGetter(Phase::weapons),
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("enchanted_bow_chance", 0.0F).forGetter(Phase::enchantedBowChance)
		).apply(instance, Phase::new));
	}

	/** A tier of gear and the chance to get it. */
	public record Roll(String tier, float chance) {
		static final Codec<Roll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("tier").forGetter(Roll::tier),
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(Roll::chance)
		).apply(instance, Roll::new));
	}

	/** The items of one tier, by slot; a mob gets one of the listed items at random. */
	public record Tier(List<Item> head, List<Item> chest, List<Item> legs, List<Item> feet, List<Item> weapons) {
		private static final Codec<List<Item>> ITEMS = BuiltInRegistries.ITEM.byNameCodec().listOf();

		static final Codec<Tier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ITEMS.optionalFieldOf("head", List.of()).forGetter(Tier::head),
				ITEMS.optionalFieldOf("chest", List.of()).forGetter(Tier::chest),
				ITEMS.optionalFieldOf("legs", List.of()).forGetter(Tier::legs),
				ITEMS.optionalFieldOf("feet", List.of()).forGetter(Tier::feet),
				ITEMS.optionalFieldOf("weapons", List.of()).forGetter(Tier::weapons)
		).apply(instance, Tier::new));
	}

	public static final Codec<EvolutionSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("piece_chance", DEFAULT.pieceChance).forGetter(EvolutionSettings::pieceChance),
			Phase.CODEC.listOf().optionalFieldOf("phases", DEFAULT.phases).forGetter(EvolutionSettings::phases),
			Codec.unboundedMap(Codec.STRING, Tier.CODEC).optionalFieldOf("tiers", DEFAULT.tiers).forGetter(EvolutionSettings::tiers)
	).apply(instance, EvolutionSettings::new));
}
