# Distributed Tic Tac Toe Microservices

Distributed Tic Tac Toe home assignment built with Java 25, Spring Boot 4.0.6, Gradle 9.4.1, H2, and a separate plain HTML/CSS/JS UI.

See [SPEC.md](SPEC.md) for the working specification and [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) for the prioritized follow-up plan.

## Modules

- `game-engine-service`: owns board state, move validation, and game outcome calculation.
- `game-session-service`: owns sessions, automated move simulation, and communication with the engine through Spring Cloud OpenFeign.
- `ui`: separate plain HTML/CSS/JS browser UI.

## Local Ports

- Game Engine Service: `http://localhost:8081`
- Game Session Service: `http://localhost:8082`
- UI: open `ui/index.html` in a browser.

## Build

```bash
./gradlew build
```

This runs compilation, Spotless formatting checks, and tests for both backend services.

GitHub Actions runs the same Gradle build on push and pull request, and also validates the Docker Compose configuration.

## CI/CD

GitHub Actions workflows:

- `CI`: runs the backend Gradle build and validates Docker Compose. UI-only and documentation-only changes are ignored.
- `UI Pages`: deploys the static `ui` directory to GitHub Pages on `dev` when files under `ui/**` change.
- `Deploy Dev`: on `dev`, builds and tests backend changes, uploads the source bundle to the dev server over SSH, and runs `docker compose -p tic-tac-toe up -d --build` on the server.

Required GitHub secret for dev deploy:

- `DEPLOY_SSH_KEY`: private SSH key for the deploy user.

Optional GitHub variables for dev deploy:

- `DEPLOY_HOST`: defaults to `141.144.230.191`.
- `DEPLOY_USER`: defaults to `opc`.
- `DEPLOY_PORT`: defaults to `22`.
- `DEPLOY_PATH`: defaults to `/home/opc/tic-tac-toe`.
- `COMPOSE_PROJECT_NAME`: defaults to `tic-tac-toe`.

## Format

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```

## Run

### Run with Docker Compose

Start both backend services:

```bash
docker compose up --build
```

Then open `ui/index.html`.

Docker Compose maps:

- Game Engine Service: `http://localhost:8081`
- Game Session Service: `http://localhost:8082`

The Game Session Service calls the Game Engine Service through the internal Compose service name `game-engine-service`.

### Run Locally with Gradle

Run the engine:

```bash
./gradlew :game-engine-service:bootRun
```

Run the session service:

```bash
./gradlew :game-session-service:bootRun
```

Then open `ui/index.html`.

The UI calls `http://localhost:8082`, and the Game Session Service calls the Game Engine Service at `http://localhost:8081`.

The UI displays the returned move history step by step with a short delay so the automated game is visible instead of appearing instantly.

## API

### Game Engine Service

```text
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
```

The session service creates sessions, generates automated moves, calls the engine through OpenFeign, stores move history in H2, and stops when the engine reports a win or draw.

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
- OpenFeign-based session-to-engine flow using a local stub engine in tests.

Run everything with:

```bash
./gradlew build
```

## Potential Improvements

The prioritized improvement roadmap lives in [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md).

## Notes

- The UI calls the Game Session Service only.
- The Game Session Service calls the Game Engine Service through Spring Cloud OpenFeign.
- Both backend services use H2 in-memory databases.
