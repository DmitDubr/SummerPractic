-- Restore original trigger bodies from 000001_init.up.sql

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
