# YoHo (Android)

Клиент каталога [reyoho.com](https://reyoho.com): фильмы и сериалы, плеер, коллекция (избранное / дневник / история), поиск.

## Стек

- Kotlin, Jetpack Compose (Material 3)
- Retrofit + OkHttp (cookie-сессия Better Auth)
- Coil, WebView (встроенные плееры)
- minSdk 28, targetSdk 37

## Запуск

1. Android Studio / JDK 11+
2. База API — `SiteHttp` (зеркала `reyoho.com` / `reyoho.ru` / `yoho.pw`, failover)
3. Сборка:

```bash
./gradlew :app:assembleDebug
```

## Возможности

- Главная: тренды / рекомендации / трейлеры; чип **Аниме** — блоки как на `/anime` (`GET /api/anime`)
- Поиск на главной: `suggest` + `persons` (без перехода в каталог); лайк в избранное из выдачи
- Каталог: `discover` + фильтры (без dropdown suggest)
- Карточка тайтла: герой, трейлер, плеер (iframe/embed), расписание сериала; без источников при сессии — заглушка как на сайте
- Коллекция (вкладка): хаб → отдельные Activity «Избранное» / «Дневник» / «История»; сетка «Мои фильмы и сериалы» (дневник ∪ избранное)
- Избранное: `POST /api/me/activity` (`addFavorite` / `removeFavorite`), список из `GET /api/sync`
- Дневник: `GET/POST/DELETE /api/diary`, `contains`; `statusLabel` и `sourceReaction` (like/dislike) независимы
- Аккаунт: вход/регистрация (email), cookie `__Secure-better-auth.session_token` / `yoho_session`; стрелка «назад» на подстраницах — только планшет

## Структура

```
app/src/main/java/com/spywer/yoho/
  data/          # SessionRepository, remote API, cookies
  ui/
    main/        # нижняя навигация (MainShell)
    home/ search/ account/
    collection/  # хаб + FavoritesList/Diary/History Activity, CollectionScreen/VM
    detail/      # TitleDetail* (экран, кино-плеер, расписание, герой)
    trailer/     # TrailerPlayerActivity + YouTube/VK (общий UI с Home/Detail)
    components/  # PosterCard, TrailerCard
    theme/ layout/
```

## Заметки

- Без входа часть плееров и личные полки недоступны (как на сайте).
- Кино-плеер и трейлеры — разные поверхности (см. `docs/ANDROID.md` → Плеер и трейлеры).
- Иконка приложения: белые **YH** на фоне `#1A2026`.
- Документация: `docs/API.md` (0.1.42), `docs/openapi.yaml`, `docs/ANDROID.md`
