package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionResponse;

public interface GameSessionService {

	SessionResponse createSession();

	SessionResponse getSession(String sessionId);

	SessionResponse simulate(String sessionId);
	
}
