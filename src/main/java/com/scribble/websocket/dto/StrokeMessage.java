package com.scribble.websocket.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record StrokeMessage(
    String playerId,
    @NotNull @Size(max = 1000) List<List<Double>> points,
    @NotBlank @Size(max = 7) String color,
    @Min(1) @Max(50) int brushSize,
    @NotBlank String tool
) {}
