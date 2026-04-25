package com.flamingo.tictactoe.session.dto;

import com.flamingo.tictactoe.session.model.PlayerSymbol;

public record MoveResponse(
		PlayerSymbol player,
		int row,
		int col
) {
}
