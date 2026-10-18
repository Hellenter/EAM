package io.github.hellenter.eam.world.structures.village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * The duergar village pieces around a feature, so that a feature that reaches into the neighbouring chunks (a lake, a
 * stream) can stay out of them. A village is built before the decorations of its own chunk, but a neighbour decorated
 * later would otherwise dig its lake right through a house.
 */
public final class VillageArea {
	private final List<BoundingBox> boxes;
	private final int margin;

	private VillageArea(List<BoundingBox> boxes, int margin) {
		this.boxes = boxes;
		this.margin = margin;
	}

	/** Every village piece referenced by the chunk of {@code origin} or the eight around it, grown by {@code margin}. */
	public static VillageArea around(WorldGenLevel level, BlockPos origin, int margin) {
		return around(level, origin, margin, DuergarVillage.VILLAGES);
	}

	/**
	 * Every piece of a structure in {@code tag} referenced by the chunk of {@code origin} or the eight around it,
	 * grown by {@code margin}, which may be up to 16.
	 */
	public static VillageArea around(WorldGenLevel level, BlockPos origin, int margin, TagKey<Structure> tag) {
		if (!(level instanceof WorldGenRegion region)) {
			return new VillageArea(List.of(), margin);
		}
		StructureManager structures = region.getLevel().structureManager().forWorldGenRegion(region);
		Registry<Structure> registry = region.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		Set<StructureStart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		List<BoundingBox> boxes = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos chunk = origin.offset(dx * 16, 0, dz * 16);
				for (Map.Entry<Structure, LongSet> entry : structures.getAllStructuresAt(chunk).entrySet()) {
					if (!registry.wrapAsHolder(entry.getKey()).is(tag)) {
						continue;
					}
					structures.fillStartsForStructure(entry.getKey(), entry.getValue(), start -> {
						if (seen.add(start)) {
							for (StructurePiece piece : start.getPieces()) {
								boxes.add(piece.getBoundingBox().inflatedBy(margin));
							}
						}
					});
				}
			}
		}
		return new VillageArea(boxes, margin);
	}

	public boolean isEmpty() {
		return boxes.isEmpty();
	}

	/** Whether {@code pos} lies in or next to a village piece. */
	public boolean touches(BlockPos pos) {
		for (BoundingBox box : boxes) {
			if (box.isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether water let out at {@code pos} could run down onto a village piece: it is beside or over one, within the
	 * margin, and at least {@code rise} blocks above its floor.
	 */
	public boolean overlooks(BlockPos pos, int rise) {
		for (BoundingBox box : boxes) {
			if (box.minX() <= pos.getX() && pos.getX() <= box.maxX() && box.minZ() <= pos.getZ() && pos.getZ() <= box.maxZ()
					&& pos.getY() >= box.minY() + margin + rise) {
				return true;
			}
		}
		return false;
	}
}
