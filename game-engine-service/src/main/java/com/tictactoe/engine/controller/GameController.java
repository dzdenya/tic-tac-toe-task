package com.tictactoe.engine.controller;

import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.service.GameService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/games")
class GameController {

	private final GameService gameService;

	GameController(GameService gameService) {
		this.gameService = gameService;
	}

	@GetMapping("/{gameId}")
	GameResponse getGame(@PathVariable String gameId) {
		return gameService.getGame(gameId);
	}

	@PostMapping("/{gameId}/move")
	GameResponse move(@PathVariable String gameId, @Valid @RequestBody MoveRequest request) {
		return gameService.move(gameId, request);
	}
}
