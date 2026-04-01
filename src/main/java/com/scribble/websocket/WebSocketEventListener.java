package com.scribble.websocket;

import com.scribble.participant.GameParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        String key = "ws:session:" + sessionId;
        Map<Object, Object> session = redisTemplate.opsForHash().entries(key);
        if (session.isEmpty()) {
            log.debug("Disconnect for unknown session: {}", sessionId);
            return;
        }

        String roomCode = session.get("roomCode").toString();
        String playerId = session.get("playerId").toString();

        // Mark player as disconnected in Redis
        String playerKey = "room:" + roomCode + ":player:" + playerId;
        redisTemplate.opsForHash().put(playerKey, "connected", "false");

        // Clean up session mapping
        redisTemplate.delete(key);

        // Notify room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/players",
                Map.of(
                    "event", "player_disconnected",
                    "playerId", playerId
                ));

        log.info("Player {} disconnected from room {}, scheduling removal in 30s", playerId, roomCode);

        // Schedule removal after 30 seconds if still disconnected
        scheduler.schedule(() -> removeIfStillDisconnected(roomCode, playerId), 30, TimeUnit.SECONDS);
    }

    private void removeIfStillDisconnected(String roomCode, String playerId) {
        String playerKey = "room:" + roomCode + ":player:" + playerId;
        String connected = (String) redisTemplate.opsForHash().get(playerKey, "connected");

        if ("true".equals(connected)) {
            log.debug("Player {} reconnected to room {}, skipping removal", playerId, roomCode);
            return;
        }

        // Still disconnected — remove from room
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForSet().remove(roomKey + ":players", playerId);
        redisTemplate.delete(playerKey);

        // Check if room is now empty
        Set<String> remaining = redisTemplate.opsForSet().members(roomKey + ":players");
        if (remaining == null || remaining.isEmpty()) {
            log.info("Room {} is empty after player {} removal, marking for cleanup", roomCode, playerId);
        }

        // Notify room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/players",
                Map.of(
                    "event", "player_removed",
                    "playerId", playerId
                ));

        log.info("Player {} removed from room {} after 30s disconnect timeout", playerId, roomCode);
    }
}
