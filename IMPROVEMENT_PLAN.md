# Improvement Plan

This document tracks improvements beyond the current baseline implementation described in [SPEC.md](SPEC.md).

## Goals

- Improve assignment polish without obscuring the core microservice design.
- Make local setup and verification easier.
- Strengthen correctness around concurrency and service communication.
- Improve the UI experience while keeping the first version plain HTML/CSS/JS.

## Priority 1: Submission Polish

### Completed

- Docker Compose starts both backend services with one command.
- GitHub Actions runs the Gradle build and validates Docker Compose configuration on push and pull request.
- GitHub Actions deploys the static UI to GitHub Pages when `ui/**` changes on `dev`.
- GitHub Actions can deploy backend services to the dev server over SSH on `dev`.

### 1. Add a One-Command Local Startup Option

Current state:

- Docker Compose starts both backend services with one command.
- UI is still opened separately from `ui/index.html`.

Remaining improvement:

- Optionally add Gradle tasks or scripts if a non-Docker startup shortcut is useful.
- Optionally serve the static UI from a lightweight local server or document a browser-friendly file workflow.

Acceptance criteria:

- A reviewer can start both backend services with `docker compose up --build`.
- README includes the exact command sequence.

### 2. Add API Examples for Both Services

Current state:

- README includes session service smoke-test examples.

Improvement:

- Add curl examples for:
  - valid engine move,
  - invalid duplicate player move,
  - occupied cell,
  - session simulation,
  - unknown endpoint,
  - wrong HTTP method.

Acceptance criteria:

- README can be used as a quick manual test guide.
- Examples match the actual JSON response shape.

### 3. Add Architecture Diagram

Current state:

- Architecture is described in text.

Improvement:

- Add a Mermaid diagram to README or SPEC showing:
  - UI -> Game Session Service,
  - Game Session Service -> Game Engine Service via OpenFeign,
  - each service -> H2 in-memory database.

Acceptance criteria:

- A reviewer can understand the service boundaries at a glance.

## Priority 2: Correctness and Robustness

### 4. Add Explicit Turn Management in the Engine

Current state:

- The engine rejects the same player moving twice in a row.
- The first move can currently be either `X` or `O`.

Improvement:

- Enforce `X` as the first player in the Game Engine Service.
- Consider returning a clear error when the wrong player attempts to move.

Acceptance criteria:

- `O` cannot make the first move.
- Existing session simulation still passes.
- Tests cover wrong first player and repeated player moves.

### 5. Add Optimistic Locking for Game and Session State

Current state:

- Service methods use `synchronized`, which only protects a single JVM instance.

Improvement:

- Add JPA `@Version` fields to game and session entities.
- Convert conflicting concurrent updates into `409 CONFLICT`.
- Keep `synchronized` only if still useful for local simplicity, or remove it once optimistic locking is in place.

Acceptance criteria:

- Concurrent move or simulation attempts cannot silently corrupt state.
- Tests cover conflicting updates where practical.

### 6. Improve Game Session Failure Recovery

Current state:

- Engine communication failures are mapped to `502 BAD_GATEWAY`.
- Session status can be set to `SIMULATING` before an engine failure.

Improvement:

- Mark the session as `FAILED` when simulation fails after it has started.
- Store enough context to inspect the failure.
- Consider allowing retry only for failed sessions that have not produced terminal game state.

Acceptance criteria:

- Failed simulations are visible through `GET /sessions/{sessionId}`.
- Tests cover engine communication failure and resulting session status.

### 7. Use a Rule-Based Move Strategy

Current state:

- Session service uses random legal moves.

Improvement:

- Replace or supplement random choice with a simple strategy:
  - win if possible,
  - block opponent win,
  - prefer center,
  - prefer corners,
  - otherwise choose any legal cell.

Acceptance criteria:

- Strategy is deterministic enough for easier testing.
- Full-game simulation still always terminates.

## Priority 3: UI Experience

### 8. Add Real-Time Updates with SSE

Current state:

- Backend simulation completes in one request.
- UI replays returned move history with a client-side delay.

Improvement:

- Add Server-Sent Events from Game Session Service:
  - `GET /sessions/{sessionId}/events`
  - stream each accepted move and final game state.
- UI updates as moves are produced by the backend.

Acceptance criteria:

- Board updates are backend-driven.
- UI no longer needs to fake replay timing after a completed response.

### 9. Add UI Controls for Replay Speed

Current state:

- UI uses a fixed delay between moves.

Improvement:

- Add a small speed control:
  - slow,
  - normal,
  - fast.

Acceptance criteria:

- User can adjust move replay speed before starting simulation.
- Layout remains stable on mobile and desktop.

### 10. Improve UI Error Presentation

Current state:

- UI shows backend error messages as plain text.

Improvement:

- Show structured error state with:
  - HTTP status,
  - message,
  - retry action where appropriate.

Acceptance criteria:

- Backend or communication failures are visible and easy to understand.

## Priority 4: Operations and Maintainability

### 11. Add Profiles for Local and Test Configuration

Current state:

- Services use default `application.yml`.

Improvement:

- Add `application-local.yml` and `application-test.yml` if configuration grows.
- Keep H2 defaults for the assignment.

Acceptance criteria:

- Test and local runtime configuration are easy to distinguish.

### 12. Add OpenAPI Documentation

Current state:

- API contracts are documented manually in SPEC and README.

Improvement:

- Add Springdoc/OpenAPI if compatible with Spring Boot 4.
- Expose generated API docs for both services.

Acceptance criteria:

- API contracts can be inspected from a browser.
- Manual docs remain aligned with generated docs.

### 13. Add Structured Logging Context

Current state:

- Unexpected errors are logged with method and path.

Improvement:

- Add request IDs or correlation IDs.
- Include `sessionId` and `gameId` where relevant.

Acceptance criteria:

- Logs can be correlated across Game Session Service and Game Engine Service.

## Suggested Order

1. Add README API examples and architecture diagram.
2. Enforce `X` as first engine move.
3. Improve session failure recovery.
4. Add optimistic locking or a narrower concurrency guard.
5. Add rule-based move strategy.
6. Add SSE for backend-driven live UI updates.
7. Optionally add a non-Docker startup shortcut if needed.

## Not Recommended for This Assignment Unless Time Remains

- Full service discovery with Eureka.
- API gateway.
- Authentication and authorization.
- Persistent production database setup.
- Heavy frontend framework migration.

These would add complexity but would not improve the core Tic Tac Toe microservices demonstration as much as correctness, tests, and a clean reviewer experience.
