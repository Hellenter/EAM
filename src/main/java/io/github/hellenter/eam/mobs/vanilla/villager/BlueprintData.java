package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * What a blueprint is of, the data component of {@code eam:blueprint}.
 *
 * @param template the structure template the villagers build
 * @param order    the elder's order it came from, for its name
 * @param turns    clockwise quarter turns the player gave it
 */
public record BlueprintData(Identifier template, String order, int turns) {
	/** The vanilla village styles a house blueprint can be of, named by the template's folder. */
	public static final Set<String> STYLES = Set.of("plains", "desert", "savanna", "snowy", "taiga");

	public static final Codec<BlueprintData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("template").forGetter(BlueprintData::template),
			Codec.STRING.fieldOf("order").forGetter(BlueprintData::order),
			Codec.intRange(0, 3).optionalFieldOf("turns", 0).forGetter(BlueprintData::turns)
	).apply(instance, BlueprintData::new));
	public static final StreamCodec<ByteBuf, BlueprintData> STREAM_CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, BlueprintData::template,
			ByteBufCodecs.STRING_UTF8, BlueprintData::order,
			ByteBufCodecs.VAR_INT, BlueprintData::turns,
			BlueprintData::new);

	public BlueprintData {
		turns = Math.floorMod(turns, 4);
	}

	/** The same blueprint turned a quarter turn further clockwise. */
	public BlueprintData turned() {
		return new BlueprintData(template, order, (turns + 1) % 4);
	}

	/**
	 * The village style of a house blueprint, from its vanilla template's folder ({@code village/<style>/houses/...}):
	 * one of {@link #STYLES}, or {@code null} for a building that is the same in every village.
	 */
	public @Nullable String style() {
		String[] parts = template.getPath().split("/");
		return parts.length >= 3 && parts[0].equals("village") && STYLES.contains(parts[1]) ? parts[1] : null;
	}

	/** Which house of its kind and style the template is, from its name's last number; 0 for none. */
	public int number() {
		String path = template.getPath();
		int at = path.lastIndexOf('_');
		if (at < 0) return 0;
		try {
			return Integer.parseInt(path.substring(at + 1));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
