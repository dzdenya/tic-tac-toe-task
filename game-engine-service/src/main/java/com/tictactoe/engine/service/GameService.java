package com.tictactoe.engine.service;

import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;

/**
 * Coordinates tic-tac-toe game state changes and exposes the current game snapshot.
 */
public interface GameService {

	/**
	 * Creates an empty game for the provided id, or returns the existing game without
	 * resetting its board.
	 *
	 * @param gameId stable game identifier supplied by the caller
	 * @return current game snapshot
	 */
	GameResponse create(String gameId);

	/**
	 * Looks up the latest persisted game state.
	 *
	 * @param gameId stable game identifier
	 * @return current game snapshot
	 */
	GameResponse getGame(String gameId);

	/**
	 * Applies a move to the game, creating the game lazily when it does not exist yet.
	 *
	 * @param gameId  stable game identifier
	 * @param request requested player and board position
	 * @return updated game snapshot after the accepted move
	 */
	GameResponse move(String gameId, MoveRequest request);

}
