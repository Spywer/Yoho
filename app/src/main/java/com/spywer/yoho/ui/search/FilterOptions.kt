package com.spywer.yoho.ui.search

import com.spywer.yoho.data.remote.FacetItemDto

/** Полный набор категорий как на reyoho.com/search → «Фильтр». */
enum class FilterCategory(val title: String) {
    Genre("Жанр"),
    Country("Страна"),
    Year("Год"),
    Rating("Рейтинг"),
    Actors("Актёры"),
    Crew("Съёмочная группа"),
    ExcludePeople("Без участия"),
    Tags("Теги"),
    Language("Язык оригинала"),
    Region("Регион"),
    Age("Возраст"),
    Duration("Длительность"),
    Votes("Число оценок"),
    Awards("Награды"),
    Budget("Бюджет и сборы"),
    Company("Кинокомпания"),
    Platform("Платформа"),
    Series("Сериал"),
    ReleaseDates("Даты выхода"),
    Preset("Готовый набор"),
    Collection("Подборка"),
    Sort("Сортировка"),
    TitleMode("Название"),
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
    FacetItemDto("africa", "Африка"),
    FacetItemDto("oceania", "Океания"),
)

val AgeOptions = listOf(
    FacetItemDto("any", "Любой"),
    FacetItemDto("max6", "Не старше 6+"),
    FacetItemDto("max12", "Не старше 12+"),
    FacetItemDto("max16", "Не старше 16+"),
    FacetItemDto("only18", "Только 18+"),
)

val CrewRoleOptions = listOf(
    FacetItemDto("director", "Режиссёр"),
    FacetItemDto("writer", "Сценарист"),
    FacetItemDto("author", "Автор"),
    FacetItemDto("producer", "Продюсер"),
    FacetItemDto("cinematographer", "Оператор"),
    FacetItemDto("composer", "Композитор"),
    FacetItemDto("editor", "Монтажёр"),
    FacetItemDto("art_director", "Художник"),
    FacetItemDto("sound", "Звукорежиссёр"),
)

val ReleaseStatusOptions = listOf(
    FacetItemDto("released", "Вышедшие"),
    FacetItemDto("upcoming", "Ещё не вышли"),
    FacetItemDto("all", "Все"),
)

val PresetOptions = listOf(
    FacetItemDto("none", "Без набора"),
    FacetItemDto("imdb_best", "Лучшее по IMDb"),
    FacetItemDto("hidden_gems", "Скрытые жемчужины"),
    FacetItemDto("popular_now", "Популярное сейчас"),
    FacetItemDto("new_month", "Новинки месяца"),
    FacetItemDto("classic_1980", "Классика по 1980 год"),
)

val CollectionOptions = listOf(
    FacetItemDto("all", "Весь каталог"),
    FacetItemDto("top500_movies", "Топ-500 фильмов"),
    FacetItemDto("top250_series", "Топ-250 сериалов"),
    FacetItemDto("top1000_popular", "Топ-1000 популярных"),
    FacetItemDto("trending_week", "В тренде за неделю"),
    FacetItemDto("trending_day", "В тренде за день"),
)

val SortOptions = listOf(
    FacetItemDto("popularity", "По популярности"),
    FacetItemDto("rating_kp", "По рейтингу КП"),
    FacetItemDto("rating_imdb", "По рейтингу IMDb"),
    FacetItemDto("rating_tmdb", "По рейтингу TMDB"),
    FacetItemDto("rating_rt", "По Rotten Tomatoes"),
    FacetItemDto("rating_mc", "По Metacritic"),
    FacetItemDto("rating_critics", "По оценке критиков"),
)

val LanguageOptions = listOf(
    FacetItemDto("ru", "Русский"),
    FacetItemDto("en", "English"),
    FacetItemDto("ja", "日本語"),
    FacetItemDto("ko", "한국어"),
    FacetItemDto("zh", "中文"),
    FacetItemDto("fr", "Français"),
    FacetItemDto("de", "Deutsch"),
    FacetItemDto("es", "Español"),
    FacetItemDto("it", "Italiano"),
    FacetItemDto("original", "Оригинал"),
    FacetItemDto("any", "Любой"),
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
