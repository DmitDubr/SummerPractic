# План: авторизация и список слотов (BE-04, BE-05, BE-06)

> Итерация для вертикального среза SCR-001 (расписание) + SCR-013 (профиль).
> Источники: `01-analysis/api/`, `BE_IMPLEMENTATION_PLAN.md`.

## Цель итерации

Клиент может:
1. **Авторизоваться** — upsert профиля (имя + телефон) и получить `sessionToken` (JWT Bearer).
2. **Получить расписание** — `GET /v1/slots` без авторизации, с дефолтным периодом 7 дней и фильтрами.

## Endpoints

| operationId | Метод | Auth | Описание |
|-------------|-------|------|----------|
| `updateProfile` | `PATCH /v1/profile` | опционально | Upsert имя + телефон → `sessionToken` |
| `getProfile` | `GET /v1/profile` | Bearer | Профиль текущего клиента |
| `listSlots` | `GET /v1/slots` | — | Список слотов за период |
| `getSlot` | `GET /v1/slots/{slotId}` | — | Детали слота |

## TOC / Todo

- [x] [AUTH-01. JWT ClientSession](#auth-01-jwt-clientsession)
- [x] [AUTH-02. Middleware и защищённые маршруты](#auth-02-middleware-и-защищённые-маршруты)
- [x] [AUTH-03. Profile upsert и GET](#auth-03-profile-upsert-и-get)
- [x] [SLOTS-01. Репозиторий listSlots](#slots-01-репозиторий-listslots)
- [x] [SLOTS-02. HTTP handler listSlots / getSlot](#slots-02-http-handler-listslots--getslot)
- [x] [SLOTS-03. Seed-данные и интеграционные тесты](#slots-03-seed-данные-и-интеграционные-тесты)

---

## AUTH-01. JWT ClientSession

**Сделать:**

- `internal/platform/auth/token.go` — HS256 JWT, claims: `client_id`, `phone`, `exp`.
- `Issue(clientID, phone)` — выдача токена.
- `Parse(token)` — валидация, возврат `clientID` и `phone`.
- Конфиг: `JWT_SECRET`, `JWT_TTL` из env.

**Готово, когда:**

- Unit-тесты: валидный токен, просроченный, битая подпись.
- `go test ./internal/platform/auth/...` зелёный.

---

## AUTH-02. Middleware и защищённые маршруты

**Сделать:**

- `optionalAuth` middleware — извлекает Bearer, кладёт `clientID` в context.
- `requireAuth` в handler — для `GET /profile`, `GET /bookings` и др.
- Невалидный/отсутствующий токен на защищённых маршрутах → `401`.

**Готово, когда:**

- `GET /v1/bookings` без Bearer → `401`.
- `GET /v1/profile` с валидным Bearer → `200`.

---

## AUTH-03. Profile upsert и GET

**Сделать:**

- `PATCH /v1/profile`:
  - валидация `^\+7\d{10}$`, имя 2–50 символов;
  - upsert по `phone` UNIQUE;
  - ответ `UpdateProfileResponse` + `sessionToken`.
- `GET /v1/profile` — профиль по `client_id` из JWT; `404` если нет.

**Готово, когда:**

- Integration: `PATCH` → `GET` с тем же токеном возвращает те же данные.
- Повторный `PATCH` с тем же телефоном обновляет имя, не создаёт дубликат.

---

## SLOTS-01. Репозиторий listSlots

**Сделать:**

- `store.ListSlots(ctx, SlotFilters)` — SQL с JOIN на `training_formats`, `zones`, `instructors`, `rental_availability`.
- Дефолтный период: `dateFrom=today`, `dateTo=today+6` (R-027).
- Фильтры: `instructorIds` (OR), `timeOfDay`, `level` (AND между группами).
- Поля: `freeSpots`, `capacity`, `status`, `isBookable`, `instructor.rating`, `price`.
- Сортировка `starts_at ASC`.

**Готово, когда:**

- Integration test: после seed возвращает ≥1 слот.
- `capacity`/`freeSpots` из БД, не захардкожены.

---

## SLOTS-02. HTTP handler listSlots / getSlot

**Сделать:**

- `OpenAPIServer.ListSlots` — парсинг query params из oapi-codegen, вызов store.
- `GET /v1/slots/{slotId}` — `SlotDetail` с `priceBreakdown`, `rentalAvailability`, `gym`.
- `isBookable` = `status=OPEN` AND `freeSpots>0` AND rental available.

**Готово, когда:**

- `curl localhost:8080/v1/slots` → JSON с `items` и `meta`.
- Неизвестный `slotId` → `404`.

---

## SLOTS-03. Seed-данные и интеграционные тесты

**Сделать:**

- `db/migrations/000002_seed_dev.up.sql` — gym, зоны, форматы, 2 инструктора, 2 слота.
- `TestProfileAuthIntegration` — PATCH → GET с Bearer.
- `TestListSlotsIntegration` — GET /slots после миграций.

**Готово, когда:**

- `TEST_DATABASE_URL=... go test ./internal/handler/... -run Integration` зелёный.
- Smoke: `docker compose up` → `GET /v1/slots` возвращает seed-слоты.

---

## Структура файлов

```text
backend/
  internal/
    platform/auth/token.go          # JWT issue/parse
    handler/
      handler.go                    # UpdateProfile, GetProfile
      openapi_server.go             # ListSlots, GetSlot
      router.go                     # optionalAuth middleware
    store/store.go                  # ListSlots, GetSlotDetail, UpsertClient
    validate/validate.go            # ClientContacts
  db/migrations/
    000001_init.up.sql
    000002_seed_dev.up.sql
```

## Доменные ограничения

| ID | Ограничение | Где |
|----|-------------|-----|
| Q 1.1 | Auth: имя + телефон, без OTP | `PATCH /profile` |
| R-027 | Дефолт listSlots — 7 дней | handler default dates |
| R-015 | capacity, цены — из БД | store SQL |
| LOGIC-001 | Валидация телефона `+7XXXXXXXXXX` | validate.ClientContacts |

## Порядок реализации

1. AUTH-01 → AUTH-02 → AUTH-03 (вертикальный срез профиля).
2. SLOTS-01 → SLOTS-02 → SLOTS-03 (каталог без auth).
3. Smoke через `docker compose up` + curl.

## Зависимости для CMP-клиента

После этой итерации клиент может реализовать:
- **SCR-001** — экран расписания (`listSlots`).
- **SCR-013** — экран профиля (`updateProfile` / `getProfile`).
