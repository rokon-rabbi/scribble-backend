package com.scribble.websocket.dto;

import java.util.List;

public record StrokeMessage(
    String playerId,
    List<List<Double>> points,
    String color,
    int brushSize,
    String tool
) {}
