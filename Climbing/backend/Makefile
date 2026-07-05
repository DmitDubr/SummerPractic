.PHONY: run test fmt docker-up docker-down generate bundle-api reset-dev-slots

run:
	go run ./cmd/api

test:
	go test ./...

fmt:
	gofmt -w .

docker-up:
	docker compose up --build

docker-down:
	docker compose down -v

reset-dev-slots:
	docker compose exec -T db psql -U vertical -d vertical -f - < db/scripts/reset-dev-slots.sql

bundle-api:
	docker run --rm -v "$(CURDIR)/api:/api" -w /api node:20-alpine sh -c "npx --yes @redocly/cli@1.28.5 bundle openapi.yaml -o openapi.bundled.yaml"

generate: bundle-api
	go generate ./internal/gen/...
