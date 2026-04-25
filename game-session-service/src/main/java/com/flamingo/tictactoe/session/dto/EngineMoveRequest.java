package com.flamingo.tictactoe.session.dto;

import com.flamingo.tictactoe.session.model.PlayerSymbol;

public record EngineMoveRequest(
		PlayerSymbol player,
		int row,
		int col
) {
}
