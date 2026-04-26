package com.tictactoe.session.dto;

import com.tictactoe.session.model.GameStatus;
import com.tictactoe.session.model.PlayerSymbol;

public record SessionMoveResponse(
		int turn,
		PlayerSymbol player,
		int row,
		int col,
		GameStatus resultingStatus
) {
}
