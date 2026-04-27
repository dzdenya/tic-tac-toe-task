package com.tictactoe.engine.domain;

import com.tictactoe.engine.model.GameStatus;
import com.tictactoe.engine.model.PlayerSymbol;

public record GameResult(GameStatus status, PlayerSymbol winner) {

	public static GameResult win(PlayerSymbol player) {
		return new GameResult(
				player == PlayerSymbol.X ? GameStatus.X_WON : GameStatus.O_WON,
				player
		);
	}

	public static GameResult draw() {
		return new GameResult(GameStatus.DRAW, null);
	}

	public static GameResult inProgress() {
		return new GameResult(GameStatus.IN_PROGRESS, null);
	}
}
