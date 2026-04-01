package com.scribble.websocket.dto;

public record GuessResult(
    String event,
    String playerId,
    String username,
    String message
) {}
