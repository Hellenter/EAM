package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** The five kinds of resources a village gathers for its elder; each profession brings one. */
public enum VillageResource implements StringRepresentable {
	WOOD("wood"),
	METAL("metal"),
	STONE("stone"),
	MONEY("money"),
	PROVISIONS("provisions");

	public static final Codec<VillageResource> CODEC = StringRepresentable.fromEnum(VillageResource::values);
	public static final StreamCodec<ByteBuf, VillageResource> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], VillageResource::ordinal);

	private final String name;

	VillageResource(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
