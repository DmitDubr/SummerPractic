# ТЗ на мобильное приложение «Вертикаль»

> Этап 5. Детальное техническое задание на клиентское мобильное приложение скалодрома
> «Вертикаль» (самостоятельная запись на групповые тренировки, роль «Клиент», R-028).

**Статус:** Актуален · **Версия:** 1.0 · **Дата:** 2026-07-03

ТЗ детализирует [фича-лист](feature-list.md) до уровня реализации. **Постановки экранов (SCR-001–013)**
— единственный источник в [3-design-brief/screens/](../3-design-brief/screens/). Переиспользуемая
логика описана по [_LOGIC_TEMPLATE.md](_LOGIC_TEMPLATE.md) в [09_Логики/](09_Логики/_INDEX.md).

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
| SCR-001 | Расписание | Экран | Critical | [SCR-001-schedule.md](../3-design-brief/screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | Bottom Sheet | High | [SCR-002-date-filter.md](../3-design-brief/screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры слотов | Bottom Sheet | High | [SCR-003-slot-filters.md](../3-design-brief/screens/SCR-003-slot-filters.md) |
| SCR-004 | Деталь слота | Экран | Critical | [SCR-004-slot-detail.md](../3-design-brief/screens/SCR-004-slot-detail.md) |
| SCR-005 | Оформление записи | Экран | Critical | [SCR-005-booking-form.md](../3-design-brief/screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | Экран | High | [SCR-006-booking-success.md](../3-design-brief/screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | Dialog | High | [SCR-007-booking-error.md](../3-design-brief/screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | Экран | Critical | [SCR-008-my-bookings.md](../3-design-brief/screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | Экран | Critical | [SCR-009-booking-detail.md](../3-design-brief/screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | Bottom Sheet | High | [SCR-010-cancel-confirm.md](../3-design-brief/screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка инструктора | Bottom Sheet | High | [SCR-011-rate-instructor.md](../3-design-brief/screens/SCR-011-rate-instructor.md) |
| SCR-012 | Лист ожидания | Экран | High | [SCR-012-waitlist.md](../3-design-brief/screens/SCR-012-waitlist.md) |
| SCR-013 | Контактные данные | Секция | High | [SCR-013-contact-profile.md](../3-design-brief/screens/SCR-013-contact-profile.md) |

## Переиспользуемые логики

[09_Логики/_INDEX.md](09_Логики/_INDEX.md) — 8 логик. Экраны подключают через «Применяемые логики».

## Соглашения

- **Платформа:** нативное мобильное приложение (iOS + Android).
- **API:** REST; домены по тегам OpenAPI: `slots`, `instructors`, `bookings`, `waitlist`, `profile`, `ratings`.
- **Числа не хардкодятся:** лимиты мест, прокатный фонд, цены — из API (R-015).
- **Идентификация:** имя + телефон при первой записи (Q 1.1); сессионный токен выдаётся в `PATCH /profile` и `POST /bookings`; отдельного экрана входа в MVP нет.
- **Нижняя навигация:** 2 вкладки — «Расписание» | «Мои записи».
- **Дизайн-макеты:** Figma — TBD (постановки в [3-design-brief/screens/](../3-design-brief/screens/)).

## Статус заполнения

| Блок | Статус |
|------|--------|
| Фича-лист, README, шаблоны | Готово |
| Логики 09_ (8 шт.) | Актуален |
| Постановки экранов SCR-001–013 | [3-design-brief/screens/](../3-design-brief/screens/) |
| OpenAPI (`../api/`) | Готово |
| Figma-макеты | TBD |
