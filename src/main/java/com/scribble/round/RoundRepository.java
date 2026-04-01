package com.scribble.round;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository extends JpaRepository<Round, UUID> {

    List<Round> findByGameIdOrderByRoundNumber(UUID gameId);

    Optional<Round> findFirstByGameIdAndStatusOrderByRoundNumber(UUID gameId, RoundStatus status);

    Optional<Round> findByGameIdAndRoundNumber(UUID gameId, int roundNumber);

    Optional<Round> findByGameIdAndStatus(UUID gameId, RoundStatus status);
}
