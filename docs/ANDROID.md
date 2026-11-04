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
| Главная | `trending`, `latest-trailers`, `recommendations/latest` |
| Каталог | `search/discover`, `facets`, `filters`, `regions`, `recent`, `suggest` |
| Карточка | `film-info`, `person/{id}`, `tv-last-air`, `tv-schedule` |
| Плеер | `player-config`, `player-choice`, `player/{type}/{id}` (+ HTML SSR fallback) |
| Избранное | `POST me/activity` (`addFavorite`/`removeFavorite`), список через `GET sync` |
| Дневник | `POST diary`, `GET diary/contains?itemId=`, `DELETE diary?itemId=` |

## Идентификаторы

- `itemId` / activity `id`: `kp:{kinopoiskId}`
- `href`: `/series/{kp}` для tv/anime, `/film/{kp}` для movie
- Дневник `statusLabel`: русские строки UI («Хочу посмотреть», …)
- `contains` ответ: `{ "inDiary": true, "statusLabel": "…" }`
