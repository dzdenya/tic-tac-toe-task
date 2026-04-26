package com.tictactoe.engine.service;

import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;

/**
 * Coordinates tic-tac-toe game state changes and exposes the current game snapshot.
 */
public interface GameService {

	/**
	 * Returns the persisted game state for the given identifier.
	 *
	 * @param gameId stable game identifier
	 * @return current game snapshot
	 */
	GameResponse getGame(String gameId);

	/**
	 * Applies a move to the game, creating the game record if this is the first move.
	 *
	 * @param gameId stable game identifier
	 * @param request player and board position to apply
	 * @return updated game snapshot after validation and outcome evaluation
	 */
	GameResponse move(String gameId, MoveRequest request);

}
