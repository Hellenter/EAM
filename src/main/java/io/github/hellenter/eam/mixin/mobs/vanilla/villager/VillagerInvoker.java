package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Opens a villager's trades from the trade button of his inventory. */
@Mixin(Villager.class)
public interface VillagerInvoker {
	@Invoker("startTrading")
	void eam$startTrading(Player player);
}
