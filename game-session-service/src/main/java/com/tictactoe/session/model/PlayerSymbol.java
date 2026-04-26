package com.tictactoe.session.model;

public enum PlayerSymbol {
	X,
	O;

	public PlayerSymbol next() {
		return this == X ? O : X;
	}
}
