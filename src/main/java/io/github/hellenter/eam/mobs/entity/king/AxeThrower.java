package io.github.hellenter.eam.mobs.entity.king;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** A mob that throws a {@link ThrownKingAxe} and gets it back: the Pillager King and his guards. */
public interface AxeThrower {
	/** The thrown axe is back in hand. */
	void catchAxe();

	/** The thrown axe knocked out {@code target}'s shield. */
	void axeBrokeShield(LivingEntity target);

	/** Damage of the thrown axe to {@code target}. */
	float axeDamageTo(float damage, Entity target);
}
