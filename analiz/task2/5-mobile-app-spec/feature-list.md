# Фича-лист — мобильное приложение «Шеф-стол»

> **Этап 5.** Перечень экранов клиентского приложения и функций на них.
> Связующий артефакт между [требованиями](../2-requirements/) и детальным ТЗ.

**Статус:** Актуален · **Версия:** 1.0 · **Дата:** 2026-07-03

---

## 1. Назначение

**«Шеф-стол»** — клиентское Android-приложение для самостоятельной записи на кулинарные классы.
Заменяет ручную запись через WhatsApp и Google-таблицу, устраняя двойные брони.

**Скоуп — только роль «Клиент»** (R-028). Владелец и шеф работают через существующую инфраструктуру.
Справочные данные (слоты, программы, шефы, типы кухни) — **read-only** из API. Оплата —
**на месте**; приложение показывает цену программы и фиксирует запись.

**Источники:**
[brief-cooking.md](../0-customer-brief/brief-cooking.md) ·
[2-requirements/](../2-requirements/) ·
[3-design-brief/](../3-design-brief/) ·
[4-design/](../4-design/) ·
[customer-questions.md](../1-elicitation/customer-questions.md)

---

## 2. Глоссарий

| Термин | Значение |
|--------|----------|
| **Слот / класс** | Кулинарное занятие: дата, время, программа, шеф, места, цена |
| **Программа** | Тема класса, краткое меню, уровень, тип кухни; цена — от программы |
| **Шеф** | Ведущий класса; лимит группы **8 или 12** — настройка шефа (Q 2.1) |
| **Бронь** | Запись клиента на класс: статус, экипировка, аллергии |
| **Прокат** | Фартук и/или набор ножей из фонда студии; **не влияет на цену** (FR-015) |
| **Ранняя отмена** | ≥ 3 ч до начала → место освобождается сразу (FR-017) |
| **Поздняя отмена** | < 3 ч до начала → предупреждение о продуктах; отмена разрешена, штрафов нет (FR-018) |

> **Принцип:** лимиты мест, прокатный фонд и цены **не хардкодятся** — приходят из API (R-015).

---

## 3. Карта навигации

```mermaid
flowchart TD
    Start([Запуск]) --> SCR001[SCR-001 Расписание]

    SCR001 -->|Чип «Период»| SCR002[SCR-002 Фильтр дат]
    SCR001 -->|Чип «Фильтры»| SCR003[SCR-003 Фильтры классов]
    SCR002 --> SCR001
    SCR003 --> SCR001

    SCR001 -->|Тап класс| SCR004[SCR-004 Деталь класса]
    SCR004 -->|Записаться| SCR005[SCR-005 Оформление записи]
    SCR005 -->|Успех| SCR006[SCR-006 Успех записи]
    SCR005 -->|Ошибка| SCR007[SCR-007 Ошибка записи]
    SCR005 -.-> SCR012[SCR-012 Аллергии]
    SCR005 -.-> SCR013[SCR-013 Контакты]

    SCR001 -->|Таб| SCR008[SCR-008 Мои записи]
    SCR008 -->|Тап бронь| SCR009[SCR-009 Деталь записи]
    SCR009 -->|Отменить| SCR010[SCR-010 Подтверждение отмены]
    SCR009 -->|Оценить| SCR011[SCR-011 Оценка шефа]
    SCR009 --> SCR012

    SCR006 --> SCR008
    SCR006 --> SCR001
    SCR007 --> SCR001

    Push -->|deep link| SCR009
    Push -->|запрос аллергий| SCR009
```

---

## 4. Инвентарь экранов

| ID | Экран | Тип | Приоритет | Постановка |
|----|-------|-----|-----------|------------|
| SCR-001 | Расписание классов | Экран (вкладка) | Critical | [SCR-001-schedule.md](../3-design-brief/screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | Bottom Sheet | High | [SCR-002-date-filter.md](../3-design-brief/screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры классов | Bottom Sheet | High | [SCR-003-class-filters.md](../3-design-brief/screens/SCR-003-class-filters.md) |
| SCR-004 | Деталь класса | Экран | Critical | [SCR-004-class-detail.md](../3-design-brief/screens/SCR-004-class-detail.md) |
| SCR-005 | Оформление записи | Экран | Critical | [SCR-005-booking-form.md](../3-design-brief/screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | Экран | High | [SCR-006-booking-success.md](../3-design-brief/screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | Dialog | High | [SCR-007-booking-error.md](../3-design-brief/screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | Экран (вкладка) | Critical | [SCR-008-my-bookings.md](../3-design-brief/screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | Экран | Critical | [SCR-009-booking-detail.md](../3-design-brief/screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | Bottom Sheet | High | [SCR-010-cancel-confirm.md](../3-design-brief/screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка шефа | Bottom Sheet | High | [SCR-011-rate-chef.md](../3-design-brief/screens/SCR-011-rate-chef.md) |
| SCR-012 | Аллергии | Секция / Sheet | High | [SCR-012-allergies.md](../3-design-brief/screens/SCR-012-allergies.md) |
| SCR-013 | Контактные данные | Секция / Sheet | High | [SCR-013-contact-profile.md](../3-design-brief/screens/SCR-013-contact-profile.md) |

---

## 5. Сквозные функции

- **Push-уведомления** (FR-027, NFR-010): напоминания, подтверждение, отмена, перенос, запрос аллергий
- **Офлайн-кэш** «Мои записи» (NFR-009): SCR-008, SCR-009
- **Паттерн состояний** [LOGIC-008](09_Логики/LOGIC-008_Паттерн-состояний-экрана.md): Loading → Content → Empty → Error → Offline → Refreshing
- **Аллергии** [LOGIC-009](09_Логики/LOGIC-009_Аллергии.md): обязательный шаг записи, проверка меню (FR-012–FR-014)
- **Только русский язык** (NFR-008)

---

## 6. Не входит в MVP

| Функция | Источник |
|---------|----------|
| Лист ожидания | FR-011, backlog |
| Фильтр по шефу | design-brief, backlog |
| Онлайн-оплата | domain §6 |
| iOS | NFR-001, backlog |
| Текстовые отзывы | FR-024 |
| SMS / email / WhatsApp | NFR-010 |
| Штрафы за позднюю отмену | FR-018, backlog |
| Админка / интерфейс шефа | R-028 |

---

## 7. Трассировка требований → экраны

| Требование | Экран |
|------------|-------|
| FR-001–005 | SCR-001 |
| FR-002 | SCR-002 |
| FR-003 | SCR-003 |
| FR-004, FR-015, FR-026 | SCR-004 |
| FR-006–FR-015, FR-028 | SCR-005 → SCR-006 / SCR-007 |
| FR-012–FR-014 | SCR-012, SCR-005, SCR-009 |
| FR-016 | SCR-008, SCR-009 |
| FR-017–FR-018 | SCR-009, SCR-010 |
| FR-019–FR-023 | SCR-009 (push + статус) |
| FR-024–FR-026 | SCR-011 |
| FR-027 | SCR-006, SCR-009 |
| Q 1.1 | SCR-005, SCR-013 |
| UC-001 | SCR-001 → SCR-004 → SCR-005 → SCR-006 |
| UC-002 | SCR-005 → SCR-006 / SCR-007 |
| UC-003 | SCR-008 → SCR-009 |
| UC-004 | SCR-009 → SCR-010 |
| UC-007 | SCR-011 |
| UC-009 | SCR-012, SCR-009 |

---

## 8. API контракт

Спецификация Client API: [openapi.yaml](../api/openapi.yaml) (версия 1.0.0).

Базовый URL: `https://api.chef-stol.example/v1`. Идентификация — сессионный `Bearer`-токен
(`ClientSession`), выдаётся в ответах `PATCH /profile` и `POST /bookings`.

### Эндпоинты

| operationId | Метод | Путь | Tag | Экран(ы) | Назначение |
|-------------|-------|------|-----|----------|------------|
| `listSlots` | GET | `/slots` | slots | SCR-001 | Список классов с фильтрами |
| `getSlot` | GET | `/slots/{slotId}` | slots | SCR-004, SCR-005 | Детали класса, pre-check |
| `listCuisineTypes` | GET | `/cuisine-types` | cuisine-types | SCR-003 | Справочник типов кухни |
| `listBookings` | GET | `/bookings` | bookings | SCR-008 | Список броней клиента |
| `createBooking` | POST | `/bookings` | bookings | SCR-005 | Создание брони (+ upsert профиля и аллергий) |
| `getBooking` | GET | `/bookings/{bookingId}` | bookings | SCR-009, SCR-011 | Детали брони, deep link |
| `updateBookingAllergies` | PATCH | `/bookings/{bookingId}` | bookings | SCR-009, SCR-012 | Изменение аллергий (FR-013) |
| `cancelBooking` | POST | `/bookings/{bookingId}/cancel` | bookings | SCR-010 | Отмена брони клиентом |
| `getProfile` | GET | `/profile` | profile | SCR-005, SCR-013 | Профиль и сохранённые аллергии |
| `updateProfile` | PATCH | `/profile` | profile | SCR-013, SCR-005 | Upsert контактов и аллергий |
| `registerPushToken` | POST | `/profile/push-token` | profile | SCR-006 | Регистрация FCM-токена |
| `checkAllergyCompatibility` | POST | `/allergies/check` | allergies | SCR-005, SCR-012 | Баннер несовместимости (FR-014) |
| `createOrUpdateChefRating` | POST | `/ratings` | ratings | SCR-011 | Оценка шефа (upsert) |
