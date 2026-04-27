package com.tictactoe.engine.model;

import com.tictactoe.engine.domain.Board;
import com.tictactoe.engine.domain.GameResult;
import com.tictactoe.engine.exception.InvalidMoveException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

@Entity
@Getter
@ToString
@Table(name = "games")
public class GameEntity {

	private static final String EMPTY_BOARD = "---------";

	@Id
	private String id;

	@Version
	private Long version;

	@Column(nullable = false, length = 9)
	private String cells;

	@Transient
	private Board board;

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
		this.board = Board.empty();
		this.status = GameStatus.IN_PROGRESS;
	}

	@PostLoad
	private void loadBoard() {
		this.board = Board.fromString(this.cells);
	}

	@PrePersist
	@PreUpdate
	private void syncCells() {
		if (board != null) {
			this.cells = board.asString();
		}
	}

	public void makeMove(PlayerSymbol player, int row, int col) {
		ensureBoardInitialized();

		validateGameNotFinished();
		validateFirstMove(player);
		validateTurn(player);

		this.board = board.makeMove(player, row, col);

		this.lastPlayer = player;
		this.lastRow = row;
		this.lastCol = col;

		updateOutcome();
	}

	private void updateOutcome() {
		GameResult result = board.evaluate();
		this.status = result.status();
		this.winner = result.winner();
	}

	private void validateGameNotFinished() {
		if (status.isTerminal()) {
			throw new InvalidMoveException("Game is already completed");
		}
	}

	private void validateFirstMove(PlayerSymbol player) {
		if (lastPlayer == null && player != PlayerSymbol.X) {
			throw new InvalidMoveException("Player X must make the first move");
		}
	}

	private void validateTurn(PlayerSymbol player) {
		if (player == lastPlayer) {
			throw new InvalidMoveException("Player cannot move twice in a row");
		}
	}

	private void ensureBoardInitialized() {
		if (board == null) {
			this.board = Board.fromString(this.cells);
		}
	}
}