package handler

import (
	"context"
	"log/slog"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/config"
	"github.com/dimbass/summerpractic/climbing/backend/internal/domain"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/auth"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store"
	"github.com/dimbass/summerpractic/climbing/backend/internal/validate"
)

type clientIDKey struct{}

type Handler struct {
	store  *store.Store
	tokens *auth.TokenService
	cfg    config.Config
}

func New(st *store.Store, tokens *auth.TokenService, cfg config.Config) *Handler {
	return &Handler{store: st, tokens: tokens, cfg: cfg}
}

func (h *Handler) clientID(ctx context.Context) (uuid.UUID, bool) {
	v := ctx.Value(clientIDKey{})
	if v == nil {
		return uuid.Nil, false
	}
	id, ok := v.(uuid.UUID)
	return id, ok
}

func (h *Handler) requireAuth(w http.ResponseWriter, r *http.Request) (uuid.UUID, bool) {
	id, ok := h.clientID(r.Context())
	if !ok {
		writeError(w, domain.NewAppError(domain.ErrUnauthorized, "Требуется авторизация", http.StatusUnauthorized))
		return uuid.Nil, false
	}
	return id, true
}

func (h *Handler) issueToken(w http.ResponseWriter, clientID uuid.UUID, phone string) string {
	token, err := h.tokens.Issue(clientID, phone)
	if err != nil {
		writeError(w, err)
		return ""
	}
	return token
}

func (h *Handler) Healthz(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *Handler) Readyz(w http.ResponseWriter, r *http.Request) {
	if err := h.store.Ping(r.Context()); err != nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"status": "unavailable"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}

func (h *Handler) getSlotByID(w http.ResponseWriter, r *http.Request, slotID uuid.UUID) {
	detail, err := h.store.GetSlotDetail(r.Context(), slotID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, detail)
}

func (h *Handler) ListInstructors(w http.ResponseWriter, r *http.Request) {
	items, err := h.store.ListInstructors(r.Context())
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, api.InstructorListResponse{Items: items})
}

func (h *Handler) GetProfile(w http.ResponseWriter, r *http.Request) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	row, err := h.store.GetClient(r.Context(), clientID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, toProfile(row))
}

func (h *Handler) UpdateProfile(w http.ResponseWriter, r *http.Request) {
	var req api.UpdateProfileRequest
	if err := decodeJSON(r, &req); err != nil {
		writeError(w, err)
		return
	}
	if err := validate.ClientContacts(api.ClientContacts{Name: req.Name, Phone: req.Phone}); err != nil {
		writeError(w, err)
		return
	}
	row, err := h.store.UpsertClient(r.Context(), req.Name, req.Phone)
	if err != nil {
		writeError(w, err)
		return
	}
	token := h.issueToken(w, row.ID, row.Phone)
	if token == "" {
		return
	}
	writeJSON(w, http.StatusOK, api.UpdateProfileResponse{
		ClientProfile: toProfile(row),
		SessionToken:  token,
	})
}

func (h *Handler) CreateBooking(w http.ResponseWriter, r *http.Request) {
	var req api.CreateBookingRequest
	if err := decodeJSON(r, &req); err != nil {
		writeError(w, err)
		return
	}
	if err := validate.ClientContacts(req.Client); err != nil {
		writeError(w, err)
		return
	}
	if err := validate.Equipment(req.Equipment); err != nil {
		writeError(w, err)
		return
	}
	slotID, err := uuid.Parse(req.SlotID)
	if err != nil {
		writeError(w, domain.NewAppError(domain.ErrValidation, "Некорректный slotId", 400))
		return
	}
	row, err := h.store.UpsertClient(r.Context(), req.Client.Name, req.Client.Phone)
	if err != nil {
		writeError(w, err)
		return
	}
	booking, err := h.store.CreateBooking(r.Context(), row.ID, slotID, req.Equipment)
	if err != nil {
		writeError(w, err)
		return
	}
	token := h.issueToken(w, row.ID, row.Phone)
	if token == "" {
		return
	}
	writeJSON(w, http.StatusCreated, api.CreateBookingResponse{
		Booking:      booking,
		SessionToken: token,
	})
}

func (h *Handler) listBookings(w http.ResponseWriter, r *http.Request, status string, limit, offset int) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	if limit <= 0 {
		limit = 50
	}
	if limit > 100 {
		limit = 100
	}
	if offset < 0 {
		offset = 0
	}
	items, total, err := h.store.ListBookings(r.Context(), clientID, status, limit, offset)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, api.BookingListResponse{
		Items: items,
		Meta:  api.PaginationMeta{Total: total, Limit: limit, Offset: offset},
	})
}

func (h *Handler) getBookingByID(w http.ResponseWriter, r *http.Request, bookingID uuid.UUID) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	booking, err := h.store.GetBooking(r.Context(), clientID, bookingID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, booking)
}

func (h *Handler) cancelBookingByID(w http.ResponseWriter, r *http.Request, bookingID uuid.UUID) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	resp, err := h.store.CancelBooking(r.Context(), clientID, bookingID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, resp)
}

func (h *Handler) joinWaitlistByID(w http.ResponseWriter, r *http.Request, slotID uuid.UUID) {
	var req api.JoinWaitlistRequest
	if r.Body != nil && r.ContentLength != 0 {
		if err := decodeJSON(r, &req); err != nil {
			writeError(w, err)
			return
		}
	}
	var clientID uuid.UUID
	if id, ok := h.clientID(r.Context()); ok {
		clientID = id
	} else if req.Client != nil {
		if err := validate.ClientContacts(*req.Client); err != nil {
			writeError(w, err)
			return
		}
		row, err := h.store.UpsertClient(r.Context(), req.Client.Name, req.Client.Phone)
		if err != nil {
			writeError(w, err)
			return
		}
		clientID = row.ID
	} else {
		writeError(w, domain.NewAppError(domain.ErrUnauthorized, "Требуется авторизация", http.StatusUnauthorized))
		return
	}
	entry, err := h.store.JoinWaitlist(r.Context(), clientID, slotID)
	if err != nil {
		writeError(w, err)
		return
	}
	row, err := h.store.GetClient(r.Context(), clientID)
	if err != nil {
		writeError(w, err)
		return
	}
	token := h.issueToken(w, row.ID, row.Phone)
	if token == "" {
		return
	}
	writeJSON(w, http.StatusCreated, api.JoinWaitlistResponse{
		WaitlistEntry: entry,
		SessionToken:  token,
	})
}

func (h *Handler) getWaitlistEntryByID(w http.ResponseWriter, r *http.Request, entryID uuid.UUID) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	entry, err := h.store.GetWaitlistEntry(r.Context(), clientID, entryID)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, entry)
}

func (h *Handler) deleteWaitlistEntryByID(w http.ResponseWriter, r *http.Request, entryID uuid.UUID) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	if err := h.store.DeleteWaitlistEntry(r.Context(), clientID, entryID); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) leaveWaitlistByID(w http.ResponseWriter, r *http.Request, bookingID uuid.UUID) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	booking, err := h.store.GetBooking(r.Context(), clientID, bookingID)
	if err != nil {
		writeError(w, err)
		return
	}
	if booking.Status != "WAITLIST" {
		writeError(w, domain.NewAppError(domain.ErrValidation, "Бронь не в листе ожидания", 409))
		return
	}
	slotID, err := uuid.Parse(booking.SlotID)
	if err != nil {
		writeError(w, err)
		return
	}
	entryID, err := h.store.FindActiveWaitlistEntry(r.Context(), clientID, slotID)
	if err != nil {
		writeError(w, err)
		return
	}
	if err := h.store.DeleteWaitlistEntry(r.Context(), clientID, entryID); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) CreateRating(w http.ResponseWriter, r *http.Request) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var req api.CreateRatingRequest
	if err := decodeJSON(r, &req); err != nil {
		writeError(w, err)
		return
	}
	if err := validate.Stars(req.Stars); err != nil {
		writeError(w, err)
		return
	}
	resp, err := h.store.CreateRating(r.Context(), clientID, req)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusCreated, resp)
}

func (h *Handler) RegisterPushToken(w http.ResponseWriter, r *http.Request) {
	clientID, ok := h.requireAuth(w, r)
	if !ok {
		return
	}
	var req api.RegisterPushTokenRequest
	if err := decodeJSON(r, &req); err != nil {
		writeError(w, err)
		return
	}
	if req.Token == "" || (req.Platform != "ios" && req.Platform != "android") {
		writeError(w, domain.NewAppError(domain.ErrValidation, "Некорректный push-токен", 400))
		return
	}
	if err := h.store.RegisterPushToken(r.Context(), clientID, req.Token, req.Platform); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) DevMarkAttended(w http.ResponseWriter, r *http.Request) {
	if !h.cfg.DevMode {
		http.NotFound(w, r)
		return
	}
	bookingID, err := uuid.Parse(chi.URLParam(r, "bookingId"))
	if err != nil {
		writeError(w, domain.NewAppError(domain.ErrValidation, "Некорректный bookingId", 400))
		return
	}
	if err := h.store.MarkAttended(r.Context(), bookingID); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) DevCancelSlotByGym(w http.ResponseWriter, r *http.Request) {
	if !h.cfg.DevMode {
		http.NotFound(w, r)
		return
	}
	slotID, err := uuid.Parse(chi.URLParam(r, "slotId"))
	if err != nil {
		writeError(w, domain.NewAppError(domain.ErrValidation, "Некорректный slotId", 400))
		return
	}
	var body struct {
		Reason string `json:"reason"`
	}
	_ = decodeJSON(r, &body)
	if body.Reason == "" {
		body.Reason = "Профилактика зоны"
	}
	if err := h.store.CancelSlotByGym(r.Context(), slotID, body.Reason); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) DevSendNotification(w http.ResponseWriter, r *http.Request) {
	if !h.cfg.DevMode {
		http.NotFound(w, r)
		return
	}
	var body struct {
		ClientID string `json:"clientId"`
		Title    string `json:"title"`
		Body     string `json:"body"`
	}
	if err := decodeJSON(r, &body); err != nil {
		writeError(w, err)
		return
	}
	if body.Title == "" {
		body.Title = "Вертикаль"
	}
	slog.Info("dev push notification",
		"client_id", body.ClientID,
		"title", body.Title,
		"body", body.Body,
	)
	w.WriteHeader(http.StatusNoContent)
}

func toProfile(row store.ClientRow) api.ClientProfile {
	return api.ClientProfile{
		ID:              row.ID.String(),
		Name:            row.Name,
		Phone:           row.Phone,
		IsComplete:      true,
		IsRegularClient: row.IsRegular,
	}
}

func startOfDay(t time.Time) time.Time {
	y, m, d := t.Date()
	return time.Date(y, m, d, 0, 0, 0, 0, t.Location())
}

func paginate(q map[string][]string) (limit, offset int) {
	limit, offset = 50, 0
	if v := first(q["limit"]); v != "" {
		if n, err := parseInt(v); err == nil && n > 0 && n <= 100 {
			limit = n
		}
	}
	if v := first(q["offset"]); v != "" {
		if n, err := parseInt(v); err == nil && n >= 0 {
			offset = n
		}
	}
	return limit, offset
}

func first(v []string) string {
	if len(v) == 0 {
		return ""
	}
	return v[0]
}

func parseInt(s string) (int, error) {
	return strconv.Atoi(s)
}
