package domain

import "errors"

type ErrorCode string

const (
	ErrValidation        ErrorCode = "VALIDATION_ERROR"
	ErrSlotRebookForbidden ErrorCode = "SLOT_REBOOK_FORBIDDEN"
	ErrNoSpots           ErrorCode = "NO_SPOTS"
	ErrOneBookingPerDay  ErrorCode = "ONE_BOOKING_PER_DAY"
	ErrSlotCancelled     ErrorCode = "SLOT_CANCELLED"
	ErrRentalUnavailable ErrorCode = "RENTAL_UNAVAILABLE"
	ErrAlreadyCancelled  ErrorCode = "ALREADY_CANCELLED"
	ErrCancelTooLate     ErrorCode = "CANCEL_TOO_LATE"
	ErrBookingNotAttended ErrorCode = "BOOKING_NOT_ATTENDED"
	ErrAlreadyRated      ErrorCode = "ALREADY_RATED"
	ErrAlreadyInWaitlist ErrorCode = "ALREADY_IN_WAITLIST"
	ErrWaitlistNotFound  ErrorCode = "WAITLIST_NOT_FOUND"
	ErrServer            ErrorCode = "SERVER_ERROR"
	ErrUnauthorized      ErrorCode = "VALIDATION_ERROR"
	ErrNotFound          ErrorCode = "VALIDATION_ERROR"
)

type AppError struct {
	Code    ErrorCode
	Message string
	Status  int
	Details []ValidationDetail
}

type ValidationDetail struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

func (e *AppError) Error() string {
	return e.Message
}

func NewAppError(code ErrorCode, message string, status int) *AppError {
	return &AppError{Code: code, Message: message, Status: status}
}

func IsAppError(err error) (*AppError, bool) {
	var appErr *AppError
	if errors.As(err, &appErr) {
		return appErr, true
	}
	return nil, false
}
