package io.github.hellenter.eam.content.feeder;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.MobAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Livestock feeding. Animals in {@link #LIVESTOCK} eat one {@link #FEED} item a day from a feeder nearby; farmer
 * villagers top feeders up. Livestock also grazes: a bite of grass or of a grass block is half a meal. From its first
 * meal on, an animal that goes more than {@link FeederSettings#thinAfterDays()} days without one gets thin and drops no
 * meat until it has made up the missed days. The block is registered in {@code EamBlocks}.
 */
public final class FeederModule {
	/** Animals that eat from feeders. */
	public static final TagKey<EntityType<?>> LIVESTOCK = TagKey.create(Registries.ENTITY_TYPE, Eam.id("livestock"));
	/** What goes in a feeder: one item is one meal. */
	public static final TagKey<Item> FEED = TagKey.create(Registries.ITEM, Eam.id("livestock_feed"));

	/** How an animal has been fed; only on animals that ate from a feeder. */
	public static final AttachmentType<LivestockState> LIVESTOCK_STATE = AttachmentRegistry.create(Eam.id("livestock"),
			builder -> builder.persistent(LivestockState.CODEC));

	/** A trough's inventory. */
	public static final ExtendedMenuType<FeederMenu, FeederMenu.Layout> MENU = Registry.register(BuiltInRegistries.MENU, Eam.id("feeder"),
			new ExtendedMenuType<>(FeederMenu::new, FeederMenu.Layout.STREAM_CODEC));

	/** Above random strolling, so a hungry animal on grass stops to eat it. */
	private static final int GRAZE_PRIORITY = 4;

	private static volatile FeederSettings settings = FeederSettings.DEFAULT;
	private static boolean enabled;

	private FeederModule() {
	}

	/** Content is always on: the block exists in every world, so its settings always load. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FeederSettingsLoader.ID, new FeederSettingsLoader());
	}

	/** Livestock eats from feeders, gets thin when unfed and breeds at them. */
	public static void init() {
		enabled = true;
		LootTableEvents.MODIFY_DROPS.register(FeederModule::dropNoMeatWhenThin);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Animal animal && isLivestock(animal)) addGrazeGoal(animal);
		});
	}

	private static void addGrazeGoal(Animal animal) {
		GoalSelector goals = ((MobAccessor) animal).eam$goalSelector();
		for (WrappedGoal wrapped : goals.getAvailableGoals()) {
			if (wrapped.getGoal() instanceof GrazeGoal) return; // Already added: the same entity loaded again.
		}
		goals.addGoal(GRAZE_PRIORITY, new GrazeGoal(animal));
	}

	/** A grass bite: half a meal. */
	static void bite(Animal animal) {
		animal.setAttached(LIVESTOCK_STATE, FeederLogic.bite(animal.getAttached(LIVESTOCK_STATE), today(animal.level()), settings.thinAfterDays()));
	}

	public static boolean enabled() {
		return enabled;
	}

	public static FeederSettings settings() {
		return settings;
	}

	static void setSettings(FeederSettings value) {
		settings = value;
	}

	/** Today's overworld day, so sleeping through the night counts as a day. */
	public static long today(Level level) {
		return FeederLogic.day(now(level));
	}

	/** Overworld clock time, which days are counted in. */
	public static long now(Level level) {
		return level.getOverworldClockTime();
	}

	/** How many times as fast {@code entity} grows up and gets ready to breed again: faster while kept at a feeder. */
	public static double growthSpeed(Entity entity) {
		if (!enabled || !isLivestock(entity)) return 1.0;
		return FeederLogic.growthSpeed(entity.getAttached(LIVESTOCK_STATE), now(entity.level()), settings.fedGrowthBoost());
	}

	public static boolean isLivestock(Entity entity) {
		return entity.is(LIVESTOCK);
	}

	public static boolean isThin(Entity entity) {
		return enabled && isLivestock(entity)
				&& FeederLogic.isThin(entity.getAttached(LIVESTOCK_STATE), today(entity.level()), settings.thinAfterDays());
	}

	private static void dropNoMeatWhenThin(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		// Only the animal's own death table, not block drops or tables nested in it.
		if (entity == null || context.hasParameter(LootContextParams.BLOCK_STATE) || !context.hasParameter(LootContextParams.DAMAGE_SOURCE)
				|| entity.getType().getDefaultLootTable().filter(table::is).isEmpty() || !isThin(entity)) {
			return;
		}
		drops.removeIf(stack -> stack.is(ItemTags.MEAT));
	}
}
