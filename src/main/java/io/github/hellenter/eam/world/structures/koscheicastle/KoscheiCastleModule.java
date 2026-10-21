package io.github.hellenter.eam.world.structures.koscheicastle;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.dimension.UnderworldDimension;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * Koschei's castle from Maxim's template {@code eam:koschei_castle}: a tuff keep on a stepped plinth on the floor of the
 * underworld's ice caves, in the middle of each patch of them, with Koschei sealed in an ice crystal on its second floor.
 * Found with {@code /locate structure eam:koschei_castle} in the underworld.
 */
public final class KoscheiCastleModule {
	public static final Identifier TEMPLATE = Eam.id("koschei_castle");
	/** Where Koschei stands in the template: in the crystal on the second floor, his feet on the floor. */
	public static final BlockPos CRYSTAL = new BlockPos(28, 27, 28);

	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Eam.id("koschei_castle"));
	public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, Eam.id("koschei_castles"));
	/** The biomes whose patches get a castle in their middle: the ice caves, the great hall of ice spires. */
	public static final TagKey<Biome> HAS_CASTLE = TagKey.create(Registries.BIOME, Eam.id("has_structure/koschei_castle"));

	public static final StructureType<KoscheiCastleStructure> CASTLE =
			Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Eam.id("koschei_castle"), () -> KoscheiCastleStructure.CODEC);
	public static final StructurePieceType PIECE =
			Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Eam.id("koschei_castle"), KoscheiCastlePiece::new);

	static {
		Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, Eam.id("biome_patch_middle"), KoscheiCastlePlacement.CODEC);
	}

	private KoscheiCastleModule() {
	}

	/** Loads the structure, piece and placement types, and remembers each world's underworld biomes for {@code /locate}. */
	public static void register() {
		ServerLevelEvents.LOAD.register((server, level) -> {
			if (level.dimension() == UnderworldDimension.LEVEL) {
				KoscheiCastlePlacement.remember(level.getSeed(), level.getChunkSource().getGenerator().getBiomeSource(),
						level.getChunkSource().randomState());
			}
		});
	}
}
