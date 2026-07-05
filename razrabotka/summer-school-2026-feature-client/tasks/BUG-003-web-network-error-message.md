# BUG-003: на Web сетевая ошибка показывается как «Не удалось войти»

**Тип:** баг  
**Область:** client `VolnaApiClient`, `AuthStore`  
**Статус:** исправлено  
**Commit:** _не сделан_

## 1. Симптом

При недоступном API или CORS-блокировке на Web (wasm) пользователь видит «Не удалось войти. Попробуйте ещё раз» вместо «Не удалось загрузить. Проверьте соединение и попробуйте снова».

В консоли: `Fail to fetch`, `Failed to request auth code Unknown`.

## 2. Требования

| ID | Связь |
|----|--------|
| LOGIC-001 | Ошибка сети на шаге 1 → снек про соединение |
| SCR-001 | HTTP/network failure → «Не удалось загрузить…» |
| LOGIC-008 | Корректная классификация ошибок для UI |

## 3. Реализация

`VolnaApiClient.toAppFailureException()` — распознаёт browser fetch errors (`Fail to fetch`, `NetworkError`, `Load failed`) и маппит в `AppFailure.NetworkUnavailable`.

## 4. Промпты

> давай ещё 1 баг и 3 фичи сразу завести документы и начать реализацию.

## 5. Ручная проверка

1. Остановить API (`docker compose stop api`).
2. На Web нажать «Получить код».
3. **Ожидание:** снек «Не удалось загрузить. Проверьте соединение и попробуйте снова».

## 6. Commit (когда будет)

```
fix(client): map wasm fetch failures to network errors on auth
```
