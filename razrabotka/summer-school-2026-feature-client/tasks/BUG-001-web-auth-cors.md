# BUG-001: Web-вход блокируется CORS

**Тип:** баг  
**Область:** backend `internal/http`, Web-клиент (wasm)  
**Статус:** исправлено  
**Commit:** _не сделан_

---

## 1. Симптом

На Web (`http://localhost:8081`) после ввода номера и нажатия «Получить код» — снек:

> Не удалось войти. Попробуйте ещё раз

Консоль браузера:

```
Access to fetch at 'http://localhost:8080/auth/request-code' from origin 'http://localhost:8081'
has been blocked by CORS policy: Response to preflight request doesn't pass access control check:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

Клиент:

```
REQUEST http://localhost:8080/auth/request-code failed with exception: Error: Fail to fetch
Failed to request auth code Unknown
```

`curl` / healthcheck на `:8080` работали — проблема только в браузере.

---

## 2. Требования

| ID | Связь |
|----|--------|
| SCR-001 | Вход по телефону + OTP, шаг «Получить код» → `requestAuthCode` |
| LOGIC-001 | OTP-авторизация, `POST /auth/request-code` |
| OpenAPI | `auth` domain, operationId `requestAuthCode` |
| LOCAL_DEV_GUIDE §6 | Web dev server + backend на `localhost:8080` — разные порты ⇒ cross-origin |

CORS в OpenAPI/ТЗ явно не описан, но для Web target обязателен на уровне HTTP transport.

---

## 3. Суть бага

Web (`:8081`) и API (`:8080`) — **разные origin**. Браузер шлёт preflight `OPTIONS`; backend не возвращал CORS-заголовки → запрос блокировался.

Дополнительно: в `AuthStore` ошибка классифицировалась как `AppFailure.Unknown`, поэтому UI показывал общий текст входа, а не сетевую ошибку.

**Причина:** в `router.go` не было CORS middleware (для Android/iOS/native клиентов CORS не нужен).

---

## 4. Реализация

**Файлы:**

- `backend/internal/http/middleware.go` — `corsMiddleware`, regex для `localhost` / `127.0.0.1`
- `backend/internal/http/router.go` — middleware первым в цепочке
- `backend/internal/http/infrastructure_test.go` — `TestCORSPreflightAllowsLocalhostOrigin`

**Поведение middleware:**

- `OPTIONS` → `204 No Content`
- `Access-Control-Allow-Origin` = echo `Origin`
- заголовки: `Authorization`, `Content-Type`, `Idempotency-Key`, `X-Request-Id`
- методы: `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS`

**Перезапуск:**

```powershell
cd backend
docker compose --profile app up --build -d
```

---

## 5. Промпты

1. > Походу у меня упал сервер в postgresql … failed to resolve host 'volna'

   _(контекст: окружение не было поднято; привело к диагностике login flow)_

2. > когда пытаюсь войти на сайт после ввода номера пишет «Не удалось войти. Попробуйте ещё раз»

3. > _(лог консоли с CORS и request-code на localhost:8080/8081)_

4. > Это был баг по сути? если да добавь .md отчёт в папку tasks об этом баге

---

## 6. Ручная проверка

### Preflight

```powershell
curl.exe -i -X OPTIONS http://127.0.0.1:8080/auth/request-code `
  -H "Origin: http://localhost:8081" `
  -H "Access-Control-Request-Method: POST"
```

**Ожидание:** `HTTP/1.1 204`, заголовок `Access-Control-Allow-Origin: http://localhost:8081`.

### Web UI

1. `docker compose --profile app up -d` (db + api).
2. `./gradlew :webApp:wasmJsBrowserDevelopmentRun` в `client/`.
3. Открыть `http://localhost:8081`, ввести телефон, «Получить код».
4. **Ожидание:** переход на шаг OTP, в Network — `request-code` со статусом `200`, в теле — `code`.

| Шаг | Результат |
|-----|-----------|
| Preflight curl | ✅ |
| Web «Получить код» | ⬜ _проверить у себя после пересборки_ |

---

## 7. Commit (когда будет)

```
fix(backend): add CORS for local Web dev auth requests

Allow localhost origins so wasm client on :8081 can call API on :8080.
Fixes SCR-001 login on Web target.
```
