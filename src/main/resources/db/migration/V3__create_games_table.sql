CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_code VARCHAR(6) NOT NULL UNIQUE,
    status VARCHAR(10) NOT NULL DEFAULT 'WAITING'
        CHECK (status IN ('WAITING', 'PLAYING', 'FINISHED')),
    max_players INT NOT NULL DEFAULT 8,
    rounds_per_player INT NOT NULL DEFAULT 2,
    turn_time_seconds INT NOT NULL DEFAULT 80,
    created_by UUID NOT NULL REFERENCES players(id),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_games_room_code ON games(room_code);
CREATE INDEX idx_games_status ON games(status);
