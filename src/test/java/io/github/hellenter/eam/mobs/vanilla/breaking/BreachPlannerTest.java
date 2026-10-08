package io.github.hellenter.eam.mobs.vanilla.breaking;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

class BreachPlannerTest {
	private static final int DIRT = 300;
	private static final int PLANKS = 1200;

	private final BreachPlanner planner = new BreachPlanner(1200, 16, 5, 10);

	/** Flat stone floor at y = -1, air above, plus whatever blocks a test puts in. */
	private static final class World implements BreachPlanner.Terrain {
		final Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();

		World set(int x, int y, int z, int cell) {
			blocks.put(BlockPos.asLong(x, y, z), cell);
			return this;
		}

		/** A one-block-thick box of {@code cell} around the cell (cx, 0, cz), two blocks tall plus a roof. */
		World box(int cx, int cz, int cell) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dz != 0) {
						set(cx + dx, 0, cz + dz, cell).set(cx + dx, 1, cz + dz, cell);
					}
					set(cx + dx, 2, cz + dz, cell);
				}
			}
			return this;
		}

		@Override
		public int cell(int x, int y, int z) {
			long key = BlockPos.asLong(x, y, z);
			if (blocks.containsKey(key)) {
				return blocks.get(key);
			}
			return y < 0 ? BreachPlanner.SOLID : BreachPlanner.AIR;
		}
	}

	@Test
	void openGroundNeedsNoBreaking() {
		BreachPlanner.Plan plan = planner.plan(new World(), 0, 0, 0, 6, 0, 0, 2);
		assertTrue(plan.reaches());
		assertEquals(-1, plan.firstBreakingStep());
	}

	@Test
	void breaksIntoDirtBoxTowardTheTarget() {
		World world = new World().box(5, 0, DIRT);
		BreachPlanner.Plan plan = planner.plan(world, 0, 0, 0, 5, 0, 0, 2);
		assertTrue(plan.reaches());
		BreachPlanner.Step step = plan.steps().get(plan.firstBreakingStep());
		// The wall facing the mob, head block first.
		assertArrayEquals(new long[] {BlockPos.asLong(4, 1, 0), BlockPos.asLong(4, 0, 0)}, step.toBreak());
		assertEquals(1, plan.steps().stream().filter(s -> s.toBreak().length > 0).count());
	}

	@Test
	void prefersSofterSideOfTheWall() {
		// Planks everywhere except dirt on the far side: walking around is cheaper than planks.
		World world = new World().box(5, 0, PLANKS).set(6, 0, 0, DIRT).set(6, 1, 0, DIRT);
		BreachPlanner.Plan plan = planner.plan(world, 0, 0, 0, 5, 0, 0, 2);
		assertTrue(plan.reaches());
		long[] broken = plan.steps().get(plan.firstBreakingStep()).toBreak();
		assertArrayEquals(new long[] {BlockPos.asLong(6, 1, 0), BlockPos.asLong(6, 0, 0)}, broken);
	}

	@Test
	void stoneBoxCannotBeEntered() {
		World world = new World().box(5, 0, BreachPlanner.SOLID);
		BreachPlanner.Plan plan = planner.plan(world, 0, 0, 0, 5, 0, 0, 2);
		assertFalse(plan.reaches());
		assertEquals(-1, plan.firstBreakingStep());
	}

	@Test
	void neverBreaksTheBlockUnderTheMob() {
		// Target right below the mob in a dirt pit: the mob digs a staircase from the side, not straight down.
		World world = new World();
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				for (int y = -4; y < 0; y++) {
					world.set(x, y, z, DIRT);
				}
			}
		}
		world.set(0, -3, 0, BreachPlanner.AIR).set(0, -2, 0, BreachPlanner.AIR);
		BreachPlanner.Plan plan = planner.plan(world, 0, 0, 0, 0, -3, 0, 2);
		assertTrue(plan.reaches());
		long feet = BlockPos.asLong(0, 0, 0);
		int x = 0;
		int y = 0;
		int z = 0;
		for (BreachPlanner.Step step : plan.steps()) {
			for (long cell : step.toBreak()) {
				assertFalse(BlockPos.getX(cell) == x && BlockPos.getZ(cell) == z && BlockPos.getY(cell) < y,
						"broke a block under the mob");
			}
			x = BlockPos.getX(step.feet());
			y = BlockPos.getY(step.feet());
			z = BlockPos.getZ(step.feet());
		}
		assertTrue(feet != plan.steps().getFirst().feet());
	}

	@Test
	void costOfOnePlan() {
		// 14 blocks away from a dirt hut on a flat plain: the usual case. Prints the cost of one plan.
		World world = new World().box(10, 0, DIRT);
		BreachPlanner.Plan warm = null;
		for (int i = 0; i < 2000; i++) {
			warm = planner.plan(world, -4, 0, 3, 10, 0, 0, 2);
		}
		int runs = 5000;
		long started = System.nanoTime();
		for (int i = 0; i < runs; i++) {
			planner.plan(world, -4, 0, 3, 10, 0, 0, 2);
		}
		long nanos = (System.nanoTime() - started) / runs;
		System.out.printf("Hut plan: %d nodes, %d us%n", warm.expanded(), nanos / 1000);

		BreachPlanner.Plan worst = planner.plan(new World().box(10, 0, BreachPlanner.SOLID), 0, 0, 0, 10, 0, 0, 2);
		started = System.nanoTime();
		for (int i = 0; i < runs; i++) {
			planner.plan(new World().box(10, 0, BreachPlanner.SOLID), 0, 0, 0, 10, 0, 0, 2);
		}
		nanos = (System.nanoTime() - started) / runs;
		System.out.printf("Stone box (search hits the cap): %d nodes, %d us%n", worst.expanded(), nanos / 1000);
		assertTrue(warm.reaches());
	}
}
