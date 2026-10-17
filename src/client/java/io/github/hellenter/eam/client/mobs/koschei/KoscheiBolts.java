package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.Koschei;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Koschei's lightning, drawn by the level rather than by his renderer: an entity his renderer skips (out of view, or culled
 * by a performance mod that does not know the bolt reaches far past his body) would take the bolt with it. Each frame the
 * bolts of every Koschei casting one are gathered, then drawn from the sword's point to each target.
 */
public final class KoscheiBolts {
	/** A bolt as gathered: from the sword's point, relative to the camera, to each target, relative to the point. */
	private record Cast(Vec3 tip, List<Strike> to, long seed, float growth, boolean gathering) {
	}

	/** Where one bolt strikes, relative to the sword's point; no arcs crawl over the one whose eyes the camera is in. */
	private record Strike(Vec3 at, boolean arcs) {
	}

	/** Seen through the struck player's own eyes the bolt ends this far in front of them and this far under their eyes. */
	private static final double FIRST_PERSON_REACH = 0.9;
	private static final double FIRST_PERSON_DROP = 0.45;
	/** A shield takes it a little further out, where it meets the raised shield. */
	private static final double SHIELD_REACH = 0.7;

	private static final List<Cast> CASTS = new ArrayList<>();

	private KoscheiBolts() {
	}

	public static void register() {
		LevelExtractionEvents.END_EXTRACTION.register(context -> {
			CASTS.clear();
			Vec3 camera = context.camera().position();
			float partialTicks = context.deltaTracker().getGameTimeDeltaPartialTick(false);
			for (Entity entity : context.level().entitiesForRendering()) {
				if (entity instanceof Koschei koschei) {
					gather(koschei, camera, partialTicks);
				}
			}
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			PoseStack poseStack = context.poseStack();
			for (Cast cast : CASTS) {
				poseStack.pushPose();
				poseStack.translate(cast.tip.x, cast.tip.y, cast.tip.z);
				if (cast.gathering) {
					KoscheiBolt.submitSparks(poseStack, context.submitNodeCollector(), Vec3.ZERO, cast.seed);
				}
				for (int i = 0; i < cast.to.size(); i++) {
					// Each bolt its own shape.
					Strike strike = cast.to.get(i);
					KoscheiBolt.submit(poseStack, context.submitNodeCollector(), Vec3.ZERO, strike.at, cast.seed + i * 7919L, cast.growth, strike.arcs);
				}
				poseStack.popPose();
			}
		});
	}

	private static Vec3 horizontal(Vec3 vector) {
		Vec3 flat = new Vec3(vector.x, 0.0, vector.z);
		return flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
	}

	private static void gather(Koschei koschei, Vec3 camera, float partialTicks) {
		float t = koschei.actionTime(partialTicks);
		if (koschei.action() != KoscheiAction.BOLT || t < 8.0F || t >= KoscheiAction.BOLT_END) {
			return;
		}
		float bodyRot = Mth.rotLerp(partialTicks, koschei.yBodyRotO, koschei.yBodyRot);
		Vec3 tip = koschei.getPosition(partialTicks).add(Koschei.swordTipOffset(bodyRot, koschei.getBbHeight()));
		Minecraft minecraft = Minecraft.getInstance();
		boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
		List<Strike> to = new ArrayList<>();
		for (Entity target : koschei.boltTargets()) {
			boolean shielded = target instanceof LivingEntity living && living.isBlocking();
			if (firstPerson && target == minecraft.getCameraEntity()) {
				// Ending in the chest right under the camera, it would pass through the view edge-on and hardly show:
				// it ends a little in front of the eyes instead, coming in from where he stands.
				Vec3 eyes = target.getEyePosition(partialTicks);
				Vec3 in = tip.subtract(eyes).normalize();
				double reach = FIRST_PERSON_REACH + (shielded ? SHIELD_REACH : 0.0);
				to.add(new Strike(eyes.add(in.scale(reach)).add(0.0, -FIRST_PERSON_DROP, 0.0).subtract(tip), false));
			} else {
				Vec3 chest = target.getPosition(partialTicks).add(0.0, target.getBbHeight() * 0.55, 0.0);
				Vec3 at = shielded ? chest.add(horizontal(tip.subtract(chest)).scale(SHIELD_REACH)) : chest;
				to.add(new Strike(at.subtract(tip), !shielded));
			}
		}
		long seed = koschei.getId() * 341873128712L + (long) Math.floor((koschei.tickCount + partialTicks) / 2.0F);
		boolean gathering = to.isEmpty() && t < KoscheiAction.BOLT_RELEASE;
		if (gathering || !to.isEmpty()) {
			CASTS.add(new Cast(tip.subtract(camera), to, seed, (t - KoscheiAction.BOLT_RELEASE) / 3.0F, gathering));
		}
	}
}
