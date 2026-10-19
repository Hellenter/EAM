package io.github.hellenter.eam.mobs.entity.duergar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the duergar ram riders, read from {@code data/eam/mobs/duergar_ram_rider.json} so datapacks can
 * override them. A missing or invalid field falls back to its default. Where squads spawn is the biome tag
 * {@code eam:has_ram_riders}; the ram itself is {@code cave_ram.json}, and how long a net holds is {@code net.seconds}
 * in {@code elite_crossbowman.json}, the same net.
 *
 * @param health             a rider's health
 * @param leaderHealth       the leader's health
 * @param speed              movement speed attribute on foot (a zombie's is 0.23)
 * @param attackDamage       bare-handed damage; his tomahawk adds its own
 * @param extraArmor         armor on top of what his pieces give
 * @param minFollowers       fewest riders a leader brings
 * @param maxFollowers       most riders a leader brings
 * @param spawnSpacing       no squad spawns within this many blocks of another rider or renegade
 * @param baldChance         chance he is bald
 * @param tomahawkDropChance chance his tomahawk drops
 * @param mountedSpeed       speed multiplier of his ram while he rides at the player
 * @param mountedKnockback   extra knockback of a blow struck from a charging ram
 * @param tomahawk           the thrown tomahawk
 * @param net                the thrown net
 */
public record RamRiderSettings(double health, double leaderHealth, double speed, double attackDamage, double extraArmor, int minFollowers,
		int maxFollowers, double spawnSpacing, double baldChance, float tomahawkDropChance, double mountedSpeed, float mountedKnockback,
		Tomahawk tomahawk, Net net) {
	public static final RamRiderSettings DEFAULT = new RamRiderSettings(16.0, 22.0, 0.23, 1.0, 2.0, 2, 3, 64.0, 0.5, 0.08F, 1.45, 1.0F,
			Tomahawk.DEFAULT, Net.DEFAULT);

	public static final Codec<RamRiderSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 1024.0).fieldOf("health").orElse(DEFAULT.health).forGetter(RamRiderSettings::health),
			Codec.doubleRange(1.0, 1024.0).fieldOf("leader_health").orElse(DEFAULT.leaderHealth).forGetter(RamRiderSettings::leaderHealth),
			Codec.doubleRange(0.0, 1.0).fieldOf("speed").orElse(DEFAULT.speed).forGetter(RamRiderSettings::speed),
			Codec.doubleRange(0.0, 100.0).fieldOf("attack_damage").orElse(DEFAULT.attackDamage).forGetter(RamRiderSettings::attackDamage),
			Codec.doubleRange(0.0, 30.0).fieldOf("extra_armor").orElse(DEFAULT.extraArmor).forGetter(RamRiderSettings::extraArmor),
			Codec.intRange(0, 10).fieldOf("min_followers").orElse(DEFAULT.minFollowers).forGetter(RamRiderSettings::minFollowers),
			Codec.intRange(0, 10).fieldOf("max_followers").orElse(DEFAULT.maxFollowers).forGetter(RamRiderSettings::maxFollowers),
			Codec.doubleRange(0.0, 256.0).fieldOf("spawn_spacing").orElse(DEFAULT.spawnSpacing).forGetter(RamRiderSettings::spawnSpacing),
			Codec.doubleRange(0.0, 1.0).fieldOf("bald_chance").orElse(DEFAULT.baldChance).forGetter(RamRiderSettings::baldChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("tomahawk_drop_chance").orElse(DEFAULT.tomahawkDropChance)
					.forGetter(RamRiderSettings::tomahawkDropChance),
			Codec.doubleRange(0.1, 4.0).fieldOf("mounted_speed").orElse(DEFAULT.mountedSpeed).forGetter(RamRiderSettings::mountedSpeed),
			Codec.floatRange(0.0F, 5.0F).fieldOf("mounted_knockback").orElse(DEFAULT.mountedKnockback)
					.forGetter(RamRiderSettings::mountedKnockback),
			Tomahawk.CODEC.fieldOf("tomahawk").orElse(DEFAULT.tomahawk).forGetter(RamRiderSettings::tomahawk),
			Net.CODEC.fieldOf("net").orElse(DEFAULT.net).forGetter(RamRiderSettings::net)
	).apply(instance, RamRiderSettings::new));

	/**
	 * The thrown tomahawk. He has spares on his belt: a throw never leaves him empty-handed, and a thrown one cannot be
	 * picked up.
	 *
	 * @param chance        chance he throws when he thinks of it (once a second) and the target is in range
	 * @param cooldownTicks ticks before he throws again
	 * @param minDistance   he throws only at a target at least this far
	 * @param maxDistance   and at most this far
	 * @param damage        damage of a hit
	 * @param speed         speed of the throw, blocks per tick
	 */
	public record Tomahawk(float chance, int cooldownTicks, double minDistance, double maxDistance, float damage, float speed) {
		public static final Tomahawk DEFAULT = new Tomahawk(0.5F, 60, 4.0, 16.0, 5.0F, 1.3F);

		public static final Codec<Tomahawk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Tomahawk::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown_ticks").orElse(DEFAULT.cooldownTicks).forGetter(Tomahawk::cooldownTicks),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Tomahawk::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Tomahawk::maxDistance),
				Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(Tomahawk::damage),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Tomahawk::speed)
		).apply(instance, Tomahawk::new));
	}

	/**
	 * The thrown net: the elite crossbowman's net, cast by hand. It leaves a net at the feet of whoever it hits, which
	 * slows him until he cuts it or it falls apart.
	 *
	 * @param chance             chance he casts when he thinks of it and the target is in range
	 * @param cooldownTicks      ticks before he casts again
	 * @param squadCooldownTicks ticks the rest of his squad waits after his cast, so they do not all net at once
	 * @param minDistance        he casts only at a target at least this far
	 * @param maxDistance        and at most this far
	 * @param speed              speed of the cast, blocks per tick
	 */
	public record Net(float chance, int cooldownTicks, int squadCooldownTicks, double minDistance, double maxDistance, float speed) {
		public static final Net DEFAULT = new Net(0.25F, 400, 160, 0.0, 10.0, 1.1F);

		public static final Codec<Net> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(Net::chance),
				Codec.intRange(0, 2400).fieldOf("cooldown_ticks").orElse(DEFAULT.cooldownTicks).forGetter(Net::cooldownTicks),
				Codec.intRange(0, 2400).fieldOf("squad_cooldown_ticks").orElse(DEFAULT.squadCooldownTicks).forGetter(Net::squadCooldownTicks),
				Codec.doubleRange(0.0, 64.0).fieldOf("min_distance").orElse(DEFAULT.minDistance).forGetter(Net::minDistance),
				Codec.doubleRange(0.0, 64.0).fieldOf("max_distance").orElse(DEFAULT.maxDistance).forGetter(Net::maxDistance),
				Codec.floatRange(0.1F, 5.0F).fieldOf("speed").orElse(DEFAULT.speed).forGetter(Net::speed)
		).apply(instance, Net::new));
	}
}
