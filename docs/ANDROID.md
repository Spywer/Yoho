# Android-клиент YoHo

Кратко по интеграции с [reyoho.com](https://reyoho.com). Полный контракт — [API.md](./API.md), OpenAPI — [openapi.yaml](./openapi.yaml).

## Конфиг

Зеркала в `SiteHttp.CANDIDATES`: `reyoho.com`, `reyoho.ru`, `yoho.pw`.

Активный хост: `SiteHttp.HOST` → `ORIGIN` / `REFERER` (persist в `site_endpoints`).

При `IOException` или HTTP 502/503/504 OkHttp failover: cooldown **10 мин** на мёртвое зеркало, перенос session-cookie на следующее (`SessionCookieJar.migrateSessionTo`), retry без участия пользователя.

Сессия: OkHttp `SessionCookieJar` + Better Auth cookie (`__Secure-better-auth.session_token` / `yoho_session`).

## Используемые API

См. `com.spywer.yoho.data.remote.YohoApi`.

| Экран | Эндпоинты |
|-------|-----------|
| Вход | `POST api/auth/sign-in/email`, `sign-up/email`, `GET get-session`, `POST sign-out` |
| Главная (общий layout) | `trending`, `latest-trailers`, `search/recent`, `recommendations/latest` |
| Главная → поиск | `GET api/search/suggest?q=`, `GET api/search/persons?q=` (параллельно, debounce) |
| Главная → Аниме | `GET api/anime` (+ опционально `api/anime/list?sort=&page=`), `recommendations/latest` |
| Каталог | `search/discover`, `facets`, `filters`, `regions`, `recent` (+ `persons` в фильтре людей) |
| Карточка | `film-info`, `person/{id}`, `tv-last-air`, `tv-schedule` |
| Плеер | `player-config`, `player-choice`, `player/{type}/{id}` (+ HTML SSR fallback без сессии) |
| Коллекция (хаб) | `GET diary?page&limit`, `GET sync`, опц. `GET profile/{username}/films`, `POST posters/resolve` |
| Избранное / История | `GET sync`; избранное: `POST me/activity` (`addFavorite`/`removeFavorite`) |
| Дневник | `GET diary`, `POST diary`, `GET diary/contains?itemId=`, `DELETE diary?itemId=` |
| Реакции like/dislike | `POST me/activity` (setRating) + `POST diary` (`sourceReaction=like\|dislike`); список: `GET ratings` |

## Коллекция

Вкладка нижней навигации **«Коллекция»** (`MainTab.Favorites` / `nav_favorites`).

### IA

| UI | Пакет / класс | Данные |
|----|---------------|--------|
| Хаб (список + сетка) | `ui/collection/CollectionHubScreen` + `CollectionHubViewModel` | меню полок + «Мои фильмы и сериалы» |
| Избранное | `FavoritesListActivity` → `CollectionScreen` (`CollectionShelf.Favorites`) | `sync.favorites` |
| Дневник | `DiaryActivity` → `CollectionShelf.Diary` | `GET /api/diary` |
| История | `HistoryActivity` → `CollectionShelf.History` | `sync.watchHistory` |

- Общий UI полки: `CollectionScreen` + `CollectionViewModel(initialShelf)` (factory).
- На телефоне кнопка «назад» на полках скрыта (системная Back); на планшете — `YohoBackButton` слева от заголовка.
- `load()` грузит только нужную полку (sync **или** diary), с отменой предыдущего Job.

### Хаб: «Мои фильмы и сериалы»

Как на сайте `/{username}/films` — **не** копия дневника:

1. Сначала `GET /api/profile/{username}/films` (если есть username).
2. Иначе **дневник ∪ избранное** (`GET diary` + `GET sync.favorites`), сортировка по свежести.
3. Пустые постеры — `POST /api/posters/resolve`.
4. В state — `HubFilmCard` (film + кэш `TitleDto`), сетка как `PosterCard` / `metrics.poster`.

### Дневник: статус и реакция

- `statusLabel` (6 русских статусов) и `sourceReaction` (`like` / `dislike` / `manual`) **независимы** и могут быть вместе.
- Смена статуса не затирает like/dislike; «Удалить статус» при реакции чистит только `statusLabel`, иначе `DELETE diary`.
- Фильтры: чипы статусов + выпадающий список оценок («Все оценки» / Понравилось / Не понравилось), фильтры комбинируются (AND).
- `displayed*` (favorites/history/diary) — поля state, пересчёт через `withDisplayed()` при смене данных/сорта/фильтра.

### Аккаунт (навигация)

Подстраницы (Друзья, Сообщения, Настройки, О сервисе, О приложении): стрелка «назад» слева от заголовка; **видима только на планшете** (`showBack = metrics.isTablet`), как на полках коллекции. Логин из полки → `MainActivity.intentOpenAccount` (`StateFlow` вкладки, без `recreate()`).

## Главная: чипы типа

Как на сайте: `/` → all, `/film` → movie, `/series` → tv, `/animation`, `/anime`.

| Чип | Layout | Источники блоков |
|-----|--------|------------------|
| Все / Фильмы / Сериалы / Мультфильмы | Общий | `trending?filter=` (now + popular_*), `latest-trailers`, `search/recent` |
| Аниме | Как `/anime` | `GET /api/anime` → trending, season, popular, top |

Блоки **общего** layout: Рекомендации → Сейчас популярно → Трейлеры → Популярные → Новинки.

Блоки **аниме**: Рекомендации → Сейчас популярно → Популярно в этом сезоне · {сезон год} → Популярное за всё время → Топ 100 аниме (без трейлеров/новинок).

### Главная: поиск (как на `/`)

Тап по поиску **не** открывает вкладку «Каталог» — поле ввода на месте.

| Что | Как |
|-----|-----|
| API | параллельно `suggest` + `persons` (от 2 символов, debounce ~280 ms) |
| Suggest ответ | `{ results: [...] }` — `title`, `originalTitle`, `year`, `mediaType`, `posterUrl`, `kinopoiskId`, `tmdbId`, `genres`, `ratingKinopoisk` / `ratingImdb` |
| UI строки | тип RU · год · жанры; справа КП/IM; лайк в избранное (`me/activity`); оригинал через `—` приглушённо |
| Чипы типа | скрываются, пока видна выдача (≥2 символа) |
| Фильтры (иконка) | по-прежнему → Каталог |
| Клавиатура | при уходе с Каталога / открытии карточки — `clearFocus` + hide IME (`MainShell`) |

На **Каталоге** dropdown suggest **нет** — только `discover` по query/фильтрам. `suggest` в фильтрах названия/людей внутри `FilterSheet` остаётся.

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
- Дневник `sourceReaction`: `manual` \| `like` \| `dislike` (лайк/дизлайк не взаимоисключают статус)
- `contains` ответ: `{ "inDiary": true, "statusLabel": "…" }`

## Плеер и трейлеры

Две разные поверхности — не смешивать поведение ориентации и lifecycle.

### Карточка: кино-плеер (`ui/detail`)

| Что | Как |
|-----|-----|
| UI | `PlayerBlock` + `PlayerWebView` в `TitleDetailScreen` |
| Конфиг | сначала `GET player-config`; без сессии — SSR `initialPlayerConfig` из HTML карточки |
| Заглушка | сессия есть и `player-config` пустой / `451 {error:taken_down}` → **«Нет доступных источников…»** (как на сайте). `player-choice` на заглушку **не** влияет |
| Без сессии | HTML `takenDownReason=login` → текст про вход; иначе no-sources |
| Скролл деталки | `Column` + `verticalScroll` (не `LazyColumn`): иначе при ландшафте item плеера dispose → мигание FS/WebView |
| WebView | `PlayerWebViewHolder` живёт на экране деталки (`remember` по id), `destroy()` при уходе |
| Cinema FS | reparent WebView на `window.decorView` (не Dialog): Dialog ломал surface при возврате |
| HTML5 FS | отдельный слой на decor (`onShowCustomView` / `CustomViewCallback`) |
| Выход из cinema | только крестик / Back / `exitCinema` — не tear-down по `enabled=false` composition |
| Ориентация в FS | `SCREEN_ORIENTATION_SENSOR` только пока cinema/HTML FS активен; иначе прежняя ориентация Activity |
| System bars | скрываются на время FS (`PlayerSystemBarsEffect`); в PiP не трогаем |
| Activity | `TitleDetailActivity` / `MainActivity`: `configChanges` orientation\|screenSize\|… без recreate |
| PiP | `MainActivity` + `TitleDetailActivity`: Home → `onUserLeaveHint` / `setAutoEnterEnabled`; eligible = плеер загружен; в PiP без `WebView.onPause()`. Трейлеры — без PiP |

Collaps и похожие iframe: shell/`putShell` на стороне моделей плеера — чтобы не перезагружать мусор при смене опций.

### Трейлеры (`ui/trailer`)

| Что | Как |
|-----|-----|
| Вход | `Context.openTrailerPlayer(...)` с Home и Detail |
| UI | `TrailerPlayerActivity` + `TrailerPlayerScreen` (YouTube IFrame lib / VK WebView) |
| Зачем Activity | своё окно: не крутит SENSOR/config у Main/Detail и не бьёт кино-плеер |
| Ориентация | **не** форсим (`screenOrientation` не задаём) — решает система / автоповорот пользователя |
| YouTube FS | `FullscreenListener` → view на decor |
| VK FS | `onShowCustomView` → decor; при выходе обязателен `CustomViewCallback.onCustomViewHidden()` |
| System bars | только на окне трейлера (`TrailerFullscreenSystemBarsEffect`) |

Не возвращать трейлер в Compose `Dialog` на хосте деталки: ориентация и WebView surface снова поедут в кино-плеер.

### Утечки / cleanup (кратко)

- Holder: `destroy()` → detach cinema, HTML FS + callback, blank, `WebView.destroy()`
- Overlay FS: снять `OnLayoutChangeListener` и `postDelayed` при detach
- Popup `onCreateWindow`: уничтожать в `shouldOverrideUrlLoading` / `onCloseWindow` (+ таймаут)
- Трейлер YouTube: `lifecycle.removeObserver` + `release()` в `onRelease`
