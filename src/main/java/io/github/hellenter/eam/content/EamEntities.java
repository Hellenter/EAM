package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ground.ThrownPebble;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.LordShield;
import io.github.hellenter.eam.mobs.entity.fallenlord.ThrownLordSpear;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity types added by EAM: projectiles and our mobs (their code lives in {@code mobs/entity}). */
public final class EamEntities {
	public static final EntityType<ThrownPebble> PEBBLE = register("pebble",
			EntityType.Builder.<ThrownPebble>of(ThrownPebble::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** Crypt boss: a skeleton as tall as a wither skeleton. */
	public static final EntityType<FallenLord> FALLEN_LORD = register("fallen_lord",
			EntityType.Builder.of(FallenLord::new, MobCategory.MONSTER)
					.noLootTable()
					.sized(0.7F, 2.4F)
					.eyeHeight(2.1F)
					.clientTrackingRange(10));

	public static final EntityType<ThrownLordSpear> LORD_SPEAR = register("lord_spear",
			EntityType.Builder.<ThrownLordSpear>of(ThrownLordSpear::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** The Fallen Lord's shield lying where he dropped it. */
	public static final EntityType<LordShield> LORD_SHIELD = register("lord_shield",
			EntityType.Builder.<LordShield>of(LordShield::new, MobCategory.MISC)
					.noLootTable()
					.sized(1.0F, 0.2F)
					.clientTrackingRange(6)
					.updateInterval(20));

	private EamEntities() {
	}

	public static void register() {
		// Loads the class, which registers the entity types above.
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Eam.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
