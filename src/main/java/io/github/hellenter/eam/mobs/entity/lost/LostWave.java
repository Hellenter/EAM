package io.github.hellenter.eam.mobs.entity.lost;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

/** Whom a call to the Lost brings. */
public enum LostWave implements StringRepresentable {
	/** A squad on foot: a leader and his followers. */
	SQUAD("squad"),
	/** A pair of heavy cavalrymen on their horses. */
	CAVALRY("cavalry"),
	/** A centurion at the head of his own squad. */
	CENTURION("centurion");

	public static final Codec<LostWave> CODEC = StringRepresentable.fromEnum(LostWave::values);
	private static final IntFunction<LostWave> BY_ID = ByIdMap.continuous(LostWave::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StreamCodec<ByteBuf, LostWave> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, LostWave::ordinal);

	private final String name;

	LostWave(String name) {
		this.name = name;
	}

	/** The next wave in turn, back to the first after the last. */
	public LostWave next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public String translationKey() {
		return "item.eam.commander_horn.wave." + name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
