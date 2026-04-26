package com.tictactoe.session.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_moves")
public class MoveEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private GameSessionEntity session;

	@Column(nullable = false)
	private int turn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PlayerSymbol player;

	@Column(name = "row_index", nullable = false)
	private int row;

	@Column(name = "col_index", nullable = false)
	private int col;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GameStatus resultingStatus;

	protected MoveEntity() {
	}

	public MoveEntity(int turn, PlayerSymbol player, int row, int col, GameStatus resultingStatus) {
		this.turn = turn;
		this.player = player;
		this.row = row;
		this.col = col;
		this.resultingStatus = resultingStatus;
	}

	public int getTurn() {
		return turn;
	}

	public PlayerSymbol getPlayer() {
		return player;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public GameStatus getResultingStatus() {
		return resultingStatus;
	}

	void setSession(GameSessionEntity session) {
		this.session = session;
	}
}
