-- Restore 000003 release trigger (without rental restore)

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
           OR (NEW.status = 'CANCELLED_BY_CLIENT' AND v_starts_at > now() + interval '1 hour') THEN
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
