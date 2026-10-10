package io.github.hellenter.eam.world.structures.crypt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Pure layout of one crypt level on a grid of 15x15 cells. Cell {@code (0, 0)} is the first room past the entrance,
 * which lies south of it; a level grows north, east and west, never south of row 0 where the entrance stands, and
 * stays within {@link #MAX_SIDEWAYS} and {@link #MAX_NORTH} cells of the first room. The first level ends in stairs
 * down; the level below starts at their foot, in the same cell, and ends in the boss room.
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
	/** How many layouts a lower level tries before it gives up on fitting the boss room. */
	private static final int BOSS_ATTEMPTS = 20;

	public enum Role {
		CORRIDOR, FIGHT, TREASURY, STAIRS
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
	private Cell stairs;
	private Boss boss;

	private CryptLayout() {
	}

	public Map<Cell, Room> rooms() {
		return rooms;
	}

	/**
	 * Lays out the first level: a main path of up to {@code mainLength} rooms from the entrance that ends in the stairs
	 * down, fight rooms on its straight stretches (never three in a row), and up to {@code branches} side passages that
	 * end in a treasury.
	 *
	 * @param nextInt returns a random int in {@code [0, bound)}
	 */
	public static CryptLayout generate(IntUnaryOperator nextInt, int mainLength, int branches) {
		CryptLayout layout = new CryptLayout();
		Cell start = new Cell(0, 0);
		layout.rooms.put(start, new Room());
		layout.rooms.get(start).doors = SOUTH;
		List<Cell> path = layout.walk(start, mainLength, nextInt);
		layout.stairs = path.getLast();
		layout.rooms.get(layout.stairs).role = Role.STAIRS;
		layout.addFights(path, path.size() - 1, nextInt);
		layout.addBranches(path, path.size() - 1, branches, nextInt);
		return layout;
	}

	/**
	 * Lays out the level under {@code above}, starting from the foot of its stairs and ending in the boss room, which
	 * takes 3x3 cells and must not lie under a room of the level above: it is taller than one level. When the boss room
	 * fits nowhere after a few tries, the main path ends in a treasury instead.
	 *
	 * @param nextInt returns a random int in {@code [0, bound)}
	 */
	public static CryptLayout generateBelow(CryptLayout above, IntUnaryOperator nextInt, int mainLength, int branches) {
		CryptLayout layout = null;
		for (int attempt = 0; attempt < BOSS_ATTEMPTS; attempt++) {
			layout = new CryptLayout();
			layout.stairs = above.stairs;
			layout.rooms.put(layout.stairs, new Room());
			layout.rooms.get(layout.stairs).role = Role.STAIRS;
			List<Cell> path = layout.walk(layout.stairs, mainLength, nextInt);
			if (layout.placeBoss(path, above.rooms.keySet(), nextInt) || attempt == BOSS_ATTEMPTS - 1) {
				if (layout.boss == null && path.size() > 1) layout.rooms.get(path.getLast()).role = Role.TREASURY;
				int end = layout.boss == null ? path.size() - 1 : path.size();
				layout.addFights(path, end, nextInt);
				layout.addBranches(path, end, branches, nextInt);
				break;
			}
		}
		return layout;
	}

	/** The cell with the stairs between this level and the one below it, or above it for a lower level. */
	public Cell stairs() {
		return stairs;
	}

	/** The boss room, or null. */
	public Boss boss() {
		return boss;
	}

	/** A 3x3-cell boss room around {@code middle}, with one door on its {@code door} side. */
	public record Boss(Cell middle, int door) {
		public boolean covers(Cell cell) {
			return Math.abs(cell.x() - middle.x()) <= 1 && Math.abs(cell.z() - middle.z()) <= 1;
		}
	}

	/** A random walk of up to {@code length} rooms from {@code start}, which leans towards going straight. */
	private List<Cell> walk(Cell start, int length, IntUnaryOperator nextInt) {
		List<Cell> path = new ArrayList<>(List.of(start));
		int direction = NORTH;
		while (path.size() < length) {
			Cell from = path.getLast();
			List<Integer> free = freeDirections(from);
			if (free.isEmpty()) break;
			if (!free.contains(direction) || nextInt.applyAsInt(100) >= STRAIGHT_CHANCE) {
				direction = free.get(nextInt.applyAsInt(free.size()));
			}
			path.add(connect(from, direction));
		}
		return path;
	}

	/**
	 * Opens the boss room off the latest room of the path that has room for it, dropping the rooms after that one.
	 *
	 * @return whether it fits anywhere
	 */
	private boolean placeBoss(List<Cell> path, Set<Cell> above, IntUnaryOperator nextInt) {
		for (int i = path.size() - 1; i >= Math.min(1, path.size() - 1); i--) {
			Cell from = path.get(i);
			List<Integer> free = freeDirections(from);
			int first = free.isEmpty() ? 0 : nextInt.applyAsInt(free.size());
			for (int k = 0; k < free.size(); k++) {
				int direction = free.get((first + k) % free.size());
				Boss candidate = new Boss(from.step(direction).step(direction), opposite(direction));
				if (!fitsBoss(candidate, above)) continue;
				if (i < path.size() - 1) {
					rooms.get(from).doors &= ~directionTo(from, path.get(i + 1));
					for (Cell dropped : path.subList(i + 1, path.size())) rooms.remove(dropped);
					path.subList(i + 1, path.size()).clear();
				}
				rooms.get(from).doors |= direction;
				boss = candidate;
				return true;
			}
		}
		return false;
	}

	private boolean fitsBoss(Boss candidate, Set<Cell> above) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				Cell cell = new Cell(candidate.middle().x() + dx, candidate.middle().z() + dz);
				if (!inReach(cell) || rooms.containsKey(cell) || above.contains(cell)) return false;
			}
		}
		return true;
	}

	private static int directionTo(Cell from, Cell to) {
		for (int direction : DIRECTIONS) {
			if (from.step(direction).equals(to)) return direction;
		}
		throw new IllegalArgumentException(from + " is not next to " + to);
	}

	/** Fight rooms on the straight cells of the path between its first room and {@code end}, exclusive. */
	private void addFights(List<Cell> path, int end, IntUnaryOperator nextInt) {
		int fightsInRow = 0;
		Cell firstStraight = null;
		boolean anyFight = false;
		for (int i = 1; i < end; i++) {
			Room room = rooms.get(path.get(i));
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
		if (!anyFight && firstStraight != null) rooms.get(firstStraight).role = Role.FIGHT;
	}

	/** Side passages of one or two rooms ending in a treasury, off straight corridors of the path before {@code end}. */
	private void addBranches(List<Cell> path, int end, int branches, IntUnaryOperator nextInt) {
		List<Cell> forks = new ArrayList<>();
		for (int i = 1; i < end; i++) {
			Room room = rooms.get(path.get(i));
			if (room.role == Role.CORRIDOR && isStraight(room.doors)) forks.add(path.get(i));
		}
		for (int made = 0; made < branches && !forks.isEmpty(); made++) {
			Cell fork = forks.remove(nextInt.applyAsInt(forks.size()));
			List<Integer> free = freeDirections(fork);
			if (free.isEmpty()) continue;
			Cell last = connect(fork, free.get(nextInt.applyAsInt(free.size())));
			int length = 1 + nextInt.applyAsInt(2);
			for (int i = 1; i < length; i++) {
				List<Integer> next = freeDirections(last);
				if (next.isEmpty()) break;
				last = connect(last, next.get(nextInt.applyAsInt(next.size())));
			}
			rooms.get(last).role = Role.TREASURY;
		}
	}

	private static boolean inReach(Cell cell) {
		return cell.z() <= 0 && cell.z() >= -MAX_NORTH && Math.abs(cell.x()) <= MAX_SIDEWAYS;
	}

	private List<Integer> freeDirections(Cell from) {
		List<Integer> free = new ArrayList<>();
		for (int direction : DIRECTIONS) {
			Cell to = from.step(direction);
			if (inReach(to) && !rooms.containsKey(to) && (boss == null || !boss.covers(to))) free.add(direction);
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
