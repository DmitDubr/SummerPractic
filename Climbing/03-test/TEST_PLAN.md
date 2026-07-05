# План тестирования — скалодром «Вертикаль»

**Дата:** 2026-07-05  
**Источники:** [TZ_REVIEW_01-analysis.md](TZ_REVIEW_01-analysis.md), [TEST_COVERAGE_PLAN.md](TEST_COVERAGE_PLAN.md)

**Уровни:** U — unit · I — integration/API · C — contract · E2E — сквозной · UI — экран  
**Статус:** ✅ есть · 🔶 частично · ⬜ нужно · 🚫 blocked (фича не готова)  
**Приоритет:** P0 — инварианты · P1 — MVP must-have · P2 — полнота · P3 — polish

---

## Каталог тестов

| ID | Область | Сценарий | Уровень | P | Статус |
|----|---------|----------|---------|---|--------|
| **Профиль и сессия** |
| T-001 | L-001 | PATCH/GET profile, sessionToken из booking | I | P1 | ✅ |
| T-002 | L-001 | Валидация имя/телефон, upsert по phone | U/I | P1 | 🔶 |
| T-003 | SCR-013 | Summary, edit, бейдж «Постоянный клиент» | UI/I | P2 | 🚫 |
| T-004 | L-001 | Offline — блок submit профиля/записи | U | P2 | ⬜ |
| **Расписание и фильтры** |
| T-005 | L-005 | listSlots + фильтры (дата, период, формат, инструктор) | I | P1 | ✅ |
| T-006 | L-005 | Пустой результат, окно 7 дней | U/UI | P2 | ⬜ |
| T-007 | SCR-001 | Группировка, чипы, ★, skeleton, PTR, offline=Error | UI/U | P2 | ⬜ |
| T-008 | SCR-002/003 | Фильтр дат, формат/инструктор, ★ на чипах | UI | P3 | ⬜ |
| **Доступность и прокат** |
| T-009 | L-002 | OPEN/FULL → waitlist CTA; bookable → «Записаться» | I/UI | P1 | 🔶 |
| T-010 | L-002 | Rental exhausted → banner, `RENTAL_UNAVAILABLE` | I/UI | P1 | 🔶 |
| T-011 | BE-04 | `slots.status=UNAVAILABLE` при исчерпании проката | I | P2 | ⬜ |
| T-012 | BE-11 | `isBookable` с учётом выбора проката | U/I | P2 | ⬜ |
| T-013 | L-002 | Коды `NO_SPOTS`, `SLOT_CANCELLED` | I/C | P1 | 🔶 |
| **Цена** |
| T-014 | L-003 | Расчёт own / shoes / harness / both | U | P1 | ✅ |
| T-015 | L-003 | Price breakdown в GET booking | I | P1 | ✅ |
| **Запись** |
| T-016 | FR-005 | createBooking happy path | I | P0 | ✅ |
| T-017 | SCR-005 | Inline профиль, equipment, pre-check, gating CTA | E2E/UI | P1 | 🔶 |
| T-018 | SCR-006 | Экран успеха, push dialog, token upload | E2E/UI | P1 | 🚫 |
| T-019 | E2E-09 | Schedule → detail → book → success → my bookings | E2E | P1 | ⬜ |
| **1 запись / день** |
| T-020 | XL-01 | Waitlist утром → book вечером → `ONE_BOOKING_PER_DAY` | I/E2E | P0 | ✅ |
| T-021 | Q 1.3 | ACTIVE + waitlist в один день → 409 | I | P1 | ⬜ |
| T-022 | SCR-007 | Маппинг API code → навигация (`NO_SPOTS`→SCR-012) | U/C | P1 | ⬜ |
| **Waitlist** |
| T-023 | SCR-012 | join waitlist, позиция, leave, re-join | I/E2E | P1 | 🔶 |
| T-024 | BE-05 | Статус `CONVERTED` после успешной записи | I | P2 | ⬜ |
| T-025 | E2E-10 | Full slot → join → leave → re-join | E2E | P2 | ⬜ |
| **Отмена** |
| T-026 | XL-02 | Early cancel ≥60 мин → spot + rental + waitlist notify | I/E2E | P0 | ✅ |
| T-027 | §7.3 п.5 | Late cancel <60 мин: 200, место не освобождается | I | P0 | ⬜ |
| T-028 | TZ-INT-04 | Отмена после start → `CANCEL_TOO_LATE` | I | P1 | ⬜ |
| T-029 | L-004-11 | Повторная отмена → `ALREADY_CANCELLED` | I | P1 | ⬜ |
| T-030 | XL-03 | Rental exhausted → early cancel → снова доступно | I/E2E | P0 | ⬜ |
| T-031 | SCR-010 | Early/late warning, bottom sheet | U/UI | P1 | 🔶 |
| T-032 | E2E-05/11 | Late cancel UI + cancel journey | E2E | P1 | ⬜ |
| T-033 | L-004 | Offline — блок отмены без сети | U | P2 | ⬜ |
| T-034 | BE-02 | Граница ровно 60 мин (`isEarlyCancel`) | U | P0 | ✅ |
| **Gym cancel / rebook** |
| T-035 | FR-011 | Gym cancel → rebook same slot forbidden | I/E2E | P0 | ✅ |
| T-036 | FR-009 | Admin cancel + reason в API, UI SCR-009 | I/UI | P1 | ⬜ |
| **Мои записи** |
| T-037 | SCR-008 | Upcoming/past, бейджи, empty, PTR | UI | P1 | 🔶 |
| T-038 | SCR-009 | CTA по статусу, gym reason, leave waitlist, rate | UI/E2E | P1 | 🔶 |
| T-039 | CMP-08 | Offline cache bookings, invalidation | I/E2E | P1 | ⬜ |
| T-040 | E2E-06 | Offline: просмотр кэша; cancel/create disabled | E2E | P1 | ⬜ |
| **Рейтинг** |
| T-041 | L-006 | Rating только после ATTENDED | I | P1 | ✅ |
| T-042 | §7.3 п.7 | Повторный POST → `ALREADY_RATED` | I | P1 | ⬜ |
| T-043 | L-006 | `BOOKING_NOT_ATTENDED` | I | P1 | ⬜ |
| T-044 | SCR-011 | Звёзды, submit, 409 flows | UI/I | P1 | 🚫 |
| T-045 | E2E-07 | Rating ATTENDED only + repeat error | E2E | P1 | 🔶 |
| T-046 | L-006 | Агрегат ★ на listSlots / SCR-003–004 | UI | P2 | 🔶 |
| **Push и уведомления** |
| T-047 | L-007 | POST push-token | I | P1 | ⬜ |
| T-048 | FR-013 | Reminder scheduler 24h/2h → payload | I | P2 | ⬜ |
| T-049 | XL-09 | Push waitlist → deep link → запись | E2E | P1 | 🚫 |
| T-050 | L-007 | Deep link: waitlist→SCR-005, gym→SCR-009 | U | P1 | 🚫 |
| **Состояния UI** |
| T-051 | L-008 | Loading / Empty / Error на ключевых экранах | U/UI | P2 | ⬜ |
| T-052 | L-008 | Schedule offline → Error (без кэша) | UI | P2 | ✅ |
| T-053 | SCR-007 | Ошибки записи → правильный экран | U/UI | P1 | ⬜ |
| T-054 | E2E-12 | NO_SPOTS→SCR-012; RENTAL_UNAVAILABLE→stay | E2E | P1 | ⬜ |
| **Контракт и инфра** |
| T-055 | BE-10 | Все 409-коды create/join/cancel vs OpenAPI | C | P1 | ⬜ |
| T-056 | — | Router, validate, token (инфраструктура) | U/C | P2 | ✅ |

---

## Сводка

| | Всего | ✅ | 🔶 | ⬜ | 🚫 |
|---|:---:|:---:|:---:|:---:|:---:|
| Тесты | 56 | 11 | 12 | 27 | 6 |

**Фазы:** F1 BE-инварианты (T-027–030, T-021) → F2 контракт/ошибки (T-022, T-055) → F3 MVP клиент (T-033, T-039–045) → F4 UI smoke (T-007–008, T-037, T-051) → F5 E2E пути (T-019, T-025, T-032, T-054)

**Блокеры:** SCR-011/T-044 (рейтинг UI) · Push/FCM T-018, T-049–050 · Offline cache T-039–040 · BE-04/05 T-011, T-024
