package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.brewing.SilverBrewingStandBlockEntity;
import io.github.hellenter.eam.content.campfire.CampFireBlockEntity;
import io.github.hellenter.eam.content.dryer.DryerBlockEntity;
import io.github.hellenter.eam.content.feeder.FeederBlockEntity;
import io.github.hellenter.eam.content.fridge.FridgeBlockEntity;
import io.github.hellenter.eam.content.furnace.ObsidianBlastFurnaceBlockEntity;
import io.github.hellenter.eam.content.sieve.SieveBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** Block entity types added by EAM. Registered after {@link EamBlocks}. */
public final class EamBlockEntities {
	public static final BlockEntityType<CampFireBlockEntity> CAMP_FIRE = register("camp_fire",
			new BlockEntityType<>(CampFireBlockEntity::new, Set.of(EamBlocks.CAMP_FIRE)));
	public static final BlockEntityType<DryerBlockEntity> FOOD_DRYER = register("food_dryer",
			new BlockEntityType<>(DryerBlockEntity::new, Set.of(EamBlocks.FOOD_DRYER)));

	public static final BlockEntityType<SieveBlockEntity> SIEVE = register("sieve",
			new BlockEntityType<>(SieveBlockEntity::new, Set.of(EamBlocks.SIEVE)));

	public static final BlockEntityType<SilverBrewingStandBlockEntity> SILVER_BREWING_STAND = register("silver_brewing_stand",
			new BlockEntityType<>(SilverBrewingStandBlockEntity::new, Set.of(EamBlocks.SILVER_BREWING_STAND)));

	public static final BlockEntityType<ObsidianBlastFurnaceBlockEntity> OBSIDIAN_BLAST_FURNACE = register("obsidian_blast_furnace",
			new BlockEntityType<>(ObsidianBlastFurnaceBlockEntity::new, Set.of(EamBlocks.OBSIDIAN_BLAST_FURNACE)));

	public static final BlockEntityType<FeederBlockEntity> FEEDER = register("feeder",
			new BlockEntityType<>(FeederBlockEntity::new, Set.of(EamBlocks.FEEDER, EamBlocks.METAL_FEEDER)));

	public static final BlockEntityType<FridgeBlockEntity> FRIDGE = register("fridge",
			new BlockEntityType<>(FridgeBlockEntity::new, Set.of(EamBlocks.SMALL_FRIDGE, EamBlocks.LARGE_FRIDGE)));

	private EamBlockEntities() {
	}

	public static void register() {
		// Loads the class, which registers the types above.
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Eam.id(name), type);
	}
}
