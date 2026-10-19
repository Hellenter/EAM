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
 * @param melee       how evolved skeletons switch between bow and blade
 * @param web         how evolved spiders shoot webs
 */
public record EvolutionSettings(float pieceChance, List<Phase> phases, Map<String, Tier> tiers, Melee melee, Web web) {
	public static final EvolutionSettings DEFAULT = new EvolutionSettings(0.6F, List.of(), Map.of(), Melee.DEFAULT, Web.DEFAULT);

	/**
	 * One phase of evolution.
	 *
	 * @param day                day count (0 = the first day) the phase begins on
	 * @param armor              the armor roll: one tier at most, each with its own chance
	 * @param weapons            the weapon roll, separate from the armor roll
	 * @param enchantedBowChance chance a mob holding a plain bow gets it enchanted
	 * @param meleeChance        chance a skeleton learns to put its bow away and fight with a blade up close
	 * @param webChance          chance a spider learns to shoot webs
	 */
	public record Phase(int day, List<Roll> armor, List<Roll> weapons, float enchantedBowChance, float meleeChance, float webChance) {
		static final Codec<Phase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.intRange(0, Integer.MAX_VALUE).fieldOf("day").forGetter(Phase::day),
				Roll.CODEC.listOf().optionalFieldOf("armor", List.of()).forGetter(Phase::armor),
				Roll.CODEC.listOf().optionalFieldOf("weapons", List.of()).forGetter(Phase::weapons),
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("enchanted_bow_chance", 0.0F).forGetter(Phase::enchantedBowChance),
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("skeleton_melee_chance", 0.0F).forGetter(Phase::meleeChance),
				Codec.floatRange(0.0F, 1.0F).optionalFieldOf("spider_web_chance", 0.0F).forGetter(Phase::webChance)
		).apply(instance, Phase::new));
	}

	/**
	 * Evolved skeletons' blade.
	 *
	 * @param drawRange    the skeleton swaps its bow for the blade once its target is this close
	 * @param sheatheRange it takes the bow back once the target is farther than this
	 */
	public record Melee(float drawRange, float sheatheRange) {
		static final Melee DEFAULT = new Melee(4.0F, 7.0F);

		static final Codec<Melee> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 64.0F).optionalFieldOf("draw_range", DEFAULT.drawRange).forGetter(Melee::drawRange),
				Codec.floatRange(0.0F, 64.0F).optionalFieldOf("sheathe_range", DEFAULT.sheatheRange).forGetter(Melee::sheatheRange)
		).apply(instance, Melee::new));
	}

	/**
	 * Evolved spiders' web shot.
	 *
	 * @param minRange      the spider shoots only at targets at least this far
	 * @param maxRange      and no farther than this
	 * @param cooldownTicks ticks between two shots
	 * @param speed         flight speed of the web
	 * @param seconds       how long a web lasts before it falls apart
	 */
	public record Web(float minRange, float maxRange, int cooldownTicks, float speed, int seconds) {
		static final Web DEFAULT = new Web(3.0F, 12.0F, 200, 1.0F, 5);

		static final Codec<Web> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 64.0F).optionalFieldOf("min_range", DEFAULT.minRange).forGetter(Web::minRange),
				Codec.floatRange(0.0F, 64.0F).optionalFieldOf("max_range", DEFAULT.maxRange).forGetter(Web::maxRange),
				Codec.intRange(1, 72000).optionalFieldOf("cooldown_ticks", DEFAULT.cooldownTicks).forGetter(Web::cooldownTicks),
				Codec.floatRange(0.1F, 4.0F).optionalFieldOf("speed", DEFAULT.speed).forGetter(Web::speed),
				Codec.intRange(1, 3600).optionalFieldOf("seconds", DEFAULT.seconds).forGetter(Web::seconds)
		).apply(instance, Web::new));
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
			Codec.unboundedMap(Codec.STRING, Tier.CODEC).optionalFieldOf("tiers", DEFAULT.tiers).forGetter(EvolutionSettings::tiers),
			Melee.CODEC.optionalFieldOf("skeleton_melee", DEFAULT.melee).forGetter(EvolutionSettings::melee),
			Web.CODEC.optionalFieldOf("spider_web", DEFAULT.web).forGetter(EvolutionSettings::web)
	).apply(instance, EvolutionSettings::new));
}
