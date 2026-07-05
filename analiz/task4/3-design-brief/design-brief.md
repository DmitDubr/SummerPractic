# Бриф для UI/UX дизайнера — гончарная мастерская «Глина»

> Клиентское мобильное приложение для записи на мастер-классы.
> Этап: **3-design-brief** → передача дизайнеру перед этапом **4-design**.

---

## Контекст продукта

| | |
| :-- | :-- |
| **Заказчик** | Гончарная мастерская «Глина» (Марина) |
| **Пользователь** | Клиент — записывается на занятие с телефона |
| **Скоуп** | Только роль «Клиент» (R-028); админка и интерфейс мастера — вне скоупа |
| **Платформа** | **Android** (первая итерация) |
| **Язык** | Только русский |
| **Оплата в MVP** | На месте; цену показываем в UI (FR-012) |

**Болевая точка:** ручная запись в Instagram-директ + ежедневник → двойные брони на круг, путаница в выходные.
**Цель UX:** самообслуживание записи за 3–4 тапа от открытия приложения.

---

## Входные артефакты

| Артефакт | Путь |
| :-- | :-- |
| Бриф заказчика | [0-customer-brief/brief-pottery.md](../0-customer-brief/brief-pottery.md) |
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
| SCR-001 | Расписание занятий | [screens/SCR-001-schedule.md](screens/SCR-001-schedule.md) |
| SCR-002 | Фильтр периода дат | [screens/SCR-002-date-filter.md](screens/SCR-002-date-filter.md) |
| SCR-003 | Фильтры занятий | [screens/SCR-003-session-filters.md](screens/SCR-003-session-filters.md) |
| SCR-004 | Деталь занятия | [screens/SCR-004-session-detail.md](screens/SCR-004-session-detail.md) |
| SCR-005 | Оформление записи | [screens/SCR-005-booking-form.md](screens/SCR-005-booking-form.md) |
| SCR-006 | Успешная запись | [screens/SCR-006-booking-success.md](screens/SCR-006-booking-success.md) |
| SCR-007 | Ошибка записи | [screens/SCR-007-booking-error.md](screens/SCR-007-booking-error.md) |
| SCR-008 | Мои записи | [screens/SCR-008-my-bookings.md](screens/SCR-008-my-bookings.md) |
| SCR-009 | Деталь записи | [screens/SCR-009-booking-detail.md](screens/SCR-009-booking-detail.md) |
| SCR-010 | Подтверждение отмены | [screens/SCR-010-cancel-confirm.md](screens/SCR-010-cancel-confirm.md) |
| SCR-011 | Оценка мастера | [screens/SCR-011-rate-master.md](screens/SCR-011-rate-master.md) |
| SCR-012 | Контактные данные | [screens/SCR-012-contact-profile.md](screens/SCR-012-contact-profile.md) |

---

## Информационная архитектура

**Нижняя навигация (2 вкладки):**

1. **Расписание** (SCR-001) — default
2. **Мои записи** (SCR-008)

Профиль как отдельная вкладка **не нужен** — контакты в SCR-005 / SCR-012.

**Основные потоки:**

| Поток | Экраны |
| :-- | :-- |
| Запись на занятие (успех) | SCR-001 → SCR-004 → SCR-005 (+ SCR-012) → SCR-006 → SCR-008 или SCR-001 |
| Запись на занятие (ошибка) | SCR-001 → SCR-004 → SCR-005 → SCR-007 → SCR-001 / SCR-005 |
| Отмена записи | SCR-008 → SCR-009 → SCR-010 → SCR-008 |
| Оценка мастера | SCR-008 → SCR-009 → SCR-011 |
| Перезапись после отмены мастерской | Push → SCR-009 → SCR-001 → SCR-004 → SCR-005 |
| Перенос занятия | Push → SCR-009 (info-блок переноса) |

---

## Сквозные UX-требования

| Требование | Детали |
| :-- | :-- |
| Empty states | «Пока нет доступных занятий» на SCR-001 (FR-005) |
| Загрузка | Skeleton на списках; spinner на submit |
| Ошибки сети | Баннер + retry; офлайн-кэш на SCR-008/009 (NFR-009) |
| Push | Только in-app push (NFR-010): напоминания, отмены, перенос |
| Доступность | Touch targets ≥ 44 pt; контраст WCAG AA |
| Статусы брони | Единая система бейджей — см. [screen-registry.md](screen-registry.md) |
| Постоянный клиент | Метка «Постоянный клиент» в профиле (FR-025) |
| Доступность мест | Только «Есть места» / «Мест нет» — без номера круга и счётчика X/Y; условие CTA записи: `freeSpots > 0` |
| Рейтинг мастера | На карточках SCR-001 и SCR-004: `★ X.X` при `ratingCount > 0`; иначе «Пока нет оценок» |
| Прокат исчерпан | На SCR-004/005 — info «со своим»; запись не блокируется при наличии мест (FR-008) |
| Программа в UI | Полное название — `program.name`; бейдж типа — `program.typeName` («Лепка» / «Работа на круге») |
| Поздняя отмена | SCR-010: предупреждение о заготовленной глине (< 3 ч), отмена разрешена (FR-015) |

---

## Вне скоупа дизайна

- Админка владельца / экран правки расписания
- Интерфейс мастера в мастерской
- iOS (вторая платформа)
- Онлайн-оплата
- Лист ожидания
- Аллергии (нет в домене «Глина»)
- Фильтр по мастеру
- Текстовые отзывы
- SMS / email / Instagram

---

## Ожидаемые deliverables от дизайнера

1. User flow diagram по потокам выше
2. Wireframes / hi-fi всех SCR-001–SCR-012
3. UI Kit: цвета, типографика, компоненты (карточка занятия, бейдж статуса, звёзды рейтинга)
4. Состояния: loading, empty, error, offline для SCR-001 и SCR-008
5. Push-notification templates (текст + deep link target)

---

## Следующий этап

После утверждения макетов → [4-design/](../4-design/) (модель данных, API sequence).
