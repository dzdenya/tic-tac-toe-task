package com.flamingo.tictactoe.engine.dto;

import java.util.List;

import com.flamingo.tictactoe.engine.model.GameStatus;
import com.flamingo.tictactoe.engine.model.PlayerSymbol;

public record GameResponse(
        String gameId,
        List<List<PlayerSymbol>> board,
        GameStatus status,
        PlayerSymbol winner,
        MoveResponse lastMove
) {
}
