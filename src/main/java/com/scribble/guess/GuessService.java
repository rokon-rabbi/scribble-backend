package com.scribble.guess;

import com.scribble.common.exception.ResourceNotFoundException;
import com.scribble.game.GameFlowService;
import com.scribble.game.Game;
import com.scribble.game.GameRepository;
import com.scribble.round.Round;
import com.scribble.round.RoundRepository;
import com.scribble.round.RoundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GuessService {

    private final GuessRepository guessRepository;
    private final GameRepository gameRepository;
    private final RoundRepository roundRepository;
    private final GameFlowService gameFlowService;

    @Transactional
    public GuessResult processGuess(String roomCode, UUID playerId, String guessText) {
        Game game = gameRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomCode));

        Round round = roundRepository.findByGameIdAndStatus(game.getId(), RoundStatus.DRAWING)
                .orElse(null);
        if (round == null) {
            return new GuessResult(false, playerId, guessText, 0);
        }

        // Drawer cannot guess
        if (round.getDrawerId().equals(playerId)) {
            return new GuessResult(false, playerId, guessText, 0);
        }

        // Already guessed correctly this round
        if (guessRepository.existsByRoundIdAndPlayerIdAndCorrectTrue(round.getId(), playerId)) {
            return new GuessResult(false, playerId, guessText, 0);
        }

        String currentWord = gameFlowService.getCurrentWord(roomCode);
        if (currentWord == null) {
            return new GuessResult(false, playerId, guessText, 0);
        }

        boolean isCorrect = currentWord.equalsIgnoreCase(guessText.trim());

        // Calculate time taken
        long turnStartedAt = gameFlowService.getTurnStartedAt(roomCode);
        int timeTakenMs = turnStartedAt > 0
                ? (int) (System.currentTimeMillis() - turnStartedAt)
                : 0;

        // Persist guess
        Guess guess = new Guess();
        guess.setRoundId(round.getId());
        guess.setPlayerId(playerId);
        guess.setGuessText(guessText.trim());
        guess.setCorrect(isCorrect);
        guess.setTimeTakenMs(timeTakenMs);
        guessRepository.save(guess);

        if (isCorrect) {
            log.info("Player {} guessed correctly in room {} ({}ms)", playerId, roomCode, timeTakenMs);
            gameFlowService.onCorrectGuess(roomCode, playerId);
        }

        return new GuessResult(isCorrect, playerId, guessText.trim(), timeTakenMs);
    }

    public record GuessResult(
        boolean correct,
        UUID playerId,
        String guessText,
        int timeTakenMs
    ) {}
}
