package com.scribble.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribble.websocket.dto.StrokeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DrawingWebSocketHandler {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @MessageMapping("/room/{roomCode}/draw")
    public void handleStroke(
            @DestinationVariable String roomCode,
            @Payload StrokeMessage stroke,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();

        // Verify sender is the current drawer
        String drawerId = (String) redisTemplate.opsForHash().get("room:" + roomCode, "drawerId");
        if (drawerId != null && !drawerId.equals(playerId)) {
            log.warn("Non-drawer {} tried to draw in room {}", playerId, roomCode);
            return;
        }

        // Buffer stroke in Redis
        try {
            String strokeJson = objectMapper.writeValueAsString(stroke);
            String strokeKey = "room:" + roomCode + ":strokes";
            redisTemplate.opsForList().rightPush(strokeKey, strokeJson);
            redisTemplate.expire(strokeKey, Duration.ofHours(2));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stroke", e);
        }

        // Broadcast to all other players in the room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/draw",
                stroke);

        log.debug("Stroke broadcast in room {} by {}", roomCode, playerId);
    }

    @MessageMapping("/room/{roomCode}/clear-canvas")
    public void handleClearCanvas(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();

        // Verify sender is the current drawer
        String drawerId = (String) redisTemplate.opsForHash().get("room:" + roomCode, "drawerId");
        if (drawerId != null && !drawerId.equals(playerId)) {
            return;
        }

        // Clear stroke buffer
        redisTemplate.delete("room:" + roomCode + ":strokes");

        // Broadcast clear to all players
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/draw",
                Map.of("type", "clear"));

        log.debug("Canvas cleared in room {} by {}", roomCode, playerId);
    }
}
