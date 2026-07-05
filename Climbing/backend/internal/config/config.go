package config

import (
	"fmt"
	"time"

	"github.com/caarlos0/env/v11"
)

type Config struct {
	HTTPPort    string `env:"HTTP_PORT" envDefault:"8080"`
	DatabaseURL string `env:"DATABASE_URL,required"`
	JWTSecret   string `env:"JWT_SECRET" envDefault:"dev-vertical-secret-change-me"`
	JWTTTL      time.Duration `env:"JWT_TTL" envDefault:"720h"`
	DevMode     bool   `env:"DEV_MODE" envDefault:"true"`
	LogLevel    string `env:"LOG_LEVEL" envDefault:"info"`
}

func Load() (Config, error) {
	var cfg Config
	if err := env.Parse(&cfg); err != nil {
		return Config{}, fmt.Errorf("parse config: %w", err)
	}
	return cfg, nil
}
