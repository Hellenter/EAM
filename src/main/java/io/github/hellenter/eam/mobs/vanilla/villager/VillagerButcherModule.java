package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.content.feeder.FeederModule;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.food.VillagerFood;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The butcher slaughters the village's livestock and cooks the meat: now and then, at work, he takes a grown animal of
 * the kind he finds most of near him, always leaving a few of each kind to breed. Cooked meat is villager food, so like
 * a farmer's bread it goes to hungry villagers. Animals he cannot walk to without a door, in a pen fenced all round with
 * no door or gate, are never his: that is how a player keeps his own herd. Named and leashed animals are left alone too.
 * Numbers are in {@code data/eam/villagers/butcher.json}.
 */
public final class VillagerButcherModule {
	private static final byte POOF = 60;

	private static volatile ButcherSettings settings = ButcherSettings.DEFAULT;
	private static boolean enabled;

	private VillagerButcherModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ButcherSettingsLoader.ID, new ButcherSettingsLoader());
	}

	public static void init() {
		enabled = true;
		Map<Item, Integer> meat = Map.of(Items.COOKED_BEEF, 4, Items.COOKED_PORKCHOP, 4, Items.COOKED_MUTTON, 3,
				Items.COOKED_CHICKEN, 3, Items.COOKED_RABBIT, 3);
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(meat::containsKey,
				(builder, item) -> builder.set(DataComponents.VILLAGER_FOOD, new VillagerFood(meat.get(item)))));
	}

	static void setSettings(ButcherSettings value) {
		settings = value;
	}

	/** Called every tick a villager thinks on the server: now and then a working butcher slaughters an animal. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		ButcherSettings current = settings;
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), current.intervalTicks())) return;
		if (villager.isBaby() || !villager.getVillagerData().profession().is(VillagerProfession.BUTCHER)) return;
		if (!villager.getBrain().isActive(Activity.WORK) || VillagerFoodModule.points(villager) >= current.foodMax()) return;
		Animal animal = pick(level, villager, current);
		if (animal != null) slaughter(level, villager, animal);
	}

	/** A grown animal of the kind there is most of near the butcher, beyond those he leaves; none if there are too few. */
	private static Animal pick(ServerLevel level, Villager villager, ButcherSettings current) {
		List<Animal> near = level.getEntitiesOfClass(Animal.class, villager.getBoundingBox().inflate(current.radius()),
				animal -> animal.isAlive() && !animal.isBaby() && FeederModule.isLivestock(animal)
						&& !animal.hasCustomName() && !animal.isLeashed());
		Map<EntityType<?>, List<Animal>> herds = new HashMap<>();
		for (Animal animal : near) {
			if (!inClosedPen(level, animal, current.penCells())) herds.computeIfAbsent(animal.getType(), type -> new ArrayList<>()).add(animal);
		}
		return herds.values().stream()
				.filter(herd -> VillagerButcherLogic.slaughters(herd.size(), current.keepPerKind()))
				.max(Comparator.comparingInt(List::size))
				.map(herd -> herd.get(villager.getRandom().nextInt(herd.size())))
				.orElse(null);
	}

	private static void slaughter(ServerLevel level, Villager villager, Animal animal) {
		List<ItemStack> drops = new ArrayList<>();
		animal.getLootTable().ifPresent(table ->
				animal.dropFromLootTable(level, level.damageSources().mobAttack(villager), false, table, drops::add));
		level.broadcastEntityEvent(animal, POOF);
		level.playSound(null, animal.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 1.0F, 1.0F);
		animal.discard();
		villager.getLookControl().setLookAt(animal);
		for (ItemStack drop : drops) {
			ItemStack cooked = cook(level, drop);
			ItemStack left = cooked.has(DataComponents.VILLAGER_FOOD) ? villager.getInventory().addItem(cooked) : cooked;
			if (!left.isEmpty()) villager.spawnAtLocation(level, left);
		}
	}

	/** The meat cooked as in a smoker, or the drop as it is when it does not cook. */
	private static ItemStack cook(ServerLevel level, ItemStack drop) {
		SingleRecipeInput input = new SingleRecipeInput(drop);
		return level.recipeAccess().getRecipeFor(RecipeType.SMOKING, input, level)
				.map(recipe -> recipe.value().assemble(input).copyWithCount(drop.getCount()))
				.orElse(drop);
	}

	/** Whether the animal stands in a pen fenced all round with no door or gate. */
	private static boolean inClosedPen(ServerLevel level, Animal animal, int cells) {
		BlockPos start = animal.blockPosition();
		return PenLogic.closedPen((x, y, z) -> step(level, x, y, z), start.getX(), start.getY(), start.getZ(), cells);
	}

	/** Where an animal at height {@code y} gets to on column {@code x, z}: up one block, down a few, or blocked. */
	private static int step(ServerLevel level, int x, int y, int z) {
		BlockPos feet = new BlockPos(x, y, z);
		BlockState state = level.getBlockState(feet);
		int barrier = barrier(state);
		if (barrier != 0) return barrier;
		if (!state.getCollisionShape(level, feet).isEmpty()) {
			BlockPos up = feet.above();
			BlockState above = level.getBlockState(up);
			barrier = barrier(above);
			if (barrier != 0) return barrier;
			return above.getCollisionShape(level, up).isEmpty() && level.getBlockState(up.above()).getCollisionShape(level, up.above()).isEmpty()
					? y + 1 : PenLogic.WALL;
		}
		for (int down = 1; down <= 3; down++) {
			BlockPos below = feet.below(down);
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return y - down + 1;
		}
		return y - 3;
	}

	/** {@link PenLogic#DOOR} for a door or gate, {@link PenLogic#WALL} for a fence or wall, else 0. */
	private static int barrier(BlockState state) {
		if (state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES)) return PenLogic.DOOR;
		if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) return PenLogic.WALL;
		return 0;
	}
}
