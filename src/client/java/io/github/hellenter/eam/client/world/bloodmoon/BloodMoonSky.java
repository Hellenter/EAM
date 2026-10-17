package io.github.hellenter.eam.client.world.bloodmoon;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/** Paints the overworld sky, fog and moon red and shows a full moon while the server says a blood moon is up. */
public final class BloodMoonSky {
	private static final Vector3fc SKY_RED = new Vector3f(0.45F, 0.03F, 0.02F);
	private static final Vector3fc FOG_RED = new Vector3f(0.30F, 0.02F, 0.02F);
	private static final float SKY_BLEND = 0.55F;
	private static final float FOG_BLEND = 0.45F;
	/** Multiplies the white moon texture; the moon is drawn additively, so the red sky behind it adds to the red. */
	private static final Vector3fc MOON_RED = new Vector3f(1.0F, 0.12F, 0.08F);

	private static boolean active;

	private BloodMoonSky() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BloodMoonPayload.TYPE, (payload, context) -> active = payload.active());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);
	}

	public static boolean activeIn(ClientLevel level) {
		return active && level != null && level.dimension() == Level.OVERWORLD;
	}

	public static Vector3fc skyColor(Vector3fc color) {
		return new Vector3f(color).lerp(SKY_RED, SKY_BLEND);
	}

	/** The moon's colour modulator: deep red while a blood moon is up, the vanilla one otherwise. */
	public static Vector4f moonColor(Vector4f color) {
		if (activeIn(Minecraft.getInstance().level)) {
			color.mul(MOON_RED.x(), MOON_RED.y(), MOON_RED.z(), 1.0F);
		}
		return color;
	}

	/** Whether the fog seen through {@code camera} turns red: the open air only, not water, lava or powder snow. */
	public static boolean tintsFog(ClientLevel level, Camera camera) {
		return activeIn(level) && camera.getFluidInCamera() == FogType.NONE;
	}

	public static void tintFog(Vector4f color) {
		color.x += (FOG_RED.x() - color.x) * FOG_BLEND;
		color.y += (FOG_RED.y() - color.y) * FOG_BLEND;
		color.z += (FOG_RED.z() - color.z) * FOG_BLEND;
	}
}
