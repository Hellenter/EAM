package io.github.hellenter.eam;

import io.github.hellenter.eam.content.BowlModule;
import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.bucket.BucketModule;
import io.github.hellenter.eam.content.heart.HeartInJarModule;
import io.github.hellenter.eam.content.talisman.TalismanModule;
import io.github.hellenter.eam.content.campfire.CampFireModule;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.content.sieve.SieveModule;
import io.github.hellenter.eam.content.arrows.ArrowHeadModule;
import io.github.hellenter.eam.content.arrows.ArrowRecoveryModule;
import io.github.hellenter.eam.content.silver.SilverModule;
import io.github.hellenter.eam.core.config.EamConfig;
import io.github.hellenter.eam.dimension.UnderworldModule;
import io.github.hellenter.eam.mobs.entity.bat.VampireBatModule;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingModule;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightModule;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryModule;
import io.github.hellenter.eam.mobs.entity.lost.LostModule;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlockModule;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerModule;
import io.github.hellenter.eam.mobs.entity.spider.SpiderModule;
import io.github.hellenter.eam.mobs.entity.wight.WightModule;
import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;
import io.github.hellenter.eam.mobs.vanilla.cows.CowDefenseModule;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;
import io.github.hellenter.eam.mobs.vanilla.running.RunningModule;
import io.github.hellenter.eam.survival.bleeding.BleedingModule;
import io.github.hellenter.eam.survival.hunger.HungerModule;
import io.github.hellenter.eam.survival.progression.ProgressionModule;
import io.github.hellenter.eam.survival.sleep.SafeRoomModule;
import io.github.hellenter.eam.survival.thirst.ThirstModule;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;
import io.github.hellenter.eam.world.crafting.CraftingModule;
import io.github.hellenter.eam.world.enchanting.EnchantingModule;
import io.github.hellenter.eam.world.farming.FarmingModule;
import io.github.hellenter.eam.world.forging.AnvilRepair;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.items.StackSizeModule;
import io.github.hellenter.eam.world.mining.HandBreakingModule;
import io.github.hellenter.eam.world.mithril.MithrilModule;
import io.github.hellenter.eam.world.ores.OreModule;
import io.github.hellenter.eam.world.smelting.BlastFurnaceModule;
import io.github.hellenter.eam.world.structures.StructureRemovalModule;
import io.github.hellenter.eam.world.structures.VillageModule;
import io.github.hellenter.eam.world.structures.crypt.CryptModule;
import io.github.hellenter.eam.world.swamp.SwampyForestModule;
import io.github.hellenter.eam.world.tools.DiamondGearModule;
import io.github.hellenter.eam.world.tools.EarlyToolsModule;
import io.github.hellenter.eam.world.tools.GearModule;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Eam implements ModInitializer {
	public static final String MOD_ID = "eam";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static EamConfig config = EamConfig.DEFAULT;

	@Override
	public void onInitialize() {
		config = EamConfig.load();
		LOGGER.info("EAM loaded, modules: {}", config);

		// Registries are filled whatever the config says, so client and server always match.
		HungerModule.registerTypes();
		ThirstModule.register();
		EamBlocks.register();
		EamBlockEntities.register();
		EamItems.register();
		BowlModule.register();
		EamSounds.register();
		EamEntities.register();
		EamParticles.register();
		FallenLordModule.register();
		SkeletonKnightModule.register();
		PillagerKingModule.register();
		SwampLurkerModule.register();
		SwampyForestModule.register();
		WightModule.register();
		LostModule.register();
		UndeadWarlockModule.register();
		LostCavalryModule.register();
		SpiderModule.register();
		VampireBatModule.register();
		BleedingModule.register();
		EamWorldgen.init();
		CampFireModule.register();
		SieveModule.register();
		BlockBreakingModule.register();
		RunningModule.register();
		CowDefenseModule.register();
		EarlyToolsModule.register();
		DiamondGearModule.register();
		CraftingModule.register();
		VillageModule.register();
		CryptModule.register();
		ForgingModule.register();
		SilverModule.register();
		ArrowHeadModule.register();
		ArrowRecoveryModule.register();
		BlastFurnaceModule.register();
		BrewingModule.register();
		FarmingModule.register();
		EnchantingModule.register();
		SafeRoomModule.register();
		BloodMoonModule.register();
		BucketModule.register();
		TalismanModule.register();
		HeartInJarModule.register();
		UnderworldModule.register();

		// Each mechanic turns itself on here, guarded by its config flag.
		if (config.progression()) ProgressionModule.init();
		if (config.hunger()) HungerModule.init();
		if (config.thirst()) ThirstModule.init();
		if (config.stackSizes()) StackSizeModule.init();
		if (config.mining()) HandBreakingModule.init();
		if (config.mobs() && config.mobBlockBreaking()) BlockBreakingModule.init();
		if (config.mobs()) ThroughLeavesModule.init();
		if (config.mobs()) SwampLurkerModule.init();
		if (config.mobs()) WightModule.init();
		if (config.mobs()) LostModule.init();
		if (config.mobs()) SpiderModule.init();
		if (config.mobs()) PillagerKingModule.init();
		if (config.mobs()) VampireBatModule.init();
		if (config.mobs() && config.mobDrops()) MobDropsModule.init();
		if (config.mobs() && config.mobRunning()) RunningModule.init();
		if (config.mobs() && config.defensiveCows()) CowDefenseModule.init();
		if (config.removeEarlyTools()) EarlyToolsModule.init();
		if (config.ores()) OreModule.init();
		if (config.crafting()) CraftingModule.init();
		if (config.gear()) GearModule.init();
		if (config.removeDiamondGear()) DiamondGearModule.init();
		if (config.lockedVillages()) VillageModule.init();
		if (config.removeStructures()) StructureRemovalModule.init();
		if (config.ironPlates()) ForgingModule.init();
		if (config.anvilTiers()) ForgingModule.initAnvilTiers();
		if (config.anvilRepair()) AnvilRepair.init();
		if (config.silver()) SilverModule.init();
		if (config.arrowHeads()) ArrowHeadModule.init();
		if (config.arrowRecovery()) ArrowRecoveryModule.init();
		if (config.silver()) BrewingModule.init();
		if (config.slowCrops()) FarmingModule.initCrops();
		if (config.slowAnimals()) FarmingModule.initAnimals();
		if (config.mithril()) MithrilModule.init();
		if (config.enchanting()) EnchantingModule.init();
		if (config.safeSleep()) SafeRoomModule.init();
		if (config.bloodMoon()) BloodMoonModule.init();
		if (config.talismanPortal()) TalismanModule.init();
		if (config.swampyForest()) SwampyForestModule.init();
		if (config.underworld()) UnderworldModule.init();
	}

	public static EamConfig config() {
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
