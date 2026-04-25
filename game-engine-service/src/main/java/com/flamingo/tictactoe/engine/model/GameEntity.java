package com.flamingo.tictactoe.engine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "games")
public class GameEntity {

	static final String EMPTY_BOARD = "---------";

	@Id
	private String id;

	@Column(nullable = false, length = 9)
	private String cells;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GameStatus status;

	@Enumerated(EnumType.STRING)
	private PlayerSymbol winner;

	@Enumerated(EnumType.STRING)
	private PlayerSymbol lastPlayer;

	private Integer lastRow;

	private Integer lastCol;

	protected GameEntity() {
	}

	public GameEntity(String id) {
		this.id = id;
		this.cells = EMPTY_BOARD;
		this.status = GameStatus.IN_PROGRESS;
	}

	public void applyMove(PlayerSymbol player, int row, int col) {
		int index = toIndex(row, col);
		StringBuilder updatedCells = new StringBuilder(cells);
		updatedCells.setCharAt(index, player.name().charAt(0));
		this.cells = updatedCells.toString();
		this.lastPlayer = player;
		this.lastRow = row;
		this.lastCol = col;
	}

	public void complete(GameStatus status, PlayerSymbol winner) {
		this.status = status;
		this.winner = winner;
	}

	public boolean isCellEmpty(int row, int col) {
		return cells.charAt(toIndex(row, col)) == '-';
	}

	private static int toIndex(int row, int col) {
		return row * 3 + col;
	}
}
