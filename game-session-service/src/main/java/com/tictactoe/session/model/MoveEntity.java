package com.tictactoe.session.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table(name = "session_moves")
public class MoveEntity {

	@Id
	private Long id;

	@Column("session_id")
	private String sessionId;

	@Column("turn")
	private int turn;

	@Column("player")
	private PlayerSymbol player;

	@Column("row_index")
	private int row;

	@Column("col_index")
	private int col;

	@Column("resulting_status")
	private GameStatus resultingStatus;

	protected MoveEntity() {
	}

	public MoveEntity(String sessionId, int turn, PlayerSymbol player, int row, int col, GameStatus resultingStatus) {
		this.sessionId = sessionId;
		this.turn = turn;
		this.player = player;
		this.row = row;
		this.col = col;
		this.resultingStatus = resultingStatus;
	}

}
