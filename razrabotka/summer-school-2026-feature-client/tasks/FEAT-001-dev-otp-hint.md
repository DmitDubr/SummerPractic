# FEAT-001: подсказка dev OTP на SCR-001

**Тип:** фича (dev UX)  
**Область:** client auth flow  
**Статус:** реализовано  
**Commit:** _не сделан_

## 1. Цель

В dev-режиме backend возвращает OTP в поле `code` ответа `requestAuthCode`. Показать код на экране подтверждения, чтобы не искать его в Network/логах.

## 2. Требования

| ID | Связь |
|----|--------|
| SCR-001 | Шаг OTP после `requestAuthCode` |
| OpenAPI auth | `RequestCodeResponse.code` (optional, dev) |
| AGENTS.md | Dev backend возвращает code вместо SMS |

## 3. Реализация

- Парсинг `code` в `RequestCodeResponseDto` → `RequestCodeResult.devCode`
- `AuthState.devOtpCode` после успешного `requestCode`
- Блок на `OtpStep`: «Код для разработки: XXXX» (только если `code` пришёл)

## 4. Промпты

> давай ещё 1 баг и 3 фичи сразу завести документы и начать реализацию.

## 5. Ручная проверка

1. API запущен, войти на Web.
2. «Получить код» → на шаге OTP видна подсказка с 4-значным кодом.
3. Код из подсказки проходит verify.

## 6. Commit (когда будет)

```
feat(client): show dev OTP hint when API returns code field
```
