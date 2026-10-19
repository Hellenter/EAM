package io.github.hellenter.eam.content.horn;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * What the enchanted horn holds: the ghost horse while it rests in the horn, or which horse is out.
 *
 * @param health   its health when it went back into the horn ({@link Float#MAX_VALUE} for a horse never called yet)
 * @param armor    the horse armor it wore then; empty while it is out, since the horse wears it
 * @param horse    the horse called from this horn, while it is out
 * @param storedAt overworld game time it went back into the horn, to heal it for the time it rested there
 */
public record GhostHorn(float health, ItemStack armor, Optional<UUID> horse, long storedAt) {
	public static final GhostHorn FRESH = new GhostHorn(Float.MAX_VALUE, ItemStack.EMPTY, Optional.empty(), 0L);

	public static final Codec<GhostHorn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.FLOAT.optionalFieldOf("health", FRESH.health).forGetter(GhostHorn::health),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("armor", ItemStack.EMPTY).forGetter(GhostHorn::armor),
			UUIDUtil.CODEC.optionalFieldOf("horse").forGetter(GhostHorn::horse),
			Codec.LONG.optionalFieldOf("stored_at", 0L).forGetter(GhostHorn::storedAt)
	).apply(instance, GhostHorn::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GhostHorn> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, GhostHorn::health,
			ItemStack.OPTIONAL_STREAM_CODEC, GhostHorn::armor,
			UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), GhostHorn::horse,
			ByteBufCodecs.VAR_LONG, GhostHorn::storedAt,
			GhostHorn::new);

	/** The horse called out: the horn keeps its id and gives it the armor. */
	public GhostHorn out(UUID id) {
		return new GhostHorn(health, ItemStack.EMPTY, Optional.of(id), storedAt);
	}

	/** The horse back in the horn with its health and armor. */
	public static GhostHorn resting(float health, ItemStack armor, long time) {
		return new GhostHorn(health, armor.copy(), Optional.empty(), time);
	}

	public boolean isOut() {
		return horse.isPresent();
	}

	public boolean holds(UUID id) {
		return horse.isPresent() && horse.get().equals(id);
	}

	// An item stack has no value equality of its own; without these every horn would look changed every tick.
	@Override
	public boolean equals(Object other) {
		return other instanceof GhostHorn that && Float.compare(health, that.health) == 0 && storedAt == that.storedAt
				&& horse.equals(that.horse) && ItemStack.matches(armor, that.armor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(health, ItemStack.hashItemAndComponents(armor), horse, storedAt);
	}
}
