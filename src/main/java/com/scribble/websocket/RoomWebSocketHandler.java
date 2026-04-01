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
import com.scribble.round.Round;
import com.scribble.round.RoundRepository;
import com.scribble.round.RoundStatus;
import com.scribble.stroke.StrokeService;
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
    private final RoundRepository roundRepository;
    private final StrokeService strokeService;
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

        boolean isExistingParticipant = participantRepo.existsByGameIdAndPlayerId(game.getId(), playerUuid);

        if (game.getStatus() == GameStatus.WAITING) {
            // Normal join
            int currentCount = participantRepo.countByGameId(game.getId());
            if (!isExistingParticipant && currentCount >= game.getMaxPlayers()) {
                throw new RoomFullException("Room " + roomCode + " is full");
            }
            if (!isExistingParticipant) {
                GameParticipant participant = new GameParticipant();
                participant.setGameId(game.getId());
                participant.setPlayerId(playerUuid);
                participantRepo.save(participant);
            }
        } else if (game.getStatus() == GameStatus.PLAYING) {
            // Only allow reconnection for existing participants
            if (!isExistingParticipant) {
                throw new IllegalStateException("Game already in progress, cannot join");
            }
            log.info("Player {} ({}) reconnecting to room {}", username, playerId, roomCode);
        } else {
            throw new IllegalStateException("Game is finished");
        }

        // Update Redis — mark connected
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForSet().add(roomKey + ":players", playerId);

        Player player = playerRepository.findById(playerUuid).orElseThrow();
        String playerKey = roomKey + ":player:" + playerId;

        // Preserve existing score on reconnect
        String existingScore = (String) redisTemplate.opsForHash().get(playerKey, "score");
        redisTemplate.opsForHash().putAll(playerKey, Map.of(
            "username", username,
            "avatarUrl", player.getAvatarUrl() != null ? player.getAvatarUrl() : "",
            "score", existingScore != null ? existingScore : "0",
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

        // Build player list
        List<Map<String, String>> playerList = buildPlayerList(roomCode);

        // Broadcast player_joined / player_reconnected
        String event = game.getStatus() == GameStatus.PLAYING ? "player_reconnected" : "player_joined";
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/players",
                Map.of(
                    "event", event,
                    "playerId", playerId,
                    "username", username,
                    "players", playerList
                ));

        log.info("Player {} ({}) {} room {}", username, playerId, event, roomCode);

        // If reconnecting mid-game, send current game state and replay strokes
        if (game.getStatus() == GameStatus.PLAYING) {
            sendReconnectionState(roomCode, game, playerId);
        }
    }

    private void sendReconnectionState(String roomCode, Game game, String playerId) {
        String roomKey = "room:" + roomCode;

        // Send current round info
        String currentRoundStr = (String) redisTemplate.opsForHash().get(roomKey, "currentRound");
        String drawerId = (String) redisTemplate.opsForHash().get(roomKey, "drawerId");
        String totalRoundsStr = (String) redisTemplate.opsForHash().get(roomKey, "totalRounds");
        String currentWord = (String) redisTemplate.opsForHash().get(roomKey, "currentWord");
        String turnStartedAtStr = (String) redisTemplate.opsForHash().get(roomKey, "turnStartedAt");
        String turnTimeMsStr = (String) redisTemplate.opsForHash().get(roomKey, "turnTimeMs");

        if (drawerId == null) return; // No active round

        String drawerUsername = playerRepository.findById(UUID.fromString(drawerId))
                .map(Player::getUsername).orElse("Unknown");

        // Calculate remaining time
        long turnStartedAt = turnStartedAtStr != null ? Long.parseLong(turnStartedAtStr) : 0;
        long turnTimeMs = turnTimeMsStr != null ? Long.parseLong(turnTimeMsStr) : 0;
        long elapsed = System.currentTimeMillis() - turnStartedAt;
        long remainingMs = Math.max(0, turnTimeMs - elapsed);

        // Send game state to reconnecting player via their specific topic
        String hint = currentWord != null ? generateHint(currentWord) : "";
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/reconnect/" + playerId,
                Map.of(
                    "event", "reconnect_state",
                    "roundNumber", currentRoundStr != null ? Integer.parseInt(currentRoundStr) : 0,
                    "totalRounds", totalRoundsStr != null ? Integer.parseInt(totalRoundsStr) : 0,
                    "drawerId", drawerId,
                    "drawerUsername", drawerUsername,
                    "wordHint", hint,
                    "wordLength", currentWord != null ? currentWord.length() : 0,
                    "remainingMs", remainingMs,
                    "turnTimeSeconds", turnTimeMs / 1000
                ));

        // Replay strokes for current round
        Round activeRound = roundRepository.findByGameIdAndStatus(game.getId(), RoundStatus.DRAWING)
                .orElse(null);
        if (activeRound != null) {
            List<String> strokes = strokeService.getStrokesForReplay(roomCode, activeRound.getId());
            if (!strokes.isEmpty()) {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomCode + "/reconnect/" + playerId,
                        Map.of(
                            "event", "stroke_replay",
                            "strokes", strokes
                        ));
                log.info("Replayed {} strokes to reconnecting player {} in room {}",
                        strokes.size(), playerId, roomCode);
            }
        }
    }

    private String generateHint(String word) {
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == ' ') {
                hint.append("  ");
            } else {
                hint.append("_ ");
            }
        }
        return hint.toString().trim();
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
