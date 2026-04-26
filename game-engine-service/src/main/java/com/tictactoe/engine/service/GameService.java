package com.tictactoe.engine.service;

import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;

public interface GameService {

	GameResponse getGame(String gameId);

	GameResponse move(String gameId, MoveRequest request);

}
