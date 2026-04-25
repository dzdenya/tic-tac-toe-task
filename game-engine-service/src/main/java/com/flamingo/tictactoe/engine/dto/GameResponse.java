package com.flamingo.tictactoe.engine.dto;

import com.flamingo.tictactoe.engine.model.GameStatus;
import com.flamingo.tictactoe.engine.model.PlayerSymbol;
import java.util.List;

public record GameResponse(
		String gameId,
		List<List<PlayerSymbol>> board,
		GameStatus status,
		PlayerSymbol winner,
		MoveResponse lastMove
) {
}
