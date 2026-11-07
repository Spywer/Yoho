package com.spywer.yoho.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface YohoApi {
    @POST("api/auth/sign-in/email")
    suspend fun signInEmail(@Body body: SignInEmailRequest): Response<AuthSessionResponse>

    @POST("api/auth/sign-up/email")
    suspend fun signUpEmail(@Body body: SignUpEmailRequest): Response<AuthSessionResponse>

    @POST("api/auth/sign-out")
    suspend fun signOut(): Response<Unit>

    /** Better Auth. Без сессии — 200 + null body. */
    @GET("api/auth/get-session")
    suspend fun authGetSession(): Response<AuthSessionResponse>

    /** Профиль текущего пользователя: роль, код, подписки. */
    @GET("api/profile/me")
    suspend fun profileMe(): Response<ProfileMeResponseDto>

    /** Персональные рекомендации (нужна сессия). */
    @GET("api/recommendations/latest")
    suspend fun recommendationsLatest(): Response<RecommendationsLatestResponse>

    @GET("api/trending")
    suspend fun trending(@Query("filter") filter: String? = null): TrendingResponse

    /** Подборки страницы /anime: trending + season + popular + top. */
    @GET("api/anime")
    suspend fun animeHome(): AnimeHomeResponse

    @GET("api/anime/list")
    suspend fun animeList(
        @Query("sort") sort: String,
        @Query("page") page: Int,
    ): AnimeListResponse

    @GET("api/latest-trailers")
    suspend fun latestTrailers(
        @Query("start") start: Int = 0,
        @Query("limit") limit: Int = 10,
    ): LatestTrailersResponse

    /**
     * Каталог / фильтры — query как на reyoho.com/search
     * (ranges: `{key}_from`/`{key}_to`, люди: actor+actor_kp, теги/студии: append).
     */
    @GET("api/search/discover")
    suspend fun discover(
        @Query("source") source: String = "catalog",
        @Query("q") query: String? = null,
        @Query("type") type: String? = null,
        @Query("genres") genres: String? = null,
        @Query("country") country: String? = null,
        @Query("region") region: String? = null,
        @Query("language") language: String? = null,
        @Query("year_from") yearFrom: Int? = null,
        @Query("year_to") yearTo: Int? = null,
        @Query("runtime_from") runtimeFrom: Int? = null,
        @Query("runtime_to") runtimeTo: Int? = null,
        @Query("rating_kp_from") ratingKpFrom: Double? = null,
        @Query("rating_kp_to") ratingKpTo: Double? = null,
        @Query("rating_imdb_from") ratingImdbFrom: Double? = null,
        @Query("rating_imdb_to") ratingImdbTo: Double? = null,
        @Query("rating_tmdb_from") ratingTmdbFrom: Double? = null,
        @Query("rating_tmdb_to") ratingTmdbTo: Double? = null,
        @Query("rating_rt_from") ratingRtFrom: Double? = null,
        @Query("rating_rt_to") ratingRtTo: Double? = null,
        @Query("rating_metacritic_from") ratingMetacriticFrom: Double? = null,
        @Query("rating_metacritic_to") ratingMetacriticTo: Double? = null,
        @Query("rating_critics_from") ratingCriticsFrom: Double? = null,
        @Query("rating_critics_to") ratingCriticsTo: Double? = null,
        @Query("votes_kp_from") votesKpFrom: Int? = null,
        @Query("votes_kp_to") votesKpTo: Int? = null,
        @Query("votes_imdb_from") votesImdbFrom: Int? = null,
        @Query("votes_imdb_to") votesImdbTo: Int? = null,
        @Query("votes_tmdb_from") votesTmdbFrom: Int? = null,
        @Query("votes_tmdb_to") votesTmdbTo: Int? = null,
        @Query("oscars_won_from") oscarsWonFrom: Int? = null,
        @Query("oscars_won_to") oscarsWonTo: Int? = null,
        @Query("awards_won_from") awardsWonFrom: Int? = null,
        @Query("awards_won_to") awardsWonTo: Int? = null,
        @Query("budget_from") budgetFrom: Long? = null,
        @Query("budget_to") budgetTo: Long? = null,
        @Query("revenue_from") revenueFrom: Long? = null,
        @Query("revenue_to") revenueTo: Long? = null,
        @Query("box_office_from") boxOfficeFrom: Long? = null,
        @Query("box_office_to") boxOfficeTo: Long? = null,
        @Query("seasons_total_from") seasonsTotalFrom: Int? = null,
        @Query("seasons_total_to") seasonsTotalTo: Int? = null,
        @Query("episodes_total_from") episodesTotalFrom: Int? = null,
        @Query("episodes_total_to") episodesTotalTo: Int? = null,
        @Query("start_year_from") startYearFrom: Int? = null,
        @Query("start_year_to") startYearTo: Int? = null,
        @Query("end_year_from") endYearFrom: Int? = null,
        @Query("end_year_to") endYearTo: Int? = null,
        @Query("premiere_world_from") premiereWorldFrom: String? = null,
        @Query("premiere_world_to") premiereWorldTo: String? = null,
        @Query("premiere_ru_from") premiereRuFrom: String? = null,
        @Query("premiere_ru_to") premiereRuTo: String? = null,
        @Query("digital_release_from") digitalReleaseFrom: String? = null,
        @Query("digital_release_to") digitalReleaseTo: String? = null,
        @Query("released") released: String? = null,
        @Query("sort") sort: String? = null,
        @Query("actor") actor: List<String>? = null,
        @Query("actor_kp") actorKp: List<Int>? = null,
        @Query("crew") crew: String? = null,
        @Query("crew_kp") crewKp: Int? = null,
        @Query("crew_role") crewRole: String? = null,
        @Query("not_person") notPerson: List<String>? = null,
        @Query("not_person_kp") notPersonKp: List<Int>? = null,
        @Query("tag") tag: List<String>? = null,
        @Query("company") company: List<String>? = null,
        @Query("platform") platform: String? = null,
        @Query("age") age: String? = null,
        @Query("preset") preset: String? = null,
        @Query("collection") collection: String? = null,
        @Query("only_with_player") onlyWithPlayer: Boolean? = null,
        @Query("exclude_watched") excludeWatched: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
    ): DiscoverResponseDto

    @GET("api/search/facets")
    suspend fun searchFacets(): SearchFacetsDto

    @GET("api/search/filters")
    suspend fun searchFiltersCatalog(): SearchFiltersCatalogDto

    @GET("api/search/regions")
    suspend fun searchRegions(): RegionMappingDto

    @GET("api/search/recent")
    suspend fun searchRecent(): RecentListResponse

    @GET("api/search/suggest")
    suspend fun suggest(
        @Query("q") query: String,
        @Query("limit") limit: Int = 8,
    ): SuggestResponseDto

    /** Автодополнение людей: /api/search/persons?q= (в фильтрах — source=kinodata). */
    @GET("api/search/persons")
    suspend fun searchPersons(
        @Query("q") query: String,
        @Query("source") source: String? = null,
    ): SearchPersonsResponse

    /** Детали тайтла как на сайте: /api/film-info?kinopoiskId=&tmdbId=&mediaType= */
    @GET("api/film-info")
    suspend fun filmInfo(
        @Query("kinopoiskId") kinopoiskId: Int? = null,
        @Query("tmdbId") tmdbId: Int? = null,
        @Query("mediaType") mediaType: String? = null,
    ): FilmInfoResponseDto

    /** Персона KP: /api/person/{id} */
    @GET("api/person/{id}")
    suspend fun person(@Path("id") id: Int): PersonDto

    /** Избранное / активность: POST { op: addFavorite|removeFavorite, item } → 204 */
    @POST("api/me/activity")
    suspend fun meActivity(@Body body: MeActivityRequest): Response<Unit>

    /**
     * setRating с явным `"value":null` (дизлайк) — через JsonObject,
     * иначе Gson выкидывает null-поля.
     */
    @POST("api/me/activity")
    suspend fun meActivityJson(@Body body: JsonObject): Response<Unit>

    /** Дневник: POST /api/diary → 201 { entry } */
    @POST("api/diary")
    suspend fun diary(@Body body: DiaryRequestDto): Response<DiaryResponseDto>

    /** Есть ли в дневнике: GET /api/diary/contains?itemId=kp:… */
    @GET("api/diary/contains")
    suspend fun diaryContains(@Query("itemId") itemId: String): Response<DiaryContainsResponseDto>

    /** Удалить из дневника: DELETE /api/diary?itemId=kp:… → { ok, removed } */
    @DELETE("api/diary")
    suspend fun diaryRemove(@Query("itemId") itemId: String): Response<DiaryRemoveResponseDto>

    /** Избранное + история: GET /api/sync → { ok, favorites, watchHistory } */
    @GET("api/sync")
    suspend fun sync(): SyncResponseDto

    /** Свой дневник: GET /api/diary?page=&limit= → { entries, total, page, pageSize } */
    @GET("api/diary")
    suspend fun diaryList(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): DiaryListResponseDto

    /** Оценки like/dislike текущего пользователя (сессия). */
    @GET("api/ratings")
    suspend fun userRatings(): UserRatingsResponse

    /** Последняя / следующая серия: /api/tv-last-air?tmdbId= */
    @GET("api/tv-last-air")
    suspend fun tvLastAir(@Query("tmdbId") tmdbId: Int): TvLastAirDto

    /** Расписание сериала: /api/tv-schedule?tmdbId=&kinopoiskId=&release=1 */
    @GET("api/tv-schedule")
    suspend fun tvSchedule(
        @Query("tmdbId") tmdbId: Int? = null,
        @Query("kinopoiskId") kinopoiskId: Int? = null,
        @Query("release") release: Int = 1,
    ): TvScheduleDto

    /** Голоса за источники плеера: /api/player-choice?itemId=kp:123 */
    @GET("api/player-choice")
    suspend fun playerChoice(@Query("itemId") itemId: String): PlayerChoiceDto

    /** Кто смотрит: /api/film/watchers?tmdbId=&kinopoiskId=&mediaType= */
    @GET("api/film/watchers")
    suspend fun filmWatchers(
        @Query("tmdbId") tmdbId: Int? = null,
        @Query("kinopoiskId") kinopoiskId: Int? = null,
        @Query("mediaType") mediaType: String? = null,
    ): FilmWatchersDto

    /**
     * Конфиг iframe-источников (как `initialPlayerConfig` на сайте):
     * /api/player-config?kinopoiskId=&tmdbId=&mediaType=
     * Без сессии часто 451 taken_down (login-wall); с сессией пустой/451 → нет источников.
     */
    @GET("api/player-config")
    suspend fun playerConfig(
        @Query("kinopoiskId") kinopoiskId: Int? = null,
        @Query("tmdbId") tmdbId: Int? = null,
        @Query("mediaType") mediaType: String? = null,
    ): retrofit2.Response<PlayerConfigDto>

    /** Legacy: /api/player/{type}/{id} — fallback, если config пустой */
    @GET("api/player/{type}/{id}")
    suspend fun player(
        @Path("type") type: String,
        @Path("id") id: String,
    ): PlayerResponseDto
}
