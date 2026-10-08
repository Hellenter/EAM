package io.github.hellenter.eam.mobs.vanilla.drops;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Death and explosion drops:
 * <ul>
 * <li>mobs in {@link #SMALL_LEATHER} (sheep, pigs) also roll {@link #SMALL_LEATHER_LOOT}, a third of a cow's leather;</li>
 * <li>a mob that burns to death in daylight drops no items, equipment or experience;</li>
 * <li>blocks in {@link #LOST_TO_CREEPERS} (logs, planks) destroyed by a creeper explosion drop nothing.</li>
 * </ul>
 */
public final class MobDropsModule {
	/** Mobs whose death loot also rolls {@link #SMALL_LEATHER_LOOT}. */
	public static final TagKey<EntityType<?>> SMALL_LEATHER = TagKey.create(Registries.ENTITY_TYPE, Eam.id("drops_small_leather"));
	/** Extra leather for {@link #SMALL_LEATHER} mobs. */
	public static final ResourceKey<LootTable> SMALL_LEATHER_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Eam.id("entities/small_leather"));
	/** Blocks that drop nothing when a creeper blows them up. */
	public static final TagKey<Block> LOST_TO_CREEPERS = TagKey.create(Registries.BLOCK, Eam.id("lost_to_creepers"));

	/** Game time a mob was last set on fire by daylight. Lost on unload, like the short fire it describes. */
	public static final AttachmentType<Long> SUN_IGNITION = AttachmentRegistry.create(Eam.id("sun_ignition"));

	private static boolean enabled;

	private MobDropsModule() {
	}

	public static void init() {
		enabled = true;
		LootTableEvents.MODIFY_DROPS.register(MobDropsModule::modifyDrops);
	}

	private static void modifyDrops(Holder<LootTable> table, LootContext context, List<ItemStack> drops) {
		Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
		if (entity == null) return;

		BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
		if (state != null) {
			// Block drops from an explosion carry the exploding entity.
			if (entity instanceof Creeper && context.hasParameter(LootContextParams.EXPLOSION_RADIUS) && state.is(LOST_TO_CREEPERS)) {
				drops.clear();
			}
			return;
		}

		// Only the mob's own death table, not tables nested in it.
		if (entity.is(SMALL_LEATHER) && context.hasParameter(LootContextParams.DAMAGE_SOURCE)
				&& entity.getType().getDefaultLootTable().filter(table::is).isPresent()) {
			context.getLevel().getServer().reloadableRegistries().getLootTable(SMALL_LEATHER_LOOT).getRandomItemsRaw(context, drops::add);
		}
	}

	/** Called each time daylight sets {@code mob} on fire. */
	public static void onSunIgnition(Entity mob) {
		if (enabled) mob.setAttached(SUN_IGNITION, mob.level().getGameTime());
	}

	/** False if {@code entity} burned to death from daylight and must leave nothing behind. */
	public static boolean dropsOnDeath(LivingEntity entity, DamageSource source) {
		if (!enabled) return true;
		Long ignition = entity.getAttached(SUN_IGNITION);
		return !MobDropsLogic.burnedBySun(source.is(DamageTypes.ON_FIRE), ignition == null ? -1 : ignition, entity.level().getGameTime());
	}
}
