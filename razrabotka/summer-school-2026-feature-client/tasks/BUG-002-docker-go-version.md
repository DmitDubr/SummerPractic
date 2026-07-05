# BUG-002: Docker-сборка API падает — Go 1.23 vs go.mod 1.25.7

**Тип:** баг (инфраструктура / dev experience)  
**Область:** `backend/Dockerfile`, `backend/compose.yaml`  
**Статус:** исправлено  
**Commit:** _не сделан_

---

## 1. Симптом

На Windows после `docker compose --profile app up --build`:

```
go: go.mod requires go >= 1.25.7 (running go 1.23.12; GOTOOLCHAIN=local)
failed to solve: process "/bin/sh -c go mod download" did not complete successfully: exit code: 1
```

API в Docker не собирается; `make run` недоступен без локального Go.

---

## 2. Требования

| Источник | Связь |
|----------|--------|
| `backend/go.mod` | `go 1.25.7` |
| `backend/README.md` | Local Run через Docker Compose |
| `LOCAL_DEV_GUIDE.md` | Запуск BE + DB для клиента |
| AGENTS.md | BE target stack Go REST API |

Версия toolchain в Docker должна соответствовать `go.mod`.

---

## 3. Суть бага

`Dockerfile` и сервис `migrate` в `compose.yaml` использовали образ `golang:1.23-alpine`, тогда как модуль уже требует **Go ≥ 1.25.7**. Сборка падала на этапе `go mod download`.

---

## 4. Реализация

**Изменения:**

- `backend/Dockerfile`: `FROM golang:1.25-alpine AS build`
- `backend/compose.yaml` (service `migrate`): `image: golang:1.25-alpine`

**Проверка сборки:**

```powershell
cd backend
docker compose --profile app up --build -d
curl.exe http://127.0.0.1:8080/healthz
```

---

## 5. Промпты

1. > _(вывод PowerShell: `make migrate` / `make run` — команда не найдена)_

2. > _(вывод `docker compose --profile app up --build` с ошибкой go 1.23 vs 1.25.7)_

3. > Задание такое … найти и исправить 3 разных бага … Commit не делай пока что. У нас есть решённый 1 баг

   _(BUG-002 учтён как второй уже исправленный баг в рамках того же инцидента запуска)_

---

## 6. Ручная проверка

| Шаг | Ожидание | Результат |
|-----|----------|-----------|
| `docker compose --profile app up --build` | Exit 0, образ `backend-api` собран | ✅ |
| `curl.exe http://127.0.0.1:8080/healthz` | `{"status":"ok"}` | ✅ |
| `docker compose --profile migrations run --rm migrate` | `no migrations to run` или успешный `up` | ✅ |

---

## 7. Commit (когда будет)

```
fix(backend): align Docker Go image with go.mod 1.25.7

Update Dockerfile and migrate service to golang:1.25-alpine.
```
