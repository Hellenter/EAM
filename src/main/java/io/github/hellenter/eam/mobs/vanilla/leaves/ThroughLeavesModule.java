package io.github.hellenter.eam.mobs.vanilla.leaves;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/**
 * Arrows fly through {@link #ARROWS_PASS} (leaves), and mobs in {@link #SEE_THROUGH} (skeletons) see through them:
 * their line of sight ray ignores those blocks, so they spot a player behind leaves and shoot him there.
 */
public final class ThroughLeavesModule {
	/** Blocks arrows fly through and that do not block the sight of {@link #SEE_THROUGH} mobs. */
	public static final TagKey<Block> ARROWS_PASS = TagKey.create(Registries.BLOCK, Eam.id("arrows_pass"));
	/** Mobs that see through {@link #ARROWS_PASS} blocks. */
	public static final TagKey<EntityType<?>> SEE_THROUGH = TagKey.create(Registries.ENTITY_TYPE, Eam.id("sees_through_leaves"));

	private static boolean enabled;

	private ThroughLeavesModule() {
	}

	public static void init() {
		enabled = true;
	}

	/** True if {@code entity} (an arrow, or a mob looking) passes through this block. */
	public static boolean passes(BlockState state, Entity entity) {
		return enabled && entity != null
				&& (entity instanceof AbstractArrow || entity.is(SEE_THROUGH))
				&& state.is(ARROWS_PASS);
	}

	/** Same, for the entity behind a ray's collision context. */
	public static boolean passes(BlockState state, CollisionContext context) {
		return enabled && context instanceof EntityCollisionContext entityContext && passes(state, entityContext.getEntity());
	}
}
