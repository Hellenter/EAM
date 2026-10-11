package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.Eam;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;

/** The looks the Pillager King has had; {@link #CURRENT} is the one he is drawn in. Each one's model layer is registered. */
public enum PillagerKingLook {
	/** Closed black plate over a short chainmail skirt (#166 to #178). */
	PLATE(PillagerKingPlateModel.LAYER, PillagerKingPlateModel::createLayer, PillagerKingPlateModel::new, "pillager_king_plate"),
	/** A chainmail hauberk to the knees, layered belts, pauldrons and a long split leather coat. */
	HAUBERK(PillagerKingHauberkModel.LAYER, PillagerKingHauberkModel::createLayer, PillagerKingHauberkModel::new, "pillager_king");

	public static final PillagerKingLook CURRENT = HAUBERK;

	public final ModelLayerLocation layer;
	public final Supplier<LayerDefinition> definition;
	public final Function<ModelPart, PillagerKingModel> model;
	public final Identifier texture;

	PillagerKingLook(ModelLayerLocation layer, Supplier<LayerDefinition> definition, Function<ModelPart, PillagerKingModel> model, String texture) {
		this.layer = layer;
		this.definition = definition;
		this.model = model;
		this.texture = Eam.id("textures/entity/pillager_king/" + texture + ".png");
	}
}
