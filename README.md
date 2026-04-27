# Distributed Tic Tac Toe Microservices

Distributed Tic Tac Toe home assignment built with Java 25, Spring Boot 4.0.6, Gradle 9.4.1, H2, and a separate React/Vite UI.

See [SPEC.md](SPEC.md) for the working specification.

A brief discussion of potential improvements is available in [IMPROVEMENTS.md](IMPROVEMENTS.md).

## Quick Start

The easiest way to build and run the complete application is one command from the repository root:

```bash
docker compose up --build
```

Then open:

```text
http://localhost:8080
```

This starts the React UI, the Game Session Service, and the Game Engine Service. Use `Ctrl+C` to stop the application.

## Prerequisites

For the one-command Docker run:

- Docker with Docker Compose v2

For local development without Docker:

- Java 25
- Node.js 22 or newer
- npm

## Modules

- `game-engine-service`: owns board state, move validation, and game outcome calculation.
- `game-session-service`: owns sessions, automated move simulation, move history, SSE updates, and communication with the engine through Spring WebClient.
- `ui`: separate React/Vite browser UI based on the Figma Make design sample.

## Local Ports

- Game Engine Service: `http://localhost:8081`
- Game Session Service: `http://localhost:8082`
- UI through Docker Compose: `http://localhost:8080`
- UI through Vite dev server: `http://localhost:5173`

## Build

Build and test both backend services:

```bash
./gradlew build
```

This runs compilation, Spotless formatting checks, and tests for both backend services.

Build the frontend:

```bash
cd ui
npm ci
npm run build
```

GitHub Actions runs the same Gradle build on push and pull request, and also validates the Docker Compose configuration.

## CI/CD

GitHub Actions workflows:

- `CI`: runs the backend Gradle build and validates Docker Compose. UI-only and documentation-only changes are ignored.
- `UI Pages`: builds the Vite UI and can deploy `ui/dist` to GitHub Pages on `dev`, but GitHub Pages requires a supported repository plan and Pages source configuration.
- `Deploy Dev`: on `dev`, builds and tests backend or UI changes, uploads the source bundle to the dev server over SSH, and runs the Traefik-oriented Docker Compose file on the server.

## Format

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```

## Run

### Option 1: Run Everything with One Command

Start the UI and both backend services:

```bash
docker compose up --build
```

Then open `http://localhost:8080`.

Docker Compose maps:

- UI: `http://localhost:8080`
- Game Engine Service: `http://localhost:8081`
- Game Session Service: `http://localhost:8082`

The Game Session Service calls the Game Engine Service through the internal Compose service name `game-engine-service`.
The UI is served by nginx and calls the Game Session Service on the same hostname with port `8082`.

### Option 2: Run Locally with Gradle and Vite

Run the backend services in two separate terminal windows.

Terminal 1, start the engine:

```bash
./gradlew :game-engine-service:bootRun
```

Terminal 2, start the session service:

```bash
./gradlew :game-session-service:bootRun
```

Terminal 3, install dependencies and start the UI dev server:

```bash
cd ui
npm ci
npm run dev
```

Then open `http://localhost:5173`.

When served from localhost, the UI calls `http://localhost:8082`.

The Game Session Service calls the Game Engine Service at `http://localhost:8081` in local Gradle mode.

The UI opens the session SSE stream and updates the board after each accepted move so the automated game is visible as it progresses.

### Deploy with Traefik

The dev deployment uses `.deploy/docker-compose.yml`, which connects the UI and session API to the external Traefik `web` network and routes:

- UI: `https://denys-task-flamingo.duckdns.org/tic-tac-toe/`
- API: `https://denys-task-flamingo.duckdns.org/tic-tac-toe-api`

## API

### Game Engine Service

```text
POST /games/{gameId}
POST /games/{gameId}/move
GET  /games/{gameId}
```

Move request:

```json
{
  "player": "X",
  "row": 0,
  "col": 2
}
```

The engine validates board bounds, occupied cells, completed games, and turn order.

### Game Session Service

```text
POST /sessions
POST /sessions/{sessionId}/simulate
GET  /sessions/{sessionId}
GET  /sessions/{sessionId}/events
```

The session service creates sessions, generates automated moves, calls the engine through WebClient, stores move history in H2, streams move events over SSE, and stops when the engine reports a win or draw.

Live simulation stream:

```text
GET /sessions/{sessionId}/events
```

The SSE stream emits `move` events for accepted moves and a final `completed` event with the completed session snapshot.

## API Smoke Test

With both services running:

```bash
curl -X POST http://localhost:8082/sessions
```

Use the returned `sessionId`:

```bash
curl -X POST http://localhost:8082/sessions/{sessionId}/simulate
curl http://localhost:8082/sessions/{sessionId}
```

## Error Handling

Both backend services return a consistent JSON error shape:

```json
{
  "timestamp": "2026-04-25T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cell is already occupied",
  "path": "/games/session-123/move"
}
```

Expected client errors such as invalid moves, unknown sessions, unknown endpoints, and unsupported HTTP methods are handled without being logged as unexpected server failures. Unexpected `500` errors are logged with method, path, and stack trace.

## Testing

The test suite covers:

- game engine move validation,
- win and draw detection,
- invalid endpoint and method handling,
- session creation,
- full automated simulation,
- move history persistence,
- repeated simulation rejection,
- WebClient-based session-to-engine flow using a local stub engine in tests,
- SSE simulation events.

Run everything with:

```bash
./gradlew build
```

Run only backend tests:

```bash
./gradlew test
```

Run tests for one backend service:

```bash
./gradlew :game-engine-service:test
./gradlew :game-session-service:test
```

Validate the frontend production build:

```bash
cd ui
npm ci
npm run build
```

## Notes

- The UI calls the Game Session Service only.
- The Game Session Service calls the Game Engine Service through Spring WebClient.
- Both backend services use H2 in-memory databases.
