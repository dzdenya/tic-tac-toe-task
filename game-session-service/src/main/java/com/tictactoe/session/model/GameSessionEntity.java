package com.tictactoe.session.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "game_sessions")
public class GameSessionEntity {

	@Id
	private String id;

	@Column(nullable = false)
	private String gameId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SessionStatus status;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("turn ASC")
	@ToString.Exclude
	private List<MoveEntity> moves = new ArrayList<>();

	protected GameSessionEntity() {
	}

	public GameSessionEntity(String id) {
		this.id = id;
		this.gameId = id;
		this.status = SessionStatus.CREATED;
	}

	public void addMove(MoveEntity move) {
		moves.add(move);
		move.setSession(this);
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof GameSessionEntity that)) return false;

		return Objects.equals(id, that.id)
			&& Objects.equals(gameId, that.gameId)
			&& status == that.status
			&& Objects.equals(moves, that.moves);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(id);
		result = 31 * result + Objects.hashCode(gameId);
		result = 31 * result + Objects.hashCode(status);
		result = 31 * result + Objects.hashCode(moves);
		return result;
	}
}
