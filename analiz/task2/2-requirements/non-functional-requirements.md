# Нефункциональные требования — кулинарная студия «Шеф-стол»

> Источник: [domain-description.md](../1-elicitation/domain-description.md).
> НФТ описывают **качественные характеристики** и ограничения системы. Функциональное поведение — в [functional-requirements.md](functional-requirements.md).
> Приоритет: **Must** / **Should** / **Could**.

| ID | Формулировка | Приоритет | Источник |
| :- | :-- | :--: | :-- |
| NFR-001 | Решение поставляется как **мобильное приложение Android** для роли «Клиент» (первая итерация). | Must | [domain-description.md §1, §6](../1-elicitation/domain-description.md#в-скоупе-mvp) |
| NFR-002 | Существующий бэкенд выступает **black-box источником истины**; гарантия отсутствия двойных броней обеспечивается на его стороне (R-004). Клиентское приложение и API полагаются на ответы бэкенда. | Must | [domain-description.md §3, §6](../1-elicitation/domain-description.md#6-границы-системы) |
| NFR-003 | Граница интеграции с бэкендом закрывается **контрактом API** (R-015); приложение корректно обрабатывает отказ бэкенда при отсутствии мест. | Must | [domain-description.md §3, §6](../1-elicitation/domain-description.md#6-границы-системы) |
| NFR-004 | Текущая поставка ограничена **ролью «Клиент»**; интерфейсы шефа и администратора не входят в скоуп (R-028). | Must | [domain-description.md §4, §6](../1-elicitation/domain-description.md#4-акторы) |
| NFR-005 | Детали серверной реализации (транзакционность, SLA, маппинг на внутренние модели бэкенда) **вне скоупа** клиентского приложения (R-004). | Must | [domain-description.md §6](../1-elicitation/domain-description.md#вне-скоупа--backlog) |
| NFR-006 | Миграция исторических данных **вне скоупа**; каноническая схема данных для клиентского контура — **контракт API** (R-015). | Must | [domain-description.md §6](../1-elicitation/domain-description.md#вне-скоупа--backlog) |
| NFR-007 | Создание и редактирование расписания, управление шефами и инициирование отмен по форс-мажору выполняются **вне** клиентского приложения (существующая инфраструктура). | Must | [domain-description.md §1, §6](../1-elicitation/domain-description.md#6-границы-системы) |
| NFR-008 | Интерфейс приложения — **только на русском языке**. | Must | [domain-description.md §6](../1-elicitation/domain-description.md#9-трассировка-qa--домен) |
| NFR-009 | Клиент может **просматривать свои записи без подключения к интернету** (офлайн-кэш). | Must | [domain-description.md §6](../1-elicitation/domain-description.md#в-скоупе-mvp) |
| NFR-010 | Уведомления клиенту доставляются **только через push** в приложении (SMS, email, WhatsApp — вне MVP). | Must | [domain-description.md §3, §6](../1-elicitation/domain-description.md#уведомления-mvp) |
