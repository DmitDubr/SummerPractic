package handler_test

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/dimbass/summerpractic/climbing/backend/internal/config"
	"github.com/dimbass/summerpractic/climbing/backend/internal/handler"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/auth"
)

func TestRouterHealthz(t *testing.T) {
	router := testRouter(t)

	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/healthz", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d", rec.Code)
	}
}

func TestRouterUnauthorizedBookings(t *testing.T) {
	router := testRouter(t)

	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/v1/bookings", nil))
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
	}
	assertErrorCode(t, rec, "VALIDATION_ERROR")
}

func TestRouterInvalidJSON(t *testing.T) {
	router := testRouter(t)

	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/v1/bookings", strings.NewReader(`{`))
	req.Header.Set("Content-Type", "application/json")
	router.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, body = %s", rec.Code, rec.Body.String())
	}
}

func TestRouterUnknownPath(t *testing.T) {
	router := testRouter(t)

	rec := httptest.NewRecorder()
	router.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/v1/unknown", nil))
	if rec.Code != http.StatusNotFound {
		t.Fatalf("status = %d", rec.Code)
	}
}

func testRouter(t *testing.T) http.Handler {
	t.Helper()
	h := handler.New(nil, auth.NewTokenService("test-secret", 24*time.Hour), config.Config{DevMode: true})
	return handler.NewRouter(h, auth.NewTokenService("test-secret", 24*time.Hour), slog.Default())
}

func assertErrorCode(t *testing.T, rec *httptest.ResponseRecorder, code string) {
	t.Helper()
	var body struct {
		Code string `json:"code"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if body.Code != code {
		t.Fatalf("code = %q, want %q", body.Code, code)
	}
}
