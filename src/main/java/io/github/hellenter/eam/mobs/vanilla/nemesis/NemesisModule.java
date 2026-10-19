package io.github.hellenter.eam.mobs.vanilla.nemesis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * A monster that kills a player becomes their nemesis: it gets a name ("Zombie, slayer of Maxim") and extra health for
 * every kill, and never despawns. It takes none of the player's things. Numbers live in
 * {@code data/eam/mobs/nemesis.json}; bosses and anything in {@link #EXCLUDED} stay as they are.
 */
public final class NemesisModule {
	/** Mobs that never become a nemesis: bosses already have a name and a fight of their own. */
	public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE, Eam.id("not_nemesis"));

	private static final Identifier HEALTH = Eam.id("nemesis_health");

	/** Who a nemesis killed last and how many players it has killed. */
	public record Nemesis(String victim, int kills) {
		static final Codec<Nemesis> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("victim").forGetter(Nemesis::victim),
				Codec.INT.fieldOf("kills").forGetter(Nemesis::kills)
		).apply(instance, Nemesis::new));
	}

	public static final AttachmentType<Nemesis> NEMESIS = AttachmentRegistry.create(Eam.id("nemesis"),
			builder -> builder.persistent(Nemesis.CODEC));

	private static volatile NemesisSettings settings = NemesisSettings.DEFAULT;
	private static boolean enabled;

	private NemesisModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(NemesisSettingsLoader.ID, new NemesisSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(NemesisSettings value) {
		settings = value;
	}

	/**
	 * Called when anything dead is about to drop its loot. For a player, the monster that dealt the killing blow, directly
	 * or with a projectile, becomes a nemesis.
	 */
	public static void onDeathLoot(LivingEntity dead, ServerLevel level, DamageSource source) {
		if (!enabled || !(dead instanceof ServerPlayer victim) || victim.isSpectator()) {
			return;
		}
		if (!(source.getEntity() instanceof Mob killer) || !canBecomeNemesis(killer)) {
			return;
		}
		NemesisSettings settings = NemesisModule.settings;
		Nemesis previous = killer.getAttached(NEMESIS);
		Nemesis nemesis = new Nemesis(victim.getGameProfile().name(), previous == null ? 1 : previous.kills() + 1);
		killer.setAttached(NEMESIS, nemesis);
		if (previous != null || !killer.hasCustomName()) {
			killer.setCustomName(Component.translatable("eam.nemesis.name", killer.getType().getDescription(), nemesis.victim()));
		}
		AttributeInstance health = killer.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			double bonus = NemesisLogic.healthBonus(nemesis.kills(), settings.healthPerKill(), settings.maxKills());
			health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			killer.setHealth(killer.getMaxHealth());
		}
		killer.setPersistenceRequired();
	}

	/**
	 * A nemesis that dies leaving no loot (burned by the sun) still drops the trophies it took in older worlds, from before
	 * nemeses stopped taking gear.
	 */
	public static void dropTrophies(LivingEntity dead) {
		if (dead instanceof Mob mob && mob.hasAttached(NEMESIS) && mob.level() instanceof ServerLevel level) {
			mob.dropPreservedEquipment(level);
		}
	}

	private static boolean canBecomeNemesis(Mob killer) {
		return killer.isAlive() && !killer.is(EXCLUDED) && !(killer instanceof OwnableEntity pet && pet.getOwner() instanceof Player);
	}
}
