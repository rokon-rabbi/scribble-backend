package com.scribble.score;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoundScoreRepository extends JpaRepository<RoundScore, UUID> {

    List<RoundScore> findByRoundId(UUID roundId);
}
