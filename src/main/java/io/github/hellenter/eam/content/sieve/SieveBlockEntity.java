package io.github.hellenter.eam.content.sieve;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** How many sifts the sieve has done (its damage) and how far the current sift is. Gravel on it is {@link SieveBlock#GRAVEL}. */
public class SieveBlockEntity extends BlockEntity {
	/** Ticks between the scraping sounds of a sift. */
	private static final int SOUND_INTERVAL = 10;

	private int damage;
	private int progress;

	public SieveBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.SIEVE, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SieveBlockEntity sieve) {
		ServerLevel serverLevel = (ServerLevel) level;
		SieveSettings settings = SieveModule.settings();
		sieve.progress++;
		sieve.setChanged();
		if (sieve.progress % SOUND_INTERVAL == 0) {
			level.playSound(null, pos, SoundEvents.GRAVEL_HIT, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.4F);
		}
		if (!SieveLogic.done(sieve.progress, settings.siftTicks())) {
			int gravel = SieveLogic.gravelLeft(sieve.progress, settings.siftTicks());
			if (gravel != state.getValue(SieveBlock.GRAVEL)) {
				level.setBlock(pos, state.setValue(SieveBlock.GRAVEL, gravel), Block.UPDATE_CLIENTS);
			}
			return;
		}

		sieve.progress = 0;
		sieve.damage++;
		LootTable loot = serverLevel.getServer().reloadableRegistries().getLootTable(SieveModule.LOOT);
		LootParams params = new LootParams.Builder(serverLevel)
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
				.create(LootContextParamSets.CHEST);
		loot.getRandomItems(params, stack -> Block.popResourceFromFace(level, pos, Direction.UP, stack));
		level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.7F, 1.2F);

		BlockState empty = state.setValue(SieveBlock.GRAVEL, 0).setValue(SieveBlock.WEAR, SieveLogic.wear(sieve.damage));
		level.setBlock(pos, empty, Block.UPDATE_ALL);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(empty));
		if (SieveLogic.wornOut(sieve.damage)) {
			fallApart(serverLevel, pos, settings);
		}
	}

	/** The mesh gives way: the sieve breaks like a block without dropping itself and leaves a few sticks. */
	private static void fallApart(ServerLevel level, BlockPos pos, SieveSettings settings) {
		level.destroyBlock(pos, false);
		int sticks = settings.minSticks() + level.getRandom().nextInt(Math.max(0, settings.maxSticks() - settings.minSticks()) + 1);
		if (sticks > 0) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.STICK, sticks));
		}
	}

	/** Gravel dust falling through the mesh. */
	public static void clientTick(Level level, BlockPos pos, BlockState state, SieveBlockEntity sieve) {
		RandomSource random = level.getRandom();
		if (random.nextFloat() < 0.6F) {
			double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAVEL.defaultBlockState()),
					x, pos.getY() + 0.7, z, 0.0, 0.0, 0.0);
		}
	}

	/** A new gravel goes on the mesh. */
	public void start() {
		progress = 0;
		setChanged();
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		// Broken mid-sift: the gravel on it is not lost.
		if (level != null && state.hasProperty(SieveBlock.GRAVEL) && state.getValue(SieveBlock.GRAVEL) > 0) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.GRAVEL));
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		damage = components.getOrDefault(DataComponents.DAMAGE, 0);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		components.set(DataComponents.DAMAGE, damage);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		output.discard("Damage");
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		damage = input.getIntOr("Damage", 0);
		progress = input.getIntOr("Progress", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("Damage", damage);
		output.putInt("Progress", progress);
	}
}
