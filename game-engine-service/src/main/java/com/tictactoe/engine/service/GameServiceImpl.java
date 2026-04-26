package com.tictactoe.engine.service;

import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.dto.MoveResponse;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.exception.InvalidMoveException;
import com.tictactoe.engine.model.GameEntity;
import com.tictactoe.engine.model.GameStatus;
import com.tictactoe.engine.model.PlayerSymbol;
import com.tictactoe.engine.repository.GameRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional implementation of the game engine rules.
 *
 * <p>The service is stateless; all mutable game data is loaded from and saved to
 * {@link GameRepository}. Move submission is synchronized to prevent concurrent
 * requests in the same JVM from applying conflicting turns.</p>
 */
@Service
public class GameServiceImpl implements GameService {

	private static final int[][] WINNING_LINES = {
			{0, 1, 2},
			{3, 4, 5},
			{6, 7, 8},
			{0, 3, 6},
			{1, 4, 7},
			{2, 5, 8},
			{0, 4, 8},
			{2, 4, 6}
	};

	private final GameRepository gameRepository;

	public GameServiceImpl(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Transactional(readOnly = true)
	@Override
	public GameResponse getGame(String gameId) {
		return gameRepository.findById(gameId)
				.map(this::toResponse)
				.orElseThrow(() -> new GameNotFoundException(gameId));
	}

	@Transactional
	@Override
	public synchronized GameResponse move(String gameId, MoveRequest request) {
		GameEntity game = gameRepository.findById(gameId)
				.orElseGet(() -> new GameEntity(gameId));

		if (game.getStatus().isTerminal()) {
			throw new InvalidMoveException("Game is already completed");
		}

		if (request.player() == game.getLastPlayer()) {
			throw new InvalidMoveException("Player cannot move twice in a row");
		}

		if (!game.isCellEmpty(request.row(), request.col())) {
			throw new InvalidMoveException("Cell is already occupied");
		}

		game.applyMove(request.player(), request.row(), request.col());
		updateOutcome(game);

		return toResponse(gameRepository.save(game));
	}

	/**
	 * Evaluates terminal game states after a valid move has been applied.
	 */
	private void updateOutcome(GameEntity game) {
		String cells = game.getCells();

		for (int[] line : WINNING_LINES) {
			char first = cells.charAt(line[0]);
			if (first != '-' && first == cells.charAt(line[1]) && first == cells.charAt(line[2])) {
				PlayerSymbol winner = PlayerSymbol.valueOf(String.valueOf(first));
				game.complete(winner == PlayerSymbol.X ? GameStatus.X_WON : GameStatus.O_WON, winner);
				return;
			}
		}

		if (!cells.contains("-")) {
			game.complete(GameStatus.DRAW, null);
		}
	}

	private GameResponse toResponse(GameEntity game) {
		return new GameResponse(
				game.getId(),
				toBoard(game.getCells()),
				game.getStatus(),
				game.getWinner(),
				toLastMove(game)
		);
	}

	private MoveResponse toLastMove(GameEntity game) {
		if (game.getLastPlayer() == null) {
			return null;
		}
		return new MoveResponse(game.getLastPlayer(), game.getLastRow(), game.getLastCol());
	}

	private List<List<PlayerSymbol>> toBoard(String cells) {
		List<List<PlayerSymbol>> board = new ArrayList<>();
		for (int row = 0; row < 3; row++) {
			List<PlayerSymbol> rowCells = new ArrayList<>();
			for (int col = 0; col < 3; col++) {
				char value = cells.charAt(row * 3 + col);
				rowCells.add(value == '-' ? null : PlayerSymbol.valueOf(String.valueOf(value)));
			}
			board.add(rowCells);
		}
		return board;
	}
}
