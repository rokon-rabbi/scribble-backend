package com.scribble.player.dto;

import com.scribble.player.Player;

import java.util.UUID;

public record LeaderboardEntry(
    UUID id,
    String username,
    String avatarUrl,
    int totalGamesPlayed,
    int totalWins,
    int totalScore
) {
    public static LeaderboardEntry from(Player player) {
        return new LeaderboardEntry(
            player.getId(),
            player.getUsername(),
            player.getAvatarUrl(),
            player.getTotalGamesPlayed(),
            player.getTotalWins(),
            player.getTotalScore()
        );
    }
}
