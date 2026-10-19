package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.ground.ThrownPebble;
import io.github.hellenter.eam.mobs.entity.bat.VampireBat;
import io.github.hellenter.eam.mobs.entity.boar.Boar;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowman;
import io.github.hellenter.eam.mobs.entity.crossbowman.NetShot;
import io.github.hellenter.eam.mobs.vanilla.evolution.WebShot;
import io.github.hellenter.eam.mobs.entity.crossbowman.RedBolt;
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
import io.github.hellenter.eam.mobs.entity.lost.LostCenturion;
import io.github.hellenter.eam.mobs.entity.lost.LostWallCommander;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurker;
import io.github.hellenter.eam.mobs.entity.spider.BlackWidow;
import io.github.hellenter.eam.mobs.entity.spider.WoodSpider;
import io.github.hellenter.eam.mobs.entity.warlock.MagicMissile;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;
import io.github.hellenter.eam.mobs.entity.upyr.Upyr;
import io.github.hellenter.eam.mobs.entity.warg.Warg;
import io.github.hellenter.eam.mobs.entity.wight.Wight;
import io.github.hellenter.eam.mobs.entity.draugr.Draugr;
import io.github.hellenter.eam.mobs.entity.drowned.DrownedSoldier;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarBomb;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarRenegade;
import io.github.hellenter.eam.mobs.entity.duergar.DuergarVillager;
import io.github.hellenter.eam.mobs.entity.duergar.ZombieDuergar;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYaga;
import io.github.hellenter.eam.mobs.entity.koschei.Koschei;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiBallLightning;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiIceCopy;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiNeedle;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiIceRaven;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiOrb;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiRaven;
import io.github.hellenter.eam.mobs.entity.yaga.HagBolt;
import io.github.hellenter.eam.mobs.entity.yaga.HagGlyph;
import io.github.hellenter.eam.mobs.entity.yaga.HagPotion;
import io.github.hellenter.eam.mobs.entity.yaga.HagThread;

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
	 * The Lost centurion: one of the Lost in battered full plate with a red crest, a long bannered spear, a shield and
	 * a horn that calls up more of them.
	 */
	public static final EntityType<LostCenturion> LOST_CENTURION = register("lost_centurion",
			EntityType.Builder.of(LostCenturion::new, MobCategory.MONSTER)
					.sized(0.65F, 2.25F)
					.eyeHeight(1.97F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** The commander of a shield wall: one of the Lost with a crossbow, who brings four spearmen with shields. */
	public static final EntityType<LostWallCommander> LOST_WALL_COMMANDER = register("lost_wall_commander",
			EntityType.Builder.of(LostWallCommander::new, MobCategory.MONSTER)
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

	/** A draugr: a dead warrior in what is left of his armor, frozen into the floor of the underworld's ice caves. */
	public static final EntityType<Draugr> DRAUGR = register("draugr",
			EntityType.Builder.of(Draugr::new, MobCategory.MONSTER)
					.sized(0.65F, 2.1F)
					.eyeHeight(1.82F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A zombie duergar: a dead grey dwarf miner in battered gear who haunts the underworld's old mines. */
	public static final EntityType<ZombieDuergar> ZOMBIE_DUERGAR = register("zombie_duergar",
			EntityType.Builder.of(ZombieDuergar::new, MobCategory.MONSTER)
					.sized(0.8F, 1.4F)
					.eyeHeight(1.15F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A duergar renegade: a living grey dwarf in heavy armor who roams the dark underworld with his squad. */
	public static final EntityType<DuergarRenegade> DUERGAR_RENEGADE = register("duergar_renegade",
			EntityType.Builder.of(DuergarRenegade::new, MobCategory.MONSTER)
					.sized(0.8F, 1.4F)
					.eyeHeight(1.15F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A duergar renegade's lit bomb, lobbed at the player. */
	public static final EntityType<DuergarBomb> DUERGAR_BOMB = register("duergar_bomb",
			EntityType.Builder.<DuergarBomb>of(DuergarBomb::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.35F, 0.35F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** A duergar of a clan village: a smith, a miner or the elder, who trade mithril for jewels. */
	public static final EntityType<DuergarVillager> DUERGAR_VILLAGER = register("duergar_villager",
			EntityType.Builder.of(DuergarVillager::new, MobCategory.MISC)
					.sized(0.8F, 1.4F)
					.eyeHeight(1.15F)
					.clientTrackingRange(10));

	/** A gaunt, hunched stalker in rags that hunts players unseen in swamps and jungles. */
	public static final EntityType<Wight> WIGHT = register("wight",
			EntityType.Builder.of(Wight::new, MobCategory.MONSTER)
					.sized(0.6F, 1.8F)
					.eyeHeight(1.55F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** A bloodsucking corpse in a burial shirt that crawls out at night by abandoned villages. */
	public static final EntityType<Upyr> UPYR = register("upyr",
			EntityType.Builder.of(Upyr::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.eyeHeight(1.74F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A warg: a big, shaggy grey wolf that hunts in packs in the snowy taiga at night. */
	public static final EntityType<Warg> WARG = register("warg",
			EntityType.Builder.of(Warg::new, MobCategory.MONSTER)
					.sized(0.85F, 1.05F)
					.eyeHeight(0.85F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** The leader of a warg pack: bigger and darker. Kill him and the pack runs. */
	public static final EntityType<Warg> WARG_LEADER = register("warg_leader",
			EntityType.Builder.of(Warg::new, MobCategory.MONSTER)
					.sized(1.0F, 1.25F)
					.eyeHeight(1.0F)
					.clientTrackingRange(10)
					.notInPeaceful());

	/** An underground boar: a brown, tusked wild pig of the underworld's lake caves and forests, a bit bigger than a pig. */
	public static final EntityType<Boar> BOAR = register("boar",
			EntityType.Builder.of(Boar::new, MobCategory.CREATURE)
					.sized(1.0F, 1.0F)
					.eyeHeight(0.8F)
					.clientTrackingRange(10));

	/** Baba Yaga: a hunched old witch with a staff, flying in her mortar until it breaks. Fire does not touch her in it. */
	public static final EntityType<BabaYaga> BABA_YAGA = register("baba_yaga",
			EntityType.Builder.of(BabaYaga::new, MobCategory.MONSTER)
					.sized(0.95F, 2.1F)
					.eyeHeight(1.85F)
					.clientTrackingRange(10));

	/** Koschei the Deathless: a gaunt old man in a spiked crown and dark armor, two and a half blocks tall, with a long sword. */
	public static final EntityType<Koschei> KOSCHEI = register("koschei",
			EntityType.Builder.of(Koschei::new, MobCategory.MONSTER)
					.sized(0.8F, 2.5F)
					.fireImmune()
					.eyeHeight(2.2F)
					.clientTrackingRange(10));

	/** One of the see-through black ravens Koschei calls up, flying through walls at his target. */
	public static final EntityType<KoscheiRaven> KOSCHEI_RAVEN = register("koschei_raven",
			EntityType.Builder.of(KoscheiRaven::new, MobCategory.MONSTER)
					.sized(0.5F, 0.4F)
					.fireImmune()
					.eyeHeight(0.25F)
					.clientTrackingRange(8));

	/** Koschei's ball of lightning, drifting after his target. */
	public static final EntityType<KoscheiBallLightning> KOSCHEI_BALL_LIGHTNING = register("koschei_ball_lightning",
			EntityType.Builder.of(KoscheiBallLightning::new, MobCategory.MONSTER)
					.sized(0.8F, 0.8F)
					.fireImmune()
					.noSave()
					.clientTrackingRange(10));

	/** A raven of ice Koschei looses at his target. */
	public static final EntityType<KoscheiIceRaven> KOSCHEI_ICE_RAVEN = register("koschei_ice_raven",
			EntityType.Builder.of(KoscheiIceRaven::new, MobCategory.MONSTER)
					.sized(0.5F, 0.4F)
					.fireImmune()
					.noSave()
					.eyeHeight(0.25F)
					.clientTrackingRange(8));

	/** A copy of Koschei in ice, standing up at his side in his second phase. */
	public static final EntityType<KoscheiIceCopy> KOSCHEI_ICE_COPY = register("koschei_ice_copy",
			EntityType.Builder.of(KoscheiIceCopy::new, MobCategory.MONSTER)
					.sized(0.8F, 2.5F)
					.fireImmune()
					.noSave()
					.eyeHeight(2.2F)
					.clientTrackingRange(10));

	/** Koschei's needle, used: it spins in a pillar of light, bores a shaft to the bedrock and bursts. */
	public static final EntityType<KoscheiNeedle> KOSCHEI_NEEDLE = register("koschei_needle",
			EntityType.Builder.<KoscheiNeedle>of(KoscheiNeedle::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.4F, 0.4F)
					.fireImmune()
					.noSave()
					.clientTrackingRange(16)
					.updateInterval(1));

	/** A bolt or a soul thrown from Koschei's staff in his second phase. */
	public static final EntityType<KoscheiOrb> KOSCHEI_ORB = register("koschei_orb",
			EntityType.Builder.<KoscheiOrb>of(KoscheiOrb::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.3125F, 0.3125F)
					.clientTrackingRange(8)
					.updateInterval(2));

	/** A bloated corpse in a rusted soldier's kit, raised from a bog by Baba Yaga. */
	public static final EntityType<DrownedSoldier> DROWNED_SOLDIER = register("drowned_soldier",
			EntityType.Builder.of(DrownedSoldier::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.eyeHeight(1.74F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** A glowing glyph Baba Yaga lays on the ground, or her acid puddle. */
	public static final EntityType<HagGlyph> HAG_GLYPH = register("hag_glyph",
			EntityType.Builder.<HagGlyph>of(HagGlyph::new, MobCategory.MISC)
					.noLootTable()
					.noSave()
					.sized(0.5F, 0.1F)
					.clientTrackingRange(10)
					.updateInterval(1));

	/** Baba Yaga's poison flask. */
	public static final EntityType<HagPotion> HAG_POTION = register("hag_potion",
			EntityType.Builder.<HagPotion>of(HagPotion::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.25F, 0.25F)
					.clientTrackingRange(8)
					.updateInterval(10));

	/** Baba Yaga's acid and purple bolts. */
	public static final EntityType<HagBolt> HAG_BOLT = register("hag_bolt",
			EntityType.Builder.<HagBolt>of(HagBolt::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.3125F, 0.3125F)
					.clientTrackingRange(8)
					.updateInterval(2));

	/** A purple thread out of the ground that holds a player by the legs. */
	public static final EntityType<HagThread> HAG_THREAD = register("hag_thread",
			EntityType.Builder.of(HagThread::new, MobCategory.MISC)
					.noLootTable()
					.noSave()
					.sized(0.45F, 1.1F)
					.clientTrackingRange(8));

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

	/** A pillager in a cuirass and a round helmet with a few tricks for his crossbow. */
	public static final EntityType<EliteCrossbowman> ELITE_CROSSBOWMAN = register("elite_crossbowman",
			EntityType.Builder.of(EliteCrossbowman::new, MobCategory.MONSTER)
					.canSpawnFarFromPlayer()
					.sized(0.6F, 1.95F)
					.passengerAttachments(2.0F)
					.ridingOffset(-0.6F)
					.clientTrackingRange(8)
					.notInPeaceful());

	/** The elite crossbowman's charged red bolt. */
	public static final EntityType<RedBolt> RED_BOLT = register("red_bolt",
			EntityType.Builder.<RedBolt>of(RedBolt::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.5F, 0.5F)
					.eyeHeight(0.13F)
					.clientTrackingRange(4)
					.updateInterval(20));

	/** The elite crossbowman's net in flight. */
	public static final EntityType<NetShot> NET_SHOT = register("net_shot",
			EntityType.Builder.<NetShot>of(NetShot::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.4F, 0.4F)
					.clientTrackingRange(4)
					.updateInterval(10));

	/** An evolved spider's web in flight. */
	public static final EntityType<WebShot> WEB_SHOT = register("web_shot",
			EntityType.Builder.<WebShot>of(WebShot::new, MobCategory.MISC)
					.noLootTable()
					.sized(0.3F, 0.3F)
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
