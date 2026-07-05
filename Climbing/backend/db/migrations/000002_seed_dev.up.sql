-- Dev seed: минимальные данные для локальной разработки

INSERT INTO gyms (id, name, address)
VALUES ('11111111-1111-1111-1111-111111111101', 'Вертикаль', 'ул. Примерная, 1, Воронеж');

INSERT INTO zones (id, gym_id, name, is_active)
VALUES
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'Болдеринг', TRUE),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111101', 'Трассы', TRUE);

INSERT INTO training_formats (id, zone_id, name, level, max_capacity, beginner_capacity_limit)
VALUES
    ('33333333-3333-3333-3333-333333333301', '22222222-2222-2222-2222-222222222201', 'Болдеринг для новичков', 'beginner', 16, 8),
    ('33333333-3333-3333-3333-333333333302', '22222222-2222-2222-2222-222222222202', 'Трассы для опытных', 'advanced', 16, 8);

INSERT INTO price_tariffs (format_id, training_price, shoes_rental_price, harness_rental_price)
VALUES
    ('33333333-3333-3333-3333-333333333301', 1200.00, 200.00, 300.00),
    ('33333333-3333-3333-3333-333333333302', 1500.00, 200.00, 300.00);

INSERT INTO instructors (id, full_name, photo_url, avg_rating, rating_count)
VALUES
    ('44444444-4444-4444-4444-444444444401', 'Анна К.', NULL, 4.80, 28),
    ('44444444-4444-4444-4444-444444444402', 'Игорь М.', NULL, 4.50, 15);

-- Слоты на ближайшие 7 дней (R-027)
INSERT INTO slots (id, format_id, instructor_id, starts_at, ends_at, capacity, booked_count, free_spots, status, base_price)
VALUES
    (
        '55555555-5555-5555-5555-555555555501',
        '33333333-3333-3333-3333-333333333301',
        '44444444-4444-4444-4444-444444444401',
        date_trunc('day', now() AT TIME ZONE 'UTC') + INTERVAL '1 day' + TIME '18:00',
        date_trunc('day', now() AT TIME ZONE 'UTC') + INTERVAL '1 day' + TIME '19:30',
        8, 2, 6, 'OPEN', 1200.00
    ),
    (
        '55555555-5555-5555-5555-555555555502',
        '33333333-3333-3333-3333-333333333302',
        '44444444-4444-4444-4444-444444444402',
        date_trunc('day', now() AT TIME ZONE 'UTC') + INTERVAL '2 days' + TIME '10:00',
        date_trunc('day', now() AT TIME ZONE 'UTC') + INTERVAL '2 days' + TIME '11:30',
        16, 16, 0, 'FULL', 1500.00
    );

INSERT INTO rental_availability (slot_id, shoes_available, harness_available, is_bookable)
VALUES
    ('55555555-5555-5555-5555-555555555501', 10, 8, TRUE),
    ('55555555-5555-5555-5555-555555555502', 0, 0, FALSE);
