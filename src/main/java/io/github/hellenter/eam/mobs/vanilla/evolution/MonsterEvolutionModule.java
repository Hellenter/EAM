package io.github.hellenter.eam.mobs.vanilla.evolution;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;

import java.util.ArrayList;
import java.util.List;

/**
 * Monsters grow stronger as the world ages. Each phase in {@code data/eam/mobs/evolution.json} begins on a day count and
 * gives freshly spawned monsters in {@link #ARMORED} a roll for armor and those in {@link #ARMED} a separate roll for a
 * weapon, each roll landing on one tier of gear at most; later phases make skeletons' bows more often enchanted. Only empty
 * slots are filled. Everything handed out is drawn worn ({@link WornLooks}) but drops as the plain item.
 */
public final class MonsterEvolutionModule {
	/** Monsters that put on armor as the world ages. */
	public static final TagKey<EntityType<?>> ARMORED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("evolution_armored"));
	/** Monsters that take up weapons as the world ages. */
	public static final TagKey<EntityType<?>> ARMED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("evolution_armed"));

	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	private static volatile EvolutionSettings settings = EvolutionSettings.DEFAULT;
	private static boolean enabled;

	private MonsterEvolutionModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EvolutionSettingsLoader.ID, new EvolutionSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(EvolutionSettings value) {
		settings = value;
	}

	/**
	 * Called for every entity freshly added to a level. Monsters get their gear; dropped items lose the worn look, even
	 * with the module off, so no worn item ever reaches a player.
	 */
	public static void onFreshEntity(Entity entity) {
		if (entity instanceof ItemEntity item) {
			if (WornLooks.isWorn(item.getItem())) {
				ItemStack mended = item.getItem().copy();
				WornLooks.mend(mended);
				item.setItem(mended);
			}
			return;
		}
		if (enabled && entity instanceof Mob mob && mob.level() instanceof ServerLevel level && (mob.is(ARMORED) || mob.is(ARMED))) {
			evolve(mob, level);
		}
	}

	private static void evolve(Mob mob, ServerLevel level) {
		EvolutionSettings settings = MonsterEvolutionModule.settings;
		long days = EvolutionLogic.daysPassed(level.getServer().overworld().getOverworldClockTime());
		int index = EvolutionLogic.phase(settings.phases().stream().map(EvolutionSettings.Phase::day).toList(), days);
		if (index < 0) {
			return;
		}
		EvolutionSettings.Phase phase = settings.phases().get(index);
		RandomSource random = mob.getRandom();
		if (mob.is(ARMORED)) {
			EvolutionSettings.Tier tier = roll(phase.armor(), settings, random);
			if (tier != null) {
				putOnArmor(mob, tier, settings.pieceChance(), random);
			}
		}
		if (mob.is(ARMED) && mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
			EvolutionSettings.Tier tier = roll(phase.weapons(), settings, random);
			if (tier != null && !tier.weapons().isEmpty()) {
				mob.setItemSlot(EquipmentSlot.MAINHAND, worn(tier.weapons(), random));
			}
		}
		ItemStack hand = mob.getItemBySlot(EquipmentSlot.MAINHAND);
		if (hand.is(Items.BOW) && !hand.isEnchanted() && random.nextFloat() < phase.enchantedBowChance()) {
			ItemStack bow = hand.copy();
			EnchantmentHelper.enchantItemFromProvider(bow, level.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT,
					level.getCurrentDifficultyAt(mob.blockPosition()), random);
			mob.setItemSlot(EquipmentSlot.MAINHAND, bow);
		}
	}

	/** The tier one roll over {@code rolls} lands on, or null. */
	private static EvolutionSettings.Tier roll(List<EvolutionSettings.Roll> rolls, EvolutionSettings settings, RandomSource random) {
		int picked = EvolutionLogic.pick(rolls.stream().map(EvolutionSettings.Roll::chance).toList(), random.nextFloat());
		return picked < 0 ? null : settings.tiers().get(rolls.get(picked).tier());
	}

	/** Each empty slot the tier has items for, with {@code pieceChance} each; at least one of them. */
	private static void putOnArmor(Mob mob, EvolutionSettings.Tier tier, float pieceChance, RandomSource random) {
		List<EquipmentSlot> open = new ArrayList<>();
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (mob.getItemBySlot(slot).isEmpty() && !items(tier, slot).isEmpty()) {
				open.add(slot);
			}
		}
		if (open.isEmpty()) {
			return;
		}
		List<EquipmentSlot> chosen = new ArrayList<>(open.stream().filter(slot -> random.nextFloat() < pieceChance).toList());
		if (chosen.isEmpty()) {
			chosen.add(open.get(random.nextInt(open.size())));
		}
		for (EquipmentSlot slot : chosen) {
			mob.setItemSlot(slot, worn(items(tier, slot), random));
		}
	}

	private static List<Item> items(EvolutionSettings.Tier tier, EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> tier.head();
			case CHEST -> tier.chest();
			case LEGS -> tier.legs();
			case FEET -> tier.feet();
			default -> tier.weapons();
		};
	}

	private static ItemStack worn(List<Item> items, RandomSource random) {
		return WornLooks.wear(new ItemStack(items.get(random.nextInt(items.size()))));
	}
}
