# SocialWorld (working title)

A social avatar platform built around **intentions**: users pick what they're in the mood for
(casual chat, deep talk, flirt, meeting people, language exchange, chill), enter themed virtual
rooms, appear as layered 2D avatars, chat in real time, and send virtual gifts.

> The product name is undecided. The neutral codename **SocialWorld** is used everywhere
> (`com.socialworld.app`, app display name, artifact ids) so renaming later is a config change,
> not a refactor.

## Repository layout

```
├── backend/   Spring Boot 3 / Java 21 modular monolith (Maven)
├── mobile/    React Native app (Expo + TypeScript + expo-router)
└── docker-compose.yml   Local PostgreSQL
```

## MVP status — all 9 phases complete

| Area | What works |
|---|---|
| Auth | Register (18+ enforced), login by username/email, JWT + rotating hashed refresh tokens, rate-limited auth endpoints, BCrypt |
| Profiles | Bio/country/gender, public profiles expose age (never birth date or email) |
| Intentions | 6 moods, changeable anytime; drives room and people recommendations |
| Games | 1v1 tic-tac-toe in the Game Room: invite, accept, live moves over the socket, quitting or dropping hands the win over |
| Languages | NATIVE/LEARNING/SPEAKING with CEFR levels |
| Avatars | Layered 2D system (8 categories), asset catalog with premium fields, creator UI |
| Rooms | 5 themed rooms, percent-based seat slots with depth scaling, one-room-at-a-time presence enforced by schema |
| Chat | Real-time over raw WebSocket (+ REST fallback), conversations with unread counts, read marking, sanitization |
| Gifts | 5-gift catalog, wallet with 100 welcome coins, transactional sending (row-locked, never negative), live notification |
| Discovery | Weighted scoring (intention > online > language fit > shared room > recency), filterable |
| Connections | Request/accept/reject, duplicate-proof in both directions |
| Moderation | Block (cuts chat/gifts/profiles/discovery/rooms both ways, neutral errors), report with 8 reasons |

Backend: 111 unit tests. Full architecture notes below.

**Hidden for launch:** language exchange (the module, API, screen and schema are
all intact — the mood was dropped from the Home screen, the Language Corner room
deactivated and the Settings row swapped for Games). Turning it back on is
undoing those three edits, not rebuilding a feature.

## Prerequisites

- Java 21, Maven 3.9+
- PostgreSQL 16 — either Docker, or a local install
  (macOS: `brew install postgresql@16 && brew services start postgresql@16`, then
  `createuser -s socialworld && createdb -O socialworld socialworld`)
- Node 18+ / npm
- Expo Go app on a phone, or an emulator

## Running the backend

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

- API: http://localhost:8080 — Swagger UI: http://localhost:8080/swagger-ui.html
- Flyway migrations (V1–V14) run automatically; they seed 5 active rooms, 24 avatar assets
  and 5 gifts.
- The **local profile also seeds 10 demo users** — log in as `mira`, `leo`, `ada`, `nova`,
  `kenji`, `sofia`, `omar`, `lena`, `marco` or `yuki`, password `password123`. Half are
  "online" and already sitting in rooms, with languages, avatars and a starter conversation.

Configuration is environment-driven (see `backend/.env.example`). Defaults work with the
docker-compose database. The app **refuses to start** with the built-in dev JWT secret outside
the local profile — set `JWT_SECRET` (`openssl rand -hex 32`) anywhere else.

```bash
cd backend && mvn test        # run the test suite
```

## Running the mobile app

```bash
cd mobile
npm install
npx expo start                # scan the QR with Expo Go
npm run typecheck             # tsc --noEmit
```

**Physical device:** `localhost` points at the phone. Set `expo.extra.apiUrl` in
`mobile/app.json` to your machine's LAN IP (e.g. `http://192.168.1.10:8080`) and restart Expo.

**Two-person test:** register two accounts (or use two demo users), join the same room from
both, tap each other's avatars — chat, gifts and blocking all work live.

## Architecture

**Modular monolith.** Each backend package under `com.socialworld.app` owns its
controller/service/repository/entities/DTOs; modules call each other's *services* only:

```
auth  user  intention  language  avatar  room  chat  gift  wallet
discovery  friendship (connections)  moderation  common  config
```

Key decisions:

- **JPA entities never cross the API boundary** — every response is a DTO record.
- **Flyway owns the schema** (`ddl-auto: validate`); JPA can never drift from migrations.
- **Errors are a stable contract**: always `{ "code": "...", "message": "..." }` via one
  `ErrorCode` enum + `GlobalExceptionHandler`. Clients switch on `code`, never parse messages.
- **Auth**: 15-min HS256 access tokens; opaque refresh tokens stored hashed, rotated on every
  use — reuse of a revoked token revokes the whole family. Constant-work login defeats
  username-enumeration timing. Fixed-window per-IP rate limiting on `/api/auth/*` behind a
  `RateLimiter` abstraction (swap in Redis later).
- **Real-time**: a clean JSON protocol over a raw WebSocket at `/ws/chat?token=...`
  (no STOMP — zero extra mobile dependencies). `ChatSessionRegistry` decouples push from the
  handler, so chat, gifts and games share it. Offline users simply catch up over REST.
- **Games are server-authoritative**: clients send intents (`{"type":"game.move","cell":4}`),
  never state. Every rule lives in a pure engine (`TicTacToe`) that the service calls under a
  row lock, so simultaneous moves cannot corrupt a board and a tampered client only gets an
  error back. `game_sessions.state` is opaque to everything but its engine — tombala and okey
  are a new engine, not a new table. Losing your last socket forfeits your live games, so no
  one waits on a turn that will never come.
- **Invariants live in the schema where possible**: one room per user (`room_presence.user_id`
  is the PK), no double-seating (unique room+slot), non-negative wallets (CHECK constraint),
  one live connection per pair (partial unique index).
- **Moderation is centralized**: `BlockService` is the single interaction authority consulted
  by chat, gifts, connections, profiles, discovery and rooms; its error is deliberately
  neutral so it never reveals who blocked whom.
- **Discovery is simple weighted scoring** over a bounded candidate pool — no ML, fully
  debuggable (the score ships in the response).

**Scale target**: one Spring Boot instance + one PostgreSQL comfortably covers the initial
0–10,000 users. The seams for later (Redis presence/rate limiting, WebSocket fan-out,
paid cosmetics, animated gifts) are already in the code but deliberately unbuilt.

## API surface (summary)

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` · `login` · `refresh` |
| Me | `GET/PUT /api/users/me` · `PUT /api/users/me/intention` |
| Languages | `GET/POST /api/users/me/languages` · `DELETE .../{id}` |
| Avatar | `GET /api/avatar/assets` · `GET/PUT /api/users/me/avatar` |
| Profiles | `GET /api/users/{id}` |
| Rooms | `GET /api/rooms[?intention=]` · `GET /api/rooms/{id}` · `POST .../join` · `POST .../leave` · `GET .../users` |
| Chat | `GET /api/conversations` · `GET/POST /api/conversations/{userId}/messages` · WS `/ws/chat?token=` |
| Gifts | `GET /api/gifts` · `POST /api/gifts/send` · `GET /api/wallets/me` |
| Games | `GET /api/games` · `POST /api/games` · `POST .../{id}/accept` · `decline` · `moves` · `forfeit` |
| Discovery | `GET /api/discovery/users?intention&languageCode&countryCode&onlineOnly&ageMin&ageMax` |
| Connections | `POST /api/connections/{userId}` · `POST .../{id}/accept` · `.../reject` · `GET /api/connections` |
| Moderation | `POST/DELETE /api/users/{id}/block` · `POST /api/reports` |

Full request/response shapes: Swagger UI.

## Mobile screens

Splash · Login · Register · Home (intention cards + recommendations) · Room List · Room
(avatars at slots) · User Profile modal (chat/gift/connect/play/block/report) · Chat List ·
Private Chat · Gift Selector · Game · Avatar Creator · Profile · Edit Profile · Settings
(Languages still exists at `/(app)/languages`, just unlinked)

## Art

Midnight Cafe runs on real art: `backend/src/main/resources/static/rooms/midnight_cafe.jpg`, served
unauthenticated, with a 10-seat slot map matched to the furniture in the scene. Everything
else (avatar layers, the other four room backgrounds, gift icons) is still a `placehold.co`
URL.

To add more art: drop the file in `static/rooms/` (or `static/avatars/`, `static/gifts/`)
and write a migration that points the row at it; name room files after the room
(`coffee_break.jpg`, `singles_lounge.jpg`, ...). Asset URLs may be **relative**
(`/rooms/midnight_cafe.jpg`) — the mobile client resolves them against its API base, so one row works
in every environment. Room scenes are vertical (9:16, ~1080×1920) and belong as JPG rather
than PNG; a room's seating lives in `room_slots` as percentages, so matching seats to new
furniture is a data change, never a code change.

## Never commit secrets

`.env` files are git-ignored; only `.env.example` templates are tracked.
