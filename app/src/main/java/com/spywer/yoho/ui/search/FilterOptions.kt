package com.spywer.yoho.ui.search

import com.spywer.yoho.data.remote.FacetItemDto

/** Группы меню фильтра — как на reyoho.com/search. */
enum class FilterGroup(val title: String) {
    Basic("Основное"),
    People("Люди"),
    Content("Содержание"),
    Fame("Известность"),
    Production("Производство"),
    Collections("Подборки"),
    Order("Порядок"),
}

/** Полный набор категорий как на reyoho.com/search → «Фильтр». */
enum class FilterCategory(val title: String, val group: FilterGroup) {
    Genre("Жанр", FilterGroup.Basic),
    Country("Страна", FilterGroup.Basic),
    Year("Год", FilterGroup.Basic),
    Rating("Рейтинг", FilterGroup.Basic),
    Actors("Актёры", FilterGroup.People),
    Crew("Съёмочная группа", FilterGroup.People),
    ExcludePeople("Без участия", FilterGroup.People),
    Tags("Теги", FilterGroup.Content),
    Language("Язык оригинала", FilterGroup.Content),
    Region("Регион", FilterGroup.Content),
    Age("Возраст", FilterGroup.Content),
    Duration("Длительность", FilterGroup.Content),
    Votes("Число оценок", FilterGroup.Fame),
    Awards("Награды", FilterGroup.Fame),
    Budget("Бюджет и сборы", FilterGroup.Fame),
    Company("Кинокомпания", FilterGroup.Production),
    Platform("Платформа", FilterGroup.Production),
    Series("Сериал", FilterGroup.Production),
    ReleaseDates("Даты выхода", FilterGroup.Production),
    Preset("Готовый набор", FilterGroup.Collections),
    Collection("Подборка", FilterGroup.Collections),
    Sort("Сортировка", FilterGroup.Order),
    TitleMode("Название", FilterGroup.Order),
}

val AllGenres = listOf(
    FacetItemDto("action", "Боевик"),
    FacetItemDto("adventure", "Приключения"),
    FacetItemDto("biography", "Биография"),
    FacetItemDto("comedy", "Комедия"),
    FacetItemDto("crime", "Криминал"),
    FacetItemDto("documentary", "Документальный"),
    FacetItemDto("drama", "Драма"),
    FacetItemDto("family", "Семейный"),
    FacetItemDto("fantasy", "Фэнтези"),
    FacetItemDto("film_noir", "Фильм-нуар"),
    FacetItemDto("history", "История"),
    FacetItemDto("horror", "Ужасы"),
    FacetItemDto("kids", "Детский"),
    FacetItemDto("music", "Музыка"),
    FacetItemDto("musical", "Мюзикл"),
    FacetItemDto("mystery", "Детектив"),
    FacetItemDto("mysticism", "Мистика"),
    FacetItemDto("romance", "Мелодрама"),
    FacetItemDto("speculative_fiction", "Фантастика"),
    FacetItemDto("sport", "Спорт"),
    FacetItemDto("thriller", "Триллер"),
    FacetItemDto("war", "Военный"),
    FacetItemDto("western", "Вестерн"),
)

/** RU-название / slug → slug для discover и /movie/genre/{slug} */
fun genreSlugOf(nameOrSlug: String): String? {
    val key = nameOrSlug.trim().lowercase()
    if (key.isEmpty()) return null
    AllGenres.firstOrNull { it.value.equals(key, ignoreCase = true) }?.value?.let { return it }
    return AllGenres.firstOrNull { it.label?.lowercase() == key }?.value
}

val AllCountries = listOf(
    FacetItemDto("RU", "Россия"),
    FacetItemDto("SU", "СССР"),
    FacetItemDto("US", "США"),
    FacetItemDto("GB", "Великобритания"),
    FacetItemDto("FR", "Франция"),
    FacetItemDto("DE", "Германия"),
    FacetItemDto("IT", "Италия"),
    FacetItemDto("ES", "Испания"),
    FacetItemDto("TR", "Турция"),
    FacetItemDto("KR", "Корея"),
    FacetItemDto("CN", "Китай"),
    FacetItemDto("JP", "Япония"),
    FacetItemDto("IN", "Индия"),
    FacetItemDto("CA", "Канада"),
    FacetItemDto("AU", "Австралия"),
    FacetItemDto("KZ", "Казахстан"),
    FacetItemDto("UA", "Украина"),
    FacetItemDto("BR", "Бразилия"),
    FacetItemDto("MX", "Мексика"),
    FacetItemDto("SE", "Швеция"),
    FacetItemDto("NO", "Норвегия"),
    FacetItemDto("DK", "Дания"),
    FacetItemDto("PL", "Польша"),
    FacetItemDto("NL", "Нидерланды"),
    FacetItemDto("BE", "Бельгия"),
    FacetItemDto("CH", "Швейцария"),
    FacetItemDto("AT", "Австрия"),
    FacetItemDto("HK", "Гонконг"),
    FacetItemDto("TW", "Тайвань"),
    FacetItemDto("TH", "Таиланд"),
)

val AllRegions = listOf(
    FacetItemDto("cis", "СНГ"),
    FacetItemDto("europe", "Европа"),
    FacetItemDto("asia", "Азия"),
    FacetItemDto("america", "Америка"),
)

/** Значения age как на сайте: age=6|12|16|18. */
val AgeOptions = listOf(
    FacetItemDto("any", "Любой"),
    FacetItemDto("6", "Не старше 6+"),
    FacetItemDto("12", "Не старше 12+"),
    FacetItemDto("16", "Не старше 16+"),
    FacetItemDto("18", "Только 18+"),
)

val CrewRoleOptions = listOf(
    FacetItemDto("director", "Режиссёр"),
    FacetItemDto("writer", "Сценарист"),
    FacetItemDto("author", "Автор"),
    FacetItemDto("producer", "Продюсер"),
    FacetItemDto("cinematographer", "Оператор"),
    FacetItemDto("composer", "Композитор"),
    FacetItemDto("editor", "Монтажёр"),
    FacetItemDto("art", "Художник"),
    FacetItemDto("sound", "Звукорежиссёр"),
)

val ReleaseStatusOptions = listOf(
    FacetItemDto("released", "Вышедшие"),
    FacetItemDto("upcoming", "Ещё не вышли"),
    FacetItemDto("any", "Все"),
)

/** Значения preset как на сайте. */
val PresetOptions = listOf(
    FacetItemDto("none", "Без набора"),
    FacetItemDto("top", "Лучшее по IMDb"),
    FacetItemDto("hidden_gems", "Скрытые жемчужины"),
    FacetItemDto("trending", "Популярное сейчас"),
    FacetItemDto("new", "Новинки месяца"),
    FacetItemDto("classics", "Классика по 1980 год"),
)

/** Значения collection как на сайте. */
val CollectionOptions = listOf(
    FacetItemDto("all", "Все"),
    FacetItemDto("films-best500", "Топ-500 фильмов"),
    FacetItemDto("series-best250", "Топ-250 сериалов"),
    FacetItemDto("all-popular1000", "Топ-1000 популярных"),
    FacetItemDto("all-week250", "В тренде за неделю"),
    FacetItemDto("all-day250", "В тренде за день"),
)

/** Значения sort как на сайте (− убыв. / + возр.). */
val SortOptions = listOf(
    FacetItemDto("-popularity", "По популярности"),
    FacetItemDto("-rating_kp", "По рейтингу КП"),
    FacetItemDto("-rating_imdb", "По рейтингу IMDb"),
    FacetItemDto("-rating_tmdb", "По рейтингу TMDB"),
    FacetItemDto("-rating_rt", "По Rotten Tomatoes"),
    FacetItemDto("-rating_metacritic", "По Metacritic"),
    FacetItemDto("-rating_critics", "По оценке критиков"),
    FacetItemDto("-votes_kp", "По числу оценок КП"),
    FacetItemDto("-votes_imdb", "По числу оценок IMDb"),
    FacetItemDto("-release_date", "Сначала новые"),
    FacetItemDto("+release_date", "Сначала старые"),
    FacetItemDto("-runtime", "Сначала длинные"),
    FacetItemDto("+runtime", "Сначала короткие"),
    FacetItemDto("-budget", "По бюджету"),
    FacetItemDto("-revenue", "По сборам в мире"),
    FacetItemDto("-box_office", "По сборам в США"),
    FacetItemDto("-oscars_won", "По числу «Оскаров»"),
    FacetItemDto("-awards_won", "По числу наград"),
)

const val DefaultSort = "-popularity"

val LanguageOptions = listOf(
    FacetItemDto("en", "Английский"),
    FacetItemDto("ru", "Русский"),
    FacetItemDto("fr", "Французский"),
    FacetItemDto("de", "Немецкий"),
    FacetItemDto("es", "Испанский"),
    FacetItemDto("it", "Итальянский"),
    FacetItemDto("ja", "Японский"),
    FacetItemDto("ko", "Корейский"),
    FacetItemDto("zh", "Китайский"),
    FacetItemDto("hi", "Хинди"),
    FacetItemDto("ta", "Тамильский"),
    FacetItemDto("te", "Телугу"),
    FacetItemDto("ml", "Малаялам"),
    FacetItemDto("tr", "Турецкий"),
    FacetItemDto("uk", "Украинский"),
    FacetItemDto("pl", "Польский"),
    FacetItemDto("pt", "Португальский"),
    FacetItemDto("sv", "Шведский"),
    FacetItemDto("da", "Датский"),
    FacetItemDto("no", "Норвежский"),
    FacetItemDto("fi", "Финский"),
    FacetItemDto("nl", "Нидерландский"),
    FacetItemDto("cs", "Чешский"),
    FacetItemDto("hu", "Венгерский"),
    FacetItemDto("ro", "Румынский"),
    FacetItemDto("el", "Греческий"),
    FacetItemDto("he", "Иврит"),
    FacetItemDto("ar", "Арабский"),
    FacetItemDto("fa", "Персидский"),
    FacetItemDto("th", "Тайский"),
    FacetItemDto("id", "Индонезийский"),
    FacetItemDto("tl", "Тагальский"),
    FacetItemDto("ka", "Грузинский"),
    FacetItemDto("kk", "Казахский"),
    FacetItemDto("be", "Белорусский"),
    FacetItemDto("sr", "Сербский"),
    FacetItemDto("hr", "Хорватский"),
    FacetItemDto("bg", "Болгарский"),
    FacetItemDto("et", "Эстонский"),
    FacetItemDto("lt", "Литовский"),
    FacetItemDto("lv", "Латышский"),
    FacetItemDto("is", "Исландский"),
)

val DefaultTagOptions = listOf(
    FacetItemDto("По книге", "По книге"),
    FacetItemDto("Основано на реальных событиях", "Основано на реальных событиях"),
    FacetItemDto("Мрачный", "Мрачный"),
    FacetItemDto("Напряжённый", "Напряжённый"),
    FacetItemDto("Эпический", "Эпический"),
    FacetItemDto("Смешной", "Смешной"),
    FacetItemDto("Романтичный", "Романтичный"),
    FacetItemDto("Супергерои", "Супергерои"),
    FacetItemDto("Киберпанк", "Киберпанк"),
    FacetItemDto("Исекай", "Исекай"),
    FacetItemDto("Нуар", "Нуар"),
    FacetItemDto("Чёрно-белый", "Чёрно-белый"),
    FacetItemDto("Путешествие во времени", "Путешествие во времени"),
    FacetItemDto("Месть", "Месть"),
    FacetItemDto("Дружба", "Дружба"),
)

val DefaultPlatformOptions = listOf(
    FacetItemDto("netflix", "Netflix"),
    FacetItemDto("kinopoisk", "Кинопоиск HD"),
    FacetItemDto("ivi", "IVI"),
    FacetItemDto("okko", "Okko"),
    FacetItemDto("premier", "Premier"),
    FacetItemDto("start", "START"),
    FacetItemDto("wink", "Wink"),
    FacetItemDto("disney", "Disney+"),
    FacetItemDto("hbo_max", "Max"),
    FacetItemDto("prime", "Prime Video"),
    FacetItemDto("apple_tv", "Apple TV+"),
    FacetItemDto("hulu", "Hulu"),
)

val DefaultCompanyOptions = listOf(
    FacetItemDto("warner", "Warner Bros."),
    FacetItemDto("universal", "Universal"),
    FacetItemDto("sony", "Sony"),
    FacetItemDto("paramount", "Paramount"),
    FacetItemDto("disney_studio", "Disney"),
    FacetItemDto("dreamworks", "DreamWorks"),
    FacetItemDto("a24", "A24"),
    FacetItemDto("netflix_studio", "Netflix Studios"),
    FacetItemDto("bbc", "BBC"),
    FacetItemDto("hbo", "HBO"),
)
