# ContentApp

Android-приложение (Kotlin + Jetpack Compose): пользователь вводит короткое
желание, получает контент (фото + промт), сгенерированный ИИ. Без регистрации
и оплаты — только бесплатная модель (v1).

**Начать здесь → [`docs/START-HERE.md`](docs/START-HERE.md)** — статус
проекта, открытые вопросы, карта файлов. Если контекст разговора с ассистентом
потерян — это первое, что нужно прочитать.

## Как открыть в Android Studio

1. Скачайте и распакуйте архив.
2. Android Studio → **File → Open** → выберите папку `ContentApp` (ту, где лежит `settings.gradle.kts`).
3. Дождитесь Gradle sync (первый раз скачает зависимости — нужен интернет).
4. Скопируйте `local.properties.example` в `local.properties` и впишите свой
   `GEMINI_API_KEY` (см. комментарии в файле).
5. Запустите на эмуляторе или устройстве (Shift+F10 / кнопка Run).

Требования: Android Studio Koala или новее, JDK 17.

## Собрать APK без Android Studio (GitHub Actions)

В проекте есть готовый workflow `.github/workflows/build-apk.yml` — собирает
debug APK на GitHub без локальной установки Android Studio/SDK.

1. Загрузить эту папку как репозиторий на GitHub (или добавить в существующий).
2. В репозитории: **Settings → Secrets and variables → Actions → New repository
   secret**, имя `GEMINI_API_KEY`, значение — свой ключ (иначе соберётся, но
   без рабочего вызова Gemini API).
3. Вкладка **Actions** → workflow "Build debug APK" → **Run workflow**
   (или просто запушить в ветку `main` — сработает автоматически).
4. После завершения сборки APK будет в артефактах запуска (**Actions** →
   конкретный run → **Artifacts** → `ContentApp-debug-apk`) — скачать и
   установить на устройство (нужно разрешить установку из неизвестных
   источников).

В проекте нет закоммиченного Gradle wrapper (`gradlew`) — workflow ставит
Gradle и Android SDK сам через официальные экшены, ничего скачивать вручную
не нужно.

## Документация

| Файл | Что внутри |
|---|---|
| [`docs/START-HERE.md`](docs/START-HERE.md) | Статус по модулям, открытые вопросы, карта файлов |
| [`docs/NEXT-STEPS.md`](docs/NEXT-STEPS.md) | Очередь задач по приоритету |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Журнал решений — что и почему выбрано |
| [`docs/SPEC.md`](docs/SPEC.md) | ID-нотация экранов/элементов/процедур/правил |
| [`docs/TESTING-CHECKLIST.md`](docs/TESTING-CHECKLIST.md) | Что и в каком порядке проверить при первом реальном запуске |
| [`docs/screens-and-procedures.pdf`](docs/screens-and-procedures.pdf) | Макеты экранов + описание процедур каждой кнопки |
| [`docs/ContentApp-Documentation.md`](docs/ContentApp-Documentation.md) | Подробная техническая документация |
| [`design/`](design/) | Вайрфреймы (.svg) и схемы (.mermaid) с этапа проектирования |

## Структура кода

```
app/src/main/java/com/example/contentapp/
├── MainActivity.kt              — точка входа
├── navigation/AppNavigation.kt  — состояние приложения, граф экранов (main -> result)
├── ai/                          — разворачивание промта (Gemini API, P1)
├── content/                     — генерация изображения (Gemini Flash, P4), сохранение в галерею
└── ui/
    ├── theme/                   — цвета, шрифты, тема
    └── screens/                 — AppNameBanner (S1.T0), MainScreen, ResultScreen, LoadingOverlay
```
