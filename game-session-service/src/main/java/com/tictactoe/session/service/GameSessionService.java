package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionResponse;

/**
 * Manages tic-tac-toe simulation sessions and their recorded move history.
 */
public interface GameSessionService {

	/**
	 * Creates a new session with an associated engine game id.
	 *
	 * @return newly created session snapshot
	 */
	SessionResponse createSession();

	/**
	 * Returns a session with its move history and current engine game state when available.
	 *
	 * @param sessionId stable session identifier
	 * @return current session snapshot
	 */
	SessionResponse getSession(String sessionId);

	/**
	 * Runs automated moves until the engine reports a win or draw.
	 *
	 * @param sessionId stable session identifier
	 * @return completed session snapshot
	 */
	SessionResponse simulate(String sessionId);

}
