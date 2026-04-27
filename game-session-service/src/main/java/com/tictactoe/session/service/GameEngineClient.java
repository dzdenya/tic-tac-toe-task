package com.tictactoe.session.service;

import com.tictactoe.session.dto.EngineMoveRequest;
import com.tictactoe.session.dto.GameResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient adapter used by the session service to delegate board validation and
 * outcome calculation to the game engine service.
 */
@Component
public class GameEngineClient {

	private final WebClient webClient;

	public GameEngineClient(@Value("${game-engine.base-url}") String gameEngineBaseUrl) {
		this.webClient = WebClient.builder()
				.baseUrl(gameEngineBaseUrl)
				.build();
	}

	/**
	 * Creates an empty engine game using the session id as the engine game id.
	 *
	 * @param gameId engine game identifier
	 * @return initialized engine game state
	 */
	public Mono<GameResponse> createGame(String gameId) {
		return webClient.post()
				.uri("/games/{gameId}", gameId)
				.retrieve()
				.bodyToMono(GameResponse.class);
	}

	/**
	 * Submits a move to the engine for validation and persistence.
	 *
	 * @param gameId  engine game identifier
	 * @param request move payload
	 * @return updated engine game state
	 */
	public Mono<GameResponse> submitMove(String gameId, EngineMoveRequest request) {
		return webClient.post()
				.uri("/games/{gameId}/move", gameId)
				.bodyValue(request)
				.retrieve()
				.bodyToMono(GameResponse.class);
	}

	/**
	 * Fetches the latest engine game state.
	 *
	 * @param gameId engine game identifier
	 * @return current engine game state
	 */
	public Mono<GameResponse> getGame(String gameId) {
		return webClient.get()
				.uri("/games/{gameId}", gameId)
				.retrieve()
				.bodyToMono(GameResponse.class);
	}
}
