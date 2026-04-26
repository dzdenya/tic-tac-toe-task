# Distributed Tic Tac Toe Microservices Specification

## 1. Purpose

This project implements a distributed Tic Tac Toe application where two backend services automatically play a complete game and a UI displays the game state, status, and move history.

The application is composed of:

- Game Engine Service: owns the Tic Tac Toe rules and game state.
- Game Session Service: owns sessions, move automation, and coordination with the engine.
- UI: a separate plain HTML/CSS/JS application that allows starting a simulation and observing the automated game.

The primary goal is to demonstrate clean Spring Boot service design, REST-based inter-service communication, robust validation, error handling, and integration testing.

## 2. Scope

### In Scope

- A game engine API for move validation, board updates, and game outcome calculation.
- A session API for creating sessions and simulating full automated games.
- A browser UI for starting simulations and viewing the board, status, and move history.
- H2-backed in-memory state storage.
- Tests for core game rules and the full automated flow.
- README documentation for building, running, and testing.

### Optional Scope

- Server-Sent Events or WebSockets for live UI updates.
- Concurrent move protection.
- Persistent storage.
- API gateway or service discovery.

### Out of Scope for the Initial Version

- User authentication.
- Human-vs-human gameplay.
- Configurable board sizes.
- Production-grade deployment infrastructure.

## 3. Architecture

### 3.1 Components

#### Game Engine Service

The Game Engine Service is responsible for authoritative game state and rule enforcement.

Responsibilities:

- Create or lazily initialize a game by `gameId`.
- Store the board state.
- Validate moves.
- Reject moves when the target cell is occupied.
- Reject moves when the game is already finished.
- Determine whether the game is in progress, won, or drawn after each accepted move.
- Return the latest game state.

#### Game Session Service

The Game Session Service is responsible for session lifecycle and automated gameplay.

Responsibilities:

- Create sessions.
- Use the session ID as the game ID when communicating with the Game Engine Service.
- Generate automated moves for players `X` and `O`.
- Alternate turns between players.
- Forward moves to the Game Engine Service through a Spring Cloud OpenFeign client.
- Store session status and move history.
- Stop simulation when the Game Engine Service reports a win or draw.
- Expose session details to the UI.

#### User Interface

The UI is responsible for visualizing and controlling the automated simulation.

Responsibilities:

- Display a `Start Simulation` action.
- Render a 3x3 board.
- Show game status.
- Show winner when available.
- Show move history.
- Present backend or communication errors.
- Replay returned move history with a short delay so the automated game progress is visible.
- Communicate with the Game Session Service only.

## 4. Domain Model

### 4.1 Board

The board is a 3x3 grid with positions represented by row and column indexes:

- `row`: integer from `0` to `2`
- `col`: integer from `0` to `2`

Each cell can contain:

- `X`
- `O`
- empty value

### 4.2 Player Symbol

Allowed player symbols:

- `X`
- `O`

### 4.3 Game Status

Allowed game statuses:

- `IN_PROGRESS`
- `X_WON`
- `O_WON`
- `DRAW`

### 4.4 Session Status

Allowed session statuses:

- `CREATED`
- `SIMULATING`
- `COMPLETED`
- `FAILED`

## 5. REST API Contracts

### 5.1 Game Engine Service

Base URL for local development:

```text
http://localhost:8081
```

#### POST `/games/{gameId}/move`

Accepts a move, validates it, updates the board, and returns the current game state.

Request:

```json
{
  "player": "X",
  "row": 0,
  "col": 2
}
```

Successful response:

```json
{
  "gameId": "session-123",
  "board": [
    ["X", null, "O"],
    [null, "X", null],
    [null, null, null]
  ],
  "status": "IN_PROGRESS",
  "winner": null,
  "lastMove": {
    "player": "X",
    "row": 0,
    "col": 2
  }
}
```

Validation errors:

- Unknown player symbol.
- Row or column outside the board.
- Target cell already occupied.
- Same player attempts to move twice in a row.
- Move attempted after game completion.

#### GET `/games/{gameId}`

Returns the current game state.

Successful response:

```json
{
  "gameId": "session-123",
  "board": [
    ["X", null, "O"],
    [null, "X", null],
    [null, null, null]
  ],
  "status": "IN_PROGRESS",
  "winner": null,
  "lastMove": {
    "player": "X",
    "row": 0,
    "col": 2
  }
}
```

If no game exists for the provided `gameId`, the service may either:

- return `404 NOT_FOUND`, or
- lazily create an empty game when the first move is submitted.

The initial implementation will use lazy creation on first move and `404 NOT_FOUND` for direct lookup of an unknown game.

### 5.2 Game Session Service

Base URL for local development:

```text
http://localhost:8082
```

#### POST `/sessions`

Creates a new game session.

Successful response:

```json
{
  "sessionId": "session-123",
  "gameId": "session-123",
  "status": "CREATED",
  "game": null,
  "moves": []
}
```

#### POST `/sessions/{sessionId}/simulate`

Runs an automated game until the game reaches a win or draw.

Successful response:

```json
{
  "sessionId": "session-123",
  "gameId": "session-123",
  "status": "COMPLETED",
  "game": {
    "gameId": "session-123",
    "board": [
      ["X", "O", "X"],
      ["O", "X", "O"],
      ["X", null, null]
    ],
    "status": "X_WON",
    "winner": "X",
    "lastMove": {
      "player": "X",
      "row": 2,
      "col": 0
    }
  },
  "moves": [
    {
      "turn": 1,
      "player": "X",
      "row": 0,
      "col": 0,
      "resultingStatus": "IN_PROGRESS"
    }
  ]
}
```

Simulation rules:

- Player `X` always starts.
- Players alternate turns.
- The first implementation may use a random legal move strategy.
- Simulation stops immediately when the engine reports `X_WON`, `O_WON`, or `DRAW`.
- Re-simulating a completed session should return a validation error.

#### GET `/sessions/{sessionId}`

Returns session details, current game state, and move history.

Successful response:

```json
{
  "sessionId": "session-123",
  "gameId": "session-123",
  "status": "COMPLETED",
  "game": {
    "gameId": "session-123",
    "board": [
      ["X", "O", "X"],
      ["O", "X", "O"],
      ["X", null, null]
    ],
    "status": "X_WON",
    "winner": "X",
    "lastMove": {
      "player": "X",
      "row": 2,
      "col": 0
    }
  },
  "moves": [
    {
      "turn": 1,
      "player": "X",
      "row": 0,
      "col": 0,
      "resultingStatus": "IN_PROGRESS"
    }
  ]
}
```

## 6. Error Handling

All services should return consistent JSON error responses.

Error response shape:

```json
{
  "timestamp": "2026-04-25T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cell is already occupied",
  "path": "/games/session-123/move"
}
```

Expected status codes:

- `400 BAD_REQUEST`: invalid move, invalid player, invalid board position, completed game mutation.
- `404 NOT_FOUND`: unknown game or session.
- `404 NOT_FOUND`: unknown endpoint.
- `405 METHOD_NOT_ALLOWED`: unsupported HTTP method for an existing endpoint.
- `409 CONFLICT`: simulation requested for a session that is already simulating or completed.
- `502 BAD_GATEWAY`: Game Session Service cannot communicate with Game Engine Service.
- `500 INTERNAL_SERVER_ERROR`: unexpected server error.

Each service should use a global exception handler with `@ControllerAdvice`. Unexpected `500` errors should be logged with method, path, and stack trace. Expected client errors such as `400`, `404`, `405`, and `409` should return consistent JSON responses without being logged as unexpected server failures.

## 7. State Management

### Initial Implementation

- Game Engine Service: H2 in-memory database storing games and board state.
- Game Session Service: H2 in-memory database storing sessions and move history.

### Concurrency Considerations

The initial version should avoid corrupt state when two requests target the same game or session.

Recommended approach:

- Keep game state updates atomic per `gameId`.
- Prevent multiple concurrent simulations for the same `sessionId`.
- Return `409 CONFLICT` when a session is already simulating.

## 8. Automated Move Strategy

The first version will use a simple legal-move strategy:

1. Fetch or track the latest board.
2. Compute empty cells.
3. Choose one empty cell.
4. Submit the move to the Game Engine Service.
5. Record the accepted move and resulting status.

The strategy can initially be random. A later improvement may use a rule-based strategy:

- win if possible,
- block opponent win if needed,
- prefer center,
- prefer corners,
- otherwise choose any legal cell.

## 9. UI Behavior

The initial UI will be implemented as a separate plain HTML, CSS, and JavaScript application. It will not use a frontend framework in the first version.

The UI should call only the Game Session Service. The Game Engine Service remains an internal backend dependency of the Game Session Service.

The Game Session Service should allow local UI access through CORS for development.

### Initial Screen

- Show an empty 3x3 board.
- Show a `Start Simulation` button.
- Show an empty move history.

### Simulation Flow

1. User clicks `Start Simulation`.
2. UI calls `POST /sessions` on the Game Session Service.
3. UI calls `POST /sessions/{sessionId}/simulate` on the Game Session Service.
4. UI replays the returned move history step by step with a short delay between moves.
5. UI renders the final game status and board.

### Optional Live Flow

If SSE or WebSockets are implemented:

1. User clicks `Start Simulation`.
2. UI creates a session.
3. UI subscribes to session updates.
4. UI triggers simulation.
5. UI updates the board after every move.

## 10. Testing Strategy

### Game Engine Service Tests

- Accepts valid moves.
- Rejects moves outside the board.
- Rejects moves to occupied cells.
- Rejects the same player moving twice in a row.
- Detects row wins.
- Detects column wins.
- Detects diagonal wins.
- Detects draw.
- Rejects moves after game completion.
- Returns `404` for unknown endpoints.
- Returns `405` for unsupported HTTP methods.

### Game Session Service Tests

- Creates a session.
- Simulates a full game.
- Alternates players correctly.
- Stores move history.
- Stops when the engine reports a terminal status.
- Handles Game Engine Service communication failures.
- Rejects simulation for unknown or completed sessions.
- Returns `404` for unknown endpoints.
- Returns `405` for unsupported HTTP methods.

### Integration Tests

- Full flow: create session, simulate game, verify terminal game outcome.
- OpenFeign communication between Game Session Service and Game Engine Service.
- Error response shape for representative invalid requests.

## 11. Implementation Constraints

- Use Java and Spring Boot.
- Use Java 25.
- Use Spring Boot 4.0.6.
- Use Gradle 9.4.1 with Groovy DSL.
- Use package name `com.flamingo.tictactoe`.
- Organize each service by layer (`controller`, `service`, `repository`, `model`, `dto`, `exception`, and service-specific `config` where needed).
- Use Spring Cloud OpenFeign for Game Session Service to Game Engine Service communication.
- Use Lombok where it reduces boilerplate without hiding business logic.
- Use Spotless for Java formatting.
- Use constructor injection and `private final` fields.
- Use DTOs for API input and output.
- Do not expose persistence or internal domain objects directly through controllers.
- Use Bean Validation for request validation.
- Use SLF4J for logging.
- Use H2 for in-memory state storage.
- Document local ports and run commands in `README.md`.

## 12. Development Milestones

### Milestone 1: Project Skeleton

- Create Gradle multi-service structure.
- Add Game Engine Service.
- Add Game Session Service.
- Add separate plain HTML/CSS/JS UI app.
- Add basic README.

### Milestone 2: Game Engine

- Implement domain model.
- Implement move endpoint.
- Implement game lookup endpoint.
- Add validation and global error handling.
- Add unit and integration tests.

### Milestone 3: Game Session

- Implement session creation.
- Implement OpenFeign client for Game Engine Service.
- Implement automated simulation.
- Store move history.
- Add service and integration tests.

### Milestone 4: UI

- Implement start simulation flow.
- Render board, status, and move history.
- Handle loading and error states.

### Milestone 5: Polish

- Improve README.
- Add optional live updates if time allows.
- Add concurrency protection.
- Review code quality and assignment alignment.

## 13. Open Decisions

- Whether to use random moves only or a simple rule-based strategy.
- Whether to implement live updates in the first version or keep the initial UI request-response based.
- Follow-up improvements are tracked separately in `IMPROVEMENT_PLAN.md` so this specification stays focused on the current implementation contract.

## 14. Proposed Initial Decisions

- Use a Gradle multi-project repository.
- Use Java 25, Spring Boot 4.0.6, and Gradle 9.4.1 with Groovy DSL.
- Use package name `com.flamingo.tictactoe`.
- Use two Spring Boot services:
  - `game-engine-service` on port `8081`
  - `game-session-service` on port `8082`
- Use H2 in-memory databases for both backend services.
- Use Spring Cloud OpenFeign communication from Game Session Service to Game Engine Service.
- Use a separate plain HTML/CSS/JS browser UI for the first version.
- Use request-response simulation first and replay returned moves in the UI; add SSE only if time remains.
