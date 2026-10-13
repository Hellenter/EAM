package io.github.hellenter.eam.content.feeder;

import com.mojang.serialization.Codec;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The feed in one feeder block and the meals it gave today. Every second it calls hungry livestock around over and
 * feeds those that reached it, one meal a day each; every few seconds it takes spare crops from farmer villagers nearby
 * and lets a fed pair breed once a day while the trough has room for more heads.
 */
public class FeederBlockEntity extends BlockEntity {
	private static final Codec<Map<Item, Integer>> FOOD_CODEC = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.INT);
	/** Ticks between feeding rounds. */
	private static final int FEED_INTERVAL = 20;
	/** Ticks between visits to farmers and breeding checks. */
	private static final int SLOW_INTERVAL = 100;
	/** How close, in blocks past its own width, an animal has to be to eat from the trough. */
	private static final double REACH = 0.9;
	private static final double WALK_SPEED = 1.0;
	/** Farthest apart, in blocks, a pair can be and still find each other. */
	private static final double PARTNER_DISTANCE = 8.0;

	private final Map<Item, Integer> food = new LinkedHashMap<>();
	private long mealDay;
	private int meals;
	private long breedDay = Long.MIN_VALUE;

	public FeederBlockEntity(BlockPos pos, BlockState state) {
		super(EamBlockEntities.FEEDER, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FeederBlockEntity feeder) {
		if (!FeederModule.enabled()) return;
		ServerLevel serverLevel = (ServerLevel) level;
		// Spread feeders over ticks.
		long tick = level.getGameTime() + pos.asLong();
		if (tick % FEED_INTERVAL == 0) feeder.feedAnimals(serverLevel, pos);
		if (tick % SLOW_INTERVAL == 0) {
			feeder.takeFromFarmers(serverLevel, pos);
			feeder.breed(serverLevel, pos, state);
		}
	}

	public int food() {
		int total = 0;
		for (int count : food.values()) total += count;
		return total;
	}

	public boolean isFull() {
		return food() >= capacity();
	}

	private int capacity() {
		FeederSettings settings = FeederModule.settings();
		return getBlockState().getBlock() instanceof FeederBlock block ? block.capacity(settings) : settings.foodCapacity();
	}

	private int heads() {
		FeederSettings settings = FeederModule.settings();
		return getBlockState().getBlock() instanceof FeederBlock block ? block.heads(settings) : settings.headsPerFeeder();
	}

	/** Meals given today. */
	public int mealsToday(ServerLevel level) {
		return mealDay == FeederModule.today(level) ? meals : 0;
	}

	/** Puts up to {@code count} of {@code item} in, as much as fits. Returns how many went in. */
	public int add(Item item, int count) {
		int added = Math.min(count, capacity() - food());
		if (added <= 0) return 0;
		food.merge(item, added, Integer::sum);
		changed();
		return added;
	}

	/** Takes one feed item out, of the kind there is most of. */
	private Item takeOne() {
		Item item = food.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		food.computeIfPresent(item, (key, count) -> count > 1 ? count - 1 : null);
		changed();
		return item;
	}

	private void changed() {
		setChanged();
		if (level == null) return;
		BlockState state = getBlockState();
		int fill = FeederLogic.fill(food(), capacity());
		if (state.hasProperty(FeederBlock.FILL) && state.getValue(FeederBlock.FILL) != fill) {
			level.setBlock(worldPosition, state.setValue(FeederBlock.FILL, fill), Block.UPDATE_CLIENTS);
		}
	}

	// --- Feeding ---

	private void feedAnimals(ServerLevel level, BlockPos pos) {
		if (food.isEmpty()) return;
		FeederSettings settings = FeederModule.settings();
		long today = FeederModule.today(level);
		int left = heads() - mealsToday(level);
		if (left <= 0) return;
		AABB trough = new AABB(pos);
		Vec3 center = Vec3.atCenterOf(pos);
		List<Animal> hungry = level.getEntitiesOfClass(Animal.class, trough.inflate(settings.animalRadius()),
				animal -> animal.isAlive() && FeederModule.isLivestock(animal)
						&& FeederLogic.canEat(animal.getAttached(FeederModule.LIVESTOCK_STATE), today));
		hungry.sort(Comparator.comparingDouble(animal -> animal.distanceToSqr(center)));
		// Only as many come over as there are meals left today, nearest first.
		int callers = left;
		for (Animal animal : hungry) {
			if (left <= 0 || callers <= 0 || food.isEmpty()) return;
			if (reaches(animal, trough)) {
				eat(level, animal, today, settings);
				left--;
				callers--;
			} else if (!animal.isLeashed() && !animal.isPassenger() && walkTo(animal, pos)) {
				// Animals that cannot get here (behind a fence, leashed) don't take a meal from those that can.
				callers--;
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

	private void eat(ServerLevel level, Animal animal, long today, FeederSettings settings) {
		Item item = takeOne();
		if (mealDay != today) {
			mealDay = today;
			meals = 0;
		}
		meals++;
		animal.setAttached(FeederModule.LIVESTOCK_STATE, FeederLogic.eat(animal.getAttached(FeederModule.LIVESTOCK_STATE), today, settings.thinAfterDays()));
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
		if (isFull() || settings.farmerRadius() <= 0) return;
		List<Villager> farmers = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(settings.farmerRadius()),
				villager -> villager.isAlive() && !villager.isBaby() && villager.getVillagerData().profession().is(VillagerProfession.FARMER));
		for (Villager farmer : farmers) {
			if (isFull()) return;
			if (giveFromFarmer(farmer, settings) > 0) {
				farmer.getLookControl().setLookAt(Vec3.atCenterOf(pos));
				level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.7F, 0.9F);
			}
		}
	}

	private int giveFromFarmer(Villager farmer, FeederSettings settings) {
		SimpleContainer inventory = farmer.getInventory();
		int points = 0;
		for (ItemStack stack : inventory.getItems()) {
			points += stack.getCount() * foodPoints(stack);
		}
		int given = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.is(FeederModule.FEED)) continue;
			int each = foodPoints(stack);
			int seeds = stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) ? settings.farmerKeepsSeeds() : 0;
			int count = FeederLogic.farmerGives(stack.getCount(), each, points, settings.farmerKeepsPoints(), seeds);
			int added = add(stack.getItem(), count);
			if (added <= 0) continue;
			inventory.removeItem(slot, added);
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

	// --- Breeding ---

	/** Once a day a pair of fed adults of one kind falls in love, while the animals around fit the trough. */
	private void breed(ServerLevel level, BlockPos pos, BlockState state) {
		FeederSettings settings = FeederModule.settings();
		long today = FeederModule.today(level);
		if (!settings.breeding() || breedDay == today || !(state.getBlock() instanceof FeederBlock block)) return;
		List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(settings.animalRadius()),
				animal -> animal.isAlive() && FeederModule.isLivestock(animal));
		if (!FeederLogic.roomToBreed(animals.size(), block.length(level, pos, state), block.heads(settings))) return;
		Map<EntityType<?>, List<Animal>> waiting = new HashMap<>();
		for (Animal animal : animals) {
			LivestockState fed = animal.getAttached(FeederModule.LIVESTOCK_STATE);
			if (fed == null || fed.lastMealDay() != today || FeederLogic.isThin(fed, today, settings.thinAfterDays())
					|| animal.getAge() != 0 || !animal.canFallInLove()) {
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
				breedDay = today;
				setChanged();
				return;
			}
		}
	}

	// --- Saving ---

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level == null) return;
		food.forEach((item, count) -> {
			for (int left = count; left > 0; ) {
				int size = Math.min(left, item.getDefaultMaxStackSize());
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(item, size));
				left -= size;
			}
		});
		food.clear();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		food.clear();
		input.read("Food", FOOD_CODEC).ifPresent(food::putAll);
		food.values().removeIf(count -> count <= 0);
		mealDay = input.getLongOr("MealDay", 0L);
		meals = input.getIntOr("Meals", 0);
		breedDay = input.getLongOr("BreedDay", Long.MIN_VALUE);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("Food", FOOD_CODEC, Map.copyOf(food));
		output.putLong("MealDay", mealDay);
		output.putInt("Meals", meals);
		output.putLong("BreedDay", breedDay);
	}
}
