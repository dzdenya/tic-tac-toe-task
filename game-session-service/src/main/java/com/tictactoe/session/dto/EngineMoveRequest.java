package com.tictactoe.session.dto;

import com.tictactoe.session.model.PlayerSymbol;

public record EngineMoveRequest(
		PlayerSymbol player,
		int row,
		int col
) {
}
