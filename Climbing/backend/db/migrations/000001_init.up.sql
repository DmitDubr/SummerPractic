-- Скалодром «Вертикаль» — инициализация схемы
-- Источники: 01-analysis/4-design/data-model.md, api/components/schemas.yaml

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- Enums (соответствие OpenAPI schemas.yaml)
-- ---------------------------------------------------------------------------

CREATE TYPE slot_status AS ENUM (
    'OPEN',
    'FULL',
    'CANCELLED',
    'UNAVAILABLE'
);

CREATE TYPE booking_status AS ENUM (
    'ACTIVE',
    'CANCELLED_BY_CLIENT',
    'CANCELLED_BY_GYM',
    'ATTENDED',
    'WAITLIST'
);

CREATE TYPE equipment_mode AS ENUM ('OWN', 'RENTAL');

CREATE TYPE training_level AS ENUM ('beginner', 'intermediate', 'advanced');

CREATE TYPE waitlist_entry_status AS ENUM ('WAITING', 'NOTIFIED', 'CONVERTED', 'LEFT');

CREATE TYPE push_platform AS ENUM ('ios', 'android');

-- ---------------------------------------------------------------------------
-- Catalog (read-only для Client API)
-- ---------------------------------------------------------------------------

CREATE TABLE gyms (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT        NOT NULL,
    address    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE zones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id     UUID        NOT NULL REFERENCES gyms (id) ON DELETE RESTRICT,
    name       TEXT        NOT NULL,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_formats (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id                 UUID            NOT NULL REFERENCES zones (id) ON DELETE RESTRICT,
    name                    TEXT            NOT NULL,
    level                   training_level  NOT NULL,
    max_capacity            INTEGER         NOT NULL,
    beginner_capacity_limit INTEGER         NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    -- BR-008, Q 2.1: лимиты 16 / 8 на уровне формата
    CONSTRAINT chk_training_format_capacity CHECK (
        max_capacity > 0
        AND max_capacity <= 16
        AND beginner_capacity_limit > 0
        AND beginner_capacity_limit <= 8
        AND beginner_capacity_limit <= max_capacity
    )
);

CREATE TABLE price_tariffs (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    format_id            UUID           NOT NULL REFERENCES training_formats (id) ON DELETE RESTRICT,
    training_price       NUMERIC(10, 2) NOT NULL,
    shoes_rental_price   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    harness_rental_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_price_tariff_non_negative CHECK (
        training_price >= 0
        AND shoes_rental_price >= 0
        AND harness_rental_price >= 0
    ),
    CONSTRAINT uq_price_tariff_format UNIQUE (format_id)
);

CREATE TABLE instructors (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    TEXT           NOT NULL,
    photo_url    TEXT,
    avg_rating   NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    rating_count INTEGER        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_instructor_rating CHECK (
        avg_rating >= 0 AND avg_rating <= 5 AND rating_count >= 0
    )
);

-- ---------------------------------------------------------------------------
-- Schedule
-- ---------------------------------------------------------------------------

CREATE TABLE slots (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    format_id      UUID           NOT NULL REFERENCES training_formats (id) ON DELETE RESTRICT,
    instructor_id  UUID           NOT NULL REFERENCES instructors (id) ON DELETE RESTRICT,
    starts_at      TIMESTAMPTZ    NOT NULL,
    ends_at        TIMESTAMPTZ    NOT NULL,
    capacity       INTEGER        NOT NULL,
    booked_count   INTEGER        NOT NULL DEFAULT 0,
    free_spots     INTEGER        NOT NULL,
    status         slot_status    NOT NULL DEFAULT 'OPEN',
    base_price     NUMERIC(10, 2) NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_slot_time CHECK (ends_at > starts_at),
    CONSTRAINT chk_slot_capacity CHECK (
        capacity > 0
        AND booked_count >= 0
        AND booked_count <= capacity
        AND free_spots >= 0
        AND free_spots = capacity - booked_count
    ),
    CONSTRAINT chk_slot_base_price CHECK (base_price >= 0)
);

CREATE TABLE rental_availability (
    slot_id            UUID    PRIMARY KEY REFERENCES slots (id) ON DELETE CASCADE,
    shoes_available    INTEGER NOT NULL,
    harness_available  INTEGER NOT NULL,
    is_bookable        BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_rental_availability_non_negative CHECK (
        shoes_available >= 0 AND harness_available >= 0
    )
);

-- ---------------------------------------------------------------------------
-- Client contour (read-write)
-- ---------------------------------------------------------------------------

CREATE TABLE clients (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        NOT NULL,
    phone       TEXT        NOT NULL,
    is_regular  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Q 1.1: формат +7XXXXXXXXXX
    CONSTRAINT chk_client_phone CHECK (phone ~ '^\+7\d{10}$'),
    CONSTRAINT chk_client_name CHECK (length(trim(name)) > 0),
    CONSTRAINT uq_clients_phone UNIQUE (phone)
);

CREATE TABLE bookings (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id            UUID            NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    slot_id              UUID            NOT NULL REFERENCES slots (id) ON DELETE RESTRICT,
    -- Денормализация для индекса «1 ACTIVE в день» (Q 1.3)
    slot_starts_at       TIMESTAMPTZ     NOT NULL,
    status               booking_status  NOT NULL DEFAULT 'ACTIVE',
    equipment_mode       equipment_mode  NOT NULL,
    rental_shoes         BOOLEAN         NOT NULL DEFAULT FALSE,
    rental_harness       BOOLEAN         NOT NULL DEFAULT FALSE,
    total_price          NUMERIC(10, 2)  NOT NULL,
    cancellation_reason  TEXT,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    cancelled_at         TIMESTAMPTZ,

    -- Q 2.3, FR-005
    CONSTRAINT chk_equipment_own CHECK (
        equipment_mode != 'OWN' OR (rental_shoes = FALSE AND rental_harness = FALSE)
    ),
    CONSTRAINT chk_equipment_rental CHECK (
        equipment_mode != 'RENTAL' OR (rental_shoes = TRUE OR rental_harness = TRUE)
    ),
    -- FR-009, R-008
    CONSTRAINT chk_cancellation_reason CHECK (
        status != 'CANCELLED_BY_GYM'
        OR (cancellation_reason IS NOT NULL AND length(trim(cancellation_reason)) > 0)
    ),
    CONSTRAINT chk_booking_total_price CHECK (total_price >= 0),
    CONSTRAINT chk_booking_cancelled_at CHECK (
        status IN ('CANCELLED_BY_CLIENT', 'CANCELLED_BY_GYM') OR cancelled_at IS NULL
    )
);

CREATE TABLE waitlist_entries (
    id          UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID                   NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    slot_id     UUID                   NOT NULL REFERENCES slots (id) ON DELETE RESTRICT,
    position    INTEGER                NOT NULL,
    status      waitlist_entry_status  NOT NULL DEFAULT 'WAITING',
    created_at  TIMESTAMPTZ            NOT NULL DEFAULT now(),

    CONSTRAINT chk_waitlist_position CHECK (position > 0)
);

CREATE TABLE instructor_ratings (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id     UUID        NOT NULL REFERENCES bookings (id) ON DELETE RESTRICT,
    instructor_id  UUID        NOT NULL REFERENCES instructors (id) ON DELETE RESTRICT,
    client_id      UUID        NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    stars          INTEGER     NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Q 5.2
    CONSTRAINT chk_rating_stars CHECK (stars BETWEEN 1 AND 5),
    -- Q 5.1, FR-012: одна оценка на бронь
    CONSTRAINT uq_instructor_ratings_booking UNIQUE (booking_id)
);

CREATE TABLE client_push_tokens (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id   UUID           NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    token       TEXT           NOT NULL,
    platform    push_platform  NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uq_client_push_token UNIQUE (client_id, token)
);

-- ---------------------------------------------------------------------------
-- Partial unique indexes (бизнес-правила, которые нельзя выразить только CHECK)
-- ---------------------------------------------------------------------------

-- Q 1.3: не более 1 ACTIVE брони в календарный день
-- Календарный день в часовом поясе площадки (Q 1.3)
CREATE UNIQUE INDEX uq_bookings_one_active_per_day
    ON bookings (client_id, ((slot_starts_at AT TIME ZONE 'Europe/Moscow')::date))
    WHERE status = 'ACTIVE';

-- Один клиент — одна «живая» бронь на слот (R-004, гонки на уровне клиента)
CREATE UNIQUE INDEX uq_bookings_client_slot_live
    ON bookings (client_id, slot_id)
    WHERE status IN ('ACTIVE', 'WAITLIST', 'ATTENDED');

-- Q 1.4: один клиент — одна активная позиция в очереди на слот
CREATE UNIQUE INDEX uq_waitlist_client_slot_active
    ON waitlist_entries (client_id, slot_id)
    WHERE status IN ('WAITING', 'NOTIFIED');

-- Уникальная позиция в очереди на слот среди ожидающих
CREATE UNIQUE INDEX uq_waitlist_slot_position_waiting
    ON waitlist_entries (slot_id, position)
    WHERE status = 'WAITING';

-- ---------------------------------------------------------------------------
-- Supporting indexes
-- ---------------------------------------------------------------------------

CREATE INDEX idx_zones_gym_id ON zones (gym_id);
CREATE INDEX idx_training_formats_zone_id ON training_formats (zone_id);
CREATE INDEX idx_slots_format_id ON slots (format_id);
CREATE INDEX idx_slots_instructor_id ON slots (instructor_id);
CREATE INDEX idx_slots_starts_at ON slots (starts_at);
CREATE INDEX idx_slots_status ON slots (status);
CREATE INDEX idx_bookings_client_id ON bookings (client_id);
CREATE INDEX idx_bookings_slot_id ON bookings (slot_id);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_waitlist_entries_slot_id ON waitlist_entries (slot_id);
CREATE INDEX idx_instructor_ratings_instructor_id ON instructor_ratings (instructor_id);

-- ---------------------------------------------------------------------------
-- Triggers: согласованность слота и брони (R-004)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION trg_bookings_sync_slot_starts_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT s.starts_at
      INTO NEW.slot_starts_at
      FROM slots s
     WHERE s.id = NEW.slot_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'slot % not found', NEW.slot_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER bookings_set_slot_starts_at
    BEFORE INSERT ON bookings
    FOR EACH ROW
    EXECUTE PROCEDURE trg_bookings_sync_slot_starts_at();

CREATE OR REPLACE FUNCTION trg_bookings_reserve_slot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_slot slots%ROWTYPE;
BEGIN
    IF NEW.status <> 'ACTIVE' THEN
        RETURN NEW;
    END IF;

    SELECT *
      INTO v_slot
      FROM slots
     WHERE id = NEW.slot_id
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'slot % not found', NEW.slot_id USING ERRCODE = '23503';
    END IF;

    IF v_slot.status = 'CANCELLED' THEN
        RAISE EXCEPTION 'slot cancelled' USING ERRCODE = 'check_violation';
    END IF;

    IF v_slot.status = 'UNAVAILABLE' THEN
        RAISE EXCEPTION 'rental unavailable' USING ERRCODE = 'check_violation';
    END IF;

    IF v_slot.free_spots <= 0 THEN
        RAISE EXCEPTION 'no spots available' USING ERRCODE = 'check_violation';
    END IF;

    UPDATE slots
       SET booked_count = booked_count + 1,
           free_spots   = free_spots - 1,
           status       = CASE
                              WHEN free_spots - 1 = 0 THEN 'FULL'::slot_status
                              ELSE status
                          END,
           updated_at   = now()
     WHERE id = NEW.slot_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER bookings_reserve_slot
    AFTER INSERT ON bookings
    FOR EACH ROW
    WHEN (NEW.status = 'ACTIVE')
    EXECUTE PROCEDURE trg_bookings_reserve_slot();

CREATE OR REPLACE FUNCTION trg_bookings_release_slot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status = 'ACTIVE'
       AND NEW.status IN ('CANCELLED_BY_CLIENT', 'CANCELLED_BY_GYM') THEN
        UPDATE slots
           SET booked_count = booked_count - 1,
               free_spots   = free_spots + 1,
               status       = CASE
                                  WHEN status = 'FULL' THEN 'OPEN'::slot_status
                                  ELSE status
                              END,
               updated_at   = now()
         WHERE id = OLD.slot_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER bookings_release_slot
    AFTER UPDATE OF status ON bookings
    FOR EACH ROW
    EXECUTE PROCEDURE trg_bookings_release_slot();

CREATE OR REPLACE FUNCTION trg_instructor_ratings_attended_only()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_status booking_status;
BEGIN
    SELECT status
      INTO v_status
      FROM bookings
     WHERE id = NEW.booking_id;

    IF v_status IS DISTINCT FROM 'ATTENDED' THEN
        RAISE EXCEPTION 'rating allowed only for attended bookings'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER instructor_ratings_attended_only
    BEFORE INSERT ON instructor_ratings
    FOR EACH ROW
    EXECUTE PROCEDURE trg_instructor_ratings_attended_only();

CREATE OR REPLACE FUNCTION trg_instructor_ratings_update_aggregate()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE instructors
       SET avg_rating   = (
               SELECT COALESCE(ROUND(AVG(stars)::numeric, 2), 0)
                 FROM instructor_ratings
                WHERE instructor_id = NEW.instructor_id
           ),
           rating_count = (
               SELECT COUNT(*)
                 FROM instructor_ratings
                WHERE instructor_id = NEW.instructor_id
           )
     WHERE id = NEW.instructor_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER instructor_ratings_update_aggregate
    AFTER INSERT ON instructor_ratings
    FOR EACH ROW
    EXECUTE PROCEDURE trg_instructor_ratings_update_aggregate();
