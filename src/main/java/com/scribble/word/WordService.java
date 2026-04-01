package com.scribble.word;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;

    public List<Word> pickRandomWords(int count) {
        return wordRepository.findRandom(count);
    }

    public List<Word> pickRandomWordsByDifficulty(WordDifficulty difficulty, int count) {
        return wordRepository.findRandomByDifficulty(difficulty.name(), count);
    }
}
