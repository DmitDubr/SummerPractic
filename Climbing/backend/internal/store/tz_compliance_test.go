package store

import (
	"testing"
	"time"
)

func TestIsEarlyCancel(t *testing.T) {
	now := time.Date(2026, 7, 5, 12, 0, 0, 0, time.UTC)

	if !isEarlyCancel(now.Add(2*time.Hour), now) {
		t.Fatal("expected early cancel for +2h")
	}
	if isEarlyCancel(now.Add(30*time.Minute), now) {
		t.Fatal("expected late cancel for +30m")
	}
}
