CREATE TABLE rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    drawer_id UUID NOT NULL REFERENCES players(id),
    word_id UUID REFERENCES words(id),
    round_number INT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'DRAWING', 'ENDED')),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,

    UNIQUE(game_id, round_number)
);

CREATE INDEX idx_rounds_game ON rounds(game_id);
