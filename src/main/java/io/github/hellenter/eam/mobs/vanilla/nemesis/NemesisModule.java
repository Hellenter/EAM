package io.github.hellenter.eam.mobs.vanilla.nemesis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * A monster that kills a player becomes their nemesis: it takes the best piece of gear the player had, wears or holds it
 * and drops it for sure when it dies, gets a name ("Zombie, slayer of Maxim") and extra health for every kill, and never
 * despawns. Numbers live in {@code data/eam/mobs/nemesis.json}; bosses and anything in {@link #EXCLUDED} stay as they are.
 */
public final class NemesisModule {
	/** Mobs that never become a nemesis: bosses already have a name and a fight of their own. */
	public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("not_nemesis"));

	private static final Identifier HEALTH = Eam.id("nemesis_health");

	/** Who a nemesis killed last and how many players it has killed. */
	public record Nemesis(String victim, int kills) {
		static final Codec<Nemesis> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("victim").forGetter(Nemesis::victim),
				Codec.INT.fieldOf("kills").forGetter(Nemesis::kills)
		).apply(instance, Nemesis::new));
	}

	public static final AttachmentType<Nemesis> NEMESIS = AttachmentRegistry.create(Eam.id("nemesis"),
			builder -> builder.persistent(Nemesis.CODEC));

	private static volatile NemesisSettings settings = NemesisSettings.DEFAULT;
	private static boolean enabled;

	private NemesisModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(NemesisSettingsLoader.ID, new NemesisSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(NemesisSettings value) {
		settings = value;
	}

	/**
	 * Called when anything dead is about to drop its loot. For a player, before anything leaves the inventory, the monster
	 * that dealt the killing blow, directly or with a projectile, takes the best piece of gear and becomes a nemesis.
	 */
	public static void onDeathLoot(LivingEntity dead, ServerLevel level, DamageSource source) {
		if (!enabled || !(dead instanceof ServerPlayer victim) || victim.isSpectator()) {
			return;
		}
		if (!(source.getEntity() instanceof Mob killer) || !canBecomeNemesis(killer)) {
			return;
		}
		NemesisSettings settings = NemesisModule.settings;
		ItemStack taken = settings.takeItem() && !level.getGameRules().get(GameRules.KEEP_INVENTORY) ? takeBest(victim.getInventory()) : ItemStack.EMPTY;
		if (!taken.isEmpty()) {
			equip(killer, level, taken);
			victim.sendSystemMessage(Component.translatable("eam.nemesis.took", killer.getType().getDescription(), taken.getStyledHoverName()));
		}
		Nemesis previous = killer.getAttached(NEMESIS);
		Nemesis nemesis = new Nemesis(victim.getGameProfile().name(), previous == null ? 1 : previous.kills() + 1);
		killer.setAttached(NEMESIS, nemesis);
		if (previous != null || !killer.hasCustomName()) {
			killer.setCustomName(Component.translatable("eam.nemesis.name", killer.getType().getDescription(), nemesis.victim()));
		}
		AttributeInstance health = killer.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			double bonus = NemesisLogic.healthBonus(nemesis.kills(), settings.healthPerKill(), settings.maxKills());
			health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			killer.setHealth(killer.getMaxHealth());
		}
		killer.setPersistenceRequired();
	}

	/** A nemesis that dies leaving no loot (burned by the sun) still drops what it took from its victims. */
	public static void dropTrophies(LivingEntity dead) {
		if (dead instanceof Mob mob && mob.hasAttached(NEMESIS) && mob.level() instanceof ServerLevel level) {
			mob.dropPreservedEquipment(level);
		}
	}

	private static boolean canBecomeNemesis(Mob killer) {
		return killer.isAlive() && !killer.is(EXCLUDED) && !(killer instanceof OwnableEntity pet && pet.getOwner() instanceof Player);
	}

	/** Removes the most valuable piece of gear from the inventory, or nothing if it holds no gear. */
	private static ItemStack takeBest(Inventory inventory) {
		int best = -1;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || !stack.isDamageableItem() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				continue;
			}
			double score = score(stack);
			if (score > bestScore) {
				best = i;
				bestScore = score;
			}
		}
		return best < 0 ? ItemStack.EMPTY : inventory.removeItemNoUpdate(best);
	}

	private static double score(ItemStack stack) {
		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		int levels = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
			levels += entry.getIntValue();
		}
		return NemesisLogic.score(sum(modifiers, Attributes.ATTACK_DAMAGE), sum(modifiers, Attributes.ARMOR),
				sum(modifiers, Attributes.ARMOR_TOUGHNESS), levels, stack.getMaxDamage());
	}

	private static double sum(ItemAttributeModifiers modifiers, Holder<Attribute> attribute) {
		double total = 0.0;
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().equals(attribute) && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}

	/** Puts the trophy where it belongs (armor on, anything else in hand); whatever was there falls to the ground. */
	private static void equip(Mob killer, ServerLevel level, ItemStack trophy) {
		EquipmentSlot slot = killer.getEquipmentSlotForItem(trophy);
		ItemStack old = killer.getItemBySlot(slot);
		if (!old.isEmpty()) {
			killer.spawnAtLocation(level, old);
		}
		killer.setItemSlot(slot, trophy);
		killer.setGuaranteedDrop(slot);
	}
}
