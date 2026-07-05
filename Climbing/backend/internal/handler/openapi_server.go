package handler

import (
	"net/http"
	"time"

	"github.com/google/uuid"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/gen"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store"
)

// OpenAPIServer adapts Handler to the oapi-codegen ServerInterface (BE-01).
type OpenAPIServer struct {
	*Handler
}

var _ gen.ServerInterface = (*OpenAPIServer)(nil)

func (s *OpenAPIServer) ListSlots(w http.ResponseWriter, r *http.Request, params gen.ListSlotsParams) {
	now := time.Now()
	dateFrom := startOfDay(now)
	dateTo := startOfDay(now).AddDate(0, 0, 6)
	if params.DateFrom != nil {
		dateFrom = startOfDay(params.DateFrom.Time)
	}
	if params.DateTo != nil {
		dateTo = startOfDay(params.DateTo.Time)
	}
	var instructorIDs []uuid.UUID
	if params.InstructorIds != nil {
		for _, id := range *params.InstructorIds {
			instructorIDs = append(instructorIDs, uuid.UUID(id))
		}
	}
	timeOfDay := ""
	if params.TimeOfDay != nil {
		timeOfDay = string(*params.TimeOfDay)
	}
	level := ""
	if params.Level != nil {
		level = string(*params.Level)
	}
	limit, offset := 50, 0
	items, total, err := s.store.ListSlots(r.Context(), store.SlotFilters{
		DateFrom:      dateFrom,
		DateTo:        dateTo,
		InstructorIDs: instructorIDs,
		TimeOfDay:     timeOfDay,
		Level:         level,
		Limit:         limit,
		Offset:        offset,
	})
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, api.SlotListResponse{
		Items: items,
		Meta:  api.PaginationMeta{Total: total, Limit: limit, Offset: offset},
	})
}

func (s *OpenAPIServer) GetSlot(w http.ResponseWriter, r *http.Request, slotId gen.SlotId) {
	s.Handler.getSlotByID(w, r, uuid.UUID(slotId))
}

func (s *OpenAPIServer) ListBookings(w http.ResponseWriter, r *http.Request, params gen.ListBookingsParams) {
	status := ""
	if params.Status != nil {
		status = string(*params.Status)
	}
	limit, offset := 50, 0
	if params.Limit != nil {
		limit = int(*params.Limit)
	}
	if params.Offset != nil {
		offset = int(*params.Offset)
	}
	s.Handler.listBookings(w, r, status, limit, offset)
}

func (s *OpenAPIServer) GetBooking(w http.ResponseWriter, r *http.Request, bookingId gen.BookingId) {
	s.Handler.getBookingByID(w, r, uuid.UUID(bookingId))
}

func (s *OpenAPIServer) CancelBooking(w http.ResponseWriter, r *http.Request, bookingId gen.BookingId) {
	s.Handler.cancelBookingByID(w, r, uuid.UUID(bookingId))
}

func (s *OpenAPIServer) LeaveWaitlist(w http.ResponseWriter, r *http.Request, bookingId gen.BookingId) {
	s.Handler.leaveWaitlistByID(w, r, uuid.UUID(bookingId))
}

func (s *OpenAPIServer) JoinWaitlist(w http.ResponseWriter, r *http.Request, slotId gen.SlotId) {
	s.Handler.joinWaitlistByID(w, r, uuid.UUID(slotId))
}

func (s *OpenAPIServer) GetWaitlistEntry(w http.ResponseWriter, r *http.Request, waitlistEntryId gen.WaitlistEntryId) {
	s.Handler.getWaitlistEntryByID(w, r, uuid.UUID(waitlistEntryId))
}

func (s *OpenAPIServer) DeleteWaitlistEntry(w http.ResponseWriter, r *http.Request, waitlistEntryId gen.WaitlistEntryId) {
	s.Handler.deleteWaitlistEntryByID(w, r, uuid.UUID(waitlistEntryId))
}
