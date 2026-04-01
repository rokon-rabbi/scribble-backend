package com.scribble.score;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "round_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"round_id", "player_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RoundScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_id", nullable = false)
    private UUID roundId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScoreRole role;
}
