package com.tictactoe.session.service;

import com.tictactoe.session.dto.*;
import com.tictactoe.session.exception.EngineCommunicationException;
import com.tictactoe.session.exception.SessionConflictException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.model.*;
import com.tictactoe.session.repository.GameSessionRepository;
import com.tictactoe.session.repository.MoveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Transactional implementation of automated tic-tac-toe sessions.
 *
 * <p>This service owns session lifecycle and move history persistence, while the
 * game engine service remains the source of truth for board validation and final
 * game outcomes.</p>
 */
@Slf4j
@Service
public class GameSessionServiceImpl implements GameSessionService {

	private static final int BOARD_SIZE = 3;
	private static final String OCCUPIED_CELL_MESSAGE = "Cell is already occupied";
	private static final Duration MOVE_DELAY = Duration.ofMillis(500);

	private final GameSessionRepository gameSessionRepository;
	private final MoveRepository moveRepository;
	private final GameEngineClient gameEngineClient;
	private final TransactionalOperator transactionalOperator;
	private final ObjectMapper objectMapper;

	public GameSessionServiceImpl(GameSessionRepository gameSessionRepository,
	                              MoveRepository moveRepository,
	                              GameEngineClient gameEngineClient,
	                              TransactionalOperator transactionalOperator,
	                              ObjectMapper objectMapper) {
		this.gameSessionRepository = gameSessionRepository;
		this.moveRepository = moveRepository;
		this.gameEngineClient = gameEngineClient;
		this.transactionalOperator = transactionalOperator;
		this.objectMapper = objectMapper;
	}

	@Override
	public Mono<SessionResponse> createSession() {
		String sessionId = UUID.randomUUID().toString();
		GameSessionEntity session = new GameSessionEntity(sessionId, sessionId);
		return createEngineGame(sessionId)
				.flatMap(game -> gameSessionRepository.save(session)
						.map(saved -> toResponse(saved, List.of(), game)));
	}

	@Override
	public Mono<SessionResponse> getSession(String sessionId) {
		return findSessionData(sessionId)
				.flatMap(sessionData -> getEngineGame(sessionData.session().getGameId())
						.map(game -> toResponse(sessionData, game)));
	}

	@Override
	public Mono<SessionResponse> simulate(String sessionId) {
		return runSimulation(sessionId)
				.filter(event -> "completed".equals(event.type()))
				.next()
				.map(SessionEventResponse::session);
	}

	@Override
	public Flux<SessionEventResponse> streamSimulation(String sessionId) {
		return runSimulation(sessionId);
	}

	/**
	 * Claims the session for simulation, then emits accepted move events and a final
	 * completion event. Any unexpected simulation failure marks the session failed
	 * before the error is propagated to the caller.
	 */
	private Flux<SessionEventResponse> runSimulation(String sessionId) {
		return beginSimulation(sessionId)
				.flatMapMany(claim -> simulationStates(claim)
						.skip(1)
						.concatMap(state -> toEvents(sessionId, state))
						.onErrorResume(RuntimeException.class,
								exception -> markSimulationFailed(sessionId).then(Mono.error(exception))));
	}

	/**
	 * Builds the recursive stream of simulation states. The first state is a seed
	 * without an engine board, so callers skip it before producing outbound events.
	 */
	private Flux<SimulationState> simulationStates(SimulationClaim claim) {
		return Mono.just(SimulationState.initial(claim.nextPlayer(), claim.nextTurn()))
				.expand(state -> state.isTerminal()
						? Mono.empty()
						: nextMoveWithDelay(claim.gameId(), state)
				);
	}

	private Mono<SimulationState> nextMoveWithDelay(String gameId, SimulationState state) {
		Mono<SimulationState> nextMove = submitNextMove(gameId, state);
		return state.game() == null ? nextMove : nextMove.delaySubscription(MOVE_DELAY);
	}

	private Mono<SimulationState> submitNextMove(String gameId, SimulationState state) {
		Cell move = chooseMove(state.game());

		return submitMoveSafe(
				gameId,
				new EngineMoveRequest(state.currentPlayer(), move.row(), move.col())
		)
				.map(game -> state.acceptedMove(game, move))
				.switchIfEmpty(getEngineGame(gameId).map(state::refreshedGame));
	}

	private Flux<SessionEventResponse> toEvents(String sessionId, SimulationState state) {
		Flux<SessionEventResponse> moveEvent = state.lastRecordedMove() == null
				? Flux.empty()
				: Flux.just(SessionEventResponse.move(toMoveResponse(state.lastRecordedMove()), state.game()));

		if (!state.isTerminal()) {
			return moveEvent;
		}

		return moveEvent.concatWith(
				completeSimulation(sessionId, state.recordedMoves(), state.game())
						.map(SessionEventResponse::completed)
		);
	}

	/**
	 * Claims a session for one simulation runner and calculates the next turn from
	 * already persisted move history, allowing failed sessions to be retried.
	 */
	private Mono<SimulationClaim> beginSimulation(String sessionId) {
		return findSessionData(sessionId)
				.flatMap(sessionData -> {
					GameSessionEntity session = sessionData.session();

					if (session.getStatus() == SessionStatus.COMPLETED) {
						return Mono.error(new SessionConflictException("Session is already completed"));
					}
					if (session.getStatus() == SessionStatus.SIMULATING) {
						return Mono.error(new SessionConflictException("Session is already simulating"));
					}

					PlayerSymbol nextPlayer = nextPlayer(sessionData.moves());
					int nextTurn = sessionData.moves().size() + 1;

					session.setStatus(SessionStatus.SIMULATING);
					return gameSessionRepository.save(session)
							.map(saved -> new SimulationClaim(saved.getGameId(), nextPlayer, nextTurn));
				})
				.as(transactionalOperator::transactional);
	}

	/**
	 * Persists all moves accepted during this run and marks the session completed in
	 * a single transaction.
	 */
	private Mono<SessionResponse> completeSimulation(String sessionId, List<RecordedMove> recordedMoves, GameResponse game) {
		return findSession(sessionId)
				.flatMap(session -> saveCompletedSession(session, recordedMoves)
						.then(completedSessionResponse(sessionId, game)))
				.as(transactionalOperator::transactional);
	}

	private Mono<Void> saveCompletedSession(GameSessionEntity session, List<RecordedMove> recordedMoves) {
		session.setStatus(SessionStatus.COMPLETED);
		return saveRecordedMoves(session.getId(), recordedMoves)
				.then(gameSessionRepository.save(session))
				.then();
	}

	private Mono<Void> saveRecordedMoves(String sessionId, List<RecordedMove> recordedMoves) {
		return moveRepository.saveAll(toMoveEntities(sessionId, recordedMoves))
				.then();
	}

	private Flux<MoveEntity> toMoveEntities(String sessionId, List<RecordedMove> recordedMoves) {
		return Flux.fromIterable(recordedMoves)
				.map(recordedMove -> toMoveEntity(sessionId, recordedMove));
	}

	private Mono<SessionResponse> completedSessionResponse(String sessionId, GameResponse game) {
		return findSessionData(sessionId)
				.map(sessionData -> toResponse(sessionData, game));
	}

	private Mono<Void> markSimulationFailed(String sessionId) {
		return findSession(sessionId)
				.flatMap(session -> {
					if (session.getStatus() == SessionStatus.SIMULATING) {
						session.setStatus(SessionStatus.FAILED);
						return gameSessionRepository.save(session).then();
					}
					return Mono.empty();
				})
				.as(transactionalOperator::transactional);
	}

	private Mono<GameResponse> createEngineGame(String gameId) {
		return engineRequest("createGame", gameEngineClient.createGame(gameId));
	}

	private Mono<GameResponse> getEngineGame(String gameId) {
		return engineRequest("getGame", gameEngineClient.getGame(gameId));
	}

	private Mono<GameResponse> engineRequest(String operation, Mono<GameResponse> request) {
		return request.onErrorMap(WebClientException.class, exception -> engineUnavailable(operation, exception));
	}

	/**
	 * Sends a session move request to the engine service.
	 */
	private Mono<GameResponse> submitMoveSafe(String gameId, EngineMoveRequest request) {
		log.debug("Submitting move to engine: {}", request);
		return gameEngineClient.submitMove(gameId, request)
				.onErrorResume(WebClientResponseException.class,
						exception -> handleMoveResponseException(request, exception))
				.onErrorMap(WebClientException.class, exception -> engineUnavailable("submitMove", exception));
	}

	private Mono<GameResponse> handleMoveResponseException(EngineMoveRequest request,
	                                                       WebClientResponseException exception) {
		if (exception.getStatusCode().value() != 400) {
			return Mono.error(engineUnavailable("submitMove", exception));
		}

		String message = errorMessage(exception);
		if (OCCUPIED_CELL_MESSAGE.equals(message)) {
			log.debug("Retryable invalid move: {}", request);
			return Mono.empty();
		}
		return Mono.error(new EngineCommunicationException("Engine rejected move: " + message, exception));
	}

	private EngineCommunicationException engineUnavailable(String operation, Throwable exception) {
		if (exception instanceof WebClientResponseException responseException) {
			log.error("Engine {} failed: status={}, body={}", operation, responseException.getStatusCode(),
					responseException.getResponseBodyAsString());
		}
		return new EngineCommunicationException("Engine unavailable", exception);
	}

	private String errorMessage(WebClientResponseException exception) {
		String body = exception.getResponseBodyAsString();
		if (body.isBlank()) {
			return "Invalid move";
		}
		try {
			JsonNode message = objectMapper.readTree(body).path("message");
			return message.stringValueOpt().orElse(body);
		} catch (JacksonException ignored) {
			return body;
		}
	}

	private Mono<GameSessionEntity> findSession(String sessionId) {
		return gameSessionRepository.findById(sessionId)
				.switchIfEmpty(Mono.error(new SessionNotFoundException(sessionId)));
	}

	private Mono<SessionData> findSessionData(String sessionId) {
		return findSession(sessionId)
				.zipWith(moveRepository.findBySessionIdOrderByTurnAsc(sessionId).collectList())
				.map(tuple -> new SessionData(tuple.getT1(), tuple.getT2()));
	}

	private PlayerSymbol nextPlayer(List<MoveEntity> moves) {
		if (moves.isEmpty()) {
			return PlayerSymbol.X;
		}
		return moves.getLast().getPlayer().next();
	}

	/**
	 * Selects a random legal move from the latest known engine board.
	 */
	private Cell chooseMove(GameResponse game) {
		List<Cell> emptyCells = emptyCells(game);
		Collections.shuffle(emptyCells);
		return emptyCells.getFirst();
	}

	private List<Cell> emptyCells(GameResponse game) {
		List<Cell> emptyCells = new ArrayList<>();
		if (game == null) {
			for (int row = 0; row < BOARD_SIZE; row++) {
				for (int col = 0; col < BOARD_SIZE; col++) {
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

	private SessionResponse toResponse(SessionData sessionData, GameResponse game) {
		return toResponse(sessionData.session(), sessionData.moves(), game);
	}

	private SessionResponse toResponse(GameSessionEntity session, List<MoveEntity> moves, GameResponse game) {
		return new SessionResponse(
				session.getId(),
				session.getGameId(),
				session.getStatus(),
				game,
				moves.stream()
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

	private SessionMoveResponse toMoveResponse(RecordedMove recordedMove) {
		return new SessionMoveResponse(
				recordedMove.turn(),
				recordedMove.player(),
				recordedMove.move().row(),
				recordedMove.move().col(),
				recordedMove.resultingStatus()
		);
	}

	private MoveEntity toMoveEntity(String sessionId, RecordedMove recordedMove) {
		return new MoveEntity(
				sessionId,
				recordedMove.turn(),
				recordedMove.player(),
				recordedMove.move().row(),
				recordedMove.move().col(),
				recordedMove.resultingStatus()
		);
	}

	private record Cell(int row, int col) {
	}

	private record SimulationClaim(String gameId, PlayerSymbol nextPlayer, int nextTurn) {
	}

	private record RecordedMove(int turn, PlayerSymbol player, Cell move, GameStatus resultingStatus) {
	}

	private record SessionData(GameSessionEntity session, List<MoveEntity> moves) {
	}

	private record SimulationState(
			GameResponse game,
			PlayerSymbol currentPlayer,
			int turn,
			List<RecordedMove> recordedMoves,
			RecordedMove lastRecordedMove
	) {

		static SimulationState initial(PlayerSymbol currentPlayer, int turn) {
			return new SimulationState(null, currentPlayer, turn, List.of(), null);
		}

		boolean isTerminal() {
			return game != null && game.status().isTerminal();
		}

		SimulationState acceptedMove(GameResponse game, Cell move) {
			RecordedMove recordedMove = new RecordedMove(turn, currentPlayer, move, game.status());
			List<RecordedMove> updatedMoves = new ArrayList<>(recordedMoves);
			updatedMoves.add(recordedMove);
			return new SimulationState(game, currentPlayer.next(), turn + 1, List.copyOf(updatedMoves), recordedMove);
		}

		SimulationState refreshedGame(GameResponse game) {
			return new SimulationState(game, currentPlayer, turn, recordedMoves, null);
		}
	}
}
