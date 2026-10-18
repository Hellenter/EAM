package io.github.hellenter.eam.mobs.entity.boss;

/**
 * A boss that knows which blocks players put up around it while it fights them: blocks placed near it while it has a
 * target are remembered by {@link BossBuilding}, and it may break them.
 */
public interface BuildingAware {
	/** Whether it is fighting someone right now, so blocks placed around it count. */
	boolean isFighting();
}
