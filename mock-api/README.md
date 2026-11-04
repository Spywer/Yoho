# YoHo Mock API (полный OpenAPI)

Временный локальный сервер по `docs/openapi.yaml`.

## Запуск

```bash
cd mock-api
npm start
```

- Local: `http://127.0.0.1:8787`
- Эмулятор Android: `http://10.0.2.2:8787`
- Порт: `PORT=9000 npm start`

Состояние (дневник, избранное, сессия) — **в памяти**, сбрасывается при рестарте.

## Auth

```http
POST /api/auth/sign-in/email
{ "email": "a@b.c", "password": "1234" }
```

После логина доступны diary / favorites / wishlist / history / comments / rate / profile/me.

## Покрытие

| Группа | Пути |
|--------|------|
| Auth | sign-in/email, sign-up, OAuth `{provider}`, sign-out, session, forgot/reset/verify |
| Profile | `/api/profile/me`, `/{username}`, films, diary, activity, friends, avatar |
| Films / Series | list, by id, seasons, timings |
| Search | search, suggest, facets, regions, getlucky |
| Collections | counters, movies, tv, franchises, marvel, anime-top |
| Social | comments, react, rate |
| Lists | diary, favorites, wishlist, history |
| Marathon / Calendar | marathon, progress, complete, calendar |
| Player | stub `Demo` (без чужих токенов стримов) |
| Images | TMDB redirect, KP/avatar placeholder gif |
| Badges / Ideas | public, by user, ideas CRUD |
| Trending / trailers | `/api/trending`, `/api/latest-trailers` |
| Search extra | `/api/search/filters`, `/recent`, `/discover` |
| TV meta | `/api/tv-schedule`, `/api/tv-last-air`, `/batch` |
| Social extras | `/api/film/watchers`, `/api/ratings`, `/api/player-choice`, `/api/seen-film` |
| Player config | `/api/player-config` (stub; 451 если id нет) |
| Warmup / OG | `/api/warm/cards`, `/api/player-tags/posters`, `/api/og/title` |
| Extra | `/health` |

Данные каталога: `data/catalog.json`.

Доки: `docs/API.md`, `docs/openapi.yaml` (v0.1.40, обновл. 03.11.2026 23:30).
