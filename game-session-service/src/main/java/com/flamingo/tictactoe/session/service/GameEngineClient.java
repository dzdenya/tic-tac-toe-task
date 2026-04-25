package com.flamingo.tictactoe.session.service;

import com.flamingo.tictactoe.session.dto.EngineMoveRequest;
import com.flamingo.tictactoe.session.dto.GameResponse;
import com.flamingo.tictactoe.session.exception.EngineCommunicationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GameEngineClient {

	private final RestClient gameEngineRestClient;

	GameEngineClient(RestClient gameEngineRestClient) {
		this.gameEngineRestClient = gameEngineRestClient;
	}

	GameResponse submitMove(String gameId, EngineMoveRequest request) {
		try {
			return gameEngineRestClient.post()
					.uri("/games/{gameId}/move", gameId)
					.body(request)
					.retrieve()
					.body(GameResponse.class);
		} catch (RestClientException exception) {
			throw new EngineCommunicationException("Game Engine Service request failed", exception);
		}
	}

	GameResponse getGame(String gameId) {
		try {
			return gameEngineRestClient.get()
					.uri("/games/{gameId}", gameId)
					.retrieve()
					.body(GameResponse.class);
		} catch (RestClientException exception) {
			throw new EngineCommunicationException("Game Engine Service request failed", exception);
		}
	}
}
