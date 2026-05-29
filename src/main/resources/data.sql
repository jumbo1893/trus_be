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

DROP VIEW IF EXISTS best_scorer_view;

-- indexy
CREATE INDEX IF NOT EXISTS idx_fmp_team_match_player
    ON football_match_player (team_id, match_id, player_id);

CREATE INDEX IF NOT EXISTS idx_fmp_goals_notnull
    ON football_match_player (match_id)
    WHERE goals IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fm_league_id
    ON football_match (id, league_id);

CREATE INDEX IF NOT EXISTS idx_fmp_team_player_match
    ON football_match_player (team_id, player_id, match_id);

CREATE INDEX IF NOT EXISTS idx_fm_id_league
    ON football_match (id, league_id);

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
    fmp.team_id, fmp.player_id, fm.league_id;

create table if not exists team_recalc_job (
  team_id       bigint primary key,
  payload       jsonb       not null,
  available_at  timestamptz not null default now(),
  attempts      int         not null default 0,
  updated_at    timestamptz not null default now()
);

create index if not exists idx_team_recalc_job_available_at
  on team_recalc_job (available_at);

  ALTER TABLE device_token ADD COLUMN IF NOT EXISTS client_device_id varchar(255);

  CREATE INDEX IF NOT EXISTS idx_device_token_token
      ON device_token(token);

  CREATE INDEX IF NOT EXISTS idx_device_token_user_device_status
      ON device_token(user_id, client_device_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_achievement_code
ON achievement (code);

ALTER TABLE player
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE player
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE match
    ADD COLUMN IF NOT EXISTS home_goal_number INTEGER,
    ADD COLUMN IF NOT EXISTS away_goal_number INTEGER;