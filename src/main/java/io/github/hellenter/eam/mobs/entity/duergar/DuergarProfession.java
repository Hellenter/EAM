package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.trading.TradeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * A duergar's trade, like a villager's profession: what he buys and sells at each of his levels (the trade sets
 * {@code eam:duergar/<profession>/level_<n>}, datapack data). Each belongs to one of the three village roles that the
 * house templates name; a duergar placed with a role takes one of its professions, so templates need no change.
 */
public enum DuergarProfession implements StringRepresentable {
	MINER("miner", DuergarVillager.Role.MINER, 3, 5),
	MASON("mason", DuergarVillager.Role.MINER, 2, 5),
	MUSHROOM_GROWER("mushroom_grower", DuergarVillager.Role.MINER, 2, 5),
	BREWER("brewer", DuergarVillager.Role.MINER, 2, 5),
	/** Sells enchanted books, and at master level the only Mending book anyone trades. */
	RUNECARVER("runecarver", DuergarVillager.Role.MINER, 2, 5),
	ARMORER("armorer", DuergarVillager.Role.SMITH, 1, 5),
	WEAPONSMITH("weaponsmith", DuergarVillager.Role.SMITH, 1, 5),
	TOOLSMITH("toolsmith", DuergarVillager.Role.SMITH, 1, 5),
	/** The clan's elder offers all his trades at once and never levels. */
	ELDER("elder", DuergarVillager.Role.ELDER, 1, 1);

	private final String name;
	private final DuergarVillager.Role role;
	private final int weight;
	private final int maxLevel;

	DuergarProfession(String name, DuergarVillager.Role role, int weight, int maxLevel) {
		this.name = name;
		this.role = role;
		this.weight = weight;
		this.maxLevel = maxLevel;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** The village role whose clothes he wears and whose houses he lives in. */
	public DuergarVillager.Role role() {
		return role;
	}

	public int maxLevel() {
		return maxLevel;
	}

	/** What he offers on reaching {@code level}. */
	public ResourceKey<TradeSet> trades(int level) {
		return ResourceKey.create(Registries.TRADE_SET, Eam.id("duergar/" + name + "/level_" + level));
	}

	/** One of the role's professions, by weight, for a roll in [0, 1). */
	public static DuergarProfession pick(DuergarVillager.Role role, double roll) {
		List<DuergarProfession> options = forRole(role);
		int total = options.stream().mapToInt(profession -> profession.weight).sum();
		int target = (int) (roll * total);
		for (DuergarProfession profession : options) {
			target -= profession.weight;
			if (target < 0) {
				return profession;
			}
		}
		return options.getLast();
	}

	public static List<DuergarProfession> forRole(DuergarVillager.Role role) {
		List<DuergarProfession> options = new ArrayList<>();
		for (DuergarProfession profession : values()) {
			if (profession.role == role) {
				options.add(profession);
			}
		}
		return options;
	}

	public static DuergarProfession byName(String name) {
		for (DuergarProfession profession : values()) {
			if (profession.name.equals(name)) {
				return profession;
			}
		}
		return MINER;
	}
}
