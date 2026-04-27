# Improvements and Alternative Design

## Potential Improvements

- Use a real persistent database, such as PostgreSQL, instead of the current in-memory H2 database so games and sessions survive service restarts.
- Add a separate gameplay service that supports more flexible game modes, such as playing against the computer or player-vs-player sessions.
- Replace or extend random move selection with smarter strategies, such as trying to win first and blocking the opponent's winning move. For computer games, allow selecting a difficulty level.
- Allow configurable board sizes, such as 3x3, 5x5, or 7x7, instead of supporting only the current fixed 3x3 board.

## Alternative Design Approaches

Kafka could be introduced for event-driven communication between services. This would make the architecture more scalable and decoupled, but it would also add extra infrastructure complexity.

A distributed transaction approach, such as two-phase commit, could keep updates across services strictly consistent. However, it would make the system more complex and tightly coupled, so it is not necessary for the current scope.
