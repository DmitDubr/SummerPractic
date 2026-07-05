# Отчёт о реализации CMP-клиента «Вертикаль»

Дата: 5 июля 2026  
Связанные документы: [CMP_CLIENT_IMPLEMENTATION_PLAN.md](CMP_CLIENT_IMPLEMENTATION_PLAN.md), [BE_IMPLEMENTATION_PLAN.md](BE_IMPLEMENTATION_PLAN.md)

## Краткое резюме

Реализован MVP-каркас Kotlin Compose Multiplatform клиента скалодрома **«Вертикаль»** в каталоге `Climbing/client/`. Покрыты основные пользовательские сценарии: просмотр расписания, деталь слота, оформление записи, список «Мои записи» и отмена брони. Архитектура повторяет референс «Волны» (`razrabotka/summer-school-2026-feature-client/client/`): MVI + Koin + Ktor, без отдельного экрана OTP — сессия через `sessionToken` при первой записи.

**Статус сборки на момент отчёта:**

| Задача Gradle | Результат |
|---------------|-----------|
| `:shared:compileCommonMainKotlinMetadata` | ✅ SUCCESS |
| `:shared:compileKotlinWasmJs` | ✅ SUCCESS |
| `:shared:compileDebugKotlinAndroid` | ❌ SDK location not found (нет `local.properties` / `ANDROID_HOME`) |
| `:androidApp:assembleDebug` | ❌ не запускалась (требуется Android SDK) |
| `:shared:allTests` | ❌ не прошла (iOS/Android targets на Windows без SDK) |

---

## Структура проекта

```text
Climbing/client/
  settings.gradle.kts          # модули: shared, androidApp, webApp
  build.gradle.kts
  gradle/libs.versions.toml    # Kotlin 2.2.20, Compose 1.9.3, Ktor 3.1.3, Koin 4.1.0
  gradle/wrapper/
  shared/                      # KMP + CMP, package com.vertical.app
    src/commonMain/kotlin/...
    src/commonTest/kotlin/...
    src/androidMain/kotlin/...
    src/iosMain/kotlin/...
    src/wasmJsMain/kotlin/...
  androidApp/                  # MainActivity, AndroidManifest
  webApp/                      # wasmJs entry point (stub)
```

**Targets в `shared`:** `android`, `iosX64`, `iosArm64`, `iosSimulatorArm64`, `wasmJs`.  
**Не создан:** модуль `iosApp` (framework `VerticalShared` собирается из `shared`, но нативная оболочка iOS отсутствует).

---

## Архитектура

```text
presentation (Compose screens + MVI stores)
    ↓
data (Ktor repositories, DTO, mappers)
    ↓
domain (models, pure policies)
    ↓
platform (expect/actual: SessionStorage, AppConfig)
```

### Core (`com.vertical.app.core`)

| Компонент | Файл | Назначение |
|-----------|------|------------|
| `Loadable`, `ActionStatus`, `EmptyReason` | `core/ui/Loadable.kt` | Паттерн состояний LOGIC-008 |
| `MviStore` | `core/mvi/MviStore.kt` | Контракт Store / Intent / Effect |
| `AppFailure`, `ApiErrorDto` | `core/error/`, `core/network/` | Маппинг ошибок API → домен |
| `VerticalApiClient` | `core/network/VerticalApiClient.kt` | Ktor: JSON, Bearer, timeout, 401 → clear session |
| `SessionStorage` (expect) | `core/storage/` | Платформенное хранение JWT |
| `defaultApiBaseUrl()` | `core/config/` | Android: `10.0.2.2:8080/v1`, iOS/Wasm: `localhost:8080/v1` |

### Session

- `SessionRepository` / `DefaultSessionRepository` — read/write/clear `sessionToken`.
- Реализации storage: `PlatformSessionStorage` для Android (SharedPreferences), iOS (NSUserDefaults), Wasm (in-memory).

### Domain policies

| Политика | LOGIC | Реализация |
|----------|-------|------------|
| `SlotAvailabilityPolicy` | LOGIC-002 | CTA: Book / Waitlist / Disabled |
| `BookingPriceCalculator` | LOGIC-003 | Превью цены OWN/RENTAL |
| `CancellationPolicy` | LOGIC-004 | Ранняя/поздняя отмена (≥60 мин / <60 мин) |
| `BookingErrorPolicy` | SCR-007 | Typed UI по кодам API (`NO_SPOTS`, `ONE_BOOKING_PER_DAY`, …) |
| `BookingStatusLabels` | SCR-008/009 | Локализованные статусы брони |
| `SlotFilterPolicy` | LOGIC-005 | Badge счётчик активных фильтров + label периода |
| `SlotGroupingPolicy` | LOGIC-005 | Группировка «Сегодня» / «Завтра» / дата |
| `PhoneValidator` | LOGIC-001 | Нормализация `+7XXXXXXXXXX` |

### Data layer

Репозитории на Ktor (`data/Repositories.kt`):

- `KtorSlotRepository` — `GET /slots`, `GET /slots/{id}` (query: dateFrom, dateTo, instructorIds, timeOfDay, level).
- `KtorInstructorRepository` — `GET /instructors`.
- `KtorProfileRepository` — `GET/PATCH /profile`, сохранение `sessionToken` из ответа.
- `KtorBookingRepository` — `POST /bookings`, `GET /bookings`, `GET /bookings/{id}`, `POST .../cancel`.
- `KtorWaitlistRepository` — `POST /slots/{id}/waitlist`.

DTO и мапперы — `data/dto/Dtos.kt` (ручные `@Serializable` data class, синхронизированы с OpenAPI backend).

**Не реализованы:** `RatingRepository`, `PushTokenRepository`, офлайн-кэш.

### DI (Koin)

`di/AppModule.kt` — singletons для API, репозиториев, session; `viewModel` для всех stores.

---

## Реализованные экраны и stores

### Навигация (`VerticalApp.kt`, `AppNavigation.kt`)

- Нижняя панель: **«Расписание»** | **«Мои записи»** (2 вкладки, без «Профиль»).
- Stack: Schedule → SlotDetail → BookingForm / Waitlist; Bookings → BookingDetail.
- Navigation Compose с type-safe routes (`@Serializable` destinations).
- Bottom bar скрывается на детальных экранах.

### SCR-001 / SCR-003 — Расписание и фильтры

| Элемент | Статус |
|---------|--------|
| `ScheduleStore` + `ScheduleScreen` | ✅ |
| Группировка слотов по дням | ✅ |
| Bottom sheet фильтров (инструктор, время суток, уровень) | ✅ |
| Pull-to-refresh / Loading / Empty / Error | ✅ частично (refresh через intent, без swipe UI) |
| SCR-002 фильтр периода дат | ✅ bottom sheet: пресеты 7/14 дней, ввод dateFrom/dateTo |

### SCR-004 — Деталь слота

| Элемент | Статус |
|---------|--------|
| `SlotDetailStore` + `SlotDetailScreen` | ✅ |
| CTA по `SlotAvailabilityPolicy` | ✅ |
| Переход на форму записи | ✅ |
| Переход в лист ожидания | ✅ → SCR-012 |

### SCR-005 / SCR-007 / SCR-013 — Оформление записи

| Элемент | Статус |
|---------|--------|
| `BookingFormStore` + `BookingFormScreen` | ✅ |
| Контакты (имя, телефон), валидация LOGIC-001 | ✅ |
| Снаряжение OWN / RENTAL (скальники, страховка) | ✅ |
| Превью цены LOGIC-003 | ✅ |
| `createBooking`, сохранение `sessionToken` | ✅ |
| Подгрузка профиля при открытии | ✅ |
| SCR-006 экран успеха | ⚠️ inline success state, навигация на «Мои записи» |
| SCR-007 диалог ошибок API | ✅ `BookingErrorPolicy` + CTA «В лист ожидания» при `NO_SPOTS` |
| SCR-013 sheet «Изменить контакты» | ❌ |

### SCR-008 / SCR-009 / SCR-010 — Мои записи и отмена

| Элемент | Статус |
|---------|--------|
| `BookingListStore` + `BookingListScreen` | ✅ |
| Empty без сессии / без записей | ✅ |
| `BookingDetailStore` + `BookingDetailScreen` | ✅ |
| Диалог отмены с предупреждением LOGIC-004 | ✅ warning передаётся в `ShowCancel` |
| Статусы брони (WAITLIST и др.) | ✅ `BookingStatusLabels` |
| `cancelBooking` | ✅ |
| Deep link из push | ❌ |
| Офлайн-кэш | ❌ |

### SCR-012 — Лист ожидания

| Элемент | Статус |
|---------|--------|
| `WaitlistStore` + `WaitlistScreen` | ✅ |
| `KtorWaitlistRepository` | ✅ |
| Навигация с SCR-004 и из ошибки SCR-007 | ✅ |
| `deleteWaitlistEntry` / позиция из API | ❌ (только join) |

---

## Соответствие roadmap (CMP-00 … CMP-16)

| ID | Итерация | Статус | Комментарий |
|----|----------|--------|-------------|
| CMP-00 | Каркас KMP | ✅ | Gradle, targets, модули |
| CMP-01 | Core MVI/DI/errors | ✅ | Без отдельного reducer unit-теста |
| CMP-02 | Network + DTO | ✅ частично | 4 репозитория; нет MockEngine тестов |
| CMP-03 | Session + Profile | ✅ | expect/actual storage |
| CMP-04 | Theme skeleton | ⚠️ | Только `MaterialTheme`, без design tokens |
| CMP-05 | Навигация 2 вкладки | ✅ | Без bottom sheets для cancel/rating |
| CMP-06 | SCR-001 + фильтры | ✅ | SCR-002 + SCR-003 |
| CMP-07 | SCR-004 | ✅ | Waitlist CTA → SCR-012 |
| CMP-08 | SCR-005/006/007/013 | ⚠️ | Happy path + SCR-007; SCR-006 inline, SCR-013 нет |
| CMP-09 | SCR-008/009/010 | ✅ | Без deep link |
| CMP-10 | SCR-012 waitlist | ✅ | join; без leave/get entry |
| CMP-11 | SCR-011 рейтинг | ❌ | |
| CMP-12 | Push + registerPushToken | ❌ | |
| CMP-13 | Офлайн-кэш | ❌ | |
| CMP-14 | Unit/data тесты | ⚠️ | `DomainPolicyTest` (3 теста) |
| CMP-15 | Smoke с BE | ❌ | Не проводился на устройстве |
| CMP-16 | Figma / polish | ❌ | |

---

## Тесты

`shared/src/commonTest/.../DomainPolicyTest.kt`:

- `bookingPriceOwnOnly` — цена без проката.
- `bookingPriceRentalBoth` — цена с обоими пунктами проката.
- `phoneValidation` — нормализация и валидация телефона.

Запуск на Windows ограничен: нет JVM-only target для commonTest; полный `allTests` требует iOS toolchain и Android SDK.

---

## Запуск и интеграция с backend

### Backend

```bash
cd Climbing/backend
docker compose up
# API: http://localhost:8080/v1
```

### Сборка клиента (проверено)

```powershell
cd Climbing/client
.\gradlew.bat :shared:compileCommonMainKotlinMetadata
.\gradlew.bat :shared:compileKotlinWasmJs
```

### Android (требуется настройка)

1. Создать `Climbing/client/local.properties`:

```properties
sdk.dir=C\:\\Users\\<USER>\\AppData\\Local\\Android\\Sdk
```

2. Запустить эмулятор и backend.
3. Собрать и установить:

```powershell
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:installDebug
```

Base URL для эмулятора: `http://10.0.2.2:8080/v1` (задан в `AppConfig.android.kt`).

### Web (wasmJs)

Модуль `webApp` содержит entry point; для полноценного браузерного запуска нужен `compileKotlinWasmJs` + dev server (не проверялся end-to-end в этом отчёте).

---

## Исправления при сборке

В процессе реализации устранены ошибки компиляции Kotlin/MPP:

- `@JvmInline` value classes заменены на `@Serializable data class` для ID-типов (совместимость с serialization).
- Дубликат `enum class SlotCta` удалён из `Policies.kt`.
- `VerticalApiClient.send` сделан `internal suspend inline` (как в `VolnaApiClient`) — иначе public inline не может вызывать private `execute`.
- `kotlin.time.Clock` + opt-in `ExperimentalTime`.
- `@OptIn(ExperimentalMaterial3Api)` для TopAppBar на детальных экранах.

---

## Известные ограничения

1. **Android SDK** не настроен в среде разработки — APK не собирался.
2. **iosApp** отсутствует — iOS framework генерируется, но нет Xcode-проекта.
3. **Waitlist leave/get**, рейтинг, push — не реализованы.
4. **SCR-002** — упрощённый date picker (текстовый ввод + пресеты), без полноценного календаря Material.
5. **Тема** — стандартный Material3, без Figma tokens.
6. **Офлайн** — нет кэша «Мои записи» (Q 9.2).
7. **Idempotency-Key** — не используется (нет в текущем OpenAPI).
8. **Idempotency-Key** — не используется (нет в текущем OpenAPI).

---

## Рекомендуемые следующие шаги

1. Настроить `local.properties` и прогнать happy path на Android-эмуляторе против `docker compose` backend.
2. **CMP-11** — `createRating` после `ATTENDED` (SCR-011).
3. **CMP-12** — push permission + `POST /profile/push-token`.
4. **SCR-013** — sheet «Изменить контакты» на форме записи.
5. **CMP-10** — `deleteWaitlistEntry`, отображение позиции из `getBooking`.
6. **CMP-14** — MockEngine тесты для `VerticalApiClient` и репозиториев.
7. Создать `iosApp` и подключить `VerticalShared.framework`.

---

## Список ключевых файлов

| Область | Путь |
|---------|------|
| Точка входа UI | `shared/.../VerticalApp.kt` |
| DI | `shared/.../di/AppModule.kt` |
| API client | `shared/.../core/network/VerticalApiClient.kt` |
| Репозитории | `shared/.../data/Repositories.kt` |
| DTO | `shared/.../data/dto/Dtos.kt` |
| Domain | `shared/.../domain/model/Models.kt`, `domain/policy/` |
| Расписание | `shared/.../presentation/schedule/` |
| Слот | `shared/.../presentation/slot/` |
| Запись | `shared/.../presentation/booking/` |
| Мои записи | `shared/.../presentation/bookings/` |
| Лист ожидания | `shared/.../presentation/waitlist/` |
| Ошибки записи | `shared/.../domain/policy/BookingErrorPolicy.kt` |
| Android | `androidApp/.../MainActivity.kt` |
| Unit-тесты | `shared/src/commonTest/.../DomainPolicyTest.kt` |

---

## Итог

Создан рабочий **CMP-клиент MVP** с чистой архитектурой и вертикальным срезом: расписание (фильтры периода и слотов) → деталь → запись / лист ожидания → список записей → отмена с предупреждением LOGIC-004. Общий Kotlin-код (`commonMain`) **компилируется успешно**. Для полного соответствия ТЗ остаются: рейтинг (SCR-011), push (LOGIC-007), sheet контактов (SCR-013), офлайн-кэш, polish UI и smoke на устройстве.
