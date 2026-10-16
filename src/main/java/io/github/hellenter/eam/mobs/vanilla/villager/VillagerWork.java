package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * What a villager of one profession works with, read from {@code data/<namespace>/villager_work/<profession>.json}
 * (the file's id is the profession's id), so datapacks can change it.
 *
 * @param item  the villager's own work item: always in his inventory, held while his hand is free, never given or taken
 * @param tools items a player can give him to work with instead, such as better hoes for a farmer
 * @param goods items his inventory takes besides food, such as brewing ingredients for a cleric
 */
public record VillagerWork(Item item, Optional<TagKey<Item>> tools, Optional<TagKey<Item>> goods) {
	public static final Codec<VillagerWork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(VillagerWork::item),
			TagKey.codec(Registries.ITEM).optionalFieldOf("tools").forGetter(VillagerWork::tools),
			TagKey.codec(Registries.ITEM).optionalFieldOf("goods").forGetter(VillagerWork::goods)
	).apply(instance, VillagerWork::new));
}
