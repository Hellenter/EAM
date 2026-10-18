package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * The villager whose gear the trade screen shows beside his trades, the tools his trade can take, and whether he is the
 * elder, whose panel also has the village button.
 */
public record TradePanel(int villagerId, Optional<TagKey<Item>> tools, boolean elder) {
}
