package com.scribble.word;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WordRepository extends JpaRepository<Word, UUID> {

    @Query(value = "SELECT * FROM words WHERE difficulty = :difficulty ORDER BY RANDOM() LIMIT :count",
           nativeQuery = true)
    List<Word> findRandomByDifficulty(@Param("difficulty") String difficulty, @Param("count") int count);

    @Query(value = "SELECT * FROM words ORDER BY RANDOM() LIMIT :count",
           nativeQuery = true)
    List<Word> findRandom(@Param("count") int count);
}
