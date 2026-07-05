# План покрытия тестами — скалодром «Вертикаль»

**Дата:** 2026-07-05  
**Источники:** [TZ_REVIEW_01-analysis.md](TZ_REVIEW_01-analysis.md), `bugs/05–07`, текущие тесты BE/CMP

**Легенда статусов:** ✅ есть · 🔶 частично · ⬜ план · 🚫 blocked (фича не готова)

**Уровни:** `U` unit · `I` integration/API · `C` contract · `E2E` сквозной · `UI` ручной/авто UI

---

## 1. Сводка по уровням

| Уровень | Область | Сейчас | План | Приоритет |
|---------|---------|:------:|:----:|:---------:|
| BE `U` | validate, auth, price, `isEarlyCancel` | 6 | +4 | P1 |
| BE `I` | handler + PostgreSQL flows | 9 | +12 | P0–P2 |
| BE `C` | OpenAPI ↔ handler, коды ошибок | 1 | +8 | P1–P3 |
| CMP `U` | domain policy, VM, mappers | 5 | +18 | P1–P2 |
| CMP `I` | repository + fake API | 0 | +10 | P2 |
| `E2E` | XL-сценарии, waitlist→push→book | 0 | +8 | P0–P2 |
| `UI` | SCR-001–013 smoke + AC | 0 | +13 | P2–P3 |

---

## 2. Покрытие по бизнес-логике (LOGIC-001–008)

| ID | Логика | Ключевые тест-кейсы | Уровень | Статус | Связь |
|----|--------|---------------------|---------|:------:|-------|
| **L-001** | Контактный профиль | PATCH/GET profile, token из booking; валидация имя/телефон; upsert по phone; offline block submit | BE `I`, CMP `U` | 🔶 | LOGIC-001, SCR-005/013 |
| **L-002** | Доступность слота | OPEN/FULL → waitlist CTA; rental exhausted; `NO_SPOTS`, `RENTAL_UNAVAILABLE`, `SLOT_CANCELLED` | BE `I`, CMP `U`, `E2E` | 🔶 | LOGIC-002, SCR-004/007/012 |
| **L-003** | Расчёт цены | own / shoes / harness / both; breakdown в GET booking | BE `U`+`I`, CMP `U` | ✅ | LOGIC-003 |
| **L-004** | Отмена 1 ч | early ≥60 мин → release spot+rental+waitlist; late <60 мин → 200 без release; `CANCEL_TOO_LATE` при started; offline block | BE `I`, CMP `U`, `E2E` | 🔶 | LOGIC-004, XL-02/03/07 |
| **L-005** | Фильтрация | 7 дней, период, формат, инструктор; пустой результат | BE `I`, CMP `U`, `UI` | ⬜ | LOGIC-005, SCR-001–003 |
| **L-006** | Рейтинг | только ATTENDED; `ALREADY_RATED`; агрегат на listSlots; UI SCR-011 | BE `I`, CMP `I`, `UI` | 🔶 | LOGIC-006, XL-05 |
| **L-007** | Push | register token; deep link waitlist/gym cancel/reminder | `E2E`, CMP `I` | 🚫 | LOGIC-007, XL-09 |
| **L-008** | Состояния экрана | Loading/Empty/Error; offline cache bookings; schedule offline → Error; PTR | CMP `U`+`UI` | ⬜ | LOGIC-008, CMP-08 |

---

## 3. Покрытие по экранам (SCR-001–013)

| SCR | Экран | Что тестируем | Тип | Статус |
|-----|-------|---------------|-----|:------:|
| SCR-001 | Расписание | группировка, чипы, ★, empty, skeleton, PTR, offline=Error | `UI`, CMP `U` | ⬜ |
| SCR-002 | Фильтр дат | выбор дня в окне 7 дней, сброс | `UI` | ⬜ |
| SCR-003 | Фильтры | формат/инструктор, ★ на чипах | `UI` | ⬜ |
| SCR-004 | Деталь слота | bookable/waitlist/rental banner, цена, рейтинг | `UI`, `E2E` | 🔶 |
| SCR-005 | Оформление | inline профиль, equipment, pre-check, gating CTA | `E2E`, `UI` | 🔶 |
| SCR-006 | Успех | итог, push dialog, token upload | `UI`, `E2E` | 🚫 |
| SCR-007 | Ошибка записи | маппинг `ONE_BOOKING_PER_DAY`→SCR-012, остальные→SCR-001/008 | CMP `U`, `UI` | ⬜ |
| SCR-008 | Мои записи | upcoming/past, бейджи, empty, offline cache, PTR | `UI`, CMP `I` | 🔶 |
| SCR-009 | Деталь записи | CTA по статусу, gym cancel reason, leave waitlist, rate | `UI`, `E2E` | 🔶 |
| SCR-010 | Подтверждение отмены | early/late warning, bottom sheet | CMP `U`, `UI` | 🔶 |
| SCR-011 | Оценка | звёзды, submit, 409 flows | `UI`, CMP `I` | 🚫 |
| SCR-012 | Waitlist | join, позиция, leave, NO_SPOTS entry | BE `I`, `UI` | 🔶 |
| SCR-013 | Профиль sheet | summary, edit, бейдж постоянного клиента | `UI` | 🚫 |

---

## 4. Каталог тестов бэкенда

| # | Тест-ID | Сценарий | Тип | Статус | ТЗ / баг |
|---|---------|----------|-----|:------:|----------|
| BE-T01 | `TestProfileAuthIntegration` | PATCH/GET profile + sessionToken | `I` | ✅ | L-001 |
| BE-T02 | `TestListSlotsIntegration` | listSlots + фильтры | `I` | ✅ | L-005 |
| BE-T03 | `TestBookingFlowIntegration` | create booking happy path | `I` | ✅ | FR-005 |
| BE-T04 | `TestJoinWaitlistIntegration` | join waitlist | `I` | ✅ | SCR-012 |
| BE-T05 | `TestOneBookingPerDayAfterWaitlistIntegration` | waitlist утром → book вечером → 409 | `I` | ✅ | XL-01 |
| BE-T06 | `TestRebookForbiddenIntegration` | gym cancel → rebook same slot forbidden | `I` | ✅ | FR-011 |
| BE-T07 | `TestGetBookingIncludesPriceBreakdownIntegration` | price breakdown в ответе | `I` | ✅ | L-003 |
| BE-T08 | `TestEarlyCancelNotifiesWaitlistIntegration` | отмена ≥60 мин → notify + release | `I` | ✅ | XL-02/03 |
| BE-T09 | `TestRatingAfterAttendedIntegration` | rating после ATTENDED | `I` | ✅ | L-006 |
| BE-T10 | `TestIsEarlyCancel` | граница 60 мин (unit) | `U` | ✅ | BE-02 |
| BE-T11 | Router / validate / token / contract | инфраструктура | `U`/`C` | ✅ | — |
| BE-T12 | `TestLateCancelNoRelease` | отмена <60 мин: 200, место не освобождается | `I` | ⬜ | §7.3 п.5 |
| BE-T13 | `TestCancelSlotAlreadyStarted` | `CANCEL_TOO_LATE` / started | `I` | ⬜ | TZ-INT-04 |
| BE-T14 | `TestRentalExhaustedThenEarlyCancel` | исчерпание → отмена → снова доступно | `I` | ⬜ | §7.3 п.3 |
| BE-T15 | `TestSlotsUnavailableOnRentalExhausted` | `slots.status=UNAVAILABLE` | `I` | ⬜ | BE-04 |
| BE-T16 | `TestIsBookablePerRentalItem` | bookable с учётом выбора проката | `U`/`I` | ⬜ | BE-11 |
| BE-T17 | `TestWaitlistConvertedStatus` | CONVERTED после успешной записи | `I` | ⬜ | BE-05 |
| BE-T18 | `TestAlreadyCancelled` | повторная отмена → 409 | `I` | ⬜ | L-004-11 |
| BE-T19 | `TestOneBookingPerDayActivePlusWaitlist` | ACTIVE + waitlist same day | `I` | ⬜ | Q 1.3 |
| BE-T20 | `TestBookingErrorCodes` | все 409-коды create/join | `C` | ⬜ | SCR-007 |
| BE-T21 | `TestRegisterPushToken` | POST push-token | `I` | ⬜ | L-007 |
| BE-T22 | `TestReminderScheduler` | 24h/2h stub → payload | `I` | ⬜ | FR-013 |
| BE-T23 | `TestGymCancelWithReason` | admin cancel + reason в API | `I` | ⬜ | FR-009 |
| BE-T24 | `TestRatingAlreadyRated` | повторный POST → ALREADY_RATED | `I` | ⬜ | §7.3 п.7 |
| BE-T25 | `TestRatingNotAttended` | BOOKING_NOT_ATTENDED | `I` | ⬜ | L-006 |

---

## 5. Каталог тестов клиента (CMP)

| # | Тест-ID | Сценарий | Тип | Статус | Связь |
|---|---------|----------|-----|:------:|-------|
| CMP-T01 | `bookingPrice*` | расчёт цены own/rental | `U` | ✅ | L-003 |
| CMP-T02 | `phoneValidation/Mask` | нормализация телефона | `U` | ✅ | L-001 |
| CMP-T03 | `spotsShortLabel` | метки мест на карточке | `U` | ✅ | L-002 |
| CMP-T04 | `CancelPolicyTest` | early/late warning, canCancel | `U` | ⬜ | L-004 |
| CMP-T05 | `BookingErrorMapperTest` | API code → SCR-007 навигация | `U` | ⬜ | SCR-007, XL-08 |
| CMP-T06 | `SlotFilterPolicyTest` | фильтры дата/период/формат | `U` | ⬜ | L-005 |
| CMP-T07 | `BookingStatusBadgeTest` | бейджи всех статусов | `U` | ⬜ | SCR-008 |
| CMP-T08 | `OfflinePolicyTest` | block cancel/create без сети | `U` | ⬜ | L-004-10, CMP-15 |
| CMP-T09 | `OfflineCacheTest` | read cache, invalidation | `I` | ⬜ | CMP-08 |
| CMP-T10 | `RatingViewModelTest` | submit, ALREADY_RATED | `I` | ⬜ | CMP-01 |
| CMP-T11 | `WaitlistViewModelTest` | join, position, leave | `I` | ⬜ | CMP-06/07 |
| CMP-T12 | `ProfileViewModelTest` | summary, edit, returning badge | `I` | ⬜ | CMP-02 |
| CMP-T13 | `PushTokenRepositoryTest` | register token mock | `I` | 🚫 | CMP-04 |
| CMP-T14 | `DeepLinkRouterTest` | waitlist→SCR-005, gym→SCR-009 | `U` | 🚫 | XL-09 |
| CMP-T15–22 | `*ScreenSnapshotTest` | SCR-001–013 ключевые состояния | `UI` | ⬜ | SCR-* |

---

## 6. Сквозные E2E-сценарии (приоритет из ревью ТЗ §7.3)

| # | Сценарий | Слои | Статус | Блокер |
|---|----------|------|:------:|--------|
| E2E-01 | Waitlist 10:00 → book 18:00 same day → `ONE_BOOKING_PER_DAY` | BE | ✅ | — |
| E2E-02 | Early cancel ровно 60 мин → spot + rental + waitlist notify | BE | ✅ | — |
| E2E-03 | Rental exhausted → early cancel → повторная запись OK | BE | ⬜ | — |
| E2E-04 | Gym cancel → rebook same slot forbidden → other slot OK | BE | ✅ | — |
| E2E-05 | Late cancel <1 ч → warning UI, место может не освободиться | BE+CMP | ⬜ | — |
| E2E-06 | Offline: просмотр кэша брони; cancel/create disabled | CMP | ⬜ | CMP-08 |
| E2E-07 | Rating: ATTENDED only; repeat → ALREADY_RATED | BE ✅ / CMP 🚫 | 🔶 | SCR-011 |
| E2E-08 | Push waitlist → deep link → успешная запись | Full | 🚫 | CMP-04 |
| E2E-09 | Full booking journey: schedule → detail → book → success → my bookings | Full | ⬜ | — |
| E2E-10 | Waitlist journey: full slot → join → leave → re-join | Full | ⬜ | — |
| E2E-11 | Cancel journey: detail → SCR-010 early/late → refresh list | CMP | ⬜ | — |
| E2E-12 | Error journey: NO_SPOTS → SCR-012; RENTAL_UNAVAILABLE → stay | CMP | ⬜ | — |

---

## 7. Матрица «изменение → регрессия»

| Если меняем | Обязательный набор тестов |
|-------------|---------------------------|
| Порог отмены | BE-T08, BE-T10, BE-T12, BE-T13, CMP-T04, E2E-02/05 |
| Лимит 1 запись/день | BE-T05, BE-T19, E2E-01, CMP-T05 |
| Прокат / isBookable | BE-T14–16, L-002 UI, E2E-03 |
| Push / deep links | BE-T21–22, CMP-T13–14, E2E-08 |
| Офлайн | CMP-T08–09, E2E-06, SCR-008/009 UI |
| Рейтинги | BE-T09, BE-T24–25, CMP-T10, SCR-011 UI |
| Коды ошибок API | BE-T20, CMP-T05, contract suite |

---

## 8. Фазы внедрения

| Фаза | Спринт | Фокус | Тесты (~шт.) |
|------|--------|-------|:------------:|
| **F1 — Стабильность BE** | 1 | Инварианты P0/P1 закрепить регрессией | BE-T12–14, BE-T18–19 (~6) |
| **F2 — Контракт и ошибки** | 2 | OpenAPI, SCR-007 маппинг | BE-T20, CMP-T05 (~10) |
| **F3 — MVP клиент** | 3 | Рейтинги, офлайн, push (по готовности фич) | CMP-T08–12, E2E-06–07 (~15) |
| **F4 — UI polish** | 4 | SCR smoke, PTR, фильтры | CMP-T15–22, UI (~20) |
| **F5 — E2E full** | 5 | Сквозные пользовательские пути | E2E-09–12 (~4) |

**Итого план:** ~55 новых тест-кейсов поверх 20 существующих → **~75** на закрытие MVP.

---

## 9. Пробелы, блокирующие полное покрытие

| ID | Что блокирует тесты | Какие тесты ждут |
|----|---------------------|------------------|
| XL-05 / CMP-01 | Нет SCR-011 | CMP-T10, E2E-07 (UI), SCR-011 |
| XL-09 / CMP-04 | Push stub на BE, нет FCM на CMP | E2E-08, CMP-T13–14 |
| CMP-08 | Нет offline cache | E2E-06, CMP-T09 |
| BE-04, BE-05 | Открытые BE-пробелы | BE-T15, BE-T17 |
| TZ-INT-04 | Семантика `CANCEL_TOO_LATE` | BE-T13, CMP-T05 |

---

## Связанные документы

- [TZ_REVIEW_01-analysis.md](TZ_REVIEW_01-analysis.md)
- [../bugs/05-logic-coverage-matrix.md](../bugs/05-logic-coverage-matrix.md)
- [../bugs/06-screen-coverage-matrix.md](../bugs/06-screen-coverage-matrix.md)
- [../bugs/07-priority-fix-list.md](../bugs/07-priority-fix-list.md)
