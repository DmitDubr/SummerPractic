package handler_test

import (
	"bytes"
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/dimbass/summerpractic/climbing/backend/internal/config"
	"github.com/dimbass/summerpractic/climbing/backend/internal/handler"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/auth"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store/testutil"
)

func TestProfileAuthIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	patchBody := map[string]string{"name": "Анна", "phone": "+79005556677"}
	patchRec := patchJSON(t, router, "/v1/profile", patchBody, "")
	if patchRec.Code != http.StatusOK {
		t.Fatalf("patch status = %d, body = %s", patchRec.Code, patchRec.Body.String())
	}

	var patched struct {
		ID           string `json:"id"`
		Name         string `json:"name"`
		Phone        string `json:"phone"`
		SessionToken string `json:"sessionToken"`
	}
	if err := json.Unmarshal(patchRec.Body.Bytes(), &patched); err != nil {
		t.Fatalf("decode patch: %v", err)
	}
	if patched.SessionToken == "" {
		t.Fatal("expected sessionToken from PATCH /profile")
	}
	if patched.Name != "Анна" || patched.Phone != "+79005556677" {
		t.Fatalf("unexpected profile: %+v", patched)
	}

	getRec := getAuth(t, router, "/v1/profile", patched.SessionToken)
	if getRec.Code != http.StatusOK {
		t.Fatalf("get profile status = %d, body = %s", getRec.Code, getRec.Body.String())
	}

	var profile struct {
		ID    string `json:"id"`
		Name  string `json:"name"`
		Phone string `json:"phone"`
	}
	if err := json.Unmarshal(getRec.Body.Bytes(), &profile); err != nil {
		t.Fatalf("decode profile: %v", err)
	}
	if profile.ID != patched.ID || profile.Name != patched.Name || profile.Phone != patched.Phone {
		t.Fatalf("profile mismatch: patch=%+v get=%+v", patched, profile)
	}
}

func TestListSlotsIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/v1/slots", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("list slots status = %d, body = %s", rec.Code, rec.Body.String())
	}

	var resp struct {
		Items []struct {
			ID        string `json:"id"`
			FreeSpots int    `json:"freeSpots"`
			Capacity  int    `json:"capacity"`
			Status    string `json:"status"`
		} `json:"items"`
		Meta struct {
			Total int `json:"total"`
		} `json:"meta"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &resp); err != nil {
		t.Fatalf("decode slots: %v", err)
	}
	if len(resp.Items) < 1 {
		t.Fatalf("expected at least 1 slot from seed, got %d", len(resp.Items))
	}
	if resp.Meta.Total < 1 {
		t.Fatalf("expected meta.total >= 1, got %d", resp.Meta.Total)
	}
	for _, slot := range resp.Items {
		if slot.ID == "" || slot.Capacity <= 0 {
			t.Fatalf("invalid slot: %+v", slot)
		}
	}
}

func TestBookingFlowIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	cfg := config.Config{DevMode: true}
	h := handler.New(st, tokens, cfg)
	router := handler.NewRouter(h, tokens, slog.Default())

	createBody := map[string]any{
		"slotId": "55555555-5555-5555-5555-555555555501",
		"client": map[string]string{"name": "Иван", "phone": "+79001234567"},
		"equipment": map[string]any{
			"mode": "OWN", "rentalShoes": false, "rentalHarness": false,
		},
	}
	createRec := postJSON(t, router, "/v1/bookings", createBody, "")
	if createRec.Code != http.StatusCreated {
		t.Fatalf("create status = %d, body = %s", createRec.Code, createRec.Body.String())
	}

	var created struct {
		ID           string `json:"id"`
		SessionToken string `json:"sessionToken"`
	}
	if err := json.Unmarshal(createRec.Body.Bytes(), &created); err != nil {
		t.Fatalf("decode create: %v", err)
	}
	if created.SessionToken == "" {
		t.Fatal("expected sessionToken")
	}

	listRec := getAuth(t, router, "/v1/bookings", created.SessionToken)
	if listRec.Code != http.StatusOK {
		t.Fatalf("list status = %d", listRec.Code)
	}

	cancelRec := postJSON(t, router, "/v1/bookings/"+created.ID+"/cancel", map[string]any{}, created.SessionToken)
	if cancelRec.Code != http.StatusOK {
		t.Fatalf("cancel status = %d, body = %s", cancelRec.Code, cancelRec.Body.String())
	}
}

func TestJoinWaitlistIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	body := map[string]any{
		"client": map[string]string{"name": "Мария", "phone": "+79007654321"},
	}
	rec := postJSON(t, router, "/v1/slots/55555555-5555-5555-5555-555555555502/waitlist", body, "")
	if rec.Code != http.StatusCreated {
		t.Fatalf("waitlist status = %d, body = %s", rec.Code, rec.Body.String())
	}

	var waitlistCount int
	err = pool.QueryRow(ctx, `
SELECT COUNT(*) FROM bookings b
JOIN clients c ON c.id = b.client_id
WHERE c.phone = '+79007654321' AND b.status = 'WAITLIST'`).Scan(&waitlistCount)
	if err != nil {
		t.Fatalf("count waitlist bookings: %v", err)
	}
	if waitlistCount != 1 {
		t.Fatalf("expected 1 WAITLIST booking, got %d", waitlistCount)
	}
}

func TestOneBookingPerDayAfterWaitlistIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	waitlistBody := map[string]any{
		"client": map[string]string{"name": "Олег", "phone": "+79005550101"},
	}
	waitRec := postJSON(t, router, "/v1/slots/55555555-5555-5555-5555-555555555502/waitlist", waitlistBody, "")
	if waitRec.Code != http.StatusCreated {
		t.Fatalf("waitlist status = %d, body = %s", waitRec.Code, waitRec.Body.String())
	}

	bookBody := map[string]any{
		"slotId": "55555555-5555-5555-5555-555555555501",
		"client": map[string]string{"name": "Олег", "phone": "+79005550101"},
		"equipment": map[string]any{
			"mode": "OWN", "rentalShoes": false, "rentalHarness": false,
		},
	}
	bookRec := postJSON(t, router, "/v1/bookings", bookBody, "")
	if bookRec.Code != http.StatusConflict {
		t.Fatalf("create status = %d, want 409, body = %s", bookRec.Code, bookRec.Body.String())
	}
	var errBody struct {
		Code string `json:"code"`
	}
	if err := json.Unmarshal(bookRec.Body.Bytes(), &errBody); err != nil {
		t.Fatalf("decode error: %v", err)
	}
	if errBody.Code != "ONE_BOOKING_PER_DAY" {
		t.Fatalf("expected ONE_BOOKING_PER_DAY, got %s", errBody.Code)
	}
}

func TestRebookForbiddenIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	cfg := config.Config{DevMode: true}
	h := handler.New(st, tokens, cfg)
	router := handler.NewRouter(h, tokens, slog.Default())

	slotID := "55555555-5555-5555-5555-555555555501"
	clientPhone := "+79003334455"
	createBody := map[string]any{
		"slotId": slotID,
		"client": map[string]string{"name": "Пётр", "phone": clientPhone},
		"equipment": map[string]any{
			"mode": "OWN", "rentalShoes": false, "rentalHarness": false,
		},
	}
	createRec := postJSON(t, router, "/v1/bookings", createBody, "")
	if createRec.Code != http.StatusCreated {
		t.Fatalf("create status = %d, body = %s", createRec.Code, createRec.Body.String())
	}

	var clientID uuid.UUID
	err = pool.QueryRow(ctx, `SELECT id FROM clients WHERE phone = $1`, clientPhone).Scan(&clientID)
	if err != nil {
		t.Fatalf("client id: %v", err)
	}
	_, err = pool.Exec(ctx, `
UPDATE bookings SET status = 'CANCELLED_BY_GYM', cancellation_reason = 'Профилактика', cancelled_at = now()
WHERE client_id = $1 AND slot_id = $2`, clientID, slotID)
	if err != nil {
		t.Fatalf("mark gym cancel: %v", err)
	}

	rebookRec := postJSON(t, router, "/v1/bookings", createBody, "")
	if rebookRec.Code != http.StatusForbidden {
		t.Fatalf("rebook status = %d, want 403, body = %s", rebookRec.Code, rebookRec.Body.String())
	}
	var errBody struct {
		Code string `json:"code"`
	}
	if err := json.Unmarshal(rebookRec.Body.Bytes(), &errBody); err != nil {
		t.Fatalf("decode error: %v", err)
	}
	if errBody.Code != "SLOT_REBOOK_FORBIDDEN" {
		t.Fatalf("expected SLOT_REBOOK_FORBIDDEN, got %s", errBody.Code)
	}
}

func TestGetBookingIncludesPriceBreakdownIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	createBody := map[string]any{
		"slotId": "55555555-5555-5555-5555-555555555501",
		"client": map[string]string{"name": "Ольга", "phone": "+79004445566"},
		"equipment": map[string]any{
			"mode": "RENTAL", "rentalShoes": true, "rentalHarness": false,
		},
	}
	createRec := postJSON(t, router, "/v1/bookings", createBody, "")
	if createRec.Code != http.StatusCreated {
		t.Fatalf("create status = %d, body = %s", createRec.Code, createRec.Body.String())
	}
	var created struct {
		ID           string `json:"id"`
		SessionToken string `json:"sessionToken"`
	}
	if err := json.Unmarshal(createRec.Body.Bytes(), &created); err != nil {
		t.Fatalf("decode create: %v", err)
	}

	getRec := getAuth(t, router, "/v1/bookings/"+created.ID, created.SessionToken)
	if getRec.Code != http.StatusOK {
		t.Fatalf("get status = %d, body = %s", getRec.Code, getRec.Body.String())
	}
	var booking struct {
		PriceBreakdown *struct {
			TrainingPrice    float64 `json:"trainingPrice"`
			ShoesRentalPrice float64 `json:"shoesRentalPrice"`
			TotalPrice       float64 `json:"totalPrice"`
		} `json:"priceBreakdown"`
	}
	if err := json.Unmarshal(getRec.Body.Bytes(), &booking); err != nil {
		t.Fatalf("decode booking: %v", err)
	}
	if booking.PriceBreakdown == nil {
		t.Fatal("expected priceBreakdown")
	}
	if booking.PriceBreakdown.TotalPrice != 1400 {
		t.Fatalf("total price = %v, want 1400", booking.PriceBreakdown.TotalPrice)
	}
}

func TestEarlyCancelNotifiesWaitlistIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	slotID := "55555555-5555-5555-5555-555555555502"

	holderBody := map[string]any{
		"slotId": slotID,
		"client": map[string]string{"name": "Держатель", "phone": "+79006667788"},
		"equipment": map[string]any{
			"mode": "OWN", "rentalShoes": false, "rentalHarness": false,
		},
	}
	_, err = pool.Exec(ctx, `
UPDATE slots SET booked_count = capacity - 1, free_spots = 1, status = 'OPEN'
WHERE id = $1`, slotID)
	if err != nil {
		t.Fatalf("free one spot: %v", err)
	}

	holderRec := postJSON(t, router, "/v1/bookings", holderBody, "")
	if holderRec.Code != http.StatusCreated {
		t.Fatalf("holder create status = %d, body = %s", holderRec.Code, holderRec.Body.String())
	}
	var holder struct {
		ID           string `json:"id"`
		SessionToken string `json:"sessionToken"`
	}
	if err := json.Unmarshal(holderRec.Body.Bytes(), &holder); err != nil {
		t.Fatalf("decode holder: %v", err)
	}

	waitBody := map[string]any{
		"client": map[string]string{"name": "Очередь", "phone": "+79007778899"},
	}
	waitRec := postJSON(t, router, "/v1/slots/"+slotID+"/waitlist", waitBody, "")
	if waitRec.Code != http.StatusCreated {
		t.Fatalf("waitlist status = %d, body = %s", waitRec.Code, waitRec.Body.String())
	}

	cancelRec := postJSON(t, router, "/v1/bookings/"+holder.ID+"/cancel", map[string]any{}, holder.SessionToken)
	if cancelRec.Code != http.StatusOK {
		t.Fatalf("cancel status = %d, body = %s", cancelRec.Code, cancelRec.Body.String())
	}

	var status string
	err = pool.QueryRow(ctx, `
SELECT status::text FROM waitlist_entries
WHERE client_id = (SELECT id FROM clients WHERE phone = '+79007778899')
  AND slot_id = $1`, slotID).Scan(&status)
	if err != nil {
		t.Fatalf("query waitlist: %v", err)
	}
	if status != "NOTIFIED" {
		t.Fatalf("waitlist status = %s, want NOTIFIED", status)
	}
}

func patchJSON(t *testing.T, router http.Handler, path string, body any, token string) *httptest.ResponseRecorder {
	t.Helper()
	b, err := json.Marshal(body)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	req := httptest.NewRequest(http.MethodPatch, path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}

func postJSON(t *testing.T, router http.Handler, path string, body any, token string) *httptest.ResponseRecorder {
	t.Helper()
	b, err := json.Marshal(body)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}

func getAuth(t *testing.T, router http.Handler, path, token string) *httptest.ResponseRecorder {
	t.Helper()
	req := httptest.NewRequest(http.MethodGet, path, nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, req)
	return rec
}

func TestRatingAfterAttendedIntegration(t *testing.T) {
	dsn := testutil.PrepareDatabase(t)
	ctx := context.Background()

	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatalf("connect: %v", err)
	}
	t.Cleanup(pool.Close)

	clientID := uuid.MustParse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
	_, err = pool.Exec(ctx, `INSERT INTO clients (id, name, phone) VALUES ($1, 'Пётр', '+79001112233')`, clientID)
	if err != nil {
		t.Fatalf("insert client: %v", err)
	}
	_, err = pool.Exec(ctx, `
INSERT INTO bookings (id, client_id, slot_id, status, equipment_mode, total_price, slot_starts_at)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', $1, '55555555-5555-5555-5555-555555555501', 'ATTENDED', 'OWN', 1200, now() + interval '1 day')`,
		clientID)
	if err != nil {
		t.Fatalf("insert booking: %v", err)
	}

	st := store.New(pool)
	tokens := auth.NewTokenService("integration-secret", 24*time.Hour)
	token, err := tokens.Issue(clientID, "+79001112233")
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}
	h := handler.New(st, tokens, config.Config{DevMode: true})
	router := handler.NewRouter(h, tokens, slog.Default())

	body := map[string]any{
		"bookingId":    "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
		"instructorId": "44444444-4444-4444-4444-444444444401",
		"stars":        5,
	}
	rec := postJSON(t, router, "/v1/ratings", body, token)
	if rec.Code != http.StatusCreated {
		t.Fatalf("rating status = %d, body = %s", rec.Code, rec.Body.String())
	}
}
