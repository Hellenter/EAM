package io.github.hellenter.eam.content.campfire;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A camp fire: a ring of stones with 0 to 9 sticks. Placed with 3, each stick used on it adds one, and with enough
 * sticks an igniter lights it. While lit it burns sticks, cooks food and goes out when the sticks run out. Breaking
 * it gives back the food, charcoal for the sticks it has burnt and, when it is not burning, its sticks.
 */
public class CampFireBlock extends BaseEntityBlock {
	public static final int PLACED_STICKS = 3;
	public static final int MAX_STICKS = 9;
	public static final IntegerProperty STICKS = IntegerProperty.create("sticks", 0, MAX_STICKS);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final int FRESH = 0;
	public static final int SCORCHED = 1;
	public static final int CHARRED = 2;
	/** How burnt the old sticks look; they blacken the longer the fire burns. */
	public static final IntegerProperty CHAR = IntegerProperty.create("char", FRESH, CHARRED);
	/** Sticks in the old group; the sticks above them were put in later and blacken on their own. See {@link StickPile}. */
	public static final IntegerProperty OLD_STICKS = IntegerProperty.create("old_sticks", 0, MAX_STICKS);
	/** How burnt the sticks put in later look. */
	public static final IntegerProperty NEW_CHAR = IntegerProperty.create("new_char", FRESH, CHARRED);
	/** Sticks lie on the ground instead of standing as a teepee when there are fewer than this. */
	public static final int STANDING_STICKS = 3;
	public static final int MAX_COALS = 3;
	/** How big the heap of coals in the middle is; it grows with the sticks the fire has burnt. */
	public static final IntegerProperty COALS = IntegerProperty.create("coals", 0, MAX_COALS);

	private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 8.0);

	public CampFireBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(STICKS, PLACED_STICKS).setValue(LIT, false).setValue(CHAR, FRESH).setValue(COALS, 0)
				.setValue(OLD_STICKS, PLACED_STICKS).setValue(NEW_CHAR, FRESH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STICKS, LIT, CHAR, COALS, OLD_STICKS, NEW_CHAR);
	}

	public static StickPile pile(BlockState state) {
		return new StickPile(state.getValue(STICKS), Math.min(state.getValue(OLD_STICKS), state.getValue(STICKS)),
				state.getValue(CHAR), state.getValue(NEW_CHAR));
	}

	public static BlockState withPile(BlockState state, StickPile pile) {
		return state.setValue(STICKS, pile.sticks()).setValue(OLD_STICKS, pile.old())
				.setValue(CHAR, pile.oldChar()).setValue(NEW_CHAR, pile.newChar());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (stack.is(Items.STICK)) {
			return addStick(stack, state, level, pos, player);
		}
		if (stack.is(CampFireModule.IGNITERS)) {
			return state.getValue(LIT) ? InteractionResult.PASS : light(stack, state, level, pos, player, hand);
		}
		// The property set is item-based: every potion is in it because water bottles boil, so a healing potion must
		// still be drinkable while looking at the fire.
		if (level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(stack)
				&& level.getBlockEntity(pos) instanceof CampFireBlockEntity fire
				&& (!(level instanceof ServerLevel serverLevel) || CampFireBlockEntity.cooks(serverLevel, stack))) {
			if (level instanceof ServerLevel server && fire.placeFood(server, player, stack)) {
				player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
				return InteractionResult.SUCCESS_SERVER;
			}
			return InteractionResult.CONSUME;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	private static InteractionResult addStick(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
		int sticks = state.getValue(STICKS);
		if (!CampFireLogic.canAddStick(sticks)) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			StickPile pile = pile(state);
			if (pile.startsNewGroup() && level.getBlockEntity(pos) instanceof CampFireBlockEntity fire) {
				fire.startNewGroup();
			}
			level.setBlock(pos, withPile(state, pile.add()), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	private static InteractionResult light(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			// The server knows the settings and decides; the client only swings.
			return InteractionResult.SUCCESS;
		}
		CampFireSettings settings = CampFireModule.settings();
		if (!CampFireLogic.canLight(state.getValue(STICKS), settings)) {
			player.sendOverlayMessage(Component.translatable("block.eam.camp_fire.needs_sticks", settings.sticksToLight()));
			return InteractionResult.FAIL;
		}
		level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		if (stack.is(Items.FIRE_CHARGE)) {
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			stack.consume(1, player);
		} else {
			level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			if (stack.isDamageableItem()) {
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand takes food off the fire, cooked food first. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof CampFireBlockEntity fire) || !fire.hasFood()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack food = fire.takeFood();
			player.getInventory().placeItemBackInInventory(food, Prediction.SERVER_ONLY);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (state.getValue(LIT) && entity instanceof LivingEntity) {
			entity.hurt(level.damageSources().campfire(), 1.0F);
		}
		super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		if (random.nextInt(10) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
					0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
		}
		if (random.nextInt(5) == 0) {
			level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
					random.nextFloat() / 2.0F, 5.0E-5, random.nextFloat() / 2.0F);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CampFireBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!state.getValue(LIT)) {
			return null;
		}
		BlockEntityTicker<CampFireBlockEntity> ticker = level.isClientSide() ? CampFireBlockEntity::clientTick : CampFireBlockEntity::serverTick;
		return createTickerHelper(type, EamBlockEntities.CAMP_FIRE, ticker);
	}
}
