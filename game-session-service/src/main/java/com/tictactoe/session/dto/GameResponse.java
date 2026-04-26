package com.tictactoe.session.dto;

import com.tictactoe.session.model.GameStatus;
import com.tictactoe.session.model.PlayerSymbol;
import java.util.List;

public record GameResponse(
		String gameId,
		List<List<PlayerSymbol>> board,
		GameStatus status,
		PlayerSymbol winner,
		MoveResponse lastMove
) {
}
