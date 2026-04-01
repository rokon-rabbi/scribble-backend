INSERT INTO words (word, difficulty, category) VALUES
-- EASY
('cat', 'EASY', 'animals'), ('dog', 'EASY', 'animals'), ('house', 'EASY', 'objects'),
('sun', 'EASY', 'nature'), ('tree', 'EASY', 'nature'), ('car', 'EASY', 'vehicles'),
('fish', 'EASY', 'animals'), ('moon', 'EASY', 'nature'), ('book', 'EASY', 'objects'),
('star', 'EASY', 'nature'), ('flower', 'EASY', 'nature'), ('ball', 'EASY', 'objects'),
('hat', 'EASY', 'clothing'), ('shoe', 'EASY', 'clothing'), ('cake', 'EASY', 'food'),
('bird', 'EASY', 'animals'), ('rain', 'EASY', 'nature'), ('apple', 'EASY', 'food'),
('chair', 'EASY', 'furniture'), ('table', 'EASY', 'furniture'),
-- MEDIUM
('guitar', 'MEDIUM', 'music'), ('elephant', 'MEDIUM', 'animals'),
('bicycle', 'MEDIUM', 'vehicles'), ('rainbow', 'MEDIUM', 'nature'),
('pizza', 'MEDIUM', 'food'), ('castle', 'MEDIUM', 'buildings'),
('rocket', 'MEDIUM', 'space'), ('penguin', 'MEDIUM', 'animals'),
('volcano', 'MEDIUM', 'nature'), ('pirate', 'MEDIUM', 'characters'),
('mermaid', 'MEDIUM', 'characters'), ('dinosaur', 'MEDIUM', 'animals'),
('compass', 'MEDIUM', 'objects'), ('telescope', 'MEDIUM', 'objects'),
('waterfall', 'MEDIUM', 'nature'), ('lighthouse', 'MEDIUM', 'buildings'),
('parachute', 'MEDIUM', 'objects'), ('snowflake', 'MEDIUM', 'nature'),
('treasure', 'MEDIUM', 'objects'), ('hurricane', 'MEDIUM', 'nature'),
-- HARD
('photosynthesis', 'HARD', 'science'), ('constellation', 'HARD', 'space'),
('archaeology', 'HARD', 'science'), ('camouflage', 'HARD', 'nature'),
('hibernation', 'HARD', 'nature'), ('democracy', 'HARD', 'concepts'),
('perspective', 'HARD', 'concepts'), ('ecosystem', 'HARD', 'science'),
('metamorphosis', 'HARD', 'science'), ('electricity', 'HARD', 'science'),
('architecture', 'HARD', 'concepts'), ('imagination', 'HARD', 'concepts'),
('championship', 'HARD', 'sports'), ('civilization', 'HARD', 'concepts'),
('celebration', 'HARD', 'concepts'), ('equilibrium', 'HARD', 'science'),
('gymnastics', 'HARD', 'sports'), ('harmonica', 'HARD', 'music'),
('kaleidoscope', 'HARD', 'objects'), ('labyrinth', 'HARD', 'objects')
ON CONFLICT (word) DO NOTHING;
