package io.github.hellenter.eam.world.structures.crypt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/**
 * Pure layout of one crypt level on a grid of 15x15 cells. On the first level cell {@code (0, 0)} is the first room past
 * the entrance, which lies south of it; a level grows north, east and west, never south of row 0 where the entrance
 * stands, and stays within its {@link Bounds}. Every level but the last ends in stairs down; the level below starts at
 * their foot, in the same cell, and the last level ends in the boss room.
 * <p>
 * The main path of every level has at least {@link #MIN_FIGHTS} fight rooms between its start and its end. Rooms where
 * paths meet are fight rooms or big rooms, never corridors, and corridors seldom follow each other. Big rooms take 2x2
 * cells and can be entered and left by any of their eight doors.
 * <p>
 * Doors are a 4-bit mask: {@link #NORTH}, {@link #EAST}, {@link #SOUTH}, {@link #WEST}. Turning a mask clockwise by a
 * quarter moves every bit one place up, the same way {@code Rotation.CLOCKWISE_90} turns north into east. A big room has
 * an 8-bit mask, see {@link Big#doors}.
 */
public final class CryptLayout {
	public static final int NORTH = 1;
	public static final int EAST = 2;
	public static final int SOUTH = 4;
	public static final int WEST = 8;
	private static final int[] DIRECTIONS = {NORTH, EAST, SOUTH, WEST};

	/**
	 * How far a level may spread from the first room when nothing else is known, in cells: a structure only reaches 8
	 * chunks (128 blocks) from the chunk it starts in, and the first room is already one cell north of it.
	 */
	public static final Bounds DEFAULT_BOUNDS = new Bounds(-4, 4, -7, 0);

	/** Fight rooms each level's main path has at least, big rooms aside. */
	public static final int MIN_FIGHTS = 3;

	/** Chance in percent that the main path keeps going straight. */
	private static final int STRAIGHT_CHANCE = 60;
	/** Chance in percent that a room after a fight room or a big room is a fight room too. */
	private static final int FIGHT_CHANCE = 55;
	/** Chance in percent that a corridor follows a corridor; a third one in a row never does. */
	private static final int SECOND_CORRIDOR_CHANCE = 25;
	/** Chance in percent that the main path leaves a big room straight ahead when it can. */
	private static final int BIG_STRAIGHT_CHANCE = 60;
	/** How many layouts a level tries before the crypt starts over. */
	private static final int LEVEL_ATTEMPTS = 40;
	/** How many times the whole crypt starts over before it gives up. */
	private static final int CRYPT_ATTEMPTS = 10;

	public enum Role {
		CORRIDOR, FIGHT, TREASURY, STAIRS, BIG
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

	/** The cells a level may use, inclusive. */
	public record Bounds(int minX, int maxX, int minZ, int maxZ) {
		public boolean contains(Cell cell) {
			return cell.x() >= minX && cell.x() <= maxX && cell.z() >= minZ && cell.z() <= maxZ;
		}
	}

	public static final class Room {
		private Role role = Role.CORRIDOR;
		private int doors;

		public Role role() {
			return role;
		}

		/** The doors this cell uses; for a cell of a big room, only those on the big room's outside. */
		public int doors() {
			return doors;
		}
	}

	/** A 3x3-cell boss room around {@code middle}, with one door on its {@code door} side. */
	public record Boss(Cell middle, int door) {
		public boolean covers(Cell cell) {
			return Math.abs(cell.x() - middle.x()) <= 1 && Math.abs(cell.z() - middle.z()) <= 1;
		}
	}

	/** A 2x2-cell big room whose north-west cell is {@code corner}. */
	public record Big(Cell corner) {
		public boolean covers(Cell cell) {
			int dx = cell.x() - corner.x();
			int dz = cell.z() - corner.z();
			return dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
		}

		/** Its cells clockwise from the north-west one. */
		public List<Cell> cells() {
			Cell northEast = corner.step(EAST);
			return List.of(corner, northEast, northEast.step(SOUTH), corner.step(SOUTH));
		}

		/**
		 * The big room's doors as an 8-bit mask, clockwise from the west half of its north side: bit 0 north-west, 1
		 * north-east, 2 east-north, 3 east-south, 4 south-east, 5 south-west, 6 west-south, 7 west-north. Turning it
		 * clockwise by a quarter moves every bit two places up.
		 */
		public int doors(Map<Cell, Room> rooms) {
			List<Cell> cells = cells();
			int doors = 0;
			for (int door = 0; door < 8; door++) {
				Room room = rooms.get(cells.get((door + 1) / 2 % 4));
				if (room != null && (room.doors & bigDoorSide(door)) != 0) doors |= 1 << door;
			}
			return doors;
		}
	}

	/** The side of a big room the door with bit {@code door} of {@link Big#doors} is on. */
	public static int bigDoorSide(int door) {
		return DIRECTIONS[door / 2];
	}

	/** Whether the door with bit {@code door} of {@link Big#doors} is on the far half of its side, clockwise. */
	public static boolean bigDoorFarHalf(int door) {
		return door % 2 == 1;
	}

	private final Bounds bounds;
	private final Map<Cell, Room> rooms = new LinkedHashMap<>();
	private final List<Big> bigs = new ArrayList<>();
	private Cell up;
	private Cell down;
	private Boss boss;

	private CryptLayout(Bounds bounds) {
		this.bounds = bounds;
	}

	public Map<Cell, Room> rooms() {
		return rooms;
	}

	/** The foot of the stairs from the level above, or null on the first level. */
	public Cell up() {
		return up;
	}

	/** The stairs down to the next level, or null on the last level. */
	public Cell down() {
		return down;
	}

	/** The boss room, or null on all but the last level. */
	public Boss boss() {
		return boss;
	}

	public List<Big> bigs() {
		return bigs;
	}

	/**
	 * Lays out a whole crypt: {@code levels} levels, the last ending in the boss room, with {@code bigRooms} big rooms
	 * spread over different levels.
	 *
	 * @param nextInt  returns a random int in {@code [0, bound)}
	 * @param rooms    how many rooms the main path of a level tries to have
	 * @param branches how many side passages to a treasury a level tries to have
	 * @return the levels from the top, or null when no layout fits in the bounds
	 */
	public static List<CryptLayout> generate(IntUnaryOperator nextInt, Bounds bounds, int levels, int bigRooms,
			IntSupplier rooms, IntSupplier branches) {
		for (int attempt = 0; attempt < CRYPT_ATTEMPTS; attempt++) {
			int[] bigs = spread(bigRooms, levels, nextInt);
			List<CryptLayout> crypt = new ArrayList<>();
			for (int level = 0; level < levels; level++) {
				CryptLayout above = level == 0 ? null : crypt.getLast();
				CryptLayout layout = level(above, bounds, bigs[level], level == levels - 1, rooms.getAsInt(), branches.getAsInt(), nextInt);
				if (layout == null) break;
				crypt.add(layout);
			}
			if (crypt.size() == levels) return crypt;
		}
		return null;
	}

	/** How many big rooms each level gets: one each on random levels first, then a second one, and so on. */
	private static int[] spread(int bigRooms, int levels, IntUnaryOperator nextInt) {
		int[] bigs = new int[levels];
		List<Integer> order = new ArrayList<>();
		for (int level = 0; level < levels; level++) order.add(level);
		for (int level = levels - 1; level > 0; level--) {
			int other = nextInt.applyAsInt(level + 1);
			order.set(other, order.set(level, order.get(other)));
		}
		for (int made = 0; made < bigRooms; made++) bigs[order.get(made % levels)]++;
		return bigs;
	}

	/**
	 * Lays out one level under {@code above}, or the first one when it is null. The last level ends in the boss room,
	 * which takes 3x3 cells and must not lie under a room of the level above: it is taller than one level.
	 */
	private static CryptLayout level(CryptLayout above, Bounds bounds, int bigs, boolean last, int length, int branches,
			IntUnaryOperator nextInt) {
		for (int attempt = 0; attempt < LEVEL_ATTEMPTS; attempt++) {
			CryptLayout layout = new CryptLayout(bounds);
			Room first = new Room();
			Cell start;
			if (above == null) {
				start = new Cell(0, 0);
				first.doors = SOUTH;
			} else {
				start = above.down;
				first.role = Role.STAIRS;
				layout.up = start;
			}
			layout.rooms.put(start, first);
			List<Cell> path = layout.walk(start, length, bigs, nextInt);
			if (layout.bigs.size() < bigs) continue;
			int end;
			if (last) {
				if (!layout.placeBoss(path, above == null ? Set.of() : above.rooms.keySet(), nextInt)) continue;
				end = path.size();
			} else {
				if (path.size() < 2) continue;
				layout.down = path.getLast();
				layout.rooms.get(layout.down).role = Role.STAIRS;
				end = path.size() - 1;
			}
			// The first level's first room is a room of the path; a lower level's is the foot of the stairs.
			int from = above == null ? 0 : 1;
			layout.addBranches(path, from, end, branches, nextInt);
			if (layout.addRoles(path, from, end, nextInt)) return layout;
		}
		return null;
	}

	/**
	 * A random walk of up to {@code length} rooms from {@code start}, which leans towards going straight and passes
	 * through {@code bigs} big rooms on the way.
	 */
	private List<Cell> walk(Cell start, int length, int bigs, IntUnaryOperator nextInt) {
		List<Cell> path = new ArrayList<>(List.of(start));
		int direction = NORTH;
		int nextBig = bigs > 0 ? 1 + nextInt.applyAsInt(Math.max(1, length - 4)) : Integer.MAX_VALUE;
		while (path.size() < length || this.bigs.size() < bigs) {
			if (path.size() > length + 6) break;
			if (this.bigs.size() < bigs && path.size() >= nextBig) {
				int out = addBig(path, direction, nextInt);
				if (out != 0) {
					direction = out;
					nextBig = path.size() + 2 + nextInt.applyAsInt(3);
					continue;
				}
			}
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
	 * Opens a big room off the last room of the path and leaves it by another of its doors into a new room, adding
	 * the cells the path goes through.
	 *
	 * @return the direction the path leaves the big room in, or 0 when no big room fits here
	 */
	private int addBig(List<Cell> path, int forward, IntUnaryOperator nextInt) {
		Cell from = path.getLast();
		List<Integer> free = freeDirections(from);
		if (free.isEmpty()) return 0;
		List<Integer> order = new ArrayList<>();
		if (free.contains(forward)) order.add(forward);
		int first = nextInt.applyAsInt(free.size());
		for (int k = 0; k < free.size(); k++) {
			int direction = free.get((first + k) % free.size());
			if (direction != forward) order.add(direction);
		}
		for (int direction : order) {
			Cell entry = from.step(direction);
			int firstSide = nextInt.applyAsInt(2);
			for (int k = 0; k < 2; k++) {
				int side = rotate(direction, (firstSide + k) % 2 == 0 ? 1 : 3);
				Cell far = entry.step(direction);
				List<Cell> cells = List.of(entry, entry.step(side), far, far.step(side));
				if (!cells.stream().allMatch(this::isFree)) continue;
				Big big = new Big(new Cell(cells.stream().mapToInt(Cell::x).min().orElseThrow(),
						cells.stream().mapToInt(Cell::z).min().orElseThrow()));
				List<Cell> exitCells = new ArrayList<>();
				List<Integer> exitDirections = new ArrayList<>();
				List<Integer> straight = new ArrayList<>();
				for (Cell cell : cells) {
					for (int out : DIRECTIONS) {
						Cell to = cell.step(out);
						if (big.covers(to) || !isFree(to) || cell.equals(entry) && out == opposite(direction)) continue;
						if (out == direction) straight.add(exitCells.size());
						exitCells.add(cell);
						exitDirections.add(out);
					}
				}
				if (exitCells.isEmpty()) continue;
				int exit = !straight.isEmpty() && nextInt.applyAsInt(100) < BIG_STRAIGHT_CHANCE
						? straight.get(nextInt.applyAsInt(straight.size()))
						: nextInt.applyAsInt(exitCells.size());
				for (Cell cell : cells) {
					Room room = new Room();
					room.role = Role.BIG;
					rooms.put(cell, room);
				}
				bigs.add(big);
				rooms.get(from).doors |= direction;
				rooms.get(entry).doors |= opposite(direction);
				path.add(entry);
				Cell out = exitCells.get(exit);
				if (!out.equals(entry)) path.add(out);
				path.add(connect(out, exitDirections.get(exit)));
				return exitDirections.get(exit);
			}
		}
		return 0;
	}

	/**
	 * Opens the boss room off the latest room of the path that has room for it, dropping the rooms after that one.
	 * Rooms up to the last big room stay.
	 *
	 * @return whether it fits anywhere
	 */
	private boolean placeBoss(List<Cell> path, Set<Cell> above, IntUnaryOperator nextInt) {
		int lastBig = -1;
		for (int i = 0; i < path.size(); i++) {
			if (rooms.get(path.get(i)).role == Role.BIG) lastBig = i;
		}
		for (int i = path.size() - 1; i > lastBig && i >= Math.min(1, path.size() - 1); i--) {
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
				if (!bounds.contains(cell) || rooms.containsKey(cell) || above.contains(cell)) return false;
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

	/**
	 * Side passages of one or two rooms ending in a treasury, off rooms of the path from {@code from} to {@code end},
	 * exclusive. Big rooms are likelier forks: they have more doors to spare.
	 */
	private void addBranches(List<Cell> path, int from, int end, int branches, IntUnaryOperator nextInt) {
		List<Cell> forks = new ArrayList<>();
		for (int i = from; i < end; i++) {
			Cell cell = path.get(i);
			forks.add(cell);
			if (rooms.get(cell).role == Role.BIG) forks.add(cell);
		}
		// The other two cells of each big room can be forks too.
		for (Big big : bigs) forks.addAll(big.cells().stream().filter(cell -> !path.contains(cell)).toList());
		for (int made = 0; made < branches && !forks.isEmpty(); ) {
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
			made++;
		}
	}

	/**
	 * Makes the rooms of the path from {@code from} to {@code end}, exclusive, fight rooms or corridors: rooms where
	 * paths meet always fight rooms, never three fights in a row otherwise, and seldom two corridors in a row.
	 *
	 * @return whether the path has at least {@link #MIN_FIGHTS} fight rooms
	 */
	private boolean addRoles(List<Cell> path, int from, int end, IntUnaryOperator nextInt) {
		List<Room> corridors = new ArrayList<>();
		int fights = 0;
		int fightsInRow = 0;
		int corridorsInRow = 0;
		for (int i = from; i < end; i++) {
			Room room = rooms.get(path.get(i));
			if (room.role == Role.BIG) {
				// A big room is a fight of its own: give the players a breather after it.
				fightsInRow = 2;
				corridorsInRow = 0;
				continue;
			}
			boolean fight;
			if (Integer.bitCount(room.doors) >= 3) fight = true;
			else if (fightsInRow >= 2) fight = false;
			else if (corridorsInRow >= 2) fight = true;
			else if (corridorsInRow == 1) fight = nextInt.applyAsInt(100) >= SECOND_CORRIDOR_CHANCE;
			else fight = nextInt.applyAsInt(100) < FIGHT_CHANCE;
			room.role = fight ? Role.FIGHT : Role.CORRIDOR;
			if (fight) {
				fights++;
				fightsInRow++;
				corridorsInRow = 0;
			} else {
				corridors.add(room);
				corridorsInRow++;
				fightsInRow = 0;
			}
		}
		while (fights < MIN_FIGHTS && !corridors.isEmpty()) {
			corridors.remove(nextInt.applyAsInt(corridors.size())).role = Role.FIGHT;
			fights++;
		}
		return fights >= MIN_FIGHTS;
	}

	private boolean isFree(Cell cell) {
		return bounds.contains(cell) && !rooms.containsKey(cell) && (boss == null || !boss.covers(cell));
	}

	private List<Integer> freeDirections(Cell from) {
		List<Integer> free = new ArrayList<>();
		for (int direction : DIRECTIONS) {
			if (isFree(from.step(direction))) free.add(direction);
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

	/** Turns a big room's 8-bit door mask clockwise by {@code quarters} quarter turns. */
	public static int rotateBig(int doors, int quarters) {
		int shift = 2 * Math.floorMod(quarters, 4);
		return ((doors << shift) | (doors >> (8 - shift))) & 0xFF;
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
	 * that role with more doors than needed, then corridors the same way. Big rooms use 8-bit masks and only big room
	 * templates.
	 *
	 * @param nextInt returns a random int in {@code [0, bound)}
	 * @return null when no template fits at all
	 */
	public static <T> Fit<T> fit(List<Piece<T>> pieces, Role role, int doors, IntUnaryOperator nextInt) {
		return fit(pieces, role, doors, id -> 1, nextInt);
	}

	/**
	 * Like {@link #fit(List, Role, int, IntUnaryOperator)}, but each fitting template is picked in proportion to its
	 * {@code weight}.
	 */
	public static <T> Fit<T> fit(List<Piece<T>> pieces, Role role, int doors, ToIntFunction<T> weight, IntUnaryOperator nextInt) {
		List<Role> roles = role == Role.CORRIDOR || role == Role.BIG ? List.of(role) : List.of(role, Role.CORRIDOR);
		for (Role wanted : roles) {
			for (boolean exact : new boolean[] {true, false}) {
				List<Fit<T>> fits = new ArrayList<>();
				for (Piece<T> piece : pieces) {
					if (piece.role() != wanted) continue;
					for (int q = 0; q < 4; q++) {
						int turned = wanted == Role.BIG ? rotateBig(piece.doors(), q) : rotate(piece.doors(), q);
						if (exact ? turned == doors : (turned & doors) == doors) fits.add(new Fit<>(piece.id(), q, turned & ~doors));
					}
				}
				if (fits.isEmpty()) continue;
				int total = fits.stream().mapToInt(fit -> weight.applyAsInt(fit.id())).sum();
				int pick = nextInt.applyAsInt(total);
				for (Fit<T> fit : fits) {
					pick -= weight.applyAsInt(fit.id());
					if (pick < 0) return fit;
				}
			}
		}
		return null;
	}
}
