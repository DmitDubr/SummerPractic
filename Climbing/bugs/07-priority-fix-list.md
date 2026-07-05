# 07 — Приоритизированный план исправлений

> **Обновление 2026-07-05:** P0 и P1 закрыты в коде; статусы в детальных документах — **Решён**. ТЗ (`01-analysis/`) не изменялось.

Сводная таблица всех расхождений с приоритетами и ссылками на детальные документы.

---

## P0 — Критичные баги инвариантов (бэкенд)

| # | ID | Описание | Файл / действие | Статус |
|---|-----|----------|-----------------|--------|
| 1 | BE-01 / XL-01 | `createBooking` должен использовать `checkOneLiveBookingPerDay` | `store.go:272` | ✅ Решён |
| 2 | BE-02 / XL-02 | Выровнять границу 1 часа Go vs PostgreSQL | `000003`, `000005` | ✅ Решён |
| 3 | BE-09 | Интеграционные тесты BE-17h | `flow_integration_test.go` | ✅ Решён |

---

## P1 — Критичный UX и данные

| # | ID | Описание | Слой | Статус |
|---|-----|----------|------|--------|
| 4 | BE-03 / XL-03 | Возврат проката при отмене | BE | ✅ Решён |
| 5 | CMP-06 / XL-04 | `leaveWaitlist` + CTA на SCR-009 | CMP | ✅ Решён |
| 6 | CMP-07 | `waitlistPosition` в DTO и UI | CMP | ✅ Решён |
| 7 | CMP-10 / XL-07 | Обработка ошибок отмены | CMP | ✅ Решён |
| 8 | CMP-05 / XL-06 | Сегмент «Прошедшие» + все статусы | CMP | ✅ Решён |
| 9 | CMP-09 | Полная деталь SCR-009 | CMP | ✅ Решён |
| 10 | CMP-18 | Недостающие API в Repositories | CMP | ✅ Решён |

---

## P2 — Полнота MVP по ТЗ

| # | ID | Описание | Слой | Документ |
|---|-----|----------|------|----------|
| 11 | CMP-01 / XL-05 | SCR-011 рейтинги | CMP | [03-client](03-client-discrepancies.md#cmp-01-scr-011--оценка-инструктора-не-реализована) |
| 12 | CMP-02 | SCR-013 contact sheet | CMP | [03-client](03-client-discrepancies.md#cmp-02-scr-013--контактный-профиль-sheet-не-реализован) |
| 13 | CMP-03, CMP-04 / XL-09 | SCR-006 + push (LOGIC-007) | CMP | [03-client](03-client-discrepancies.md#cmp-04-logic-007--push-не-реализован) |
| 14 | CMP-08 | Офлайн-кэш SCR-008/009 | CMP | [03-client](03-client-discrepancies.md#cmp-08-logic-008--офлайн-кэш-не-реализован) |
| 15 | CMP-11 | Рейтинг на SCR-003, SCR-004 | CMP | [03-client](03-client-discrepancies.md#cmp-11-logic-006--рейтинг-инструктора-отображается-частично) |
| 16 | CMP-15 | Блокировка отмены offline | CMP | [03-client](03-client-discrepancies.md#cmp-15-logic-004--отмена-offline-не-блокируется) |
| 17 | BE-04 | `slots.status = UNAVAILABLE` | BE | [02-backend](02-backend-discrepancies.md#be-04-slotsstatus--unavailable-при-исчерпании-проката) |
| 18 | BE-05 | Waitlist `CONVERTED` | BE | [02-backend](02-backend-discrepancies.md#be-05-статус-waitlist-converted-не-выставляется) |

---

## P3 — Документация и polish

| # | ID | Описание | Слой | Документ |
|---|-----|----------|------|----------|
| 19 | TZ-INT-01 / XL-08 | Унифицировать `ONE_BOOKING_PER_DAY` в SCR-007 | ТЗ | [01-tz-internal](01-tz-internal-inconsistencies.md#tz-int-01-код-лимита-1-запись-в-день) |
| 20 | TZ-INT-02 | Эндпоинт рейтинга в SCR-011 | ТЗ | [01-tz-internal](01-tz-internal-inconsistencies.md#tz-int-02-эндпоинт-оценки-инструктора) |
| 21 | TZ-INT-03 | API выхода из waitlist в SCR-009 | ТЗ | [01-tz-internal](01-tz-internal-inconsistencies.md#tz-int-03-выход-из-листа-ожидания) |
| 22 | TZ-INT-04 | Семантика `CANCEL_TOO_LATE` | ТЗ/BE | [01-tz-internal](01-tz-internal-inconsistencies.md#tz-int-04-cancel_too_late-в-openapi-vs-mvp) |
| 23 | CMP-12 | Календарь SCR-002 | CMP | [03-client](03-client-discrepancies.md#cmp-12-scr-002--упрощённый-фильтр-дат) |
| 24 | CMP-13 | Pull-to-refresh | CMP | [03-client](03-client-discrepancies.md#cmp-13-нет-pull-to-refresh) |
| 25 | CMP-14 | SCR-010 bottom sheet | CMP | ✅ Решён |
| 26 | BE-10 | OpenAPI vs handler errors | BE | [02-backend](02-backend-discrepancies.md#be-10-openapi-vs-handler--семантика-ошибок) |
| 27 | BE-11 | `isBookable` per rental item | BE | [02-backend](02-backend-discrepancies.md#be-11-getslotisbookable-не-учитывает-выбор-проката-пользователя) |
| 28 | CMP-16, CMP-17 | Profile cache-first, VM lifecycle | CMP | [03-client](03-client-discrepancies.md) |

---

## Статистика

| Категория | Количество |
|-----------|:----------:|
| Внутренние противоречия ТЗ | 5 |
| Бэкенд vs ТЗ | 11 |
| Клиент vs ТЗ | 18 |
| Сквозные (cross-layer) | 9 |
| **Всего уникальных ID** | **~35** |

| Приоритет | Количество |
|-----------|:----------:|
| P0 | 3 |
| P1 | 7 |
| P2 | 8 |
| P3 | 10 |

---

## Рекомендуемый порядок работ

1. **Спринт 1 (стабильность):** BE-01, BE-02, BE-09, BE-03
2. **Спринт 2 (waitlist + bookings UI):** CMP-06, CMP-07, CMP-05, CMP-09, CMP-10, CMP-18
3. **Спринт 3 (MVP completeness):** CMP-01, CMP-02, CMP-03, CMP-04, CMP-08, CMP-11
4. **Спринт 4 (polish + docs):** P3 items, TZ-INT-*
