package com.scribble.guess;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuessRepository extends JpaRepository<Guess, UUID> {

    List<Guess> findByRoundId(UUID roundId);

    List<Guess> findByRoundIdAndPlayerId(UUID roundId, UUID playerId);

    boolean existsByRoundIdAndPlayerIdAndCorrectTrue(UUID roundId, UUID playerId);
}
