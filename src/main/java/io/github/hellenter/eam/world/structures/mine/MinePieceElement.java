package io.github.hellenter.eam.world.structures.mine;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A piece of an underworld mine once the mine is laid out ({@link UnderworldMineStructure}): the template it was picked
 * as, now maybe part of a caved-in or flooded wing ({@link MineWingProcessor}), and with a chance of a chest minecart
 * left standing on one of its rails.
 */
public class MinePieceElement extends SinglePoolElement {
	public static final MapCodec<MinePieceElement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			templateCodec(),
			processorsCodec(),
			projectionCodec(),
			overrideLiquidSettingsCodec(),
			MineWingProcessor.CODEC.codec().optionalFieldOf("wing").forGetter(element -> element.wing),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("minecart_chance", 0.0F).forGetter(element -> element.minecartChance),
			Codec.LONG.fieldOf("seed").forGetter(element -> element.seed)
	).apply(instance, MinePieceElement::new));

	private final Optional<MineWingProcessor> wing;
	private final float minecartChance;
	private final long seed;

	protected MinePieceElement(Either<Identifier, StructureTemplate> template, Holder<StructureProcessorList> processors,
			StructureTemplatePool.Projection projection, Optional<LiquidSettings> overrideLiquidSettings,
			Optional<MineWingProcessor> wing, float minecartChance, long seed) {
		super(template, processors, projection, overrideLiquidSettings);
		this.wing = wing;
		this.minecartChance = minecartChance;
		this.seed = seed;
	}

	public MinePieceElement(Identifier template, Holder<StructureProcessorList> processors, StructureTemplatePool.Projection projection,
			Optional<MineWingProcessor> wing, float minecartChance, long seed) {
		this(Either.left(template), processors, projection, Optional.of(LiquidSettings.IGNORE_WATERLOGGING), wing, minecartChance, seed);
	}

	@Override
	protected StructurePlaceSettings getSettings(Rotation rotation, BoundingBox chunkBB, LiquidSettings liquidSettings, boolean keepJigsaws) {
		StructurePlaceSettings settings = super.getSettings(rotation, chunkBB, liquidSettings, keepJigsaws);
		wing.ifPresent(settings::addProcessor);
		return settings;
	}

	@Override
	public boolean place(StructureTemplateManager structureTemplateManager, WorldGenLevel level, StructureManager structureManager,
			ChunkGenerator generator, BlockPos position, BlockPos referencePos, Rotation rotation, BoundingBox chunkBB,
			RandomSource random, LiquidSettings liquidSettings, boolean keepJigsaws) {
		if (!super.place(structureTemplateManager, level, structureManager, generator, position, referencePos, rotation, chunkBB,
				random, liquidSettings, keepJigsaws)) {
			return false;
		}
		placeMinecart(structureTemplateManager, level, position, rotation, chunkBB);
		return true;
	}

	/**
	 * Maybe a chest minecart on one of the piece's rails. The rail is picked the same way in every chunk the piece
	 * crosses, and the cart is only left where that rail survived the mine's decay, the rubble and the water.
	 */
	private void placeMinecart(StructureTemplateManager structureTemplateManager, WorldGenLevel level, BlockPos position,
			Rotation rotation, BoundingBox chunkBB) {
		RandomSource random = RandomSource.create(seed ^ position.asLong());
		if (minecartChance <= 0.0F || random.nextFloat() >= minecartChance) {
			return;
		}
		StructureTemplate template = this.template.map(structureTemplateManager::getOrCreate, t -> t);
		List<StructureTemplate.StructureBlockInfo> rails = template.filterBlocks(position,
				new StructurePlaceSettings().setRotation(rotation), Blocks.RAIL);
		if (rails.isEmpty()) {
			return;
		}
		BlockPos at = rails.get(random.nextInt(rails.size())).pos();
		if (!chunkBB.isInside(at) || !level.getBlockState(at).is(BlockTags.RAILS)) {
			return;
		}
		MinecartChest cart = EntityTypes.CHEST_MINECART.create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
		if (cart != null) {
			cart.setInitialPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
			cart.setLootTable(UnderworldMine.MINECART_LOOT, random.nextLong());
			level.addFreshEntity(cart);
		}
	}

	@Override
	public StructurePoolElementType<?> getType() {
		return UnderworldMineModule.PIECE;
	}

	@Override
	public String toString() {
		return "MinePiece[" + template + ", " + wing + "]";
	}
}
