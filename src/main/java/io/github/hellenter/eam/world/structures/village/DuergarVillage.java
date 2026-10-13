package io.github.hellenter.eam.world.structures.village;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Duergar clan villages in the upper underworld: a jigsaw structure per style (stone halls in the stone wastes, mushroom
 * huts in the mushroom forest, mud brick houses on stilts by the lakes), built from the templates under
 * {@code eam:duergar_village/<style>/}. Pure data like the mines: a plaza with the elder, streets, crossings and houses
 * for a smith and miners, each with its duergar placed in the template.
 */
public final class DuergarVillage {
	/** The three styles, each in its own biome. */
	public enum Style {
		STONE("stone"),
		MUSHROOM("mushroom"),
		LAKE("lake");

		private final String name;

		Style(String name) {
			this.name = name;
		}

		public String id() {
			return name;
		}

		public ResourceKey<Structure> structure() {
			return ResourceKey.create(Registries.STRUCTURE, Eam.id("duergar_village_" + name));
		}

		public TagKey<Biome> biomes() {
			return TagKey.create(Registries.BIOME, Eam.id("has_structure/duergar_village_" + name));
		}

		public ResourceKey<StructureTemplatePool> pool(String kind) {
			return ResourceKey.create(Registries.TEMPLATE_POOL, Eam.id("duergar_village/" + name + "/" + kind));
		}
	}

	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("duergar_villages"));

	/** Every village; renegades never spawn inside one. */
	public static final TagKey<Structure> VILLAGES = TagKey.create(Registries.STRUCTURE, Eam.id("duergar_villages"));
	/** What the elder's lair map looks for, nearest first: the bosses' lairs on the surface. */
	public static final TagKey<Structure> LAIR_MAPS = TagKey.create(Registries.STRUCTURE, Eam.id("on_duergar_lair_maps"));

	public static final ResourceKey<LootTable> CHEST_LOOT = loot("chest");
	public static final ResourceKey<LootTable> BARREL_LOOT = loot("barrel");
	public static final ResourceKey<LootTable> SMITH_LOOT = loot("smith");

	private DuergarVillage() {
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/duergar_village/" + name));
	}
}
