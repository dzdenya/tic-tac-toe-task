package com.tictactoe.session.repository;

import com.tictactoe.session.model.GameSessionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface GameSessionRepository extends ReactiveCrudRepository<GameSessionEntity, String> {
}
