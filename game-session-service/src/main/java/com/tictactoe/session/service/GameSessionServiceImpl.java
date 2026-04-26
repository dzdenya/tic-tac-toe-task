package com.tictactoe.session.service;

import com.tictactoe.session.dto.EngineMoveRequest;
import com.tictactoe.session.dto.GameResponse;
import com.tictactoe.session.dto.SessionMoveResponse;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.exception.EngineCommunicationException;
import com.tictactoe.session.exception.SessionConflictException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.model.GameSessionEntity;
import com.tictactoe.session.model.MoveEntity;
import com.tictactoe.session.model.PlayerSymbol;
import com.tictactoe.session.model.SessionStatus;
import com.tictactoe.session.repository.GameSessionRepository;
import feign.FeignException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class GameSessionServiceImpl implements GameSessionService {

	private final GameSessionRepository gameSessionRepository;
	private final GameEngineClient gameEngineClient;

	public GameSessionServiceImpl(GameSessionRepository gameSessionRepository,
	                              GameEngineClient gameEngineClient) {
		this.gameSessionRepository = gameSessionRepository;
		this.gameEngineClient = gameEngineClient;
	}

	@Transactional
	@Override
	public SessionResponse createSession() {
		GameSessionEntity session = gameSessionRepository.save(
				new GameSessionEntity(UUID.randomUUID().toString())
		);
		return toResponse(session, null);
	}

	@Transactional(readOnly = true)
	@Override
	public SessionResponse getSession(String sessionId) {
		GameSessionEntity session = findSession(sessionId);
		GameResponse game = session.getMoves().isEmpty() ? null : getGame(session.getGameId());
		return toResponse(session, game);
	}


	@Transactional
	@Override
	public synchronized SessionResponse simulate(String sessionId) {
		GameSessionEntity session = findSession(sessionId);

		if (session.getStatus() == SessionStatus.COMPLETED) {
			throw new SessionConflictException("Session is already completed");
		}
		if (session.getStatus() == SessionStatus.SIMULATING) {
			throw new SessionConflictException("Session is already simulating");
		}

		session.setStatus(SessionStatus.SIMULATING);
		GameResponse game = null;
		PlayerSymbol currentPlayer = nextPlayer(session);

		while (game == null || !game.status().isTerminal()) {
			Cell move = chooseMove(game);
			game = submitMove(
					session.getGameId(),
					new EngineMoveRequest(
							currentPlayer,
							move.row(),
							move.col()
					)
			);
			session.addMove(new MoveEntity(session.getMoves().size() + 1, currentPlayer, move.row(), move.col(),
					game.status()));
			currentPlayer = currentPlayer.next();
		}

		session.setStatus(SessionStatus.COMPLETED);
		return toResponse(gameSessionRepository.save(session), game);
	}

	private GameResponse getGame(String gameId) {
		try {
			log.info("Getting game from engine: {}", gameId);
			return gameEngineClient.getGame(gameId);
		} catch (FeignException exception) {
			throw new EngineCommunicationException("Game Engine Service request failed", exception);
		}
	}

	private GameResponse submitMove(String gameId, EngineMoveRequest request) {
		try {
			log.info("Submitting move to engine: {}", request);
			return gameEngineClient.submitMove(gameId, request);
		} catch (FeignException exception) {
			throw new EngineCommunicationException("Game Engine Service request failed", exception);
		}
	}

	private GameSessionEntity findSession(String sessionId) {
		return gameSessionRepository.findById(sessionId)
				.orElseThrow(() -> new SessionNotFoundException(sessionId));
	}

	private PlayerSymbol nextPlayer(GameSessionEntity session) {
		if (session.getMoves().isEmpty()) {
			return PlayerSymbol.X;
		}
		return session.getMoves().getLast().getPlayer().next();
	}

	private Cell chooseMove(GameResponse game) {
		List<Cell> emptyCells = emptyCells(game);
		Collections.shuffle(emptyCells);
		return emptyCells.getFirst();
	}

	private List<Cell> emptyCells(GameResponse game) {
		List<Cell> emptyCells = new ArrayList<>();
		if (game == null) {
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					emptyCells.add(new Cell(row, col));
				}
			}
			return emptyCells;
		}

		for (int row = 0; row < game.board().size(); row++) {
			List<PlayerSymbol> rowCells = game.board().get(row);
			for (int col = 0; col < rowCells.size(); col++) {
				if (rowCells.get(col) == null) {
					emptyCells.add(new Cell(row, col));
				}
			}
		}
		return emptyCells;
	}

	private SessionResponse toResponse(GameSessionEntity session, GameResponse game) {
		return new SessionResponse(
				session.getId(),
				session.getGameId(),
				session.getStatus(),
				game,
				session.getMoves().stream()
						.map(move -> new SessionMoveResponse(
								move.getTurn(),
								move.getPlayer(),
								move.getRow(),
								move.getCol(),
								move.getResultingStatus()
						))
						.toList()
		);
	}

	private record Cell(int row, int col) {
	}
}
