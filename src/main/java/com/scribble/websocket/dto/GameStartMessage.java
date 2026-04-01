package com.scribble.websocket.dto;

import java.util.List;
import java.util.UUID;

public record GameStartMessage(
    String roomCode,
    int totalRounds,
    List<UUID> playerOrder
) {}
