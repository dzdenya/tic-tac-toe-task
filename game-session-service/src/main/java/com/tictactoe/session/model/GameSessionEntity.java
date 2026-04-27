package com.tictactoe.session.model;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@ToString
@Table(name = "game_sessions")
public class GameSessionEntity {

	@Id
	private String id;

	@Version
	private Long version;

	@Column("game_id")
	private String gameId;

	@Column("status")
	private SessionStatus status;

	protected GameSessionEntity() {
	}

	public GameSessionEntity(String id, String gameId) {
		this.id = id;
		this.gameId = gameId;
		this.status = SessionStatus.CREATED;
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof GameSessionEntity that)) return false;

		return Objects.equals(id, that.id)
			&& Objects.equals(gameId, that.gameId)
			&& status == that.status;
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(id);
		result = 31 * result + Objects.hashCode(gameId);
		result = 31 * result + Objects.hashCode(status);
		return result;
	}
}
