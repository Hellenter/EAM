package io.github.hellenter.eam.world.tools;

import io.github.hellenter.eam.content.mithril.MithrilGear;
import io.github.hellenter.eam.content.rusty.RustyGear;
import io.github.hellenter.eam.content.silver.SilverGear;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Weaker gear: copper and iron tools wear out sooner, player armor gives less protection and copper armor wears out
 * twice as fast (silver copies copper). Edits the {@code max_damage} and {@code attribute_modifiers} item components; numbers are in
 * {@link GearLogic}.
 */
public final class GearModule {
	private static final List<ArmorMaterial> MATERIALS = List.of(
			ArmorMaterials.LEATHER,
			ArmorMaterials.COPPER,
			ArmorMaterials.CHAINMAIL,
			ArmorMaterials.IRON,
			ArmorMaterials.GOLD,
			ArmorMaterials.DIAMOND,
			ArmorMaterials.TURTLE_SCUTE,
			ArmorMaterials.NETHERITE,
			SilverGear.ARMOR,
			MithrilGear.ARMOR,
			RustyGear.IRON_ARMOR,
			RustyGear.CHAINMAIL_ARMOR
	);

	/** Set order used by {@link GearLogic#scaleSet}. */
	private static final ArmorType[] SET = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};

	private GearModule() {
	}

	public static void init() {
		Map<ResourceKey<EquipmentAsset>, Map<ArmorType, Integer>> armor = scaledArmor();

		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item -> true, (builder, item) -> {
			GearLogic.toolDurability(BuiltInRegistries.ITEM.getKey(item).getPath())
					.ifPresent(durability -> builder.set(DataComponents.MAX_DAMAGE, durability));

			Equippable equippable = builder.get(DataComponents.EQUIPPABLE);
			if (equippable == null || equippable.assetId().isEmpty()) {
				return;
			}
			ResourceKey<EquipmentAsset> asset = equippable.assetId().get();
			ArmorType type = armorType(equippable);
			Map<ArmorType, Integer> set = armor.get(asset);
			if (type == null || set == null) {
				return;
			}

			ItemAttributeModifiers modifiers = builder.get(DataComponents.ATTRIBUTE_MODIFIERS);
			if (modifiers != null) {
				builder.set(DataComponents.ATTRIBUTE_MODIFIERS, withArmor(modifiers, set.get(type)));
			}

			Integer maxDamage = builder.get(DataComponents.MAX_DAMAGE);
			if ((asset.equals(EquipmentAssets.COPPER) || asset.equals(SilverGear.ASSET)) && maxDamage != null) {
				builder.set(DataComponents.MAX_DAMAGE, GearLogic.copperArmorDurability(maxDamage));
			}
		}));
	}

	private static Map<ResourceKey<EquipmentAsset>, Map<ArmorType, Integer>> scaledArmor() {
		Map<ResourceKey<EquipmentAsset>, Map<ArmorType, Integer>> result = new HashMap<>();
		for (ArmorMaterial material : MATERIALS) {
			int[] vanilla = new int[SET.length];
			for (int i = 0; i < SET.length; i++) {
				vanilla[i] = material.defense().getOrDefault(SET[i], 0);
			}
			int[] scaled = GearLogic.scaleSet(vanilla, GearLogic.ARMOR_FACTOR);
			Map<ArmorType, Integer> set = new HashMap<>();
			for (int i = 0; i < SET.length; i++) {
				set.put(SET[i], scaled[i]);
			}
			result.put(material.assetId(), set);
		}
		return result;
	}

	/** Player armor slots only: horse and wolf armor keep their values. */
	private static ArmorType armorType(Equippable equippable) {
		for (ArmorType type : SET) {
			if (type.getSlot() == equippable.slot()) {
				return type;
			}
		}
		return null;
	}

	private static ItemAttributeModifiers withArmor(ItemAttributeModifiers modifiers, int armor) {
		return new ItemAttributeModifiers(modifiers.modifiers().stream()
				.map(entry -> entry.attribute().is(Attributes.ARMOR)
						? new ItemAttributeModifiers.Entry(entry.attribute(),
								new AttributeModifier(entry.modifier().id(), armor, entry.modifier().operation()),
								entry.slot(), entry.display())
						: entry)
				.toList());
	}
}
