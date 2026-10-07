package io.github.hellenter.eam.survival.hunger;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.Optional;

/** Reads {@link HungerRules} from data packs on every data reload. */
final class HungerRulesLoader extends SimpleReloadListener<HungerRules> {
	static final Identifier ID = Eam.id("hunger_rules");
	private static final Identifier FILE = Eam.id("survival/hunger.json");

	@Override
	protected HungerRules prepare(PreparableReloadListener.SharedState state) {
		Optional<Resource> resource = state.resourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			return HungerRules.DEFAULT;
		}

		try (Reader reader = resource.get().openAsReader()) {
			return HungerRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
					.resultOrPartial(error -> Eam.LOGGER.error("Invalid {}: {}", FILE, error))
					.orElse(HungerRules.DEFAULT);
		} catch (Exception e) {
			Eam.LOGGER.error("Failed to read {}, using default hunger rules", FILE, e);
			return HungerRules.DEFAULT;
		}
	}

	@Override
	protected void apply(HungerRules rules, PreparableReloadListener.SharedState state) {
		HungerModule.setRules(rules);
	}
}
