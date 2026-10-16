package io.github.hellenter.eam.content.feeder;

import com.mojang.serialization.Codec;

import io.github.hellenter.eam.content.EamBlockEntities;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerFoodModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * The feed in one feeder block and the meals it gave today. Every second it calls hungry livestock around over and
 * feeds those that reached it, one meal a day each; an animal that ate grass today still comes for one meal when it has
 * not eaten from a feeder for a day, as eating from one makes it grow up and breed faster. Every few seconds it takes
 * spare crops from farmer villagers nearby and lets a fed pair breed, once a day or faster with
 * {@link FeederSettings#fedGrowthBoost()}, while the trough has room for more heads, and once a day checks that its
 * animals have water near and space: days without them make an animal sick. An animal with no way to the trough is not
 * searched a path for again for a while.
 *
 * <p>The feed lies in the block's inventory, {@link FeederBlock#slots} slots that take only feed. Players fill it
 * through the trough's menu, hoppers and droppers from any side; hoppers cannot take feed back out.
 */
public class FeederBlockEntity extends BlockEntity implements WorldlyContainer {
	/** Ticks between feeding rounds. */
	private static final int FEED_INTERVAL = 20;
	/** Ticks between visits to farmers and breeding checks. */
	private static final int SLOW_INTERVAL = 100;
	/** How close, in blocks past its own width, an animal has to be to eat from the trough. */
	private static final double REACH = 0.9;
	private static final double WALK_SPEED = 1.0;
	/** Ticks a found or missing water source is trusted before the feeder looks again. */
	private static final int WATER_RECHECK = 1200;
	/** Ticks of slowness a sick animal gets at each check, a little more than the checks are apart. */
	private static final int SICK_SLOWNESS = SLOW_INTERVAL + 40;
	/** Ticks before the trough tries again to find a way for an animal that had none. */
	private static final int UNREACHABLE_RETRY = 200;
	/** Farthest apart, in blocks, a pair can be and still find each other. */
	private static final double PARTNER_DISTANCE = 8.0;
	/** Feeders saved before they had an inventory kept their feed as item counts. */
	private static final Codec<Map<Item, Integer>> OLD_FOOD_CODEC = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);

	private final NonNullList<ItemStack> items;
	private final int[] slots;
	private long mealDay;
	private int meals;
	private long breedTime = LivestockState.NEVER;
	/**
	 * Animals, by entity id, that had no way to the trough, with the game time to look for one again. Path searches are
	 * the costly part of calling animals over; not saved.
	 */
	private final Int2LongOpenHashMap unreachable = new Int2LongOpenHashMap();
	/** Whether there was water near at the last look, and the game time to look again. Not saved. */
	private boolean water;
	private long waterRecheck = Long.MIN_VALUE;

	public FeederBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.FEEDER, pos, state);
		FeederSettings settings = FeederModule.settings();
		int size = state.getBlock() instanceof FeederBlock block ? block.slots(settings) : settings.slotsPerFeeder();
		items = NonNullList.withSize(size, ItemStack.EMPTY);
		slots = IntStream.range(0, size).toArray();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity feeder) {
		if (!FeederModule.enabled()) return;
		ServerLevel serverLevel = (ServerLevel) level;
		// Spread feeders over ticks.
		long tick = level.getGameTime() + pos.asLong();
		if (tick % FEED_INTERVAL == 0) feeder.feedAnimals(serverLevel, pos);
		if (tick % SLOW_INTERVAL == 0) {
			feeder.takeFromFarmers(serverLevel, pos);
			FeederSettings settings = FeederModule.settings();
			List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(settings.animalRadius()),
					animal -> animal.isAlive() && FeederModule.isLivestock(animal));
			Set<Animal> unwell = feeder.checkNeeds(serverLevel, pos, animals, settings);
			feeder.breed(serverLevel, pos, state, animals, unwell);
		}
	}

	private boolean hasFood() {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) return true;
		}
		return false;
	}

	public int heads() {
		FeederSettings settings = FeederModule.settings();
		return getBlockState().getBlock() instanceof FeederBlock block ? block.heads(settings) : settings.headsPerFeeder();
	}

	/** Meals given today. */
	public int mealsToday(ServerLevel level) {
		return mealDay == FeederModule.today(level) ? meals : 0;
	}

	/** Puts up to {@code count} of {@code item} in, topping up stacks of it first. Returns how many went in. */
	public int add(Item item, int count) {
		ItemStack left = new ItemStack(item, count);
		for (int pass = 0; pass < 2 && !left.isEmpty(); pass++) {
			for (int i = 0; i < items.size() && !left.isEmpty(); i++) {
				ItemStack stack = items.get(i);
				if (pass == 0 && ItemStack.isSameItemSameComponents(stack, left)) {
					int moved = Math.min(left.getCount(), stack.getMaxStackSize() - stack.getCount());
					stack.grow(moved);
					left.shrink(moved);
				} else if (pass == 1 && stack.isEmpty()) {
					items.set(i, left.split(left.getMaxStackSize()));
				}
			}
		}
		int added = count - left.getCount();
		if (added > 0) setChanged();
		return added;
	}

	/** Takes one feed item out, of the kind there is most of. */
	private Item takeOne() {
		Map<Item, Integer> counts = new HashMap<>();
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		Item item = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		// The smallest stack of it, so whole slots free up.
		ItemStack smallest = items.stream().filter(stack -> stack.is(item)).min(Comparator.comparingInt(ItemStack::getCount)).orElseThrow();
		smallest.shrink(1);
		setChanged();
		return item;
	}

	// --- Container: players, hoppers and droppers put feed in ---

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return !hasFood();
	}

	@Override
	public ItemStack getItem(int index) {
		return items.get(index);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, index, count);
		if (!taken.isEmpty()) setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return ContainerHelper.takeItem(items, index);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		items.set(index, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return stack.is(FeederModule.FEED);
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return slots;
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return canPlaceItem(index, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return false;
	}

	/** Also shows the new amount of hay in the trough. */
	@Override
	public void setChanged() {
		super.setChanged();
		if (level == null || level.isClientSide()) return;
		BlockState state = getBlockState();
		int fill = FeederLogic.fill(AbstractContainerMenu.getRedstoneSignalFromContainer(this), 15);
		if (state.hasProperty(FeederBlock.FILL) && state.getValue(FeederBlock.FILL) != fill) {
			level.setBlock(worldPosition, state.setValue(FeederBlock.FILL, fill), Block.UPDATE_CLIENTS);
		}
	}

	// --- Feeding ---

	private void feedAnimals(ServerLevel level, BlockPos pos) {
		if (!hasFood()) return;
		FeederSettings settings = FeederModule.settings();
		long now = FeederModule.now(level);
		long today = FeederLogic.day(now);
		int left = heads() - mealsToday(level);
		if (left <= 0) return;
		long gameTime = level.getGameTime();
		unreachable.int2LongEntrySet().removeIf(entry -> entry.getLongValue() <= gameTime);
		AABB trough = new AABB(pos);
		Vec3 center = Vec3.atCenterOf(pos);
		List<Animal> hungry = level.getEntitiesOfClass(Animal.class, trough.inflate(settings.animalRadius()),
				animal -> animal.isAlive() && FeederModule.isLivestock(animal)
						&& FeederLogic.wantsFeeder(animal.getAttached(FeederModule.LIVESTOCK_STATE), today, now));
		hungry.sort(Comparator.comparingDouble(animal -> animal.distanceToSqr(center)));
		// Only as many come over as there are meals left today, nearest first.
		int callers = left;
		for (Animal animal : hungry) {
			if (left <= 0 || callers <= 0 || !hasFood()) return;
			if (reaches(animal, trough)) {
				eat(level, animal, today, now, settings);
				unreachable.remove(animal.getId());
				left--;
				callers--;
			} else if (animal.isLeashed() || animal.isPassenger() || unreachable.containsKey(animal.getId())) {
				// Animals that cannot get here (behind a fence, leashed) don't take a meal from those that can.
				continue;
			} else if (walkTo(animal, pos)) {
				callers--;
			} else {
				unreachable.put(animal.getId(), gameTime + UNREACHABLE_RETRY);
			}
		}
	}

	private static boolean reaches(Animal animal, AABB trough) {
		AABB box = animal.getBoundingBox();
		double dx = Math.max(0, Math.max(trough.minX - box.maxX, box.minX - trough.maxX));
		double dz = Math.max(0, Math.max(trough.minZ - box.maxZ, box.minZ - trough.maxZ));
		double dy = Math.max(0, Math.max(trough.minY - box.maxY, box.minY - trough.maxY));
		return dx <= REACH && dz <= REACH && dy <= REACH;
	}

	/** Sends the animal to the trough. False if it has no way there. */
	private static boolean walkTo(Animal animal, BlockPos pos) {
		if (!animal.getNavigation().isDone() && pos.equals(animal.getNavigation().getTargetPos())) return true;
		Path path = animal.getNavigation().createPath(pos, 1);
		if (path == null || !path.canReach()) return false;
		return animal.getNavigation().moveTo(path, WALK_SPEED);
	}

	private void eat(ServerLevel level, Animal animal, long today, long now, FeederSettings settings) {
		Item item = takeOne();
		if (mealDay != today) {
			mealDay = today;
			meals = 0;
		}
		meals++;
		animal.setAttached(FeederModule.LIVESTOCK_STATE,
				FeederLogic.feederMeal(animal.getAttached(FeederModule.LIVESTOCK_STATE), today, now, settings.thinAfterDays()));
		animal.getNavigation().stop();
		animal.getLookControl().setLookAt(Vec3.atCenterOf(worldPosition));
		level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.6F,
				0.8F + level.getRandom().nextFloat() * 0.4F);
		Vec3 mouth = animal.getEyePosition().add(animal.getLookAngle().scale(animal.getBbWidth() * 0.5));
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), mouth.x, mouth.y - 0.1, mouth.z, 6, 0.1, 0.1, 0.1, 0.05);
	}

	// --- Farmers ---

	/** Farmer villagers nearby put in the crops they can spare beyond what they keep for themselves and other villagers. */
	private void takeFromFarmers(ServerLevel level, BlockPos pos) {
		FeederSettings settings = FeederModule.settings();
		if (settings.farmerRadius() <= 0) return;
		List<Villager> farmers = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(settings.farmerRadius()),
				villager -> villager.isAlive() && !villager.isBaby() && villager.getVillagerData().profession().is(VillagerProfession.FARMER));
		for (Villager farmer : farmers) {
			if (giveFromFarmer(farmer, settings) > 0) {
				farmer.getLookControl().setLookAt(Vec3.atCenterOf(pos));
				level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.7F, 0.9F);
			}
		}
	}

	private int giveFromFarmer(Villager farmer, FeederSettings settings) {
		SimpleContainer inventory = farmer.getInventory();
		int points = 0;
		// Crops are counted over all slots: with food stacks of 8 a farmer's carrots lie in several stacks.
		Map<Item, Integer> crops = new LinkedHashMap<>();
		for (ItemStack stack : inventory.getItems()) {
			points += stack.getCount() * foodPoints(stack);
			if (stack.is(FeederModule.FEED)) crops.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		int keep = settings.farmerKeepsPoints() + VillagerFoodModule.farmerKeeps(farmer, points);
		int given = 0;
		for (Map.Entry<Item, Integer> crop : crops.entrySet()) {
			ItemStack sample = new ItemStack(crop.getKey());
			int each = foodPoints(sample);
			int seeds = sample.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) ? settings.farmerKeepsSeeds() : 0;
			int count = FeederLogic.farmerGives(crop.getValue(), each, points, keep, seeds);
			int added = count > 0 ? add(crop.getKey(), count) : 0;
			if (added <= 0) continue;
			inventory.removeItemType(crop.getKey(), added);
			points -= added * each;
			given += added;
		}
		return given;
	}

	/** Villager food points of a stack item; wheat counts one as farmers bake it into bread. */
	private static int foodPoints(ItemStack stack) {
		var villagerFood = stack.get(DataComponents.VILLAGER_FOOD);
		if (villagerFood != null) return villagerFood.nutrition();
		return stack.is(FeederModule.FEED) ? 1 : 0;
	}

	// --- Water and space ---

	/**
	 * Once a day checks each animal kept at this feeder for water near the feeder and space around it, and keeps sick
	 * ones slow. Returns the animals that lack water or space now or are sick: they are not paired. Neighbours are
	 * counted among the animals already found around, so this costs no more entity searches; water is looked for once a
	 * minute.
	 */
	private Set<Animal> checkNeeds(ServerLevel level, BlockPos pos, List<Animal> animals, FeederSettings settings) {
		long now = FeederModule.now(level);
		long today = FeederLogic.day(now);
		boolean hasWater = hasWater(level, pos, settings.waterRadius());
		Set<Animal> unwell = new HashSet<>();
		for (Animal animal : animals) {
			LivestockState fed = animal.getAttached(FeederModule.LIVESTOCK_STATE);
			if (!FeederLogic.fedAtFeeder(fed, now)) continue;
			boolean crowded = FeederLogic.crowded(neighbours(animal, animals, settings.crowdRadius()), settings.crowdLimit());
			boolean bad = crowded || !hasWater;
			LivestockState checked = FeederLogic.checkNeeds(fed, today, bad, settings.sickAfterDays());
			if (checked != fed) animal.setAttached(FeederModule.LIVESTOCK_STATE, checked);
			if (checked.sick()) animal.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SICK_SLOWNESS, 0));
			if (bad || checked.sick()) unwell.add(animal);
		}
		return unwell;
	}

	/** Other livestock standing within {@code radius} blocks across and two up or down of {@code animal}. */
	private static int neighbours(Animal animal, List<Animal> animals, int radius) {
		int count = 0;
		for (Animal other : animals) {
			if (other != animal && Math.abs(other.getX() - animal.getX()) <= radius && Math.abs(other.getZ() - animal.getZ()) <= radius
					&& Math.abs(other.getY() - animal.getY()) <= 2) {
				count++;
			}
		}
		return count;
	}

	/** Whether there is a water block or a water cauldron within {@code radius} across, from two below to one above. */
	private boolean hasWater(ServerLevel level, BlockPos pos, int radius) {
		if (radius <= 0) return true;
		long gameTime = level.getGameTime();
		if (gameTime < waterRecheck) return water;
		waterRecheck = gameTime + WATER_RECHECK;
		water = false;
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-radius, -2, -radius), pos.offset(radius, 1, radius))) {
			if (!level.hasChunkAt(at)) continue;
			BlockState state = level.getBlockState(at);
			if (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.WATER_CAULDRON)) {
				water = true;
				break;
			}
		}
		return water;
	}

	// --- Breeding ---

	/**
	 * Once a day, or {@link FeederSettings#fedGrowthBoost()} times as often, a pair of adults of one kind kept at a feeder
	 * and fed today falls in love, while the animals around fit the trough.
	 */
	private void breed(ServerLevel level, BlockPos pos, BlockState state, List<Animal> animals, Set<Animal> unwell) {
		FeederSettings settings = FeederModule.settings();
		long now = FeederModule.now(level);
		long today = FeederLogic.day(now);
		if (!settings.breeding() || !FeederLogic.canBreedAgain(breedTime, now, settings.fedGrowthBoost())
				|| !(state.getBlock() instanceof FeederBlock block)) {
			return;
		}
		if (!FeederLogic.roomToBreed(animals.size(), block.length(level, pos, state), block.heads(settings))) return;
		Map<EntityType<?>, List<Animal>> waiting = new HashMap<>();
		for (Animal animal : animals) {
			LivestockState fed = animal.getAttached(FeederModule.LIVESTOCK_STATE);
			if (fed == null || fed.lastMealDay() != today || !FeederLogic.fedAtFeeder(fed, now) || FeederLogic.isThin(fed, today, settings.thinAfterDays())
					|| unwell.contains(animal) || animal.getAge() != 0 || !animal.canFallInLove()) {
				continue;
			}
			List<Animal> kind = waiting.computeIfAbsent(animal.getType(), type -> new ArrayList<>());
			// Vanilla lovers only look for a partner this close.
			Animal partner = kind.stream().filter(other -> other.distanceToSqr(animal) <= PARTNER_DISTANCE * PARTNER_DISTANCE)
					.findFirst().orElse(null);
			kind.add(animal);
			if (partner != null) {
				partner.setInLove(null);
				animal.setInLove(null);
				breedTime = now;
				setChanged();
				return;
			}
		}
	}

	// --- Saving ---

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) Containers.dropContents(level, pos, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		input.read("Food", OLD_FOOD_CODEC).ifPresent(food -> food.forEach(this::add));
		input.read("Slot", ItemStack.CODEC).ifPresent(stack -> add(stack.getItem(), stack.getCount()));
		mealDay = input.getLongOr("MealDay", 0L);
		meals = input.getIntOr("Meals", 0);
		breedTime = input.getLongOr("BreedTime", LivestockState.NEVER);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("MealDay", mealDay);
		output.putInt("Meals", meals);
		output.putLong("BreedTime", breedTime);
	}
}
