package io.github.hellenter.eam.mobs.entity.ghosthorse;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.horn.GhostHorn;
import io.github.hellenter.eam.content.horn.GhostHornItem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The ghost horse and its enchanted horn ({@link GhostHornItem}): the horn calls the horse out and back in, the horse
 * keeps to its master and drinks his blows, and its rider takes less damage and deals more. Numbers are in
 * {@code data/eam/mobs/ghost_horse.json}.
 */
public final class GhostHorseModule {
	private static volatile GhostHorseSettings settings = GhostHorseSettings.DEFAULT;

	private GhostHorseModule() {
	}

	public static void register() {
		FabricDefaultAttributeRegistry.register(EamEntities.GHOST_HORSE, GhostHorse.createAttributes());
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GhostHorseSettingsLoader.ID, new GhostHorseSettingsLoader());
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> masterDealt(entity, source, damage));
		// A master who leaves the world takes his horse with him in the horn, armor and all.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> recallAll(handler.player));
	}

	static GhostHorseSettings settings() {
		return settings;
	}

	static void setSettings(GhostHorseSettings value) {
		settings = value;
	}

	/** The horn is blown: the horse comes back into it if it is out, or is called out of it. */
	public static void blow(ServerLevel level, Player player, ItemStack horn) {
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(6), SoundSource.PLAYERS,
				4.0F, 0.6F);
		level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
		GhostHorn held = horn.getOrDefault(GhostHornItem.HORSE, GhostHorn.FRESH);
		if (held.horse().isPresent() && level.getEntityInAnyDimension(held.horse().get()) instanceof GhostHorse horse && horse.isAlive()) {
			recall(horse, horn);
			player.sendOverlayMessage(Component.translatable("item.eam.ghost_horn.recalled").withStyle(ChatFormatting.GRAY));
			return;
		}
		summon(level, player, horn, held);
	}

	private static void summon(ServerLevel level, Player player, ItemStack horn, GhostHorn held) {
		GhostHorse horse = EamEntities.GHOST_HORSE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (horse == null) {
			return;
		}
		Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
		Vec3 front = look.lengthSqr() < 1.0E-4 ? player.position() : player.position().add(look.normalize().scale(2.0));
		horse.snapTo(front.x, player.getY(), front.z, player.getYRot(), 0.0F);
		if (!level.noCollision(horse)) {
			horse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
		}
		horse.finalizeSpawn(level, level.getCurrentDifficultyAt(horse.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
		horse.applySettings();
		horse.setOwner(player);
		horse.setTamed(true);
		GhostHorseSettings numbers = settings;
		horse.setHealth(GhostHorseLogic.rested(held.health(), numbers.health(), numbers.regen(), now(level) - held.storedAt()));
		if (!held.armor().isEmpty()) {
			horse.wearArmor(held.armor().copy());
		}
		level.addFreshEntity(horse);
		horn.set(GhostHornItem.HORSE, held.out(horse.getUUID()));
		level.sendParticles(ParticleTypes.SOUL, horse.getX(), horse.getY(0.5), horse.getZ(), 30, 0.6, 0.8, 0.6, 0.04);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, horse.getX(), horse.getY(0.3), horse.getZ(), 20, 0.6, 0.4, 0.6, 0.02);
		level.playSound(null, horse.getX(), horse.getY(), horse.getZ(), SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.NEUTRAL, 1.5F, 0.6F);
	}

	/** The horse goes back into {@code horn} with its health and armor. */
	private static void recall(GhostHorse horse, ItemStack horn) {
		if (!(horse.level() instanceof ServerLevel level)) {
			return;
		}
		ItemStack armor = horse.getBodyArmorItem().copy();
		horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
		horn.set(GhostHornItem.HORSE, GhostHorn.resting(Math.min(horse.getHealth(), settings.health()), armor, now(level)));
		horse.vanish(level);
	}

	/**
	 * Once a second: a horse whose master is gone or no longer carries its horn fades and drops its armor; one whose
	 * master went far or to another world goes back into the horn.
	 */
	static void checkTether(ServerLevel level, GhostHorse horse) {
		UUID owner = horse.getOwnerReference() == null ? null : horse.getOwnerReference().getUUID();
		ServerPlayer master = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		ItemStack horn = master == null ? ItemStack.EMPTY : findHorn(master, horse.getUUID());
		if (horn.isEmpty()) {
			ItemStack armor = horse.getBodyArmorItem();
			if (!armor.isEmpty()) {
				horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
				horse.spawnAtLocation(level, armor);
			}
			horse.vanish(level);
			return;
		}
		double leash = settings.leashDistance();
		if (master.level() != level || master.distanceToSqr(horse) > leash * leash) {
			recall(horse, horn);
		}
	}

	/** Killed, it leaves its horn empty; two mithril ingots enchant it again. Its armor falls where it died. */
	static void horseDied(ServerLevel level, GhostHorse horse) {
		UUID owner = horse.getOwnerReference() == null ? null : horse.getOwnerReference().getUUID();
		ServerPlayer master = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (master == null) {
			return;
		}
		ItemStack horn = findHorn(master, horse.getUUID());
		if (horn.isEmpty()) {
			return;
		}
		ItemStack empty = new ItemStack(EamItems.EMPTY_GHOST_HORN);
		if (horn == master.containerMenu.getCarried()) {
			master.containerMenu.setCarried(empty);
		} else {
			Inventory inventory = master.getInventory();
			inventory.setItem(slotOf(inventory, horn), empty);
		}
		master.sendOverlayMessage(Component.translatable("item.eam.ghost_horn.died").withStyle(ChatFormatting.DARK_RED));
		level.playSound(null, master.getX(), master.getY(), master.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
	}

	private static int slotOf(Inventory inventory, ItemStack stack) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot) == stack) {
				return slot;
			}
		}
		return -1;
	}

	private static void recallAll(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack horn = inventory.getItem(slot);
			GhostHorn held = horn.get(GhostHornItem.HORSE);
			if (held != null && held.horse().isPresent()
					&& player.level().getEntityInAnyDimension(held.horse().get()) instanceof GhostHorse horse && horse.isAlive()) {
				recall(horse, horn);
			}
		}
	}

	/** The horn that called {@code horse}, in its master's inventory or on his cursor; empty if he does not carry it. */
	private static ItemStack findHorn(ServerPlayer master, UUID horse) {
		Inventory inventory = master.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (holds(stack, horse)) {
				return stack;
			}
		}
		ItemStack carried = master.containerMenu.getCarried();
		return holds(carried, horse) ? carried : ItemStack.EMPTY;
	}

	private static boolean holds(ItemStack stack, UUID horse) {
		GhostHorn held = stack.get(GhostHornItem.HORSE);
		return held != null && held.holds(horse);
	}

	/** Overworld time, the same in every world, so a horse rests in the horn whichever world it went in. */
	private static long now(ServerLevel level) {
		return level.getServer().overworld().getGameTime();
	}

	/** Its master dealt damage: his horse drinks a share of it. */
	private static void masterDealt(LivingEntity victim, DamageSource source, float damage) {
		if (damage <= 0.0F || !(source.getEntity() instanceof ServerPlayer master) || !(master.level() instanceof ServerLevel level)) {
			return;
		}
		if (victim instanceof GhostHorse hit && hit.isMaster(master)) {
			return;
		}
		GhostHorse horse = horseOf(level, master);
		if (horse != null) {
			horse.drink(damage);
		}
	}

	private static @Nullable GhostHorse horseOf(ServerLevel level, Player master) {
		if (master.getVehicle() instanceof GhostHorse ridden && ridden.isMaster(master)) {
			return ridden;
		}
		return level.getEntitiesOfClass(GhostHorse.class, master.getBoundingBox().inflate(settings.leashDistance()),
				horse -> horse.isAlive() && horse.isMaster(master)).stream().findFirst().orElse(null);
	}

	/** Its rider takes a fifth less damage and deals more, whatever deals or takes it; {@code /kill} goes through untouched. */
	public static float hurt(LivingEntity target, DamageSource source, float amount) {
		if (amount <= 0.0F || target.level().isClientSide() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return amount;
		}
		GhostHorseSettings numbers = settings;
		if (target instanceof Player rider && ridesOwnHorse(rider)) {
			amount = GhostHorseLogic.scaled(amount, numbers.riderTaken());
		}
		Entity attacker = source.getEntity();
		if (attacker instanceof Player rider && rider != target && rider.getVehicle() != target && ridesOwnHorse(rider)) {
			amount = GhostHorseLogic.scaled(amount, numbers.riderDealt());
		}
		return amount;
	}

	private static boolean ridesOwnHorse(Player player) {
		return player.getVehicle() instanceof GhostHorse horse && horse.isMaster(player);
	}
}
