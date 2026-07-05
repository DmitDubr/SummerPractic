package handler

import (
	"context"
	"log/slog"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/dimbass/summerpractic/climbing/backend/internal/domain"
	"github.com/dimbass/summerpractic/climbing/backend/internal/gen"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/auth"
)

func NewRouter(h *Handler, tokens *auth.TokenService, log *slog.Logger) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)
	r.Use(middleware.Recoverer)
	r.Use(requestLogger(log))
	r.Use(corsMiddleware)

	r.Get("/healthz", h.Healthz)
	r.Get("/readyz", h.Readyz)

	r.Route("/v1", func(r chi.Router) {
		r.Use(optionalAuth(tokens))
		openAPI := &OpenAPIServer{Handler: h}
		gen.HandlerWithOptions(openAPI, gen.ChiServerOptions{
			BaseRouter: r,
			ErrorHandlerFunc: func(w http.ResponseWriter, r *http.Request, err error) {
				writeError(w, domain.NewAppError(domain.ErrValidation, err.Error(), http.StatusBadRequest))
			},
		})

		r.Route("/dev", func(r chi.Router) {
			r.Post("/bookings/{bookingId}/mark-attended", h.DevMarkAttended)
			r.Post("/slots/{slotId}/cancel-by-gym", h.DevCancelSlotByGym)
			r.Post("/notifications/send", h.DevSendNotification)
		})
	})

	return r
}

func optionalAuth(tokens *auth.TokenService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			token := bearerToken(r)
			if token == "" {
				next.ServeHTTP(w, r)
				return
			}
			clientID, _, err := tokens.Parse(token)
			if err != nil {
				next.ServeHTTP(w, r)
				return
			}
			ctx := context.WithValue(r.Context(), clientIDKey{}, clientID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func requireAuth(tokens *auth.TokenService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			token := bearerToken(r)
			if token == "" {
				writeError(w, domain.NewAppError(domain.ErrUnauthorized, "Требуется авторизация", http.StatusUnauthorized))
				return
			}
			clientID, _, err := tokens.Parse(token)
			if err != nil {
				writeError(w, domain.NewAppError(domain.ErrUnauthorized, "Требуется авторизация", http.StatusUnauthorized))
				return
			}
			ctx := context.WithValue(r.Context(), clientIDKey{}, clientID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func requestLogger(log *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			ww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)
			next.ServeHTTP(ww, r)
			log.Info("request",
				"method", r.Method,
				"path", r.URL.Path,
				"status", ww.Status(),
				"duration", time.Since(start),
				"request_id", middleware.GetReqID(r.Context()),
			)
		})
	}
}

func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type")
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}
