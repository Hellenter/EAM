package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamEntities;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Elite crossbowmen: pillagers in a cuirass and a round helmet who shoot a plain bolt, a burst of three, a red bolt
 * that goes through a shield and, now and then, a net that holds a player in place. Some pillagers of patrols and
 * outposts spawn as them, raid waves get them on top of their pillagers; there is a spawn egg too. Numbers are in {@code data/eam/mobs/elite_crossbowman.json}.
 */
public final class EliteCrossbowmanModule {
	/** The red bolt: it goes through a shield, which takes part of the damage itself. */
	public static final ResourceKey<DamageType> RED_BOLT_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("red_bolt"));

	/** Mining speed of swords and shears on the net, as on cobweb. */
	private static final float NET_CUT_SPEED = 15.0F;

	private static volatile EliteCrossbowmanSettings settings = EliteCrossbowmanSettings.DEFAULT;
	private static boolean enabled;

	private EliteCrossbowmanModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.ELITE_CROSSBOWMAN, EliteCrossbowman.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EliteCrossbowmanSettingsLoader.ID, new EliteCrossbowmanSettingsLoader());
		// Whatever cuts cobweb quickly (swords, shears) cuts the net, and evolved spiders' webs, as quickly.
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(item -> true, (builder, item) -> {
			Tool tool = builder.get(DataComponents.TOOL);
			if (tool != null && cutsCobweb(tool)) {
				List<Tool.Rule> rules = new ArrayList<>(tool.rules().size() + 1);
				rules.add(Tool.Rule.minesAndDrops(HolderSet.direct(EamBlocks.NET.builtInRegistryHolder(),
						EamBlocks.SPIDER_WEB.builtInRegistryHolder()), NET_CUT_SPEED));
				rules.addAll(tool.rules());
				builder.set(DataComponents.TOOL, new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(),
						tool.canDestroyBlocksInCreative()));
			}
		}));
	}

	/** Pillagers of patrols and outposts may spawn as elite crossbowmen. */
	public static void init() {
		enabled = true;
	}

	private static boolean cutsCobweb(Tool tool) {
		for (Tool.Rule rule : tool.rules()) {
			if (rule.blocks().contains(Blocks.COBWEB.builtInRegistryHolder())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A pillager just created for a patrol or an outpost: by chance an elite crossbowman takes its place. Any
	 * other entity is returned as it is.
	 */
	public static @Nullable Entity promote(@Nullable Entity created, Level level, EntitySpawnReason reason) {
		if (!enabled || created == null || created.getType() != EntityTypes.PILLAGER || level.getRandom().nextFloat() >= settings.replaceChance()) {
			return created;
		}
		EliteCrossbowman elite = EamEntities.ELITE_CROSSBOWMAN.create(level, reason);
		return elite != null ? elite : created;
	}

	public static EliteCrossbowmanSettings settings() {
		return settings;
	}

	static void setSettings(EliteCrossbowmanSettings value) {
		settings = value;
	}
}
