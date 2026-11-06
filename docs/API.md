# YoHo API Specification

**Версия:** 0.1.42
**Дата публикации:** 5 ноября 2026
**Базовый URL:** `https://reyoho.com` | `https://reyoho.ru`
**Протокол:** HTTPS (HTTP/2)
**Формат обмена:** JSON (UTF-8)
**Контакты:** reyoho@proton.me

> Актуализация 0.1.42: страница `/anime` (`GET /api/anime`), фильтры discover (preset/collection/age/sort), Android home-layout для аниме.

---

## 1. Обзор

YoHo API предоставляет программный интерфейс для доступа к каталогу аудиовизуального контента (фильмы, сериалы, аниме, мультфильмы) и сопутствующим социальным функциям.

**Объём базы данных:** более **1 677 921** единицы контента с метаданными из Kinopoisk, TMDB, IMDb и Rotten Tomatoes.

### 1.1. Архитектура

| Компонент | Технология |
|-----------|-----------|
| Фреймворк | Next.js 15+ (App Router) |
| Рендеринг | React Server Components + Suspense Streaming + Partial Prerendering |
| Сборка | Turbopack |
| Серверная инфраструктура | Nginx (основной), Angie (медиа-прокси) |
| Аутентификация | Better Auth (cookie-based) |
| PWA | Service Worker + Manifest |

### 1.2. Стратегия кеширования

| Класс ресурса | Cache-Control | X-Cache-Status | TTL |
|---------------|---------------|----------------|-----|
| Публичные каталоги | `public` | `HIT` | 300 секунд |
| Персонализированные страницы | `no-store` | `BYPASS` | 0 |
| Изображения и статика | `immutable` | `HIT` | 1 год |
| TV Last Air batch | `public, s-maxage=300, stale-while-revalidate=3600` | HIT | 5 мин + SWR 1 ч |
| Player tags | `private, max-age=60` | — | 60 сек (клиент) |
| API endpoints | `no-store` | `BYPASS` | 0 |

### 1.3. Стандартные заголовки ответа

| Заголовок | Описание |
|-----------|----------|
| `X-Cache-Status` | Статус CDN: `HIT`, `MISS`, `BYPASS`, `EXPIRED`, `STALE`, `REVALIDATED` |
| `X-Nextjs-Stale-Time` | Время жизни RSC-кеша (секунды) |
| `X-Nextjs-Postponed` | Флаг Partial Prerendering (`0` / `1` / `2`) |
| `X-Nextjs-Cache` | Статус ISR-кеша: `HIT`, `MISS`, `STALE` |
| `X-Yoho-Pathname` | Внутренний маршрут приложения |
| `X-Request-Id` | Уникальный идентификатор запроса (UUID v4) |

---

## 2. Аутентификация

### 2.1. Общая модель

Аутентификация — cookie Better Auth. После входа обычно присутствуют:

```
__Secure-better-auth.session_token=<token>; Path=/; HttpOnly; Secure; SameSite=Lax
```

На части сценариев / legacy также встречается `yoho_session`. Клиенты должны слать cookie с `credentials: include` (или OkHttp CookieJar). Проверка сессии:

```http
GET /api/auth/get-session
```

Без сессии — `200` с пустым/`null` телом.

### 2.2. Доступные методы

| Метод | Описание |
|-------|----------|
| Email/Password | Классическая регистрация и вход |
| Twitch OAuth | Авторизация через стриминговую платформу |
| Kick OAuth | Авторизация через стриминговую платформу |
| Google OAuth | Авторизация через Google-аккаунт |

### 2.3. Эндпоинты

#### Вход по email

```http
POST /api/auth/sign-in/email
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret123",
  "callbackURL": "/anime"
}
```

**Успешный ответ (200):**
```json
{
  "user": {
    "id": "AkhwTfq0hCpMEfbvzKzjIGO6cYJMLzlM",
    "username": "spywer",
    "user_code": "#90360",
    "email": "user@example.com",
    "display_name": "Spywer",
    "role": "user",
    "avatar": "/api/profile/avatar/AkhwTfq0hCpMEfbvzKzjIGO6cYJMLzlM?v=1790370461167",
    "avatar_source": "internal",
    "registered_at": "2026-09-23T00:00:00Z",
    "is_private": false
  },
  "session": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "expiresAt": "2026-12-03T20:53:14Z"
  },
  "redirectTo": "/anime"
}
```

**Ответ при неверных данных (401):**
```json
{
  "message": "Invalid email or password",
  "code": "INVALID_EMAIL_OR_PASSWORD"
}
```

#### Регистрация

```http
POST /api/auth/sign-up/email
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret123",
  "username": "newuser",
  "name": "Display Name"
}
```

#### OAuth-авторизация

```http
POST /api/auth/sign-in/{provider}
Content-Type: application/json

{ "callbackURL": "/anime" }
```

Где `{provider}`: `twitch`, `kick`, `google`.

#### Дополнительные операции

| Метод | URL | Назначение |
|-------|-----|-----------|
| `POST` | `/api/auth/sign-out` | Завершение сессии |
| `GET` | `/api/auth/session` | Проверка текущей сессии |
| `POST` | `/api/auth/forgot-password` | Инициация восстановления пароля |
| `POST` | `/api/auth/reset-password` | Установка нового пароля по токену |
| `GET` | `/api/auth/verify-email` | Подтверждение email-адреса |

---

## 3. Модель контента

### 3.1. Типы контента

**Внешние типы (ContentMediaType)** — используются в API-запросах:

| Значение | Описание |
|----------|----------|
| `movie` | Фильм |
| `tv` | Сериал |
| `anime` | Аниме |
| `animation` | Мультфильм |

**Внутренние типы (ContentKind)** — полный каталог, возвращается в `/api/search/filters`:

| Значение | Количество | Описание |
|----------|-----------|----------|
| `movie` | 756 961 | Фильмы |
| `short` | 610 712 | Короткометражки |
| `series` | 184 275 | Сериалы |
| `cartoon` | 84 131 | Мультфильмы |
| `tv_show` | 35 756 | ТВ-шоу |
| `anime` | 9 050 | Аниме |
| `mini_series` | 989 | Мини-сериалы |

### 3.2. Модель фильма

```json
{
  "id_kp": 326,
  "id_tmdb": 278,
  "media_type": "movie",
  "title": {
    "ru": "Побег из Шоушенка",
    "original": "The Shawshank Redemption"
  },
  "year": 1994,
  "description": "...",
  "tagline": "Страх — это кандалы. Надежда — это свобода",
  "duration": { "minutes": 142, "formatted": "2 ч 22 мин" },
  "age_rating": "16+",
  "ratings": {
    "kinopoisk": { "score": 9.1, "votes": 1000000, "url": "https://..." },
    "imdb": { "score": 9.3, "votes": 2500000, "url": "https://..." },
    "tmdb": { "score": 8.7, "votes": 31699 },
    "rottentomatoes": { "score": "91%", "url": "https://..." }
  },
  "genres": ["драма", "криминал"],
  "countries": ["США"],
  "director": "Фрэнк Дарабонт",
  "cast": [
    { "id": 123, "name": "Тим Роббинс", "role": "Энди Дюфрейн", "poster": "/api/img/..." }
  ],
  "crew": {
    "screenplay": ["Стивен Кинг", "Фрэнк Дарабонт"],
    "producer": ["Лиз Глоцер", "Дэвид В. Лестер"],
    "cinematography": "Роджер Дикинс",
    "composer": "Томас Ньюман",
    "editor": "Ричард Фрэнсис-Брюс"
  },
  "production": {
    "distributor": "Columbia Pictures (Sony Pictures)",
    "rights_holder": "Castle Rock Entertainment (Warner Bros.)",
    "budget": "$25 000 000"
  },
  "poster": "/api/img/t/p/w500/qXB2JGRQmGtR9X5QqJjPqZ7XqzY.jpg",
  "trailer": {
    "url": "https://youtube.com/watch?v=...",
    "title": "Official Trailer",
    "date": "2024-01-15",
    "thumbnail": "/api/img/t/p/w1280/..."
  },
  "similar": [456, 789],
  "sequels": [],
  "prequels": [],
  "remakes": []
}
```

### 3.3. Модель сериала

```json
{
  "id_kp": 464963,
  "id_tmdb": 1399,
  "media_type": "tv",
  "title": { "ru": "Игра престолов", "original": "Game of Thrones" },
  "year_start": 2011,
  "year_end": 2019,
  "status": "SERIAL_COMPLETE",
  "age_rating": "18+",
  "ratings": { "..." : "..." },
  "genres": ["драма", "фэнтези", "приключения"],
  "countries": ["США", "Великобритания"],
  "directors": ["Дэвид Наттер", "Алан Тейлор"],
  "composer": "Рамин Джавади",
  "cast_total": 784,
  "awards_count": 99,
  "totals": { "seasons": 8, "episodes": 73 },
  "seasons": [
    {
      "number": 1,
      "name": "1 сезон",
      "episodes_count": 10,
      "date_start": "2011-04-17",
      "period": "апрель 2011 – июнь 2011",
      "status": "completed",
      "episodes": [
        {
          "number": 1,
          "title": "Зима близко",
          "title_original": "Winter Is Coming",
          "air_date": "2011-04-17",
          "air_time": "≈04:00",
          "duration": 62,
          "description": "...",
          "poster": "/api/img/...",
          "status": "released"
        }
      ]
    }
  ]
}
```

**Статусы сериала:** `SERIAL_ONGOING`, `SERIAL_COMPLETE`, `SEASON_COMPLETE`
**Статусы эпизода:** `released`, `upcoming`
**Статусы шоу (TMDB):** `Returning Series`, `Ended`, `In Production`, `Canceled`

### 3.4. Эндпоинты контента

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/films` | Список фильмов с фильтрами |
| `GET` | `/api/films/{id}` | Детали фильма |
| `GET` | `/api/films/{id}/timings` | Тайминги фильма |
| `POST` | `/api/films/{id}/timings` | Добавить тайминг |
| `GET` | `/api/film/watchers?tmdbId={id}&kinopoiskId={kpId}&mediaType=tv` | Зрители с оценками |
| `GET` | `/api/series` | Список сериалов |
| `GET` | `/api/series/{id}` | Детали сериала |
| `GET` | `/api/series/{id}/seasons` | Список сезонов |
| `GET` | `/api/series/{id}/seasons/{n}` | Эпизоды сезона |
| `GET` | `/api/tv-last-air?tmdbId={id}` | Последняя вышедшая серия |
| `GET` | `/api/tv-schedule?tmdbId={id}&release=1&kinopoiskId={kpId}` | Полное расписание |
| `POST` | `/api/tv-last-air/batch` | Батч последних серий |

### 3.5. Модель последней серии

```json
{
  "fp": "8|6|2019-05-19",
  "meta": {
    "tmdbId": 1399,
    "seasonNumber": 8,
    "episodeNumber": 6,
    "airDate": "2019-05-19",
    "fingerprint": "8|6|2019-05-19",
    "showStatus": "Ended",
    "inProduction": false,
    "airAt": 1558314000000,
    "airPrecision": "estimated",
    "nextSeasonNumber": 38,
    "nextEpisodeNumber": 6,
    "nextAirDate": "2026-11-08",
    "nextAirAt": 1794187800000,
    "nextAirPrecision": "exact"
  },
  "badge": "СЕРИАЛ ЗАВЕРШЁН"
}
```

### 3.6. Модель расписания серий

```json
{
  "tmdbId": 1399,
  "seasons": [
    {
      "seasonNumber": 8,
      "episodes": [
        {
          "seasonNumber": 8,
          "episodeNumber": 6,
          "title": "The Iron Throne",
          "airDate": "2019-05-19"
        }
      ]
    }
  ],
  "release": {
    "context": {
      "networks": ["HBO"],
      "originCountry": ["US"],
      "airTimes": {},
      "shikimoriNextAt": null
    },
    "dubs": {},
    "journal": {
      "since": 1790943406941,
      "updatedAt": 1793735588815
    }
  }
}
```

### 3.7. Модель зрителей тайтла

```json
{
  "watchers": [
    {
      "id": "MlLhGs4hhk5L95nIaOv3bfehOIl6Xq10",
      "code": "91789",
      "name": "Ruslan 🥀",
      "avatar": "/api/profile/avatar/...",
      "isStreamer": false,
      "isLive": false,
      "score": 9,
      "rating": "like",
      "subscribed": false
    }
  ]
}
```

---

## 4. Поиск

### 4.1. Ключевые соглашения API

| Аспект | Правило |
|--------|---------|
| **Множественные значения** | CSV через запятую в одном параметре (`genres=action,drama`) |
| **Страны** | ISO 3166-1 alpha-2: `GB`, `AU`, `US`, `RU`, `SU` |
| **Типы контента** | ISO-подобные: `movie`, `tv`, `anime`, `animation` |
| **Жанры** | Английские slug'и в нижнем регистре: `action`, `film_noir`, `speculative_fiction` |
| **Годы** | Диапазон `year_from` + `year_to` (оба включительно) |
| **Регионы** | Пресеты: `cis`, `europe`, `asia`, `america`, `africa`, `oceania` |

### 4.2. Базовый запрос

```http
GET /api/search
  ?q=текст
  &type=movie
  &genres=action,war,mystery,crime,romance
  &country=GB,AU,AT
  &region=europe
  &year_from=2020
  &year_to=2026
  &rating_min=7.0
  &rating_max=10.0
  &sort=popularity
  &sort_order=desc
  &language=ru
  &only_with_player=false
  &exclude_watched=false
  &page=1
  &limit=20
```

### 4.3. Перечень параметров

| Параметр | Тип | Описание |
|----------|-----|----------|
| `q` | string | Поисковый запрос |
| `type` | enum | `all`, `movie`, `tv`, `anime`, `animation` |
| `genres` | string | Жанры через запятую |
| `country` | string | ISO 3166-1 alpha-2 коды через запятую |
| `region` | enum | Региональные пресеты |
| `year_from` | integer | Начало диапазона года |
| `year_to` | integer | Конец диапазона года |
| `rating_min` | number | Минимальная оценка (0–10) |
| `rating_max` | number | Максимальная оценка (0–10) |
| `sort` | enum | `rating`, `year`, `popularity`, `date_added` |
| `sort_order` | enum | `asc`, `desc` |
| `language` | enum | `ru`, `en`, `original`, `any` |
| `only_with_player` | boolean | Только тайтлы с плеером |
| `exclude_watched` | boolean | Исключить просмотренные (требует авторизации) |
| `page` | integer | Номер страницы (от 1) |
| `limit` | integer | Количество результатов (1–100) |

### 4.4. Полный перечень жанров (25 значений)

```
action, adventure, biography, comedy, crime, documentary, drama, family,
fantasy, film_noir, history, horror, kids, music, musical, mystery,
mysticism, romance, speculative_fiction, sport, thriller, war, western
```

**Важные различия:**
- `mystery` (детектив) ≠ `mysticism` (мистика)
- `kids` (детский) ≠ `family` (семейный)
- `speculative_fiction` — обобщающая категория фантастики (замена `sci-fi`)

### 4.5. Региональные пресеты

| Регион | Включает страны |
|--------|-----------------|
| `cis` | RU, BY, KZ, KG, AM, AZ, MD, TJ, TM, UZ, UA |
| `europe` | GB, DE, FR, IT, ES, AT, BE, NL, PL, GR, DK, SE, NO, FI, CZ, SK, HU, RO, BG, HR, SI, RS, BA, ME, MK, AL, LT, LV, EE, IS, IE, LU, MT, CY, CH |
| `asia` | CN, JP, KR, IN, ID, TH, VN, MY, PH, SG, PK, BD, LK, NP, AF, MM, KH, LA, MN, TW, HK, IL, SA, AE, TR, IR, IQ, SY, JO, LB, YE, OM, QA, KW, BH, KZ, UZ, KG, TJ, TM, AZ, GE, AM |
| `america` | US, CA, MX, BR, AR, CL, CO, PE, VE, EC, BO, PY, UY, CR, PA, GT, HN, SV, NI, CU, DO, HT, JM, TT, BS, BB, GY, SR |
| `africa` | EG, ZA, NG, KE, GH, DZ, MA, TN, ET, TZ, UG, ZW, AO, BW, CM, CI, SN, ML, BF, NE, TD, SD, SO, MG, MZ, MW, ZM, NA, RW, BJ, TG |
| `oceania` | AU, NZ, FJ, PG, WS, TO, VU, NC, PF, GU |

**Логика:** при выборе региона в UI автоматически заполняется параметр `country`. В URL передаются ОБА параметра для восстановления состояния.

### 4.6. Вспомогательные эндпоинты

| Метод | URL | Назначение |
|-------|-----|-----------|
| `GET` | `/api/search/suggest?q=...&limit=10` | Автодополнение |
| `GET` | `/api/search/facets` | Доступные значения фильтров для UI |
| `GET` | `/api/search/regions` | Маппинг регионов на страны |
| `GET` | `/api/search/filters` | Полный каталог фильтров |
| `GET` | `/api/search/recent` | Недавние добавления |
| `GET` | `/api/search/discover?source={s}&type={t}` | Обнаружение контента |

### 4.7. Полный каталог фильтров

```http
GET /api/search/filters
```

**Ответ:**
```json
{
  "kinds": {
    "movie": 756961,
    "short": 610712,
    "series": 184275,
    "cartoon": 84131,
    "tv_show": 35756,
    "anime": 9050,
    "mini_series": 989
  },
  "tags": [
    { "tag": "чёрно-белый", "kind": "subgenre" },
    { "tag": "Напряжённый", "kind": "adjective" },
    { "tag": "Дружба", "kind": "keyword" }
  ],
  "platforms": [
    { "slug": "netflix", "name": "Netflix" },
    { "slug": "hbo_max", "name": "HBO" }
  ],
  "companies": [
    "Warner Bros.",
    "Universal Pictures"
  ]
}
```

**Категории тегов:**
- `subgenre` — поджанры и эпохи (чёрно-белый, 1940-е, исекай, киберпанк)
- `adjective` — настроение и атмосфера (Напряжённый, Мрачный, Эпический)
- `keyword` — тематики и тропы (Дружба, Месть, Путешествие во времени)

**Платформы:** более 400 стриминговых сервисов и ТВ-каналов.
**Компании:** более 400 студий-производителей.

### 4.8. Обнаружение контента

```http
GET /api/search/discover?source=catalog&type=movie,series,mini_series,cartoon,anime,tv_show,short
```

**Параметры (как в URL `reyoho.com/search`):**
- `source`: обычно `catalog` (подборки задаются `preset` / `collection`, а не сменой source)
- `type`: CSV из kinds (`movie`, `series`, `mini_series`, `cartoon`, `anime`, `tv_show`, `short`)
- диапазоны: `{key}_from` / `{key}_to` (`rating_kp`, `runtime`, `votes_imdb`, `budget`, …)
- люди: `actor` + `actor_kp`, `crew` + `crew_kp` (+ `crew_role`), `not_person` + `not_person_kp`
- `age`: `6` \| `12` \| `16` \| `18`
- `preset`: `top`, `hidden_gems`, `trending`, `new`, `classics`
- `collection`: `films-best500`, `series-best250`, `all-popular1000`, `all-week250`, `all-day250`
- `sort`: `-popularity`, `-rating_kp`, …, `-box_office`, `-oscars_won`, `-awards_won`, …
- `released`: только если не дефолт — `upcoming` или `any`
- `region`: `cis`, `europe`, `asia`, `america`

### 4.9. Ответ поиска

```json
{
  "total": 1677921,
  "page": 1,
  "limit": 20,
  "results": [
    {
      "id_kp": 326,
      "media_type": "movie",
      "title": { "ru": "Побег из Шоушенка", "original": "The Shawshank Redemption" },
      "year": 1994,
      "poster": "/api/img/t/p/w300/...",
      "ratings": {
        "kinopoisk": { "score": 9.1, "votes": 1000000 },
        "imdb": { "score": 9.3, "votes": 2500000 }
      }
    }
  ]
}
```

---

## 5. Тренды и популярное

### 5.1. Эндпоинт `/api/trending`

Возвращает список трендовых и популярных тайтлов для **общей** главной (`/`, `/film`, `/series`, `/animation`).

| Фильтр | Назначение |
|--------|-----------|
| `all` / *(без параметра)* | Общий тренд |
| `movie` | Трендовые фильмы |
| `popular_movie` | Популярные фильмы |
| `tv` | Трендовые сериалы |
| `popular_tv` | Популярные сериалы |
| `animation` | Трендовая анимация |
| `popular_animation` | Популярная анимация |
| `anime` | Трендовое аниме (legacy; страница `/anime` использует §5.2) |
| `popular_anime` | Популярное аниме (legacy) |

```http
GET /api/trending?filter=popular_movie
```

**Ответ:**
```json
{
  "items": [
    {
      "id": 1492198,
      "title": "Искусственный интеллект",
      "originalTitle": "Artificial",
      "posterUrl": "/api/img/t/p/w500/...",
      "backdropUrl": null,
      "year": "2026",
      "mediaType": "movie",
      "kinopoiskFilmId": 8431006
    }
  ],
  "ratings": {
    "movie:1423191": {
      "kp": 7.3,
      "imdb": 7.6,
      "kinopoiskFilmId": 7378605
    }
  },
  "builtAt": 1793735433514
}
```

**Ключ рейтинга:** `{mediaType}:{tmdbId}` — для оптимизации размера ответа.

### 5.2. Страница аниме `/api/anime`

Отдельный feed для [reyoho.com/anime](https://reyoho.com/anime) (не общий `/api/trending`).

```http
GET /api/anime
GET /api/anime/list?sort=trending|season|popular&page=2
```

**Ответ `/api/anime`:**
```json
{
  "trending": { "items": [ /* AnimeItem */ ], "nextPage": 2 },
  "season": { "items": [ /* … */ ], "nextPage": 2 },
  "popular": { "items": [ /* … */ ], "nextPage": 2 },
  "top": [ /* до ~100 */ ],
  "seasonTop": [ /* … */ ],
  "seasonLabel": { "season": "FALL", "year": 2026 }
}
```

**UI-блоки сайта:**
1. Сейчас популярно ← `trending` (`sort=trending`)
2. Популярно в этом сезоне · {зима\|весна\|лето\|осень} {year} ← `season`
3. Популярное за всё время ← `popular`
4. Сайдбар «Топ 100 аниме» / «Топ сезона» ← `top` / `seasonTop`

**AnimeItem (фрагмент):** `anilistId`, `idMal`, `title`/`romaji`/`russian`/`ruTitle`, `year`, `score`, `coverImage`/`ruPosterUrl`, `mediaType`, `tmdbId`, `kpId`, `kpRating`, `imdbRating`.

`seasonLabel.season`: `WINTER` \| `SPRING` \| `SUMMER` \| `FALL`.

---

## 6. Трейлеры

```http
GET /api/latest-trailers?start=0&limit=6
```

**Ответ:**
```json
{
  "items": [
    {
      "mediaType": "movie",
      "tmdbId": 1101412,
      "title": "Вышка 2",
      "releaseYear": 2026,
      "backdropUrl": "/api/img/t/p/w780/...",
      "youtubeKey": "v5wPLSdL9MQ",
      "trailerTitle": "Вышка 2 — Русский трейлер (Дубляж, 2026)",
      "publishedAt": "2026-08-21T12:39:50.000Z"
    }
  ],
  "nextStart": 6,
  "hasMore": true,
  "feedVersion": 3549
}
```

**Формирование YouTube-ссылок:**
- Watch: `https://www.youtube.com/watch?v={youtubeKey}`
- Embed: `https://www.youtube-nocookie.com/embed/{youtubeKey}`

---

## 7. Рандомайзер

```http
POST /api/getlucky
Content-Type: application/json

{
  "type": "all",
  "known_only": true,
  "exclude_watched": false,
  "filters": {
    "genres": ["drama", "thriller"],
    "countries": ["US"],
    "year_from": 2020,
    "year_to": 2026
  }
}
```

**Параметр `known_only`:** при значении `true` в выборку попадают только тайтлы с 1000+ оценок на Кинопоиске.

**Ответ:**
```json
{
  "film": { /* Film or Series */ },
  "roulette_items": [ /* 32 тайтла для визуализации ленты */ ],
  "sound_enabled": true
}
```

---

## 8. Профили пользователей

### 8.1. Модель профиля

```json
{
  "user": {
    "id": "AkhwTfq0hCpMEfbvzKzjIGO6cYJMLzlM",
    "username": "spywer",
    "user_code": "#90360",
    "role": "user",
    "avatar": "/api/profile/avatar/...",
    "registered_at": "2026-09-23",
    "profile_views": 31,
    "is_private": false,
    "followers_count": 0,
    "following_count": 0
  },
  "stats": {
    "yoho_coins": 0,
    "timings_count": 0,
    "favorites_count": 0,
    "ratings_count": 0,
    "votes_count": 0
  },
  "wishlist": {
    "items": [
      { "id": 7378359, "type": "movie", "title": "Каратель", "year": 2026 }
    ],
    "filter": "<base64-encoded JSON>"
  },
  "favorite_films": { "slots": 4, "filled": 0, "items": [] },
  "tag_cloud": [
    { "name": "2020-е", "category": "years", "count": 26, "weight": 64 }
  ],
  "badges": {
    "ideas": 45,
    "dubs": 0,
    "reviews": 150,
    "timings": 30,
    "marathon_completed": 3
  }
}
```

### 8.2. Эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/profile/me` | Профиль текущего пользователя |
| `PATCH` | `/api/profile/me` | Обновление профиля |
| `GET` | `/api/profile/{username}` | Публичный профиль |
| `GET` | `/api/profile/{username}/films` | Оценки пользователя |
| `GET` | `/api/profile/{username}/diary` | Дневник пользователя |
| `GET` | `/api/profile/{username}/activity` | Лента активности |
| `GET` | `/api/profile/{username}/friends` | Друзья и подписки |

---

## 9. Дневник просмотров

Статус на сайте передаётся **русской строкой** `statusLabel` (не enum `want_to_watch`).

| statusLabel | UI |
|-------------|-----|
| `Просмотрено` | watched |
| `Смотрю` | watching |
| `Хочу посмотреть` | want |
| `Пересматриваю` | rewatching |
| `Отложено` | on hold |
| `Бросил` | dropped |

### 9.1. Добавить / обновить статус

```http
POST /api/diary
Content-Type: application/json
Cookie: __Secure-better-auth.session_token=…

{
  "itemId": "kp:5512084",
  "href": "/series/5512084",
  "title": "Гангстерленд",
  "year": "2025",
  "mediaType": "tv",
  "sourceReaction": "manual",
  "statusLabel": "Хочу посмотреть",
  "updateStatusIfItemExists": true
}
```

- `href`: `/series/{kp}` для `tv`/`anime`, `/film/{kp}` для `movie`
- **201** → `{ "entry": { "id", "itemId", "statusLabel", "mediaType", … } }`

### 9.2. Проверка «в дневнике»

```http
GET /api/diary/contains?itemId=kp:5512084
```

**200:**
```json
{ "inDiary": true, "statusLabel": "Хочу посмотреть" }
```

### 9.3. Удаление

```http
DELETE /api/diary?itemId=kp:6553387
```

**200:**
```json
{ "ok": true, "removed": true }
```

### 9.4. Сводка эндпоинтов

| Метод | URL | Описание |
|-------|-----|----------|
| `POST` | `/api/diary` | Добавить / сменить статус |
| `GET` | `/api/diary/contains?itemId=` | Есть ли в дневнике |
| `DELETE` | `/api/diary?itemId=` | Убрать из дневника |
| `GET` | `/api/profile/{username}/diary` | Публичный дневник профиля |

---

## 10. Избранное, sync, история

### 10.1. Избранное (activity)

Сайт не использует отдельный `/api/favorites` для кнопки «В избранное» на карточке. Актуально:

```http
POST /api/me/activity
Content-Type: application/json

{
  "op": "addFavorite",
  "item": {
    "id": "kp:5512084",
    "href": "/series/5512084",
    "title": "Гангстерленд",
    "year": "2025",
    "posterUrl": "/api/img/t/p/w185/….jpg",
    "mediaType": "tv",
    "ts": 1793816996319
  }
}
```

- `op`: `addFavorite` | `removeFavorite`
- **204** No Content

Постер в activity — относительный path, предпочтительно `w185`.

### 10.2. Sync (полки)

```http
GET /api/sync
```

**200:**
```json
{
  "ok": true,
  "favorites": [ /* FavoriteItem */ ],
  "watchHistory": [ /* FavoriteItem */ ]
}
```

Элемент полки (поля опциональны):

```json
{
  "id": "kp:5512084",
  "content_id": 5512084,
  "content_type": "tv",
  "href": "/series/5512084",
  "title": "Гангстерленд",
  "year": "2025",
  "posterUrl": "/api/img/…",
  "mediaType": "tv",
  "ts": 1793816996319
}
```

### 10.3. Вишлист / legacy history

| Метод | URL | Примечание |
|-------|-----|------------|
| `GET/POST` | `/api/wishlist` | UI сайта (фильтр `f` base64) |
| `POST` | `/api/history` | Legacy прогресс; в Android — через `watchHistory` в `/api/sync` |

---

## 11. Комментарии и реакции

### 11.1. Модель комментария

```json
{
  "id": "cmv5iw9q707lqnyw68fzaszhi",
  "user": { "username": "ktotakouu", "avatar": "/api/profile/avatar/..." },
  "text": "Отличный сериал!",
  "created_at": "2026-10-12T20:25:00Z",
  "edited": false,
  "has_spoilers": false,
  "reactions": {
    "laugh": 1,
    "poop": 0,
    "clown": 0,
    "fire": 1,
    "heart": 1,
    "total": 3
  },
  "replies": []
}
```

### 11.2. Реакции

| Код | Эмодзи | Unicode |
|-----|--------|---------|
| `laugh` | 😂 | U+1F602 |
| `poop` | 💩 | U+1F4A9 |
| `clown` | 🤡 | U+1F921 |
| `fire` | 🔥 | U+1F525 |
| `heart` | ❤️ | U+2764 |

### 11.3. Эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/{type}/{id}/comments` | Список комментариев |
| `POST` | `/api/{type}/{id}/comments` | Добавить комментарий |
| `POST` | `/api/comments/{id}/react` | Поставить/снять реакцию |

**Форматирование текста:** `bold`, `italic`, `strike`, `list`, `quote`, `spoiler`.

---

## 12. Оценки

```http
POST /api/{type}/{id}/rate
Content-Type: application/json

{
  "rating": 8.5,
  "review": "Отличный фильм, рекомендую!",
  "spoiler": false
}

DELETE /api/{type}/{id}/rate
```

### 12.1. Все оценки пользователя

```http
GET /api/ratings
```

Возвращает список всех оценок, поставленных текущим авторизованным пользователем. Используется для синхронизации между устройствами и экспорта данных.

**Ответ:**
```json
{
  "ratings": [
    {
      "content_id": 111543,
      "content_type": "movie",
      "rating": 8.5,
      "review": "Отличный фильм",
      "spoiler": false,
      "created_at": "2026-11-03T15:30:00Z"
    }
  ]
}
```

---

## 13. Марафоны

### 13.1. Модель марафона

```json
{
  "id": "doomsday",
  "title": "Марафон Doomsday",
  "description": "69 фильмов и сериалов Marvel к премьере",
  "deadline": "2026-12-16T00:00:00Z",
  "days_left": 42,
  "countdown": { "days": 42, "hours": 7, "minutes": 43, "seconds": 2 },
  "totals": { "items": 69, "films": 55, "series": 14 },
  "items": [
    {
      "position": 54,
      "content_id": 123,
      "content_type": "movie",
      "title": "Стражи Галактики 3",
      "completed": false,
      "completed_at": null
    }
  ]
}
```

### 13.2. Эндпоинты

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/marathon` | Текущий марафон |
| `GET` | `/api/marathon/progress` | Прогресс пользователя |
| `POST` | `/api/marathon/items/{position}/complete` | Отметить позицию |

---

## 14. Календарь выхода серий

```http
GET /api/calendar?month=2026-11-01T00:00:00Z
```

**Ответ:**
```json
[
  {
    "series_id": 5512084,
    "title": "Гангстерленд",
    "season": 2,
    "episode": 8,
    "air_date": "2026-11-10",
    "poster": "/api/img/..."
  }
]
```

---

## 15. Коллекции и подборки

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/collections/counters` | Счётчики для навигации |
| `GET` | `/api/collections/movies?type={type}&value={value}` | Коллекции фильмов |
| `GET` | `/api/collections/tv?type={type}&value={value}` | Коллекции сериалов |
| `GET` | `/api/collections/franchises` | Список франшиз |
| `GET` | `/api/collections/marvel` | Marvel по хронологии |
| `GET` | `/api/collections/anime-top` | Топ-100 аниме |

Значения `type`: `best`, `genre`, `country`, `year`.

**Пример счётчиков:**
```json
{
  "movies": {
    "best": 14823,
    "genres": {
      "comedy": 48842, "drama": 79748, "thriller": 23525,
      "action": 19236, "horror": 13260, "fantasy": 8812
    },
    "countries": {
      "russia": 7389, "ussr": 5319, "usa": 67816
    }
  }
}
```

---

## 16. Видеоплееры

### 16.1. Запрос источников

```http
GET /api/player/{type}/{id}
```

### 16.2. Модель ответа

```json
{
  "players": [
    {
      "name": "Alloha",
      "quality": "1080p",
      "active": true,
      "share": 48,
      "dub": "Дубляж"
    },
    {
      "name": "Stravers",
      "quality": "1080p",
      "active": false,
      "share": 10,
      "dub": null,
      "iframe_url": "https://really-as.stravers.live/?token_movie=83a9126c6994b1d6414b0f387f3cdf&token=8066edf1fb5b2a42da70fe7cbb758c",
      "tokens": {
        "token_movie": "83a9126c6994b1d6414b0f387f3cdf",
        "token": "8066edf1fb5b2a42da70fe7cbb758c",
        "expires_at": "2026-12-03T20:53:14Z"
      },
      "uses_storage_access": true,
      "provider_number": 11
    }
  ],
  "quality_tags": [
    "camrip", "good_quality", "bad_voice", "dub",
    "ad_inserts", "ad_overlay", "no_ads", "need_timings"
  ]
}
```

### 16.3. Модель токенов Stravers

| Токен | Описание | Поведение |
|-------|----------|-----------|
| `token_movie` | MD5-хэш идентификатора тайтла | Постоянен для конкретного тайтла |
| `token` | Сессионный токен пользователя (MD5 от `yoho_session`) | Одинаков для всех тайтлов в сессии, обновляется при logout/login |

### 16.4. Поддерживаемые провайдеры

| Провайдер | Домены |
|-----------|--------|
| Alloha | `*.alloha.tv` |
| Kodik | `*.kodik.info`, `*.kodik.biz`, `*.kodik.cc`, `*.kodik.online`, `kodikplayer.com` |
| Collaps | `*.collaps.org`, `*.collaps.cc`, `*.collaps.ink` |
| Vibix | `vibix.org` |
| VideoSeed | `*.videoseed.tv` |
| Kinescope | `*.kinescope.io`, `*.kinescopecdn.net` |
| Stravers | `*.stravers.live` (требует токены) |
| Kinoserial | `tv-1-kinoserial.net` ... `tv-5-kinoserial.net` |
| Ortified | `*.ortified.ws` |
| NextEmbed | `api.nextembed.ws` |
| Turbo | — |
| CVH | — |

### 16.5. Конфигурация плеера

```http
GET /api/player-config?kinopoiskId=12686425&mediaType=movie
```

**Успешный ответ (200):**
```json
{
  "available": true,
  "primaryPlayer": "Alloha",
  "qualities": ["1080p", "720p", "480p"],
  "dubs": ["Дубляж", "Многоголосый"],
  "iframe_url": "https://..."
}
```

**Ответ для заблокированного / недоступного конфига (451 Unavailable For Legal Reasons):**
```json
{ "error": "taken_down" }
```

HTTP 451: DMCA / нет источников; **без cookie сессии** тот же код часто означает login-wall (SSR: `takenDownReason=login`).  
В Android-клиенте: **с сессией** пустой/`451` config → UI «Нет доступных источников…»; **без сессии** — сначала HTML SSR, иначе текст про вход. `player-choice` на эту заглушку не влияет.

### 16.6. Статистика выбора плееров

```http
GET /api/player-choice?itemId=kp:111543
```

**Формат `itemId`:** `{source}:{id}` где `source` = `kp`, `tmdb`, `imdb`.

**Ответ:**
```json
{
  "total": 46,
  "items": [
    { "source": "turbo",   "count": 21, "share": 0.456 },
    { "source": "collaps", "count": 2,  "share": 0.043 },
    { "source": "alloha",  "count": 23, "share": 0.5 }
  ]
}
```

**Назначение:** автоматический выбор самого популярного плеера для конкретного тайтла.

### 16.7. Параметры iframe

```html
<iframe
  src="{iframe_url}"
  sandbox="allow-scripts allow-same-origin allow-presentation allow-popups"
  allow="autoplay; fullscreen; picture-in-picture; storage-access"
  referrerpolicy="strict-origin-when-cross-origin"
  loading="lazy"
/>
```

Cookie для кросс-доменных iframe: `SameSite=None; Secure; Partitioned=false`

---

## 17. Изображения и медиа

### 17.1. Прокси TMDB

```http
GET /api/img/t/p/w{size}/{path}
```

**Размеры:** `w45`, `w185`, `w300`, `w500`, `w780`, `w1280`

### 17.2. Прокси Кинопоиска

```http
GET /api/img/kp-mds/get-kinopoisk-image/{id}/{hash}/{size}
```

**Стандартный размер:** `600x900`

### 17.3. Маленькие постеры Кинопоиска

```http
GET /api/img/kp/posters/kp_small/{id}.jpg
```

### 17.4. Аватары пользователей

```http
GET /api/profile/avatar/{id}?v={timestamp}
```

### 17.5. Динамические OG-изображения

```http
GET /api/og/title?kind={kind}&id={id}&v={version}
```

Где `kind`: `film`, `series`, `profile`.

### 17.6. Эмодзи для реакций

```http
GET /emoji/{code}.svg
```

**Коды:** `1f602` (laugh), `1f4a9` (poop), `1f921` (clown), `1f525` (fire), `2764_fe0f` (heart)

---

## 18. Бейджи и достижения

```http
GET /api/badges/public        # Публичные счётчики
GET /api/badges/{username}    # Бейджи пользователя
```

**Модель:**
```json
{
  "ideas": 45,
  "dubs": 0,
  "reviews": 150,
  "timings": 30,
  "marathon_completed": 3,
  "ratings": 50
}
```

---

## 19. Идеи сообщества

```http
GET  /api/ideas              # Список идей
POST /api/ideas              # Создание идеи
```

**Модель:**
```json
{
  "id": 1,
  "title": "Добавить темную тему",
  "description": "...",
  "votes": 10,
  "status": "open",
  "author": { "id": "...", "username": "user1", "avatar": "..." },
  "created_at": "2026-10-01T15:30:00Z"
}
```

**Статусы:** `open`, `in_progress`, `implemented`, `rejected`.

---

## 20. Статистика просмотров

### 20.1. Глобальная статистика по плеерам

```http
GET /api/seen-film
```

**Ответ:**
```json
{
  "total": 303,
  "items": [
    { "source": "turbo",     "count": 139, "share": 0.458 },
    { "source": "alloha",    "count": 122, "share": 0.402 },
    { "source": "cvh",       "count": 15,  "share": 0.049 },
    { "source": "collaps",   "count": 23,  "share": 0.075 },
    { "source": "vibix",     "count": 3,   "share": 0.009 },
    { "source": "videoseed", "count": 1,   "share": 0.003 }
  ]
}
```

**Назначение:** глобальная статистика использования плееров по всему сайту.

---

## 21. Сервисные эндпоинты (Warmup & Metadata)

### 21.1. Прогрев RSC-кеша карточек

```http
POST /api/warm/cards
Content-Type: application/json

{
  "paths": [
    "/film/628328",
    "/film/8738497",
    "/series/7791628",
    "/movie/1257147"
  ]
}
```

**Ответ (200):**
```json
{
  "warmed": 7,
  "cached": 3,
  "skipped": 0,
  "deferred": []
}
```

Используется клиентом для предварительного прогрева кеша при наведении на карточки.

### 21.2. Теги качества для бейджей

```http
POST /api/player-tags/posters
Content-Type: application/json

{
  "itemIds": ["kp:12686425", "kp:6548088", "kp:4789626"]
}
```

**Ответ (200):**
```json
{
  "tags": {
    "kp:5582129": ["camrip", "ads"],
    "kp:6548088": ["dub"],
    "kp:13135908": ["ads", "bad_voice"],
    "kp:77164": ["need_timings"]
  }
}
```

**Доступные теги:** `camrip`, `good_quality`, `bad_voice`, `dub`, `ad_inserts`, `ad_overlay`, `no_ads`, `need_timings`, `ads`.

**Формат `itemIds`:** `{source}:{id}` где `source` = `kp`, `tmdb`, `imdb`.

### 21.3. Батч последних серий сериалов

```http
POST /api/tv-last-air/batch
Content-Type: application/json

{
  "tmdbIds": [30984, 84773, 456]
}
```

**Ответ (200):**
```json
{
  "items": {
    "456": {
      "tmdbId": 456,
      "fp": "38|5|2026-11-01",
      "badge": "38 СЕЗОН 5 СЕРИЯ",
      "posterUrl": "/api/img/t/p/w500/...",
      "meta": {
        "tmdbId": 456,
        "seasonNumber": 38,
        "episodeNumber": 5,
        "airDate": "2026-11-01",
        "fingerprint": "38|5|2026-11-01",
        "showStatus": "Returning Series",
        "inProduction": true,
        "nextSeasonNumber": 38,
        "nextEpisodeNumber": 6,
        "nextAirDate": "2026-11-08",
        "airAt": 1793581200000,
        "airPrecision": "exact",
        "nextAirAt": 1794187800000,
        "nextAirPrecision": "exact"
      }
    }
  }
}
```

**Статусы шоу:** `Returning Series`, `Ended`, `In Production`, `Canceled`.
**Точность даты:** `exact` (точная), `estimated` (приблизительная).

---

## 22. Коды ошибок

| Код HTTP | Код ошибки | Описание |
|----------|-----------|----------|
| 400 | `VALIDATION_ERROR` | Некорректные параметры |
| 400 | `BAD_REQUEST` | Неверный формат запроса |
| 401 | `UNAUTHORIZED` | Требуется авторизация |
| 401 | `INVALID_EMAIL_OR_PASSWORD` | Неверные учётные данные |
| 403 | `FORBIDDEN` | Доступ запрещён |
| 404 | `NOT_FOUND` | Ресурс не найден |
| 409 | `CONFLICT` | Ресурс уже существует |
| 429 | `RATE_LIMITED` | Превышен лимит запросов |
| 451 | `TAKEN_DOWN` | Недоступен по юридическим причинам (DMCA) |
| 500 | `INTERNAL_ERROR` | Внутренняя ошибка сервера |

**Формат ответа об ошибке:**
```json
{
  "message": "Описание ошибки",
  "code": "ERROR_CODE"
}
```

---

## 23. Карта маршрутов

### 23.1. Публичные страницы

| URL | Назначение | Кеш |
|-----|-----------|-----|
| `/` | Главная | edge HIT |
| `/search` | Расширенный поиск (SPA-shell) | BYPASS |
| `/getlucky` | Рандомайзер | edge HIT |
| `/marathon` | Марафон | edge HIT |
| `/marvel` | Marvel по хронологии | edge HIT |
| `/franchises` | Франшизы | edge HIT |
| `/calendar` | Календарь выхода серий | edge HIT |
| `/anime/top` | Топ-100 аниме | edge HIT |
| `/extension` | Расширение YoHo Stream Mode | edge HIT |
| `/ideas` | Идеи сообщества | edge HIT |
| `/thanks` | Благодарности | edge HIT |
| `/terms`, `/privacy`, `/dmca` | Юридические страницы | edge HIT |

### 23.2. Динамические страницы

| URL | Назначение | Кеш |
|-----|-----------|-----|
| `/film/{id}` | Страница фильма | BYPASS + RSC streaming |
| `/series/{id}` | Страница сериала | BYPASS + RSC streaming |
| `/name/{id}` | Страница персоны | BYPASS |
| `/{username}` | Профиль пользователя | BYPASS |
| `/{username}/{activity\|films\|diary\|friends}` | Разделы профиля | BYPASS |

### 23.3. Страницы каталога

| URL | Назначение |
|-----|-----------|
| `/movie` | Все фильмы |
| `/tv` | Все сериалы |
| `/anime` | Аниме (отдельные блоки, см. §5.2 `/api/anime`) |
| `/anime/top` | Топ-100 аниме |
| `/film`, `/series`, `/animation` | Разделы главной (общий layout + `/api/trending`) |
| `/movie/best`, `/tv/best` | Лучшие тайтлы |
| `/movie/genre/{slug}`, `/tv/genre/{slug}` | По жанру |
| `/movie/country/{slug}`, `/tv/country/{slug}` | По стране |
| `/movie/year/{year}`, `/tv/year/{year}` | По году |

### 23.4. API-эндпоинты

| Категория | Эндпоинты |
|-----------|-----------|
| **Аутентификация** | `/api/auth/sign-in/email`, `/api/auth/sign-up/email`, `/api/auth/sign-in/{provider}`, `/api/auth/sign-out`, `/api/auth/get-session`, `/api/auth/forgot-password`, `/api/auth/reset-password`, `/api/auth/verify-email` |
| **Профиль** | `/api/profile/me`, `/api/profile/{username}`, `/api/profile/{username}/{films\|diary\|activity\|friends}` |
| **Контент** | `/api/film-info`, `/api/person/{id}`, `/api/films`, `/api/films/{id}`, `/api/film/watchers`, `/api/series`, `/api/series/{id}/…` |
| **Сериалы** | `/api/tv-last-air`, `/api/tv-schedule`, `/api/tv-last-air/batch` |
| **Поиск** | `/api/search/suggest`, `/api/search/facets`, `/api/search/regions`, `/api/search/filters`, `/api/search/recent`, `/api/search/discover` |
| **Тренды** | `/api/trending`, `/api/anime`, `/api/anime/list`, `/api/latest-trailers`, `/api/recommendations/latest`, `/api/getlucky` |
| **Коллекции** | `/api/collections/{counters\|movies\|tv\|franchises\|marvel\|anime-top}` |
| **Социальные** | `/api/{type}/{id}/comments`, `/api/comments/{id}/react`, `/api/{type}/{id}/rate`, `/api/ratings` |
| **Дневник** | `/api/diary`, `/api/diary/contains` |
| **Полки** | `/api/me/activity`, `/api/sync`, `/api/wishlist` |
| **Марафон** | `/api/marathon`, `/api/marathon/progress`, `/api/marathon/items/{position}/complete` |
| **Календарь** | `/api/calendar` |
| **Плееры** | `/api/player/{type}/{id}`, `/api/player-config`, `/api/player-choice` |
| **Изображения** | `/api/img/t/p/w{size}/{path}`, `/api/img/kp-mds/get-kinopoisk-image/{id}/{hash}/{size}`, `/api/img/kp/posters/kp_small/{id}.jpg`, `/api/profile/avatar/{id}`, `/api/og/title`, `/emoji/{code}.svg` |
| **Бейджи** | `/api/badges/public`, `/api/badges/{username}` |
| **Идеи** | `/api/ideas` |
| **Статистика** | `/api/seen-film` |
| **Warmup** | `/api/warm/cards`, `/api/player-tags/posters` |

---

## 24. Интеграции

### 24.1. Источники метаданных

| Сервис | Домен |
|--------|-------|
| TMDB | `image.tmdb.org` |
| Кинопоиск | `st.kp.yandex.net` |
| Kinopoisk API | `kinopoiskapiunofficial.tech` |

### 24.2. CDN и медиа

| Сервис | Домен |
|--------|-------|
| Media CDN | `media.reyoho.ru`, `media.reyoho.com` |
| SuperDuperCDN | `*.superdupercdn.com`, `*.z.superdupercdn.com` |
| GraphicsLab | `graphicslab.io` |
| VideoFrame | `*.videoframe2.com` |
| CDNVideoHub | `*.cdnvideohub.com` |

### 24.3. Аналитика и сторонние сервисы

| Сервис | Назначение |
|--------|-----------|
| Яндекс.Метрика | Аналитика (с WebSocket) |
| Яндекс.Реклама | Рекламная сеть |
| Google AdSense | Рекламная сеть |
| Google reCAPTCHA | Защита от ботов |
| Google Fonts | Веб-шрифты |

### 24.4. Социальные каналы и поддержка

| Канал | URL |
|-------|-----|
| Telegram-канал | `https://t.me/yohoru` |
| Telegram-группа | `https://t.me/yohopw` |
| Discord | `https://discord.gg/VeghBZykCJ` |
| Boosty | `https://boosty.to/yohoru` |
| CloudTips | `https://pay.cloudtips.ru/p/ccd12038` |
| Email | `reyoho@proton.me` |

### 24.5. Сокращатели ссылок (для шеринга таймингов)

- `shortener2.link`, `*.shortener2.link`
- `kurolesya.link`, `*.kurolesya.link`
- `gorodyshka.link`, `*.gorodyshka.link`

---

## 25. Клиентское состояние (localStorage)

| Ключ | Назначение |
|------|-----------|
| `yoho:wh:v1` | История просмотра |
| `yoho:fav:v1` | Избранное |
| `yoho:auth-slot-w:v1` | Ширина слота аватара в шапке |
| `yoho:shelves-tab:v1` | Активная вкладка полок |
| `yoho:shelves-h:v2:{tab}` | Высота полки |
| `yoho:poster-col-w:v1` | Ширина колонки постеров |
| `yoho:tv-mode:v1` | Режим ТВ |
| `wf-ann-dismissed` | Скрытое объявление |
| `wf-streamer-dismissed` | Скрытый баннер стримера |
| `wf-ads-blocked` | Флаг блокировщика рекламы |

---

## 26. Горячие клавиши

| Комбинация | Действие |
|-----------|----------|
| `Alt + T` | Панель уведомлений |
| `Shift + Enter` | Отправка отчёта об ошибке |

---

## 27. Android-клиент (YoHo app)

Пути, которые реально дергает приложение (`app/.../YohoApi.kt`), база из `api.properties` (`API_BASE_URL`):

| Группа | Методы |
|--------|--------|
| Auth | `sign-in/email`, `sign-up/email`, `sign-out`, `get-session` |
| Home (общий) | `trending`, `latest-trailers`, `search/recent`, `recommendations/latest` |
| Home (аниме) | `anime`, `anime/list`, `recommendations/latest` |
| Search | `search/discover`, `facets`, `filters`, `regions`, `recent`, `suggest`, `persons` |
| Title | `film-info`, `person/{id}`, `tv-last-air`, `tv-schedule` |
| Player | `player-config`, `player-choice`, `player/{type}/{id}` |
| Shelves | `me/activity`, `sync` |
| Diary | `POST diary`, `GET diary/contains`, `DELETE diary?itemId=` |

Подробности Android-layout и маппинг фильтров — [ANDROID.md](./ANDROID.md).

---

*Документ актуален для версии 0.1.42. По вопросам использования API обращайтесь на reyoho@proton.me.*