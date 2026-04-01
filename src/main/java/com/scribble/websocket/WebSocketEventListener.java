package com.scribble.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = event.getSessionId();

        String key = "ws:session:" + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            log.debug("Disconnect for unknown session: {}", sessionId);
            return;
        }

        String roomCode = session.get("roomCode").toString();
        String playerId = session.get("playerId").toString();

        // Mark player as disconnected
        redisTemplate.opsForHash().put(
                "room:" + roomCode + ":player:" + playerId,
                "connected", "false");

        // Notify room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/players",
                Map.of("event", "player_disconnected", "playerId", playerId));

        log.info("WebSocket disconnected: sessionId={}, roomCode={}, playerId={}", sessionId, roomCode, playerId);
    }
}
