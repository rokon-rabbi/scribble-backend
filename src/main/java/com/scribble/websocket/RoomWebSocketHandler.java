package com.scribble.websocket;

import com.scribble.common.exception.ResourceNotFoundException;
import com.scribble.common.exception.RoomFullException;
import com.scribble.game.Game;
import com.scribble.game.GameRepository;
import com.scribble.game.GameStatus;
import com.scribble.participant.GameParticipant;
import com.scribble.participant.GameParticipantRepository;
import com.scribble.player.Player;
import com.scribble.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketHandler {

    private final GameRepository gameRepository;
    private final GameParticipantRepository participantRepo;
    private final PlayerRepository playerRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomCode}/join")
    public void joinRoom(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = headerAccessor.getSessionAttributes().get("playerId").toString();
        String username = headerAccessor.getSessionAttributes().get("username").toString();
        String sessionId = headerAccessor.getSessionId();
        UUID playerUuid = UUID.fromString(playerId);

        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Game already started or finished");
        }

        int currentCount = participantRepo.countByGameId(game.getId());
        if (currentCount >= game.getMaxPlayers()) {
            throw new RoomFullException("Room " + roomCode + " is full");
        }

        // Add as participant if not already in
        if (!participantRepo.existsByGameIdAndPlayerId(game.getId(), playerUuid)) {
            GameParticipant participant = new GameParticipant();
            participant.setGameId(game.getId());
            participant.setPlayerId(playerUuid);
            participantRepo.save(participant);
        }

        // Update Redis
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForSet().add(roomKey + ":players", playerId);

        Player player = playerRepository.findById(playerUuid).orElseThrow();
        String playerKey = roomKey + ":player:" + playerId;
        redisTemplate.opsForHash().putAll(playerKey, Map.of(
            "username", username,
            "avatarUrl", player.getAvatarUrl() != null ? player.getAvatarUrl() : "",
            "score", "0",
            "connected", "true"
        ));
        redisTemplate.expire(playerKey, Duration.ofHours(2));
        redisTemplate.expire(roomKey, Duration.ofHours(2));

        // Map session to room
        String sessionKey = "ws:session:" + sessionId;
        redisTemplate.opsForHash().putAll(sessionKey, Map.of(
            "roomCode", roomCode,
            "playerId", playerId
        ));
        redisTemplate.expire(sessionKey, Duration.ofMinutes(30));

        // Build player list from Redis
        List<Map<String, String>> playerList = buildPlayerList(roomCode);

        // Broadcast player_joined to room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/players",
                Map.of(
                    "event", "player_joined",
                    "playerId", playerId,
                    "username", username,
                    "players", playerList
                ));

        log.info("Player {} ({}) joined room {}", username, playerId, roomCode);
    }

    private List<Map<String, String>> buildPlayerList(String roomCode) {
        Set<String> playerIds = redisTemplate.opsForSet().members("room:" + roomCode + ":players");
        if (playerIds == null) return List.of();

        List<Map<String, String>> players = new ArrayList<>();
        for (String pid : playerIds) {
            Map<Object, Object> info = redisTemplate.opsForHash().entries("room:" + roomCode + ":player:" + pid);
            if (!info.isEmpty()) {
                players.add(Map.of(
                    "playerId", pid,
                    "username", info.getOrDefault("username", "").toString(),
                    "avatarUrl", info.getOrDefault("avatarUrl", "").toString(),
                    "score", info.getOrDefault("score", "0").toString(),
                    "connected", info.getOrDefault("connected", "true").toString()
                ));
            }
        }
        return players;
    }
}
