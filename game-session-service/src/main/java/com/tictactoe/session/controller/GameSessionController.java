package com.tictactoe.session.controller;

import com.tictactoe.session.dto.SessionEventResponse;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.service.GameSessionService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/sessions")
class GameSessionController {

	private final GameSessionService gameSessionService;

	GameSessionController(GameSessionService gameSessionService) {
		this.gameSessionService = gameSessionService;
	}

	@PostMapping
	Mono<SessionResponse> createSession() {
		return gameSessionService.createSession();
	}

	@GetMapping("/{sessionId}")
	Mono<SessionResponse> getSession(@PathVariable String sessionId) {
		return gameSessionService.getSession(sessionId);
	}

	@PostMapping("/{sessionId}/simulate")
	Mono<SessionResponse> simulate(@PathVariable String sessionId) {
		return gameSessionService.simulate(sessionId);
	}

	@GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	Flux<ServerSentEvent<SessionEventResponse>> streamSimulation(@PathVariable String sessionId) {
		return gameSessionService.streamSimulation(sessionId)
				.map(event -> ServerSentEvent.<SessionEventResponse>builder()
						.event(event.type())
						.data(event)
						.build());
	}
}
