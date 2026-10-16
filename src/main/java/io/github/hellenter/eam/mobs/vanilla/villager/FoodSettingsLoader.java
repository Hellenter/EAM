package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/food.json} on every datapack (re)load. */
final class FoodSettingsLoader extends SimpleJsonResourceReloadListener<FoodSettings> {
	static final Identifier ID = Eam.id("villager_food");
	static final Identifier SETTINGS = Eam.id("food");

	FoodSettingsLoader() {
		super(FoodSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, FoodSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerFoodModule.setSettings(files.getOrDefault(SETTINGS, FoodSettings.DEFAULT));
	}
}
