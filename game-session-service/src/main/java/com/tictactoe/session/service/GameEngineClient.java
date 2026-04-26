package com.tictactoe.session.service;

import com.tictactoe.session.dto.EngineMoveRequest;
import com.tictactoe.session.dto.GameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "game-engine", url = "${game-engine.base-url}")
public interface GameEngineClient {

	@PostMapping("/games/{gameId}/move")
	GameResponse submitMove(@PathVariable String gameId, EngineMoveRequest request);

	@GetMapping("/games/{gameId}")
	GameResponse getGame(@PathVariable String gameId);

}
