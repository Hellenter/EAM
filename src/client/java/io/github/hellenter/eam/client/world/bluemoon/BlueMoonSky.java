package io.github.hellenter.eam.client.world.bluemoon;

import io.github.hellenter.eam.world.bluemoon.BlueMoonPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

/** Paints the overworld sky and fog blue and shows a full moon while the server says a blue moon is up. */
public final class BlueMoonSky {
	private static final Vector3fc SKY_BLUE = new Vector3f(0.10F, 0.24F, 0.62F);
	private static final Vector3fc FOG_BLUE = new Vector3f(0.10F, 0.20F, 0.48F);
	private static final float SKY_BLEND = 0.5F;
	private static final float FOG_BLEND = 0.4F;

	private static boolean active;

	private BlueMoonSky() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BlueMoonPayload.TYPE, (payload, context) -> active = payload.active());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);
	}

	public static boolean activeIn(ClientLevel level) {
		return active && level != null && level.dimension() == Level.OVERWORLD;
	}

	public static Vector3fc skyColor(Vector3fc color) {
		return new Vector3f(color).lerp(SKY_BLUE, SKY_BLEND);
	}

	/** Whether the fog seen through {@code camera} turns blue: the open air only, not water, lava or powder snow. */
	public static boolean tintsFog(ClientLevel level, Camera camera) {
		return activeIn(level) && camera.getFluidInCamera() == FogType.NONE;
	}

	public static void tintFog(Vector4f color) {
		color.x += (FOG_BLUE.x() - color.x) * FOG_BLEND;
		color.y += (FOG_BLUE.y() - color.y) * FOG_BLEND;
		color.z += (FOG_BLUE.z() - color.z) * FOG_BLEND;
	}
}
