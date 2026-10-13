package io.github.hellenter.eam.world.structures.mine;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Abandoned mines in the upper underworld, after vanilla mineshafts: a jigsaw structure built from the templates under
 * {@code eam:underworld_mine/}, which Maxim edits in his own world. It is pure data (datagen JSON and the templates), so
 * there is nothing to register at runtime. Brown wool in a template's floor stays the natural rock, or turns into a poplar
 * plank where the tunnel crosses a cave or a lake.
 */
public final class UnderworldMine {
	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("underworld_mine"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("underworld_mines"));
	public static final TagKey<Biome> HAS_MINE = TagKey.create(Registries.BIOME, Eam.id("has_structure/underworld_mine"));

	/** The hall the mine starts from. */
	public static final ResourceKey<StructureTemplatePool> START_POOL = pool("start");
	/** Tunnels, crossings, stairs and rooms; every jigsaw in the templates draws from it. */
	public static final ResourceKey<StructureTemplatePool> TUNNEL_POOL = pool("tunnels");
	/** Cave-ins that close a tunnel when nothing else fits or the mine is deep enough. */
	public static final ResourceKey<StructureTemplatePool> END_POOL = pool("ends");
	public static final ResourceKey<StructureProcessorList> PROCESSORS =
			ResourceKey.create(Registries.PROCESSOR_LIST, Eam.id("underworld_mine"));

	public static final ResourceKey<LootTable> CHEST_LOOT = loot("chest");
	public static final ResourceKey<LootTable> BARREL_LOOT = loot("barrel");

	private UnderworldMine() {
	}

	private static ResourceKey<StructureTemplatePool> pool(String name) {
		return ResourceKey.create(Registries.TEMPLATE_POOL, Eam.id("underworld_mine/" + name));
	}

	private static ResourceKey<LootTable> loot(String name) {
		return ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/underworld_mine/" + name));
	}
}
