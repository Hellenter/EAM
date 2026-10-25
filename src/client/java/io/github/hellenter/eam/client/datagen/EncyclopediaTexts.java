package io.github.hellenter.eam.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

/** The encyclopedia's pages in both languages: a stage or boss each, its list name, subtitle and text (paragraphs split by newlines). */
final class EncyclopediaTexts {
	private EncyclopediaTexts() {
	}

	private static void page(FabricLanguageProvider.TranslationBuilder builder, String key, String name, String subtitle,
			String... paragraphs) {
		builder.add("encyclopedia.eam." + key + ".name", name);
		builder.add("encyclopedia.eam." + key + ".subtitle", subtitle);
		builder.add("encyclopedia.eam." + key + ".text", String.join("\n", paragraphs));
	}

	private static void boss(FabricLanguageProvider.TranslationBuilder builder, String key, String order, String name,
			String subtitle, String... paragraphs) {
		builder.add("encyclopedia.eam." + key + ".order", order);
		page(builder, key, name, subtitle, paragraphs);
	}

	static void english(FabricLanguageProvider.TranslationBuilder builder) {
		builder.add("key.category.eam.eam", "EAM");
		builder.add("key.eam.encyclopedia", "Encyclopedia");
		builder.add("gui.eam.encyclopedia.title", "Encyclopedia");
		builder.add("gui.eam.encyclopedia.open", "Encyclopedia (%s)");
		builder.add("gui.eam.encyclopedia.bosses", "Bosses, in order");
		builder.add("gui.eam.encyclopedia.reached", "✔ reached");

		page(builder, "stone_age", "Stone Age", "Tier 1: flint",
				"You start with 3 hearts, bare hands and nothing else. Wood and stone will not break by hand: pick up pebbles, sticks and flint shards lying in forests and on mountains, and dig gravel for more shards. Four shards make a flint.",
				"A flint knife comes first: the crafting table works only with a knife in one of its slots. Then a flint hatchet and axe for wood. Crafting takes time, and the table is faster than the 2x2 grid.",
				"Three sticks make a camp fire: cook food and boil water on it. Eight clay balls around a camp fire make a clay oven for food. A cauldron of water on a fire boils: drink from it with an empty hand. A leather flask carries water; a filter keeps it clean. Watch thirst and the three nutrients in the inventory.");
		page(builder, "copper_silver", "Copper and Silver", "Tier 2: copper = silver",
				"Gravel and the sieve give copper nuggets: nine make an ingot. Metal cannot be crafted in the 2x2 grid; it needs the table with a flint knife.",
				"Copper or silver tools mine iron ore. Knives, saws and hammers in the table's slots speed crafting up. The copper and silver anvils repair and forge tier 2 gear.",
				"Silver is found as ore. Its tools hurt the undead more, and the silver brewing stand makes holy water and EAM potions.");
		page(builder, "iron", "Iron", "Tier 3: iron",
				"Iron smelts only in a blast furnace, made from copper or silver ingots. A hot iron ingot is forged into a plate on the iron anvil (the vanilla anvil).",
				"Iron armor is made from plates and ingots. A full iron set earns Iron Age: from then on villages generate alive and can be traded with, guarded and built up.",
				"Pillager outposts may hold a map to the Pillager King's castle: he is the first boss.");
		page(builder, "ancient_metal", "Ancient Metal", "Tier 3+: stronger than iron",
				"Ancient metal has no ore. It comes from crypts: the skeletons there wear ancient chainmail and carry ancient swords, and crypt chests hold more.",
				"It works on iron's tier (copper or silver knife, iron anvil) but lasts twice as long. The King's crypt map or a fermented ender eye leads to a crypt.");
		page(builder, "mithril", "Mithril", "Tier 4: mithril",
				"Mithril ore lies deep, below y -32, and is richer in mountains, swamps and the Underworld. It needs an iron pickaxe.",
				"Obsidian is very hard now and also needs iron. Mithril smelts only in the obsidian blast furnace (iron plates, a furnace and obsidian). Hot mithril is forged on the mithril anvil; crafting with it needs an iron knife.",
				"The Fallen Lord's armor is mithril too.");
		page(builder, "underworld", "Underworld", "A dimension below the world",
				"Light an obsidian portal with the Fallen Lord's black talisman and it leads to the Underworld instead of the Nether. One block there is eight in the overworld.",
				"The upper level has five biomes: lava caves, mushroom forest, stone wastes, lake caves and ice caves. Duergar villages, mines, the Lost and other dwellers live there.",
				"A band of deep bedrock divides the upper level from the depths: only Koschei's needle bores through it. The depths hold adamantium, more mithril and the only portal to the Nether.");
		page(builder, "adamantium", "Adamantium", "Tier 5: adamantium",
				"Adamantium ore is found only in the Underworld depths and needs a mithril pickaxe. It smelts in the obsidian blast furnace and is forged on the adamantium anvil; crafting with it needs a mithril knife.",
				"Adamantium armor keeps its full protection. Netherite is an upgrade of adamantium gear at the smithing table and is repaired only on the adamantium anvil.");

		boss(builder, "pillager_king", "1.", "Pillager King", "Boss 1 of 3",
				"Lives in his castle, guarded by pillagers and elite crossbowmen. Outposts may hold the castle map.",
				"Two axes, combos and charges. Parry when his eyes turn yellow and the bell rings; red eyes warn of a charge or a heavy rush.",
				"Drops: his helm (a trophy that keeps raids away), a map to the nearest crypt, emeralds and diamonds.");
		boss(builder, "fallen_lord", "2.", "Fallen Lord", "Boss 2 of 3",
				"Waits in the boss hall of a crypt. The King's crypt map or a fermented ender eye shows the way.",
				"Sword, shield and spear. He parries back, knocks shields out and drinks to heal. Strike when he is open, parry on the bell.",
				"Drops: the black talisman that opens the way to the Underworld, enchanted mithril armor and a great burst of experience.");
		boss(builder, "koschei", "3.", "Koschei the Deathless", "Boss 3 of 3",
				"The lord of the Underworld. His lair is not placed in the world yet: it will stand in the ice caves.",
				"Phase one: sword, lightning and ravens; parry the lunge and the overhead blow. Phase two: the staff, thralls, ice and storms.",
				"Drops: Koschei's needle. Thrown, it bores a shaft down through deep bedrock to the depths.");
		boss(builder, "baba_yaga", "★", "Baba Yaga", "Optional boss",
				"Lives in a hut in the middle of a swampy forest. Not needed to progress.",
				"First she flies in her mortar and raises the swamp's dead; parry her dive. Then she fights on foot with acid, threads, fire and claws.",
				"Drops: a Mending book, ash and a map to her grave with a rich chest.");
	}

	static void russian(FabricLanguageProvider.TranslationBuilder builder) {
		builder.add("key.category.eam.eam", "EAM");
		builder.add("key.eam.encyclopedia", "Энциклопедия");
		builder.add("gui.eam.encyclopedia.title", "Энциклопедия");
		builder.add("gui.eam.encyclopedia.open", "Энциклопедия (%s)");
		builder.add("gui.eam.encyclopedia.bosses", "Боссы по порядку");
		builder.add("gui.eam.encyclopedia.reached", "✔ пройдено");

		page(builder, "stone_age", "Каменный век", "Тир 1: кремень",
				"Ты начинаешь с 3 сердцами и пустыми руками. Дерево и камень руками не сломать: собирай камешки, палки и осколки кремня в лесах и горах, копай гравий ради осколков. Из четырёх осколков выходит кремень.",
				"Первым делом кремнёвый нож: верстак работает только с ножом в одном из слотов. Потом кремнёвый топорик и топор для дерева. Крафт занимает время, на верстаке быстрее, чем в сетке 2x2.",
				"Три палки дают костёр: на нём жарят еду и кипятят воду. Восемь комков глины вокруг костра дают глиняную печку для еды. Котёл с водой на огне закипает: из него можно пить пустой рукой. Кожаная фляга носит воду, фильтр делает её чистой. Следи за жаждой и тремя нутриентами в инвентаре.");
		page(builder, "copper_silver", "Медь и серебро", "Тир 2: медь = серебро",
				"Гравий и сито дают медные самородки: из девяти выходит слиток. Металл в сетке 2x2 не крафтится, нужен верстак с кремнёвым ножом.",
				"Медные и серебряные инструменты добывают железную руду. Ножи, пилы и молоты в слотах верстака ускоряют крафт. Медная и серебряная наковальни чинят и куют снаряжение 2 тира.",
				"Серебро добывается из руды. Серебро сильнее бьёт нежить, а серебряная варочная стойка делает святую воду и зелья EAM.");
		page(builder, "iron", "Железо", "Тир 3: железо",
				"Железо плавится только в доменной печи, её делают из медных или серебряных слитков. Раскалённый слиток куётся в пластину на железной наковальне (обычной).",
				"Железная броня делается из пластин и слитков. Полный железный комплект даёт достижение «Железный век»: после него деревни появляются живыми, с торговлей, стражей и стройкой.",
				"В аванпостах разбойников бывает карта к замку Короля разбойников: он первый босс.");
		page(builder, "ancient_metal", "Древний металл", "Тир 3+: крепче железа",
				"У древнего металла нет руды. Он приходит из склепов: скелеты там носят древнюю кольчугу и древние мечи, ещё больше лежит в сундуках.",
				"Работает на тире железа (медный или серебряный нож, железная наковальня), но служит вдвое дольше. К склепу ведут карта склепа от Короля или ферментированное око Края.");
		page(builder, "mithril", "Мифрил", "Тир 4: мифрил",
				"Мифриловая руда лежит глубоко, ниже y -32, больше её в горах, болотах и в Подземье. Нужна железная кирка.",
				"Обсидиан теперь очень твёрдый и тоже требует железа. Мифрил плавится только в обсидиановой доменной печи (железные пластины, печь и обсидиан). Раскалённый мифрил куют на мифриловой наковальне, для крафта нужен железный нож.",
				"Броня Падшего лорда тоже мифриловая.");
		page(builder, "underworld", "Подземье", "Измерение под миром",
				"Зажги обсидиановый портал чёрным талисманом Падшего лорда, и он поведёт в Подземье вместо Незера. Один блок там равен восьми в обычном мире.",
				"На верхнем уровне пять биомов: лавовые пещеры, грибной лес, каменные пустоши, озёрные и ледяные пещеры. Там живут двергары с деревнями и шахтами, Потерянные и другие обитатели.",
				"Слой глубинной коренной породы отделяет верхний уровень от глубин: пробить его может только Кощеева игла. В глубинах адамантий, больше мифрила и единственный путь в Незер.");
		page(builder, "adamantium", "Адамантий", "Тир 5: адамантий",
				"Адамантиевая руда есть только в глубинах Подземья, нужна мифриловая кирка. Плавится в обсидиановой доменной печи, куётся на адамантиевой наковальне, для крафта нужен мифриловый нож.",
				"Адамантиевая броня сохраняет полную защиту. Незерит делается улучшением адамантиевого снаряжения на кузнечном столе и чинится только на адамантиевой наковальне.");

		boss(builder, "pillager_king", "1.", "Король разбойников", "Босс 1 из 3",
				"Живёт в своём замке под охраной разбойников и элитных арбалетчиков. Карта к замку бывает в аванпостах.",
				"Два топора, комбо и рывки. Парируй, когда глаза желтеют и звенит колокол; красные глаза предупреждают о рывке или тяжёлом натиске.",
				"Добыча: его шлем (трофей, отгоняющий набеги), карта к ближайшему склепу, изумруды и алмазы.");
		boss(builder, "fallen_lord", "2.", "Падший лорд", "Босс 2 из 3",
				"Ждёт в зале босса в склепе. Дорогу покажут карта склепа от Короля или ферментированное око Края.",
				"Меч, щит и копьё. Он сам парирует, выбивает щит и пьёт зелье, чтобы лечиться. Бей, когда он открыт, парируй по звону колокола.",
				"Добыча: чёрный талисман, открывающий путь в Подземье, зачарованная мифриловая броня и большой выброс опыта.");
		boss(builder, "koschei", "3.", "Кощей Бессмертный", "Босс 3 из 3",
				"Владыка Подземья. Его логово пока не генерируется в мире: оно встанет в ледяных пещерах.",
				"Первая фаза: меч, молнии и вороны, парируются выпад и удар сверху. Вторая фаза: посох, мертвецы-слуги, лёд и бури.",
				"Добыча: Кощеева игла. Брошенная, она пробивает шахту сквозь глубинную коренную породу до глубин.");
		boss(builder, "baba_yaga", "★", "Баба-Яга", "Необязательный босс",
				"Живёт в избушке посреди болотного леса. Для прохождения не нужна.",
				"Сначала летает в ступе и поднимает мертвецов болота, парируй её пике. Потом бьётся пешком: кислота, нити, огонь и когти.",
				"Добыча: книга «Починка», пепел и карта к её могиле с богатым сундуком.");
	}
}
