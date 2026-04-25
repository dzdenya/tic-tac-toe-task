package com.flamingo.tictactoe.session.repository;

import com.flamingo.tictactoe.session.model.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, String> {
}
