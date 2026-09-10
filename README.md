# SocialWorld (working title)

A social avatar platform built around **intentions**: users pick what they're in the mood for
(casual chat, deep talk, flirt, meeting people, language exchange, chill), enter themed virtual
rooms, appear as layered 2D avatars, chat, and send virtual gifts.

> The product name is undecided. The neutral codename **SocialWorld** is used everywhere
> (`com.socialworld.app`, app display name, artifact ids) so renaming later is a config change,
> not a refactor.

## Repository layout

```
├── backend/   Spring Boot 3 / Java 21 modular monolith (Maven)
├── mobile/    React Native app (Expo + TypeScript + expo-router)
└── docker-compose.yml   Local PostgreSQL
```

## Current status — Phase 1 complete

- Project bootstrap (backend + mobile)
- PostgreSQL via Docker Compose, schema managed by Flyway
- User entity (18+ enforced at registration)
- Registration, login (username **or** email), JWT access tokens + rotating refresh tokens
- Global `{code, message}` error contract
- Mobile: splash, login, register, placeholder home with persisted session (SecureStore)

Later phases (profiles, intentions, avatars, rooms, chat, gifts, discovery, moderation) build on
this codebase — see the module layout in `backend/src/main/java/com/socialworld/app/`.

## Prerequisites

- Java 21, Maven 3.9+
- Docker (for PostgreSQL)
- Node 18+ / npm
- Expo Go app on a phone, or an emulator

## Running the backend

```bash
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. Run the API (local profile enables SQL logging)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Flyway migrations run automatically on startup.

Configuration is environment-driven; see `backend/.env.example`. Defaults work with the
docker-compose database. **Set a real `JWT_SECRET` for anything beyond local development.**

### Quick smoke test

```bash
curl -s -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"password123","birthDate":"1998-04-12","countryCode":"TR"}'

curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"alice","password":"password123"}'
```

### Backend tests

```bash
cd backend
mvn test
```

## Running the mobile app

```bash
cd mobile
npm install
npx expo start
```

Scan the QR code with Expo Go, or press `a`/`i` for an emulator.

**Physical device note:** `localhost` points at the phone itself. Set
`expo.extra.apiUrl` in `mobile/app.json` to your machine's LAN IP,
e.g. `http://192.168.1.10:8080`, then restart Expo.

Type checking:

```bash
npm run typecheck
```

## API error contract

Errors are always:

```json
{ "code": "UNDERAGE", "message": "You must be at least 18 years old." }
```

Clients switch on `code` (stable), never on `message`.

## Auth model

- Access token: short-lived JWT (HS256, 15 min) sent as `Authorization: Bearer <token>`.
- Refresh token: opaque random value (30 days), stored **hashed** server-side, **rotated on every
  refresh**; reuse of a revoked token revokes all of the user's refresh tokens.
- Passwords: BCrypt.

## Never commit secrets

`.env` files are git-ignored; only `.env.example` templates are tracked.
