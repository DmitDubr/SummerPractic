-- Re-apply broken 000003 body (for rollback only; do not use in production)

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

    IF NEW.equipment_mode = 'RENTAL' THEN
        IF NEW.rental_shoes THEN
            UPDATE rental_availability
               SET shoes_available = shoes_available - 1,
                   updated_at = now()
             WHERE slot_id = NEW.slot_id
               AND shoes_available > 0;
            IF NOT FOUND THEN
                RAISE EXCEPTION 'rental unavailable' USING ERRCODE = 'check_violation';
            END IF;
        END IF;
        IF NEW.rental_harness THEN
            UPDATE rental_availability
               SET harness_available = harness_available - 1,
                   updated_at = now()
             WHERE slot_id = NEW.slot_id
               AND harness_available > 0;
            IF NOT FOUND THEN
                RAISE EXCEPTION 'rental unavailable' USING ERRCODE = 'check_violation';
            END IF;
        END IF;
        UPDATE rental_availability
           SET is_bookable = (shoes_available > 0 OR harness_available > 0),
               updated_at = now()
         WHERE slot_id = NEW.slot_id;
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
