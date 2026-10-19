package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What a blueprint is of, the data component of {@code eam:blueprint}.
 *
 * @param template the structure template the villagers build
 * @param order    the elder's order it came from, for its name
 * @param turns    clockwise quarter turns the player gave it
 */
public record BlueprintData(Identifier template, String order, int turns) {
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
}
