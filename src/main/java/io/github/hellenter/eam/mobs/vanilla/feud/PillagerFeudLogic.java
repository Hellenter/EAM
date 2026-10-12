package io.github.hellenter.eam.mobs.vanilla.feud;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;

/** Who counts as an enemy in the war between monsters and pillagers. */
public final class PillagerFeudLogic {
	private PillagerFeudLogic() {
	}

	/** A target of the other side, unless both are raiders and one of them is in a raid (raid witches side with illagers). */
	public static boolean isEnemy(Mob mob, LivingEntity target, TagKey<EntityType<?>> enemies) {
		if (!target.is(enemies)) {
			return false;
		}
		return !(mob instanceof Raider self && target instanceof Raider other && (self.hasActiveRaid() || other.hasActiveRaid()));
	}
}
