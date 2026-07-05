package scheduler

import (
	"context"
	"log/slog"
	"time"
)

// ReminderScheduler logs upcoming booking reminders (Q 6.1 stub, BE-12).
type ReminderScheduler struct {
	log *slog.Logger
}

func NewReminderScheduler(log *slog.Logger) *ReminderScheduler {
	return &ReminderScheduler{log: log}
}

func (s *ReminderScheduler) Run(ctx context.Context) {
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()

	s.log.Info("reminder scheduler started")
	for {
		select {
		case <-ctx.Done():
			s.log.Info("reminder scheduler stopped")
			return
		case <-ticker.C:
			s.log.Info("reminder scheduler tick",
				"day_before", "stub",
				"two_hours_before", "stub",
			)
		}
	}
}
