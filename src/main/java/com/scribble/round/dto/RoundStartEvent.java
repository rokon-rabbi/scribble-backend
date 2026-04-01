package com.scribble.round.dto;

import java.util.UUID;

public record RoundStartEvent(
    int roundNumber,
    int totalRounds,
    UUID drawerId,
    String drawerUsername,
    String wordHint,
    int turnTimeSeconds
) {}
