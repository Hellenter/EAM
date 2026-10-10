package io.github.hellenter.eam.content.bucket;

import io.github.hellenter.eam.mixin.content.bucket.CauldronDispatcherInvoker;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Copper, silver, gold and mithril buckets do everything the vanilla (iron) bucket does with water, lava, powder snow
 * and milk: in the world, in cauldrons and dispensers. Any empty bucket may melt when it scoops lava, weaker metals
 * more often (MITE's chances, {@code data/eam/buckets/lava_melting.json}); the lava stays where it was.
 */
public final class BucketModule {
	private static volatile BucketSettings settings = new BucketSettings(Map.of());

	private BucketModule() {
	}

	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(BucketSettingsLoader.ID, new BucketSettingsLoader());
		PayloadTypeRegistry.clientboundPlay().register(BucketMeltChancesPayload.TYPE, BucketMeltChancesPayload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendMeltChances(handler.player));
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) ->
				server.getPlayerList().getPlayers().forEach(BucketModule::sendMeltChances));
		for (Map<BucketContent, Item> set : MetalBuckets.sets()) {
			DispenserBlock.registerBehavior(set.get(BucketContent.EMPTY), EMPTY_BUCKET_DISPENSE);
		}
		for (Map<BucketContent, Item> set : MetalBuckets.metalSets()) {
			for (BucketContent content : new BucketContent[] {BucketContent.WATER, BucketContent.LAVA, BucketContent.POWDER_SNOW}) {
				DispenserBlock.registerBehavior(set.get(content), FILLED_BUCKET_DISPENSE);
			}
			registerCauldrons(set);
		}
		// The vanilla bucket scooping lava from a cauldron may melt too.
		dispatcher(CauldronInteractions.LAVA).eam$put(Items.BUCKET, BucketModule::fillFromLavaCauldron);
		UseItemCallback.EVENT.register(BucketModule::meltWhenScoopingLava);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(Items.BUCKET) || MetalBuckets.contentOf(held.getItem()) != BucketContent.EMPTY) return InteractionResult.PASS;
			// Vanilla milks only into the vanilla bucket.
			SoundEvent sound;
			if (entity instanceof AbstractCow cow && !cow.isBaby()) {
				sound = SoundEvents.COW_MILK;
			} else if (entity instanceof Goat goat && !goat.isBaby()) {
				sound = goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_MILK : SoundEvents.GOAT_MILK;
			} else {
				return InteractionResult.PASS;
			}
			player.playSound(sound, 1.0F, 1.0F);
			Item milk = MetalBuckets.withContent(held.getItem(), BucketContent.MILK);
			player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(milk)));
			return InteractionResult.SUCCESS;
		});
	}

	static void setSettings(BucketSettings newSettings) {
		settings = newSettings;
	}

	/** Tells the client each empty bucket's melting chance for the tooltip. */
	private static void sendMeltChances(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, BucketMeltChancesPayload.TYPE)) return;
		Map<Item, Float> chances = new HashMap<>();
		for (Map<BucketContent, Item> set : MetalBuckets.sets()) {
			Item empty = set.get(BucketContent.EMPTY);
			chances.put(empty, settings.meltChance(empty));
		}
		ServerPlayNetworking.send(player, new BucketMeltChancesPayload(chances));
	}

	/** Whether the empty {@code bucket} melts as it scoops lava now. */
	private static boolean melts(Item bucket, Level level) {
		return BucketLogic.melts(settings.meltChance(bucket), level.getRandom().nextFloat());
	}

	/** Fizz and smoke where a bucket melted. */
	private static void meltEffects(Level level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
		}
	}

	private static void melt(Player player, ItemStack bucket, Level level, BlockPos pos) {
		player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
		bucket.shrink(1);
		meltEffects(level, pos);
		player.sendOverlayMessage(Component.translatable("message.eam.bucket_melted"));
	}

	/** Runs before any bucket's own use: an empty bucket aimed at a lava source may melt instead of filling. */
	private static InteractionResult meltWhenScoopingLava(Player player, Level level, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (level.isClientSide() || player.hasInfiniteMaterials() || MetalBuckets.contentOf(held.getItem()) != BucketContent.EMPTY) {
			return InteractionResult.PASS;
		}
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(Player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
		BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
		if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
		BlockPos pos = hit.getBlockPos();
		if (!isLavaSource(level.getFluidState(pos)) || !level.mayInteract(player, pos)
				|| !(level.getBlockState(pos).getBlock() instanceof BucketPickup) || !melts(held.getItem(), level)) {
			return InteractionResult.PASS;
		}
		melt(player, held, level, pos);
		// The client already scooped the lava on its own; show it back.
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
		}
		return InteractionResult.SUCCESS;
	}

	private static boolean isLavaSource(FluidState fluid) {
		return fluid.is(FluidTags.LAVA) && fluid.isSource();
	}

	private static void registerCauldrons(Map<BucketContent, Item> set) {
		Item empty = set.get(BucketContent.EMPTY);
		for (CauldronInteraction.Dispatcher dispatcher : new CauldronInteraction.Dispatcher[] {
				CauldronInteractions.EMPTY, CauldronInteractions.WATER, CauldronInteractions.LAVA, CauldronInteractions.POWDER_SNOW}) {
			CauldronDispatcherInvoker invoker = dispatcher(dispatcher);
			invoker.eam$put(set.get(BucketContent.WATER), (state, level, pos, player, hand, itemInHand) -> emptyIntoCauldron(level, pos,
					player, hand, itemInHand, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
					SoundEvents.BUCKET_EMPTY));
			invoker.eam$put(set.get(BucketContent.LAVA), (state, level, pos, player, hand, itemInHand) -> emptyIntoCauldron(level, pos,
					player, hand, itemInHand, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA));
			invoker.eam$put(set.get(BucketContent.POWDER_SNOW), (state, level, pos, player, hand, itemInHand) -> emptyIntoCauldron(level,
					pos, player, hand, itemInHand, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
					SoundEvents.BUCKET_EMPTY_POWDER_SNOW));
		}
		dispatcher(CauldronInteractions.WATER).eam$put(empty, (state, level, pos, player, hand, itemInHand) -> CauldronInteractions.fillBucket(
				state, level, pos, player, hand, itemInHand, new ItemStack(set.get(BucketContent.WATER)),
				s -> s.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL));
		dispatcher(CauldronInteractions.LAVA).eam$put(empty, BucketModule::fillFromLavaCauldron);
		dispatcher(CauldronInteractions.POWDER_SNOW).eam$put(empty, (state, level, pos, player, hand, itemInHand) -> CauldronInteractions.fillBucket(
				state, level, pos, player, hand, itemInHand, new ItemStack(set.get(BucketContent.POWDER_SNOW)),
				s -> s.getValue(LayeredCauldronBlock.LEVEL) == 3, SoundEvents.BUCKET_FILL_POWDER_SNOW));
	}

	private static InteractionResult fillFromLavaCauldron(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			ItemStack itemInHand) {
		if (!level.isClientSide() && !player.hasInfiniteMaterials() && melts(itemInHand.getItem(), level)) {
			melt(player, itemInHand, level, pos);
			return InteractionResult.SUCCESS;
		}
		Item lava = MetalBuckets.withContent(itemInHand.getItem(), BucketContent.LAVA);
		return CauldronInteractions.fillBucket(state, level, pos, player, hand, itemInHand, new ItemStack(lava), s -> true,
				SoundEvents.BUCKET_FILL_LAVA);
	}

	/** {@link CauldronInteractions#emptyBucket} that gives back the same metal's empty bucket. */
	private static InteractionResult emptyIntoCauldron(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack itemInHand,
			BlockState newState, SoundEvent sound) {
		if (!level.isClientSide()) {
			Item used = itemInHand.getItem();
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemInHand, player, new ItemStack(MetalBucketItem.emptyOf(used))));
			player.awardStat(Stats.FILL_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(used));
			level.setBlockAndUpdate(pos, newState);
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	private static CauldronDispatcherInvoker dispatcher(CauldronInteraction.Dispatcher dispatcher) {
		return (CauldronDispatcherInvoker) (Object) dispatcher;
	}

	/** Vanilla's filled bucket dispense, leaving the same metal's empty bucket. */
	private static final DispenseItemBehavior FILLED_BUCKET_DISPENSE = new DefaultDispenseItemBehavior() {
		private final DefaultDispenseItemBehavior dropItem = new DefaultDispenseItemBehavior();

		@Override
		public ItemStack execute(BlockSource source, ItemStack dispensed) {
			DispensibleContainerItem bucket = (DispensibleContainerItem) dispensed.getItem();
			BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
			Level level = source.level();
			if (bucket.emptyContents(null, level, target, null)) {
				bucket.checkExtraContent(null, level, dispensed, target);
				return this.consumeWithRemainder(source, dispensed, new ItemStack(MetalBucketItem.emptyOf(dispensed.getItem())));
			}
			return this.dropItem.dispense(source, dispensed);
		}
	};

	/** Vanilla's empty bucket dispense for every metal: fills into the same metal, and may melt on lava. */
	private static final DispenseItemBehavior EMPTY_BUCKET_DISPENSE = new DefaultDispenseItemBehavior() {
		@Override
		public ItemStack execute(BlockSource source, ItemStack dispensed) {
			LevelAccessor level = source.level();
			BlockPos target = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
			BlockState blockState = level.getBlockState(target);
			if (!(blockState.getBlock() instanceof BucketPickup pickupBlock)) {
				return super.execute(source, dispensed);
			}
			if (isLavaSource(blockState.getFluidState()) && melts(dispensed.getItem(), source.level())) {
				dispensed.shrink(1);
				meltEffects(source.level(), target);
				return dispensed;
			}
			ItemStack pickup = pickupBlock.pickupBlock(null, level, target, blockState);
			if (pickup.isEmpty()) {
				return super.execute(source, dispensed);
			}
			level.gameEvent(null, GameEvent.FLUID_PICKUP, target);
			BucketContent content = MetalBuckets.contentOf(pickup.getItem());
			Item filled = content == null ? null : MetalBuckets.withContent(dispensed.getItem(), content);
			return this.consumeWithRemainder(source, dispensed, new ItemStack(filled != null ? filled : pickup.getItem()));
		}
	};
}
