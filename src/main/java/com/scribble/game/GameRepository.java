package com.scribble.game;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByRoomCode(String roomCode);

    Page<Game> findByStatus(GameStatus status, Pageable pageable);
}
