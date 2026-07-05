package auth

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

type Claims struct {
	ClientID string `json:"client_id"`
	Phone    string `json:"phone"`
	jwt.RegisteredClaims
}

type TokenService struct {
	secret []byte
	ttl    time.Duration
}

func NewTokenService(secret string, ttl time.Duration) *TokenService {
	return &TokenService{secret: []byte(secret), ttl: ttl}
}

func (s *TokenService) Issue(clientID uuid.UUID, phone string) (string, error) {
	now := time.Now()
	claims := Claims{
		ClientID: clientID.String(),
		Phone:    phone,
		RegisteredClaims: jwt.RegisteredClaims{
			Subject:   clientID.String(),
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(s.ttl)),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	signed, err := token.SignedString(s.secret)
	if err != nil {
		return "", fmt.Errorf("sign token: %w", err)
	}
	return signed, nil
}

func (s *TokenService) Parse(tokenString string) (uuid.UUID, string, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (any, error) {
		if token.Method != jwt.SigningMethodHS256 {
			return nil, fmt.Errorf("unexpected signing method")
		}
		return s.secret, nil
	})
	if err != nil {
		return uuid.Nil, "", fmt.Errorf("parse token: %w", err)
	}
	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return uuid.Nil, "", fmt.Errorf("invalid token")
	}
	clientID, err := uuid.Parse(claims.ClientID)
	if err != nil {
		return uuid.Nil, "", fmt.Errorf("invalid client id: %w", err)
	}
	return clientID, claims.Phone, nil
}
