package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;
import java.util.Optional;

/**
 * The Lost wear ancient metal armor worn through: the pieces are real ancient metal armor (its protection, and what drops),
 * drawn with one of the {@code eam:worn_ancient_metal_N} looks. The leader's is only scratched and dented; followers'
 * breastplates have holes punched through (three looks), and every chestplate has torn chainmail sleeves. Their shields
 * are battered too.
 */
public final class LostGear {
	/** The leader's look, then the three holed breastplates of the followers. */
	public static final List<ResourceKey<EquipmentAsset>> WORN = List.of(asset("worn_ancient_metal_0"), asset("worn_ancient_metal_1"),
			asset("worn_ancient_metal_2"), asset("worn_ancient_metal_3"));

	/** The look of their shields: {@code assets/eam/items/lost_shield.json}. */
	public static final Identifier SHIELD_MODEL = Eam.id("lost_shield");

	private LostGear() {
	}

	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Eam.id(name));
	}

	/** Dresses and arms one of the Lost for his role. */
	static void equip(Mob mob, LostRole role, RandomSource random) {
		int look = role == LostRole.LEADER ? 0 : 1 + random.nextInt(WORN.size() - 1);
		boolean leader = role == LostRole.LEADER;
		boolean archer = role == LostRole.ARCHER;
		mob.setItemSlot(EquipmentSlot.CHEST, worn(EamItems.ANCIENT_METAL_CHESTPLATE, look));
		if (leader || random.nextFloat() < (archer ? 0.85F : 0.95F)) {
			mob.setItemSlot(EquipmentSlot.HEAD, worn(EamItems.ANCIENT_METAL_HELMET, look));
		}
		if (leader || random.nextFloat() < 0.5F) {
			mob.setItemSlot(EquipmentSlot.LEGS, worn(EamItems.ANCIENT_METAL_LEGGINGS, look));
		}
		// Every one of them keeps his boots.
		mob.setItemSlot(EquipmentSlot.FEET, worn(EamItems.ANCIENT_METAL_BOOTS, look));
		switch (role) {
			case LEADER -> arm(mob, EamItems.ANCIENT_METAL_SWORD, Items.SHIELD);
			case SWORDSMAN -> arm(mob, random.nextBoolean() ? EamItems.ANCIENT_METAL_SWORD : EamItems.RUSTY_IRON_SWORD, Items.SHIELD);
			case SPEARMAN -> arm(mob, Items.IRON_SPEAR, Items.SHIELD);
			case ARCHER -> arm(mob, Items.BOW, null);
		}
	}

	private static void arm(Mob mob, Item mainHand, Item offHand) {
		mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(mainHand));
		ItemStack shield = offHand == null ? ItemStack.EMPTY : new ItemStack(offHand);
		if (shield.is(Items.SHIELD)) {
			// A battered wooden shield, split and holed; it is still a plain shield.
			shield.set(DataComponents.ITEM_MODEL, SHIELD_MODEL);
		}
		mob.setItemSlot(EquipmentSlot.OFFHAND, shield);
	}

	/** An armor piece drawn with the worn look {@code look}; everything else about it stays as it is. */
	public static ItemStack worn(Item item, int look) {
		ItemStack stack = new ItemStack(item);
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null) {
			stack.set(DataComponents.EQUIPPABLE, new Equippable(equippable.slot(), equippable.equipSound(), Optional.of(WORN.get(look)),
					equippable.cameraOverlay(), equippable.allowedEntities(), equippable.dispensable(), equippable.swappable(),
					equippable.damageOnHurt(), equippable.equipOnInteract(), equippable.canBeSheared(), equippable.shearingSound()));
		}
		return stack;
	}
}
