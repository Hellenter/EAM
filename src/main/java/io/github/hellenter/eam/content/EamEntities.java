package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ground.ThrownPebble;
import io.github.hellenter.eam.mobs.entity.bat.VampireBat;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.cavalry.LostWarhorse;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.LordShield;
import io.github.hellenter.eam.mobs.entity.fallenlord.ThrownLordSpear;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuard;
import io.github.hellenter.eam.mobs.entity.king.PillagerKing;
import io.github.hellenter.eam.mobs.entity.king.ThrownKingAxe;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnight;
import io.github.hellenter.eam.mobs.entity.lost.Lost;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurker;
import io.github.hellenter.eam.mobs.entity.spider.BlackWidow;
import io.github.hellenter.eam.mobs.entity.spider.WoodSpider;
import io.github.hellenter.eam.mobs.entity.warlock.MagicMissile;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;
import io.github.hellenter.eam.mobs.entity.wight.Wight;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity types added by EAM: projectiles and our mobs (their code lives in {@code mobs/entity}). */
public final class EamEntities {
	public static final EntityType<ThrownPebble> PEBBLE = register("pebble",
			EntityType.Builder.<ThrownPebble>of(ThrownPebble::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** Crypt boss: a skeleton as tall as a wither skeleton. Fire and lava do not hurt him. */
	public static final EntityType<FallenLord> FALLEN_LORD = register("fallen_lord",
			EntityType.Builder.of(FallenLord::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7F, 2.4F)
					.eyeHeight(2.1F)
					.clientTrackingRange(10));

	public static final EntityType<ThrownLordSpear> LORD_SPEAR = register("lord_spear",
			EntityType.Builder.<ThrownLordSpear>of(ThrownLordSpear::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** The Fallen Lord's shield lying where he dropped it. */
	public static final EntityType<LordShield> LORD_SHIELD = register("lord_shield",
			EntityType.Builder.<LordShield>of(LordShield::new, MobCategory.MISC)
					.noLootTable()
					.sized(1.0F, 0.2F)
					.clientTrackingRange(6)
					.updateInterval(20));

	/** Crypt skeleton with a shield and a sword, a tenth smaller than the Fallen Lord; spawned by green-wool trial spawners. */
	public static final EntityType<SkeletonKnight> SKELETON_KNIGHT = register("skeleton_knight",
			EntityType.Builder.of(SkeletonKnight::new, MobCategory.MONSTER)
					.sized(0.63F, 2.16F)
					.eyeHeight(1.89F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** One of the Lost: an underworld corpse in worn ancient metal, 2.25 blocks tall, roaming in squads. */
	public static final EntityType<Lost> LOST = register("lost",
			EntityType.Builder.of(Lost::new, MobCategory.MONSTER)
					.sized(0.65F, 2.25F)
					.eyeHeight(1.97F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/**
	 * A heavy Lost cavalryman: one of the Lost in full plate with lance and shield, a tenth smaller than the skeleton
	 * knight; he spawns on his armored horse with a partner.
	 */
	public static final EntityType<LostCavalryman> LOST_CAVALRYMAN = register("lost_cavalryman",
			EntityType.Builder.of(LostCavalryman::new, MobCategory.MONSTER)
					.sized(0.57F, 1.94F)
					.eyeHeight(1.7F)
					.ridingOffset(-0.7F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** The heavy cavalryman's horse: an undead horse in plate barding over chainmail. */
	public static final EntityType<LostWarhorse> LOST_WARHORSE = register("lost_warhorse",
			EntityType.Builder.of(LostWarhorse::new, MobCategory.MONSTER)
					.sized(1.3964844F, 1.6F)
					.eyeHeight(1.52F)
					.passengerAttachments(1.31875F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** A withered body in a monk's robe that hides in swamp mud; hunched on all fours. */
	public static final EntityType<SwampLurker> SWAMP_LURKER = register("swamp_lurker",
			EntityType.Builder.of(SwampLurker::new, MobCategory.MONSTER)
					.sized(0.8F, 1.3F)
					.eyeHeight(1.1F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A gaunt, hunched stalker in rags that hunts players unseen in swamps and jungles. */
	public static final EntityType<Wight> WIGHT = register("wight",
			EntityType.Builder.of(Wight::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.55F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** MITE's wood spider: a small bark-brown spider from the tree crowns, 0.6 the size of a spider. */
	public static final EntityType<WoodSpider> WOOD_SPIDER = register("wood_spider",
			EntityType.Builder.of(WoodSpider::new, MobCategory.MONSTER)
					.sized(0.84F, 0.54F)
					.eyeHeight(0.39F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** MITE's black widow: a wood spider in black with a poisonous bite. */
	public static final EntityType<BlackWidow> BLACK_WIDOW = register("black_widow",
			EntityType.Builder.of(BlackWidow::new, MobCategory.MONSTER)
					.sized(0.84F, 0.54F)
					.eyeHeight(0.39F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A blood-sucking underworld bat, a third bigger than a cave bat; flocks of them hunt players. */
	public static final EntityType<VampireBat> VAMPIRE_BAT = register("vampire_bat",
			EntityType.Builder.of(VampireBat::new, MobCategory.MONSTER)
					.sized(0.65F, 1.15F)
					.eyeHeight(0.6F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** One of the Lost who kept his mind: a caster in a black torn robe and cloak, as tall as the Lost; walks with their squads. */
	public static final EntityType<UndeadWarlock> UNDEAD_WARLOCK = register("undead_warlock",
			EntityType.Builder.of(UndeadWarlock::new, MobCategory.MONSTER)
					.sized(0.65F, 2.25F)
					.eyeHeight(1.97F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** The undead warlock's red homing missile. */
	public static final EntityType<MagicMissile> MAGIC_MISSILE = register("magic_missile",
			EntityType.Builder.<MagicMissile>of(MagicMissile::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.3125F, 0.3125F)
					.clientTrackingRange(8)
					.updateInterval(2));

	/** The first boss: a giant in black plate with two axes, two and a half blocks tall. */
	public static final EntityType<PillagerKing> PILLAGER_KING = register("pillager_king",
			EntityType.Builder.of(PillagerKing::new, MobCategory.MONSTER)
					.sized(0.8F, 2.5F)
					.eyeHeight(2.2F)
					.clientTrackingRange(10));

	/** The Pillager King's thrown axe. */
	public static final EntityType<ThrownKingAxe> KING_AXE = register("king_axe",
			EntityType.Builder.<ThrownKingAxe>of(ThrownKingAxe::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** A pillager in a helmet and cuirass with an axe: the king's onslaught, weakened, and a thrown axe. */
	public static final EntityType<PillagerGuard> PILLAGER_GUARD = register("pillager_guard",
			EntityType.Builder.of(PillagerGuard::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.passengerAttachments(2.0F)
					.ridingOffset(-0.6F)
					.clientTrackingRange(8));

	/** A pillager guard's thrown axe. */
	public static final EntityType<ThrownKingAxe> GUARD_AXE = register("guard_axe",
			EntityType.Builder.<ThrownKingAxe>of(ThrownKingAxe::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.clientTrackingRange(4)
					.updateInterval(10));

	private EamEntities() {
	}

	public static void register() {
		// Loads the class, which registers the entity types above.
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Eam.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
