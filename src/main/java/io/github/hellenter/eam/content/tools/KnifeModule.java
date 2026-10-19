package io.github.hellenter.eam.content.tools;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Knives butcher better: an animal in {@link #BUTCHERED} that a player kills in melee with a knife ({@link #KNIVES})
 * drops more of its loot. Knives also take the sword enchantments through {@code #minecraft:enchantable/melee_weapon}.
 * Numbers are in {@code data/eam/tools/knife.json}.
 */
public final class KnifeModule {
	/** Every knife, whatever its tier. */
	public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, Eam.id("knives"));
	/** Animals whose drops a knife kill multiplies. */
	public static final TagKey<EntityType<?>> BUTCHERED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("butchered_by_knife"));

	private static volatile KnifeSettings settings = KnifeSettings.DEFAULT;

	private KnifeModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(KnifeSettingsLoader.ID, new KnifeSettingsLoader());
	}

	public static void init() {
		LootTableEvents.MODIFY_DROPS.register(KnifeModule::modifyDrops);
	}

	private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		DamageSource source = context.getOptional(LootContextParams.DAMAGE_SOURCE);
		if (entity == null || source == null || !entity.is(BUTCHERED)) return;
		// Only the animal's own death table, not tables nested in it.
		if (entity.getType().getDefaultLootTable().filter(table::is).isEmpty()) return;
		if (!(source.getEntity() instanceof Player) || source.getDirectEntity() != source.getEntity()) return;
		ItemStack weapon = source.getWeaponItem();
		if (weapon == null || !weapon.is(KNIVES)) return;

		double multiplier = settings.animalLootMultiplier();
		for (ItemStack drop : drops) {
			drop.setCount(KnifeLootLogic.bonusCount(drop.getCount(), multiplier, drop.getMaxStackSize(), context.getRandom().nextDouble()));
		}
	}

	public static KnifeSettings settings() {
		return settings;
	}

	static void setSettings(KnifeSettings value) {
		settings = value;
	}
}
