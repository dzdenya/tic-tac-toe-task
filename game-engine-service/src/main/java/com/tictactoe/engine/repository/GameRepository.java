package com.tictactoe.engine.repository;

import com.tictactoe.engine.model.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<GameEntity, String> {
}
