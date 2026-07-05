# FEAT-003: запрос push после первой брони (LOGIC-007 / BS-002)

**Тип:** фича  
**Область:** client booking success + backend createBooking  
**Статус:** реализовано  
**Commit:** _не сделан_

## 1. Цель

После первой успешной записи на BS-002 показать подводку про напоминания и запросить разрешение на уведомления один раз за установку.

## 2. Требования

| ID | Связь |
|----|--------|
| BS-002 AC-005, AC-N01, AC-N02 | Push при первой брони, без повторов |
| LOGIC-007 | `is_first_booking`, `push_permission_requested`, `reminder_hours` |
| OpenAPI bookings | `CreateBookingResponse.is_first_booking`, `reminder_hours` |

## 3. Реализация

**Backend:** при `createBooking` — `is_first_booking` (нет других active броней до insert), `reminder_hours: [24, 2]`.

**Client:**
- `AppPreferences` — флаг `push_permission_requested`
- `PushPermissionGateway` (expect/actual): Web — in-app choice (системный Notification API — follow-up); Android/iOS — stub
- `Booking.reminderHours`, UI-карточка на `BookingSuccessSheet`

## 4. Промпты

> давай ещё 1 баг и 3 фичи сразу завести документы и начать реализацию.

## 5. Ручная проверка

1. Новый пользователь → войти → записаться на слот.
2. На «Вы записаны» — карточка «Напомним за 24 и 2 ч…» с кнопками.
3. «Включить» → системный запрос (Web: Notification permission).
4. Вторая бронь → карточка не показывается.

## 6. Commit (когда будет)

```
feat: push permission prompt after first booking (LOGIC-007)
```
