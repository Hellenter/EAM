package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerWork;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
		only(provider, VillagerProfession.LIBRARIAN, Items.BOOK);
		only(provider, VillagerProfession.CLERIC, Items.GLASS_BOTTLE);
		only(provider, VillagerProfession.CARTOGRAPHER, Items.MAP);
		only(provider, VillagerProfession.LEATHERWORKER, Items.LEATHER);
		only(provider, VillagerProfession.FLETCHER, Items.ARROW);
	}

	private static void withTools(BiConsumer<Identifier, VillagerWork> provider, ResourceKey<VillagerProfession> profession, Item item) {
		provider.accept(profession.identifier(), new VillagerWork(item, Optional.of(VillagerGearModule.toolsTag(profession))));
	}

	private static void only(BiConsumer<Identifier, VillagerWork> provider, ResourceKey<VillagerProfession> profession, Item item) {
		provider.accept(profession.identifier(), new VillagerWork(item, Optional.empty()));
	}

	@Override
	public String getName() {
		return "EAM villager work items";
	}
}
