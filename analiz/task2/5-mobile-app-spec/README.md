# ТЗ на мобильное приложение «Шеф-стол»

> Этап 5. Детальное техническое задание на клиентское Android-приложение кулинарной студии
> «Шеф-стол» (самостоятельная запись на кулинарные классы, роль «Клиент», R-028).

**Статус:** Актуален · **Версия:** 1.0 · **Дата:** 2026-07-03

ТЗ детализирует [фича-лист](feature-list.md) до уровня реализации. **Постановки экранов (SCR-001–013)**
с детальным ТЗ (API, логики, AC) — в [3-design-brief/screens/](../3-design-brief/screens/) (этап 5, шаблон
[_SCREEN_TEMPLATE.md](_SCREEN_TEMPLATE.md)). Переиспользуемая логика — в [09_Логики/](09_Логики/_INDEX.md).

**Источники:**
[Фича-лист](feature-list.md) ·
[3-design-brief/](../3-design-brief/) ·
[2-requirements/](../2-requirements/) ·
[4-design/](../4-design/) ·
[customer-questions.md](../1-elicitation/customer-questions.md) ·
[API (OpenAPI)](../api/openapi.yaml)

---

## Экраны

| ID | Экран | Тип | Приоритет | Постановка |
|----|-------|-----|-----------|------------|
| SCR-001 | Расписание классов | Экран | Critical | [SCR-001-schedule.md](../3-design-brief/screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | Bottom Sheet | High | [SCR-002-date-filter.md](../3-design-brief/screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры классов | Bottom Sheet | High | [SCR-003-class-filters.md](../3-design-brief/screens/SCR-003-class-filters.md) |
| SCR-004 | Деталь класса | Экран | Critical | [SCR-004-class-detail.md](../3-design-brief/screens/SCR-004-class-detail.md) |
| SCR-005 | Оформление записи | Экран | Critical | [SCR-005-booking-form.md](../3-design-brief/screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | Экран | High | [SCR-006-booking-success.md](../3-design-brief/screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | Dialog | High | [SCR-007-booking-error.md](../3-design-brief/screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | Экран | Critical | [SCR-008-my-bookings.md](../3-design-brief/screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | Экран | Critical | [SCR-009-booking-detail.md](../3-design-brief/screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | Bottom Sheet | High | [SCR-010-cancel-confirm.md](../3-design-brief/screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка шефа | Bottom Sheet | High | [SCR-011-rate-chef.md](../3-design-brief/screens/SCR-011-rate-chef.md) |
| SCR-012 | Аллергии | Секция / Sheet | High | [SCR-012-allergies.md](../3-design-brief/screens/SCR-012-allergies.md) |
| SCR-013 | Контактные данные | Секция | High | [SCR-013-contact-profile.md](../3-design-brief/screens/SCR-013-contact-profile.md) |

## Переиспользуемые логики

[09_Логики/_INDEX.md](09_Логики/_INDEX.md) — 9 логик. Экраны подключают через «Применяемые логики».

## Соглашения

- **Платформа:** Android (NFR-001); iOS вне MVP.
- **API:** REST; теги OpenAPI: `slots`, `cuisine-types`, `bookings`, `profile`, `allergies`, `ratings`.
- **Числа не хардкодятся:** лимиты мест (8/12 — от шефа), прокатный фонд, цены программ — из API (R-015).
- **Идентификация:** имя + телефон при первой записи (Q 1.1); сессионный токен в `PATCH /profile` и `POST /bookings`; отдельного экрана входа нет.
- **Нижняя навигация:** 2 вкладки — «Расписание» | «Мои записи».
- **Терминология:** **шеф**, **класс**, **программа**; не «инструктор» / «тренировка».
- **Дизайн-макеты:** Figma — TBD (постановки в [3-design-brief/screens/](../3-design-brief/screens/)).

## Статус заполнения

| Блок | Статус |
|------|--------|
| Фича-лист, README, шаблоны | Готово |
| Логики 09_ (9 шт.) | Актуален |
| Постановки экранов SCR-001–013 (детальное ТЗ) | [3-design-brief/screens/](../3-design-brief/screens/) · Актуален |
| OpenAPI (`../api/`) | Готово |
| Figma-макеты | TBD |
