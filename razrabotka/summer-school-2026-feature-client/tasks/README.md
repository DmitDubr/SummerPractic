# Задание: баги и фичи

**Цель:** найти и исправить **3 бага**, реализовать или доработать **3 фичи** (минимум — 1 баг и 1 фича).

**Commit:** пока не делаем — сначала закрываем документы и ручную проверку по каждой задаче.

## Прогресс: 3/3 бага + 3/3 фичи (код готов, commit — позже)

| ID | Тип | Название | Статус | Документ |
|----|-----|----------|--------|----------|
| BUG-001 | Баг | Web-вход блокируется CORS | ✅ исправлено | [BUG-001-web-auth-cors.md](BUG-001-web-auth-cors.md) |
| BUG-002 | Баг | Docker-сборка API: Go 1.23 vs go.mod 1.25.7 | ✅ исправлено | [BUG-002-docker-go-version.md](BUG-002-docker-go-version.md) |
| BUG-003 | Баг | Web: fetch-ошибка → неверный текст auth | ✅ исправлено | [BUG-003-web-network-error-message.md](BUG-003-web-network-error-message.md) |
| FEAT-001 | Фича | Dev OTP hint на SCR-001 | ✅ реализовано | [FEAT-001-dev-otp-hint.md](FEAT-001-dev-otp-hint.md) |
| FEAT-002 | Фича | Индикатор активных фильтров (BS-001) | ✅ реализовано | [FEAT-002-active-filters-indicator.md](FEAT-002-active-filters-indicator.md) |
| FEAT-003 | Фича | Push после первой брони (LOGIC-007) | ✅ реализовано | [FEAT-003-push-after-first-booking.md](FEAT-003-push-after-first-booking.md) |

## Шаблон документа (баг или фича)

Каждая задача — отдельный `.md` в `tasks/`:

1. **Симптом / цель** — что видит пользователь или что нужно получить.
2. **Требования** — ссылки на `FR-*`, `SCR-*`, `LOGIC-*`, OpenAPI `operationId`, если есть.
3. **Реализация** — что изменено в коде, ключевые файлы.
4. **Промпты** — все отправленные промпты (из чата / Cursor), связанные с задачей.
5. **Ручная проверка** — шаги и ожидаемый результат.
6. **Commit** — сообщение коммита (когда будем коммитить; сейчас **не делали**).

## Ручная проверка (общая)

```powershell
# backend
cd backend
docker compose --profile app up --build -d

# web client
cd client
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

| Задача | Как проверить |
|--------|----------------|
| BUG-001 | Web login, Network → `request-code` 200 |
| BUG-002 | `docker compose --profile app up --build` без ошибки Go |
| BUG-003 | API stopped → «Проверьте соединение» на входе |
| FEAT-001 | OTP hint на шаге кода после «Получить код» |
| FEAT-002 | Точка на иконке фильтров после «Применить» |
| FEAT-003 | Первая бронь → карточка push на «Вы записаны» |

## Связанные изменения в репозитории (ещё без commit)

- `backend/internal/http/middleware.go` — CORS middleware (BUG-001)
- `backend/internal/http/router.go` — подключение CORS (BUG-001)
- `backend/internal/http/infrastructure_test.go` — тест preflight (BUG-001)
- `backend/Dockerfile`, `backend/compose.yaml` — Go 1.25 (BUG-002)
