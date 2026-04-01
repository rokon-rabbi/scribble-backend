# Scribble Clone — Backend

Real-time multiplayer drawing-and-guessing game backend (like Skribbl.io), built with Spring Boot 3, WebSockets (STOMP), PostgreSQL, and Redis.

## Tech Stack

- **Java 21** + **Spring Boot 3.4**
- **WebSocket** — STOMP protocol + SockJS fallback
- **PostgreSQL 16** — persistent data (players, games, rounds, scores)
- **Redis 7** — live game state, room management, stroke buffering
- **Spring Security** + **JWT** — stateless authentication
- **Flyway** — database migrations
- **SpringDoc OpenAPI** — Swagger UI
- **Gradle Kotlin DSL** — build tool

## Project Structure

Domain-driven packaging (package-by-feature, not package-by-layer):

```
src/main/java/com/scribble/
├── ScribbleApplication.java
├── config/              # Security, WebSocket, Redis, OpenAPI, CORS, rate limiting
├── common/              # Shared exceptions, DTOs (ApiResponse), utilities
├── auth/                # Register, login, JWT token provider, auth filter
├── player/              # Player entity, profile, leaderboard
├── game/                # Game/room CRUD, game flow state machine
├── participant/         # Game-player join table
├── round/               # Round entity, round lifecycle
├── word/                # Word bank entity, random word selection
├── guess/               # Guess processing, correct/incorrect routing
├── score/               # Round scoring (guesser time-based, drawer flat)
├── stroke/              # Canvas stroke persistence (Redis → PostgreSQL)
└── websocket/           # STOMP handlers (room, drawing, guessing, game flow)
```

## Prerequisites

- Java 21+
- Docker & Docker Compose

## Getting Started

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL on port **5433**
- Redis on port **6380**

### 2. Run the application

```bash
./gradlew bootRun
```

The app starts on **http://localhost:8080**. Flyway automatically runs all migrations and seeds 60 words.

### 3. Verify

- **Health check**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API docs**: http://localhost:8080/v3/api-docs

## API Endpoints

### REST

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register a new player |
| POST | `/api/v1/auth/login` | No | Login, receive JWT |
| GET | `/api/v1/players/me` | JWT | Get own profile |
| GET | `/api/v1/players/leaderboard` | JWT | Get global leaderboard |
| POST | `/api/v1/games` | JWT | Create a game room |
| GET | `/api/v1/games/public` | JWT | List public waiting rooms |
| GET | `/api/v1/games/{roomCode}` | JWT | Get room info |

### WebSocket (STOMP)

Connect to `ws://localhost:8080/ws` with SockJS. Pass JWT in the `Authorization` header on CONNECT.

**Client sends to:**

| Destination | Description |
|-------------|-------------|
| `/app/room/{roomCode}/join` | Join a room |
| `/app/room/{roomCode}/start` | Owner starts the game |
| `/app/room/{roomCode}/choose-word` | Drawer picks a word `{wordId}` |
| `/app/room/{roomCode}/draw` | Send a stroke |
| `/app/room/{roomCode}/guess` | Send a guess |
| `/app/room/{roomCode}/clear-canvas` | Drawer clears the canvas |

**Client subscribes to:**

| Topic | Description |
|-------|-------------|
| `/topic/room/{roomCode}/players` | Player join/leave/disconnect events |
| `/topic/room/{roomCode}/game` | Game events (start, round_choosing, round_start, round_end, game_end) |
| `/topic/room/{roomCode}/draw` | Stroke broadcasts |
| `/topic/room/{roomCode}/chat` | Chat messages (wrong guesses) |
| `/topic/room/{roomCode}/guess-result` | Correct guess notifications |
| `/topic/room/{roomCode}/word-choices` | Word options sent to drawer |
| `/topic/room/{roomCode}/reconnect/{playerId}` | Reconnection state + stroke replay |

## Game Flow

```
1. Owner creates room            → POST /api/v1/games
2. Players join via WebSocket     → /app/room/{code}/join
3. Owner starts game              → /app/room/{code}/start
4. Server picks 3 words           → sent to drawer via /topic/.../word-choices
5. Drawer picks a word            → /app/room/{code}/choose-word
6. Server broadcasts hint         → "_ _ _ _ _" via /topic/.../game (round_start)
7. Drawer draws on canvas         → strokes via /app/room/{code}/draw
8. Others guess in chat           → /app/room/{code}/guess
9. Correct guess                  → notified via /topic/.../guess-result
10. Round ends (timer or all guessed) → scores broadcast via /topic/.../game (round_end)
11. Repeat for all rounds
12. Game ends                     → final leaderboard via /topic/.../game (game_end)
```

## Scoring

- **Guesser**: `(timeRemaining / totalTime) × 100` — faster guesses earn more points
- **Drawer**: `20 × correctGuessCount` — flat reward per correct guesser

## Database

9 Flyway migrations create: `players`, `words`, `games`, `game_participants`, `rounds`, `guesses`, `round_scores`, `strokes`, plus 60 seed words (20 easy, 20 medium, 20 hard).

## Redis Keys

| Key | Type | Description |
|-----|------|-------------|
| `room:{code}` | Hash | Game state (status, drawerId, currentWord, timer) |
| `room:{code}:players` | Set | Player IDs in the room |
| `room:{code}:player:{id}` | Hash | Player info (username, score, connected) |
| `room:{code}:strokes` | List | Stroke buffer for current round |
| `ws:session:{id}` | Hash | WebSocket session → room mapping |
| `rooms:public` | Set | Public waiting room codes |
| `ratelimit:guess:{id}` | String | Rate limit counter (2/sec) |

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_PORT` | 5433 | PostgreSQL port |
| `DB_USERNAME` | scribble | Database username |
| `DB_PASSWORD` | scribble | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6380 | Redis port |
| `JWT_SECRET` | (dev default) | JWT signing key (change in production) |

## Testing

```bash
./gradlew test
```

Runs 7 integration tests against the Docker Compose services (PostgreSQL + Redis must be running).

## Build

```bash
./gradlew build
```

Produces `build/libs/scribble-0.0.1-SNAPSHOT.jar`.
