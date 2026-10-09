package io.github.hellenter.eam.world.structures.crypt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Pure layout of one crypt level on a grid of 15x15 cells. Cell {@code (0, 0)} is the first room past the entrance,
 * which lies south of it; the level grows north, east and west, never south of row 0 where the entrance stands, and
 * stays within {@link #MAX_SIDEWAYS} and {@link #MAX_NORTH} cells of the first room.
 * <p>
 * Doors are a 4-bit mask: {@link #NORTH}, {@link #EAST}, {@link #SOUTH}, {@link #WEST}. Turning a mask clockwise by a
 * quarter moves every bit one place up, the same way {@code Rotation.CLOCKWISE_90} turns north into east.
 */
public final class CryptLayout {
	public static final int NORTH = 1;
	public static final int EAST = 2;
	public static final int SOUTH = 4;
	public static final int WEST = 8;
	private static final int[] DIRECTIONS = {NORTH, EAST, SOUTH, WEST};

	/**
	 * How far the level may spread from the first room, in cells: a structure only reaches 8 chunks (128 blocks) from
	 * the chunk it starts in, and the first room is already one cell north of it.
	 */
	public static final int MAX_SIDEWAYS = 4;
	public static final int MAX_NORTH = 7;

	/** Chance in percent that the main path keeps going straight. */
	private static final int STRAIGHT_CHANCE = 60;
	/** Chance in percent that a straight cell of the main path is a fight room. */
	private static final int FIGHT_CHANCE = 55;

	public enum Role {
		CORRIDOR, FIGHT, TREASURY
	}

	public record Cell(int x, int z) {
		Cell step(int direction) {
			return switch (direction) {
				case NORTH -> new Cell(x, z - 1);
				case SOUTH -> new Cell(x, z + 1);
				case EAST -> new Cell(x + 1, z);
				default -> new Cell(x - 1, z);
			};
		}
	}

	public static final class Room {
		private Role role = Role.CORRIDOR;
		private int doors;

		public Role role() {
			return role;
		}

		public int doors() {
			return doors;
		}
	}

	private final Map<Cell, Room> rooms = new LinkedHashMap<>();

	private CryptLayout() {
	}

	public Map<Cell, Room> rooms() {
		return rooms;
	}

	/**
	 * Lays out one level: a main path of up to {@code mainLength} rooms from the entrance that ends in a treasury, fight
	 * rooms on its straight stretches (never three in a row), and up to {@code branches} side passages that end in a
	 * treasury too.
	 *
	 * @param nextInt returns a random int in {@code [0, bound)}
	 */
	public static CryptLayout generate(IntUnaryOperator nextInt, int mainLength, int branches) {
		CryptLayout layout = new CryptLayout();
		Cell start = new Cell(0, 0);
		layout.rooms.put(start, new Room());
		layout.rooms.get(start).doors = SOUTH;

		List<Cell> path = new ArrayList<>(List.of(start));
		int direction = NORTH;
		while (path.size() < mainLength) {
			Cell from = path.getLast();
			List<Integer> free = layout.freeDirections(from);
			if (free.isEmpty()) break;
			if (!free.contains(direction) || nextInt.applyAsInt(100) >= STRAIGHT_CHANCE) {
				direction = free.get(nextInt.applyAsInt(free.size()));
			}
			path.add(layout.connect(from, direction));
		}
		layout.rooms.get(path.getLast()).role = Role.TREASURY;

		int fightsInRow = 0;
		Cell firstStraight = null;
		boolean anyFight = false;
		for (int i = 1; i < path.size() - 1; i++) {
			Room room = layout.rooms.get(path.get(i));
			if (!isStraight(room.doors)) {
				fightsInRow = 0;
				continue;
			}
			if (firstStraight == null) firstStraight = path.get(i);
			if (fightsInRow < 2 && nextInt.applyAsInt(100) < FIGHT_CHANCE) {
				room.role = Role.FIGHT;
				fightsInRow++;
				anyFight = true;
			} else {
				fightsInRow = 0;
			}
		}
		if (!anyFight && firstStraight != null) layout.rooms.get(firstStraight).role = Role.FIGHT;

		List<Cell> forks = new ArrayList<>();
		for (int i = 1; i < path.size() - 1; i++) {
			Room room = layout.rooms.get(path.get(i));
			if (room.role == Role.CORRIDOR && isStraight(room.doors)) forks.add(path.get(i));
		}
		for (int made = 0; made < branches && !forks.isEmpty(); made++) {
			Cell fork = forks.remove(nextInt.applyAsInt(forks.size()));
			List<Integer> free = layout.freeDirections(fork);
			if (free.isEmpty()) continue;
			Cell end = layout.connect(fork, free.get(nextInt.applyAsInt(free.size())));
			int length = 1 + nextInt.applyAsInt(2);
			for (int i = 1; i < length; i++) {
				List<Integer> next = layout.freeDirections(end);
				if (next.isEmpty()) break;
				end = layout.connect(end, next.get(nextInt.applyAsInt(next.size())));
			}
			layout.rooms.get(end).role = Role.TREASURY;
		}
		return layout;
	}

	private List<Integer> freeDirections(Cell from) {
		List<Integer> free = new ArrayList<>();
		for (int direction : DIRECTIONS) {
			Cell to = from.step(direction);
			if (to.z() <= 0 && to.z() >= -MAX_NORTH && Math.abs(to.x()) <= MAX_SIDEWAYS && !rooms.containsKey(to)) free.add(direction);
		}
		return free;
	}

	private Cell connect(Cell from, int direction) {
		Cell to = from.step(direction);
		rooms.get(from).doors |= direction;
		Room room = new Room();
		room.doors = opposite(direction);
		rooms.put(to, room);
		return to;
	}

	public static int opposite(int direction) {
		return rotate(direction, 2);
	}

	/** Turns a door mask clockwise by {@code quarters} quarter turns. */
	public static int rotate(int doors, int quarters) {
		int q = Math.floorMod(quarters, 4);
		return ((doors << q) | (doors >> (4 - q))) & 0xF;
	}

	public static boolean isStraight(int doors) {
		return doors == (NORTH | SOUTH) || doors == (EAST | WEST);
	}

	/** A room template with the doors it was built with, unrotated. */
	public record Piece<T>(T id, Role role, int doors) {
	}

	/** A template turned to fit a cell; {@code extraDoors} are its doors the cell does not use, to be walled up. */
	public record Fit<T>(T id, int quarters, int extraDoors) {
	}

	/**
	 * Picks a template and rotation for a room. Exact door matches of the room's role come first, then templates of
	 * that role with more doors than needed, then corridors the same way.
	 *
	 * @param nextInt returns a random int in {@code [0, bound)}
	 * @return null when no template fits at all
	 */
	public static <T> Fit<T> fit(List<Piece<T>> pieces, Role role, int doors, IntUnaryOperator nextInt) {
		for (Role wanted : role == Role.CORRIDOR ? List.of(Role.CORRIDOR) : List.of(role, Role.CORRIDOR)) {
			for (boolean exact : new boolean[] {true, false}) {
				List<Fit<T>> fits = new ArrayList<>();
				for (Piece<T> piece : pieces) {
					if (piece.role() != wanted) continue;
					for (int q = 0; q < 4; q++) {
						int turned = rotate(piece.doors(), q);
						if (exact ? turned == doors : (turned & doors) == doors) fits.add(new Fit<>(piece.id(), q, turned & ~doors));
					}
				}
				if (!fits.isEmpty()) return fits.get(nextInt.applyAsInt(fits.size()));
			}
		}
		return null;
	}
}
