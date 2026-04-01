package com.scribble.round.dto;

import java.util.List;
import java.util.Map;

public record RoundEndEvent(
    int roundNumber,
    String correctWord,
    List<Map<String, Object>> scores
) {}
