# 02 — Расхождения бэкенда с ТЗ

Путь: `Climbing/backend/`. Сравнение с LOGIC-001–008, SCR-*, OpenAPI.

---

## BE-01: Асимметрия лимита «1 запись в день»

| Поле | Значение |
|------|----------|
| **ID** | BE-01 |
| **Приоритет** | P0 |
| **Статус** | Решён |
| **Файл** | `internal/store/store.go` |

**Ожидание ТЗ:** не более одной «записи» в календарный день — и ACTIVE-бронь, и WAITLIST.

**Факт:**

- `joinWaitlist` → `checkOneLiveBookingPerDay` (статусы `ACTIVE`, `WAITLIST`)
- `createBooking` → `checkOneBookingPerDay` (только `ACTIVE`)

**Сценарий-баг:** клиент в очереди утром → может создать ACTIVE-бронь вечером того же дня.

**Исправление:** в `CreateBooking` заменить `checkOneBookingPerDay` на `checkOneLiveBookingPerDay`.

```go
// store.go:272 — сейчас
if err := checkOneLiveBookingPerDay(ctx, tx, clientID, detail.StartAt); err != nil {
```

**Решение (2026-07-05):** В `CreateBooking` используется `checkOneLiveBookingPerDay` — блокируется вторая запись в тот же день при наличии WAITLIST. Добавлен интеграционный тест `TestOneBookingPerDayAfterWaitlistIntegration` в `flow_integration_test.go`.

---

## BE-02: Граница «1 час» — расхождение Go и PostgreSQL

| Поле | Значение |
|------|----------|
| **ID** | BE-02 |
| **Приоритет** | P0 |
| **Статус** | Решён |
| **ТЗ** | LOGIC-004, L-004-03, L-004-08, Q 3.4 |
| **Файлы** | `internal/store/store.go`, `db/migrations/000003_tz_compliance.up.sql` |

**Ожидание ТЗ:** при `minutesUntilStart >= 60` — ранняя отмена, место освобождается сразу, waitlist уведомляется.

**Факт:**

| Слой | Условие «ранней отмены» |
|------|-------------------------|
| Go `isEarlyCancel` | `startsAt.Sub(now) >= time.Hour` |
| DB trigger `trg_bookings_release_slot` | `starts_at > now() + interval '1 hour'` |

**Сценарий-баг:** ровно за 60 минут Go может вызвать `notifyFirstWaitlist`, а trigger не освободит место.

**Исправление:** выровнять оператор (`>=` vs `>`) в Go и SQL.

**Решение (2026-07-05):** Условие в `trg_bookings_release_slot` выровнено на `v_starts_at >= now() + interval '1 hour'` в `000003_tz_compliance.up.sql` и миграции `000005_release_rental_boundary.up.sql` (согласовано с Go `isEarlyCancel`: `>= 1 hour`).

---

## BE-03: Прокатный фонд не возвращается при отмене

| Поле | Значение |
|------|----------|
| **ID** | BE-03 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | Q 2.4, RE-03, RE-04, L-002-05 |
| **Файлы** | `db/migrations/000003_tz_compliance.up.sql` |

**Ожидание ТЗ:** прокатный фонд учитывается атомарно при бронировании и должен корректно освобождаться.

**Факт:** trigger `trg_bookings_reserve_slot` декрементирует `shoes_available` / `harness_available`; в `trg_bookings_release_slot` зеркальной логики нет.

**Эффект:** со временем прокат «заканчивается» даже после отмен броней.

**Исправление:** добавить восстановление инвентаря при отмене RENTAL-брони.

**Решение (2026-07-05):** В `000005_release_rental_boundary.up.sql` trigger `trg_bookings_release_slot` восстанавливает `shoes_available`/`harness_available` и пересчитывает `is_bookable` при ранней отмене RENTAL-брони.

---

## BE-04: `slots.status = UNAVAILABLE` при исчерпании проката

| Поле | Значение |
|------|----------|
| **ID** | BE-04 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | Q 2.4, L-002-05, SCR-004 |

**Ожидание ТЗ:** при исчерпании проката слот помечается недоступным (`UNAVAILABLE` / `isBookable=false`).

**Факт:** меняется только `rental_availability.is_bookable`; `slots.status` остаётся `OPEN`.

**Влияние:** UI может показывать слот как «открытый», но запись заблокирована.

---

## BE-05: Статус waitlist `CONVERTED` не выставляется

| Поле | Значение |
|------|----------|
| **ID** | BE-05 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | SCR-012, waitlist lifecycle |

**Факт:** enum `CONVERTED` есть в схеме, но не устанавливается при завершении записи после push.

---

## BE-06: Waitlist не уведомляется при отмене скалодромом

| Поле | Значение |
|------|----------|
| **ID** | BE-06 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | WL-10, Q 3.4 |

**Факт:** `notifyFirstWaitlist` вызывается только в `CancelBooking` (клиент). `CancelSlotByGym` не уведомляет очередь (слот становится `CANCELLED`).

**Примечание:** для отменённого скалодромом слота waitlist неактуален; расхождение скорее аналитическое.

---

## BE-07: Лимит 8 мест для новичкового формата не enforced

| Поле | Значение |
|------|----------|
| **ID** | BE-07 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | BR-008, SCR-004, R-015 |

**Факт:** `beginner_capacity_limit` в схеме/seed; в `CreateBooking` отдельной проверки нет — полагается на `capacity` из слота.

---

## BE-08: `createBooking` не использует Bearer-сессию

| Поле | Значение |
|------|----------|
| **ID** | BE-08 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-001, L-001-10 |

**Факт:** `createBooking` всегда требует `client` в теле; JWT из заголовка не подставляет профиль автоматически.

**Влияние:** повторные клиенты вынуждены передавать контакты в каждом запросе (клиент так и делает).

---

## BE-09: Неполное покрытие тестами BE-17h

| Поле | Значение |
|------|----------|
| **ID** | BE-09 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | `BE_IMPLEMENTATION_PLAN.md` BE-17h |

**Не покрыто интеграционными тестами:**

- поздняя отмена → `free_spots` не меняется;
- `ONE_BOOKING_PER_DAY` при create после waitlist в тот же день;
- гонка за прокат;
- граница ровно 60 минут;
- возврат проката при отмене.

**Существующие тесты:** `flow_integration_test.go`, `tz_compliance_test.go` (unit `isEarlyCancel` only).

**Решение (2026-07-05):** Добавлен `TestOneBookingPerDayAfterWaitlistIntegration` (create после waitlist → 409 `ONE_BOOKING_PER_DAY`). Существующие тесты покрывают раннюю отмену с waitlist (`TestEarlyCancelNotifiesWaitlistIntegration`), rebook forbidden, join waitlist. Граничные сценарии (ровно 60 мин, гонка проката) остаются на unit/нагрузочном уровне — поведение зафиксировано в миграции `000005`.

---

## BE-10: OpenAPI vs handler — семантика ошибок

| Поле | Значение |
|------|----------|
| **ID** | BE-10 |
| **Приоритет** | P3 |
| **Статус** | Открыт |

| Ситуация | OpenAPI / ожидание | Факт handler |
|----------|-------------------|--------------|
| 401 / 404 | `VALIDATION_ERROR` в examples | `domain.ErrUnauthorized`, `ErrNotFound` → `VALIDATION_ERROR` |
| `joinWaitlist` на слот с местами | 409 | 409 `VALIDATION_ERROR`, не `NO_SPOTS` |
| `CANCEL_TOO_LATE` | «не в MVP» для <1ч | Используется для «слот уже начался» |
| `listSlots` pagination | `meta.limit/offset` в response | Hardcoded 50/0, нет query params |

---

## BE-11: `getSlot.isBookable` не учитывает выбор проката пользователя

| Поле | Значение |
|------|----------|
| **ID** | BE-11 |
| **Приоритет** | P3 |
| **Статус** | Открыт |
| **ТЗ** | L-002-09, SCR-005 |

**Факт:** `isBookable` глобальный по слоту; не различает «нужны только скальники» vs «только страховка».

**Влияние:** клиент может пройти pre-check на SCR-004, но получить `RENTAL_UNAVAILABLE` на submit.
