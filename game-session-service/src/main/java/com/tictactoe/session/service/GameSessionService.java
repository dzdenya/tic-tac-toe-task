package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionEventResponse;
import com.tictactoe.session.dto.SessionResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Manages tic-tac-toe simulation sessions and their recorded move history.
 */
public interface GameSessionService {

	/**
	 * Creates a new session with an associated engine game id and initializes the
	 * matching engine game before persisting the session.
	 *
	 * @return newly created session snapshot
	 */
	Mono<SessionResponse> createSession();

	/**
	 * Returns a session with its move history and current engine game state when available.
	 *
	 * @param sessionId stable session identifier
	 * @return current session snapshot
	 */
	Mono<SessionResponse> getSession(String sessionId);

	/**
	 * Runs automated moves until the engine reports a win or draw.
	 *
	 * @param sessionId stable session identifier
	 * @return completed session snapshot
	 */
	Mono<SessionResponse> simulate(String sessionId);

	/**
	 * Runs automated moves and emits an SSE-friendly event after each accepted move,
	 * followed by a completion event that contains the final session snapshot.
	 *
	 * @param sessionId stable session identifier
	 * @return simulation events as they are accepted
	 */
	Flux<SessionEventResponse> streamSimulation(String sessionId);

}
