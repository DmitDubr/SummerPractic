# LOGIC-005 — Фильтрация классов

**ID:** LOGIC-005  
**Тип:** Логика  
**Приоритет:** High  
**Статус:** Актуален

---

## Обзор

Формирует query-параметры для `listSlots` из фильтров периода ([SCR-002](../../3-design-brief/screens/SCR-002-date-filter.md))
и классов ([SCR-003](../../3-design-brief/screens/SCR-003-class-filters.md)), управляет badge на SCR-001,
сортировкой и группировкой. Фильтр **по шефу не входит** в MVP; фильтр **по типу кухни** — да (FR-003).

---

## Точки применения

| Экран | Элемент / триггер |
| :-- | :-- |
| [SCR-001](../../3-design-brief/screens/SCR-001-schedule.md) | Загрузка списка, pull-to-refresh, empty, badge |
| [SCR-002](../../3-design-brief/screens/SCR-002-date-filter.md) | «Применить» / «Сбросить» — `dateFrom`/`dateTo` |
| [SCR-003](../../3-design-brief/screens/SCR-003-class-filters.md) | «Применить» / «Сбросить» — `cuisineTypeIds`, `timeOfDay`, `level` |

---

## Флоу

```mermaid
flowchart TD
    Start([Триггер]) --> Collect[appliedDateRange + appliedClassFilters]
    Collect --> Build[query listSlots]
    Build --> DateCheck{дефолт 7 дней?}
    DateCheck -->|Да| OmitDates[без dateFrom/dateTo]
    DateCheck -->|Нет| SetDates[dateFrom + dateTo]
    OmitDates --> Request[GET /slots]
    SetDates --> Request
    Request --> Response{items.length?}
    Response -->|> 0| Group[Сортировка + группировка по дням]
    Response -->|= 0| Empty[Empty state]
```

---

## Описание логики

### Параметры `listSlots`

| Параметр | Источник | По умолчанию | Правило |
| :-- | :-- | :-- | :-- |
| `dateFrom`, `dateTo` | SCR-002 | — | Не передаются при дефолте 7 дней (R-027) |
| `cuisineTypeIds` | SCR-003 | все | OR внутри группы; не передаётся при `[]` |
| `timeOfDay` | SCR-003 | все | `morning` / `afternoon` / `evening` |
| `level` | SCR-003 | все | `BEGINNER` / `INTERMEDIATE` / `ADVANCED` |

Справочник типов кухни: `listCuisineTypes` на SCR-003.

### Badge «Фильтры»

Считается число заполненных категорий (0–3):

| Категория | +1 если |
| :-- | :-- |
| Тип кухни | `cuisineTypeIds.length > 0` |
| Время суток | `timeOfDay` задан |
| Уровень | `level` задан |

Период ≠ дефолт — отдельный чип «Период», не в badge.

### Empty states

| Условие | Текст |
| :-- | :-- |
| Пусто, дефолт | «Пока нет доступных классов» (FR-005) |
| Пусто, фильтры активны | «Ничего не найдено» + «Сбросить фильтры» |

### Группировка (клиент)

1. Сортировка: `startsAt` ↑.
2. Группировка по календарному дню.
3. Заголовки: «Сегодня», «Завтра», иначе «День недели, D MMM».

---

## Входные / выходные данные

| Параметр | Тип | Направление | Описание |
| :-- | :-- | :--: | :-- |
| `appliedDateRange` | object | in | Период из SCR-002 |
| `appliedClassFilters` | object | in | Фильтры из SCR-003 |
| `queryParams` | object | out | Для `listSlots` |
| `filterBadgeCount` | int | out | 0–3 |

---

## Связанные требования

| ID | Описание |
| :-- | :-- |
| FR-001–FR-005 | Расписание и фильтры |
| FR-003 | Время, уровень, тип кухни |
| R-027 | Дефолт 7 дней |
| UC-001 | Просмотр расписания |

**API:** [../../api/openapi.yaml](../../api/openapi.yaml) → `listSlots`, `listCuisineTypes`

---

## Критерии приёмки

| ID | Критерий |
| :-- | :-- |
| AC-L-001 | **Дано** «Сбросить» на SCR-002, **Тогда** `dateFrom`/`dateTo` не передаются, API — 7 дней. |
| AC-L-002 | **Дано** выбран тип кухни и `evening`, **Тогда** badge = 2, query содержит оба параметра. |
| AC-L-003 | **Дано** пустой список при фильтрах, **Тогда** empty «Ничего не найдено» + сброс. |
| AC-L-004 | **Дано** непустой список, **Тогда** группировка по дням, сортировка по времени. |
