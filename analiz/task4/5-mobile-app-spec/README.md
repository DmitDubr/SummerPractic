# ТЗ на мобильное приложение «Глина»

> Этап 5. Детальное техническое задание на клиентское Android-приложение гончарной мастерской
> «Глина» (самостоятельная запись на мастер-классы, роль «Клиент», R-028).

**Статус:** Актуален · **Версия:** 1.0 · **Дата:** 2026-07-03

ТЗ детализирует [фича-лист](feature-list.md) до уровня реализации. **Постановки экранов (SCR-001–012)**
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
| SCR-001 | Расписание занятий | Экран (вкладка) | Must | [SCR-001-schedule.md](../3-design-brief/screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | Bottom Sheet | Must | [SCR-002-date-filter.md](../3-design-brief/screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры занятий | Bottom Sheet | Must | [SCR-003-session-filters.md](../3-design-brief/screens/SCR-003-session-filters.md) |
| SCR-004 | Деталь занятия | Экран | Must | [SCR-004-session-detail.md](../3-design-brief/screens/SCR-004-session-detail.md) |
| SCR-005 | Оформление записи | Экран | Must | [SCR-005-booking-form.md](../3-design-brief/screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | Экран | Must | [SCR-006-booking-success.md](../3-design-brief/screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | Dialog | Must | [SCR-007-booking-error.md](../3-design-brief/screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | Экран (вкладка) | Must | [SCR-008-my-bookings.md](../3-design-brief/screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | Экран | Must | [SCR-009-booking-detail.md](../3-design-brief/screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | Bottom Sheet | Must | [SCR-010-cancel-confirm.md](../3-design-brief/screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка мастера | Bottom Sheet | Must | [SCR-011-rate-master.md](../3-design-brief/screens/SCR-011-rate-master.md) |
| SCR-012 | Контактные данные | Секция / Sheet | Must | [SCR-012-contact-profile.md](../3-design-brief/screens/SCR-012-contact-profile.md) |

## Переиспользуемые логики

[09_Логики/_INDEX.md](09_Логики/_INDEX.md) — 9 логик. Экраны подключают через «Применяемые логики».

## Соглашения

- **Платформа:** Android (NFR-001); iOS вне MVP.
- **Push:** FCM в MVP (FR-024, NFR-010); `registerPushToken` с `platform: android`; deep links на SCR-009 и SCR-001.
- **API:** REST; теги OpenAPI: `slots`, `programs`, `bookings`, `profile`, `ratings`.
- **Числа не хардкодятся:** лимиты групп (до 6 на новичковые — от мастера/программы), прокатный фонд, цены программ — из API (R-015).
- **Идентификация:** имя + телефон при первой записи (Q 1.1); сессионный токен в `PATCH /profile` и `POST /bookings`; отдельного экрана входа нет.
- **Нижняя навигация:** 2 вкладки — «Расписание» | «Мои записи».
- **Терминология:** **мастер**, **занятие**, **программа** (лепка / круг); экипировка — инструменты, фартук (OWN / RENTAL).
- **Доступность слота:** только «есть места» / «мест нет» (`freeSpots > 0`), без счётчика X/Y.
- **Экипировка:** прокат не влияет на цену; при `rentalFullyExhausted` — только «со своим» (LOGIC-009, FR-008).
- **Дизайн-макеты:** Figma — TBD (постановки в [3-design-brief/screens/](../3-design-brief/screens/)).

## Статус заполнения

| Блок | Статус |
|------|--------|
| Фича-лист, README, шаблоны | Готово |
| Логики 09_ (9 шт.) | Актуален |
| Постановки экранов SCR-001–012 (детальное ТЗ) | Актуален · [3-design-brief/screens/](../3-design-brief/screens/) |
| OpenAPI (`../api/`) | Готово |
| Figma-макеты | TBD |
