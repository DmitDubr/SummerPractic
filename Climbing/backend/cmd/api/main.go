package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/dimbass/summerpractic/climbing/backend/internal/config"
	"github.com/dimbass/summerpractic/climbing/backend/internal/handler"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/auth"
	"github.com/dimbass/summerpractic/climbing/backend/internal/platform/scheduler"
	"github.com/dimbass/summerpractic/climbing/backend/internal/store"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("config", "error", err)
		os.Exit(1)
	}

	log := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))

	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	pool, err := pgxpool.New(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Error("db connect", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	if err := pool.Ping(ctx); err != nil {
		log.Error("db ping", "error", err)
		os.Exit(1)
	}

	st := store.New(pool)
	tokens := auth.NewTokenService(cfg.JWTSecret, cfg.JWTTTL)
	h := handler.New(st, tokens, cfg)
	router := handler.NewRouter(h, tokens, log)

	schedulerCtx, schedulerCancel := context.WithCancel(context.Background())
	defer schedulerCancel()
	go scheduler.NewReminderScheduler(log).Run(schedulerCtx)

	srv := &http.Server{
		Addr:         ":" + cfg.HTTPPort,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	go func() {
		log.Info("api listening", "addr", srv.Addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Error("server", "error", err)
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()
	_ = srv.Shutdown(shutdownCtx)
	log.Info("api stopped")
}
