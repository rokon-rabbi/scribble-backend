package com.scribble.word;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WordDifficulty difficulty;

    @Column(length = 50)
    private String category;
}
