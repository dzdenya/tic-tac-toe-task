package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Board;
import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.dto.MoveResponse;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.model.GameEntity;
import com.tictactoe.engine.model.PlayerSymbol;
import com.tictactoe.engine.repository.GameRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameServiceImpl implements GameService {

	private static final int GRID_SIZE = 3;

	private final GameRepository gameRepository;

	public GameServiceImpl(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Transactional
	@Override
	public GameResponse create(String gameId) {
		GameEntity game = gameRepository.findById(gameId)
				.orElseGet(() -> new GameEntity(gameId));

		return toResponse(gameRepository.save(game));
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
	public GameResponse move(String gameId, MoveRequest request) {
		GameEntity game = gameRepository.findById(gameId)
				.orElseGet(() -> new GameEntity(gameId));

		game.makeMove(request.player(), request.row(), request.col());

		return toResponse(gameRepository.save(game));
	}

	private GameResponse toResponse(GameEntity game) {
		return new GameResponse(
				game.getId(),
				toBoard(game),
				game.getStatus(),
				game.getWinner(),
				toLastMove(game)
		);
	}

	private MoveResponse toLastMove(GameEntity game) {
		if (game.getLastPlayer() == null) {
			return null;
		}
		return new MoveResponse(
				game.getLastPlayer(),
				game.getLastRow(),
				game.getLastCol()
		);
	}

	/**
	 * Converts the compact persisted board representation into the API's 3x3 grid.
	 */
	private List<List<PlayerSymbol>> toBoard(GameEntity game) {
		Board board = Board.fromString(game.getCells());

		return IntStream.range(0, GRID_SIZE)
				.mapToObj(row ->
						IntStream.range(0, GRID_SIZE)
								.mapToObj(col -> board.get(row, col))
								.toList()
				)
				.toList();
	}
}
