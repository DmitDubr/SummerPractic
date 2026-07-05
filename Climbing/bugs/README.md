# Расхождения реализации с ТЗ — скалодром «Вертикаль»

> Аудит от 2026-07-05. Сравнение `Climbing/01-analysis/` (ТЗ, LOGIC-001–008, SCR-001–013) с реализацией в `Climbing/backend/` и `Climbing/client/`.

## Сводка

| Слой | Оценка | Критичных (P0) |
|------|--------|----------------|
| ТЗ (внутренние противоречия) | 5 несогласованностей | — (ТЗ не менялось) |
| Бэкенд (Go) | ~95% MVP P0/P1 | 0 открытых |
| Клиент (CMP) | ~70% MVP P0/P1 | 0 открытых |
| Сквозные (ТЗ ↔ BE ↔ CMP) | 9 сценариев | 0 открытых P0/P1 |

**Закрыто 2026-07-05:** BE-01–03, BE-09, CMP-05–07, CMP-09–10, CMP-14, CMP-18, XL-01–04, XL-06–07.

**Остаётся (P2/P3):** рейтинги, push, офлайн-кэш, polish UI, TZ-INT-* (документация).

## Документы

| Файл | Содержание |
|------|------------|
| [01-tz-internal-inconsistencies.md](01-tz-internal-inconsistencies.md) | Противоречия внутри артефактов ТЗ |
| [02-backend-discrepancies.md](02-backend-discrepancies.md) | Бэкенд vs ТЗ |
| [03-client-discrepancies.md](03-client-discrepancies.md) | CMP-клиент vs ТЗ |
| [04-cross-layer-bugs.md](04-cross-layer-bugs.md) | Критичные расхождения на стыке слоёв |
| [05-logic-coverage-matrix.md](05-logic-coverage-matrix.md) | Матрица LOGIC-001–008 |
| [06-screen-coverage-matrix.md](06-screen-coverage-matrix.md) | Матрица SCR-001–013 |
| [07-priority-fix-list.md](07-priority-fix-list.md) | Приоритизированный план исправлений |

## Источники ТЗ

- `01-analysis/5-mobile-app-spec/README.md`
- `01-analysis/5-mobile-app-spec/09_Логики/`
- `01-analysis/3-design-brief/screens/SCR-*.md`
- `01-analysis/api/openapi.yaml`

## Источники реализации

- `backend/internal/store/store.go`, `handler/`, `validate/`
- `client/shared/src/commonMain/kotlin/com/vertical/app/`
- `02-development/CMP_CLIENT_IMPLEMENTATION_REPORT.md`
- `02-development/BE_IMPLEMENTATION_PLAN.md`
