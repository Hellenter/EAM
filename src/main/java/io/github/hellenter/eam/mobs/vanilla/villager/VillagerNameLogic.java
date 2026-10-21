package io.github.hellenter.eam.mobs.vanilla.villager;

import java.util.List;

/**
 * The names villagers are given: Slavic first names, a list for men and one for women, and surnames, kept in their
 * masculine form with the English and Russian spelling of each; a woman's surname is made from it.
 */
public final class VillagerNameLogic {
	/** A name: its id in translation keys and saves, and how it is written in English and in Russian. */
	public record Entry(String id, String english, String russian) {
	}

	public static final List<Entry> MALE = List.of(
			new Entry("bogdan", "Bogdan", "Богдан"), new Entry("borislav", "Borislav", "Борислав"),
			new Entry("vadim", "Vadim", "Вадим"), new Entry("vlas", "Vlas", "Влас"),
			new Entry("vsevolod", "Vsevolod", "Всеволод"), new Entry("gleb", "Gleb", "Глеб"),
			new Entry("gordey", "Gordey", "Гордей"), new Entry("danila", "Danila", "Данила"),
			new Entry("dobrynya", "Dobrynya", "Добрыня"), new Entry("yegor", "Yegor", "Егор"),
			new Entry("yeremey", "Yeremey", "Еремей"), new Entry("zakhar", "Zakhar", "Захар"),
			new Entry("ignat", "Ignat", "Игнат"), new Entry("kuzma", "Kuzma", "Кузьма"),
			new Entry("luka", "Luka", "Лука"), new Entry("miron", "Miron", "Мирон"),
			new Entry("mstislav", "Mstislav", "Мстислав"), new Entry("nikita", "Nikita", "Никита"),
			new Entry("ostap", "Ostap", "Остап"), new Entry("pakhom", "Pakhom", "Пахом"),
			new Entry("prokhor", "Prokhor", "Прохор"), new Entry("radim", "Radim", "Радим"),
			new Entry("savva", "Savva", "Савва"), new Entry("svyatoslav", "Svyatoslav", "Святослав"),
			new Entry("stepan", "Stepan", "Степан"), new Entry("timofey", "Timofey", "Тимофей"),
			new Entry("foma", "Foma", "Фома"), new Entry("yaroslav", "Yaroslav", "Ярослав"));

	public static final List<Entry> FEMALE = List.of(
			new Entry("alyona", "Alyona", "Алёна"), new Entry("bozhena", "Bozhena", "Божена"),
			new Entry("varvara", "Varvara", "Варвара"), new Entry("vasilisa", "Vasilisa", "Василиса"),
			new Entry("vera", "Vera", "Вера"), new Entry("darina", "Darina", "Дарина"),
			new Entry("dobrava", "Dobrava", "Добрава"), new Entry("yevdokiya", "Yevdokiya", "Евдокия"),
			new Entry("zabava", "Zabava", "Забава"), new Entry("zlata", "Zlata", "Злата"),
			new Entry("lada", "Lada", "Лада"), new Entry("lyubava", "Lyubava", "Любава"),
			new Entry("lyudmila", "Lyudmila", "Людмила"), new Entry("malusha", "Malusha", "Малуша"),
			new Entry("marfa", "Marfa", "Марфа"), new Entry("milava", "Milava", "Милава"),
			new Entry("nastasya", "Nastasya", "Настасья"), new Entry("praskovya", "Praskovya", "Прасковья"),
			new Entry("ruslana", "Ruslana", "Руслана"), new Entry("snezhana", "Snezhana", "Снежана"),
			new Entry("ulyana", "Ulyana", "Ульяна"), new Entry("fyokla", "Fyokla", "Фёкла"),
			new Entry("yarina", "Yarina", "Ярина"), new Entry("agafya", "Agafya", "Агафья"));

	/** Surnames in their masculine form. */
	public static final List<Entry> SURNAMES = List.of(
			new Entry("kuznetsov", "Kuznetsov", "Кузнецов"), new Entry("goncharov", "Goncharov", "Гончаров"),
			new Entry("bondarev", "Bondarev", "Бондарев"), new Entry("melnikov", "Melnikov", "Мельников"),
			new Entry("plotnikov", "Plotnikov", "Плотников"), new Entry("rybakov", "Rybakov", "Рыбаков"),
			new Entry("pastukhov", "Pastukhov", "Пастухов"), new Entry("kozhevnikov", "Kozhevnikov", "Кожевников"),
			new Entry("bortnikov", "Bortnikov", "Бортников"), new Entry("ovchinnikov", "Ovchinnikov", "Овчинников"),
			new Entry("kolesnikov", "Kolesnikov", "Колесников"), new Entry("sapozhnikov", "Sapozhnikov", "Сапожников"),
			new Entry("sokolov", "Sokolov", "Соколов"), new Entry("volkov", "Volkov", "Волков"),
			new Entry("medvedev", "Medvedev", "Медведев"), new Entry("zaytsev", "Zaytsev", "Зайцев"),
			new Entry("gvozdev", "Gvozdev", "Гвоздев"), new Entry("kamenev", "Kamenev", "Каменев"),
			new Entry("lisitsyn", "Lisitsyn", "Лисицын"), new Entry("voronin", "Voronin", "Воронин"),
			new Entry("berezin", "Berezin", "Березин"), new Entry("dubrovin", "Dubrovin", "Дубровин"),
			new Entry("belozersky", "Belozersky", "Белозерский"), new Entry("ozersky", "Ozersky", "Озерский"),
			new Entry("zhuravlyov", "Zhuravlyov", "Журавлёв"), new Entry("lebedev", "Lebedev", "Лебедев"));

	private VillagerNameLogic() {
	}

	/** The first names of men or of women. */
	public static List<Entry> given(boolean female) {
		return female ? FEMALE : MALE;
	}

	/**
	 * The feminine form of a masculine surname, in either spelling: Kuznetsov becomes Kuznetsova, Belozersky
	 * Belozerskaya, Белозерский Белозерская.
	 */
	public static String feminine(String surname) {
		if (surname.endsWith("sky")) return surname.substring(0, surname.length() - 1) + "aya";
		if (surname.endsWith("ский")) return surname.substring(0, surname.length() - 2) + "ая";
		boolean cyrillic = !surname.isEmpty() && Character.UnicodeBlock.of(surname.charAt(surname.length() - 1)) == Character.UnicodeBlock.CYRILLIC;
		return surname + (cyrillic ? "а" : "a");
	}
}
