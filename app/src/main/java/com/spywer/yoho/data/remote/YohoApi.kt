package com.spywer.yoho.data.remote

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

    /** Персональные рекомендации (нужна сессия). */
    @GET("api/recommendations/latest")
    suspend fun recommendationsLatest(): Response<RecommendationsLatestResponse>

    @GET("api/trending")
    suspend fun trending(@Query("filter") filter: String? = null): TrendingResponse

    @GET("api/latest-trailers")
    suspend fun latestTrailers(
        @Query("start") start: Int = 0,
        @Query("limit") limit: Int = 10,
    ): LatestTrailersResponse

    /** Каталог / фильтры / текстовый поиск — как на сайте. */
    @GET("api/search/discover")
    suspend fun discover(
        @Query("source") source: String = "catalog",
        @Query("q") query: String? = null,
        @Query("type") type: String? = null,
        @Query("genres") genres: String? = null,
        @Query("country") country: String? = null,
        @Query("region") region: String? = null,
        @Query("year_from") yearFrom: Int? = null,
        @Query("year_to") yearTo: Int? = null,
        @Query("rating_min") ratingMin: Double? = null,
        @Query("rating_max") ratingMax: Double? = null,
        @Query("rating_kp_min") ratingKpMin: Double? = null,
        @Query("rating_kp_max") ratingKpMax: Double? = null,
        @Query("rating_imdb_min") ratingImdbMin: Double? = null,
        @Query("rating_imdb_max") ratingImdbMax: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("language") language: String? = null,
        @Query("actors") actors: String? = null,
        @Query("crew") crew: String? = null,
        @Query("crew_role") crewRole: String? = null,
        @Query("exclude_people") excludePeople: String? = null,
        @Query("tags") tags: String? = null,
        @Query("age") age: String? = null,
        @Query("duration_min") durationMin: Int? = null,
        @Query("duration_max") durationMax: Int? = null,
        @Query("votes_kp_min") votesKpMin: Int? = null,
        @Query("votes_kp_max") votesKpMax: Int? = null,
        @Query("companies") companies: String? = null,
        @Query("platforms") platforms: String? = null,
        @Query("preset") preset: String? = null,
        @Query("collection") collection: String? = null,
        @Query("title") titleContains: String? = null,
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
    ): List<SuggestItemDto>

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
     * Без yoho_session часто 451 taken_down.
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
