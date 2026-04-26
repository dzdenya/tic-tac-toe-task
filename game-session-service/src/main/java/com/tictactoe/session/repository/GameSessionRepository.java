package com.tictactoe.session.repository;

import com.tictactoe.session.model.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, String> {
}
