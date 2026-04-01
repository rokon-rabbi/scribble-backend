package com.scribble.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {

    @Value("${game.scoring.max-guesser-points}")
    private int maxGuesserPoints;

    @Value("${game.scoring.drawer-points-per-guess}")
    private int drawerPointsPerGuess;

    /**
     * Guesser gets more points for guessing faster.
     * Formula: (timeRemaining / totalTime) × maxPoints
     */
    public int calculateGuesserScore(long timeRemainingMs, long totalTimeMs) {
        if (totalTimeMs <= 0) return 0;
        double ratio = (double) timeRemainingMs / totalTimeMs;
        return (int) Math.round(ratio * maxGuesserPoints);
    }

    /**
     * Drawer gets flat points per correct guesser.
     */
    public int calculateDrawerScore(int correctGuessCount) {
        return correctGuessCount * drawerPointsPerGuess;
    }
}
