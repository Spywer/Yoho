package com.spywer.yoho.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spywer.yoho.data.remote.FacetItemDto
import com.spywer.yoho.data.remote.NetworkModule
import com.spywer.yoho.data.remote.SearchFacetsDto
import com.spywer.yoho.data.remote.SearchFiltersCatalogDto
import com.spywer.yoho.data.remote.SuggestItemDto
import com.spywer.yoho.data.remote.absMediaUrl
import com.spywer.yoho.ui.layout.LocalHomeMetrics
import com.spywer.yoho.ui.theme.YohoColors
import com.spywer.yoho.ui.theme.YohoThemeExtras
import kotlinx.coroutines.delay

private val TitleSp: TextUnit
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 28.sp else 18.sp
    }
private val CategorySp: TextUnit
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 22.sp else 14.sp
    }
private val BodySp: TextUnit
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 20.sp else 14.sp
    }
private val ChipSp: TextUnit
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 18.sp else 12.sp
    }
private val HintSp: TextUnit
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 15.sp else 12.sp
    }
private val RowMin: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 56.dp else 36.dp
    }
private val ChipMinH: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 48.dp else 30.dp
    }
private val FieldH: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 52.dp else 36.dp
    }
private val CatPadV: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 16.dp else 8.dp
    }
private val FieldPadH: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 14.dp else 10.dp
    }
private val FieldPadV: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 12.dp else 6.dp
    }
private val IconBtn: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 52.dp else 36.dp
    }
private val IconSz: Dp
    @Composable get() {
        val m = LocalHomeMetrics.current
        return if (m.isTablet) 28.dp else 20.dp
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    facets: SearchFacetsDto,
    catalog: SearchFiltersCatalogDto,
    filters: SearchFilterState,
    onDismiss: () -> Unit,
    onUpdate: (SearchFilterState) -> Unit,
    onClear: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nav by remember { mutableStateOf<FilterCategory?>(null) }
    var query by remember { mutableStateOf("") }
    val yoho = YohoThemeExtras.colors
    val metrics = LocalHomeMetrics.current
    val config = LocalConfiguration.current
    val screenH = config.screenHeightDp.dp
    val screenW = config.screenWidthDp.dp
    // Medium/Expanded — почти на весь экран, чтобы сразу видеть больше значений.
    val sheetModifier = if (metrics.isTablet) {
        Modifier
            .fillMaxHeight(0.94f)
            .heightIn(min = screenH * 0.88f, max = screenH * 0.96f)
    } else {
        Modifier.heightIn(max = 640.dp)
    }
    // По умолчанию M3 режет sheet до 640dp — на планшете разворачиваем по ширине.
    val sheetMaxWidth = if (metrics.isTablet) {
        screenW * 0.96f
    } else {
        BottomSheetDefaults.SheetMaxWidth
    }
    val hPad = if (metrics.isTablet) 28.dp else 12.dp
    val listGap = if (metrics.isTablet) 8.dp else 2.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(sheetModifier)
                .padding(horizontal = hPad)
                .padding(bottom = if (metrics.isTablet) metrics.sectionGap else 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nav != null) {
                    IconButton(
                        onClick = { nav = null; query = "" },
                        modifier = Modifier.size(IconBtn),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            Modifier.size(IconSz),
                        )
                    }
                }
                Text(
                    text = nav?.title ?: "Фильтр",
                    fontWeight = FontWeight.Bold,
                    fontSize = TitleSp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (filters.isActive) {
                    TextButton(
                        onClick = {
                            nav = null
                            query = ""
                            onClear()
                        },
                    ) {
                        Text("Сбросить", fontSize = BodySp)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(IconBtn)) {
                    Icon(Icons.Filled.Close, null, Modifier.size(IconSz))
                }
            }

            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = if (nav == null) "Найти фильтр или значение…" else "Поиск…",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(if (metrics.isTablet) 8.dp else 4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = metrics.isTablet)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(listGap),
            ) {
                when (val category = nav) {
                    null -> {
                        FilterCategory.entries
                            .filter { query.isBlank() || it.title.contains(query, true) }
                            .forEach { cat ->
                                Text(
                                    text = cat.title,
                                    fontSize = CategorySp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = RowMin)
                                        .clickable { nav = cat; query = "" }
                                        .padding(
                                            vertical = CatPadV,
                                            horizontal = if (metrics.isTablet) 6.dp else 2.dp,
                                        ),
                                )
                            }
                        Spacer(Modifier.height(if (metrics.isTablet) 8.dp else 4.dp))
                        ToggleRow("Только с плеером", filters.onlyWithPlayer) {
                            onUpdate(filters.copy(onlyWithPlayer = it))
                        }
                        ToggleRow("Исключить просмотренные", filters.excludeWatched) {
                            onUpdate(filters.copy(excludeWatched = it))
                        }
                    }

                    FilterCategory.Genre -> ValueChips(
                        items = facets.genres.ifEmpty { AllGenres }.filterQuery(query),
                        selected = filters.genres,
                        onToggle = { onUpdate(filters.copy(genres = filters.genres.toggle(it))) },
                        colors = yoho,
                    )

                    FilterCategory.Country -> ValueChips(
                        items = facets.countries.ifEmpty { AllCountries }.filterQuery(query),
                        selected = filters.countries,
                        onToggle = {
                            onUpdate(filters.copy(countries = filters.countries.toggle(it), region = null))
                        },
                        colors = yoho,
                    )

                    FilterCategory.Year -> {
                        IntRangeRow(
                            label = "Год",
                            range = filters.year,
                            onChange = { onUpdate(filters.copy(year = it)) },
                        )
                    }

                    FilterCategory.Rating -> {
                        DoubleRangeRow("КП", filters.ratingKp) {
                            onUpdate(filters.copy(ratingKp = it))
                        }
                        DoubleRangeRow("IMDb", filters.ratingImdb) {
                            onUpdate(filters.copy(ratingImdb = it))
                        }
                        DoubleRangeRow("TMDB", filters.ratingTmdb) {
                            onUpdate(filters.copy(ratingTmdb = it))
                        }
                        DoubleRangeRow("Rotten Tomatoes", filters.ratingRt) {
                            onUpdate(filters.copy(ratingRt = it))
                        }
                        DoubleRangeRow("Metacritic", filters.ratingMc) {
                            onUpdate(filters.copy(ratingMc = it))
                        }
                        DoubleRangeRow("Критики", filters.ratingCritics) {
                            onUpdate(filters.copy(ratingCritics = it))
                        }
                    }

                    FilterCategory.Actors -> PersonEditor(
                        hint = "Имя актёра…",
                        selected = filters.actors,
                        onAdd = { onUpdate(filters.copy(actors = filters.actors + it)) },
                        onRemove = { onUpdate(filters.copy(actors = filters.actors - it)) },
                        colors = yoho,
                    )

                    FilterCategory.Crew -> {
                        ValueChips(
                            items = CrewRoleOptions,
                            selected = filters.crewRole?.let { setOf(it) }.orEmpty(),
                            onToggle = {
                                onUpdate(
                                    filters.copy(crewRole = if (filters.crewRole == it) null else it),
                                )
                            },
                            colors = yoho,
                        )
                        PersonEditor(
                            hint = "Имя человека…",
                            selected = filters.crew,
                            onAdd = { onUpdate(filters.copy(crew = filters.crew + it)) },
                            onRemove = { onUpdate(filters.copy(crew = filters.crew - it)) },
                            colors = yoho,
                        )
                    }

                    FilterCategory.ExcludePeople -> PersonEditor(
                        hint = "Имя человека…",
                        selected = filters.excludePeople,
                        onAdd = { onUpdate(filters.copy(excludePeople = filters.excludePeople + it)) },
                        onRemove = { onUpdate(filters.copy(excludePeople = filters.excludePeople - it)) },
                        colors = yoho,
                    )

                    FilterCategory.Tags -> ValueChips(
                        items = catalog.tagFacets().ifEmpty { DefaultTagOptions }.filterQuery(query),
                        selected = filters.tags,
                        onToggle = { onUpdate(filters.copy(tags = filters.tags.toggle(it))) },
                        colors = yoho,
                    )

                    FilterCategory.Language -> ValueChips(
                        items = LanguageOptions.filterQuery(query),
                        selected = filters.language?.let { setOf(it) }.orEmpty(),
                        onToggle = {
                            onUpdate(filters.copy(language = if (filters.language == it) null else it))
                        },
                        colors = yoho,
                    )

                    FilterCategory.Region -> ValueChips(
                        items = facets.regions.ifEmpty { AllRegions }.filterQuery(query),
                        selected = filters.region?.let { setOf(it) }.orEmpty(),
                        onToggle = {
                            onUpdate(
                                filters.copy(
                                    region = if (filters.region == it) null else it,
                                    countries = emptySet(),
                                ),
                            )
                        },
                        colors = yoho,
                    )

                    FilterCategory.Age -> ValueChips(
                        items = AgeOptions,
                        selected = setOf(filters.age ?: "any"),
                        onToggle = {
                            onUpdate(filters.copy(age = if (it == "any") null else it))
                        },
                        colors = yoho,
                    )

                    FilterCategory.Duration -> IntRangeRow(
                        label = "Длительность (мин)",
                        range = filters.duration,
                        onChange = { onUpdate(filters.copy(duration = it)) },
                    )

                    FilterCategory.Votes -> {
                        IntRangeRow("КП", filters.votesKp) {
                            onUpdate(filters.copy(votesKp = it))
                        }
                        IntRangeRow("IMDb", filters.votesImdb) {
                            onUpdate(filters.copy(votesImdb = it))
                        }
                        IntRangeRow("TMDB", filters.votesTmdb) {
                            onUpdate(filters.copy(votesTmdb = it))
                        }
                    }

                    FilterCategory.Awards -> {
                        IntRangeRow("Оскаров", filters.oscars) {
                            onUpdate(filters.copy(oscars = it))
                        }
                        IntRangeRow("Наград", filters.awardsTotal) {
                            onUpdate(filters.copy(awardsTotal = it))
                        }
                    }

                    FilterCategory.Budget -> {
                        LongRangeRow("Бюджет", filters.budget) {
                            onUpdate(filters.copy(budget = it))
                        }
                        LongRangeRow("Сборы в мире", filters.boxOfficeWorld) {
                            onUpdate(filters.copy(boxOfficeWorld = it))
                        }
                        LongRangeRow("Сборы в США", filters.boxOfficeUs) {
                            onUpdate(filters.copy(boxOfficeUs = it))
                        }
                    }

                    FilterCategory.Company -> ValueChips(
                        items = catalog.companyFacets().ifEmpty { DefaultCompanyOptions }.filterQuery(query),
                        selected = filters.companies,
                        onToggle = { onUpdate(filters.copy(companies = filters.companies.toggle(it))) },
                        colors = yoho,
                    )

                    FilterCategory.Platform -> ValueChips(
                        items = catalog.platformFacets().ifEmpty { DefaultPlatformOptions }.filterQuery(query),
                        selected = filters.platforms,
                        onToggle = { onUpdate(filters.copy(platforms = filters.platforms.toggle(it))) },
                        colors = yoho,
                    )

                    FilterCategory.Series -> {
                        IntRangeRow("Сезонов", filters.seasons) {
                            onUpdate(filters.copy(seasons = it))
                        }
                        IntRangeRow("Серий", filters.episodes) {
                            onUpdate(filters.copy(episodes = it))
                        }
                        IntRangeRow("Начало показа", filters.airStart) {
                            onUpdate(filters.copy(airStart = it))
                        }
                        IntRangeRow("Конец показа", filters.airEnd) {
                            onUpdate(filters.copy(airEnd = it))
                        }
                    }

                    FilterCategory.ReleaseDates -> {
                        ValueChips(
                            items = ReleaseStatusOptions,
                            selected = setOf(filters.releaseStatus ?: "all"),
                            onToggle = {
                                onUpdate(filters.copy(releaseStatus = if (it == "all") null else it))
                            },
                            colors = yoho,
                        )
                        DateRangeRow(
                            "Мировая премьера",
                            filters.premiereWorldFrom,
                            filters.premiereWorldTo,
                        ) { from, to ->
                            onUpdate(filters.copy(premiereWorldFrom = from, premiereWorldTo = to))
                        }
                        DateRangeRow(
                            "Премьера в России",
                            filters.premiereRuFrom,
                            filters.premiereRuTo,
                        ) { from, to ->
                            onUpdate(filters.copy(premiereRuFrom = from, premiereRuTo = to))
                        }
                        DateRangeRow(
                            "Цифровой релиз",
                            filters.digitalFrom,
                            filters.digitalTo,
                        ) { from, to ->
                            onUpdate(filters.copy(digitalFrom = from, digitalTo = to))
                        }
                    }

                    FilterCategory.Preset -> ValueChips(
                        items = PresetOptions,
                        selected = setOf(filters.preset ?: "none"),
                        onToggle = {
                            onUpdate(filters.copy(preset = if (it == "none") null else it))
                        },
                        colors = yoho,
                    )

                    FilterCategory.Collection -> ValueChips(
                        items = CollectionOptions,
                        selected = setOf(filters.collection ?: "all"),
                        onToggle = {
                            onUpdate(filters.copy(collection = if (it == "all") null else it))
                        },
                        colors = yoho,
                    )

                    FilterCategory.Sort -> ValueChips(
                        items = SortOptions,
                        selected = setOf(filters.sort),
                        onToggle = { onUpdate(filters.copy(sort = it)) },
                        colors = yoho,
                    )

                    FilterCategory.TitleMode -> {
                        TitleSuggestField(
                            value = filters.titleContains.orEmpty(),
                            onValueChange = {
                                onUpdate(filters.copy(titleContains = it.ifBlank { null }))
                            },
                            onPick = { title ->
                                onUpdate(filters.copy(titleContains = title))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleSuggestField(
    value: String,
    onValueChange: (String) -> Unit,
    onPick: (String) -> Unit,
) {
    var suggestions by remember { mutableStateOf<List<SuggestItemDto>>(emptyList()) }

    LaunchedEffect(value) {
        if (value.trim().length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(280)
        suggestions = runCatching {
            NetworkModule.api.suggest(value.trim(), limit = 8)
        }.getOrDefault(emptyList())
    }

    Column(Modifier.fillMaxWidth().padding(vertical = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp)) {
        Text("Название", fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = BodySp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldH)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = FieldPadH, vertical = FieldPadV),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "Начните вводить название…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = BodySp,
                    )
                }
                inner()
            },
        )
        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 4.dp),
            ) {
                suggestions.forEach { item ->
                    val title = item.title.orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (title.isNotBlank()) {
                                    onPick(title)
                                    suggestions = emptyList()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = item.poster.absMediaUrl(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp, 56.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = BodySp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = listOfNotNull(item.type, item.year?.toString())
                                    .joinToString(" · "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = HintSp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntRangeRow(
    label: String,
    range: IntRangeFilter,
    onChange: (IntRangeFilter) -> Unit,
) {
    val rowPad = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp
    val rowGap = if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp
    val fieldGap = if (LocalHomeMetrics.current.isTablet) 12.dp else 8.dp
    Column(Modifier.fillMaxWidth().padding(vertical = rowPad)) {
        Text(label, fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(rowGap))
        Row(horizontalArrangement = Arrangement.spacedBy(fieldGap)) {
            NumberField(
                value = range.from?.toString().orEmpty(),
                placeholder = "от",
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(from = it.toIntOrNull())) },
            )
            NumberField(
                value = range.to?.toString().orEmpty(),
                placeholder = "до",
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(to = it.toIntOrNull())) },
            )
        }
    }
}

@Composable
private fun DoubleRangeRow(
    label: String,
    range: DoubleRangeFilter,
    onChange: (DoubleRangeFilter) -> Unit,
) {
    val rowPad = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp
    val rowGap = if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp
    val fieldGap = if (LocalHomeMetrics.current.isTablet) 12.dp else 8.dp
    Column(Modifier.fillMaxWidth().padding(vertical = rowPad)) {
        Text(label, fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(rowGap))
        Row(horizontalArrangement = Arrangement.spacedBy(fieldGap)) {
            NumberField(
                value = range.from?.toString().orEmpty(),
                placeholder = "от",
                decimal = true,
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(from = it.toDoubleOrNull())) },
            )
            NumberField(
                value = range.to?.toString().orEmpty(),
                placeholder = "до",
                decimal = true,
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(to = it.toDoubleOrNull())) },
            )
        }
    }
}

@Composable
private fun LongRangeRow(
    label: String,
    range: LongRangeFilter,
    onChange: (LongRangeFilter) -> Unit,
) {
    val rowPad = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp
    val rowGap = if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp
    val fieldGap = if (LocalHomeMetrics.current.isTablet) 12.dp else 8.dp
    Column(Modifier.fillMaxWidth().padding(vertical = rowPad)) {
        Text(label, fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(rowGap))
        Row(horizontalArrangement = Arrangement.spacedBy(fieldGap)) {
            NumberField(
                value = range.from?.toString().orEmpty(),
                placeholder = "от",
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(from = it.toLongOrNull())) },
            )
            NumberField(
                value = range.to?.toString().orEmpty(),
                placeholder = "до",
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(range.copy(to = it.toLongOrNull())) },
            )
        }
    }
}

@Composable
private fun DateRangeRow(
    label: String,
    from: String?,
    to: String?,
    onChange: (String?, String?) -> Unit,
) {
    val rowPad = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp
    val rowGap = if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp
    val fieldGap = if (LocalHomeMetrics.current.isTablet) 12.dp else 8.dp
    Column(Modifier.fillMaxWidth().padding(vertical = rowPad)) {
        Text(label, fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(rowGap))
        Row(horizontalArrangement = Arrangement.spacedBy(fieldGap)) {
            NumberField(
                value = from.orEmpty(),
                placeholder = "с (ГГГГ-ММ-ДД)",
                decimal = false,
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(it.ifBlank { null }, to) },
            )
            NumberField(
                value = to.orEmpty(),
                placeholder = "по (ГГГГ-ММ-ДД)",
                decimal = false,
                modifier = Modifier.weight(1f),
                onValueChange = { onChange(from, it.ifBlank { null }) },
            )
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    val keyboard = if (decimal) {
        KeyboardOptions(keyboardType = KeyboardType.Decimal)
    } else {
        KeyboardOptions(keyboardType = KeyboardType.Number)
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboard,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = BodySp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(FieldH)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = FieldPadH, vertical = FieldPadV),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = BodySp)
            }
            inner()
        },
    )
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (LocalHomeMetrics.current.isTablet) 6.dp else 4.dp),
    ) {
        Text(label, fontSize = BodySp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(if (LocalHomeMetrics.current.isTablet) 8.dp else 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = BodySp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldH)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = FieldPadH, vertical = FieldPadV),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = BodySp)
                }
                inner()
            },
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val padV = if (LocalHomeMetrics.current.isTablet) 10.dp else 4.dp
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = BodySp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = padV),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().height(RowMin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = BodySp)
                }
                inner()
            }
        },
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val padV = if (LocalHomeMetrics.current.isTablet) 8.dp else 2.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowMin)
            .padding(vertical = padV, horizontal = if (LocalHomeMetrics.current.isTablet) 4.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = CategorySp)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonEditor(
    hint: String,
    selected: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    colors: YohoColors,
) {
    var draft by remember { mutableStateOf("") }
    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val name = draft.trim()
                if (name.isNotEmpty()) {
                    onAdd(name)
                    draft = ""
                }
            },
        ),
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = BodySp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (LocalHomeMetrics.current.isTablet) 10.dp else 4.dp)
            .height(FieldH)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = FieldPadH, vertical = FieldPadV),
        decorationBox = { inner ->
            if (draft.isEmpty()) {
                Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = BodySp)
            }
            inner()
        },
    )
    Text("Enter — добавить", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = HintSp)
    Spacer(Modifier.height(if (LocalHomeMetrics.current.isTablet) 12.dp else 6.dp))
    ValueChips(
        items = selected.map { FacetItemDto(it, it) },
        selected = selected,
        onToggle = onRemove,
        colors = colors,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValueChips(
    items: List<FacetItemDto>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    colors: YohoColors,
) {
    val metrics = LocalHomeMetrics.current
    val chipGap = if (metrics.isTablet) metrics.blockGap else 4.dp
    if (items.isEmpty()) {
        Text(
            "Ничего не найдено",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = BodySp,
            modifier = Modifier.padding(if (metrics.isTablet) 16.dp else 8.dp),
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(chipGap),
        verticalArrangement = Arrangement.spacedBy(chipGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = chipGap),
    ) {
        items.forEach { item ->
            val value = item.value ?: return@forEach
            FilterChip(
                selected = value in selected,
                onClick = { onToggle(value) },
                enabled = item.available != false,
                label = {
                    Text(
                        item.label ?: value,
                        fontSize = ChipSp,
                        modifier = Modifier.padding(
                            vertical = if (metrics.isTablet) 4.dp else 0.dp,
                            horizontal = if (metrics.isTablet) 2.dp else 0.dp,
                        ),
                    )
                },
                shape = RoundedCornerShape(if (metrics.isTablet) 14.dp else 10.dp),
                modifier = Modifier.defaultMinSize(minHeight = ChipMinH),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.chipSelected,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = colors.chipUnselected,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null,
            )
        }
    }
}

private fun List<FacetItemDto>.filterQuery(q: String): List<FacetItemDto> {
    if (q.isBlank()) return this
    return filter {
        it.label?.contains(q, true) == true || it.value?.contains(q, true) == true
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun SearchFiltersCatalogDto.tagFacets(): List<FacetItemDto> =
    tags.map { FacetItemDto(value = it.tag, label = it.tag) }

private fun SearchFiltersCatalogDto.platformFacets(): List<FacetItemDto> =
    platforms.map { FacetItemDto(value = it.slug, label = it.name ?: it.slug) }

private fun SearchFiltersCatalogDto.companyFacets(): List<FacetItemDto> =
    companies.map { FacetItemDto(value = it, label = it) }
