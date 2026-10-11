package io.github.hellenter.eam.survival.bleeding;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.hunger.HungerModule;
import io.github.hellenter.eam.survival.progression.ProgressionModule;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Bleeding: a wound from a vampire bat's bite. It hurts every {@link BleedingSettings#damageInterval()} ticks, through
 * armor, and makes water and food drain {@link BleedingSettings#drainMultiplier()} times as fast. Numbers are in
 * {@code data/eam/survival/bleeding.json}.
 */
public final class BleedingModule {
	public static final Holder<MobEffect> BLEEDING = Registry.registerForHolder(
			BuiltInRegistries.MOB_EFFECT, Eam.id("bleeding"), new BleedingEffect());

	public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Eam.id("bleeding"));

	private static volatile BleedingSettings settings = BleedingSettings.DEFAULT;

	private BleedingModule() {
	}

	/** Registers the effect and its settings; whoever inflicts bleeding decides whether it happens. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BleedingSettingsLoader.ID, new BleedingSettingsLoader());
	}

	static void setSettings(BleedingSettings value) {
		settings = value;
	}

	/** Opens a wound for {@code ticks} ticks, or keeps an open one going that long. */
	public static void bleed(LivingEntity target, LivingEntity source, int ticks) {
		if (ticks > 0) {
			target.addEffect(new MobEffectInstance(BLEEDING, ticks), source);
		}
	}

	/** One tick of bleeding. */
	static void tick(ServerLevel level, LivingEntity mob) {
		MobEffectInstance effect = mob.getEffect(BLEEDING);
		BleedingSettings current = settings;
		if (effect != null && BleedingLogic.hurtsThisTick(effect.getDuration(), current.damageInterval())) {
			mob.hurtServer(level, mob.damageSources().source(DAMAGE), current.damage());
		}
		if (mob instanceof ServerPlayer player && !player.getAbilities().invulnerable && !ProgressionModule.inGracePeriod(player)) {
			player.getFoodData().addExhaustion(BleedingLogic.extraDrain(HungerModule.passiveExhaustion(), current.drainMultiplier()));
			ThirstModule.addPassiveDrain(player, current.drainMultiplier() - 1.0F);
		}
	}

	/** Food exhaustion from activity, raised while the player bleeds. Called from the exhaustion hook. */
	public static float activityExhaustion(Player player, float amount) {
		return BleedingLogic.activity(amount, player.hasEffect(BLEEDING), settings.drainMultiplier());
	}
}
