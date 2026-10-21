package io.github.hellenter.eam.content.lockedchest;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/** Opens a lone locked chest, or both halves of a double one as six rows. */
public record LockedChestMenuProvider(List<LockedChestBlockEntity> halves) implements ExtendedMenuProvider<LockedChestMenu.OpenData> {
	@Override
	public Component getDisplayName() {
		return halves.size() > 1 ? Component.translatable("container.eam.locked_chest_double") : halves.getFirst().getDisplayName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		Container container = halves.size() > 1 ? new CompoundContainer(halves.get(0), halves.get(1)) : halves.getFirst();
		return new LockedChestMenu(containerId, inventory, container, halves, getScreenOpeningData((ServerPlayer) player));
	}

	/** Whose the chest is, as the first half says (a half placed onto another takes its owner). */
	@Override
	public LockedChestMenu.OpenData getScreenOpeningData(ServerPlayer player) {
		ServerLevel level = player.level();
		LockedChestBlockEntity first = halves.getFirst();
		boolean canChange = halves.stream().allMatch(half -> half.placedBy(player)) || player.isCreative();
		return new LockedChestMenu.OpenData(halves.size() * 3, first.ownerName(level), canChange, first.ownerId(level),
				canChange ? LockedChestModule.candidates(level, first.getBlockPos()) : List.of());
	}
}
