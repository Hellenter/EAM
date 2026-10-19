package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.villager.StructureTemplateAccessor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The blueprint templates the server read, by template id. A jigsaw block becomes the block it turns into when the
 * structure generates; the building entrance's jigsaw, which joins a village house to its street, gives the side the
 * building faces. A template with none, such as our defences, faces north: their outer side. Emptied when datapacks
 * reload.
 */
public final class BlueprintTemplates {
	private static final String ENTRANCE = "minecraft:building_entrance";
	private static final Map<Identifier, Optional<BlueprintTemplate>> CACHE = new ConcurrentHashMap<>();

	private BlueprintTemplates() {
	}

	static void clear() {
		CACHE.clear();
	}

	/** The template, or {@code null} if there is none of that id. */
	public static @Nullable BlueprintTemplate get(ServerLevel level, Identifier id) {
		return CACHE.computeIfAbsent(id, key -> load(level, key)).orElse(null);
	}

	private static Optional<BlueprintTemplate> load(ServerLevel level, Identifier id) {
		Optional<StructureTemplate> template = level.getStructureTemplateManager().get(id);
		if (template.isEmpty() || ((StructureTemplateAccessor) template.get()).eam$palettes().isEmpty()) {
			Eam.LOGGER.warn("No blueprint template {}", id);
			return Optional.empty();
		}
		StructureTemplate.Palette palette = ((StructureTemplateAccessor) template.get()).eam$palettes().getFirst();
		List<BlueprintTemplate.Entry> blocks = new ArrayList<>();
		Direction front = Direction.NORTH;
		for (StructureTemplate.StructureBlockInfo info : palette.blocks()) {
			BlockState state = info.state();
			if (state.is(Blocks.JIGSAW)) {
				if (entrance(info)) front = JigsawBlock.getFrontFacing(state);
				state = finalState(level, info);
			}
			if (state.is(Blocks.STRUCTURE_VOID)) continue;
			blocks.add(new BlueprintTemplate.Entry(info.pos().getX(), info.pos().getY(), info.pos().getZ(), state));
		}
		var size = template.get().getSize();
		return Optional.of(new BlueprintTemplate(id, size.getX(), size.getY(), size.getZ(), List.copyOf(blocks), front));
	}

	/** Whether the jigsaw joins the building to its street, facing sideways. */
	private static boolean entrance(StructureTemplate.StructureBlockInfo info) {
		return info.nbt() != null && ENTRANCE.equals(info.nbt().getStringOr("name", ""))
				&& JigsawBlock.getFrontFacing(info.state()).getAxis().isHorizontal();
	}

	private static BlockState finalState(ServerLevel level, StructureTemplate.StructureBlockInfo info) {
		String name = info.nbt() == null ? "minecraft:air" : info.nbt().getStringOr("final_state", "minecraft:air");
		try {
			return BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), name, false).blockState();
		} catch (CommandSyntaxException e) {
			return Blocks.AIR.defaultBlockState();
		}
	}
}
