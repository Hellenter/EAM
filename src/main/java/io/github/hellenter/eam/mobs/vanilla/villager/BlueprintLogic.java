package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure blueprint rules, unit tested: how a template turns and where it stands. */
public final class BlueprintLogic {
	private BlueprintLogic() {
	}

	/**
	 * Where a template block at {@code x}, {@code z} of a template {@code sizeX} by {@code sizeZ} lands once the
	 * template is turned clockwise {@code turns} quarter turns, its corner kept at the origin: {@code {x, z}}.
	 */
	public static int[] turn(int x, int z, int sizeX, int sizeZ, int turns) {
		return switch (Math.floorMod(turns, 4)) {
			case 1 -> new int[] {sizeZ - 1 - z, x};
			case 2 -> new int[] {sizeX - 1 - x, sizeZ - 1 - z};
			case 3 -> new int[] {z, sizeX - 1 - x};
			default -> new int[] {x, z};
		};
	}

	/** The template's size along x and z once turned: {@code {x, z}}. */
	public static int[] turnedSize(int sizeX, int sizeZ, int turns) {
		return Math.floorMod(turns, 2) == 1 ? new int[] {sizeZ, sizeX} : new int[] {sizeX, sizeZ};
	}

	/**
	 * The corner a turned template is placed from so that it stands centred on the block a player looks at, its bottom
	 * layer level with that block: the bottom layer is the template's foundation and takes the place of the ground.
	 * {@code {x, y, z}}.
	 */
	public static int[] origin(int lookX, int lookY, int lookZ, int turnedX, int turnedZ) {
		return new int[] {lookX - turnedX / 2, lookY, lookZ - turnedZ / 2};
	}
}
