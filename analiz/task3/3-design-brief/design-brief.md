# Бриф для UI/UX дизайнера — картинг-центр «Апекс»

> Клиентское мобильное приложение для записи на заезды.
> Этап: **3-design-brief** → передача дизайнеру перед этапом **4-design**.

---

## Контекст продукта

| | |
| :-- | :-- |
| **Заказчик** | Картинг-центр «Апекс» (Денис) |
| **Пользователь** | Клиент — записывается на заезд с телефона, у трассы |
| **Скоуп** | Только роль «Клиент» (R-028); админка и интерфейс маршала — вне скоупа |
| **Платформа** | **iOS** (MVP v1) |
| **Язык** | Только русский |
| **Оплата в MVP** | На месте; цену показываем в UI (FR-013) |

**Болевая точка:** ручная запись в Telegram + доска маркером → двойные брони, путаница в выходные.
**Цель UX:** самообслуживание записи за 3–4 тапа от открытия приложения.

---

## Входные артефакты

| Артефакт | Путь |
| :-- | :-- |
| Бриф заказчика | [0-customer-brief/brief-karting.md](../0-customer-brief/brief-karting.md) |
| Домен | [1-elicitation/domain-description.md](../1-elicitation/domain-description.md) |
| Q&A заказчика | [1-elicitation/customer-questions.md](../1-elicitation/customer-questions.md) |
| Бизнес-требования | [2-requirements/business-requirements.md](../2-requirements/business-requirements.md) |
| Функциональные требования | [2-requirements/functional-requirements.md](../2-requirements/functional-requirements.md) |
| Нефункциональные требования | [2-requirements/non-functional-requirements.md](../2-requirements/non-functional-requirements.md) |
| User Stories | [2-requirements/user-stories.md](../2-requirements/user-stories.md) |
| Use Cases | [2-requirements/use-cases.md](../2-requirements/use-cases.md) |

---

## Реестр экранов

Полный реестр с навигационной схемой: **[screen-registry.md](screen-registry.md)**

| ID | Экран | Постановка |
| :- | :-- | :-- |
| SCR-001 | Расписание заездов | [screens/SCR-001-schedule.md](screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | [screens/SCR-002-date-filter.md](screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры заездов | [screens/SCR-003-heat-filters.md](screens/SCR-003-heat-filters.md) |
| SCR-004 | Деталь заезда | [screens/SCR-004-heat-detail.md](screens/SCR-004-heat-detail.md) |
| SCR-005 | Оформление записи | [screens/SCR-005-booking-form.md](screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | [screens/SCR-006-booking-success.md](screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | [screens/SCR-007-booking-error.md](screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | [screens/SCR-008-my-bookings.md](screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | [screens/SCR-009-booking-detail.md](screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | [screens/SCR-010-cancel-confirm.md](screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка маршала | [screens/SCR-011-rate-marshal.md](screens/SCR-011-rate-marshal.md) |
| SCR-013 | Контактные данные | [screens/SCR-013-contact-profile.md](screens/SCR-013-contact-profile.md) |

---

## Информационная архитектура

**Нижняя навигация (2 вкладки):**

1. **Расписание** (SCR-001) — default
2. **Мои записи** (SCR-008)

Профиль как отдельная вкладка **не нужен** — контакты в SCR-005 / SCR-013.

**Основные потоки:**

| Поток | Экраны |
| :-- | :-- |
| Запись на заезд | SCR-001 → SCR-004 → SCR-005 (+ SCR-013) → SCR-006 |
| Отмена записи | SCR-008 → SCR-009 → SCR-010 → SCR-008 |
| Оценка маршала (v2) | SCR-008 → SCR-009 → SCR-011 |
| Перезапись после отмены центром (v2) | Push → SCR-001 → SCR-004 |
| Перенос заезда (v2) | Push / SMS → SCR-009 |

---

## Сквозные UX-требования

| Требование | Детали |
| :-- | :-- |
| Empty states | «Пока нет доступных заездов» на SCR-001 (FR-005) |
| Доступность слота | «Есть места» / «Мест нет» — без точного числа картов |
| Загрузка | Skeleton на списках; spinner на submit |
| Ошибки сети | Баннер + retry; офлайн-кэш на SCR-008/009 (NFR-009) |
| Push / SMS (v2) | Напоминание за 2 ч, отмены, перенос (FR-029, NFR-010) |
| Доступность | Touch targets ≥ 44 pt; контраст WCAG AA |
| Терминология | **Заезд**, **маршал**, **конфигурация трассы** — не «класс» / «инструктор» |
| Статусы брони | Единая система бейджей — см. [screen-registry.md](screen-registry.md) |

---

## MVP v1 vs v2

| MVP v1 (Must) | v2 (Should) |
| :-- | :-- |
| SCR-001–010, SCR-013 | SCR-011 (оценка маршала) |
| Запись, мои записи, отмена | Push + SMS, рейтинги на карточках |
| Метка постоянного клиента | Перенос заезда в UI |

---

## Вне скоупа дизайна

- Админка владельца / расписание / погода
- Интерфейс маршала
- Онлайн-оплата
- Лист ожидания
- Аллергии
- Android (вторая платформа)
- Штрафы за позднюю отмену
- Карты с ограничением скорости в UI (назначает маршал на месте)

---

## Рекомендации по визуальному стилю

- Контекст: **уличный картинг**, динамика, скорость; не «ресторан» и не «спортзал»
- Акцент на **времени старта** и **маршале** (фильтр по маршалу в MVP)
- Погода — только через статус отмены центром, без отдельного погодного виджета
- iOS Human Interface Guidelines; native navigation patterns
