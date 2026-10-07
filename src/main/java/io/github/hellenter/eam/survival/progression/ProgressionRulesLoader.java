package io.github.hellenter.eam.survival.progression;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.Optional;

/** Reads {@link ProgressionRules} from data packs on every data reload. */
final class ProgressionRulesLoader extends SimpleReloadListener<ProgressionRules> {
	static final Identifier ID = Eam.id("progression_rules");
	private static final Identifier FILE = Eam.id("survival/progression.json");

	@Override
	protected ProgressionRules prepare(PreparableReloadListener.SharedState state) {
		Optional<Resource> resource = state.resourceManager().getResource(FILE);
		if (resource.isEmpty()) {
			return ProgressionRules.DEFAULT;
		}

		try (Reader reader = resource.get().openAsReader()) {
			return ProgressionRules.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
					.resultOrPartial(error -> Eam.LOGGER.error("Invalid {}: {}", FILE, error))
					.orElse(ProgressionRules.DEFAULT);
		} catch (Exception e) {
			Eam.LOGGER.error("Failed to read {}, using default progression rules", FILE, e);
			return ProgressionRules.DEFAULT;
		}
	}

	@Override
	protected void apply(ProgressionRules rules, PreparableReloadListener.SharedState state) {
		ProgressionModule.setRules(rules);
	}
}
