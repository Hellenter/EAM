package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.darkness.DarknessLogic;
import io.github.hellenter.eam.world.darkness.DarknessModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.timeline.Timeline;

import org.joml.Vector3f;

import java.util.concurrent.CompletableFuture;

/** Writes the {@code eam:darkness} timeline: darker nights by moon phase, near-black caves and a brightness cap. */
public class EamTimelineProvider extends FabricDynamicRegistryProvider {
	public EamTimelineProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Timeline> context) {
		float ambient = DarknessLogic.OVERWORLD_AMBIENT;
		// One period is a whole moon cycle, like minecraft:moon, so each night can have its own darkness.
		context.register(DarknessModule.TIMELINE, Timeline.builder(context.lookup(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD))
				.setPeriodTicks(DarknessLogic.DAY_TICKS * DarknessLogic.NIGHT_SKY_FACTOR.length)
				.addModifierTrack(EnvironmentAttributes.SKY_LIGHT_FACTOR, FloatModifier.MULTIPLY, track ->
						DarknessLogic.nightKeyframes(DarknessLogic.NIGHT_SKY_FACTOR).forEach(k -> track.addKeyframe(k.ticks(), k.value())))
				.addModifierTrack(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ColorModifier.MULTIPLY_RGB,
						track -> track.addKeyframe(0, new Vector3f(ambient, ambient, ambient)))
				.addTrack(DarknessModule.MAX_BRIGHTNESS, track -> track.addKeyframe(0, DarknessLogic.MAX_BRIGHTNESS))
				.build());
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.TIMELINE), DarknessModule.TIMELINE);
	}

	@Override
	public String getName() {
		return "EAM timelines";
	}
}
