CREATE TABLE words (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    word VARCHAR(50) NOT NULL UNIQUE,
    difficulty VARCHAR(10) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    category VARCHAR(50)
);

CREATE INDEX idx_words_difficulty ON words(difficulty);
