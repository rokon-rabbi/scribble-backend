package com.scribble.participant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, UUID> {

    List<GameParticipant> findByGameId(UUID gameId);

    boolean existsByGameIdAndPlayerId(UUID gameId, UUID playerId);

    int countByGameId(UUID gameId);
}
