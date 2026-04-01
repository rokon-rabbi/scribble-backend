package com.scribble.participant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_participants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "final_score", nullable = false)
    private int finalScore = 0;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();
}
