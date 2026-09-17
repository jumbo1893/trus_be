package com.jumbo.trus.service.recap;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

/** Each fact table is read separately: joins must never multiply drinks/fines/goals. */
@Repository
@RequiredArgsConstructor
public class SeasonRecapData {
    private final NamedParameterJdbcTemplate jdbc;
    public record Facts(List<Map<String,Object>> players, List<Map<String,Object>> matches,
            List<Map<String,Object>> drinks, List<Map<String,Object>> fines, List<Map<String,Object>> goals,
            List<Map<String,Object>> achievements, List<Map<String,Object>> steps,
            List<Map<String,Object>> footbar, List<Map<String,Object>> attendance) {}

    public List<Map<String,Object>> seasons() {
        return jdbc.queryForList("""
                SELECT s.id, s.app_team_id, s.name, s.from_date, s.to_date, MAX(m.date) AS last_match
                FROM season s JOIN match m ON m.season_id=s.id AND m.app_team_id=s.app_team_id
                WHERE s.id <> -1 AND s.app_team_id IS NOT NULL
                GROUP BY s.id ORDER BY s.app_team_id, MAX(m.date), s.id
                """, Map.of());
    }
    public List<Map<String,Object>> members(long team) {
        return jdbc.queryForList("SELECT user_id, player_id FROM user_team_role WHERE app_team_id=:team ORDER BY user_id", Map.of("team", team));
    }
    public Set<Long> consentingUsers(long team) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT c.user_id FROM step_consent c WHERE c.app_team_id=:team AND c.enabled=true
                  AND EXISTS (SELECT 1 FROM user_team_role r WHERE r.user_id=c.user_id AND r.app_team_id=:team)
                """, Map.of("team", team), Long.class));
    }
    public Facts load(long team, long season, LocalDate from, LocalDate to) {
        var params = new HashMap<String,Object>();
        params.put("team", team); params.put("season", season);
        params.put("from", java.sql.Date.valueOf(from)); params.put("to", java.sql.Date.valueOf(to));
        params.put("start", java.sql.Timestamp.from(from.atStartOfDay(java.time.ZoneId.of("Europe/Prague")).toInstant()));
        params.put("end", java.sql.Timestamp.from(to.plusDays(1).atStartOfDay(java.time.ZoneId.of("Europe/Prague")).toInstant()));
        return new Facts(
            query("SELECT id, name, fan FROM player WHERE app_team_id=:team AND deleted=false", params),
            query("SELECT id, name, date FROM match WHERE season_id=:season AND app_team_id=:team ORDER BY date,id", params),
            query("""
                SELECT b.player_id, b.match_id, SUM(b.beer_number) AS beers, SUM(b.liquor_number) AS shots
                FROM beer b JOIN match m ON m.id=b.match_id
                WHERE m.season_id=:season AND m.app_team_id=:team AND b.app_team_id=:team
                GROUP BY b.player_id,b.match_id
                """, params),
            query("""
                SELECT rf.player_id, rf.match_id, f.name, f.code, rf.fine_number AS count,
                       rf.fine_number * CAST(f.amount AS bigint) AS amount, f.amount AS unit_amount
                FROM received_fine rf JOIN fine f ON f.id=rf.fine_id JOIN match m ON m.id=rf.match_id
                WHERE m.season_id=:season AND m.app_team_id=:team AND rf.app_team_id=:team
                """, params),
            query("""
                SELECT g.player_id, g.match_id, SUM(g.goal_number) AS goals, SUM(g.assist_number) AS assists
                FROM goal g JOIN match m ON m.id=g.match_id
                WHERE m.season_id=:season AND m.app_team_id=:team AND g.app_team_id=:team
                GROUP BY g.player_id,g.match_id
                """, params),
            query("""
                SELECT pa.player_id, pa.achievement_id, a.name,
                  (SELECT COUNT(*) FROM player_achievement allpa JOIN player p ON p.id=allpa.player_id
                   WHERE allpa.achievement_id=a.id AND allpa.accomplished=true AND p.app_team_id=:team
                     AND p.deleted=false AND (a.only_for_players=false OR p.fan=false)) AS holders,
                  (SELECT COUNT(*) FROM player p WHERE p.app_team_id=:team AND p.deleted=false
                     AND (a.only_for_players=false OR p.fan=false)) AS eligible
                FROM player_achievement pa JOIN achievement a ON a.id=pa.achievement_id
                JOIN player p ON p.id=pa.player_id LEFT JOIN match m ON m.id=pa.match_id
                WHERE p.app_team_id=:team AND p.deleted=false AND pa.accomplished=true
                  AND (m.season_id=:season OR pa.season_id=:season OR
                       (pa.match_id IS NULL AND pa.season_id IS NULL AND pa.accomplished_date>=:start AND pa.accomplished_date<:end))
                """, params),
            query("""
                SELECT u.id AS player_id, u.name, s.step_date, s.step_number AS steps
                FROM step_update s JOIN auth u ON u.id=s.user_id
                WHERE s.step_date BETWEEN :from AND :to
                  AND EXISTS (SELECT 1 FROM step_consent c WHERE c.user_id=u.id AND c.app_team_id=:team AND c.enabled=true)
                  AND EXISTS (SELECT 1 FROM user_team_role r WHERE r.user_id=u.id AND r.app_team_id=:team)
                """, params),
            query("""
                SELECT fs.player_id, fs.match_id, fs.shot_count AS shots, fs.pass_count AS passes,
                       fs.distance/1000.0 AS km, fs.shot_speed*3.6 AS speed
                FROM footbar_session fs JOIN match m ON m.id=fs.match_id
                WHERE m.season_id=:season AND m.app_team_id=:team
                """, params),
            query("""
                SELECT DISTINCT mp.player_id, mp.match_id FROM match_players mp JOIN match m ON m.id=mp.match_id
                WHERE m.season_id=:season AND m.app_team_id=:team
                """, params));
    }
    private List<Map<String,Object>> query(String sql, Map<String,Object> params) { return jdbc.queryForList(sql, params); }
}
