package io.github.hellenter.eam.client.mobs.villager;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

import java.util.function.Predicate;

/** Extra villager parts with their own texture, posed like the villager: his bare hands, his mail. */
public class VillagerPartLayer extends RenderLayer<VillagerFigureRenderState, VillagerFigureModel> {
	private final VillagerFigureModel model;
	private final Identifier texture;
	private final Predicate<VillagerFigureRenderState> shows;

	public VillagerPartLayer(RenderLayerParent<VillagerFigureRenderState, VillagerFigureModel> renderer, VillagerFigureModel model,
			Identifier texture, Predicate<VillagerFigureRenderState> shows) {
		super(renderer);
		this.model = model;
		this.texture = texture;
		this.shows = shows;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, VillagerFigureRenderState state,
			float yRot, float xRot) {
		if (shows.test(state)) coloredCutoutModelCopyLayerRender(model, texture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
	}
}
