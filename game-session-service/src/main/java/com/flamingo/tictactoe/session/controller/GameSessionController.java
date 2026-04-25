package com.flamingo.tictactoe.session.controller;

import com.flamingo.tictactoe.session.dto.SessionResponse;
import com.flamingo.tictactoe.session.service.GameSessionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
class GameSessionController {

	private final GameSessionService gameSessionService;

	GameSessionController(GameSessionService gameSessionService) {
		this.gameSessionService = gameSessionService;
	}

	@PostMapping
	SessionResponse createSession() {
		return gameSessionService.createSession();
	}

	@GetMapping("/{sessionId}")
	SessionResponse getSession(@PathVariable String sessionId) {
		return gameSessionService.getSession(sessionId);
	}

	@PostMapping("/{sessionId}/simulate")
	SessionResponse simulate(@PathVariable String sessionId) {
		return gameSessionService.simulate(sessionId);
	}
}
