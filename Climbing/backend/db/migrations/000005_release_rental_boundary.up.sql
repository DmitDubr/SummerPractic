-- BE-02: граница ранней отмены >= 1 час (согласовано с Go isEarlyCancel)
-- BE-03: возврат прокатного фонда при отмене RENTAL-брони

CREATE OR REPLACE FUNCTION trg_bookings_release_slot()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_starts_at timestamptz;
BEGIN
    IF OLD.status = 'ACTIVE'
       AND NEW.status IN ('CANCELLED_BY_CLIENT', 'CANCELLED_BY_GYM') THEN
        SELECT starts_at
          INTO v_starts_at
          FROM slots
         WHERE id = OLD.slot_id;

        IF NEW.status = 'CANCELLED_BY_GYM'
           OR (NEW.status = 'CANCELLED_BY_CLIENT' AND v_starts_at >= now() + interval '1 hour') THEN
            IF OLD.equipment_mode = 'RENTAL' THEN
                IF OLD.rental_shoes THEN
                    UPDATE rental_availability
                       SET shoes_available = shoes_available + 1
                     WHERE slot_id = OLD.slot_id;
                END IF;
                IF OLD.rental_harness THEN
                    UPDATE rental_availability
                       SET harness_available = harness_available + 1
                     WHERE slot_id = OLD.slot_id;
                END IF;
                UPDATE rental_availability
                   SET is_bookable = (shoes_available > 0 OR harness_available > 0)
                 WHERE slot_id = OLD.slot_id;
            END IF;

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
    END IF;

    RETURN NEW;
END;
$$;
