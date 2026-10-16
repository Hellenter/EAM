package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.List;

public class KoscheiRenderState extends HumanoidRenderState {
	/** What he is doing. */
	public KoscheiAction action = KoscheiAction.NONE;
	/** Ticks into it. */
	public float actionTime;
	public Eyes eyes = Eyes.GLOW;
	/** How high the blow he turns aside comes, 0 at his feet to 1 at the top of his head. */
	public float guardHeight = 0.6F;
	/** Which side it comes from, -1 his right to 1 his left. */
	public float guardSide;
	public Blade blade = Blade.NONE;
	/** Degrees per tick his body turns as he walks round. */
	public float turnRate;
	/** Blocks per tick he moves up (falling: below zero). */
	public float fallSpeed;
	/** How fast he moves over the ground: 0 walking, 1 rushing. */
	public float dash;
	/** The sword's point while he aims or holds the bolt, relative to his feet; null otherwise. */
	public @Nullable Vec3 boltFrom;
	/** Where each bolt strikes, relative to his feet; empty while it is not loosed. */
	public List<Vec3> boltTo = List.of();
	/** Changes every other tick, so the bolt's shape crackles. */
	public long boltSeed;
	/** How far the bolt has grown toward the target, 0 to 1. */
	public float boltGrowth;

	public enum Eyes {
		/** His eyes always glow a cold pale blue. */
		GLOW,
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
