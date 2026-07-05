package auth

import (
	"testing"
	"time"

	"github.com/google/uuid"
)

func TestIssueAndParseToken(t *testing.T) {
	svc := NewTokenService("test-secret", time.Hour)
	id := uuid.New()
	token, err := svc.Issue(id, "+79001234567")
	if err != nil {
		t.Fatal(err)
	}
	parsedID, phone, err := svc.Parse(token)
	if err != nil {
		t.Fatal(err)
	}
	if parsedID != id || phone != "+79001234567" {
		t.Fatalf("unexpected claims: %s %s", parsedID, phone)
	}
}

func TestParseExpiredToken(t *testing.T) {
	svc := NewTokenService("test-secret", -time.Hour)
	id := uuid.New()
	token, err := svc.Issue(id, "+79001234567")
	if err != nil {
		t.Fatal(err)
	}
	if _, _, err := svc.Parse(token); err == nil {
		t.Fatal("expected error for expired token")
	}
}

func TestParseInvalidToken(t *testing.T) {
	svc := NewTokenService("test-secret", time.Hour)
	if _, _, err := svc.Parse("not-a-jwt"); err == nil {
		t.Fatal("expected error for invalid token")
	}
}
