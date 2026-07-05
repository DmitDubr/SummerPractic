package testutil

import (
	"context"
	"fmt"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

// PrepareDatabase creates an isolated schema, applies migrations and returns a DSN.
func PrepareDatabase(t *testing.T) string {
	t.Helper()

	databaseURL := os.Getenv("TEST_DATABASE_URL")
	if databaseURL == "" {
		t.Skip("TEST_DATABASE_URL is not set")
	}

	schemaName := fmt.Sprintf("test_%d", time.Now().UnixNano())
	ctx := context.Background()

	basePool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		t.Skipf("database unavailable: %v", err)
	}
	if _, err := basePool.Exec(ctx, "CREATE SCHEMA "+schemaName); err != nil {
		basePool.Close()
		t.Skipf("database unavailable: %v", err)
	}
	basePool.Close()

	dsn := databaseURLWithSearchPath(t, databaseURL, schemaName)

	m, err := migrate.New(
		"file://"+filepath.ToSlash(migrationsDir(t)),
		dsn,
	)
	if err != nil {
		t.Fatalf("create migrator: %v", err)
	}
	t.Cleanup(func() {
		_, _ = m.Close()
	})

	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		t.Fatalf("migrate up: %v", err)
	}

	t.Cleanup(func() {
		cleanupPool, err := pgxpool.New(context.Background(), databaseURL)
		if err != nil {
			return
		}
		_, _ = cleanupPool.Exec(context.Background(), "DROP SCHEMA IF EXISTS "+schemaName+" CASCADE")
		cleanupPool.Close()
	})

	return strings.Replace(dsn, "pgx5://", "postgres://", 1)
}

func migrationsDir(t *testing.T) string {
	t.Helper()

	wd, err := os.Getwd()
	if err != nil {
		t.Fatalf("getwd: %v", err)
	}
	for dir := wd; ; dir = filepath.Dir(dir) {
		candidate := filepath.Join(dir, "db", "migrations")
		if _, err := os.Stat(candidate); err == nil {
			return candidate
		}
		if parent := filepath.Dir(dir); parent == dir {
			break
		}
	}
	t.Fatal("db/migrations not found")
	return ""
}

func databaseURLWithSearchPath(t *testing.T, databaseURL, schemaName string) string {
	t.Helper()

	parsed, err := url.Parse(databaseURL)
	if err != nil {
		t.Fatalf("parse TEST_DATABASE_URL: %v", err)
	}

	query := parsed.Query()
	if _, ok := query["sslmode"]; !ok && !strings.Contains(databaseURL, "sslmode=") {
		query.Set("sslmode", "disable")
	}
	query.Set("search_path", schemaName)
	parsed.RawQuery = query.Encode()

	host := parsed.Host
	path := parsed.Path
	if path == "" {
		path = "/vertical"
	}

	return "pgx5://" + parsed.User.String() + "@" + host + path + "?" + query.Encode()
}
