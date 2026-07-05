# Backend API — «Вертикаль»

Client API для мобильного приложения скалодрома.

## Требования

- Go 1.23+
- Docker (для PostgreSQL и полного стека)

## Быстрый старт

```bash
cp .env.example .env
docker compose up --build
curl http://localhost:8080/healthz
curl http://localhost:8080/v1/slots
```

## Локальная разработка без Docker API

```bash
docker compose up db migrate
export DATABASE_URL=postgres://vertical:vertical@localhost:5432/vertical?sslmode=disable
go run ./cmd/api
```

## OpenAPI / codegen (BE-01)

Спека: копия `01-analysis/api/` в `api/`. Bundled-версия для oapi-codegen:

```bash
make bundle-api    # требует Docker (node:20-alpine)
make generate      # bundle + go generate → internal/gen/api.gen.go
```

Роутер `/v1` собирается из `gen.ServerInterface` через `OpenAPIServer`.

## Dev endpoints (`DEV_MODE=true`)

- `POST /v1/dev/bookings/{bookingId}/mark-attended` — статус ATTENDED для тестов оценки
- `POST /v1/dev/slots/{slotId}/cancel-by-gym` — отмена слота скалодромом
- `POST /v1/dev/notifications/send` — заглушка push (лог в stdout)

## Миграции

Файлы в `db/migrations/`. Применяются автоматически через сервис `migrate` в Docker Compose.

## Тесты

```bash
# unit-тесты (без БД)
go test ./internal/validate/... ./internal/platform/... ./internal/gen/... ./internal/store/... ./internal/handler/... -run '^Test(?!.*Integration)'

# интеграционные (нужен PostgreSQL)
export TEST_DATABASE_URL=postgres://vertical:vertical@localhost:5432/vertical?sslmode=disable
go test ./internal/handler/... -run Integration

# всё
go test ./...
```
