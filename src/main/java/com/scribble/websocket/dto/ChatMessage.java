package com.scribble.websocket.dto;

public record ChatMessage(
    String text,
    String playerId,
    String username,
    long timestamp
) {}
