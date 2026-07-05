-- Сброс тестовых слотов к seed-состоянию (только dev UUID из 000002_seed_dev.up.sql)
DELETE FROM instructor_ratings
WHERE booking_id IN (
    SELECT id FROM bookings
    WHERE slot_id IN (
        '55555555-5555-5555-5555-555555555501',
        '55555555-5555-5555-5555-555555555502'
    )
);

DELETE FROM waitlist_entries
WHERE slot_id IN (
    '55555555-5555-5555-5555-555555555501',
    '55555555-5555-5555-5555-555555555502'
);

DELETE FROM bookings
WHERE slot_id IN (
    '55555555-5555-5555-5555-555555555501',
    '55555555-5555-5555-5555-555555555502'
);

UPDATE slots
SET booked_count = 2,
    free_spots   = 6,
    status       = 'OPEN'
WHERE id = '55555555-5555-5555-5555-555555555501';

UPDATE slots
SET booked_count = 16,
    free_spots   = 0,
    status       = 'FULL'
WHERE id = '55555555-5555-5555-5555-555555555502';

UPDATE rental_availability
SET shoes_available = 10, harness_available = 8, is_bookable = TRUE
WHERE slot_id = '55555555-5555-5555-5555-555555555501';

UPDATE rental_availability
SET shoes_available = 0, harness_available = 0, is_bookable = FALSE
WHERE slot_id = '55555555-5555-5555-5555-555555555502';
