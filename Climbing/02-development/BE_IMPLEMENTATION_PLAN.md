# План реализации BE для «Вертикаль»

> Скалодром «Вертикаль» — Client API (Go) для мобильного приложения клиента.
> Источники: `01-analysis/`, OpenAPI `01-analysis/api/openapi.yaml`, миграции `backend/db/migrations/`.

## TOC / Todo реализации

- [ ] [BE-00. Создать каркас backend-приложения](#be-00-создать-каркас-backend-приложения)
- [ ] [BE-01. Подключить OpenAPI как контракт](#be-01-подключить-openapi-как-контракт)
- [ ] [BE-02. Реализовать общую HTTP-инфраструктуру](#be-02-реализовать-общую-http-инфраструктуру)
- [ ] [BE-03. Довести БД, миграции и sqlc](#be-03-довести-бд-миграции-и-sqlc)
- [ ] [BE-04. Реализовать сессии ClientSession (JWT)](#be-04-реализовать-сессии-clientsession-jwt)
- [ ] [BE-05. Реализовать Profile](#be-05-реализовать-profile)
- [ ] [BE-06. Реализовать read-only каталог слотов и инструкторов](#be-06-реализовать-read-only-каталог-слотов-и-инструкторов)
- [ ] [BE-07. Реализовать атомарное создание брони](#be-07-реализовать-атомарное-создание-брони)
- [ ] [BE-08. Реализовать список и детали броней](#be-08-реализовать-список-и-детали-броней)
- [ ] [BE-09. Реализовать отмену брони (правило 1 часа)](#be-09-реализовать-отмену-брони-правило-1-часа)
- [ ] [BE-10. Реализовать лист ожидания](#be-10-реализовать-лист-ожидания)
- [ ] [BE-11. Реализовать оценки инструкторов](#be-11-реализовать-оценки-инструкторов)
- [ ] [BE-12. Реализовать push-токены и dev-симуляции](#be-12-реализовать-push-токены-и-dev-симуляции)
- [ ] [BE-13. Довести контрактные ошибки и валидацию](#be-13-довести-контрактные-ошибки-и-валидацию)
- [ ] [BE-14. Добавить полный набор Go-тестов](#be-14-добавить-полный-набор-go-тестов)
- [ ] [BE-15. Подготовить локальный запуск и документацию](#be-15-подготовить-локальный-запуск-и-документацию)
- [ ] [BE-16. Финальная проверка готовности BE](#be-16-финальная-проверка-готовности-be)

## Стек приложения

- Язык: **Go 1.23+**.
- API: REST JSON, **OpenAPI-first** — `01-analysis/api/openapi.yaml` (многофайловый: `paths/`, `components/`).
- HTTP: `chi` + middleware (request id, recovery, logging, auth).
- Codegen: **oapi-codegen** — типы и `ServerInterface` по контракту.
- БД: **PostgreSQL 16**, `pgx`/`pgxpool`, транзакции + row locks для бронирований.
- SQL: **sqlc** для типобезопасных запросов; **golang-migrate** для миграций.
- Auth: имя + телефон (Q 1.1), **JWT Bearer** `sessionToken` в ответах `PATCH /profile` и `POST /bookings` (отдельного OTP/login flow нет).
- Логи: `log/slog`, конфиг через env (`caarlos0/env`).
- Тесты: unit + integration (testcontainers-go), concurrency tests для booking/cancel/waitlist.
- Runtime: Docker Compose (API + PostgreSQL), Makefile.

## Функционал и endpoints

| Домен | operationId | Endpoint | Функционал | Auth |
|-------|-------------|----------|------------|------|
| Slots | `listSlots` | `GET /slots` | Список слотов, фильтры, дефолт 7 дней (R-027) | — |
| Slots | `getSlot` | `GET /slots/{slotId}` | Детали слота, pre-check перед записью | — |
| Instructors | `listInstructors` | `GET /instructors` | Справочник для фильтра SCR-003 | — |
| Bookings | `createBooking` | `POST /bookings` | Upsert профиля + атомарная бронь | опционально |
| Bookings | `listBookings` | `GET /bookings` | Список броней клиента | Bearer |
| Bookings | `getBooking` | `GET /bookings/{bookingId}` | Детали брони, deep link | Bearer |
| Bookings | `cancelBooking` | `POST /bookings/{bookingId}/cancel` | Отмена клиентом (LOGIC-004) | Bearer |
| Bookings | `leaveWaitlist` | `POST /bookings/{bookingId}/leave-waitlist` | Выход из очереди через бронь | Bearer |
| Waitlist | `joinWaitlist` | `POST /slots/{slotId}/waitlist` | Вступление в очередь | Bearer / аноним* |
| Waitlist | `getWaitlistEntry` | `GET /waitlist/{waitlistEntryId}` | Позиция в очереди | Bearer |
| Waitlist | `deleteWaitlistEntry` | `DELETE /waitlist/{waitlistEntryId}` | Покинуть очередь | Bearer |
| Profile | `getProfile` | `GET /profile` | Контактный профиль | Bearer |
| Profile | `updateProfile` | `PATCH /profile` | Upsert имя + телефон | Bearer / аноним* |
| Profile | `registerPushToken` | `POST /profile/push-token` | Регистрация FCM/APNs | Bearer |
| Ratings | `createRating` | `POST /ratings` | Оценка инструктора 1–5 | Bearer |

\* `createBooking`, `updateProfile`, `joinWaitlist` принимают `client` в теле при отсутствии сессии и выдают `sessionToken`.

Служебные (вне OpenAPI, для эксплуатации): `GET /healthz`, `GET /readyz`.

## Доменные ограничения (что BE обязан enforce)

| ID | Ограничение | Где |
|----|-------------|-----|
| R-004 | Атомарность брони, 0 двойных записей на место | `createBooking` TX + триггеры БД |
| Q 1.3 | Не более 1 `ACTIVE` брони в календарный день | partial unique index + use case |
| Q 1.4 | Лист ожидания при `freeSpots = 0` | `joinWaitlist` |
| Q 2.3 | RENTAL — хотя бы скальники или страховка | валидация + CHECK в БД |
| Q 2.4 | Прокат исчерпан → `RENTAL_UNAVAILABLE` / `UNAVAILABLE` | pre-check + use case |
| Q 3.2 / LOGIC-004 | Ранняя отмена ≥1 ч — место освобождается | `cancelBooking` + trigger |
| Q 3.1 | Поздняя отмена <1 ч — **разрешена**, штрафов нет | не блокировать на BE |
| FR-011 / R-008 | Запись на `CANCELLED` слот запрещена | `SLOT_CANCELLED` / `SLOT_REBOOK_FORBIDDEN` |
| FR-009–010 | `CANCELLED_BY_GYM` + причина (инициация вне скоупа) | dev-симуляция + обработка статуса |
| FR-012 / Q 5.1 | Оценка только после `ATTENDED`, одна на бронь | `createRating` + trigger |
| R-015 | Лимиты 8/16, цены, прокатный фонд — из БД/API | не хардкодить |
| R-027 | Дефолт `listSlots` — 7 дней | query default в handler |
| R-028 | Только клиентский контур | не делать admin/instructor API |

## Вне скоупа BE (не реализовывать)

- Админка, интерфейс инструктора, CRUD расписания (NFR-007).
- Онлайн-оплата, штрафы за позднюю отмену, скидки постоянным (feature-list §6).
- OTP/SMS-авторизация (в «Вертикаль» — имя + телефон, Q 1.1).
- Фильтр болдеринг/трассы на API (отложен, Q 1.6).
- Миграция легаси-данных (R-015).

## Правила для итераций

- Один пункт = одна итерация: минимальный вертикальный срез + тесты + прогон проверки.
- Изменение публичного API — сначала правка `01-analysis/api/*`, затем codegen.
- Зависимости направлены внутрь: `handler → usecase → domain → repository`.
- Бизнес-инварианты — в use case **и** в БД (триггеры/CHECK как страховка).
- `gyms`, `zones`, `training_formats`, `instructors`, `slots` — **read-only** для Client API; dev-данные через seed `000002_seed_dev`.

## Целевая структура `backend/`

```text
backend/
  cmd/api/main.go
  api/openapi.yaml              # symlink или copy из 01-analysis/api/
  internal/
    config/
    domain/                     # сущности, доменные ошибки (ErrorCode)
    usecase/
      profile/
      schedule/                 # listSlots, getSlot, listInstructors
      booking/                  # create, cancel, list, get
      waitlist/
      rating/
    adapter/
      http/                     # oapi-codegen handlers, error mapper
      postgres/                 # sqlc repositories
    platform/
      auth/                     # JWT issue/validate
      clock/                    # injectable time (тесты LOGIC-004)
  db/
    migrations/                 # 000001_init, 000002_seed_dev (черновик есть)
    queries/                    # .sql для sqlc
  Makefile
  docker-compose.yml
  README.md
```

---

## Декомпозиция BE

### BE-00. Создать каркас backend-приложения

Сделать:

- Инициализировать Go module в `backend/`.
- Создать `cmd/api/main.go`, `internal/config`, wiring зависимостей.
- Добавить Makefile: `fmt`, `lint`, `test`, `run`, `migrate-up`, `migrate-down`, `sqlc`.
- Docker Compose: PostgreSQL 16 + placeholder API-сервис.

Готово, когда:

- `go test ./...` проходит (пустые пакеты допустимы).
- `docker compose up db` поднимает PostgreSQL.
- `GET /healthz` возвращает 200.

---

### BE-01. Подключить OpenAPI как контракт

Сделать:

- Настроить **oapi-codegen** на `01-analysis/api/openapi.yaml` (bundle paths/components).
- Сгенерировать `internal/adapter/http/gen/` — types + `ServerInterface`.
- Сохранить соответствие `operationId` именам handler-методов.
- Добавить `go generate` и правило «generated code не редактировать».

Готово, когда:

- `go generate ./...` воспроизводимо генерирует код.
- Все 15 `operationId` из таблицы выше присутствуют в `ServerInterface`.
- `go build ./cmd/api` проходит.

---

### BE-02. Реализовать общую HTTP-инфраструктуру

Сделать:

- Router `chi` с префиксом `/v1`.
- Middleware: request id, access log (`slog`), panic recovery, CORS (dev).
- Единый **error mapper** → `ErrorResponse` (`code`, `message`, `details?`) по `schemas.yaml`.
- Маппинг HTTP: 400, 401, 403, 404, 409, 500.
- Заглушки handlers (501) для всех operationId — чтобы роутер собирался.

Готово, когда:

- Handler tests: невалидный JSON → `400 VALIDATION_ERROR`.
- Неизвестный path → 404.
- Защищённый endpoint без Bearer → `401`.

---

### BE-03. Довести БД, миграции и sqlc

Сделать:

- Проверить и при необходимости доработать `db/migrations/000001_init.up.sql`:
  - enum-статусы совпадают с OpenAPI;
  - partial unique indexes (1 бронь/день, waitlist, rating);
  - триггеры reserve/release slot, `ATTENDED`-only rating.
- Применить `000002_seed_dev` — gym, зоны, форматы, инструкторы, 2 слота.
- Настроить **sqlc**: `db/queries/*.sql` → `internal/adapter/postgres/sqlc/`.
- Repository interfaces в `internal/domain` или `internal/usecase`.

Готово, когда:

- `migrate up` на пустой БД проходит без ошибок.
- Integration test: seed → `listSlots` repository возвращает ≥1 слот.
- `go test ./internal/adapter/postgres/...` проходит.

---

### BE-04. Реализовать сессии ClientSession (JWT)

Сделать:

- JWT HS256 (dev secret из env): claims `client_id`, `phone`, `exp`.
- `IssueToken(clientID)` — вызывается из `updateProfile` и `createBooking`.
- Middleware `ClientSession`: извлечь Bearer, положить `clientID` в context.
- Опциональная auth: endpoints с `security: [{}]` + Bearer — принимать оба варианта.

Готово, когда:

- Unit tests: валидный/просроченный/битый токен.
- Middleware test: context содержит `clientID`.

---

### BE-05. Реализовать Profile

Сделать:

- `GET /profile` — профиль текущего клиента; `404` если нет (первая запись).
- `PATCH /profile` — upsert `name` + `phone`:
  - валидация `^\+7\d{10}$`, имя непустое (LOGIC-001);
  - upsert по `phone` UNIQUE;
  - ответ `UpdateProfileResponse` + `sessionToken`.
- `isComplete = true` при валидных полях; `isRegularClient` из БД (Q 7.2, только флаг).

Готово, когда:

- Integration: PATCH → GET возвращает те же данные + token.
- Дубликат телефона другого клиента обрабатывается корректно.
- `go test` для profile use case проходит.

---

### BE-06. Реализовать read-only каталог слотов и инструкторов

Сделать:

- `GET /slots`:
  - дефолт `dateFrom=today`, `dateTo=today+6` (R-027, LOGIC-005);
  - фильтры: `instructorIds` (OR), `timeOfDay`, `level` (AND между группами);
  - сортировка `startAt ASC`, группировка — на клиенте;
  - поля: `freeSpots`, `capacity`, `status`, `isBookable`, `instructor.rating`.
- `GET /slots/{slotId}`:
  - `SlotDetail`: `priceBreakdown`, `rentalAvailability`, `gym`, `durationMinutes`;
  - `isBookable` = f(status, freeSpots, rental).
- `GET /instructors` — список с `rating`, пагинация `limit`/`offset`.

Готово, когда:

- Tests: дефолт 7 дней, каждый фильтр, пустой результат, 404 unknown slot.
- `capacity`/`freeSpots` приходят из БД, не захардкожены.
- Ответы соответствуют примерам в `paths/slots.yaml`.

---

### BE-07. Реализовать атомарное создание брони

Сделать:

- `POST /bookings` по `api-sequence.md` §4:
  1. Валидация `client`, `equipment` (OWN/RENTAL rules).
  2. Upsert профиля (если передан `client`).
  3. Pre-check слота: не `CANCELLED`, `freeSpots > 0`, прокат доступен при RENTAL.
  4. TX: `SELECT slot FOR UPDATE` → insert booking → trigger decrement spots.
  5. Расчёт `totalPrice` серверный (LOGIC-003) → `priceBreakdown`.
  6. Ответ 201 + `sessionToken`.
- Доменные ошибки:

| HTTP | ErrorCode | Условие |
|------|-----------|---------|
| 409 | `NO_SPOTS` | Гонка / нет мест |
| 409 | `ONE_BOOKING_PER_DAY` | Q 1.3 |
| 409 | `SLOT_CANCELLED` | status=CANCELLED |
| 409 | `RENTAL_UNAVAILABLE` | Q 2.4 |
| 403 | `SLOT_REBOOK_FORBIDDEN` | FR-011 |
| 400 | `VALIDATION_ERROR` | Поля |

Готово, когда:

- Happy path: 201, `ACTIVE`, spots уменьшились на 1.
- Concurrency test: N параллельных create на последнее место → ровно 1 успех, остальные `NO_SPOTS`.
- `ONE_BOOKING_PER_DAY` при второй брони в тот же день.

---

### BE-08. Реализовать список и детали броней

Сделать:

- `GET /bookings` — только брони текущего `client_id`; фильтр `status`, пагинация.
- `GET /bookings/{bookingId}` — полный `Booking`:
  - вложенный `slot`, `gym`, `priceBreakdown`;
  - `instructorRating` (null = можно оценить);
  - `waitlistPosition` при `status=WAITLIST`;
  - `cancellationReason` при `CANCELLED_BY_GYM`.
- Чужая бронь → `404` (не раскрывать существование).

Готово, когда:

- Tests: list filter, get own, get foreign → 404.
- Поля совпадают с `schemas.yaml#/Booking`.

---

### BE-09. Реализовать отмену брони (правило 1 часа)

Сделать:

- `POST /bookings/{bookingId}/cancel`:
  - только `status=ACTIVE`, `slot.startsAt > now`;
  - статус → `CANCELLED_BY_CLIENT`, `cancelledAt = now`;
  - если до начала **≥ 1 ч** — trigger освобождает место (Q 3.2, 3.4);
  - если **< 1 ч** — отмена **разрешена**, место тоже освобождается (MVP: штрафов нет, LOGIC-004 — предупреждение только на клиенте).
- Повторная отмена → `409 ALREADY_CANCELLED`.
- Отмена после старта → `403` или скрытая кнопка (контракт допускает 403).

Готово, когда:

- Unit tests границы: `60min`, `59min59s`, после `startsAt`.
- Concurrency: параллельные cancel не освобождают место дважды.
- Integration: cancel → `getSlot` показывает +1 `freeSpots`.

---

### BE-10. Реализовать лист ожидания

Сделать:

- `POST /slots/{slotId}/waitlist`:
  - только при `freeSpots = 0` и `status != CANCELLED`;
  - upsert client при отсутствии сессии;
  - позиция `MAX(position)+1` among `WAITING`;
  - `409 ALREADY_IN_WAITLIST` при дубле.
- `GET /waitlist/{waitlistEntryId}` — только своя запись.
- `DELETE /waitlist/{waitlistEntryId}` — status → `LEFT`.
- `POST /bookings/{bookingId}/leave-waitlist` — альтернативный выход.
- При освобождении места (cancel другого клиента): dev-hook или use case помечает первого в очереди `NOTIFIED` (push — BE-12).

Готово, когда:

- Join на FULL слот → 201 с `position`.
- Join на OPEN слот → 409.
- Два join одного клиента → `ALREADY_IN_WAITLIST`.
- Leave → 204, позиции пересчитываются (опционально в MVP).

---

### BE-11. Реализовать оценки инструкторов

Сделать:

- `POST /ratings`:
  - `stars` 1–5, `bookingId`, `instructorId`;
  - проверка: booking принадлежит клиенту, `status=ATTENDED`;
  - `instructorId` совпадает со слотом брони;
  - `409 ALREADY_RATED`, `403 BOOKING_NOT_ATTENDED`;
  - trigger обновляет `instructors.avg_rating`, `rating_count`.
- После create: `getBooking` возвращает `instructorRating` read-only.

Готово, когда:

- Happy path 201; повтор → `ALREADY_RATED`.
- Рейтинг на `listSlots`/`getSlot` обновляется после оценки.
- ATTENDED-only enforced DB trigger + use case.

---

### BE-12. Реализовать push-токены и dev-симуляции

Сделать:

- `POST /profile/push-token` — upsert в `client_push_tokens`, 204.
- **Dev-only** endpoints (за флагом `DEV_MODE=true`, вне OpenAPI):
  - `POST /dev/slots/{id}/cancel-by-gym` — `CANCELLED_BY_GYM` на брони + `slot.status=CANCELLED` (FR-009, R-008);
  - `POST /dev/bookings/{id}/mark-attended` — `status=ATTENDED` (для тестов SCR-011);
  - `POST /dev/notifications/send` — ручная отправка push (заглушка: log).
- Scheduler stub или cron skeleton для напоминаний (Q 6.1): за день, за 2 ч — логировать, не блокировать MVP.

Готово, когда:

- `registerPushToken` сохраняет токен, повторный вызов идемпотентен.
- Dev cancel-by-gym: брони получают `cancellationReason`, слот `CANCELLED`.
- Dev mark-attended: после этого `createRating` работает.

---

### BE-13. Довести контрактные ошибки и валидацию

Сделать:

- Зафиксировать все `ErrorCode` из `schemas.yaml`:
  `VALIDATION_ERROR`, `SLOT_REBOOK_FORBIDDEN`, `NO_SPOTS`, `ONE_BOOKING_PER_DAY`,
  `SLOT_CANCELLED`, `RENTAL_UNAVAILABLE`, `ALREADY_CANCELLED`, `CANCEL_TOO_LATE`,
  `BOOKING_NOT_ATTENDED`, `ALREADY_RATED`, `ALREADY_IN_WAITLIST`, `WAITLIST_NOT_FOUND`, `SERVER_ERROR`.
- Все error responses — `application/json`, сообщения на **русском** (Q 9.3).
- Request validation по OpenAPI для path/query/body.

Готово, когда:

- Contract test matrix: каждый endpoint — success + основные error codes.
- Примеры в `paths/*.yaml` не противоречат реальным ответам.

---

### BE-14. Добавить полный набор Go-тестов

Сделать:

- Unit: price calculation (LOGIC-003), cancel rule (LOGIC-004), slot availability, JWT.
- Integration: PostgreSQL testcontainer, полные flows:
  - profile → createBooking → list → get → cancel;
  - joinWaitlist → leave;
  - mark-attended → createRating.
- Race: `go test -race` на booking/waitlist/cancel packages.

Готово, когда:

- `go test ./...` стабильно зелёный.
- Coverage критичных use case ≥ разумный порог (booking, cancel, waitlist).

---

### BE-15. Подготовить локальный запуск и документацию

Сделать:

- `backend/README.md`: prerequisites, env, migrate, seed, run, test.
- `.env.example`: `DATABASE_URL`, `JWT_SECRET`, `HTTP_PORT`, `DEV_MODE`.
- `docker compose up` — API + DB + auto-migrate.
- Описать: catalog read-only, seed data, dev endpoints.

Готово, когда:

- Новый разработчик поднимает API с нуля по README ≤ 15 мин.
- `curl localhost:8080/v1/slots` возвращает seed-слоты.

---

### BE-16. Финальная проверка готовности BE

Сделать:

- Сверить все 15 endpoints с `01-analysis/5-mobile-app-spec/feature-list.md` §8.
- Сверить доменные ограничения с `01-analysis/4-design/data-model.md` и LOGIC-*.
- Прогнать: `go fmt`, `golangci-lint`, `go test ./...`, `go test -race` (критичные пакеты).
- Smoke: полный UC-001 → UC-006 через curl/httpie.
- Чеклист «вне скоупа» — ничего лишнего не реализовано.

Готово, когда:

- Все operationId реализованы и соответствуют OpenAPI.
- Booking/cancel/waitlist выдерживают параллельные запросы.
- CMP-клиент может интегрироваться без mock на все MVP-сценарии.

---

## Трассировка: итерация → требования → экраны

| Итерация | FR / Q | Экраны клиента |
|----------|--------|----------------|
| BE-06 | FR-001–004, R-027 | SCR-001, 002, 003, 004 |
| BE-05, BE-07 | FR-005–006, Q 1.1 | SCR-005, 006, 007, 013 |
| BE-08, BE-09 | FR-007–008 | SCR-008, 009, 010 |
| BE-10 | Q 1.4 | SCR-012 |
| BE-11 | FR-012 | SCR-011 |
| BE-12 | FR-010, FR-013, Q 6.1 | SCR-006, push deep link → SCR-009 |
| BE-12 dev | FR-009–011, R-008 | SCR-009, 007 |

## Порядок интеграции с CMP-клиентом

1. **BE-00…BE-06** — клиент может делать SCR-001 (расписание).
2. **BE-04, BE-05, BE-07** — vertical slice записи (SCR-004 → 005 → 006).
3. **BE-08, BE-09** — «Мои записи» и отмена.
4. **BE-10…BE-12** — waitlist, рейтинги, push.

---

## Статус артефактов на старте

| Артефакт | Статус |
|----------|--------|
| OpenAPI `01-analysis/api/` | Готов |
| Миграция `000001_init` | Черновик в `backend/db/migrations/` |
| Seed `000002_seed_dev` | Черновик |
| Go-код `backend/` | Не начат |
| План CMP-клиента | `razrabotka/.../CMP_CLIENT_IMPLEMENTATION_PLAN.md` (референс формата) |
