package store

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/domain"
)

type Store struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) *Store {
	return &Store{pool: pool}
}

func (s *Store) Ping(ctx context.Context) error {
	return s.pool.Ping(ctx)
}

type SlotFilters struct {
	DateFrom      time.Time
	DateTo        time.Time
	InstructorIDs []uuid.UUID
	TimeOfDay     string
	Level         string
	Limit         int
	Offset        int
}

type ClientRow struct {
	ID         uuid.UUID
	Name       string
	Phone      string
	IsRegular  bool
}

func (s *Store) UpsertClient(ctx context.Context, name, phone string) (ClientRow, error) {
	const q = `
INSERT INTO clients (name, phone)
VALUES ($1, $2)
ON CONFLICT (phone) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = now()
RETURNING id, name, phone, is_regular`
	var row ClientRow
	err := s.pool.QueryRow(ctx, q, name, phone).Scan(&row.ID, &row.Name, &row.Phone, &row.IsRegular)
	return row, err
}

func (s *Store) GetClient(ctx context.Context, id uuid.UUID) (ClientRow, error) {
	const q = `SELECT id, name, phone, is_regular FROM clients WHERE id = $1`
	var row ClientRow
	err := s.pool.QueryRow(ctx, q, id).Scan(&row.ID, &row.Name, &row.Phone, &row.IsRegular)
	if err == pgx.ErrNoRows {
		return ClientRow{}, domain.NewAppError(domain.ErrNotFound, "Профиль не найден", 404)
	}
	return row, err
}

func (s *Store) ListSlots(ctx context.Context, f SlotFilters) ([]api.SlotSummary, int, error) {
	args := []any{f.DateFrom, f.DateTo}
	where := `s.starts_at >= $1 AND s.starts_at < ($2::date + interval '1 day')`
	argN := 3

	if len(f.InstructorIDs) > 0 {
		where += fmt.Sprintf(" AND s.instructor_id = ANY($%d)", argN)
		args = append(args, f.InstructorIDs)
		argN++
	}
	if f.Level != "" {
		where += fmt.Sprintf(" AND tf.level = $%d::training_level", argN)
		args = append(args, f.Level)
		argN++
	}
	if f.TimeOfDay != "" {
		switch f.TimeOfDay {
		case "morning":
			where += " AND EXTRACT(HOUR FROM s.starts_at AT TIME ZONE 'Europe/Moscow') BETWEEN 6 AND 11"
		case "afternoon":
			where += " AND EXTRACT(HOUR FROM s.starts_at AT TIME ZONE 'Europe/Moscow') BETWEEN 12 AND 16"
		case "evening":
			where += " AND EXTRACT(HOUR FROM s.starts_at AT TIME ZONE 'Europe/Moscow') BETWEEN 17 AND 23"
		}
	}

	countQ := `SELECT COUNT(*) FROM slots s
JOIN training_formats tf ON tf.id = s.format_id
WHERE ` + where
	var total int
	if err := s.pool.QueryRow(ctx, countQ, args...).Scan(&total); err != nil {
		return nil, 0, err
	}

	listQ := `
SELECT s.id, s.starts_at, tf.name, z.name,
       i.id, i.full_name, i.photo_url, i.avg_rating,
       s.free_spots, s.capacity, s.base_price, s.status::text,
       COALESCE(ra.is_bookable, true) AND s.status = 'OPEN' AND s.free_spots > 0 AS is_bookable
FROM slots s
JOIN training_formats tf ON tf.id = s.format_id
JOIN zones z ON z.id = tf.zone_id
JOIN instructors i ON i.id = s.instructor_id
LEFT JOIN rental_availability ra ON ra.slot_id = s.id
WHERE ` + where + `
ORDER BY s.starts_at ASC
LIMIT $` + fmt.Sprint(argN) + ` OFFSET $` + fmt.Sprint(argN+1)
	args = append(args, f.Limit, f.Offset)

	rows, err := s.pool.Query(ctx, listQ, args...)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	var items []api.SlotSummary
	for rows.Next() {
		var item api.SlotSummary
		var rating *float64
		var photo *string
		var price float64
		if err := rows.Scan(
			&item.ID, &item.StartAt, &item.Format, &item.Zone,
			&item.Instructor.ID, &item.Instructor.Name, &photo, &rating,
			&item.FreeSpots, &item.Capacity, &price, &item.Status, &item.IsBookable,
		); err != nil {
			return nil, 0, err
		}
		item.Instructor.AvatarURL = photo
		item.Instructor.Rating = rating
		item.Price = &price
		items = append(items, item)
	}
	if items == nil {
		items = []api.SlotSummary{}
	}
	return items, total, rows.Err()
}

func (s *Store) GetSlotDetail(ctx context.Context, slotID uuid.UUID) (api.SlotDetail, error) {
	const q = `
SELECT s.id, s.starts_at, s.ends_at, tf.id, tf.name, tf.level::text, z.name,
       i.id, i.full_name, i.photo_url, i.avg_rating, i.rating_count,
       s.free_spots, s.capacity, s.base_price, s.status::text,
       COALESCE(ra.shoes_available, 0), COALESCE(ra.harness_available, 0), COALESCE(ra.is_bookable, true),
       pt.training_price, pt.shoes_rental_price, pt.harness_rental_price,
       g.id, g.name, g.address
FROM slots s
JOIN training_formats tf ON tf.id = s.format_id
JOIN zones z ON z.id = tf.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gyms g ON g.id = z.gym_id
LEFT JOIN rental_availability ra ON ra.slot_id = s.id
LEFT JOIN price_tariffs pt ON pt.format_id = tf.id
WHERE s.id = $1`
	var d api.SlotDetail
	var photo *string
	var rating float64
	var ratingCount int
	var gymID uuid.UUID
	err := s.pool.QueryRow(ctx, q, slotID).Scan(
		&d.ID, &d.StartAt, &d.EndAt, &d.FormatInfo.ID, &d.FormatInfo.Name, &d.FormatInfo.Level, &d.ZoneName,
		&d.Instructor.ID, &d.Instructor.Name, &photo, &rating, &ratingCount,
		&d.FreeSpots, &d.Capacity, &d.BasePrice, &d.Status,
		&d.RentalAvailability.ShoesAvailable, &d.RentalAvailability.HarnessAvailable, &d.RentalAvailability.IsBookable,
		&d.PriceBreakdown.TrainingPrice, &d.PriceBreakdown.ShoesRentalPrice, &d.PriceBreakdown.HarnessRentalPrice,
		&gymID, &d.Gym.Name, &d.Gym.Address,
	)
	if err == pgx.ErrNoRows {
		return api.SlotDetail{}, domain.NewAppError(domain.ErrNotFound, "Слот не найден", 404)
	}
	if err != nil {
		return api.SlotDetail{}, err
	}
	d.Gym.ID = gymID.String()
	d.Format = d.FormatInfo.Name
	d.Zone = d.ZoneName
	d.Instructor.PhotoURL = photo
	d.Instructor.AvatarURL = photo
	d.Instructor.Rating = &rating
	d.Instructor.RatingCount = ratingCount
	d.Price = &d.BasePrice
	d.DurationMinutes = int(d.EndAt.Sub(d.StartAt).Minutes())
	d.RentalAvailable = d.RentalAvailability.IsBookable
	d.IsBookable = d.Status == "OPEN" && d.FreeSpots > 0 && d.RentalAvailability.IsBookable
	d.PriceBreakdown.TotalPrice = d.PriceBreakdown.TrainingPrice
	return d, nil
}

func (s *Store) ListInstructors(ctx context.Context) ([]api.InstructorSummary, error) {
	const q = `SELECT id, full_name, photo_url, avg_rating FROM instructors ORDER BY full_name`
	rows, err := s.pool.Query(ctx, q)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var items []api.InstructorSummary
	for rows.Next() {
		var item api.InstructorSummary
		var photo *string
		var rating float64
		if err := rows.Scan(&item.ID, &item.Name, &photo, &rating); err != nil {
			return nil, err
		}
		item.AvatarURL = photo
		item.Rating = &rating
		items = append(items, item)
	}
	if items == nil {
		items = []api.InstructorSummary{}
	}
	return items, rows.Err()
}

func CalcTotalPrice(pb api.PriceBreakdown, eq api.EquipmentChoice) float64 {
	total := pb.TrainingPrice
	if eq.Mode == "RENTAL" {
		if eq.RentalShoes {
			total += pb.ShoesRentalPrice
		}
		if eq.RentalHarness {
			total += pb.HarnessRentalPrice
		}
	}
	return total
}

func (s *Store) CreateBooking(ctx context.Context, clientID uuid.UUID, slotID uuid.UUID, eq api.EquipmentChoice) (api.Booking, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return api.Booking{}, err
	}
	defer tx.Rollback(ctx)

	detail, err := getSlotForUpdate(ctx, tx, slotID)
	if err != nil {
		return api.Booking{}, err
	}

	if detail.Status == "CANCELLED" {
		return api.Booking{}, domain.NewAppError(domain.ErrSlotCancelled, "Тренировка отменена", 409)
	}
	if err := checkRebookForbidden(ctx, tx, clientID, slotID); err != nil {
		return api.Booking{}, err
	}
	if detail.FreeSpots <= 0 {
		return api.Booking{}, domain.NewAppError(domain.ErrNoSpots, "Места закончились", 409)
	}
	if eq.Mode == "RENTAL" {
		if !detail.RentalAvailability.IsBookable {
			return api.Booking{}, domain.NewAppError(domain.ErrRentalUnavailable, "Прокат закончился", 409)
		}
		if eq.RentalShoes && detail.RentalAvailability.ShoesAvailable <= 0 {
			return api.Booking{}, domain.NewAppError(domain.ErrRentalUnavailable, "Прокат закончился", 409)
		}
		if eq.RentalHarness && detail.RentalAvailability.HarnessAvailable <= 0 {
			return api.Booking{}, domain.NewAppError(domain.ErrRentalUnavailable, "Прокат закончился", 409)
		}
	}

	if err := checkOneLiveBookingPerDay(ctx, tx, clientID, detail.StartAt); err != nil {
		return api.Booking{}, err
	}

	total := CalcTotalPrice(detail.PriceBreakdown, eq)
	const ins = `
INSERT INTO bookings (client_id, slot_id, status, equipment_mode, rental_shoes, rental_harness, total_price)
VALUES ($1, $2, 'ACTIVE', $3::equipment_mode, $4, $5, $6)
RETURNING id, created_at`
	var booking api.Booking
	booking.SlotID = slotID.String()
	booking.Status = "ACTIVE"
	booking.Equipment = eq
	booking.TotalPrice = total
	err = tx.QueryRow(ctx, ins, clientID, slotID, eq.Mode, eq.RentalShoes, eq.RentalHarness, total).
		Scan(&booking.ID, &booking.CreatedAt)
	if err != nil {
		if isUniqueViolation(err, "uq_bookings_one_active_per_day") {
			return api.Booking{}, domain.NewAppError(domain.ErrOneBookingPerDay, "Уже есть запись на этот день", 409)
		}
		if isUniqueViolation(err, "uq_bookings_client_slot_live") {
			return api.Booking{}, domain.NewAppError(domain.ErrNoSpots, "Вы уже записаны на этот слот", 409)
		}
		if isCheckViolation(err) {
			return api.Booking{}, mapCheckViolation(err)
		}
		return api.Booking{}, err
	}

	if err := tx.Commit(ctx); err != nil {
		return api.Booking{}, err
	}
	return booking, nil
}

func getSlotForUpdate(ctx context.Context, tx pgx.Tx, slotID uuid.UUID) (api.SlotDetail, error) {
	const q = `
SELECT s.id, s.starts_at, s.ends_at, s.free_spots, s.capacity, s.base_price, s.status::text,
       COALESCE(ra.shoes_available, 0), COALESCE(ra.harness_available, 0), COALESCE(ra.is_bookable, true),
       COALESCE(pt.training_price, s.base_price), COALESCE(pt.shoes_rental_price, 0), COALESCE(pt.harness_rental_price, 0)
FROM slots s
LEFT JOIN rental_availability ra ON ra.slot_id = s.id
LEFT JOIN price_tariffs pt ON pt.format_id = s.format_id
WHERE s.id = $1
FOR UPDATE OF s`
	var d api.SlotDetail
	err := tx.QueryRow(ctx, q, slotID).Scan(
		&d.ID, &d.StartAt, &d.EndAt, &d.FreeSpots, &d.Capacity, &d.BasePrice, &d.Status,
		&d.RentalAvailability.ShoesAvailable, &d.RentalAvailability.HarnessAvailable, &d.RentalAvailability.IsBookable,
		&d.PriceBreakdown.TrainingPrice, &d.PriceBreakdown.ShoesRentalPrice, &d.PriceBreakdown.HarnessRentalPrice,
	)
	if err == pgx.ErrNoRows {
		return api.SlotDetail{}, domain.NewAppError(domain.ErrNotFound, "Слот не найден", 404)
	}
	return d, err
}

func checkRebookForbidden(ctx context.Context, tx pgx.Tx, clientID, slotID uuid.UUID) error {
	const q = `
SELECT 1 FROM bookings
WHERE client_id = $1 AND slot_id = $2 AND status = 'CANCELLED_BY_GYM'
LIMIT 1`
	var one int
	err := tx.QueryRow(ctx, q, clientID, slotID).Scan(&one)
	if err == nil {
		return domain.NewAppError(domain.ErrSlotRebookForbidden, "Запись на этот слот недоступна", 403)
	}
	if err != pgx.ErrNoRows {
		return err
	}
	return nil
}

func checkOneLiveBookingPerDay(ctx context.Context, tx pgx.Tx, clientID uuid.UUID, startsAt time.Time) error {
	const q = `
SELECT 1 FROM bookings
WHERE client_id = $1
  AND status IN ('ACTIVE', 'WAITLIST')
  AND (slot_starts_at AT TIME ZONE 'Europe/Moscow')::date = ($2 AT TIME ZONE 'Europe/Moscow')::date
LIMIT 1`
	var one int
	err := tx.QueryRow(ctx, q, clientID, startsAt).Scan(&one)
	if err == nil {
		return domain.NewAppError(domain.ErrOneBookingPerDay, "Уже есть запись на этот день", 409)
	}
	if err != pgx.ErrNoRows {
		return err
	}
	return nil
}

func isEarlyCancel(startsAt, now time.Time) bool {
	return startsAt.Sub(now) >= time.Hour
}

func notifyFirstWaitlist(ctx context.Context, tx pgx.Tx, slotID uuid.UUID) error {
	const q = `
UPDATE waitlist_entries
   SET status = 'NOTIFIED', updated_at = now()
 WHERE id = (
     SELECT id
       FROM waitlist_entries
      WHERE slot_id = $1 AND status = 'WAITING'
      ORDER BY position ASC, created_at ASC
      LIMIT 1
      FOR UPDATE
 )
RETURNING id, client_id`
	var entryID, clientID uuid.UUID
	err := tx.QueryRow(ctx, q, slotID).Scan(&entryID, &clientID)
	if err == pgx.ErrNoRows {
		return nil
	}
	if err != nil {
		return err
	}
	slog.Info("waitlist spot available",
		"slot_id", slotID,
		"waitlist_entry_id", entryID,
		"client_id", clientID,
	)
	return nil
}

func cancelWaitlistBooking(ctx context.Context, tx pgx.Tx, clientID, slotID uuid.UUID) error {
	_, err := tx.Exec(ctx, `
UPDATE bookings
   SET status = 'CANCELLED_BY_CLIENT', cancelled_at = now()
 WHERE client_id = $1 AND slot_id = $2 AND status = 'WAITLIST'`, clientID, slotID)
	return err
}

func checkOneBookingPerDay(ctx context.Context, tx pgx.Tx, clientID uuid.UUID, startsAt time.Time) error {
	const q = `
SELECT 1 FROM bookings
WHERE client_id = $1
  AND status = 'ACTIVE'
  AND (slot_starts_at AT TIME ZONE 'Europe/Moscow')::date = ($2 AT TIME ZONE 'Europe/Moscow')::date
LIMIT 1`
	var one int
	err := tx.QueryRow(ctx, q, clientID, startsAt).Scan(&one)
	if err == nil {
		return domain.NewAppError(domain.ErrOneBookingPerDay, "Уже есть запись на этот день", 409)
	}
	if err != pgx.ErrNoRows {
		return err
	}
	return nil
}

func (s *Store) ListBookings(ctx context.Context, clientID uuid.UUID, status string, limit, offset int) ([]api.BookingSummary, int, error) {
	where := `b.client_id = $1`
	args := []any{clientID}
	if status != "" {
		where += ` AND b.status = $2::booking_status`
		args = append(args, status)
	}
	countQ := `SELECT COUNT(*) FROM bookings b WHERE ` + where
	var total int
	if err := s.pool.QueryRow(ctx, countQ, args...).Scan(&total); err != nil {
		return nil, 0, err
	}

	limitArg := len(args) + 1
	offsetArg := len(args) + 2
	listQ := `
SELECT b.id, b.status::text, b.total_price,
       s.starts_at, tf.name, z.name, i.full_name,
       w.position
FROM bookings b
JOIN slots s ON s.id = b.slot_id
JOIN training_formats tf ON tf.id = s.format_id
JOIN zones z ON z.id = tf.zone_id
JOIN instructors i ON i.id = s.instructor_id
LEFT JOIN waitlist_entries w
       ON w.client_id = b.client_id AND w.slot_id = b.slot_id
      AND w.status IN ('WAITING', 'NOTIFIED')
WHERE ` + where + `
ORDER BY s.starts_at DESC
LIMIT $` + fmt.Sprint(limitArg) + ` OFFSET $` + fmt.Sprint(offsetArg)
	args = append(args, limit, offset)

	rows, err := s.pool.Query(ctx, listQ, args...)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	var items []api.BookingSummary
	for rows.Next() {
		var item api.BookingSummary
		var waitlistPos *int
		if err := rows.Scan(
			&item.ID, &item.Status, &item.TotalPrice,
			&item.Slot.StartsAt, &item.Slot.Format.Name, &item.Slot.Zone.Name, &item.Slot.Instructor.FullName,
			&waitlistPos,
		); err != nil {
			return nil, 0, err
		}
		item.WaitlistPosition = waitlistPos
		items = append(items, item)
	}
	if items == nil {
		items = []api.BookingSummary{}
	}
	return items, total, rows.Err()
}

func (s *Store) GetBooking(ctx context.Context, clientID, bookingID uuid.UUID) (api.Booking, error) {
	const q = `
SELECT b.id, b.slot_id, b.status::text, b.equipment_mode::text, b.rental_shoes, b.rental_harness,
       b.total_price, b.cancellation_reason, b.cancelled_at, b.created_at,
       s.starts_at, s.ends_at, tf.id, tf.name, tf.level::text, z.id, z.name,
       i.id, i.full_name, i.photo_url, i.avg_rating, i.rating_count,
       g.name, g.address,
       ir.id, ir.stars, ir.created_at,
       COALESCE(pt.training_price, s.base_price), COALESCE(pt.shoes_rental_price, 0), COALESCE(pt.harness_rental_price, 0),
       w.position
FROM bookings b
JOIN slots s ON s.id = b.slot_id
JOIN training_formats tf ON tf.id = s.format_id
JOIN zones z ON z.id = tf.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gyms g ON g.id = z.gym_id
LEFT JOIN price_tariffs pt ON pt.format_id = s.format_id
LEFT JOIN instructor_ratings ir ON ir.booking_id = b.id
LEFT JOIN waitlist_entries w ON w.client_id = b.client_id AND w.slot_id = b.slot_id AND w.status IN ('WAITING', 'NOTIFIED')
WHERE b.id = $1 AND b.client_id = $2`
	var booking api.Booking
	booking.Slot = &api.SlotRef{}
	booking.Gym = &api.GymInfo{}
	var ratingID *uuid.UUID
	var stars *int
	var ratingAt *time.Time
	var photo *string
	var rating float64
	var trainingPrice, shoesPrice, harnessPrice float64
	var waitlistPos *int
	err := s.pool.QueryRow(ctx, q, bookingID, clientID).Scan(
		&booking.ID, &booking.SlotID, &booking.Status,
		&booking.Equipment.Mode, &booking.Equipment.RentalShoes, &booking.Equipment.RentalHarness,
		&booking.TotalPrice, &booking.CancellationReason, &booking.CancelledAt, &booking.CreatedAt,
		&booking.Slot.StartsAt, &booking.Slot.EndsAt,
		&booking.Slot.Format.ID, &booking.Slot.Format.Name, &booking.Slot.Format.Level,
		&booking.Slot.Zone.ID, &booking.Slot.Zone.Name,
		&booking.Slot.Instructor.ID, &booking.Slot.Instructor.Name, &photo, &rating, &booking.Slot.Instructor.RatingCount,
		&booking.Gym.Name, &booking.Gym.Address,
		&ratingID, &stars, &ratingAt,
		&trainingPrice, &shoesPrice, &harnessPrice,
		&waitlistPos,
	)
	if err == pgx.ErrNoRows {
		return api.Booking{}, domain.NewAppError(domain.ErrNotFound, "Бронь не найдена", 404)
	}
	if err != nil {
		return api.Booking{}, err
	}
	booking.Slot.ID = booking.SlotID
	booking.Slot.Instructor.PhotoURL = photo
	booking.Slot.Instructor.Rating = &rating
	if ratingID != nil && stars != nil && ratingAt != nil {
		booking.InstructorRating = &api.InstructorRating{
			ID:        ratingID.String(),
			Stars:     *stars,
			CreatedAt: *ratingAt,
		}
	}
	pb := api.PriceBreakdown{
		TrainingPrice:      trainingPrice,
		ShoesRentalPrice:   shoesPrice,
		HarnessRentalPrice: harnessPrice,
	}
	pb.TotalPrice = CalcTotalPrice(pb, booking.Equipment)
	booking.PriceBreakdown = &pb
	if waitlistPos != nil {
		booking.WaitlistPosition = waitlistPos
	}
	return booking, nil
}

func (s *Store) CancelBooking(ctx context.Context, clientID, bookingID uuid.UUID) (api.CancelBookingResponse, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return api.CancelBookingResponse{}, err
	}
	defer tx.Rollback(ctx)

	const sel = `
SELECT b.id, b.status::text, s.starts_at, b.slot_id
FROM bookings b
JOIN slots s ON s.id = b.slot_id
WHERE b.id = $1 AND b.client_id = $2
FOR UPDATE OF b`
	var id uuid.UUID
	var slotID uuid.UUID
	var status string
	var startsAt time.Time
	err = tx.QueryRow(ctx, sel, bookingID, clientID).Scan(&id, &status, &startsAt, &slotID)
	if err == pgx.ErrNoRows {
		return api.CancelBookingResponse{}, domain.NewAppError(domain.ErrNotFound, "Бронь не найдена", 404)
	}
	if err != nil {
		return api.CancelBookingResponse{}, err
	}
	if status != "ACTIVE" {
		return api.CancelBookingResponse{}, domain.NewAppError(domain.ErrAlreadyCancelled, "Запись уже отменена", 409)
	}
	if !startsAt.After(time.Now()) {
		return api.CancelBookingResponse{}, domain.NewAppError(domain.ErrCancelTooLate, "Отмена недоступна", 403)
	}

	const upd = `
UPDATE bookings SET status = 'CANCELLED_BY_CLIENT', cancelled_at = now()
WHERE id = $1
RETURNING id, cancelled_at`
	var resp api.CancelBookingResponse
	resp.Status = "CANCELLED_BY_CLIENT"
	err = tx.QueryRow(ctx, upd, bookingID).Scan(&resp.ID, &resp.CancelledAt)
	if err != nil {
		return api.CancelBookingResponse{}, err
	}
	if isEarlyCancel(startsAt, time.Now()) {
		if err := notifyFirstWaitlist(ctx, tx, slotID); err != nil {
			return api.CancelBookingResponse{}, err
		}
	}
	if err := tx.Commit(ctx); err != nil {
		return api.CancelBookingResponse{}, err
	}
	return resp, nil
}

func (s *Store) JoinWaitlist(ctx context.Context, clientID, slotID uuid.UUID) (api.WaitlistEntry, error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return api.WaitlistEntry{}, err
	}
	defer tx.Rollback(ctx)

	detail, err := getSlotForUpdate(ctx, tx, slotID)
	if err != nil {
		return api.WaitlistEntry{}, err
	}
	if detail.Status == "CANCELLED" {
		return api.WaitlistEntry{}, domain.NewAppError(domain.ErrSlotCancelled, "Тренировка отменена", 409)
	}
	if detail.FreeSpots > 0 {
		return api.WaitlistEntry{}, domain.NewAppError(domain.ErrValidation, "На слот есть свободные места", 409)
	}
	if err := checkOneLiveBookingPerDay(ctx, tx, clientID, detail.StartAt); err != nil {
		return api.WaitlistEntry{}, err
	}

	const posQ = `SELECT COALESCE(MAX(position), 0) + 1 FROM waitlist_entries WHERE slot_id = $1 AND status = 'WAITING'`
	var position int
	if err := tx.QueryRow(ctx, posQ, slotID).Scan(&position); err != nil {
		return api.WaitlistEntry{}, err
	}

	const ins = `
INSERT INTO waitlist_entries (client_id, slot_id, position, status)
VALUES ($1, $2, $3, 'WAITING')
RETURNING id, created_at`
	var entry api.WaitlistEntry
	entry.SlotID = slotID.String()
	entry.Position = position
	entry.Status = "WAITING"
	err = tx.QueryRow(ctx, ins, clientID, slotID, position).Scan(&entry.ID, &entry.CreatedAt)
	if err != nil {
		if isUniqueViolation(err, "uq_waitlist_client_slot_active") {
			return api.WaitlistEntry{}, domain.NewAppError(domain.ErrAlreadyInWaitlist, "Вы уже в листе ожидания на этот слот", 409)
		}
		return api.WaitlistEntry{}, err
	}

	const bookingIns = `
INSERT INTO bookings (client_id, slot_id, status, equipment_mode, rental_shoes, rental_harness, total_price)
VALUES ($1, $2, 'WAITLIST', 'OWN', false, false, 0)`
	if _, err := tx.Exec(ctx, bookingIns, clientID, slotID); err != nil {
		if isUniqueViolation(err, "uq_bookings_client_slot_live") {
			return api.WaitlistEntry{}, domain.NewAppError(domain.ErrAlreadyInWaitlist, "Вы уже в листе ожидания на этот слот", 409)
		}
		return api.WaitlistEntry{}, err
	}

	if err := tx.Commit(ctx); err != nil {
		return api.WaitlistEntry{}, err
	}
	return entry, nil
}

func (s *Store) GetWaitlistEntry(ctx context.Context, clientID, entryID uuid.UUID) (api.WaitlistEntry, error) {
	const q = `
SELECT w.id, w.slot_id, w.position, w.status::text, w.created_at
FROM waitlist_entries w
WHERE w.id = $1 AND w.client_id = $2`
	var entry api.WaitlistEntry
	err := s.pool.QueryRow(ctx, q, entryID, clientID).Scan(
		&entry.ID, &entry.SlotID, &entry.Position, &entry.Status, &entry.CreatedAt,
	)
	if err == pgx.ErrNoRows {
		return api.WaitlistEntry{}, domain.NewAppError(domain.ErrWaitlistNotFound, "Запись не найдена", 404)
	}
	return entry, err
}

func (s *Store) DeleteWaitlistEntry(ctx context.Context, clientID, entryID uuid.UUID) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	const sel = `SELECT slot_id FROM waitlist_entries WHERE id = $1 AND client_id = $2 AND status IN ('WAITING', 'NOTIFIED')`
	var slotID uuid.UUID
	err = tx.QueryRow(ctx, sel, entryID, clientID).Scan(&slotID)
	if err == pgx.ErrNoRows {
		return domain.NewAppError(domain.ErrWaitlistNotFound, "Запись не найдена", 404)
	}
	if err != nil {
		return err
	}

	const q = `
UPDATE waitlist_entries SET status = 'LEFT', updated_at = now()
WHERE id = $1 AND client_id = $2 AND status IN ('WAITING', 'NOTIFIED')`
	ct, err := tx.Exec(ctx, q, entryID, clientID)
	if err != nil {
		return err
	}
	if ct.RowsAffected() == 0 {
		return domain.NewAppError(domain.ErrWaitlistNotFound, "Запись не найдена", 404)
	}
	if err := cancelWaitlistBooking(ctx, tx, clientID, slotID); err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func (s *Store) CreateRating(ctx context.Context, clientID uuid.UUID, req api.CreateRatingRequest) (api.CreateRatingResponse, error) {
	instructorID, err := uuid.Parse(req.InstructorID)
	if err != nil {
		return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrValidation, "Некорректный instructorId", 400)
	}
	bookingID, err := uuid.Parse(req.BookingID)
	if err != nil {
		return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrValidation, "Некорректный bookingId", 400)
	}

	const check = `
SELECT b.status::text, s.instructor_id
FROM bookings b
JOIN slots s ON s.id = b.slot_id
WHERE b.id = $1 AND b.client_id = $2`
	var status string
	var slotInstructor uuid.UUID
	err = s.pool.QueryRow(ctx, check, bookingID, clientID).Scan(&status, &slotInstructor)
	if err == pgx.ErrNoRows {
		return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrNotFound, "Бронь не найдена", 404)
	}
	if err != nil {
		return api.CreateRatingResponse{}, err
	}
	if status != "ATTENDED" {
		return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrBookingNotAttended, "Оценить можно после тренировки", 403)
	}
	if slotInstructor != instructorID {
		return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrValidation, "Инструктор не совпадает со слотом", 400)
	}

	const ins = `
INSERT INTO instructor_ratings (booking_id, instructor_id, client_id, stars)
VALUES ($1, $2, $3, $4)
RETURNING id, created_at`
	var resp api.CreateRatingResponse
	resp.BookingID = req.BookingID
	resp.InstructorID = req.InstructorID
	resp.Stars = req.Stars
	err = s.pool.QueryRow(ctx, ins, bookingID, instructorID, clientID, req.Stars).Scan(&resp.ID, &resp.CreatedAt)
	if err != nil {
		if isUniqueViolation(err, "uq_instructor_ratings_booking") {
			return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrAlreadyRated, "Вы уже оценили этого инструктора", 409)
		}
		if isCheckViolation(err) {
			return api.CreateRatingResponse{}, domain.NewAppError(domain.ErrBookingNotAttended, "Оценить можно после тренировки", 403)
		}
		return api.CreateRatingResponse{}, err
	}
	return resp, nil
}

func (s *Store) FindActiveWaitlistEntry(ctx context.Context, clientID, slotID uuid.UUID) (uuid.UUID, error) {
	const q = `
SELECT id FROM waitlist_entries
WHERE client_id = $1 AND slot_id = $2 AND status IN ('WAITING', 'NOTIFIED')
LIMIT 1`
	var id uuid.UUID
	err := s.pool.QueryRow(ctx, q, clientID, slotID).Scan(&id)
	if err == pgx.ErrNoRows {
		return uuid.Nil, domain.NewAppError(domain.ErrWaitlistNotFound, "Запись не найдена", 404)
	}
	return id, err
}

func (s *Store) RegisterPushToken(ctx context.Context, clientID uuid.UUID, token, platform string) error {
	const q = `
INSERT INTO client_push_tokens (client_id, token, platform)
VALUES ($1, $2, $3::push_platform)
ON CONFLICT (client_id, token) DO UPDATE SET updated_at = now(), platform = EXCLUDED.platform`
	_, err := s.pool.Exec(ctx, q, clientID, token, platform)
	return err
}

func (s *Store) MarkAttended(ctx context.Context, bookingID uuid.UUID) error {
	ct, err := s.pool.Exec(ctx, `UPDATE bookings SET status = 'ATTENDED' WHERE id = $1 AND status = 'ACTIVE'`, bookingID)
	if err != nil {
		return err
	}
	if ct.RowsAffected() == 0 {
		return domain.NewAppError(domain.ErrNotFound, "Бронь не найдена или не активна", 404)
	}
	return nil
}

func (s *Store) CancelSlotByGym(ctx context.Context, slotID uuid.UUID, reason string) error {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx, `UPDATE slots SET status = 'CANCELLED', updated_at = now() WHERE id = $1`, slotID)
	if err != nil {
		return err
	}
	_, err = tx.Exec(ctx, `
UPDATE bookings SET status = 'CANCELLED_BY_GYM', cancellation_reason = $2, cancelled_at = now()
WHERE slot_id = $1 AND status = 'ACTIVE'`, slotID, reason)
	if err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func isUniqueViolation(err error, constraint string) bool {
	var pgErr *pgconn.PgError
	if !errors.As(err, &pgErr) {
		return false
	}
	return pgErr.Code == "23505" && (constraint == "" || pgErr.ConstraintName == constraint)
}

func isCheckViolation(err error) bool {
	var pgErr *pgconn.PgError
	if !errors.As(err, &pgErr) {
		return false
	}
	return pgErr.Code == "23514"
}

func mapCheckViolation(err error) *domain.AppError {
	msg := strings.ToLower(err.Error())
	switch {
	case strings.Contains(msg, "slot cancelled"):
		return domain.NewAppError(domain.ErrSlotCancelled, "Тренировка отменена", 409)
	case strings.Contains(msg, "rental unavailable"):
		return domain.NewAppError(domain.ErrRentalUnavailable, "Прокат закончился", 409)
	default:
		return domain.NewAppError(domain.ErrNoSpots, "Места закончились", 409)
	}
}
