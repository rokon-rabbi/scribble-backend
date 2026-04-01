package com.scribble.game.dto;

import com.scribble.game.Game;

import java.util.UUID;

public record RoomInfo(
    UUID id,
    String roomCode,
    String status,
    int maxPlayers,
    int currentPlayerCount,
    String ownerUsername
) {
    public static RoomInfo from(Game game, int playerCount, String ownerName) {
        return new RoomInfo(
            game.getId(),
            game.getRoomCode(),
            game.getStatus().name(),
            game.getMaxPlayers(),
            playerCount,
            ownerName
        );
    }
}
