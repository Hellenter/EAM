package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;

/**
 * Every villager's first name and surname, picked at random from {@link VillagerNameLogic} the first time the server
 * needs it and kept with him. A name a player gives him with a name tag is shown instead.
 */
public final class VillagerNames {
	public static final AttachmentType<Name> NAME = AttachmentRegistry.create(Eam.id("villager_name"),
			builder -> builder.persistent(Name.CODEC));

	private VillagerNames() {
	}

	/** Loads the class, so the name attachment is registered before any villager is loaded. */
	static void register() {
	}

	/** A villager's name: the ids of his first name and surname, and whether she is a woman. */
	public record Name(String given, String surname, boolean female) {
		static final Codec<Name> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("given").forGetter(Name::given),
				Codec.STRING.fieldOf("surname").forGetter(Name::surname),
				Codec.BOOL.fieldOf("female").forGetter(Name::female)
		).apply(instance, Name::new));

		/** The name, spelled in the reader's language. */
		public Component component() {
			return Component.translatable("villager_name.eam.full",
					Component.translatableWithFallback(givenKey(given), given),
					Component.translatableWithFallback(surnameKey(surname, female), surname));
		}
	}

	public static String givenKey(String id) {
		return "villager_name.eam.given." + id;
	}

	public static String surnameKey(String id, boolean female) {
		return "villager_name.eam.surname." + id + (female ? ".female" : ".male");
	}

	/** The name the villager goes by: the one a player gave him, or his own, given now if he has none yet. */
	public static Component name(Villager villager) {
		if (villager.getCustomName() != null) return villager.getCustomName();
		Name name = villager.getAttached(NAME);
		if (name == null) {
			name = random(villager.getRandom());
			villager.setAttached(NAME, name);
		}
		return name.component();
	}

	private static Name random(RandomSource random) {
		boolean female = random.nextBoolean();
		List<VillagerNameLogic.Entry> given = VillagerNameLogic.given(female);
		List<VillagerNameLogic.Entry> surnames = VillagerNameLogic.SURNAMES;
		return new Name(given.get(random.nextInt(given.size())).id(), surnames.get(random.nextInt(surnames.size())).id(), female);
	}
}
