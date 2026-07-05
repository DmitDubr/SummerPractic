# ТЗ на мобильное приложение «Апекс»

> Этап 5. Детальное техническое задание на клиентское iOS-приложение картинг-центра
> «Апекс» (самостоятельная запись на заезды, роль «Клиент», R-028).

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
| SCR-001 | Расписание заездов | Экран | Critical | [SCR-001-schedule.md](../3-design-brief/screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | Bottom Sheet | High | [SCR-002-date-filter.md](../3-design-brief/screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры заездов | Bottom Sheet | High | [SCR-003-heat-filters.md](../3-design-brief/screens/SCR-003-heat-filters.md) |
| SCR-004 | Деталь заезда | Экран | Critical | [SCR-004-heat-detail.md](../3-design-brief/screens/SCR-004-heat-detail.md) |
| SCR-005 | Оформление записи | Экран | Critical | [SCR-005-booking-form.md](../3-design-brief/screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | Экран | High | [SCR-006-booking-success.md](../3-design-brief/screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | Dialog | High | [SCR-007-booking-error.md](../3-design-brief/screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | Экран | Critical | [SCR-008-my-bookings.md](../3-design-brief/screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | Экран | Critical | [SCR-009-booking-detail.md](../3-design-brief/screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | Bottom Sheet | High | [SCR-010-cancel-confirm.md](../3-design-brief/screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка маршала | Bottom Sheet | Should (v2) | [SCR-011-rate-marshal.md](../3-design-brief/screens/SCR-011-rate-marshal.md) |
| SCR-013 | Контактные данные | Секция | High | [SCR-013-contact-profile.md](../3-design-brief/screens/SCR-013-contact-profile.md) |

## Переиспользуемые логики

[09_Логики/_INDEX.md](09_Логики/_INDEX.md) — 8 логик. Экраны подключают через «Применяемые логики».

## Соглашения

- **Платформа:** iOS (NFR-001); Android вне MVP.
- **API:** REST; теги OpenAPI: `slots`, `marshals`, `bookings`, `profile`, `ratings`.
- **Числа не хардкодятся:** лимиты участников (8 на новичковый — от типа слота), прокатный фонд, цены конфигурации трассы — из API (R-015).
- **Идентификация:** имя + телефон при первой записи (Q 1.1); сессионный токен в `PATCH /profile` и `POST /bookings`; отдельного экрана входа нет.
- **Нижняя навигация:** 2 вкладки — «Расписание» | «Мои записи».
- **Терминология:** **маршал**, **заезд**, **конфигурация трассы**; экипировка — шлем, подшлемник.
- **Доступность слота:** только «есть места» / «мест нет» (`hasSpots`), без счётчика (Q 2.6).
- **Дизайн-макеты:** Figma — TBD (постановки в [3-design-brief/screens/](../3-design-brief/screens/)).

## Статус заполнения

| Блок | Статус |
|------|--------|
| Фича-лист, README, шаблоны | Готово |
| Логики 09_ (8 шт.) | Актуален |
| Постановки экранов SCR-001–013 (детальное ТЗ) | Актуален · [3-design-brief/screens/](../3-design-brief/screens/) |
| OpenAPI (`../api/`) | Готово |
| Figma-макеты | TBD |
