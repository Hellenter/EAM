package io.github.hellenter.eam.client.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The Pillager King's trophy: his closed helm with the crimson crest stuck on a pike, a strip of the crest's cloth tied
 * under it. The model looks north and is turned for the other facings; it rises above its block like a fence post with
 * a lantern. The item has its own flat icon.
 */
final class KingTrophyModels {
	private static final Identifier MODEL = Eam.id("block/pillager_king_trophy");
	private static final String HELM = "eam:block/pillager_king_trophy_helm";
	private static final String VISOR = "eam:block/pillager_king_trophy_visor";
	private static final String CREST = "eam:block/pillager_king_trophy_crest";
	private static final String PIKE = "eam:block/pillager_king_trophy_pike";

	private KingTrophyModels() {
	}

	static void generate(BlockModelGenerators generators) {
		generators.modelOutput.accept(MODEL, KingTrophyModels::model);
		generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(EamBlocks.PILLAGER_KING_TROPHY, BlockModelGenerators.plainVariant(MODEL))
				.with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
	}

	private static JsonObject model() {
		JsonArray elements = new JsonArray();
		// The pike, driven into the ground, its top inside the helm.
		elements.add(box(7, 0, 7, 9, 20, 9, "#pike", "#pike"));
		// The bowl of the helm, its face to the north.
		JsonObject bowl = box(4, 18, 4, 12, 26, 12, "#helm", "#helm");
		face(bowl, "north").addProperty("texture", "#visor");
		elements.add(bowl);
		// The rounded top and the flared neck guard.
		elements.add(box(4.5, 26, 4.5, 11.5, 27, 11.5, "#helm", "#helm"));
		elements.add(box(4, 16.5, 10, 12, 18, 12.5, "#helm", "#helm"));
		// Cheek guards reaching down in front.
		elements.add(box(4, 15.5, 3.5, 6.5, 18, 5, "#helm", "#helm"));
		elements.add(box(9.5, 15.5, 3.5, 12, 18, 5, "#helm", "#helm"));
		// The crest from brow to nape, falling down the back.
		elements.add(box(7.25, 27, 3, 8.75, 30, 13, "#crest", "#crest"));
		elements.add(box(7.25, 21, 12.5, 8.75, 29, 14, "#crest", "#crest"));
		// A strip of red cloth tied round the pike under the helm.
		elements.add(box(6.75, 13, 6.75, 9.25, 14.5, 9.25, "#crest", "#crest"));
		elements.add(box(9.25, 8, 8, 12, 14.5, 8, "#crest", null));

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", HELM);
		textures.addProperty("helm", HELM);
		textures.addProperty("visor", VISOR);
		textures.addProperty("crest", CREST);
		textures.addProperty("pike", PIKE);
		JsonObject model = new JsonObject();
		model.addProperty("parent", "minecraft:block/block");
		model.add("textures", textures);
		model.add("elements", elements);
		return model;
	}

	/**
	 * A box with every face textured from the middle of its texture at one texel per pixel, so small parts keep the
	 * grain of the big ones. A flat box (one size 0) gets only its two wide faces; {@code ends} null leaves out the top
	 * and bottom.
	 */
	private static JsonObject box(double x1, double y1, double z1, double x2, double y2, double z2, String sides, String ends) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double dz = z2 - z1;
		JsonObject faces = new JsonObject();
		for (Direction direction : Direction.values()) {
			double w;
			double h;
			switch (direction.getAxis()) {
				case X -> {
					w = dz;
					h = dy;
				}
				case Z -> {
					w = dx;
					h = dy;
				}
				default -> {
					w = dx;
					h = dz;
				}
			}
			boolean end = direction.getAxis() == Direction.Axis.Y;
			if (w == 0 || h == 0 || end && ends == null) {
				continue;
			}
			JsonObject face = new JsonObject();
			face.add("uv", uv(Math.min(w, 16), Math.min(h, 16)));
			face.addProperty("texture", end ? ends : sides);
			faces.add(direction.getSerializedName(), face);
		}
		JsonObject element = new JsonObject();
		element.add("from", vector(x1, y1, z1));
		element.add("to", vector(x2, y2, z2));
		element.add("faces", faces);
		return element;
	}

	private static JsonObject face(JsonObject element, String name) {
		return element.getAsJsonObject("faces").getAsJsonObject(name);
	}

	/** A {@code w} by {@code h} window in the middle of the texture. */
	private static JsonArray uv(double w, double h) {
		double u = (16 - w) / 2;
		double v = (16 - h) / 2;
		JsonArray uv = new JsonArray();
		uv.add(u);
		uv.add(v);
		uv.add(u + w);
		uv.add(v + h);
		return uv;
	}

	private static JsonArray vector(double x, double y, double z) {
		JsonArray vector = new JsonArray();
		vector.add(x);
		vector.add(y);
		vector.add(z);
		return vector;
	}
}
