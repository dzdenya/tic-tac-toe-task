package com.tictactoe.engine.dto;

import com.tictactoe.engine.model.PlayerSymbol;

public record MoveResponse(
		PlayerSymbol player,
		int row,
		int col
) {
}
