package com.flamingo.tictactoe.engine.controller;

import jakarta.validation.Valid;

import com.flamingo.tictactoe.engine.dto.GameResponse;
import com.flamingo.tictactoe.engine.dto.MoveRequest;
import com.flamingo.tictactoe.engine.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
