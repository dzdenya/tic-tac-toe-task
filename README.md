# Distributed Tic Tac Toe Microservices

Distributed Tic Tac Toe home assignment built with Java 25, Spring Boot 4.0.6, Gradle 9.4.1, H2, and a separate plain HTML/CSS/JS UI.

See [SPEC.md](SPEC.md) for the working specification.

## Modules

- `game-engine-service`: owns board state, move validation, and game outcome calculation.
- `game-session-service`: owns sessions, automated move simulation, and communication with the engine.
- `ui`: separate plain HTML/CSS/JS browser UI.

## Local Ports

- Game Engine Service: `http://localhost:8081`
- Game Session Service: `http://localhost:8082`
- UI: open `ui/index.html` in a browser.

## Build

```bash
./gradlew build
```

## Run

Run the engine:

```bash
./gradlew :game-engine-service:bootRun
```

Run the session service:

```bash
./gradlew :game-session-service:bootRun
```

Then open `ui/index.html`.

## Notes

- The UI calls the Game Session Service only.
- The Game Session Service calls the Game Engine Service through REST.
- Both backend services use H2 in-memory databases.
