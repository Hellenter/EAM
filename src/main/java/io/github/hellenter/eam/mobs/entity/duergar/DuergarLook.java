package io.github.hellenter.eam.mobs.entity.duergar;

/** What the duergar model needs from any kind of duergar: his gear and which of his kind's skins he has. */
public interface DuergarLook {
	/** The gear, beard and hair he wears, packed as {@link DuergarGear} packs them. */
	int gear();

	/** Which of his kind's skins. */
	int skin();
}
