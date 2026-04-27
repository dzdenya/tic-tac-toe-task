package com.tictactoe.session.dto;

/**
 * Event payload streamed to the UI while a session simulation is running.
 */
public record SessionEventResponse(
		String type,
		SessionMoveResponse move,
		GameResponse game,
		SessionResponse session,
		String error
) {

	public static SessionEventResponse move(SessionMoveResponse move, GameResponse game) {
		return new SessionEventResponse("move", move, game, null, null);
	}

	public static SessionEventResponse completed(SessionResponse session) {
		return new SessionEventResponse("completed", null, session.game(), session, null);
	}

	public static SessionEventResponse error(String message) {
		return new SessionEventResponse("error", null, null, null, message);
	}
}
