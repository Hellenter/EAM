package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

public class KoscheiRenderState extends HumanoidRenderState {
	/** What he is doing. */
	public KoscheiAction action = KoscheiAction.NONE;
	/** Ticks into it. */
	public float actionTime;
	public Eyes eyes = Eyes.NONE;
	public Blade blade = Blade.NONE;
	/** Degrees per tick his body turns as he walks round. */
	public float turnRate;
	/** Blocks per tick he moves up (falling: below zero). */
	public float fallSpeed;
	/** How fast he moves over the ground: 0 walking, 1 rushing. */
	public float dash;
	/** The sword's point while he aims or holds the bolt, relative to his feet; null otherwise. */
	public @Nullable Vec3 boltFrom;
	/** Where the bolt strikes, relative to his feet; null while it is not loosed. */
	public @Nullable Vec3 boltTo;
	/** Changes every other tick, so the bolt's shape crackles. */
	public long boltSeed;
	/** How far the bolt has grown toward the target, 0 to 1. */
	public float boltGrowth;

	public enum Eyes {
		NONE,
		/** He can be parried now. */
		YELLOW,
		/** About to vanish: purple and blue in turn. */
		WARP
	}

	public enum Blade {
		NONE,
		ORANGE,
		RED
	}
}
