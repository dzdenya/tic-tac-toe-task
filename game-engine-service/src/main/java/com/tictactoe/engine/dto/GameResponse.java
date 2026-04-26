package com.tictactoe.engine.dto;

import com.tictactoe.engine.model.GameStatus;
import com.tictactoe.engine.model.PlayerSymbol;
import java.util.List;

public record GameResponse(
		String gameId,
		List<List<PlayerSymbol>> board,
		GameStatus status,
		PlayerSymbol winner,
		MoveResponse lastMove
) {
}
