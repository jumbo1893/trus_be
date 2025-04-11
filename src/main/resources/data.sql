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

-- view pro souhrn inv. statistik
CREATE OR REPLACE VIEW football_sum_individual_stats AS
SELECT
    fmp.team_id AS team_id,
    fmp.player_id AS player_id,
	fm.league_id AS league_id,
	COUNT(fmp.player_id) AS matches,
    SUM(fmp.goalkeeping_minutes) AS goalkeeping_minutes,
	SUM(fmp.goals) AS goals,
	SUM(fmp.own_goals) AS own_goals,
	SUM(fmp.received_goals) AS received_goals,
	SUM(CAST(fmp.best_player AS INT)) AS best_players,
	SUM(CAST(fmp.hattrick AS INT)) AS hattricks,
	SUM(CAST(fmp.clean_sheet AS INT)) AS clean_sheets,
	SUM(fmp.yellow_cards) AS yellow_cards,
	SUM(fmp.red_cards) AS red_cards,
	SUM( -- Výpočet bodů za zápas
        CASE
            WHEN fmp.team_id = fm.home_team_id THEN
                CASE
                    WHEN fm.home_goal_number > fm.away_goal_number THEN 3
                    WHEN fm.home_goal_number = fm.away_goal_number THEN 1
                    ELSE 0
                END
            WHEN fmp.team_id = fm.away_team_id THEN
                CASE
                    WHEN fm.away_goal_number > fm.home_goal_number THEN 3
                    WHEN fm.away_goal_number = fm.home_goal_number THEN 1
                    ELSE 0
                END
            ELSE 0
        END
    ) AS match_points
FROM
    football_match_player fmp
JOIN
    football_match fm ON fmp.match_id = fm.id
GROUP BY
    fmp.team_id, fmp.player_id, fm.league_id
ORDER BY
    matches DESC;