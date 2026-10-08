package io.github.hellenter.eam.content.sieve;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.loot.LootTable;

/** The sieve: sifts gravel into flint shards, copper nuggets and flint. The block itself is registered in {@code EamBlocks}. */
public final class SieveModule {
	/** What one sifted gravel gives: one roll, which comes up empty half the time. */
	public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("gameplay/sieve"));

	private static volatile SieveSettings settings = SieveSettings.DEFAULT;

	private SieveModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SieveSettingsLoader.ID, new SieveSettingsLoader());
	}

	public static SieveSettings settings() {
		return settings;
	}

	static void setSettings(SieveSettings value) {
		settings = value;
	}
}
