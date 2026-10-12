package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.boss.BossParry;
import io.github.hellenter.eam.mobs.entity.drowned.DrownedSoldier;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Baba Yaga, the swamp witch boss. In phase one she circles in her mortar, out of reach of anything but a parry, and
 * fights with waves of swamp creatures, poison flasks, spells and glyphs; in phase two, on foot, she keeps behind one
 * group of creatures and fights with bolts, a fire beam and her claws. Not placed in the world yet: spawn her with the
 * spawn egg or {@code /summon eam:baba_yaga}. Numbers are in {@code data/eam/mobs/baba_yaga.json}.
 */
public final class BabaYagaModule {
	/** Her glyph of harm and her acid: through armor and shields. */
	public static final ResourceKey<DamageType> CURSE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("baba_yaga_curse"));
	/** Her fire beam: through armor; a shield held towards her blocks it, which she checks herself. */
	public static final ResourceKey<DamageType> BEAM_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("baba_yaga_beam"));
	/** The entity tag on the creatures she raised. */
	public static final String MINION_TAG = "eam.baba_yaga_minion";
	/** Her creatures this close to her never burn in daylight. */
	public static final double SUN_SHELTER_RADIUS = 100.0;

	private static volatile BabaYagaSettings settings = BabaYagaSettings.DEFAULT;

	private BabaYagaModule() {
	}

	public static void register() {
		BossParry.register();
		FabricDefaultAttributeRegistry.register(EamEntities.BABA_YAGA, BabaYaga.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.HAG_THREAD, HagThread.createAttributes());
		FabricDefaultAttributeRegistry.register(EamEntities.DROWNED_SOLDIER, DrownedSoldier.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BabaYagaSettingsLoader.ID, new BabaYagaSettingsLoader());
	}

	public static BabaYagaSettings settings() {
		return settings;
	}

	static void setSettings(BabaYagaSettings value) {
		settings = value;
	}

	/** Whether Baba Yaga raised this creature: it fights at once, in the open, and her spells and glyphs help it. */
	public static boolean isMinion(Entity entity) {
		return entity.entityTags().contains(MINION_TAG);
	}

	/** Whether daylight spares this mob: one of her creatures within {@link #SUN_SHELTER_RADIUS} of a Baba Yaga. */
	public static boolean shelteredFromSun(Mob mob) {
		return isMinion(mob) && !mob.level().getEntitiesOfClass(BabaYaga.class, mob.getBoundingBox().inflate(SUN_SHELTER_RADIUS),
				yaga -> yaga.isAlive() && yaga.distanceToSqr(mob) <= SUN_SHELTER_RADIUS * SUN_SHELTER_RADIUS).isEmpty();
	}
}
