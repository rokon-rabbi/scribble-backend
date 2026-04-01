CREATE TABLE round_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id),
    points_earned INT NOT NULL DEFAULT 0,
    role VARCHAR(10) NOT NULL CHECK (role IN ('DRAWER', 'GUESSER')),

    UNIQUE(round_id, player_id)
);

CREATE INDEX idx_round_scores_round ON round_scores(round_id);
