package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

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
	/** The phase he fights in. */
	public int phase = 1;
	/** Whether his sword is in his hand, and his staff. */
	public boolean sword = true;
	public boolean staff;

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
