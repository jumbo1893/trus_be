package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.entity.*;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.*;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.exceptions.AiUnavailableException;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchReportContextService {
    private final FootballMatchRepository footballMatches;
    private final MatchRepository matches;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public void checkAccess(Long id, AppTeamEntity team) {
        requireMatch(id, team);
    }

    private FootballMatchEntity requireMatch(Long id, AppTeamEntity team) {
        FootballMatchEntity match = footballMatches.findById(id).orElse(null);
        if (match == null || team.getTeam() == null ||
                (!Objects.equals(match.getHomeTeam().getId(), team.getTeam().getId()) &&
                 !Objects.equals(match.getAwayTeam().getId(), team.getTeam().getId()))) {
            throw new AuthException("Zápas není dostupný v aktuálním týmu.", AuthException.INSUFFICIENT_RIGHTS);
        }
        return match;
    }

    @Transactional(readOnly = true)
    public String build(Long id, AppTeamEntity team) {
        FootballMatchEntity match = requireMatch(id, team);
        MatchEntity local = matches.findAllByFootballMatchId(id, team.getId()).orElse(null);
        Integer homeGoals = local != null && local.getHomeGoalNumber() != null && local.getAwayGoalNumber() != null
                ? local.getHomeGoalNumber() : match.getHomeGoalNumber();
        Integer awayGoals = local != null && local.getHomeGoalNumber() != null && local.getAwayGoalNumber() != null
                ? local.getAwayGoalNumber() : match.getAwayGoalNumber();
        if (match.getDate() == null || !match.getDate().before(new Date()) ||
                homeGoals == null || awayGoals == null ||
                (!match.isAlreadyPlayed() && (local == null || local.getHomeGoalNumber() == null || local.getAwayGoalNumber() == null))) {
            throw new AiUnavailableException("Report lze vytvořit až pro odehraný zápas se známým výsledkem.");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentAppTeam", team.getName());
        Map<String, Object> target = matchSummary(match);
        target.put("homeGoals", homeGoals);
        target.put("awayGoals", awayGoals);
        target.put("referee", match.getReferee());
        target.put("refereeComment", match.getRefereeComment());
        target.put("stadium", match.getStadium());
        target.put("players", safe(match.getPlayerList()).stream().map(p -> playerSummary(p, team.getId())).toList());
        data.put("match", target);
        data.put("season", match.getLeague().getName() + " " + match.getLeague().getYear());
        List<FootballMatchEntity> homeSeason = seasonBefore(match, match.getHomeTeam().getId());
        List<FootballMatchEntity> awaySeason = seasonBefore(match, match.getAwayTeam().getId());
        data.put("homeSeasonBeforeMatch", homeSeason.stream().map(this::matchSummary).toList());
        data.put("awaySeasonBeforeMatch", awaySeason.stream().map(this::matchSummary).toList());
        data.put("homeFormLastFiveBeforeMatch", formBefore(match, match.getHomeTeam().getId()));
        data.put("awayFormLastFiveBeforeMatch", formBefore(match, match.getAwayTeam().getId()));
        data.put("previousHeadToHead", footballMatches.findAlreadyPlayedMatchesOfTwoTeams(
                match.getHomeTeam().getId(), match.getAwayTeam().getId()).stream()
                .filter(m -> isBefore(m, match)).limit(10).map(this::matchSummary).toList());
        data.put("homeSeasonPlayerStatsBeforeMatch", seasonPlayers(homeSeason, match.getHomeTeam().getId(), team.getId()));
        data.put("awaySeasonPlayerStatsBeforeMatch", seasonPlayers(awaySeason, match.getAwayTeam().getId(), team.getId()));
        data.put("tableNote", "Poslední importovaná tabulka dané sezony v době generování. Není to historická tabulka k datu zápasu; neodvozuj z ní tehdejší pořadí ani posun po zápase.");
        data.put("latestImportedSeasonTable", safe(match.getLeague().getTableTeamList()).stream()
                .sorted(Comparator.comparingInt(TableTeamEntity::getRank))
                .map(t -> fields("team", t.getTeam().getName(), "rank", t.getRank(), "points", t.getPoints(),
                        "matches", t.getMatches(), "wins", t.getWins(), "draws", t.getDraws(), "losses", t.getLosses(),
                        "goalsScored", t.getGoalsScored(), "goalsReceived", t.getGoalsReceived(), "penalty", t.getPenalty())).toList());
        data.put("localMatchData", local == null ? null : localSummary(local, team.getId()));
        data.put("missingDataNote", "Prázdné seznamy znamenají žádné evidované údaje, nikoli jistotu, že se nic nestalo. Ruční góly a oficiální statistiky jsou dva zdroje téhož výkonu: nesčítej je. Piva nejsou panáky.");
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new AiUnavailableException("Nelze připravit podklady pro report.", e);
        }
    }

    private List<FootballMatchEntity> seasonBefore(FootballMatchEntity target, Long teamId) {
        return footballMatches.findAiTeamMatchesInLeague(teamId, target.getLeague().getId()).stream()
                .filter(m -> isBefore(m, target)).sorted(Comparator.comparing(FootballMatchEntity::getDate).reversed()).toList();
    }

    private List<Map<String, Object>> formBefore(FootballMatchEntity target, Long teamId) {
        // Include the previous season at the start of a new one.
        return footballMatches.findReportForm(teamId, target.getDate(),
                org.springframework.data.domain.PageRequest.of(0, 5)).stream()
                .filter(m -> isBefore(m, target)).map(this::matchSummary).toList();
    }

    private boolean isBefore(FootballMatchEntity m, FootballMatchEntity target) {
        return m.isAlreadyPlayed() && m.getDate() != null && m.getDate().before(target.getDate())
                && m.getHomeGoalNumber() != null && m.getAwayGoalNumber() != null;
    }

    private Map<String, Object> matchSummary(FootballMatchEntity m) {
        return fields("id", m.getId(), "date", m.getDate(), "homeTeam", m.getHomeTeam().getName(),
                "awayTeam", m.getAwayTeam().getName(), "homeGoals", m.getHomeGoalNumber(),
                "awayGoals", m.getAwayGoalNumber(), "round", m.getRound());
    }

    private Map<String, Object> playerSummary(FootballMatchPlayerEntity p, Long appTeamId) {
        return fields("player", officialIdentity(p.getPlayer(), appTeamId), "team", p.getTeam().getName(),
                "goals", p.getGoals(), "ownGoals", p.getOwnGoals(), "yellowCards", p.getYellowCards(),
                "redCards", p.getRedCards(), "bestPlayer", p.isBestPlayer(),
                "receivedGoals", p.getReceivedGoals(), "goalkeepingMinutes", p.getGoalkeepingMinutes(),
                "yellowCardComment", p.getYellowCardComment(), "redCardComment", p.getRedCardComment());
    }

    private List<Map<String, Object>> seasonPlayers(List<FootballMatchEntity> season, Long teamId, Long appTeamId) {
        Map<Long, Map<String, Object>> players = new LinkedHashMap<>();
        for (FootballMatchEntity match : season) {
            for (FootballMatchPlayerEntity p : safe(match.getPlayerList())) {
                if (!Objects.equals(p.getTeam().getId(), teamId)) continue;
                Map<String, Object> row = players.computeIfAbsent(p.getPlayer().getId(), key ->
                        fields("player", officialIdentity(p.getPlayer(), appTeamId), "matches", 0, "goals", 0, "yellowCards", 0, "redCards", 0));
                row.put("matches", (int) row.get("matches") + 1);
                row.put("goals", (int) row.get("goals") + p.getGoals());
                row.put("yellowCards", (int) row.get("yellowCards") + p.getYellowCards());
                row.put("redCards", (int) row.get("redCards") + p.getRedCards());
            }
        }
        return new ArrayList<>(players.values());
    }

    private Map<String, Object> localSummary(MatchEntity m, Long teamId) {
        Map<String, Object> result = fields("players", safe(m.getPlayerList()).stream()
                .filter(p -> p.getAppTeam() != null && Objects.equals(p.getAppTeam().getId(), teamId))
                .map(this::localIdentity).toList());
        result.put("goalsAndAssists", safe(m.getGoalList()).stream().filter(g -> Objects.equals(g.getAppTeam().getId(), teamId))
                .map(g -> fields("player", localIdentity(g.getPlayer()), "goals", g.getGoalNumber(), "assists", g.getAssistNumber())).toList());
        result.put("fines", safe(m.getFineList()).stream().filter(f -> Objects.equals(f.getAppTeam().getId(), teamId))
                .map(f -> fields("player", localIdentity(f.getPlayer()), "situation", f.getFine().getName(),
                        "count", f.getFineNumber(), "unitAmountCzk", f.getFine().getAmount())).toList());
        List<BeerEntity> beers = safe(m.getBeerList()).stream().filter(b -> Objects.equals(b.getAppTeam().getId(), teamId)).toList();
        result.put("drinks", beers.stream().map(b -> fields("player", localIdentity(b.getPlayer()),
                "beers", b.getBeerNumber(), "liquors", b.getLiquorNumber())).toList());
        result.put("totalBeersRecorded", beers.stream().mapToInt(BeerEntity::getBeerNumber).sum());
        MatchWeatherEntity w = m.getWeather();
        result.put("weather", w == null ? null : fields("temperatureC", w.getTemperature(),
                "precipitationMm", w.getPrecipitation(), "windSpeed", w.getWindSpeed(),
                "weatherCode", w.getWeatherCode(), "sourceType", w.getSourceType(), "measuredAt", w.getMeasuredAt()));
        return result;
    }

    private Map<String, Object> officialIdentity(FootballPlayerEntity player, Long appTeamId) {
        PlayerEntity local = player.getPlayer();
        if (local != null && local.getAppTeam() != null && Objects.equals(local.getAppTeam().getId(), appTeamId)) {
            return fields("footballPlayerId", player.getId(), "officialName", player.getName(),
                    "playerId", local.getId(), "nickname", local.getName(), "displayName", local.getName());
        }
        return fields("footballPlayerId", player.getId(), "officialName", player.getName(), "displayName", player.getName());
    }

    private Map<String, Object> localIdentity(PlayerEntity player) {
        FootballPlayerEntity official = player.getFootballPlayer();
        return fields("playerId", player.getId(), "nickname", player.getName(), "displayName", player.getName(),
                "footballPlayerId", official == null ? null : official.getId(),
                "officialName", official == null ? null : official.getName());
    }

    private static <T> Collection<T> safe(Collection<T> list) {
        return list == null ? List.of() : list;
    }

    private static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
