package io.github.hellenter.eam.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public final class EamLanguageProviders {
	private EamLanguageProviders() {
	}

	public static class English extends FabricLanguageProvider {
		public English(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "en_us", registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add("death.attack.eam.dehydration", "%1$s died of thirst");
			builder.add("death.attack.eam.dehydration.player", "%1$s died of thirst while fighting %2$s");
		}
	}

	public static class Russian extends FabricLanguageProvider {
		public Russian(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, "ru_ru", registries);
		}

		@Override
		public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
			builder.add("death.attack.eam.dehydration", "%1$s умер от жажды");
			builder.add("death.attack.eam.dehydration.player", "%1$s умер от жажды, сражаясь с %2$s");
		}
	}
}
