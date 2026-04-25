package com.flamingo.tictactoe.session.dto;

import com.flamingo.tictactoe.session.model.GameStatus;
import com.flamingo.tictactoe.session.model.PlayerSymbol;

public record SessionMoveResponse(
		int turn,
		PlayerSymbol player,
		int row,
		int col,
		GameStatus resultingStatus
) {
}
