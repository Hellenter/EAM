package io.github.hellenter.eam.content.bucket;

import com.google.common.base.Suppliers;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The bucket sets: one item per {@link BucketContent} for each metal. The vanilla buckets are the iron set. Lets a
 * bucket turn into the same metal's bucket with other contents, so a copper bucket scoops water into a copper water
 * bucket and empties back into a copper bucket.
 */
public final class MetalBuckets {
	private static final Supplier<List<Map<BucketContent, Item>>> SETS = Suppliers.memoize(() -> List.of(
			set(Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.POWDER_SNOW_BUCKET, Items.MILK_BUCKET),
			set(EamItems.COPPER_BUCKET, EamItems.COPPER_WATER_BUCKET, EamItems.COPPER_LAVA_BUCKET, EamItems.COPPER_POWDER_SNOW_BUCKET,
					EamItems.COPPER_MILK_BUCKET),
			set(EamItems.SILVER_BUCKET, EamItems.SILVER_WATER_BUCKET, EamItems.SILVER_LAVA_BUCKET, EamItems.SILVER_POWDER_SNOW_BUCKET,
					EamItems.SILVER_MILK_BUCKET),
			set(EamItems.GOLDEN_BUCKET, EamItems.GOLDEN_WATER_BUCKET, EamItems.GOLDEN_LAVA_BUCKET, EamItems.GOLDEN_POWDER_SNOW_BUCKET,
					EamItems.GOLDEN_MILK_BUCKET),
			set(EamItems.MITHRIL_BUCKET, EamItems.MITHRIL_WATER_BUCKET, EamItems.MITHRIL_LAVA_BUCKET, EamItems.MITHRIL_POWDER_SNOW_BUCKET,
					EamItems.MITHRIL_MILK_BUCKET)));

	private static final Supplier<Map<Item, Map<BucketContent, Item>>> SET_OF = Suppliers.memoize(() -> {
		Map<Item, Map<BucketContent, Item>> setOf = new HashMap<>();
		for (Map<BucketContent, Item> set : SETS.get()) {
			set.values().forEach(item -> setOf.put(item, set));
		}
		return setOf;
	});

	private static final Supplier<Map<Item, BucketContent>> CONTENT_OF = Suppliers.memoize(() -> {
		Map<Item, BucketContent> contentOf = new HashMap<>();
		for (Map<BucketContent, Item> set : SETS.get()) {
			set.forEach((content, item) -> contentOf.put(item, content));
		}
		return contentOf;
	});

	private MetalBuckets() {
	}

	/** Every bucket set, the vanilla (iron) one first. */
	public static List<Map<BucketContent, Item>> sets() {
		return SETS.get();
	}

	/** Our metal sets, without the vanilla one. */
	public static List<Map<BucketContent, Item>> metalSets() {
		return SETS.get().subList(1, SETS.get().size());
	}

	/** What {@code item} holds, or null if it is not one of the buckets here. */
	public static @Nullable BucketContent contentOf(Item item) {
		return CONTENT_OF.get().get(item);
	}

	/** The bucket of the same metal as {@code bucket} holding {@code content}, or null if {@code bucket} is not one of ours. */
	public static @Nullable Item withContent(Item bucket, BucketContent content) {
		Map<BucketContent, Item> set = SET_OF.get().get(bucket);
		return set == null ? null : set.get(content);
	}

	private static Map<BucketContent, Item> set(Item empty, Item water, Item lava, Item powderSnow, Item milk) {
		Map<BucketContent, Item> set = new EnumMap<>(BucketContent.class);
		set.put(BucketContent.EMPTY, empty);
		set.put(BucketContent.WATER, water);
		set.put(BucketContent.LAVA, lava);
		set.put(BucketContent.POWDER_SNOW, powderSnow);
		set.put(BucketContent.MILK, milk);
		return set;
	}
}
