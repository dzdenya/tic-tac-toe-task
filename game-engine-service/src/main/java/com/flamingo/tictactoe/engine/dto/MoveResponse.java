package com.flamingo.tictactoe.engine.dto;

import com.flamingo.tictactoe.engine.model.PlayerSymbol;

public record MoveResponse(
        PlayerSymbol player,
        int row,
        int col
) {
}
