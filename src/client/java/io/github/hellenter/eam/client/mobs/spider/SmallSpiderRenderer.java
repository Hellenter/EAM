package io.github.hellenter.eam.client.mobs.spider;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.spider.WoodSpider;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** The wood spider and the black widow: the vanilla spider model at 0.6 scale with their own skins. */
public class SmallSpiderRenderer extends SpiderRenderer<WoodSpider> {
	public static final ModelLayerLocation WOOD_SPIDER = new ModelLayerLocation(Eam.id("wood_spider"), "main");
	public static final ModelLayerLocation BLACK_WIDOW = new ModelLayerLocation(Eam.id("black_widow"), "main");
	private static final float SCALE = 0.6F;

	private final Identifier texture;

	private SmallSpiderRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, String name) {
		super(context, layer);
		texture = Eam.id("textures/entity/spider/" + name + ".png");
		shadowRadius = 0.8F * SCALE;
	}

	public static SmallSpiderRenderer woodSpider(EntityRendererProvider.Context context) {
		return new SmallSpiderRenderer(context, WOOD_SPIDER, "wood_spider");
	}

	public static SmallSpiderRenderer blackWidow(EntityRendererProvider.Context context) {
		return new SmallSpiderRenderer(context, BLACK_WIDOW, "black_widow");
	}

	public static LayerDefinition createLayer() {
		return SpiderModel.createSpiderBodyLayer().apply(MeshTransformer.scaling(SCALE));
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return texture;
	}
}
