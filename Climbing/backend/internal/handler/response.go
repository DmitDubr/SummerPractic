package handler

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strings"

	"github.com/dimbass/summerpractic/climbing/backend/internal/api"
	"github.com/dimbass/summerpractic/climbing/backend/internal/domain"
)

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, err error) {
	var appErr *domain.AppError
	if errors.As(err, &appErr) {
		writeJSON(w, appErr.Status, api.ErrorResponse{
			Code:    string(appErr.Code),
			Message: appErr.Message,
			Details: toAPIDetails(appErr.Details),
		})
		return
	}
	writeJSON(w, http.StatusInternalServerError, api.ErrorResponse{
		Code:    string(domain.ErrServer),
		Message: "Внутренняя ошибка сервера",
	})
}

func toAPIDetails(details []domain.ValidationDetail) []api.ValidationDetail {
	if len(details) == 0 {
		return nil
	}
	out := make([]api.ValidationDetail, len(details))
	for i, d := range details {
		out[i] = api.ValidationDetail{Field: d.Field, Message: d.Message}
	}
	return out
}

func decodeJSON(r *http.Request, dst any) error {
	defer r.Body.Close()
	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(dst); err != nil {
		return domain.NewAppError(domain.ErrValidation, "Некорректные данные запроса", http.StatusBadRequest)
	}
	if err := dec.Decode(&struct{}{}); err != io.EOF {
		return domain.NewAppError(domain.ErrValidation, "Некорректные данные запроса", http.StatusBadRequest)
	}
	return nil
}

func bearerToken(r *http.Request) string {
	h := r.Header.Get("Authorization")
	if h == "" {
		return ""
	}
	parts := strings.SplitN(h, " ", 2)
	if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
		return ""
	}
	return strings.TrimSpace(parts[1])
}
