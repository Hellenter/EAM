package io.github.hellenter.eam.client.mobs.koschei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Koschei's lightning, built anew every other tick from pieces: a jagged trunk from the sword's point to the target, made
 * by splitting the straight line again and again and knocking each new midpoint aside, with forks branching off it the
 * same way and short arcs crawling over the target. Each piece is a pair of crossed ribbons in three layers, a white core
 * inside pale and deep blue glows, drawn additively like vanilla lightning and from both sides. While it grows the trunk reaches out piece by
 * piece.
 */
final class KoscheiBolt {
	/** Trunk splits: 2^5 = 32 pieces. */
	private static final int TRUNK_DEPTH = 5;
	private static final int BRANCH_DEPTH = 3;
	/** Radius and colour (r, g, b, a) of each layer, outermost first. */
	private static final float[][] LAYERS = {
			{0.16F, 0.15F, 0.25F, 0.75F, 0.25F},
			{0.07F, 0.45F, 0.65F, 1.0F, 0.45F},
			{0.025F, 0.95F, 0.97F, 1.0F, 0.9F}
	};

	private KoscheiBolt() {
	}

	/** The full bolt from {@code from} to {@code to}, grown {@code growth} of the way, with arcs over the target if {@code arcs}. */
	static void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 from, Vec3 to, long seed, float growth, boolean arcs) {
		RandomSource random = RandomSource.create(seed);
		List<Vec3> trunk = path(from, to, TRUNK_DEPTH, 0.12, random);
		int shown = Math.max(1, Math.round((trunk.size() - 1) * Math.clamp(growth, 0.0F, 1.0F)));
		List<List<Vec3>> pieces = new ArrayList<>();
		pieces.add(trunk.subList(0, shown + 1));
		double length = to.subtract(from).length();
		int forks = 3 + random.nextInt(3);
		for (int i = 0; i < forks; i++) {
			int at = 2 + random.nextInt(Math.max(1, trunk.size() - 4));
			if (at >= shown) {
				continue;
			}
			Vec3 start = trunk.get(at);
			Vec3 along = trunk.get(at + 1).subtract(start).normalize();
			Vec3 dir = along.add(randomPerpendicular(along, random).scale(0.9)).normalize();
			double forkLength = length * (0.12 + random.nextDouble() * 0.18);
			pieces.add(path(start, start.add(dir.scale(forkLength)), BRANCH_DEPTH, 0.18, random));
		}
		if (arcs && growth >= 1.0F) {
			// Arcs crawling over the target.
			for (int i = 0; i < 3; i++) {
				Vec3 dir = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
				pieces.add(path(to, to.add(dir.scale(0.5 + random.nextDouble() * 0.4)), 2, 0.25, random));
			}
		}
		draw(poseStack, collector, pieces, 1.0F);
	}

	/** Small arcs flickering round the sword's point while he gathers the bolt. */
	static void submitSparks(PoseStack poseStack, SubmitNodeCollector collector, Vec3 tip, long seed) {
		RandomSource random = RandomSource.create(seed);
		List<List<Vec3>> pieces = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Vec3 dir = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
			pieces.add(path(tip, tip.add(dir.scale(0.3 + random.nextDouble() * 0.3)), 2, 0.3, random));
		}
		draw(poseStack, collector, pieces, 0.5F);
	}

	private static void draw(PoseStack poseStack, SubmitNodeCollector collector, List<List<Vec3>> pieces, float thickness) {
		collector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
			Matrix4fc matrix = pose.pose();
			for (float[] layer : LAYERS) {
				for (int p = 0; p < pieces.size(); p++) {
					List<Vec3> points = pieces.get(p);
					// Forks are thinner than the trunk.
					float radius = layer[0] * thickness * (p == 0 ? 1.0F : 0.6F);
					for (int i = 0; i + 1 < points.size(); i++) {
						ribbon(matrix, buffer, points.get(i), points.get(i + 1), radius, layer);
					}
				}
			}
		});
	}

	/**
	 * A jagged path: the straight line split {@code depth} times, each new midpoint knocked aside by up to {@code jag}
	 * of its piece's length, so the bolt is rough at every scale.
	 */
	private static List<Vec3> path(Vec3 from, Vec3 to, int depth, double jag, RandomSource random) {
		List<Vec3> points = new ArrayList<>();
		points.add(from);
		points.add(to);
		for (int level = 0; level < depth; level++) {
			List<Vec3> next = new ArrayList<>(points.size() * 2);
			for (int i = 0; i + 1 < points.size(); i++) {
				Vec3 a = points.get(i);
				Vec3 b = points.get(i + 1);
				Vec3 piece = b.subtract(a);
				double length = piece.length();
				Vec3 mid = a.add(piece.scale(0.5));
				if (length > 1.0E-4) {
					mid = mid.add(randomPerpendicular(piece.scale(1.0 / length), random).scale(length * jag * (random.nextDouble() * 2.0 - 1.0) * 2.0));
				}
				next.add(a);
				next.add(mid);
			}
			next.add(points.getLast());
			points = next;
		}
		return points;
	}

	private static Vec3 randomPerpendicular(Vec3 dir, RandomSource random) {
		Vec3 helper = Math.abs(dir.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
		Vec3 u = dir.cross(helper).normalize();
		Vec3 v = dir.cross(u).normalize();
		double angle = random.nextDouble() * Math.PI * 2.0;
		return u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
	}

	/** Two crossed quads along a piece, so it shows from any side. */
	private static void ribbon(Matrix4fc matrix, VertexConsumer buffer, Vec3 a, Vec3 b, float radius, float[] colour) {
		Vec3 dir = b.subtract(a);
		if (dir.lengthSqr() < 1.0E-8) {
			return;
		}
		dir = dir.normalize();
		Vec3 helper = Math.abs(dir.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
		Vec3 u = dir.cross(helper).normalize().scale(radius);
		Vec3 v = dir.cross(u).normalize().scale(radius);
		quad(matrix, buffer, a, b, u, colour);
		quad(matrix, buffer, a, b, v, colour);
	}

	private static void quad(Matrix4fc matrix, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 side, float[] c) {
		vertex(matrix, buffer, a.add(side), c);
		vertex(matrix, buffer, b.add(side), c);
		vertex(matrix, buffer, b.subtract(side), c);
		vertex(matrix, buffer, a.subtract(side), c);
		// And the other way round, for the back face.
		vertex(matrix, buffer, a.subtract(side), c);
		vertex(matrix, buffer, b.subtract(side), c);
		vertex(matrix, buffer, b.add(side), c);
		vertex(matrix, buffer, a.add(side), c);
	}

	private static void vertex(Matrix4fc matrix, VertexConsumer buffer, Vec3 at, float[] c) {
		buffer.addVertex(matrix, (float) at.x, (float) at.y, (float) at.z).setColor(c[1], c[2], c[3], c[4]);
	}
}
