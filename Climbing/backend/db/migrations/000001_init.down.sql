DROP TRIGGER IF EXISTS instructor_ratings_update_aggregate ON instructor_ratings;
DROP TRIGGER IF EXISTS instructor_ratings_attended_only ON instructor_ratings;
DROP TRIGGER IF EXISTS bookings_release_slot ON bookings;
DROP TRIGGER IF EXISTS bookings_reserve_slot ON bookings;
DROP TRIGGER IF EXISTS bookings_set_slot_starts_at ON bookings;

DROP FUNCTION IF EXISTS trg_instructor_ratings_update_aggregate();
DROP FUNCTION IF EXISTS trg_instructor_ratings_attended_only();
DROP FUNCTION IF EXISTS trg_bookings_release_slot();
DROP FUNCTION IF EXISTS trg_bookings_reserve_slot();
DROP FUNCTION IF EXISTS trg_bookings_sync_slot_starts_at();

DROP TABLE IF EXISTS client_push_tokens;
DROP TABLE IF EXISTS instructor_ratings;
DROP TABLE IF EXISTS waitlist_entries;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS rental_availability;
DROP TABLE IF EXISTS slots;
DROP TABLE IF EXISTS instructors;
DROP TABLE IF EXISTS price_tariffs;
DROP TABLE IF EXISTS training_formats;
DROP TABLE IF EXISTS zones;
DROP TABLE IF EXISTS gyms;

DROP TYPE IF EXISTS push_platform;
DROP TYPE IF EXISTS waitlist_entry_status;
DROP TYPE IF EXISTS training_level;
DROP TYPE IF EXISTS equipment_mode;
DROP TYPE IF EXISTS booking_status;
DROP TYPE IF EXISTS slot_status;
