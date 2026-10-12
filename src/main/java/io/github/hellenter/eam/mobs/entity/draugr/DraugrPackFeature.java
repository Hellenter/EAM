package io.github.hellenter.eam.mobs.entity.draugr;

import io.github.hellenter.eam.content.EamEntities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A pack of draugr frozen into a cave floor around the origin (a block of air just above the floor): each stands in a
 * column of ice or snow {@link Draugr#DEPTH} blocks deep with his head just under the surface, a few of them waiting,
 * the rest asleep. Now and then the top of a column is clear ice and a face looks up through it.
 *
 * @param count          how many draugr in the pack
 * @param waiting        how many of them wait
 * @param radius         how far from the origin they lie; at most 23, so the pack stays within the blocks a feature may
 *                       write to
 * @param clearIceChance chance the top of a column is clear ice
 */
public record DraugrPackFeature(IntProvider count, IntProvider waiting, int radius, float clearIceChance) implements Feature {
	public static final MapCodec<DraugrPackFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(1, 64).fieldOf("count").forGetter(DraugrPackFeature::count),
			IntProviders.codec(0, 16).fieldOf("waiting").forGetter(DraugrPackFeature::waiting),
			Codec.intRange(1, 23).fieldOf("radius").forGetter(DraugrPackFeature::radius),
			Codec.floatRange(0.0F, 1.0F).fieldOf("clear_ice_chance").forGetter(DraugrPackFeature::clearIceChance)
	).apply(i, DraugrPackFeature::new));

	/** How far above and below the origin a draugr's spot on the floor is looked for. */
	private static final int SCAN_UP = 6;
	private static final int SCAN_DOWN = 10;
	/** Columns stand at least this far apart. */
	private static final int SPACING = 2;

	@Override
	public MapCodec<DraugrPackFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int count = this.count.sample(random);
		int waiting = Math.min(count, this.waiting.sample(random));
		// A feature may write within one chunk around its own.
		int chunkX = origin.getX() & ~15;
		int chunkZ = origin.getZ() & ~15;
		List<BlockPos> tombs = new ArrayList<>();
		for (int attempt = 0; attempt < count * 8 && tombs.size() < count; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = this.radius * Math.sqrt(random.nextDouble());
			int x = origin.getX() + Mth.floor(Math.cos(angle) * distance + 0.5);
			int z = origin.getZ() + Mth.floor(Math.sin(angle) * distance + 0.5);
			if (x < chunkX - 15 || x > chunkX + 30 || z < chunkZ - 15 || z > chunkZ + 30 || crowded(tombs, x, z)) {
				continue;
			}
			BlockPos top = floor(level, x, origin.getY(), z);
			if (top != null) {
				tombs.add(top);
			}
		}
		for (int n = 0; n < tombs.size(); n++) {
			bury(level, random, tombs.get(n), n < waiting ? DraugrState.WAITING : DraugrState.SLEEPING);
		}
		return !tombs.isEmpty();
	}

	private static boolean crowded(List<BlockPos> tombs, int x, int z) {
		for (BlockPos tomb : tombs) {
			int dx = tomb.getX() - x;
			int dz = tomb.getZ() - z;
			if (dx * dx + dz * dz < SPACING * SPACING) {
				return true;
			}
		}
		return false;
	}

	/** The floor's top block at {@code x, z} near {@code y}, if it is ice or snow with solid ground under it and room above. */
	private static @Nullable BlockPos floor(WorldGenLevel level, int x, int y, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int top = y + SCAN_UP; top >= y - SCAN_DOWN; top--) {
			pos.set(x, top, z);
			if (!level.getBlockState(pos).is(DraugrModule.BURIED_IN)) {
				continue;
			}
			if (!level.getBlockState(pos.setY(top + 1)).isAir() || !level.getBlockState(pos.setY(top + 2)).isAir()) {
				continue;
			}
			boolean grounded = true;
			for (int dy = 1; dy < Draugr.DEPTH + 1 && grounded; dy++) {
				grounded = level.getBlockState(pos.setY(top - dy)).isSolid();
			}
			if (grounded) {
				return new BlockPos(x, top, z);
			}
		}
		return null;
	}

	/** Freezes a draugr into the column under {@code top}: ice under ice, snow under snow. */
	private void bury(WorldGenLevel level, RandomSource random, BlockPos top, DraugrState state) {
		boolean snow = level.getBlockState(top).is(Blocks.SNOW_BLOCK) || level.getBlockState(top).is(Blocks.POWDER_SNOW);
		BlockState below = snow ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
		BlockState surface = random.nextFloat() < this.clearIceChance ? Blocks.ICE.defaultBlockState() : below;
		for (int dy = 1; dy < Draugr.DEPTH; dy++) {
			level.setBlock(top.below(dy), below, 2);
		}
		level.setBlock(top, surface, 2);
		Draugr draugr = EamEntities.DRAUGR.create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
		if (draugr == null) {
			return;
		}
		float yaw = random.nextFloat() * 360.0F;
		draugr.snapTo(top.getX() + 0.5, top.getY() - Draugr.FEET_UNDER_TOP, top.getZ() + 0.5, yaw, 0.0F);
		draugr.setYBodyRot(yaw);
		draugr.setYHeadRot(yaw);
		draugr.finalizeSpawn(level, level.getCurrentDifficultyAt(top), EntitySpawnReason.CHUNK_GENERATION, null);
		draugr.setState(state);
		draugr.setPersistenceRequired();
		level.addFreshEntityWithPassengers(draugr);
	}
}
