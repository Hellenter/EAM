package io.github.hellenter.eam.core.link;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Item components as they were before EAM changed them (stack sizes, tools, food). A client on a vanilla server
 * reads these instead, so its inventory and break times match the server.
 */
public final class VanillaComponents {
	private static final Identifier BEFORE_EAM = Eam.id("vanilla_components");
	private static volatile Map<Item, DataComponentMap> snapshot = Map.of();

	private VanillaComponents() {
	}

	/** Must be registered before any module edits components: it runs in a phase before the default one. */
	static void register() {
		DefaultItemComponentEvents.MODIFY.addPhaseOrdering(BEFORE_EAM, Event.DEFAULT_PHASE);
		DefaultItemComponentEvents.MODIFY.register(BEFORE_EAM, context -> {
			Map<Item, DataComponentMap> taken = new IdentityHashMap<>();
			context.modify(item -> true, (builder, lookup, item) -> taken.put(item, builder.build()));
			snapshot = taken;
		});
	}

	/** The pre-EAM components of an item, or {@code null} when it has none recorded (EAM's own items). */
	public static DataComponentMap of(Item item) {
		return snapshot.get(item);
	}
}
