package com.scribble.game;

import com.scribble.common.exception.ResourceNotFoundException;
import com.scribble.common.exception.RoomFullException;
import com.scribble.common.util.RoomCodeGenerator;
import com.scribble.game.dto.CreateRoomRequest;
import com.scribble.game.dto.RoomInfo;
import com.scribble.participant.GameParticipant;
import com.scribble.participant.GameParticipantRepository;
import com.scribble.player.Player;
import com.scribble.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository participantRepo;
    private final PlayerRepository playerRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RoomCodeGenerator roomCodeGenerator;

    @Transactional
    public RoomInfo createRoom(UUID playerId, CreateRoomRequest request) {
        Player owner = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        String roomCode = generateUniqueRoomCode();

        Game game = new Game();
        game.setRoomCode(roomCode);
        game.setStatus(GameStatus.WAITING);
        game.setMaxPlayers(request.maxPlayers());
        game.setRoundsPerPlayer(request.roundsPerPlayer());
        game.setTurnTimeSeconds(request.turnTimeSeconds());
        game.setCreatedBy(playerId);
        game = gameRepository.save(game);

        // Add creator as first participant
        GameParticipant participant = new GameParticipant();
        participant.setGameId(game.getId());
        participant.setPlayerId(playerId);
        participantRepo.save(participant);

        // Initialize Redis room state
        String key = "room:" + roomCode;
        redisTemplate.opsForHash().putAll(key, Map.of(
            "gameId", game.getId().toString(),
            "status", "WAITING",
            "ownerId", playerId.toString()
        ));
        redisTemplate.expire(key, Duration.ofHours(2));

        // Add player to room's player set
        redisTemplate.opsForSet().add("room:" + roomCode + ":players", playerId.toString());

        // Cache player info
        String playerKey = "room:" + roomCode + ":player:" + playerId;
        redisTemplate.opsForHash().putAll(playerKey, Map.of(
            "username", owner.getUsername(),
            "avatarUrl", owner.getAvatarUrl() != null ? owner.getAvatarUrl() : "",
            "score", "0"
        ));
        redisTemplate.expire(playerKey, Duration.ofHours(2));

        // Add to public rooms
        redisTemplate.opsForSet().add("rooms:public", roomCode);

        log.info("Room {} created by player {}", roomCode, owner.getUsername());
        return RoomInfo.from(game, 1, owner.getUsername());
    }

    public Page<RoomInfo> listPublicRooms(Pageable pageable) {
        return gameRepository.findByStatus(GameStatus.WAITING, pageable)
                .map(game -> {
                    int playerCount = participantRepo.countByGameId(game.getId());
                    String ownerName = playerRepository.findById(game.getCreatedBy())
                            .map(Player::getUsername)
                            .orElse("Unknown");
                    return RoomInfo.from(game, playerCount, ownerName);
                });
    }

    public RoomInfo getRoomInfo(String roomCode) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        int playerCount = participantRepo.countByGameId(game.getId());
        String ownerName = playerRepository.findById(game.getCreatedBy())
                .map(Player::getUsername)
                .orElse("Unknown");
        return RoomInfo.from(game, playerCount, ownerName);
    }

    private String generateUniqueRoomCode() {
        for (int i = 0; i < 10; i++) {
            String code = roomCodeGenerator.generate();
            if (gameRepository.findByRoomCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique room code");
    }
}
