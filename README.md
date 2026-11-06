# YoHo (Android)

Клиент каталога [reyoho.com](https://reyoho.com): фильмы и сериалы, плеер, избранное, дневник, поиск.

## Стек

- Kotlin, Jetpack Compose (Material 3)
- Retrofit + OkHttp (cookie-сессия Better Auth)
- Coil, WebView (встроенные плееры)
- minSdk 28, targetSdk 37

## Запуск

1. Android Studio / JDK 11+
2. База API в `api.properties`:

```properties
API_BASE_URL=https://reyoho.com/
```

Для локального mock (эмулятор):

```properties
API_BASE_URL=http://10.0.2.2:8787/
```

3. Сборка:

```bash
./gradlew :app:assembleDebug
```

## Возможности

- Главная: тренды / рекомендации / трейлеры; чип **Аниме** — блоки как на `/anime` (`GET /api/anime`)
- Каталог / поиск с фильтрами (значения query как на сайте)
- Карточка тайтла: герой, трейлер, плеер (iframe/embed), расписание сериала
- Избранное: `POST /api/me/activity` (`addFavorite` / `removeFavorite`)
- Дневник: `POST/DELETE /api/diary`, статус через `GET /api/diary/contains`
- Аккаунт: вход/регистрация (email), cookie `__Secure-better-auth.session_token` / `yoho_session`

## Структура

```
app/src/main/java/com/spywer/yoho/
  data/          # SessionRepository, remote API, cookies
  ui/
    main/        # нижняя навигация
    home/ search/ favorites/ account/
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
- Локальный mock: `mock-api/` (см. `mock-api/README.md`)
