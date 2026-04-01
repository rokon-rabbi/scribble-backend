package com.scribble.websocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessage(
    @NotBlank @Size(max = 200) String text,
    String playerId,
    String username,
    long timestamp
) {}
