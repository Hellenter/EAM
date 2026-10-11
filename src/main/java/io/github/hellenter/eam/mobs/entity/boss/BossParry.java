package io.github.hellenter.eam.mobs.entity.boss;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Holds the shared {@link ParryPenalty}, read from {@code data/eam/mobs/boss_parry.json} on every datapack (re)load. */
public final class BossParry {
	private static final Identifier ID = Eam.id("boss_parry_settings");
	private static final Identifier SETTINGS = Eam.id("boss_parry");

	private static volatile ParryPenalty penalty = ParryPenalty.DEFAULT;
	private static boolean registered;

	private BossParry() {
	}

	/** Called by every boss module that can be parried; registers the loader once. */
	public static synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ID, new Loader());
	}

	public static ParryPenalty penalty() {
		return penalty;
	}

	private static final class Loader extends SimpleJsonResourceReloadListener<ParryPenalty> {
		Loader() {
			super(ParryPenalty.CODEC, FileToIdConverter.json("mobs"));
		}

		@Override
		protected void apply(Map<Identifier, ParryPenalty> files, ResourceManager manager, ProfilerFiller profiler) {
			penalty = files.getOrDefault(SETTINGS, ParryPenalty.DEFAULT);
		}
	}
}
