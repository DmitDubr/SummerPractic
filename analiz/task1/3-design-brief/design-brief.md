# Бриф для UI/UX дизайнера — скалодром «Вертикаль»

> Клиентское мобильное приложение для записи на групповые тренировки.
> Этап: **3-design-brief** → передача дизайнеру перед этапом **4-design**.

---

## Контекст продукта

| | |
| :-- | :-- |
| **Заказчик** | Скалодром «Вертикаль» (Оля) |
| **Пользователь** | Клиент — записывается на тренировку с телефона, в т. ч. в зале |
| **Скоуп** | Только роль «Клиент» (R-028); админка и интерфейс инструктора — вне скоупа |
| **Платформа** | Мобильное приложение (iOS / Android) |
| **Язык** | Только русский (Q 9.3) |
| **Оплата в MVP** | На месте; цену показываем в UI (Q 7.1) |

**Болевая точка:** ручная запись в Telegram → двойные брони, путаница в часы пик.
**Цель UX:** самообслуживание записи за 3–4 тапа от открытия приложения.

---

## Входные артефакты

| Артефакт | Путь |
| :-- | :-- |
| Бриф заказчика | [0-customer-brief/brief-climbing.md](../0-customer-brief/brief-climbing.md) |
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
| SCR-001 | Расписание | [screens/SCR-001-schedule.md](screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | [screens/SCR-002-date-filter.md](screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры слотов | [screens/SCR-003-slot-filters.md](screens/SCR-003-slot-filters.md) |
| SCR-004 | Деталь слота | [screens/SCR-004-slot-detail.md](screens/SCR-004-slot-detail.md) |
| SCR-005 | Оформление записи | [screens/SCR-005-booking-form.md](screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | [screens/SCR-006-booking-success.md](screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | [screens/SCR-007-booking-error.md](screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | [screens/SCR-008-my-bookings.md](screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | [screens/SCR-009-booking-detail.md](screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | [screens/SCR-010-cancel-confirm.md](screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка инструктора | [screens/SCR-011-rate-instructor.md](screens/SCR-011-rate-instructor.md) |
| SCR-012 | Лист ожидания | [screens/SCR-012-waitlist.md](screens/SCR-012-waitlist.md) |
| SCR-013 | Контактные данные | [screens/SCR-013-contact-profile.md](screens/SCR-013-contact-profile.md) |

---

## Информационная архитектура

**Нижняя навигация (2 вкладки):**

1. **Расписание** (SCR-001) — default
2. **Мои записи** (SCR-008)

Профиль как отдельная вкладка **не нужен** — контактные данные встроены в SCR-005 / SCR-013.

**Основные потоки:**

| Поток | Экраны |
| :-- | :-- |
| Запись на тренировку | SCR-001 → SCR-004 → SCR-005 → SCR-006 |
| Лист ожидания | SCR-004 → SCR-012 → SCR-008 |
| Отмена записи | SCR-008 → SCR-009 → SCR-010 → SCR-008 |
| Оценка инструктора | SCR-008 → SCR-009 → SCR-011 |
| Перезапись после отмены скалодромом | Push → SCR-009 → SCR-001 → SCR-004 |

---

## Сквозные UX-требования

| Требование | Детали |
| :-- | :-- |
| Empty states | Текст «Пока нет доступных тренировок» на SCR-001 (FR-004) |
| Загрузка | Skeleton на списках; spinner на submit |
| Ошибки сети | Баннер + retry; офлайн-кэш на SCR-008/009 (Q 9.2) |
| Push | Только in-app push (Q 6.2): напоминания, отмены, место в листе ожидания (Q 6.1) |
| Доступность | Touch targets ≥ 44 pt; контраст текста WCAG AA |
| Статусы брони | Единая цветовая система бейджей — см. [screen-registry.md](screen-registry.md) |

---

## Вне скоупа дизайна

- Админка владельца / расписание / профилактика
- Интерфейс инструктора
- Онлайн-оплата (backlog)
- Штрафы за позднюю отмену (вторая итерация)
- Фильтр по формату болдеринг / трассы (Q 1.6)

---

## Ожидаемые deliverables от дизайнера

1. User flow diagram по потокам выше
2. Wireframes / hi-fi всех SCR-001–SCR-013
3. UI Kit: цвета, типографика, компоненты (карточка слота, бейдж статуса, звёзды рейтинга)
4. Состояния: loading, empty, error, offline для SCR-001 и SCR-008
5. Push-notification templates (текст + deep link target)

---

## Следующий этап

После утверждения макетов → [4-design/](../4-design/) (модель данных, API sequence).
