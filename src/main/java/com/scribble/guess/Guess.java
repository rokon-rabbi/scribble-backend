package com.scribble.guess;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guesses")
@Getter
@Setter
@NoArgsConstructor
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "guess_text", nullable = false, length = 100)
    private String guessText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

    @Column(name = "time_taken_ms", nullable = false)
    private int timeTakenMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
