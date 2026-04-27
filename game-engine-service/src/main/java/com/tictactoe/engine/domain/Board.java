package com.tictactoe.engine.domain;

import com.tictactoe.engine.exception.InvalidMoveException;
import com.tictactoe.engine.model.PlayerSymbol;
import java.util.Optional;

public final class Board {

	private static final int SIZE = 3;
	private static final int TOTAL_CELLS = SIZE * SIZE;

	private static final char EMPTY = '-';

	private static final int[][] WINNING_LINES = {
			{0, 1, 2},
			{3, 4, 5},
			{6, 7, 8},
			{0, 3, 6},
			{1, 4, 7},
			{2, 5, 8},
			{0, 4, 8},
			{2, 4, 6}
	};

	private final char[] cells;

	private Board(char[] cells) {
		this.cells = cells;
	}

	public static Board empty() {
		return new Board("---------".toCharArray());
	}

	public static Board fromString(String value) {
		if (value == null || value.length() != TOTAL_CELLS) {
			throw new IllegalArgumentException("Invalid board string");
		}

		char[] chars = value.toCharArray();

		for (char c : chars) {
			if (c != 'X' && c != 'O' && c != EMPTY) {
				throw new IllegalArgumentException("Invalid character in board: " + c);
			}
		}

		return new Board(chars);
	}

	public Board makeMove(PlayerSymbol player, int row, int col) {
		validateBounds(row, col);

		int index = toIndex(row, col);

		if (cells[index] != EMPTY) {
			throw new InvalidMoveException("Cell is already occupied");
		}

		char[] copy = cells.clone();
		copy[index] = toChar(player);

		return new Board(copy);
	}

	public boolean isCellEmpty(int row, int col) {
		validateBounds(row, col);
		return cells[toIndex(row, col)] == EMPTY;
	}

	public Optional<PlayerSymbol> getWinner() {
		for (int[] line : WINNING_LINES) {
			char first = cells[line[0]];

			if (first != EMPTY &&
					first == cells[line[1]] &&
					first == cells[line[2]]) {

				return Optional.of(fromChar(first));
			}
		}

		return Optional.empty();
	}

	public boolean isFull() {
		for (char c : cells) {
			if (c == EMPTY) return false;
		}
		return true;
	}

	public GameResult evaluate() {
		Optional<PlayerSymbol> winner = getWinner();

		if (winner.isPresent()) {
			return GameResult.win(winner.get());
		}

		if (isFull()) {
			return GameResult.draw();
		}

		return GameResult.inProgress();
	}

	public PlayerSymbol get(int row, int col) {
		validateBounds(row, col);
		char c = cells[toIndex(row, col)];
		return c == EMPTY ? null : fromChar(c);
	}

	public String asString() {
		return new String(cells);
	}

	private int toIndex(int row, int col) {
		return row * SIZE + col;
	}

	private void validateBounds(int row, int col) {
		if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
			throw new IndexOutOfBoundsException(
					"Invalid position: (" + row + ", " + col + ")"
			);
		}
	}

	private char toChar(PlayerSymbol player) {
		return switch (player) {
			case X -> 'X';
			case O -> 'O';
		};
	}

	private PlayerSymbol fromChar(char c) {
		return switch (c) {
			case 'X' -> PlayerSymbol.X;
			case 'O' -> PlayerSymbol.O;
			default -> throw new IllegalStateException("Unexpected value: " + c);
		};
	}
}
