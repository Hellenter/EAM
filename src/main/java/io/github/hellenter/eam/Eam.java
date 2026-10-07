package io.github.hellenter.eam;

import io.github.hellenter.eam.core.config.EamConfig;
import io.github.hellenter.eam.survival.hunger.HungerModule;
import io.github.hellenter.eam.world.items.StackSizeModule;

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

		// Each mechanic registers itself here, guarded by its config flag.
		HungerModule.registerTypes();
		if (config.hunger()) HungerModule.init();
		if (config.stackSizes()) StackSizeModule.init();
	}

	public static EamConfig config() {
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
