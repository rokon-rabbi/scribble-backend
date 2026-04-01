CREATE TABLE strokes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    stroke_order INT NOT NULL,
    points_data JSONB NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#000000',
    brush_size INT NOT NULL DEFAULT 5,
    tool VARCHAR(10) NOT NULL DEFAULT 'BRUSH'
        CHECK (tool IN ('BRUSH', 'ERASER', 'FILL')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_strokes_round ON strokes(round_id);
CREATE INDEX idx_strokes_order ON strokes(round_id, stroke_order);
