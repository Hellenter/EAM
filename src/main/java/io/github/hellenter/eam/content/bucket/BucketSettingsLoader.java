package io.github.hellenter.eam.content.bucket;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/buckets/lava_melting.json} on every datapack (re)load. */
final class BucketSettingsLoader extends SimpleJsonResourceReloadListener<BucketSettings> {
	static final Identifier ID = Eam.id("bucket_settings");
	static final Identifier SETTINGS = Eam.id("lava_melting");

	BucketSettingsLoader() {
		super(BucketSettings.CODEC, FileToIdConverter.json("buckets"));
	}

	@Override
	protected void apply(Map<Identifier, BucketSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BucketModule.setSettings(files.getOrDefault(SETTINGS, new BucketSettings(Map.of())));
	}
}
