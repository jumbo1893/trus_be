-- data.sql

-- Vložení hardcodované sezony Ostatní, pokud neexistuje
INSERT INTO season (id, from_date, name, to_date, editable)
SELECT -1, '1970-01-01', 'Ostatní', '1970-12-31', false
WHERE NOT EXISTS (SELECT 1 FROM season WHERE id = -1);

-- Vložení hardcodované pokuty Gól, pokud neexistuje
INSERT INTO fine (id, amount, name, editable, inactive)
SELECT -1, 10, 'Gol', false, false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -1);

-- Vložení hardcodované pokuty Hattrick, pokud neexistuje
INSERT INTO fine (id, amount, name, editable, inactive)
SELECT -2, 50, 'Hattrick', false, false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -2);

-- view pro best_scorer
CREATE OR REPLACE VIEW best_scorer_view AS
SELECT
    fmp.team_id AS team_id,
    fmp.player_id AS player_id,
    SUM(fmp.goals) AS total_goals,
    fm.league_id AS league_id
FROM
    football_match_player fmp
JOIN
    football_match fm ON fmp.match_id = fm.id
WHERE
    fmp.goals IS NOT NULL
GROUP BY
    fmp.team_id, fmp.player_id, fm.league_id
ORDER BY
    total_goals DESC;