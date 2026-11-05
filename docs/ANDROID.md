# Android-клиент YoHo

Кратко по интеграции с [reyoho.com](https://reyoho.com). Полный контракт — [API.md](./API.md), OpenAPI — [openapi.yaml](./openapi.yaml).

## Конфиг

`api.properties` → `BuildConfig.API_BASE_URL` (по умолчанию `https://reyoho.com/`).

Сессия: OkHttp `SessionCookieJar` + Better Auth cookie (`__Secure-better-auth.session_token` / `yoho_session`).

## Используемые API

См. `com.spywer.yoho.data.remote.YohoApi`.

| Экран | Эндпоинты |
|-------|-----------|
| Вход | `POST api/auth/sign-in/email`, `sign-up/email`, `GET get-session`, `POST sign-out` |
| Главная (общий layout) | `trending`, `latest-trailers`, `search/recent`, `recommendations/latest` |
| Главная → Аниме | `GET api/anime` (+ опционально `api/anime/list?sort=&page=`), `recommendations/latest` |
| Каталог | `search/discover`, `facets`, `filters`, `regions`, `recent`, `suggest`, `persons` |
| Карточка | `film-info`, `person/{id}`, `tv-last-air`, `tv-schedule` |
| Плеер | `player-config`, `player-choice`, `player/{type}/{id}` (+ HTML SSR fallback) |
| Избранное | `POST me/activity` (`addFavorite`/`removeFavorite`), список через `GET sync` |
| Дневник | `POST diary`, `GET diary/contains?itemId=`, `DELETE diary?itemId=` |

## Главная: чипы типа

Как на сайте: `/` → all, `/film` → movie, `/series` → tv, `/animation`, `/anime`.

| Чип | Layout | Источники блоков |
|-----|--------|------------------|
| Все / Фильмы / Сериалы / Мультфильмы | Общий | `trending?filter=` (now + popular_*), `latest-trailers`, `search/recent` |
| Аниме | Как `/anime` | `GET /api/anime` → trending, season, popular, top |

Блоки **общего** layout: Рекомендации → Сейчас популярно → Трейлеры → Популярные → Новинки.

Блоки **аниме**: Рекомендации → Сейчас популярно → Популярно в этом сезоне · {сезон год} → Популярное за всё время → Топ 100 аниме (без трейлеров/новинок).

Маппинг `trending` filter (не аниме-layout):

| Чип | Сейчас популярно | Популярные |
|-----|------------------|------------|
| Все | `all` | `popular` |
| Фильмы | `movie` | `popular_movie` |
| Сериалы | `tv` | `popular_tv` |
| Мультфильмы | `animation` | `popular_animation` |

## Каталог / фильтры

Значения query как на `reyoho.com/search` (см. `FilterOptions.kt`, `SearchViewModel`):

- `source` всегда `catalog` (preset/collection — отдельные query)
- `type`: UI `movie` / `tv`→`series,tv_show,mini_series` / `anime` / `animation`→`cartoon`
- рейтинги/диапазоны: `{key}_from` / `{key}_to`
- люди: `actor`+`actor_kp`, `crew`+`crew_kp`(+`crew_role`), `not_person`+`not_person_kp`
- `age`: `6` \| `12` \| `16` \| `18`
- `preset`: `top`, `hidden_gems`, `trending`, `new`, `classics`
- `collection`: `films-best500`, `series-best250`, `all-popular1000`, `all-week250`, `all-day250`
- `sort`: `-popularity`, `-rating_*`, `-votes_*`, `±release_date`, `±runtime`, `-budget`, `-revenue`, `-box_office`, `-oscars_won`, `-awards_won`
- `region`: `cis`, `europe`, `asia`, `america`
- `released`: шлётся только `upcoming` / `any` (дефолт «вышедшие» не передаётся)

## Идентификаторы

- `itemId` / activity `id`: `kp:{kinopoiskId}`
- `href`: `/series/{kp}` для tv/anime, `/film/{kp}` для movie
- Дневник `statusLabel`: русские строки UI («Хочу посмотреть», …)
- `contains` ответ: `{ "inDiary": true, "statusLabel": "…" }`
