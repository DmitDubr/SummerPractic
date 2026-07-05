# Бриф для UI/UX дизайнера — кулинарная студия «Шеф-стол»

> Клиентское мобильное приложение для записи на кулинарные классы.
> Этап: **3-design-brief** → передача дизайнеру перед этапом **4-design**.

---

## Контекст продукта

| | |
| :-- | :-- |
| **Заказчик** | Кулинарная студия «Шеф-стол» (Артём) |
| **Пользователь** | Клиент — записывается на класс с телефона |
| **Скоуп** | Только роль «Клиент» (R-028); админка и интерфейс шефа — вне скоупа |
| **Платформа** | **Android** (первая итерация) |
| **Язык** | Только русский |
| **Оплата в MVP** | На месте; цену показываем в UI (FR-015) |

**Болевая точка:** ручная запись в WhatsApp + Google-таблица → двойные брони, путаница в выходные.
**Цель UX:** самообслуживание записи за 3–4 тапа от открытия приложения.

---

## Входные артефакты

| Артефакт | Путь |
| :-- | :-- |
| Бриф заказчика | [0-customer-brief/brief-cooking.md](../0-customer-brief/brief-cooking.md) |
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
| SCR-001 | Расписание классов | [screens/SCR-001-schedule.md](screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | [screens/SCR-002-date-filter.md](screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры классов | [screens/SCR-003-class-filters.md](screens/SCR-003-class-filters.md) |
| SCR-004 | Деталь класса | [screens/SCR-004-class-detail.md](screens/SCR-004-class-detail.md) |
| SCR-005 | Оформление записи | [screens/SCR-005-booking-form.md](screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | [screens/SCR-006-booking-success.md](screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | [screens/SCR-007-booking-error.md](screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | [screens/SCR-008-my-bookings.md](screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | [screens/SCR-009-booking-detail.md](screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | [screens/SCR-010-cancel-confirm.md](screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка шефа | [screens/SCR-011-rate-chef.md](screens/SCR-011-rate-chef.md) |
| SCR-012 | Аллергии | [screens/SCR-012-allergies.md](screens/SCR-012-allergies.md) |
| SCR-013 | Контактные данные | [screens/SCR-013-contact-profile.md](screens/SCR-013-contact-profile.md) |

---

## Информационная архитектура

**Нижняя навигация (2 вкладки):**

1. **Расписание** (SCR-001) — default
2. **Мои записи** (SCR-008)

Профиль как отдельная вкладка **не нужен** — контакты в SCR-005 / SCR-013; аллергии — SCR-012 / SCR-005.

**Основные потоки:**

| Поток | Экраны |
| :-- | :-- |
| Запись на класс | SCR-001 → SCR-004 → SCR-005 (+ SCR-012, SCR-013) → SCR-006 |
| Отмена записи | SCR-008 → SCR-009 → SCR-010 → SCR-008 |
| Оценка шефа | SCR-008 → SCR-009 → SCR-011 |
| Перезапись после отмены студией | Push → SCR-009 → SCR-001 → SCR-004 |
| Перенос класса | Push → SCR-009 |

---

## Сквозные UX-требования

| Требование | Детали |
| :-- | :-- |
| Empty states | «Пока нет доступных классов» на SCR-001 (FR-005) |
| Загрузка | Skeleton на списках; spinner на submit |
| Ошибки сети | Баннер + retry; офлайн-кэш на SCR-008/009 (NFR-009) |
| Push | Только in-app push (NFR-010): напоминания, отмены, аллергии, перенос |
| Доступность | Touch targets ≥ 44 pt; контраст WCAG AA |
| Статусы брони | Единая система бейджей — см. [screen-registry.md](screen-registry.md) |
| Постоянный клиент | Метка «Постоянный клиент» в профиле (FR-028) |

---

## Вне скоупа дизайна

- Админка владельца / экран правки расписания
- Интерфейс шефа на кухне
- iOS (вторая платформа)
- Онлайн-оплата
- Лист ожидания
- Фильтр по шефу
- Текстовые отзывы
- SMS / email / WhatsApp

---

## Ожидаемые deliverables от дизайнера

1. User flow diagram по потокам выше
2. Wireframes / hi-fi всех SCR-001–SCR-013
3. UI Kit: цвета, типографика, компоненты (карточка класса, бейдж статуса, звёзды рейтинга, поле аллергий)
4. Состояния: loading, empty, error, offline для SCR-001 и SCR-008
5. Push-notification templates (текст + deep link target)

---

## Следующий этап

После утверждения макетов → [4-design/](../4-design/) (модель данных, API sequence).
