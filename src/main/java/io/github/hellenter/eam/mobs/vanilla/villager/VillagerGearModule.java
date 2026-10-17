package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mixin.mobs.vanilla.villager.VillagerInvoker;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Optional;

/**
 * Villagers get an inventory: armor, a weapon or a tool of their trade, a shield and the slots they keep food in. A right
 * click opens a villager's trades with his gear beside them and a button to his inventory; sneaking opens the inventory
 * directly, and so does a right click on a villager with nothing to trade. Each profession
 * has a work item the villager always has and holds while his hand is free; which one is in
 * {@code data/<namespace>/villager_work/<profession>.json}. What a player gives drops when the villager dies; the work
 * item never leaves him.
 */
public final class VillagerGearModule {
	/** Marks a villager's own work item: never given, taken or dropped. */
	public static final DataComponentType<Unit> WORK_ITEM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Eam.id("villager_work_item"),
			DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

	/** What a villager takes in his main hand to fight with, besides the tools of his trade. */
	public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM, Eam.id("villager_weapons"));
	/** What a villager takes in his off hand. */
	public static final TagKey<Item> OFF_HAND = TagKey.create(Registries.ITEM, Eam.id("villager_off_hand"));

	/** The tools a player can give a villager of {@code profession} to work with, named by its datagen. */
	public static TagKey<Item> toolsTag(ResourceKey<VillagerProfession> profession) {
		return TagKey.create(Registries.ITEM, Eam.id("villager_tools/" + profession.identifier().getPath()));
	}

	/**
	 * The villager's real main hand item while vanilla shows a trade or bone meal in his hand, so showing it never loses
	 * what a player gave him.
	 */
	public static final AttachmentType<ItemStack> HELD_ASIDE = AttachmentRegistry.create(Eam.id("villager_held_aside"),
			builder -> builder.persistent(ItemStack.OPTIONAL_CODEC));

	public static final ExtendedMenuType<VillagerGearMenu, VillagerGearMenu.OpenData> MENU = Registry.register(BuiltInRegistries.MENU,
			Eam.id("villager_gear"), new ExtendedMenuType<>(VillagerGearMenu::new, VillagerGearMenu.OpenData.STREAM_CODEC));

	private static volatile Map<Identifier, VillagerWork> work = Map.of();
	private static boolean enabled;

	private VillagerGearModule() {
	}

	/** The menu, component and payload are registered whatever the config says, so client and server always match. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(VillagerWorkLoader.ID, new VillagerWorkLoader());
		PayloadTypeRegistry.serverboundPlay().register(VillagerTradePayload.TYPE, VillagerTradePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VillagerInventoryPayload.TYPE, VillagerInventoryPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VillagerTradePanelPayload.TYPE, VillagerTradePanelPayload.STREAM_CODEC);
	}

	public static void init() {
		enabled = true;
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!(entity instanceof Villager villager) || !interacts(player, villager, hand)) return InteractionResult.PASS;
			if (player instanceof ServerPlayer serverPlayer) {
				if (serverPlayer.isSecondaryUseActive() || villager.getOffers().isEmpty()) {
					serverPlayer.openMenu(new Provider(villager));
				} else {
					startTrading(villager, serverPlayer);
				}
			}
			return InteractionResult.SUCCESS;
		});
		ServerPlayNetworking.registerGlobalReceiver(VillagerTradePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof VillagerGearMenu menu) || menu.villager() == null) return;
			Villager villager = menu.villager();
			player.closeContainer();
			startTrading(villager, player);
		});
		ServerPlayNetworking.registerGlobalReceiver(VillagerInventoryPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			Villager villager = VillagerTradePanel.trading(player);
			if (villager == null || !villager.isAlive()) return;
			player.closeContainer();
			player.openMenu(new Provider(villager));
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Villager villager) resetHand(villager);
		});
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof Villager villager) dropNoWorkItem(villager);
			return true;
		});
		ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, params) -> {
			if (previous instanceof Villager) dropNoWorkItem(converted);
		});
	}

	static void setWork(Map<Identifier, VillagerWork> value) {
		work = value;
	}

	/** Called when a villager's data changes on the server: a new profession brings its work item. */
	public static void onVillagerDataChanged(Villager villager) {
		if (enabled && !villager.level().isClientSide()) resetHand(villager);
	}

	public static boolean isWorkItem(ItemStack stack) {
		return stack.has(WORK_ITEM);
	}

	/** The villager's work item, or nothing for a child or a profession without one. */
	public static ItemStack workItem(Villager villager) {
		VillagerWork current = work(villager);
		if (current == null || villager.isBaby()) return ItemStack.EMPTY;
		ItemStack stack = new ItemStack(current.item());
		stack.set(WORK_ITEM, Unit.INSTANCE);
		return stack;
	}

	/** Tools of the villager's trade a player can give him, if his trade has any. */
	public static Optional<TagKey<Item>> tools(Villager villager) {
		VillagerWork current = work(villager);
		return current == null ? Optional.empty() : current.tools();
	}

	/** Items the villager's inventory takes besides food, if his trade has any. */
	public static Optional<TagKey<Item>> goods(Villager villager) {
		VillagerWork current = work(villager);
		return current == null ? Optional.empty() : current.goods();
	}

	/** The goods a player can give a villager of {@code profession}, named by its datagen. */
	public static TagKey<Item> goodsTag(ResourceKey<VillagerProfession> profession) {
		return TagKey.create(Registries.ITEM, Eam.id("villager_goods/" + profession.identifier().getPath()));
	}

	/** What the villager really holds in his main hand, even while vanilla shows a trade in it. */
	public static ItemStack realHand(Villager villager) {
		ItemStack aside = villager.getAttached(HELD_ASIDE);
		return aside != null ? aside : villager.getMainHandItem();
	}

	/**
	 * Puts {@code stack} from a player in the villager's {@code slot}; it drops when he dies. An empty main hand gets the
	 * work item back.
	 */
	static void give(Villager villager, EquipmentSlot slot, ItemStack stack) {
		ItemStack placed = slot == EquipmentSlot.MAINHAND && stack.isEmpty() ? workItem(villager) : stack;
		if (slot == EquipmentSlot.MAINHAND && villager.hasAttached(HELD_ASIDE)) {
			villager.setAttached(HELD_ASIDE, placed);
		} else {
			villager.setItemSlot(slot, placed);
		}
		if (!stack.isEmpty()) villager.setGuaranteedDrop(slot);
	}

	/**
	 * Vanilla shows a trade or bone meal in the villager's hand ({@code stack}), and clears it after with an empty stack:
	 * the real item is put aside meanwhile and comes back after. Vanilla also clears the hand when there is nothing to
	 * show and nothing was shown; then the hand holds the real item, which stays.
	 */
	public static void showInHand(Villager villager, ItemStack stack) {
		if (!enabled) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, stack);
			return;
		}
		if (stack.isEmpty()) {
			ItemStack aside = villager.removeAttached(HELD_ASIDE);
			if (aside != null) villager.setItemSlot(EquipmentSlot.MAINHAND, aside);
			resetHand(villager);
			return;
		}
		if (!villager.hasAttached(HELD_ASIDE)) villager.setAttached(HELD_ASIDE, villager.getMainHandItem());
		villager.setItemSlot(EquipmentSlot.MAINHAND, stack);
	}

	/**
	 * Whether the villager's brain runs this tick: not while a player has his inventory open, so he does not walk off
	 * from the player or swap what is in his hands; he stands and looks at the player instead.
	 */
	public static boolean thinks(Villager villager) {
		if (!enabled || !(villager.level() instanceof ServerLevel level)) return true;
		for (ServerPlayer player : level.players()) {
			if (player.containerMenu instanceof VillagerGearMenu menu && menu.villager() == villager) {
				villager.getNavigation().stop();
				villager.getLookControl().setLookAt(player);
				return false;
			}
		}
		return true;
	}

	/** After something a player gave the villager wore: a broken main hand item gives way to the work item. */
	static void afterWear(Villager villager) {
		if (villager.getMainHandItem().isEmpty()) resetHand(villager);
	}

	/** Whether vanilla's drop chance for a shown item applies: not when the hand holds a player's item aside. */
	public static boolean keepsVanillaDropChance() {
		return !enabled;
	}

	private static boolean interacts(Player player, Villager villager, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) return false;
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.VILLAGER_SPAWN_EGG) || held.is(Items.NAME_TAG) || held.is(Items.LEAD)) return false;
		return villager.isAlive() && !villager.isBaby() && !villager.isSleeping() && !villager.isTrading();
	}

	private static void startTrading(Villager villager, ServerPlayer player) {
		if (!villager.isAlive() || villager.isTrading() || villager.isSleeping()) return;
		player.awardStat(Stats.TALKED_TO_VILLAGER);
		if (villager.getOffers().isEmpty()) {
			villager.setUnhappyCounter(40);
			villager.makeSound(SoundEvents.VILLAGER_NO);
			return;
		}
		((VillagerInvoker) villager).eam$startTrading(player);
		VillagerTradePanel.show(villager, player);
	}

	/** Gives the villager his work item when his hand holds no item of a player's, or takes an old trade's away. */
	private static void resetHand(Villager villager) {
		if (villager.hasAttached(HELD_ASIDE)) return;
		ItemStack hand = villager.getMainHandItem();
		ItemStack expected = workItem(villager);
		boolean right = !expected.isEmpty() && ItemStack.isSameItemSameComponents(hand, expected);
		if (VillagerGearLogic.resetsHand(hand.isEmpty(), isWorkItem(hand), right, !expected.isEmpty())) {
			villager.setItemSlot(EquipmentSlot.MAINHAND, expected);
		}
	}

	/** Takes the work item out of the villager's hands before he dies or turns into another mob, so it never drops. */
	private static void dropNoWorkItem(Mob mob) {
		ItemStack aside = mob.removeAttached(HELD_ASIDE);
		if (aside != null) mob.setItemSlot(EquipmentSlot.MAINHAND, aside);
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			if (isWorkItem(mob.getItemBySlot(slot))) mob.setItemSlot(slot, ItemStack.EMPTY);
		}
	}

	private static VillagerWork work(Villager villager) {
		return villager.getVillagerData().profession().unwrapKey().map(key -> work.get(key.identifier())).orElse(null);
	}

	private record Provider(Villager villager) implements ExtendedMenuProvider<VillagerGearMenu.OpenData> {
		@Override
		public Component getDisplayName() {
			return villager.getDisplayName();
		}

		@Override
		public VillagerGearMenu.OpenData getScreenOpeningData(ServerPlayer player) {
			return new VillagerGearMenu.OpenData(villager.getId(), tools(villager), goods(villager));
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new VillagerGearMenu(containerId, inventory, getScreenOpeningData((ServerPlayer) player),
					new VillagerGearContainer(villager), villager);
		}
	}
}
