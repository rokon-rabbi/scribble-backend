package com.scribble.game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_code", nullable = false, unique = true, length = 6)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameStatus status = GameStatus.WAITING;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 8;

    @Column(name = "rounds_per_player", nullable = false)
    private int roundsPerPlayer = 2;

    @Column(name = "turn_time_seconds", nullable = false)
    private int turnTimeSeconds = 80;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
