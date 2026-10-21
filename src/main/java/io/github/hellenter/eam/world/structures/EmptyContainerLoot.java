package io.github.hellenter.eam.world.structures;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Gives every {@code block} container of a template that was saved empty and without a loot table the {@code loot_table}
 * one; containers with their own loot are left as they are.
 */
public record EmptyContainerLoot(Block block, ResourceKey<LootTable> lootTable) implements StructureProcessor {
	/** A few weak potions: what an empty barrel of Baba Yaga's hut gets. */
	public static final ResourceKey<LootTable> LIGHT_POTIONS = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/light_potions"));
	/** Light potions of the silver stand: what an empty barrel of a duergar house gets. */
	public static final ResourceKey<LootTable> DUERGAR_POTIONS = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("chests/duergar_potions"));

	public static final MapCodec<EmptyContainerLoot> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(EmptyContainerLoot::block),
			LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(EmptyContainerLoot::lootTable))
			.apply(i, EmptyContainerLoot::new));

	/** Registers the processor type, so processor lists in data can name it. */
	public static void register() {
		Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, Eam.id("empty_container_loot"), CODEC);
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot, BlockPos origin,
			StructureBlockInfo info, StructurePlaceSettings settings) {
		CompoundTag nbt = info.nbt();
		if (!info.state().is(block) || nbt == null || nbt.contains("LootTable") || !nbt.getListOrEmpty("Items").isEmpty()) {
			return info;
		}
		CompoundTag filled = nbt.copy();
		filled.putString("LootTable", lootTable.identifier().toString());
		filled.putLong("LootTableSeed", settings.getRandom(info.pos()).nextLong());
		return new StructureBlockInfo(info.pos(), info.state(), filled);
	}

	@Override
	public MapCodec<EmptyContainerLoot> codec() {
		return CODEC;
	}
}
