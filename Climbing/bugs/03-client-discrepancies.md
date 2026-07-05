# 03 — Расхождения CMP-клиента с ТЗ

Путь: `Climbing/client/shared/src/commonMain/kotlin/com/vertical/app/`.

---

## CMP-01: SCR-011 — Оценка инструктора не реализована

| Поле | Значение |
|------|----------|
| **ID** | CMP-01 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-006, SCR-011, UC-006, FR-012 |

**Ожидание:** bottom sheet, `POST /ratings`, CTA на SCR-009 для `attended`.

**Факт:** нет `RatingRepository`, store, screen; нет вызова `createRating`.

---

## CMP-02: SCR-013 — Контактный профиль (sheet) не реализован

| Поле | Значение |
|------|----------|
| **ID** | CMP-02 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-001, SCR-013 |

**Ожидание:**

- при `isComplete=true` — сводка «Имя · +7 ***» + «Изменить»;
- bottom sheet для редактирования;
- бейдж «Постоянный клиент» при `isRegularClient`.

**Факт:** только inline-поля в `BookingFormScreen.kt`; sheet и summary mode отсутствуют.

---

## CMP-03: SCR-006 — Успех записи как AlertDialog

| Поле | Значение |
|------|----------|
| **ID** | CMP-03 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | SCR-006, LOGIC-007, XR-07, XR-08 |

**Ожидание:** полноэкранный success + запрос push-разрешения (LOGIC-007).

**Факт:** `AlertDialog` в `BookingFormScreen.kt`; нет push prompt, нет `registerPushToken`.

---

## CMP-04: LOGIC-007 — Push не реализован

| Поле | Значение |
|------|----------|
| **ID** | CMP-04 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-007, FR-010, FR-013, Q 6.1–6.2 |

**Отсутствует:**

- запрос разрешения на SCR-006;
- `POST /profile/push-token`;
- флаг `pushPermissionRequested` в storage;
- deep links из push.

---

## CMP-05: SCR-008 — Нет сегмента «Прошедшие»

| Поле | Значение |
|------|----------|
| **ID** | CMP-05 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | SCR-008, BS-02, BS-03, FR-007 |

**Ожидание:** переключатель «Предстоящие | Прошедшие»; все статусы брони.

**Факт:** `BookingListStore` фильтрует только `Active` и `Waitlist`:

```kotlin
// BookingStores.kt
val upcoming = list.filter {
    it.status == BookingStatus.Active || it.status == BookingStatus.Waitlist
}
```

**Скрыты:** `Attended`, `CancelledByClient`, `CancelledByGym`.

**Решение (2026-07-05):** Добавлен переключатель «Предстоящие | Прошедшие» (`BookingsSegment`, `BookingListScreen`). Прошедшие показывают все статусы кроме Active/Waitlist (`BookingsSegmentPolicy`).

---

## CMP-06: Waitlist — нет выхода из очереди

| Поле | Значение |
|------|----------|
| **ID** | CMP-06 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | SCR-009, SCR-012, WL-09 |

**Ожидание:** CTA «Покинуть очередь» на SCR-009; `DELETE /waitlist/{id}` или `leave-waitlist`.

**Факт:** нет API-вызова в `Repositories.kt`; `CancellationPolicy.canCancel()` только для `Active`.

**Решение (2026-07-05):** `BookingRepository.leaveWaitlist()` → `POST /bookings/{id}/leave-waitlist`. CTA «Покинуть очередь» на SCR-009, `CancellationPolicy.canLeaveWaitlist()`.

---

## CMP-07: `waitlistPosition` не в модели

| Поле | Значение |
|------|----------|
| **ID** | CMP-07 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | SCR-008, SCR-009, WL-08 |

**Ожидание:** «Вы N-й в очереди» / «Место в очереди: N».

**Факт:** поле отсутствует в `BookingDto`, `Models.kt`, UI.

**Решение (2026-07-05):** Поле `waitlistPosition` в `BookingSummary`/`Booking`, DTO mapping, отображение в списке и детали («Место в очереди: N»).

---

## CMP-08: LOGIC-008 — Офлайн-кэш не реализован

| Поле | Значение |
|------|----------|
| **ID** | CMP-08 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-008, Q 9.2, SCR-008, SCR-009 |

**Ожидание:**

- кэш `listBookings`, `getBooking`;
- состояние `Offline` + баннер;
- disabled destructive actions offline.

**Факт:** нет локального persistence; в `Loadable.kt` нет варианта `Offline`.

---

## CMP-09: SCR-009 — Неполная деталь записи

| Поле | Значение |
|------|----------|
| **ID** | CMP-09 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | SCR-009, GC-01–GC-04 |

**Отсутствует в UI:**

- блок снаряжения (своё / прокат);
- адрес скалодрома;
- интервал времени (18:30 – 20:00);
- `cancellation_reason` для `cancelled_by_gym`;
- CTA «Выбрать другую тренировку»;
- CTA «Оценить инструктора»;
- блок позиции в очереди.

**Решение (2026-07-05):** Расширены `Booking`/`BookingDto` (gym, endsAt, zone, cancellationReason, waitlistPosition). SCR-009 показывает снаряжение, адрес, интервал времени, причину отмены скалодрома, позицию в очереди, CTA «Выбрать другую тренировку». CTA «Оценить инструктора» — см. CMP-01 (P2).

---

## CMP-10: Ошибка отмены молча игнорируется

| Поле | Значение |
|------|----------|
| **ID** | CMP-10 |
| **Приоритет** | P1 |
| **Статус** | Решён |
| **ТЗ** | SCR-010, L-004-11, `ALREADY_CANCELLED` |

**Факт:** `BookingDetailStore.cancel()` при failure только закрывает dialog без сообщения:

```kotlin
onFailure = {
    mutableState.update { it.copy(cancelling = false, showCancelConfirm = false) }
}
```

**Решение (2026-07-05):** `CancelErrorPolicy` + Snackbar с текстом ошибки (`ALREADY_CANCELLED`, `CANCEL_TOO_LATE` и др.) в `BookingDetailScreen`.

---

## CMP-11: LOGIC-006 — Рейтинг инструктора отображается частично

| Поле | Значение |
|------|----------|
| **ID** | CMP-11 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | L-006-01, L-006-02 |

| Экран | ТЗ | Клиент |
|-------|-----|--------|
| SCR-001 карточка | ★ рейтинг | ✅ частично |
| SCR-003 чипы инструкторов | ★ на чипе | ❌ |
| SCR-004 деталь | ★ + ratingCount, «Пока нет оценок» | ❌ (`ratingCount` отброшен в DTO) |
| SCR-009 | рейтинг + CTA оценки | ❌ |

---

## CMP-12: SCR-002 — Упрощённый фильтр дат

| Поле | Значение |
|------|----------|
| **ID** | CMP-12 |
| **Приоритет** | P3 |
| **Статус** | Открыт |
| **ТЗ** | SCR-002, L-005-02 |

**Ожидание:** календарный date range picker, max 30 дней, disabled прошлые даты.

**Факт:** текстовый ввод дат в `DateFilterSheet`; чип «14 дней» не auto-apply.

---

## CMP-13: Нет pull-to-refresh

| Поле | Значение |
|------|----------|
| **ID** | CMP-13 |
| **Приоритет** | P3 |
| **Статус** | Открыт |
| **ТЗ** | SCR-001, SCR-008, L-008-03 |

**Факт:** `ScheduleIntent.Refresh` есть, но жест PTR на экранах отсутствует.

---

## CMP-14: SCR-010 — Dialog вместо bottom sheet

| Поле | Значение |
|------|----------|
| **ID** | CMP-14 |
| **Приоритет** | P3 |
| **Статус** | Решён |
| **ТЗ** | SCR-010 |

**Ожидание:** bottom sheet с drag handle, «Оставить запись» primary.

**Факт:** `AlertDialog` в `BookingDetailScreen.kt`.

**Решение (2026-07-05):** Подтверждение отмены через `ModalBottomSheet` с primary «Оставить запись».

---

## CMP-15: LOGIC-004 — Отмена offline не блокируется

| Поле | Значение |
|------|----------|
| **ID** | CMP-15 |
| **Приоритет** | P2 |
| **Статус** | Открыт |
| **ТЗ** | L-004-10 |

**Ожидание:** кнопка «Отменить запись» disabled offline + «Требуется интернет».

**Факт:** нет детекции сети в клиенте.

---

## CMP-16: `getProfile` без сессии

| Поле | Значение |
|------|----------|
| **ID** | CMP-16 |
| **Приоритет** | P3 |
| **Статус** | Открыт |
| **ТЗ** | LOGIC-001 |

**Факт:** `BookingFormStore.open()` вызывает `getProfile()` без проверки токена → 401 при первой записи (поля остаются пустыми, допустимо, но не по LOGIC-001 cache-first).

---

## CMP-17: Shared ViewModels на корне NavHost

| Поле | Значение |
|------|----------|
| **ID** | CMP-17 |
| **Приоритет** | P3 |
| **Статус** | Открыт |
| **ТЗ** | XR-08 (косвенно) |

**Факт:** stores как `koinViewModel()` в `VerticalApp.kt` — состояние success/error может сохраняться при повторном входе на экран.

---

## CMP-18: Отсутствующие API-операции в Repositories

| Поле | Значение |
|------|----------|
| **ID** | CMP-18 |
| **Приоритет** | P1–P2 |
| **Статус** | Решён |
| **ТЗ** | feature-list.md §8 |

| operationId | Path | Экран |
|-------------|------|-------|
| `leaveWaitlist` | `POST /bookings/{id}/leave-waitlist` | SCR-009 |
| `deleteWaitlistEntry` | `DELETE /waitlist/{id}` | SCR-012 |
| `getWaitlistEntry` | `GET /waitlist/{id}` | SCR-012 |
| `registerPushToken` | `POST /profile/push-token` | SCR-006 |
| `createRating` | `POST /ratings` | SCR-011 |

**Решение (2026-07-05):** Добавлены `BookingRepository.leaveWaitlist`, `WaitlistRepository.get/delete`, `RatingRepository.createRating`, `PushRepository.registerPushToken` с Ktor-реализациями в `Repositories.kt`. UI для rating/push — см. CMP-01/CMP-04 (P2).
