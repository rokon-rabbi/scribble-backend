package com.scribble.player.dto;

import com.scribble.player.Player;

import java.time.Instant;
import java.util.UUID;

public record PlayerProfile(
    UUID id,
    String username,
    String email,
    String avatarUrl,
    int totalGamesPlayed,
    int totalWins,
    int totalScore,
    Instant createdAt
) {
    public static PlayerProfile from(Player player) {
        return new PlayerProfile(
            player.getId(),
            player.getUsername(),
            player.getEmail(),
            player.getAvatarUrl(),
            player.getTotalGamesPlayed(),
            player.getTotalWins(),
            player.getTotalScore(),
            player.getCreatedAt()
        );
    }
}
