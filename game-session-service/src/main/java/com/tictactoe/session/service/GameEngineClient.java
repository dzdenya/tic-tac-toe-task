package com.tictactoe.session.service;

import com.tictactoe.session.dto.EngineMoveRequest;
import com.tictactoe.session.dto.GameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client used by the session service to delegate board validation and
 * outcome calculation to the game engine service.
 */
@FeignClient(name = "game-engine", url = "${game-engine.base-url}")
public interface GameEngineClient {

	/**
	 * Submits a move to the engine for validation and persistence.
	 *
	 * @param gameId engine game identifier
	 * @param request move payload
	 * @return updated engine game state
	 */
	@PostMapping("/games/{gameId}/move")
	GameResponse submitMove(@PathVariable String gameId, EngineMoveRequest request);

	/**
	 * Fetches the latest engine game state.
	 *
	 * @param gameId engine game identifier
	 * @return current engine game state
	 */
	@GetMapping("/games/{gameId}")
	GameResponse getGame(@PathVariable String gameId);

}
