package com.scribble.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
    @NotNull @Min(2) @Max(12) Integer maxPlayers,
    @NotNull @Min(1) @Max(5) Integer roundsPerPlayer,
    @NotNull @Min(30) @Max(180) Integer turnTimeSeconds
) {}
