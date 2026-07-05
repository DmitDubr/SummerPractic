# Функциональные требования — картинг-центр «Апекс»

> Источник: [domain-description.md](../1-elicitation/domain-description.md).
> ФТ описывают **что** делает система. Нефункциональные аспекты — в [non-functional-requirements.md](non-functional-requirements.md).
> Приоритет: **Must** — MVP v1; **Should** — v2 (backlog в домене).

| ID | Формулировка | Приоритет | Источник |
| :- | :-- | :--: | :-- |
| FR-001 | Клиентское приложение отображает список доступных слотов на **ближайшие 7 дней** от текущего момента (R-027). | Must | [domain-description.md §3, §5.1](../1-elicitation/domain-description.md#51-просмотр-расписания) |
| FR-002 | Клиент может **расширить период** просмотра расписания с помощью фильтра дат. | Must | [domain-description.md §3, §5.1](../1-elicitation/domain-description.md#51-просмотр-расписания) |
| FR-003 | Клиент может **фильтровать** расписание по **времени суток** и **маршалу**. | Must | [domain-description.md §3, §5.1](../1-elicitation/domain-description.md#вместимость-и-расписание) |
| FR-004 | Для каждого слота отображаются: **время начала**, **конфигурация трассы (кратко)**, **маршал**, **«есть места» / «мест нет»**, **цена**; **рейтинг маршала** — во v2. | Must | [domain-description.md §5.1](../1-elicitation/domain-description.md#51-просмотр-расписания) |
| FR-005 | При отсутствии доступных слотов на ближайшие дни приложение показывает empty state: **«Пока нет доступных заездов»**. | Must | [domain-description.md §5.1, §8](../1-elicitation/domain-description.md#8-глоссарий) |
| FR-006 | Клиент может **записаться на выбранный слот**, указав **имя и телефон** (при первой записи). | Must | [domain-description.md §3](../1-elicitation/domain-description.md#бронирование-и-экипировка) |
| FR-007 | При записи клиент может указать **нескольких участников** в одной брони. | Must | [domain-description.md §3, §5.2](../1-elicitation/domain-description.md#52-бронирование) |
| FR-008 | При записи клиент указывает **свою экипировку** или **прокат** (шлем, подшлемник). | Must | [domain-description.md §3, §5.2](../1-elicitation/domain-description.md#52-бронирование) |
| FR-009 | При **исчерпании прокатного фонда** слот **недоступен** для записи. | Must | [domain-description.md §3](../1-elicitation/domain-description.md#бронирование-и-экипировка) |
| FR-010 | При бронировании приложение отправляет запрос бэкенду и **отображает результат**: подтверждение или отказ при отсутствии мест (R-004). | Must | [domain-description.md §3, §5.2](../1-elicitation/domain-description.md#52-бронирование) |
| FR-011 | Клиент может иметь **несколько записей в один день** (ограничение «одна запись в день» не применяется). | Must | [domain-description.md §3](../1-elicitation/domain-description.md#вместимость-и-расписание) |
| FR-012 | При заполненном заезде приложение отображает **«мест нет»** (лист ожидания не реализуется). | Must | [domain-description.md §3, §6](../1-elicitation/domain-description.md#вне-скоупа--backlog) |
| FR-013 | Приложение **отображает цену** заезда (зависит от **конфигурации трассы**); **прокат не влияет** на цену; оплата выполняется **на месте**. | Must | [domain-description.md §3](../1-elicitation/domain-description.md#бронирование-и-экипировка) |
| FR-014 | Клиент может **просматривать свои брони** со статусами: активна, отменена клиентом, **отменена центром**, посещена и др. | Must | [domain-description.md §2](../1-elicitation/domain-description.md#2-ключевые-сущности) |
| FR-015 | Клиент может **отменить свою бронь**; при отмене **≥ 1 ч** до начала карт в слоте **сразу** освобождается. | Must | [domain-description.md §3, §5.3](../1-elicitation/domain-description.md#53-отмена-брони) |
| FR-016 | При отмене **менее чем за 1 ч** до начала приложение показывает **предупреждение**; отмена **разрешена** (штрафов в MVP нет). | Must | [domain-description.md §3](../1-elicitation/domain-description.md#отмена-клиентом) |
| FR-017 | При отмене заезда **центром** (в т.ч. погода) бронь сохраняется со статусом **«Отменён центром»** и **причиной** (R-008). | Must | [domain-description.md §3, §5.6](../1-elicitation/domain-description.md#отмена-центром-и-погода-r-008) |
| FR-018 | Повторная запись клиента на **тот же отменённый центром слот** запрещена (R-008). | Must | [domain-description.md §3](../1-elicitation/domain-description.md#отмена-центром-и-погода-r-008) |
| FR-019 | Для **постоянных клиентов** отображается **метка** в профиле. | Must | [domain-description.md §3, §6](../1-elicitation/domain-description.md#постоянные-клиенты) |
| FR-020 | Слоты, конфигурации трасс, маршалы и данные проката **поступают из существующего бэкенда** через API; приложение их потребляет, но **не создаёт и не редактирует**. | Must | [domain-description.md §1, §6](../1-elicitation/domain-description.md#6-границы-системы) |
| FR-021 | Клиентское приложение реализует сценарии **только для роли «Клиент»** (R-028). | Must | [domain-description.md §4, §6](../1-elicitation/domain-description.md#4-акторы) |
| FR-022 | Client API обеспечивает операции по контракту: **слоты**, **бронирования**, **профиль**, **маршалы**, **прокат** (R-015). | Must | [domain-description.md §6](../1-elicitation/domain-description.md#в-скоупе-mvp-v1) |
| FR-023 | Клиенту отправляется **push-уведомление** при отмене заезда центром с предложением **перезаписаться на другой заезд** (R-008). | Should | [domain-description.md §3](../1-elicitation/domain-description.md#отмена-центром-и-погода-r-008) |
| FR-024 | Клиент может **перезаписаться на другой заезд** из **push-уведомления** при отмене центром. | Should | [domain-description.md §3](../1-elicitation/domain-description.md#отмена-центром-и-погода-r-008) |
| FR-025 | Клиенту отправляется **уведомление** (push и SMS) при **переносе** заезда (смена времени или маршала). | Should | [domain-description.md §3, §5.4](../1-elicitation/domain-description.md#54-напоминания-v2) |
| FR-026 | После **посещённого** заезда клиент может **оценить маршала** звёздами в течение **недели**. | Should | [domain-description.md §3, §5.5](../1-elicitation/domain-description.md#55-оценка-маршала-v2) |
| FR-027 | Клиент может иметь **не более одной оценки на маршала**; оценку можно **изменить** в период оценивания. | Should | [domain-description.md §3](../1-elicitation/domain-description.md#оценки-маршалов-v2) |
| FR-028 | **Рейтинги маршалов** отображаются **другим клиентам** при выборе слота. | Should | [domain-description.md §3, §5.1](../1-elicitation/domain-description.md#оценки-маршалов-v2) |
| FR-029 | Приложение отправляет клиенту **push и SMS**: напоминание **за 2 часа**, отмены (клиент / центром, в т.ч. погода). | Should | [domain-description.md §3, §5.4](../1-elicitation/domain-description.md#уведомления-v2) |
