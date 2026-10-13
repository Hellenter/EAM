package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Glyph;
import io.github.hellenter.eam.mobs.entity.yaga.HagGlyph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.phys.AABB;

/**
 * Baba Yaga's glyphs: a glowing circle with a pentagram laid flat on the ground, slowly turning, each kind in its own
 * colour (violet blinding, green draining, fiery orange harm) and the acid bolt's bubbling puddle. The glyph of harm
 * pulses faster and faster as it is about to strike, then flares. A fresh glyph lies grey for its first moments and
 * lights up into its colour when it wakes. The small copy over her head faces the camera.
 * <p>
 * A glyph on the ground is drawn in small tiles, each laid on the surface of its own block column (snow layers, slabs
 * and steps included), with a dark copy under it so it still reads on snow and other bright ground.
 */
public class HagGlyphRenderer extends EntityRenderer<HagGlyph, HagGlyphRenderer.State> {
	private static final RenderType[] TYPES = {
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_blind.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_drain.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_harm.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_acid.png")),
	};
	/** The same glyphs in grey, before they wake; the acid puddle has none. */
	/** The dark copies, drawn without glow. */
	private static final RenderType[] SHADOW_TYPES = {
			RenderTypes.entityTranslucent(Eam.id("textures/entity/baba_yaga/glyph_blind.png")),
			RenderTypes.entityTranslucent(Eam.id("textures/entity/baba_yaga/glyph_drain.png")),
			RenderTypes.entityTranslucent(Eam.id("textures/entity/baba_yaga/glyph_harm.png")),
			RenderTypes.entityTranslucent(Eam.id("textures/entity/baba_yaga/glyph_acid.png")),
	};
	private static final RenderType[] INACTIVE_TYPES = {
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_blind_inactive.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_drain_inactive.png")),
			RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/glyph_harm_inactive.png")),
	};
	private static final int FADE = 10;
	/** Ticks the grey glyph takes to light up into its colour. */
	private static final float WAKE = 4.0F;
	/** How bright the grey glyph shows, against a lit one. */
	private static final float INACTIVE_ALPHA = 0.7F;
	private static final int HARM_FLASH = 10;
	/** Tiles per block along each side. */
	private static final int TILES = 4;
	/** How far over the surface the glyph and its dark copy lie. */
	private static final float LIFT = 0.03F;
	private static final float SHADOW_LIFT = 0.015F;
	/** The dark copy under the glyph: a little larger, so its lines get a dark rim. */
	private static final float SHADOW_SCALE = 1.06F;
	private static final float SHADOW_ALPHA = 0.65F;
	/** The ground under a glyph is looked up again this often. */
	private static final int SURFACE_REFRESH = 20;
	private static final Map<HagGlyph, Surface> SURFACES = new WeakHashMap<>();

	public HagGlyphRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HagGlyph entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.kind = entity.kind();
		state.radius = entity.radius();
		state.lifetime = entity.lifetime();
		state.preview = entity.isPreview();
		state.inactiveTicks = entity.inactiveTicks();
		state.surface = state.preview ? null : surface(entity);
	}

	/** The ground under the glyph, looked up again now and then: blocks can change under it. */
	private static Surface surface(HagGlyph entity) {
		Surface surface = SURFACES.get(entity);
		int r = Mth.ceil(entity.radius() * SHADOW_SCALE);
		if (surface == null || entity.tickCount - surface.tick >= SURFACE_REFRESH || surface.reach != r) {
			surface = Surface.scan(entity.level(), entity.getX(), entity.getY(), entity.getZ(), r, entity.tickCount);
			SURFACES.put(entity, surface);
		}
		return surface;
	}

	@Override
	protected AABB getBoundingBoxForCulling(HagGlyph entity, float partialTicks) {
		float r = entity.radius();
		return entity.getBoundingBox().inflate(r, 1.0, r);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		float age = state.ageInTicks;
		float alpha = Math.min(1.0F, age / FADE);
		float scale = 1.0F;
		float spin = age * (state.kind == Glyph.ACID ? 0.0F : 1.2F);
		if (state.kind == Glyph.HARM && !state.preview) {
			float left = state.lifetime - age;
			if (left > 0.0F) {
				// The fuse: faster and brighter the nearer it is to striking.
				float urgency = 1.0F - Mth.clamp(left / state.lifetime, 0.0F, 1.0F);
				float pulse = 0.5F + 0.5F * Mth.sin(age * (0.25F + urgency * 1.4F));
				alpha *= 0.55F + 0.45F * pulse * (0.4F + 0.6F * urgency);
				spin = age * (1.2F + urgency * 6.0F);
			} else {
				// The strike: a flare that swells and fades.
				float flash = Mth.clamp(-left / HARM_FLASH, 0.0F, 1.0F);
				alpha = 1.0F - flash;
				scale = 1.0F + flash * 0.25F;
			}
		} else if (!state.preview) {
			alpha *= Math.min(1.0F, (state.lifetime - age) / FADE);
			alpha *= 0.8F + 0.2F * Mth.sin(age * 0.2F);
		}
		if (alpha <= 0.01F) {
			return;
		}
		float r = state.radius * scale;
		// Grey until it wakes, then its colour fades in over the grey.
		float lit = state.preview || state.kind.ordinal() >= INACTIVE_TYPES.length ? 1.0F
				: Mth.clamp((age - state.inactiveTicks) / WAKE, 0.0F, 1.0F);
		if (!state.preview) {
			int dark = ARGB.color(Mth.floor(255.0F * Mth.clamp(alpha * SHADOW_ALPHA, 0.0F, 1.0F)), 0, 0, 0);
			drawOnGround(poseStack, collector, SHADOW_TYPES[state.kind.ordinal()], state, r * SHADOW_SCALE, spin, dark, SHADOW_LIFT);
		}
		if (lit < 1.0F) {
			draw(poseStack, collector, INACTIVE_TYPES[state.kind.ordinal()], state, r, spin, alpha * INACTIVE_ALPHA * (1.0F - lit), camera);
		}
		if (lit > 0.0F) {
			draw(poseStack, collector, TYPES[state.kind.ordinal()], state, r, spin, alpha * lit, camera);
		}
		super.submit(state, poseStack, collector, camera);
	}

	private static void draw(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, State state, float r, float spin, float alpha,
			CameraRenderState camera) {
		if (alpha <= 0.01F) {
			return;
		}
		int color = ARGB.color(Mth.floor(255.0F * Mth.clamp(alpha, 0.0F, 1.0F)), 255, 255, 255);
		poseStack.pushPose();
		if (state.preview) {
			poseStack.rotate(camera.orientation);
			poseStack.rotateDegrees(Axis.ZP, spin);
			collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
				vertex(buffer, pose, -r, -r, 0.0F, color, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, r, -r, 0.0F, color, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, r, r, 0.0F, color, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
				vertex(buffer, pose, -r, r, 0.0F, color, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
			});
		} else {
			drawOnGround(poseStack, collector, type, state, r, spin, color, LIFT);
		}
		poseStack.popPose();
	}

	/**
	 * The glyph laid on the ground: square tiles fixed to the block grid, each on the surface of its column, the
	 * picture turning across them through the texture coordinates.
	 */
	private static void drawOnGround(PoseStack poseStack, SubmitNodeCollector collector, RenderType type, State state, float r, float spin,
			int color, float lift) {
		Surface surface = state.surface;
		if (surface == null) {
			return;
		}
		float angle = spin * Mth.DEG_TO_RAD;
		float cos = Mth.cos(angle);
		float sin = Mth.sin(angle);
		float tile = 1.0F / TILES;
		// The glyph's centre within its block, for the tile corners relative to it.
		float fx = surface.fracX;
		float fz = surface.fracZ;
		collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
			for (int bx = -surface.reach; bx <= surface.reach; bx++) {
				for (int bz = -surface.reach; bz <= surface.reach; bz++) {
					float top = surface.height(bx, bz);
					if (Float.isNaN(top)) {
						continue;
					}
					float y = top + lift;
					for (int i = 0; i < TILES; i++) {
						for (int j = 0; j < TILES; j++) {
							float x0 = bx - fx + i * tile;
							float z0 = bz - fz + j * tile;
							float x1 = x0 + tile;
							float z1 = z0 + tile;
							// Skip tiles wholly outside the glyph's circle.
							float nx = Mth.clamp(0.0F, x0, x1);
							float nz = Mth.clamp(0.0F, z0, z1);
							if (nx * nx + nz * nz > r * r) {
								continue;
							}
							groundVertex(buffer, pose, x0, y, z0, r, cos, sin, color);
							groundVertex(buffer, pose, x0, y, z1, r, cos, sin, color);
							groundVertex(buffer, pose, x1, y, z1, r, cos, sin, color);
							groundVertex(buffer, pose, x1, y, z0, r, cos, sin, color);
						}
					}
				}
			}
		});
	}

	/** A corner of a ground tile, its texture coordinates taken from the turning picture under it. */
	private static void groundVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float r, float cos, float sin,
			int color) {
		// Into the picture's own frame, turned by the glyph's spin about the vertical.
		float px = x * cos - z * sin;
		float pz = x * sin + z * cos;
		float u = Mth.clamp(0.5F + px / (2.0F * r), 0.0F, 1.0F);
		float v = Mth.clamp(0.5F + pz / (2.0F * r), 0.0F, 1.0F);
		vertex(buffer, pose, x, y, z, color, u, v, 0.0F, 1.0F, 0.0F);
	}

	static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int color, float u, float v, float nx, float ny,
			float nz) {
		buffer.addVertex(pose, x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightCoordsUtil.FULL_BRIGHT)
				.setNormal(pose, nx, ny, nz);
	}

	public static class State extends EntityRenderState {
		public Glyph kind = Glyph.BLIND;
		public float radius = 1.0F;
		public int lifetime = 100;
		public boolean preview;
		public int inactiveTicks;
		public Surface surface;
	}

	/**
	 * The ground's height in each block column around a glyph, relative to the glyph, or NaN where there is none near
	 * its level (a pit or a wall).
	 */
	static final class Surface {
		/** How far a column's surface may lie above or below the glyph and still carry it. */
		private static final int UP = 1;
		private static final int DOWN = 3;

		final int reach;
		final int tick;
		final float fracX;
		final float fracZ;
		private final float[] heights;

		private Surface(int reach, int tick, float fracX, float fracZ) {
			this.reach = reach;
			this.tick = tick;
			this.fracX = fracX;
			this.fracZ = fracZ;
			this.heights = new float[(reach * 2 + 1) * (reach * 2 + 1)];
		}

		float height(int bx, int bz) {
			return heights[(bx + reach) * (reach * 2 + 1) + bz + reach];
		}

		static Surface scan(Level level, double x, double y, double z, int reach, int tick) {
			int cx = Mth.floor(x);
			int cz = Mth.floor(z);
			Surface surface = new Surface(reach, tick, (float) (x - cx), (float) (z - cz));
			int baseY = Mth.floor(y + 0.01);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int bx = -reach; bx <= reach; bx++) {
				for (int bz = -reach; bz <= reach; bz++) {
					float found = Float.NaN;
					for (int by = baseY + UP; by >= baseY - DOWN; by--) {
						pos.set(cx + bx, by, cz + bz);
						double top = top(level, pos);
						if (!Double.isNaN(top)) {
							// Only an open surface carries it, not the inside of a wall.
							double over = top(level, pos.move(0, 1, 0));
							if (Double.isNaN(over) || over < 0.5) {
								found = (float) (by + top - y);
							}
							break;
						}
					}
					surface.heights[(bx + reach) * (reach * 2 + 1) + bz + reach] = found;
				}
			}
			return surface;
		}

		/** The top of what stands in a block to lie on, within the block (0 to 1), or NaN if it is open. */
		private static double top(Level level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				return Double.NaN;
			}
			// A thin snow layer has no collision, but the glyph lies on its surface.
			VoxelShape shape = state.getBlock() instanceof SnowLayerBlock ? state.getShape(level, pos) : state.getCollisionShape(level, pos);
			if (shape.isEmpty()) {
				return Double.NaN;
			}
			return Math.min(1.0, shape.max(Direction.Axis.Y));
		}
	}
}
