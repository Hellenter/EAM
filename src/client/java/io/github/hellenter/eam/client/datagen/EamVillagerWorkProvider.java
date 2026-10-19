package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.vanilla.villager.EconomySettings;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerWork;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Each profession's work item and the tools a player can give in its place, in
 * {@code data/minecraft/villager_work/<profession>.json}. Nitwits and villagers without a job have none.
 */
public class EamVillagerWorkProvider extends FabricCodecDataProvider<VillagerWork> {
	public EamVillagerWorkProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villager_work", VillagerWork.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, VillagerWork> provider, HolderLookup.Provider registries) {
		withTools(provider, VillagerProfession.FARMER, Items.WOODEN_HOE);
		withTools(provider, VillagerProfession.FISHERMAN, Items.FISHING_ROD);
		withTools(provider, VillagerProfession.SHEPHERD, Items.SHEARS);
		withTools(provider, VillagerProfession.BUTCHER, EamItems.FLINT_KNIFE);
		withTools(provider, VillagerProfession.ARMORER, EamItems.COPPER_HAMMER);
		withTools(provider, VillagerProfession.TOOLSMITH, EamItems.COPPER_HAMMER);
		withTools(provider, VillagerProfession.WEAPONSMITH, EamItems.COPPER_HAMMER);
		withTools(provider, VillagerProfession.MASON, Items.WOODEN_PICKAXE);
		withComponents(provider, VillagerProfession.LIBRARIAN, Items.BOOK);
		provider.accept(VillagerProfession.CLERIC.identifier(), new VillagerWork(Items.GLASS_BOTTLE, Optional.empty(),
				Optional.of(VillagerGearModule.goodsTag(VillagerProfession.CLERIC))));
		provider.accept(VillagerProfession.CARTOGRAPHER.identifier(), new VillagerWork(Items.MAP, Optional.empty(),
				Optional.of(VillagerGearModule.goodsTag(VillagerProfession.CARTOGRAPHER))));
		withComponents(provider, VillagerProfession.LEATHERWORKER, Items.LEATHER);
		withComponents(provider, VillagerProfession.FLETCHER, Items.ARROW);
	}

	private static void withTools(BiConsumer<Identifier, VillagerWork> provider, ResourceKey<VillagerProfession> profession, Item item) {
		provider.accept(profession.identifier(), new VillagerWork(item, Optional.of(VillagerGearModule.toolsTag(profession)), components(profession)));
	}

	private static void withComponents(BiConsumer<Identifier, VillagerWork> provider, ResourceKey<VillagerProfession> profession, Item item) {
		provider.accept(profession.identifier(), new VillagerWork(item, Optional.empty(), components(profession)));
	}

	/** The goods of a trade are the components that raise its share of the village economy. */
	private static Optional<TagKey<Item>> components(ResourceKey<VillagerProfession> profession) {
		return Optional.of(EconomySettings.componentsTag(profession.identifier().getPath()));
	}

	@Override
	public String getName() {
		return "EAM villager work items";
	}
}
