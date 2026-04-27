package com.tictactoe.session.repository;

import com.tictactoe.session.model.MoveEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MoveRepository extends ReactiveCrudRepository<MoveEntity, Long> {

	Flux<MoveEntity> findBySessionIdOrderByTurnAsc(String sessionId);
}
