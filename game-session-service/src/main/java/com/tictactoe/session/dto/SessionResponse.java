package com.tictactoe.session.dto;

import com.tictactoe.session.model.SessionStatus;
import java.util.List;

public record SessionResponse(
		String sessionId,
		String gameId,
		SessionStatus status,
		GameResponse game,
		List<SessionMoveResponse> moves
) {
}
