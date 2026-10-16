package io.github.hellenter.eam.client.mobs.villager;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.world.entity.npc.villager.VillagerData;

import org.jspecify.annotations.Nullable;

/** A villager drawn as a humanoid: free arms, held items and armor. */
public class VillagerFigureRenderState extends HumanoidRenderState implements VillagerDataHolderRenderState {
	public boolean isUnhappy;
	public @Nullable VillagerData villagerData;

	@Override
	public @Nullable VillagerData getVillagerData() {
		return villagerData;
	}
}
