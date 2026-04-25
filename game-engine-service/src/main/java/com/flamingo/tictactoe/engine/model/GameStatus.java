package com.flamingo.tictactoe.engine.model;

public enum GameStatus {
	IN_PROGRESS,
	X_WON,
	O_WON,
	DRAW;

	public boolean isTerminal() {
		return this != IN_PROGRESS;
	}
}
