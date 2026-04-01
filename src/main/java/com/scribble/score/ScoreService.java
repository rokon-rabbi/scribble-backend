package com.scribble.score;

import com.scribble.common.util.ScoreCalculator;
import com.scribble.guess.Guess;
import com.scribble.guess.GuessRepository;
import com.scribble.round.Round;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScoreService {

    private final RoundScoreRepository roundScoreRepository;
    private final GuessRepository guessRepository;
    private final ScoreCalculator scoreCalculator;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public List<Map<String, Object>> calculateAndPersistRoundScores(
            Round round, String roomCode, long turnTimeMs) {

        List<Guess> correctGuesses = guessRepository.findByRoundId(round.getId()).stream()
                .filter(Guess::isCorrect)
                .toList();

        List<Map<String, Object>> scoreEntries = new ArrayList<>();
        String roomKey = "room:" + roomCode;

        // Score each correct guesser
        for (Guess guess : correctGuesses) {
            long timeRemainingMs = turnTimeMs - guess.getTimeTakenMs();
            if (timeRemainingMs < 0) timeRemainingMs = 0;

            int points = scoreCalculator.calculateGuesserScore(timeRemainingMs, turnTimeMs);

            RoundScore score = new RoundScore();
            score.setRoundId(round.getId());
            score.setPlayerId(guess.getPlayerId());
            score.setPointsEarned(points);
            score.setRole(ScoreRole.GUESSER);
            roundScoreRepository.save(score);

            // Update running score in Redis
            updateRedisScore(roomKey, guess.getPlayerId(), points);

            String username = getPlayerUsername(roomKey, guess.getPlayerId());
            scoreEntries.add(Map.of(
                "playerId", guess.getPlayerId().toString(),
                "username", username,
                "points", points,
                "role", "GUESSER",
                "timeTakenMs", guess.getTimeTakenMs()
            ));

            log.debug("Guesser {} scored {} points ({}ms remaining)",
                    guess.getPlayerId(), points, timeRemainingMs);
        }

        // Score drawer
        int drawerPoints = scoreCalculator.calculateDrawerScore(correctGuesses.size());
        if (drawerPoints > 0) {
            RoundScore drawerScore = new RoundScore();
            drawerScore.setRoundId(round.getId());
            drawerScore.setPlayerId(round.getDrawerId());
            drawerScore.setPointsEarned(drawerPoints);
            drawerScore.setRole(ScoreRole.DRAWER);
            roundScoreRepository.save(drawerScore);

            updateRedisScore(roomKey, round.getDrawerId(), drawerPoints);

            String drawerUsername = getPlayerUsername(roomKey, round.getDrawerId());
            scoreEntries.add(Map.of(
                "playerId", round.getDrawerId().toString(),
                "username", drawerUsername,
                "points", drawerPoints,
                "role", "DRAWER",
                "timeTakenMs", 0
            ));

            log.debug("Drawer {} scored {} points ({} correct guesses)",
                    round.getDrawerId(), drawerPoints, correctGuesses.size());
        }

        return scoreEntries;
    }

    private void updateRedisScore(String roomKey, UUID playerId, int pointsToAdd) {
        String playerKey = roomKey + ":player:" + playerId;
        String currentStr = (String) redisTemplate.opsForHash().get(playerKey, "score");
        int current = currentStr != null ? Integer.parseInt(currentStr) : 0;
        redisTemplate.opsForHash().put(playerKey, "score", String.valueOf(current + pointsToAdd));
    }

    private String getPlayerUsername(String roomKey, UUID playerId) {
        String username = (String) redisTemplate.opsForHash()
                .get(roomKey + ":player:" + playerId, "username");
        return username != null ? username : "Unknown";
    }
}
