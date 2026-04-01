package com.scribble.game;

import com.scribble.common.exception.GameStateException;
import com.scribble.common.exception.ResourceNotFoundException;
import com.scribble.participant.GameParticipant;
import com.scribble.participant.GameParticipantRepository;
import com.scribble.player.Player;
import com.scribble.player.PlayerRepository;
import com.scribble.round.Round;
import com.scribble.round.RoundRepository;
import com.scribble.round.RoundStatus;
import com.scribble.round.dto.RoundEndEvent;
import com.scribble.round.dto.RoundStartEvent;
import com.scribble.websocket.dto.GameStartMessage;
import com.scribble.websocket.dto.WordChoiceMessage;
import com.scribble.word.Word;
import com.scribble.word.WordRepository;
import com.scribble.word.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameFlowService {

    private final GameRepository gameRepository;
    private final GameParticipantRepository participantRepo;
    private final RoundRepository roundRepository;
    private final PlayerRepository playerRepository;
    private final WordService wordService;
    private final WordRepository wordRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${game.defaults.word-choices:3}")
    private int wordChoiceCount;

    private final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

    @Transactional
    public void startGame(String roomCode, UUID requesterId) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        if (game.getStatus() != GameStatus.WAITING) {
            throw new GameStateException("Game is not in WAITING state");
        }
        if (!game.getCreatedBy().equals(requesterId)) {
            throw new GameStateException("Only the room owner can start the game");
        }

        List<GameParticipant> participants = participantRepo.findByGameId(game.getId());
        if (participants.size() < 2) {
            throw new GameStateException("Need at least 2 players to start");
        }

        // Shuffle player order for drawing turns
        List<UUID> playerOrder = new ArrayList<>(participants.stream()
                .map(GameParticipant::getPlayerId).toList());
        Collections.shuffle(playerOrder);

        // Create all round entities
        int totalRounds = playerOrder.size() * game.getRoundsPerPlayer();
        for (int i = 0; i < totalRounds; i++) {
            Round round = new Round();
            round.setGameId(game.getId());
            round.setDrawerId(playerOrder.get(i % playerOrder.size()));
            round.setRoundNumber(i + 1);
            round.setStatus(RoundStatus.PENDING);
            roundRepository.save(round);
        }

        // Update game status
        game.setStatus(GameStatus.PLAYING);
        game.setStartedAt(Instant.now());
        gameRepository.save(game);

        // Update Redis
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForHash().put(roomKey, "status", "PLAYING");
        redisTemplate.opsForHash().put(roomKey, "totalRounds", String.valueOf(totalRounds));
        redisTemplate.opsForHash().put(roomKey, "currentRound", "0");

        // Remove from public rooms
        redisTemplate.opsForSet().remove("rooms:public", roomCode);

        // Broadcast game_start
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game",
                Map.of(
                    "event", "game_start",
                    "totalRounds", totalRounds,
                    "playerOrder", playerOrder
                ));

        log.info("Game started in room {} with {} players, {} rounds", roomCode, participants.size(), totalRounds);

        // Start first round
        startNextRound(roomCode);
    }

    public void startNextRound(String roomCode) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        Optional<Round> nextRound = roundRepository
                .findFirstByGameIdAndStatusOrderByRoundNumber(game.getId(), RoundStatus.PENDING);

        if (nextRound.isEmpty()) {
            endGame(roomCode);
            return;
        }

        Round round = nextRound.get();
        round.setStatus(RoundStatus.DRAWING);
        round.setStartedAt(Instant.now());
        roundRepository.save(round);

        // Update Redis
        String roomKey = "room:" + roomCode;
        redisTemplate.opsForHash().put(roomKey, "currentRound", String.valueOf(round.getRoundNumber()));
        redisTemplate.opsForHash().put(roomKey, "drawerId", round.getDrawerId().toString());
        redisTemplate.opsForHash().put(roomKey, "correctGuessers", "");
        redisTemplate.opsForHash().put(roomKey, "turnTimeMs", String.valueOf(game.getTurnTimeSeconds() * 1000L));
        redisTemplate.expire(roomKey, Duration.ofHours(2));

        // Clear stroke buffer
        redisTemplate.delete("room:" + roomCode + ":strokes");

        // Pick random words for drawer to choose from
        List<Word> words = wordService.pickRandomWords(wordChoiceCount);
        List<WordChoiceMessage.WordOption> options = words.stream()
                .map(w -> new WordChoiceMessage.WordOption(w.getId(), w.getWord()))
                .toList();

        // Get drawer username
        String drawerUsername = playerRepository.findById(round.getDrawerId())
                .map(Player::getUsername).orElse("Unknown");

        // Send word choices to drawer-specific topic
        String drawerId = round.getDrawerId().toString();
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/word-choices",
                new WordChoiceMessage(options));

        // Notify room that a new round is starting (without revealing words)
        int totalRounds = Integer.parseInt(
                redisTemplate.opsForHash().get(roomKey, "totalRounds").toString());

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game",
                Map.of(
                    "event", "round_choosing",
                    "roundNumber", round.getRoundNumber(),
                    "totalRounds", totalRounds,
                    "drawerId", drawerId,
                    "drawerUsername", drawerUsername
                ));

        log.info("Round {} started in room {}, drawer: {}", round.getRoundNumber(), roomCode, drawerUsername);
    }

    @Transactional
    public void onDrawerPickedWord(String roomCode, UUID drawerId, UUID wordId) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        Round round = roundRepository.findByGameIdAndStatus(game.getId(), RoundStatus.DRAWING)
                .orElseThrow(() -> new GameStateException("No active drawing round"));

        if (!round.getDrawerId().equals(drawerId)) {
            throw new GameStateException("You are not the drawer");
        }

        round.setWordId(wordId);
        roundRepository.save(round);

        // Store word in Redis
        String roomKey = "room:" + roomCode;
        String wordText = wordRepository.findById(wordId)
                .map(Word::getWord)
                .orElseThrow(() -> new ResourceNotFoundException("Word not found"));

        redisTemplate.opsForHash().put(roomKey, "currentWord", wordText);
        redisTemplate.opsForHash().put(roomKey, "turnStartedAt",
                String.valueOf(System.currentTimeMillis()));

        // Generate hint
        String hint = generateHint(wordText);

        // Get drawer info
        String drawerUsername = playerRepository.findById(drawerId)
                .map(Player::getUsername).orElse("Unknown");
        int totalRounds = Integer.parseInt(
                redisTemplate.opsForHash().get(roomKey, "totalRounds").toString());
        int turnTimeSeconds = game.getTurnTimeSeconds();

        // Broadcast round_start with hint
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game",
                Map.of(
                    "event", "round_start",
                    "roundNumber", round.getRoundNumber(),
                    "totalRounds", totalRounds,
                    "drawerId", drawerId.toString(),
                    "drawerUsername", drawerUsername,
                    "wordHint", hint,
                    "wordLength", wordText.length(),
                    "turnTimeSeconds", turnTimeSeconds
                ));

        // Start timer
        ScheduledFuture<?> timer = timerExecutor.schedule(
                () -> endRound(roomCode),
                turnTimeSeconds, TimeUnit.SECONDS);
        activeTimers.put(roomCode, timer);

        log.info("Drawer picked word '{}' in room {}, timer started ({}s)",
                wordText, roomCode, turnTimeSeconds);
    }

    public void onCorrectGuess(String roomCode, UUID playerId) {
        String roomKey = "room:" + roomCode;
        String correctGuessers = (String) redisTemplate.opsForHash().get(roomKey, "correctGuessers");
        if (correctGuessers == null) correctGuessers = "";

        // Add player to correct guessers
        if (!correctGuessers.isEmpty()) {
            correctGuessers += "," + playerId;
        } else {
            correctGuessers = playerId.toString();
        }
        redisTemplate.opsForHash().put(roomKey, "correctGuessers", correctGuessers);

        // Check if all non-drawers have guessed
        String drawerId = (String) redisTemplate.opsForHash().get(roomKey, "drawerId");
        Set<String> allPlayers = redisTemplate.opsForSet().members(roomKey + ":players");
        if (allPlayers == null) return;

        long nonDrawerCount = allPlayers.stream()
                .filter(p -> !p.equals(drawerId))
                .count();
        long correctCount = correctGuessers.split(",").length;

        if (correctCount >= nonDrawerCount) {
            log.info("All players guessed correctly in room {}, ending round early", roomCode);
            endRound(roomCode);
        }
    }

    @Transactional
    public void endRound(String roomCode) {
        // Cancel timer
        ScheduledFuture<?> timer = activeTimers.remove(roomCode);
        if (timer != null) {
            timer.cancel(false);
        }

        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        Round round = roundRepository.findByGameIdAndStatus(game.getId(), RoundStatus.DRAWING)
                .orElse(null);
        if (round == null) {
            log.warn("No active round to end in room {}", roomCode);
            return;
        }

        round.setStatus(RoundStatus.ENDED);
        round.setEndedAt(Instant.now());
        roundRepository.save(round);

        // Get the word for reveal
        String roomKey = "room:" + roomCode;
        String currentWord = (String) redisTemplate.opsForHash().get(roomKey, "currentWord");
        if (currentWord == null) currentWord = "???";

        // Broadcast round_end with word reveal
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game",
                Map.of(
                    "event", "round_end",
                    "roundNumber", round.getRoundNumber(),
                    "correctWord", currentWord
                ));

        // Clear round-specific Redis state
        redisTemplate.opsForHash().delete(roomKey, "currentWord", "turnStartedAt", "correctGuessers");

        log.info("Round {} ended in room {}, word was '{}'", round.getRoundNumber(), roomCode, currentWord);

        // Schedule next round after 5-second delay
        timerExecutor.schedule(() -> startNextRound(roomCode), 5, TimeUnit.SECONDS);
    }

    @Transactional
    public void endGame(String roomCode) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        game.setStatus(GameStatus.FINISHED);
        game.setEndedAt(Instant.now());
        gameRepository.save(game);

        // Aggregate scores from Redis
        String roomKey = "room:" + roomCode;
        Set<String> playerIds = redisTemplate.opsForSet().members(roomKey + ":players");

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        if (playerIds != null) {
            for (String pid : playerIds) {
                String scoreStr = (String) redisTemplate.opsForHash()
                        .get(roomKey + ":player:" + pid, "score");
                int score = scoreStr != null ? Integer.parseInt(scoreStr) : 0;
                String username = (String) redisTemplate.opsForHash()
                        .get(roomKey + ":player:" + pid, "username");

                leaderboard.add(Map.of(
                    "playerId", pid,
                    "username", username != null ? username : "Unknown",
                    "score", score
                ));

                // Update participant final score
                UUID playerUuid = UUID.fromString(pid);
                participantRepo.findByGameId(game.getId()).stream()
                        .filter(p -> p.getPlayerId().equals(playerUuid))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setFinalScore(score);
                            participantRepo.save(p);
                        });
            }
        }

        // Sort by score descending and assign ranks
        leaderboard.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        // Update player stats
        if (!leaderboard.isEmpty()) {
            String winnerId = (String) leaderboard.get(0).get("playerId");
            for (Map<String, Object> entry : leaderboard) {
                UUID pid = UUID.fromString((String) entry.get("playerId"));
                playerRepository.findById(pid).ifPresent(player -> {
                    player.setTotalGamesPlayed(player.getTotalGamesPlayed() + 1);
                    player.setTotalScore(player.getTotalScore() + (int) entry.get("score"));
                    if (pid.toString().equals(winnerId)) {
                        player.setTotalWins(player.getTotalWins() + 1);
                    }
                    playerRepository.save(player);
                });
            }
        }

        // Update Redis
        redisTemplate.opsForHash().put(roomKey, "status", "FINISHED");

        // Broadcast game_end
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomCode + "/game",
                Map.of(
                    "event", "game_end",
                    "leaderboard", leaderboard
                ));

        log.info("Game ended in room {}", roomCode);

        // Clean up Redis keys after a delay
        timerExecutor.schedule(() -> cleanupRoom(roomCode), 60, TimeUnit.SECONDS);
    }

    public String getCurrentWord(String roomCode) {
        return (String) redisTemplate.opsForHash().get("room:" + roomCode, "currentWord");
    }

    public long getTurnStartedAt(String roomCode) {
        String val = (String) redisTemplate.opsForHash().get("room:" + roomCode, "turnStartedAt");
        return val != null ? Long.parseLong(val) : 0;
    }

    public long getTurnTimeMs(String roomCode) {
        String val = (String) redisTemplate.opsForHash().get("room:" + roomCode, "turnTimeMs");
        return val != null ? Long.parseLong(val) : 0;
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

    private void cleanupRoom(String roomCode) {
        String roomKey = "room:" + roomCode;
        Set<String> playerIds = redisTemplate.opsForSet().members(roomKey + ":players");
        if (playerIds != null) {
            for (String pid : playerIds) {
                redisTemplate.delete(roomKey + ":player:" + pid);
            }
        }
        redisTemplate.delete(roomKey);
        redisTemplate.delete(roomKey + ":players");
        redisTemplate.delete(roomKey + ":strokes");
        redisTemplate.opsForSet().remove("rooms:public", roomCode);
        log.info("Cleaned up Redis keys for room {}", roomCode);
    }
}
