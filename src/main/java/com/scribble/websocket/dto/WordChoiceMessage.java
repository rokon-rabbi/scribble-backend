package com.scribble.websocket.dto;

import java.util.List;
import java.util.UUID;

public record WordChoiceMessage(
    List<WordOption> words
) {
    public record WordOption(UUID id, String word) {}
}
