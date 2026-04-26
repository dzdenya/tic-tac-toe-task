package com.tictactoe.session.dto;

import com.tictactoe.session.model.PlayerSymbol;

public record MoveResponse(
		PlayerSymbol player,
		int row,
		int col
) {
}
