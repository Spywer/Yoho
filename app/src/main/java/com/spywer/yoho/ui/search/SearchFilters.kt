package com.spywer.yoho.ui.search

import com.spywer.yoho.data.remote.FacetItemDto
import com.spywer.yoho.data.remote.SearchFacetsDto
import com.spywer.yoho.data.remote.SearchFiltersCatalogDto

data class SearchTypeOption(val value: String, val label: String)

val DefaultSearchTypes = listOf(
    SearchTypeOption("all", "Все"),
    SearchTypeOption("movie", "Фильмы"),
    SearchTypeOption("tv", "Сериалы"),
    SearchTypeOption("animation", "Мультфильмы"),
    SearchTypeOption("anime", "Аниме"),
)

data class IntRangeFilter(val from: Int? = null, val to: Int? = null) {
    val isSet: Boolean get() = from != null || to != null
    fun label(prefix: String = ""): String {
        val body = when {
            from != null && to != null -> "$from–$to"
            from != null -> "от $from"
            to != null -> "до $to"
            else -> ""
        }
        return if (prefix.isEmpty()) body else "$prefix $body".trim()
    }
}

data class DoubleRangeFilter(val from: Double? = null, val to: Double? = null) {
    val isSet: Boolean get() = from != null || to != null
    fun label(prefix: String): String {
        val body = when {
            from != null && to != null -> "$from–$to"
            from != null -> "от $from"
            to != null -> "до $to"
            else -> ""
        }
        return "$prefix $body".trim()
    }
}

data class LongRangeFilter(val from: Long? = null, val to: Long? = null) {
    val isSet: Boolean get() = from != null || to != null
    fun label(prefix: String): String {
        val body = when {
            from != null && to != null -> "$from–$to"
            from != null -> "от $from"
            to != null -> "до $to"
            else -> ""
        }
        return "$prefix $body".trim()
    }
}

/** Человек в фильтре: имя + KP id (как actor / actor_kp на сайте). */
data class PersonFilterRef(
    val name: String,
    val kpId: Int,
)

data class SearchFilterState(
    /** Пустой набор = все типы. Иначе мультивыбор: movie, tv, animation, anime. */
    val types: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val countries: Set<String> = emptySet(),
    val region: String? = null,
    val year: IntRangeFilter = IntRangeFilter(),
    val ratingKp: DoubleRangeFilter = DoubleRangeFilter(),
    val ratingImdb: DoubleRangeFilter = DoubleRangeFilter(),
    val ratingTmdb: DoubleRangeFilter = DoubleRangeFilter(),
    val ratingRt: DoubleRangeFilter = DoubleRangeFilter(),
    val ratingMc: DoubleRangeFilter = DoubleRangeFilter(),
    val ratingCritics: DoubleRangeFilter = DoubleRangeFilter(),
    val actors: List<PersonFilterRef> = emptyList(),
    val crewRole: String? = null,
    val crew: List<PersonFilterRef> = emptyList(),
    val excludePeople: List<PersonFilterRef> = emptyList(),
    val tags: Set<String> = emptySet(),
    val language: String? = null,
    val age: String? = null,
    val duration: IntRangeFilter = IntRangeFilter(),
    val votesKp: IntRangeFilter = IntRangeFilter(),
    val votesImdb: IntRangeFilter = IntRangeFilter(),
    val votesTmdb: IntRangeFilter = IntRangeFilter(),
    val oscars: IntRangeFilter = IntRangeFilter(),
    val awardsTotal: IntRangeFilter = IntRangeFilter(),
    val budget: LongRangeFilter = LongRangeFilter(),
    val boxOfficeWorld: LongRangeFilter = LongRangeFilter(),
    val boxOfficeUs: LongRangeFilter = LongRangeFilter(),
    val companies: Set<String> = emptySet(),
    val platforms: Set<String> = emptySet(),
    val seasons: IntRangeFilter = IntRangeFilter(),
    val episodes: IntRangeFilter = IntRangeFilter(),
    val airStart: IntRangeFilter = IntRangeFilter(),
    val airEnd: IntRangeFilter = IntRangeFilter(),
    val releaseStatus: String? = null,
    val premiereWorldFrom: String? = null,
    val premiereWorldTo: String? = null,
    val premiereRuFrom: String? = null,
    val premiereRuTo: String? = null,
    val digitalFrom: String? = null,
    val digitalTo: String? = null,
    val preset: String? = null,
    val collection: String? = null,
    val sort: String = DefaultSort,
    val titleContains: String? = null,
    val onlyWithPlayer: Boolean = false,
    val excludeWatched: Boolean = false,
) {
    val hasAdvanced: Boolean
        get() = genres.isNotEmpty() || countries.isNotEmpty() || region != null ||
            year.isSet || ratingKp.isSet || ratingImdb.isSet || ratingTmdb.isSet ||
            ratingRt.isSet || ratingMc.isSet || ratingCritics.isSet ||
            actors.isNotEmpty() || crew.isNotEmpty() || crewRole != null ||
            excludePeople.isNotEmpty() || tags.isNotEmpty() || language != null ||
            age != null || duration.isSet || votesKp.isSet || votesImdb.isSet ||
            votesTmdb.isSet || oscars.isSet || awardsTotal.isSet || budget.isSet ||
            boxOfficeWorld.isSet || boxOfficeUs.isSet || companies.isNotEmpty() ||
            platforms.isNotEmpty() || seasons.isSet || episodes.isSet ||
            airStart.isSet || airEnd.isSet || releaseStatus != null ||
            premiereWorldFrom != null || premiereWorldTo != null ||
            premiereRuFrom != null || premiereRuTo != null ||
            digitalFrom != null || digitalTo != null || preset != null ||
            collection != null || sort != DefaultSort ||
            !titleContains.isNullOrBlank() || onlyWithPlayer || excludeWatched

    val isActive: Boolean get() = types.isNotEmpty() || hasAdvanced

    fun toggleType(value: String): SearchFilterState = when {
        value == "all" -> copy(types = emptySet())
        value in types -> copy(types = types - value)
        else -> copy(types = types + value - "all")
    }

    fun activeGroups(
        facets: SearchFacetsDto,
        catalog: SearchFiltersCatalogDto,
    ): List<ActiveFilterGroup> = buildList {
        fun labelOf(value: String, from: List<FacetItemDto>, fallback: String = value) =
            from.find { it.value == value }?.label ?: fallback

        fun group(title: String, chips: List<ActiveFilterChip>) {
            if (chips.isNotEmpty()) add(ActiveFilterGroup(title = title, chips = chips))
        }

        // Типы показываются отдельным мультиселектом сверху
        group(
            "Жанры",
            genres.map { g ->
                ActiveFilterChip("genre:$g", labelOf(g, facets.genres.ifEmpty { AllGenres }, g))
            },
        )
        group(
            "Страны",
            countries.map { c ->
                ActiveFilterChip("country:$c", labelOf(c, facets.countries.ifEmpty { AllCountries }, c))
            },
        )
        group(
            "Регион",
            listOfNotNull(
                region?.let { r ->
                    ActiveFilterChip("region", labelOf(r, facets.regions.ifEmpty { AllRegions }, r))
                },
            ),
        )
        group("Год", listOfNotNull(year.takeIf { it.isSet }?.let { ActiveFilterChip("year", it.label()) }))
        group(
            "Рейтинг",
            buildList {
                if (ratingKp.isSet) add(ActiveFilterChip("rating_kp", ratingKp.label("КП")))
                if (ratingImdb.isSet) add(ActiveFilterChip("rating_imdb", ratingImdb.label("IMDb")))
                if (ratingTmdb.isSet) add(ActiveFilterChip("rating_tmdb", ratingTmdb.label("TMDB")))
                if (ratingRt.isSet) add(ActiveFilterChip("rating_rt", ratingRt.label("RT")))
                if (ratingMc.isSet) add(ActiveFilterChip("rating_mc", ratingMc.label("MC")))
                if (ratingCritics.isSet) add(ActiveFilterChip("rating_critics", ratingCritics.label("Критики")))
            },
        )
        group(
            "Актёры",
            actors.map { ActiveFilterChip("actor:${it.kpId}", it.name) },
        )
        group(
            "Съёмочная группа",
            buildList {
                crewRole?.let { add(ActiveFilterChip("crew_role", it)) }
                crew.forEach { add(ActiveFilterChip("crew:${it.kpId}", it.name)) }
            },
        )
        group(
            "Исключить",
            excludePeople.map { ActiveFilterChip("exclude:${it.kpId}", it.name) },
        )
        group(
            "Теги",
            tags.map { t ->
                ActiveFilterChip("tag:$t", catalog.tags.find { it.tag == t }?.tag ?: t)
            },
        )
        group(
            "Язык",
            listOfNotNull(
                language?.let { ActiveFilterChip("language", labelOf(it, LanguageOptions, it)) },
            ),
        )
        group(
            "Возраст",
            listOfNotNull(age?.let { ActiveFilterChip("age", labelOf(it, AgeOptions, it)) }),
        )
        group(
            "Длительность",
            listOfNotNull(duration.takeIf { it.isSet }?.let { ActiveFilterChip("duration", it.label("мин")) }),
        )
        group(
            "Голоса",
            buildList {
                if (votesKp.isSet) add(ActiveFilterChip("votes_kp", votesKp.label("КП")))
                if (votesImdb.isSet) add(ActiveFilterChip("votes_imdb", votesImdb.label("IMDb")))
                if (votesTmdb.isSet) add(ActiveFilterChip("votes_tmdb", votesTmdb.label("TMDB")))
            },
        )
        group(
            "Награды",
            buildList {
                if (oscars.isSet) add(ActiveFilterChip("oscars", oscars.label("Оскаров")))
                if (awardsTotal.isSet) add(ActiveFilterChip("awards", awardsTotal.label("Всего")))
            },
        )
        group(
            "Бюджет и сборы",
            buildList {
                if (budget.isSet) add(ActiveFilterChip("budget", budget.label("Бюджет")))
                if (boxOfficeWorld.isSet) add(ActiveFilterChip("box_world", boxOfficeWorld.label("Мир")))
                if (boxOfficeUs.isSet) add(ActiveFilterChip("box_us", boxOfficeUs.label("США")))
            },
        )
        group("Студии", companies.map { ActiveFilterChip("company:$it", it) })
        group(
            "Платформы",
            platforms.map { p ->
                val name = catalog.platforms.find { it.slug == p }?.name
                    ?: labelOf(p, DefaultPlatformOptions, p)
                ActiveFilterChip("platform:$p", name)
            },
        )
        group(
            "Сезоны / серии",
            buildList {
                if (seasons.isSet) add(ActiveFilterChip("seasons", seasons.label("Сезонов")))
                if (episodes.isSet) add(ActiveFilterChip("episodes", episodes.label("Серий")))
            },
        )
        group(
            "Эфир",
            buildList {
                if (airStart.isSet) add(ActiveFilterChip("air_start", airStart.label("Начало")))
                if (airEnd.isSet) add(ActiveFilterChip("air_end", airEnd.label("Конец")))
            },
        )
        group(
            "Статус",
            listOfNotNull(
                releaseStatus?.let {
                    ActiveFilterChip("release_status", labelOf(it, ReleaseStatusOptions, it))
                },
            ),
        )
        group(
            "Премьера",
            buildList {
                if (premiereWorldFrom != null || premiereWorldTo != null) {
                    add(
                        ActiveFilterChip(
                            "premiere_world",
                            "Мир ${premiereWorldFrom ?: "…"}–${premiereWorldTo ?: "…"}",
                        ),
                    )
                }
                if (premiereRuFrom != null || premiereRuTo != null) {
                    add(
                        ActiveFilterChip(
                            "premiere_ru",
                            "РФ ${premiereRuFrom ?: "…"}–${premiereRuTo ?: "…"}",
                        ),
                    )
                }
                if (digitalFrom != null || digitalTo != null) {
                    add(
                        ActiveFilterChip(
                            "digital",
                            "Цифра ${digitalFrom ?: "…"}–${digitalTo ?: "…"}",
                        ),
                    )
                }
            },
        )
        group(
            "Подборка",
            buildList {
                preset?.let { add(ActiveFilterChip("preset", labelOf(it, PresetOptions, it))) }
                collection?.let {
                    add(ActiveFilterChip("collection", labelOf(it, CollectionOptions, it)))
                }
            },
        )
        group(
            "Сортировка",
            listOfNotNull(
                sort.takeIf { it != DefaultSort }?.let {
                    ActiveFilterChip("sort", labelOf(it, SortOptions, it))
                },
            ),
        )
        group(
            "Название",
            listOfNotNull(
                titleContains?.takeIf { it.isNotBlank() }?.let { ActiveFilterChip("title", "«$it»") },
            ),
        )
        group(
            "Прочее",
            buildList {
                if (onlyWithPlayer) add(ActiveFilterChip("player", "С плеером"))
                if (excludeWatched) add(ActiveFilterChip("watched", "Без просмотренных"))
            },
        )
    }
}

data class ActiveFilterGroup(val title: String, val chips: List<ActiveFilterChip>)

data class ActiveFilterChip(val id: String, val label: String)

fun SearchFacetsDto.withFallbackTypes(): SearchFacetsDto {
    if (types.isNotEmpty()) return this
    return copy(
        types = DefaultSearchTypes.map {
            FacetItemDto(value = it.value, label = it.label, available = true)
        },
    )
}

fun SearchFilterState.removeChip(id: String): SearchFilterState = when {
    id.startsWith("type:") -> copy(types = types - id.removePrefix("type:"))
    id.startsWith("genre:") -> copy(genres = genres - id.removePrefix("genre:"))
    id.startsWith("country:") -> copy(countries = countries - id.removePrefix("country:"))
    id == "region" -> copy(region = null)
    id == "year" -> copy(year = IntRangeFilter())
    id == "rating_kp" -> copy(ratingKp = DoubleRangeFilter())
    id == "rating_imdb" -> copy(ratingImdb = DoubleRangeFilter())
    id == "rating_tmdb" -> copy(ratingTmdb = DoubleRangeFilter())
    id == "rating_rt" -> copy(ratingRt = DoubleRangeFilter())
    id == "rating_mc" -> copy(ratingMc = DoubleRangeFilter())
    id == "rating_critics" -> copy(ratingCritics = DoubleRangeFilter())
    id.startsWith("actor:") -> {
        val kp = id.removePrefix("actor:").toIntOrNull()
        copy(actors = actors.filterNot { it.kpId == kp })
    }
    id == "crew_role" -> copy(crewRole = null)
    id.startsWith("crew:") -> {
        val kp = id.removePrefix("crew:").toIntOrNull()
        copy(crew = crew.filterNot { it.kpId == kp })
    }
    id.startsWith("exclude:") -> {
        val kp = id.removePrefix("exclude:").toIntOrNull()
        copy(excludePeople = excludePeople.filterNot { it.kpId == kp })
    }
    id.startsWith("tag:") -> copy(tags = tags - id.removePrefix("tag:"))
    id == "language" -> copy(language = null)
    id == "age" -> copy(age = null)
    id == "duration" -> copy(duration = IntRangeFilter())
    id == "votes_kp" -> copy(votesKp = IntRangeFilter())
    id == "votes_imdb" -> copy(votesImdb = IntRangeFilter())
    id == "votes_tmdb" -> copy(votesTmdb = IntRangeFilter())
    id == "oscars" -> copy(oscars = IntRangeFilter())
    id == "awards" -> copy(awardsTotal = IntRangeFilter())
    id == "budget" -> copy(budget = LongRangeFilter())
    id == "box_world" -> copy(boxOfficeWorld = LongRangeFilter())
    id == "box_us" -> copy(boxOfficeUs = LongRangeFilter())
    id.startsWith("company:") -> copy(companies = companies - id.removePrefix("company:"))
    id.startsWith("platform:") -> copy(platforms = platforms - id.removePrefix("platform:"))
    id == "seasons" -> copy(seasons = IntRangeFilter())
    id == "episodes" -> copy(episodes = IntRangeFilter())
    id == "air_start" -> copy(airStart = IntRangeFilter())
    id == "air_end" -> copy(airEnd = IntRangeFilter())
    id == "release_status" -> copy(releaseStatus = null)
    id == "premiere_world" -> copy(premiereWorldFrom = null, premiereWorldTo = null)
    id == "premiere_ru" -> copy(premiereRuFrom = null, premiereRuTo = null)
    id == "digital" -> copy(digitalFrom = null, digitalTo = null)
    id == "preset" -> copy(preset = null)
    id == "collection" -> copy(collection = null)
    id == "sort" -> copy(sort = DefaultSort)
    id == "title" -> copy(titleContains = null)
    id == "player" -> copy(onlyWithPlayer = false)
    id == "watched" -> copy(excludeWatched = false)
    else -> this
}

/** Сколько выбранных значений в категории — для бейджа в списке фильтров. */
fun SearchFilterState.selectionCount(category: FilterCategory): Int = when (category) {
    FilterCategory.Genre -> genres.size
    FilterCategory.Country -> countries.size
    FilterCategory.Year -> if (year.isSet) 1 else 0
    FilterCategory.Rating -> listOf(
        ratingKp, ratingImdb, ratingTmdb, ratingRt, ratingMc, ratingCritics,
    ).count { it.isSet }
    FilterCategory.Actors -> actors.size
    FilterCategory.Crew -> crew.size + if (crewRole != null) 1 else 0
    FilterCategory.ExcludePeople -> excludePeople.size
    FilterCategory.Tags -> tags.size
    FilterCategory.Language -> if (language != null) 1 else 0
    FilterCategory.Region -> if (region != null) 1 else 0
    FilterCategory.Age -> if (age != null) 1 else 0
    FilterCategory.Duration -> if (duration.isSet) 1 else 0
    FilterCategory.Votes -> listOf(votesKp, votesImdb, votesTmdb).count { it.isSet }
    FilterCategory.Awards -> listOf(oscars, awardsTotal).count { it.isSet }
    FilterCategory.Budget -> listOf(budget, boxOfficeWorld, boxOfficeUs).count { it.isSet }
    FilterCategory.Company -> companies.size
    FilterCategory.Platform -> platforms.size
    FilterCategory.Series -> listOf(seasons, episodes, airStart, airEnd).count { it.isSet }
    FilterCategory.ReleaseDates -> {
        var n = 0
        if (releaseStatus != null) n++
        if (premiereWorldFrom != null || premiereWorldTo != null) n++
        if (premiereRuFrom != null || premiereRuTo != null) n++
        if (digitalFrom != null || digitalTo != null) n++
        n
    }
    FilterCategory.Preset -> if (preset != null) 1 else 0
    FilterCategory.Collection -> if (collection != null) 1 else 0
    FilterCategory.Sort -> if (sort != DefaultSort) 1 else 0
    FilterCategory.TitleMode -> if (!titleContains.isNullOrBlank()) 1 else 0
}
