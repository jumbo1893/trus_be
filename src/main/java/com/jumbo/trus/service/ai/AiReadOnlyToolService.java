package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.VisitedCountryResponse;
import com.jumbo.trus.dto.achievement.AchievementDTO;
import com.jumbo.trus.dto.achievement.AchievementDetail;
import com.jumbo.trus.dto.achievement.AchievementPlayerDetail;
import com.jumbo.trus.dto.achievement.IPlayerAchievementStats;
import com.jumbo.trus.dto.achievement.PlayerAchievementDTO;
import com.jumbo.trus.dto.ai.AiFineSummaryProjection;
import com.jumbo.trus.dto.ai.AiRepeatOpponentProjection;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.step.StepPeriod;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.entity.football.LeagueEntity;
import com.jumbo.trus.entity.football.TeamEntity;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.ReceivedFineRepository;
import com.jumbo.trus.repository.TeamVisitedCountryProjection;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.StepService;
import com.jumbo.trus.service.UserVisitedCountryService;
import com.jumbo.trus.service.achievement.AchievementService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerAchievementService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiReadOnlyToolService {

    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Prague");
    private static final int MAX_TOOL_OUTPUT_CHARS = 50_000;

    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;
    private final ReceivedFineRepository receivedFineRepository;
    private final FootballMatchRepository footballMatchRepository;
    private final PlayerService playerService;
    private final SeasonService seasonService;
    private final GoalService goalService;
    private final BeerService beerService;
    private final ReceivedFineService receivedFineService;
    private final AttendanceService attendanceService;
    private final FootballMatchService footballMatchService;
    private final FootballPlayerStatsService footballPlayerStatsService;
    private final TeamService teamService;
    private final PlayerStatsFacade playerStatsFacade;
    private final AchievementService achievementService;
    private final PlayerAchievementService playerAchievementService;
    private final StepService stepService;
    private final UserVisitedCountryService userVisitedCountryService;
    private final TrusBotPersonFactService personFactService;

    public ArrayNode toolDefinitions() {
        try {
            return (ArrayNode) objectMapper.readTree(TOOL_DEFINITIONS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nelze načíst definice AI nástrojů", exception);
        }
    }

    @Transactional(readOnly = true)
    public String execute(String toolName, JsonNode arguments, AiToolContext context) {
        try {
            Object result = switch (toolName) {
                case "find_matches" -> findMatches(arguments, context);
                case "find_official_matches" -> findOfficialMatches(arguments, context);
                case "read_team_statistics" -> readTeamStatistics(arguments, context);
                case "read_fine_summary" -> readFineSummary(arguments, context);
                case "read_repeat_opponents" -> readRepeatOpponents(context);
                case "read_team_directory" -> readTeamDirectory(arguments, context);
                case "read_official_football" -> readOfficialFootball(arguments, context);
                case "read_achievements" -> readAchievements(arguments, context);
                case "read_steps" -> readSteps(arguments, context);
                case "read_visited_countries" -> readVisitedCountries(arguments, context);
                case "read_player_profile" -> readPlayerProfile(arguments, context);
                case "read_person_facts" -> personFactService.readRandomFacts(
                        requiredText(arguments, "person"),
                        context
                );
                case "search_interviews" -> personFactService.searchInterviewAnswers(
                        nullableText(arguments, "person"),
                        requiredText(arguments, "topic"),
                        textList(arguments, "keywords", 8),
                        clamp(arguments.path("limit").asInt(3), 1, 20),
                        context
                );
                case "read_app_navigation" -> readAppNavigation(arguments);
                default -> Map.of("error", "Neznámý read-only nástroj: " + toolName);
            };
            return serializeAndLimit(result);
        } catch (DataAccessException exception) {
            // Databázová chyba musí opustit transakci, jinak se projeví až jako
            // zavádějící UnexpectedRollbackException bez původní příčiny.
            throw exception;
        } catch (RuntimeException exception) {
            return serializeAndLimit(Map.of(
                    "error", "Nástroj nemohl data načíst.",
                    "detail", safeMessage(exception)
            ));
        }
    }

    private Object findMatches(JsonNode arguments, AiToolContext context) {
        Date fromDate = startOfDay(nullableDate(arguments, "from_date"));
        LocalDate inclusiveTo = nullableDate(arguments, "to_date");
        Date toDate = inclusiveTo == null ? null : startOfDay(inclusiveTo.plusDays(1));
        Long seasonId = nullableLong(arguments, "season_id");
        String opponent = nullableText(arguments, "opponent");
        int limit = clamp(arguments.path("limit").asInt(10), 1, 20);

        Specification<MatchEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("appTeam").get("id"), context.appTeam().getId()));
            if (seasonId != null && seasonId != com.jumbo.trus.config.Config.ALL_SEASON_ID) {
                predicates.add(builder.equal(root.get("season").get("id"), seasonId));
            }
            if (fromDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("date"), fromDate));
            }
            if (toDate != null) {
                predicates.add(builder.lessThan(root.get("date"), toDate));
            }
            if (opponent != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("name")),
                        "%" + opponent.toLowerCase(Locale.ROOT) + "%"
                ));
            }
            query.distinct(true);
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        int fetchLimit = Math.min(60, limit * 3);
        List<MatchEntity> manualMatches = matchRepository.findAll(
                specification,
                PageRequest.of(0, fetchLimit, Sort.by(Sort.Direction.DESC, "date"))
        ).getContent();

        TeamEntity officialTeam = context.appTeam().getTeam();
        Date officialFromDate = fromDate;
        Date officialToDate = toDate;
        if (seasonId != null && seasonId != com.jumbo.trus.config.Config.ALL_SEASON_ID) {
            SeasonDTO selectedSeason = seasonService.getSeason(seasonId);
            if (officialFromDate == null) {
                officialFromDate = selectedSeason.getFromDate();
            }
            if (officialToDate == null && selectedSeason.getToDate() != null) {
                LocalDate seasonEnd = selectedSeason.getToDate().toInstant().atZone(APP_ZONE).toLocalDate();
                officialToDate = startOfDay(seasonEnd.plusDays(1));
            }
        }
        Date finalOfficialFromDate = officialFromDate;
        Date finalOfficialToDate = officialToDate;
        List<FootballMatchEntity> officialMatches = officialTeam == null || officialTeam.getId() == null
                ? List.of()
                : footballMatchRepository.findAll(
                        officialMatchSpecification(
                                officialTeam,
                                finalOfficialFromDate,
                                finalOfficialToDate,
                                opponent,
                                null,
                                false
                        ),
                        PageRequest.of(0, fetchLimit, Sort.by(Sort.Direction.DESC, "date"))
                ).getContent();

        return combineMatches(officialMatches, manualMatches, officialTeam, limit);
    }

    private List<Map<String, Object>> combineMatches(
            List<FootballMatchEntity> officialMatches,
            List<MatchEntity> manualMatches,
            TeamEntity officialTeam,
            int limit
    ) {
        List<CombinedMatchRow> combined = new ArrayList<>();
        Set<Long> officialIds = new HashSet<>();
        Set<String> naturalKeys = new HashSet<>();
        Long officialTeamId = officialTeam == null ? null : officialTeam.getId();

        for (FootballMatchEntity match : officialMatches) {
            TeamEntity opponent = opponent(match, officialTeamId);
            String opponentName = opponent == null ? null : opponent.getName();
            officialIds.add(match.getId());
            naturalKeys.add(naturalMatchKey(opponentName, match.getDate()));
            Map<String, Object> row = officialMatchRow(match, officialTeamId, false);
            row.put("data_source", "official_import");
            combined.add(new CombinedMatchRow(match.getDate(), row));
        }

        for (MatchEntity match : manualMatches) {
            Long linkedOfficialId = match.getFootballMatch() == null ? null : match.getFootballMatch().getId();
            String naturalKey = naturalMatchKey(match.getName(), match.getDate());
            if ((linkedOfficialId != null && officialIds.contains(linkedOfficialId))
                    || naturalKeys.contains(naturalKey)) {
                continue;
            }
            naturalKeys.add(naturalKey);
            combined.add(new CombinedMatchRow(match.getDate(), manualMatchRow(match)));
        }

        return combined.stream()
                .sorted(Comparator.comparing(
                        CombinedMatchRow::date,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(limit)
                .map(CombinedMatchRow::row)
                .toList();
    }

    private Map<String, Object> manualMatchRow(MatchEntity match) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", match.getId());
        result.put("data_source", "manual_app");
        result.put("official_match_id", match.getFootballMatch() == null ? null : match.getFootballMatch().getId());
        result.put("opponent", match.getName());
        result.put("date", formatDateTime(match.getDate()));
        result.put("season_id", match.getSeason() == null ? null : match.getSeason().getId());
        result.put("home", match.isHome());
        result.put("home_goals", match.getHomeGoalNumber());
        result.put("away_goals", match.getAwayGoalNumber());
        result.put("weather", matchWeatherRow(match.getWeather()));
        result.put("participants", Optional.ofNullable(match.getPlayerList()).orElseGet(List::of)
                .stream()
                .map(player -> Map.of(
                        "id", player.getId(),
                        "name", player.getName(),
                        "fan", player.isFan()
                )).toList());
        return result;
    }

    private String naturalMatchKey(String opponent, Date date) {
        String normalizedOpponent = opponent == null
                ? ""
                : opponent.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String localDate = date == null
                ? ""
                : date.toInstant().atZone(APP_ZONE).toLocalDate().toString();
        return normalizedOpponent + "|" + localDate;
    }

    private Object findOfficialMatches(JsonNode arguments, AiToolContext context) {
        TeamEntity team = context.appTeam().getTeam();
        if (team == null || team.getId() == null) {
            return Map.of("error", "Aktuální tým není propojený s importovaným fotbalovým týmem.");
        }

        Long teamId = team.getId();
        Date fromDate = startOfDay(nullableDate(arguments, "from_date"));
        LocalDate inclusiveTo = nullableDate(arguments, "to_date");
        Date toDate = inclusiveTo == null ? null : startOfDay(inclusiveTo.plusDays(1));
        String opponent = nullableText(arguments, "opponent");
        Boolean playedOnly = nullableBoolean(arguments, "played_only");
        boolean currentLeagueOnly = arguments.path("current_league_only").asBoolean(false);
        boolean includePlayers = arguments.path("include_players").asBoolean(false);
        int limit = clamp(arguments.path("limit").asInt(20), 1, 30);

        Specification<FootballMatchEntity> specification = officialMatchSpecification(
                team,
                fromDate,
                toDate,
                opponent,
                playedOnly,
                currentLeagueOnly
        );

        List<FootballMatchEntity> matches = footballMatchRepository.findAll(
                specification,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "date"))
        ).getContent();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data_source", "official_import");
        result.put("matches", matches.stream()
                .map(match -> officialMatchRow(match, teamId, includePlayers))
                .toList());
        return result;
    }

    private Specification<FootballMatchEntity> officialMatchSpecification(
            TeamEntity team,
            Date fromDate,
            Date toDate,
            String opponent,
            Boolean playedOnly,
            boolean currentLeagueOnly
    ) {
        Long teamId = team.getId();
        return (root, query, builder) -> {
            Predicate isHomeTeam = builder.equal(root.get("homeTeam").get("id"), teamId);
            Predicate isAwayTeam = builder.equal(root.get("awayTeam").get("id"), teamId);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.or(isHomeTeam, isAwayTeam));
            if (fromDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<Date>get("date"), fromDate));
            }
            if (toDate != null) {
                predicates.add(builder.lessThan(root.<Date>get("date"), toDate));
            }
            if (playedOnly != null) {
                predicates.add(builder.equal(root.get("alreadyPlayed"), playedOnly));
            }
            if (currentLeagueOnly && team.getCurrentLeague() != null) {
                predicates.add(builder.equal(root.get("league").get("id"), team.getCurrentLeague().getId()));
            }
            if (opponent != null) {
                String pattern = "%" + opponent.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.and(isHomeTeam, builder.like(builder.lower(root.get("awayTeam").get("name")), pattern)),
                        builder.and(isAwayTeam, builder.like(builder.lower(root.get("homeTeam").get("name")), pattern))
                ));
            }
            query.distinct(true);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<String, Object> officialMatchRow(
            FootballMatchEntity match,
            Long currentTeamId,
            boolean includePlayers
    ) {
        boolean home = match.getHomeTeam() != null
                && Objects.equals(match.getHomeTeam().getId(), currentTeamId);
        TeamEntity opponent = home ? match.getAwayTeam() : match.getHomeTeam();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", match.getId());
        row.put("date", formatDateTime(match.getDate()));
        row.put("opponent_id", opponent == null ? null : opponent.getId());
        row.put("opponent", opponent == null ? null : opponent.getName());
        row.put("home", home);
        row.put("home_team", teamRow(match.getHomeTeam()));
        row.put("away_team", teamRow(match.getAwayTeam()));
        row.put("home_goals", match.getHomeGoalNumber());
        row.put("away_goals", match.getAwayGoalNumber());
        row.put("already_played", match.isAlreadyPlayed());
        row.put("round", match.getRound());
        row.put("league", leagueRow(match.getLeague()));
        row.put("stadium", match.getStadium());
        row.put("referee", match.getReferee());
        row.put("referee_comment", match.getRefereeComment());
        row.put("result_url", match.getUrlResult());
        if (includePlayers) {
            row.put("players", Optional.ofNullable(match.getPlayerList()).orElseGet(Set::of)
                    .stream()
                    .map(this::officialPlayerRow)
                    .toList());
        }
        return row;
    }

    private Map<String, Object> matchWeatherRow(com.jumbo.trus.entity.MatchWeatherEntity weather) {
        if (weather == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("temperature", weather.getTemperature());
        row.put("apparent_temperature", weather.getApparentTemperature());
        row.put("relative_humidity", weather.getRelativeHumidity());
        row.put("precipitation", weather.getPrecipitation());
        row.put("rain", weather.getRain());
        row.put("snowfall", weather.getSnowfall());
        row.put("weather_code", weather.getWeatherCode());
        row.put("cloud_cover", weather.getCloudCover());
        row.put("wind_speed", weather.getWindSpeed());
        row.put("wind_gusts", weather.getWindGusts());
        row.put("source", weather.getSourceType());
        return row;
    }

    private Map<String, Object> officialPlayerRow(FootballMatchPlayerEntity player) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("player_id", player.getPlayer() == null ? null : player.getPlayer().getId());
        row.put("player", player.getPlayer() == null ? null : player.getPlayer().getName());
        row.put("team_id", player.getTeam() == null ? null : player.getTeam().getId());
        row.put("team", player.getTeam() == null ? null : player.getTeam().getName());
        row.put("goals", player.getGoals());
        row.put("received_goals", player.getReceivedGoals());
        row.put("own_goals", player.getOwnGoals());
        row.put("yellow_cards", player.getYellowCards());
        row.put("red_cards", player.getRedCards());
        row.put("best_player", player.isBestPlayer());
        row.put("clean_sheet", player.isCleanSheet());
        return row;
    }

    private Object readTeamStatistics(JsonNode arguments, AiToolContext context) {
        String category = requiredText(arguments, "category");
        String aggregation = requiredText(arguments, "aggregation");

        StatisticsFilter filter = new StatisticsFilter();
        filter.setAppTeam(context.appTeam());
        filter.setSeasonId(nullableLong(arguments, "season_id"));
        filter.setPlayerId(nullableLong(arguments, "player_id"));
        filter.setMatchId(nullableLong(arguments, "match_id"));
        filter.setStringFilter(nullableText(arguments, "search"));
        filter.setMatchStatsOrPlayerStats("matches".equals(aggregation));
        filter.setDetailed(false);
        filter.setSplitPlayerFinesByFine(arguments.path("split_fines_by_type").asBoolean(false));
        filter.setLimit(500);

        return switch (category) {
            case "goals" -> goalService.getAllDetailed(filter);
            case "drinks" -> beerService.getAllDetailed(filter);
            case "fines" -> receivedFineService.getAllDetailed(filter);
            case "attendance" -> attendanceService.getAllDetailed(filter);
            default -> Map.of("error", "Neznámá kategorie statistik: " + category);
        };
    }

    private Object readFineSummary(JsonNode arguments, AiToolContext context) {
        String fineName = requiredText(arguments, "fine_name");
        List<AiFineSummaryProjection> rows = receivedFineRepository.findAiFineSummaryByName(
                context.appTeam().getId(),
                fineName
        );

        SeasonFilter seasonFilter = new SeasonFilter(false, false, false);
        seasonFilter.setAppTeam(context.appTeam());
        List<SeasonDTO> seasons = seasonService.getAll(seasonFilter);
        SeasonWindow seasonWindow = findSeasonWindow(seasons, new Date());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", fineName);
        result.put("matched_fine_count", rows.stream().map(AiFineSummaryProjection::getFineId).distinct().count());
        result.put("all_time", aggregateFineRows(rows));
        result.put("current_season", seasonSummary(seasonWindow.current(), rows));
        result.put("previous_season", seasonSummary(seasonWindow.previous(), rows));

        Map<Long, List<AiFineSummaryProjection>> rowsByFine = new LinkedHashMap<>();
        for (AiFineSummaryProjection row : rows) {
            rowsByFine.computeIfAbsent(row.getFineId(), ignored -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> matchedFines = new ArrayList<>();
        for (List<AiFineSummaryProjection> fineRows : rowsByFine.values()) {
            AiFineSummaryProjection firstRow = fineRows.get(0);
            Map<String, Object> fine = new LinkedHashMap<>();
            fine.put("fine_id", firstRow.getFineId());
            fine.put("fine_name", firstRow.getFineName());
            fine.put("exact_name_match", firstRow.getFineName().equalsIgnoreCase(fineName));
            fine.put("all_time", aggregateFineRows(fineRows));
            fine.put("current_season", seasonSummary(seasonWindow.current(), fineRows));
            fine.put("previous_season", seasonSummary(seasonWindow.previous(), fineRows));
            fine.put("by_season", fineRows.stream().map(this::fineSeasonRow).toList());
            matchedFines.add(fine);
        }
        result.put("matched_fines", matchedFines);
        if (rows.isEmpty()) {
            result.put("message", "Nebyla nalezena žádná udělená pokuta odpovídající názvu.");
        }
        return result;
    }

    private Object readRepeatOpponents(AiToolContext context) {
        TeamEntity officialTeam = context.appTeam().getTeam();
        if (officialTeam != null && officialTeam.getId() != null && officialTeam.getCurrentLeague() != null) {
            return readOfficialRepeatOpponents(officialTeam);
        }

        SeasonFilter seasonFilter = new SeasonFilter(false, false, false);
        seasonFilter.setAppTeam(context.appTeam());
        SeasonWindow seasonWindow = findSeasonWindow(seasonService.getAll(seasonFilter), new Date());
        SeasonDTO currentSeason = seasonWindow.current();

        if (currentSeason == null) {
            return Map.of(
                    "repeat_opponents", List.of(),
                    "message", "Tým nyní nemá sezonu odpovídající dnešnímu datu."
            );
        }

        List<AiRepeatOpponentProjection> opponents = matchRepository.findAiRepeatOpponents(
                context.appTeam().getId(),
                currentSeason.getId(),
                currentSeason.getFromDate()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current_season", seasonDirectoryRow(currentSeason));
        result.put("repeat_opponent_count", opponents.size());
        result.put("repeat_opponents", opponents.stream().map(this::repeatOpponentRow).toList());
        return result;
    }

    private Object readOfficialRepeatOpponents(TeamEntity team) {
        LeagueEntity currentLeague = team.getCurrentLeague();
        List<FootballMatchEntity> currentMatches = footballMatchRepository.findAiTeamMatchesInLeague(
                team.getId(),
                currentLeague.getId()
        );
        List<FootballMatchEntity> historicalMatches = footballMatchRepository.findAiPlayedTeamMatchesOutsideLeague(
                team.getId(),
                currentLeague.getId()
        );

        Map<Long, List<FootballMatchEntity>> currentByOpponent = matchesByOpponent(currentMatches, team.getId());
        Map<Long, List<FootballMatchEntity>> historyByOpponent = matchesByOpponent(historicalMatches, team.getId());
        List<Map<String, Object>> repeated = new ArrayList<>();
        for (Map.Entry<Long, List<FootballMatchEntity>> entry : currentByOpponent.entrySet()) {
            List<FootballMatchEntity> history = historyByOpponent.get(entry.getKey());
            if (history == null || history.isEmpty()) {
                continue;
            }
            List<FootballMatchEntity> current = entry.getValue();
            TeamEntity opponent = opponent(current.get(0), team.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("opponent_id", opponent.getId());
            row.put("opponent", opponent.getName());
            row.put("current_season_match_count", current.size());
            row.put("first_current_season_match", current.stream()
                    .map(FootballMatchEntity::getDate)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .map(this::formatDateTime)
                    .orElse(null));
            row.put("historical_match_count", history.size());
            row.put("last_historical_match", history.stream()
                    .map(FootballMatchEntity::getDate)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .map(this::formatDateTime)
                    .orElse(null));
            repeated.add(row);
        }
        repeated.sort(Comparator.comparing(row -> String.valueOf(row.get("opponent"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data_source", "official_import");
        result.put("current_league", leagueRow(currentLeague));
        result.put("repeat_opponent_count", repeated.size());
        result.put("repeat_opponents", repeated);
        return result;
    }

    private Map<Long, List<FootballMatchEntity>> matchesByOpponent(
            List<FootballMatchEntity> matches,
            Long teamId
    ) {
        Map<Long, List<FootballMatchEntity>> result = new LinkedHashMap<>();
        for (FootballMatchEntity match : matches) {
            TeamEntity opponent = opponent(match, teamId);
            if (opponent != null && opponent.getId() != null) {
                result.computeIfAbsent(opponent.getId(), ignored -> new ArrayList<>()).add(match);
            }
        }
        return result;
    }

    private TeamEntity opponent(FootballMatchEntity match, Long teamId) {
        if (match.getHomeTeam() != null && Objects.equals(match.getHomeTeam().getId(), teamId)) {
            return match.getAwayTeam();
        }
        if (match.getAwayTeam() != null && Objects.equals(match.getAwayTeam().getId(), teamId)) {
            return match.getHomeTeam();
        }
        return null;
    }

    private Map<String, Object> teamRow(TeamEntity team) {
        if (team == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", team.getId());
        row.put("name", team.getName());
        return row;
    }

    private Map<String, Object> leagueRow(LeagueEntity league) {
        if (league == null) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", league.getId());
        row.put("name", league.getName());
        row.put("year", league.getYear());
        row.put("organization", league.getOrganization());
        row.put("current", league.isCurrentLeague());
        return row;
    }

    private Map<String, Object> repeatOpponentRow(AiRepeatOpponentProjection opponent) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("opponent", opponent.getOpponent());
        row.put("current_season_match_count", nullableLong(opponent.getCurrentSeasonMatchCount()));
        row.put("first_current_season_match", formatDateTime(opponent.getFirstCurrentSeasonMatch()));
        row.put("historical_match_count", nullableLong(opponent.getHistoricalMatchCount()));
        row.put("last_historical_match", formatDateTime(opponent.getLastHistoricalMatch()));
        return row;
    }

    private SeasonWindow findSeasonWindow(List<SeasonDTO> seasons, Date now) {
        for (int index = 0; index < seasons.size(); index++) {
            SeasonDTO season = seasons.get(index);
            if (!season.getFromDate().after(now) && !season.getToDate().before(now)) {
                SeasonDTO previous = index + 1 < seasons.size() ? seasons.get(index + 1) : null;
                return new SeasonWindow(season, previous);
            }
        }

        SeasonDTO latestCompleted = seasons.stream()
                .filter(season -> season.getToDate().before(now))
                .findFirst()
                .orElse(null);
        return new SeasonWindow(null, latestCompleted);
    }

    private Map<String, Object> aggregateFineRows(List<AiFineSummaryProjection> rows) {
        Map<String, Object> aggregate = new LinkedHashMap<>();
        aggregate.put("fine_count", rows.stream().mapToLong(row -> nullableLong(row.getFineCount())).sum());
        aggregate.put("total_amount", rows.stream().mapToLong(row -> nullableLong(row.getTotalAmount())).sum());
        return aggregate;
    }

    private Map<String, Object> seasonSummary(SeasonDTO season, List<AiFineSummaryProjection> rows) {
        if (season == null) {
            return null;
        }
        List<AiFineSummaryProjection> seasonRows = rows.stream()
                .filter(row -> Objects.equals(row.getSeasonId(), season.getId()))
                .toList();
        Map<String, Object> result = seasonDirectoryRow(season);
        result.putAll(aggregateFineRows(seasonRows));
        return result;
    }

    private Map<String, Object> fineSeasonRow(AiFineSummaryProjection row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.getSeasonId());
        result.put("name", row.getSeasonName());
        result.put("from", formatDateTime(row.getSeasonFrom()));
        result.put("to", formatDateTime(row.getSeasonTo()));
        result.put("fine_count", nullableLong(row.getFineCount()));
        result.put("total_amount", nullableLong(row.getTotalAmount()));
        return result;
    }

    private long nullableLong(Long value) {
        return value == null ? 0 : value;
    }

    private Object readTeamDirectory(JsonNode arguments, AiToolContext context) {
        return switch (requiredText(arguments, "dataset")) {
            case "players" -> playerService.getAll(context.appTeam().getId())
                    .stream()
                    .map(this::playerDirectoryRow)
                    .toList();
            case "seasons" -> {
                SeasonFilter filter = new SeasonFilter(true, true, false);
                filter.setAppTeam(context.appTeam());
                filter.setLimit(100);
                yield seasonService.getAll(filter)
                        .stream()
                        .map(this::seasonDirectoryRow)
                        .toList();
            }
            default -> Map.of("error", "Neznámý týmový dataset");
        };
    }

    private Object readOfficialFootball(JsonNode arguments, AiToolContext context) {
        String dataset = requiredText(arguments, "dataset");
        boolean currentSeason = arguments.path("current_season").asBoolean(true);
        return switch (dataset) {
            case "leagues" -> readOfficialLeagues(context);
            case "table" -> teamService.getTable(context.appTeam().getTeam().getId());
            case "fixtures" -> footballMatchService.getNextMatches(context.appTeam());
            case "next_and_last" -> footballMatchService.getNextAndLastFootballMatchDetail(context.appTeam());
            case "player_stats" -> footballPlayerStatsService.getPlayerStatsForTeam(currentSeason, context.appTeam());
            default -> Map.of("error", "Neznámý fotbalový dataset: " + dataset);
        };
    }

    private Object readAchievements(JsonNode arguments, AiToolContext context) {
        String dataset = requiredText(arguments, "dataset");
        Long playerId = nullableLong(arguments, "player_id");
        String search = nullableText(arguments, "search");
        Boolean accomplishedOnly = nullableBoolean(arguments, "accomplished_only");
        int limit = clamp(arguments.path("limit").asInt(30), 1, 100);

        return switch (dataset) {
            case "catalog" -> achievementService.getAllDetailedAchievements(context.appTeam().getId())
                    .stream()
                    .filter(detail -> matchesAchievementSearch(detail.getAchievement(), search))
                    .limit(limit)
                    .map(this::achievementDetailRow)
                    .toList();
            case "player" -> readPlayerAchievements(
                    playerId == null ? context.currentPlayerId() : playerId,
                    search,
                    accomplishedOnly,
                    limit,
                    context
            );
            case "recent" -> {
                List<Long> playerIds = playerService.getAll(context.appTeam().getId())
                        .stream()
                        .map(PlayerDTO::getId)
                        .toList();
                if (playerIds.isEmpty()) {
                    yield List.of();
                }
                yield playerAchievementService.getLastPlayerAchievements(limit, playerIds)
                        .stream()
                        .filter(achievement -> matchesAchievementSearch(achievement.getAchievement(), search))
                        .map(this::playerAchievementRow)
                        .toList();
            }
            case "leaderboard" -> achievementLeaderboard(context, limit);
            default -> Map.of("error", "Neznámý achievementový dataset: " + dataset);
        };
    }

    private Object readSteps(JsonNode arguments, AiToolContext context) {
        String dataset = requiredText(arguments, "dataset");
        int limit = clamp(arguments.path("limit").asInt(30), 1, 366);
        return switch (dataset) {
            case "leaderboard" -> {
                String requestedPeriod = nullableText(arguments, "period");
                StepPeriod period = requestedPeriod == null
                        ? StepPeriod.TODAY
                        : StepPeriod.valueOf(requestedPeriod);
                yield stepService.getLeaderboardForTeam(period, context.appTeam());
            }
            case "me" -> stepService.getStepsForUser(
                            context.user().getId(),
                            nullableDate(arguments, "from_date"),
                            nullableDate(arguments, "to_date")
                    ).stream()
                    .limit(limit)
                    .toList();
            default -> Map.of("error", "Neznámý dataset kroků: " + dataset);
        };
    }

    private Object readVisitedCountries(JsonNode arguments, AiToolContext context) {
        String dataset = requiredText(arguments, "dataset");
        Long requestedPlayerId = nullableLong(arguments, "player_id");
        int limit = clamp(arguments.path("limit").asInt(50), 1, 100);

        if ("me".equals(dataset)) {
            List<VisitedCountryResponse> countries = userVisitedCountryService
                    .getVisitedCountryResponses(context.user().getId());
            return visitedCountrySummary(
                    context.currentPlayerId(),
                    context.currentPlayerName(),
                    countries.stream().map(this::visitedCountryRow).toList()
            );
        }

        Map<Long, String> teamPlayers = playerService.getAll(context.appTeam().getId()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PlayerDTO::getId,
                        PlayerDTO::getName,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        List<TeamVisitedCountryProjection> teamCountries = userVisitedCountryService
                .getTeamVisitedCountries(context.appTeam().getId());
        if ("player".equals(dataset)) {
            Long playerId = requestedPlayerId == null ? context.currentPlayerId() : requestedPlayerId;
            if (playerId == null || !teamPlayers.containsKey(playerId)) {
                return Map.of("error", "Hráč nebyl v aktuálním týmu nalezen.");
            }
            List<TeamVisitedCountryProjection> playerCountries = teamCountries.stream()
                    .filter(country -> Objects.equals(country.getPlayerId(), playerId))
                    .toList();
            String playerName = playerCountries.stream()
                    .map(TeamVisitedCountryProjection::getPlayerName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(teamPlayers.get(playerId));
            return visitedCountrySummary(
                    playerId,
                    playerName,
                    playerCountries.stream().map(this::visitedCountryRow).toList()
            );
        }
        if (!"team".equals(dataset)) {
            return Map.of("error", "Neznámý dataset navštívených zemí: " + dataset);
        }

        Map<Long, List<TeamVisitedCountryProjection>> byPlayer = new LinkedHashMap<>();
        for (Long playerId : teamPlayers.keySet()) {
            byPlayer.put(playerId, new ArrayList<>());
        }
        for (TeamVisitedCountryProjection country : teamCountries) {
            byPlayer.computeIfAbsent(country.getPlayerId(), ignored -> new ArrayList<>()).add(country);
        }
        return byPlayer.entrySet().stream()
                .map(entry -> {
                    List<TeamVisitedCountryProjection> countries = entry.getValue();
                    String playerName = countries.isEmpty()
                            ? teamPlayers.get(entry.getKey())
                            : countries.get(0).getPlayerName();
                    return visitedCountrySummary(
                            entry.getKey(),
                            playerName,
                            countries.stream().map(this::visitedCountryRow).toList()
                    );
                })
                .sorted(Comparator
                        .comparingLong((Map<String, Object> row) -> (long) row.get("country_count"))
                        .reversed()
                        .thenComparing(row -> String.valueOf(row.get("player"))))
                .limit(limit)
                .toList();
    }

    private Map<String, Object> visitedCountrySummary(
            Long playerId,
            String playerName,
            List<Map<String, Object>> countries
    ) {
        Map<String, Map<String, Object>> distinctCountries = new LinkedHashMap<>();
        for (Map<String, Object> country : countries) {
            distinctCountries.putIfAbsent(String.valueOf(country.get("code")), country);
        }
        List<Map<String, Object>> countryList = new ArrayList<>(distinctCountries.values());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("player_id", playerId);
        result.put("player", playerName);
        result.put("country_count", (long) countryList.size());
        result.put("foreign_country_count", countryList.stream()
                .filter(country -> !"CZ".equalsIgnoreCase(String.valueOf(country.get("code"))))
                .count());
        result.put("continent_count", countryList.stream()
                .map(country -> country.get("continent_code"))
                .filter(Objects::nonNull)
                .distinct()
                .count());
        result.put("countries", countryList);
        return result;
    }

    private Map<String, Object> visitedCountryRow(VisitedCountryResponse country) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", country.code());
        row.put("name", country.nameCs());
        row.put("continent_code", country.continentCode());
        row.put("first_visited_at", country.firstVisitedAt());
        return row;
    }

    private Map<String, Object> visitedCountryRow(TeamVisitedCountryProjection country) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", country.getCode());
        row.put("name", country.getNameCs());
        row.put("continent_code", country.getContinentCode());
        row.put("first_visited_at", country.getFirstVisitedAt());
        return row;
    }

    private Object readPlayerAchievements(
            Long playerId,
            String search,
            Boolean accomplishedOnly,
            int limit,
            AiToolContext context
    ) {
        if (playerId == null || !belongsToCurrentTeam(playerId, context)) {
            return Map.of("error", "Hráč nebyl v aktuálním týmu nalezen.");
        }
        AchievementPlayerDetail detail = achievementService.getAchievementsForPlayer(
                playerId,
                context.appTeam().getId()
        );
        List<PlayerAchievementDTO> achievements = new ArrayList<>();
        if (!Boolean.FALSE.equals(accomplishedOnly)) {
            achievements.addAll(detail.getAccomplishedPlayerAchievements());
        }
        if (!Boolean.TRUE.equals(accomplishedOnly)) {
            achievements.addAll(detail.getNotAccomplishedPlayerAchievements());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("player_id", playerId);
        result.put("total_count", detail.getTotalCount());
        result.put("success_rate", detail.getSuccessRate());
        result.put("achievements", achievements.stream()
                .filter(achievement -> matchesAchievementSearch(achievement.getAchievement(), search))
                .limit(limit)
                .map(this::playerAchievementRow)
                .toList());
        return result;
    }

    private Object achievementLeaderboard(AiToolContext context, int limit) {
        Map<Long, String> playerNames = playerService.getAll(context.appTeam().getId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        PlayerDTO::getId,
                        PlayerDTO::getName,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return playerAchievementService.getListOfPlayersOrderAccomplishedAchievements(
                        context.appTeam().getId(),
                        limit
                ).stream()
                .map(stats -> achievementLeaderboardRow(stats, playerNames))
                .toList();
    }

    private Map<String, Object> achievementLeaderboardRow(
            IPlayerAchievementStats stats,
            Map<Long, String> playerNames
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("player_id", stats.getPlayerId());
        row.put("player", playerNames.get(stats.getPlayerId()));
        row.put("accomplished_count", nullableLong(stats.getAccomplishedCount()));
        row.put("not_accomplished_count", nullableLong(stats.getNotAccomplishedCount()));
        return row;
    }

    private boolean matchesAchievementSearch(AchievementDTO achievement, String search) {
        if (search == null) {
            return true;
        }
        if (achievement == null) {
            return false;
        }
        String normalizedSearch = search.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(achievement.getName(), normalizedSearch)
                || containsIgnoreCase(achievement.getCode(), normalizedSearch)
                || containsIgnoreCase(achievement.getDescription(), normalizedSearch);
    }

    private boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearch);
    }

    private Map<String, Object> achievementDetailRow(AchievementDetail detail) {
        Map<String, Object> row = achievementRow(detail.getAchievement());
        row.put("total_count", detail.getTotalCount());
        row.put("accomplished_count", detail.getAccomplishedCount());
        row.put("success_rate", detail.getSuccessRate());
        row.put("accomplished_players", detail.getAccomplishedPlayers());
        return row;
    }

    private Map<String, Object> achievementRow(AchievementDTO achievement) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (achievement == null) {
            return row;
        }
        row.put("id", achievement.getId());
        row.put("name", achievement.getName());
        row.put("code", achievement.getCode());
        row.put("description", achievement.getDescription());
        row.put("secondary_condition", achievement.getSecondaryCondition());
        row.put("only_for_players", achievement.isOnlyForPlayers());
        row.put("manual", achievement.isManually());
        row.put("calculation_scope", achievement.getCalculationScope());
        row.put("team_success_rate", achievement.getTeamSuccessRate());
        row.put("rarity", achievement.getRarity());
        return row;
    }

    private Map<String, Object> playerAchievementRow(PlayerAchievementDTO playerAchievement) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", playerAchievement.getId());
        row.put("achievement", achievementRow(playerAchievement.getAchievement()));
        row.put("player_id", playerAchievement.getPlayer() == null ? null : playerAchievement.getPlayer().getId());
        row.put("player", playerAchievement.getPlayer() == null ? null : playerAchievement.getPlayer().getName());
        row.put("accomplished", playerAchievement.getAccomplished());
        row.put("accomplished_date", formatDateTime(playerAchievement.getAccomplishedDate()));
        row.put("detail", playerAchievement.getDetail());
        row.put("season_id", playerAchievement.getSeasonId());
        row.put("match_id", playerAchievement.getMatch() == null ? null : playerAchievement.getMatch().getId());
        row.put("football_match_id", playerAchievement.getFootballMatch() == null
                ? null
                : playerAchievement.getFootballMatch().getId());
        return row;
    }

    private Object readOfficialLeagues(AiToolContext context) {
        TeamEntity team = context.appTeam().getTeam();
        if (team == null) {
            return Map.of("error", "Aktuální tým není propojený s importovaným fotbalovým týmem.");
        }
        Map<Long, LeagueEntity> leagues = new LinkedHashMap<>();
        if (team.getCurrentLeague() != null) {
            leagues.put(team.getCurrentLeague().getId(), team.getCurrentLeague());
        }
        for (LeagueEntity league : Optional.ofNullable(team.getLeagueList()).orElseGet(List::of)) {
            leagues.putIfAbsent(league.getId(), league);
        }
        return leagues.values().stream().map(this::leagueRow).toList();
    }

    private Object readPlayerProfile(JsonNode arguments, AiToolContext context) {
        Long playerId = nullableLong(arguments, "player_id");
        if (playerId == null) {
            playerId = context.currentPlayerId();
        }
        if (playerId == null || !belongsToCurrentTeam(playerId, context)) {
            return Map.of("error", "Hráč nebyl v aktuálním týmu nalezen.");
        }
        boolean currentSeason = arguments.path("current_season").asBoolean(true);
        return playerStatsFacade.setupPlayerStats(playerId, context.appTeam(), currentSeason);
    }

    private boolean belongsToCurrentTeam(Long playerId, AiToolContext context) {
        return playerService.getAll(context.appTeam().getId())
                .stream()
                .map(PlayerDTO::getId)
                .anyMatch(playerId::equals);
    }

    private Map<String, Object> playerDirectoryRow(PlayerDTO player) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", player.getId());
        row.put("name", player.getName());
        row.put("active", player.isActive());
        row.put("fan", player.isFan());
        if (player.getFootballPlayer() != null) {
            row.put("official_player_name", player.getFootballPlayer().getName());
        }
        return row;
    }

    private Map<String, Object> seasonDirectoryRow(SeasonDTO season) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", season.getId());
        row.put("name", season.getName());
        row.put("from", formatDateTime(season.getFromDate()));
        row.put("to", formatDateTime(season.getToDate()));
        return row;
    }

    private String serializeAndLimit(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() <= MAX_TOOL_OUTPUT_CHARS) {
                return json;
            }
            return objectMapper.writeValueAsString(Map.of(
                    "error", "Výsledek je příliš velký. Použij užší období, sezonu, hráče nebo zápas."
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nelze serializovat výsledek AI nástroje", exception);
        }
    }

    private LocalDate nullableDate(JsonNode arguments, String field) {
        String value = nullableText(arguments, field);
        return value == null ? null : LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private Date startOfDay(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(APP_ZONE).toInstant());
    }

    private String formatDateTime(Date value) {
        return value == null ? null : value.toInstant().atZone(APP_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private Long nullableLong(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private String nullableText(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    private Boolean nullableBoolean(JsonNode arguments, String field) {
        JsonNode value = arguments.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private String requiredText(JsonNode arguments, String field) {
        String value = nullableText(arguments, field);
        if (value == null) {
            throw new IllegalArgumentException("Chybí parametr " + field);
        }
        return value;
    }

    private List<String> textList(JsonNode arguments, String field, int maximumSize) {
        JsonNode value = arguments.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
                if (result.size() >= maximumSize) {
                    break;
                }
            }
        }
        return result;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private Object readAppNavigation(JsonNode arguments) {
        String query = requiredText(arguments, "query");
        int limit = clamp(arguments.path("limit").asInt(3), 1, 5);
        ArrayNode guide;
        try (InputStream stream = AiReadOnlyToolService.class
                .getResourceAsStream("/ai/trusbot-navigation-guide.json")) {
            if (stream == null) {
                throw new IllegalStateException("Navigační příručka TrusBota nebyla nalezena.");
            }
            guide = (ArrayNode) objectMapper.readTree(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Navigační příručku TrusBota nelze načíst.", exception);
        }

        String normalizedQuery = normalizeNavigationText(query);
        List<String> tokens = Arrays.stream(normalizedQuery.split("\\s+"))
                .filter(token -> token.length() >= 3)
                .distinct()
                .toList();
        List<NavigationMatch> matches = new ArrayList<>();
        for (JsonNode entry : guide) {
            String title = normalizeNavigationText(entry.path("title").asText());
            String searchable = normalizeNavigationText(entry.toString());
            int score = searchable.contains(normalizedQuery) ? 100 : 0;
            if (title.contains(normalizedQuery)) {
                score += 40;
            }
            for (String token : tokens) {
                if (title.contains(token)) {
                    score += 12;
                } else if (searchable.contains(token)) {
                    score += 3;
                }
            }
            if (score > 0) {
                matches.add(new NavigationMatch(score, entry));
            }
        }
        matches.sort(Comparator.comparingInt(NavigationMatch::score).reversed());
        List<JsonNode> selected = matches.stream()
                .limit(limit)
                .map(NavigationMatch::entry)
                .toList();
        return Map.of(
                "query", query,
                "matches", selected,
                "message", selected.isEmpty()
                        ? "V navigační příručce nebyl nalezen odpovídající postup."
                        : "Postup je z aktuální klientské příručky; respektuj uvedené podmínky dostupnosti."
        );
    }

    private String normalizeNavigationText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static final String TOOL_DEFINITIONS = """
            [
              {
                "type": "function",
                "name": "find_matches",
                "description": "Najde a zkombinuje zápasy aktuálního týmu: přednost mají kompletní importované oficiální zápasy, ručně zadané zápasy doplní chybějící záznamy a duplicity se odstraní. Použij pro minulé a předchozí zápasy i otázky s relativním nebo konkrétním datem.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "from_date": {"type": ["string", "null"], "description": "Počáteční datum včetně ve formátu YYYY-MM-DD."},
                    "to_date": {"type": ["string", "null"], "description": "Koncové datum včetně ve formátu YYYY-MM-DD."},
                    "season_id": {"type": ["integer", "null"]},
                    "opponent": {"type": ["string", "null"]},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 20}
                  },
                  "required": ["from_date", "to_date", "season_id", "opponent", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "find_official_matches",
                "description": "Čte importované oficiální zápasy aktuálního týmu z football_match a volitelně hráčské výkony z football_match_player. Použij pro ligovou historii, rozpis, výsledky a detaily oficiálních zápasů, zejména když ručně zadané sezony nebo zápasy nestačí.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "from_date": {"type": ["string", "null"], "description": "Počáteční datum včetně ve formátu YYYY-MM-DD."},
                    "to_date": {"type": ["string", "null"], "description": "Koncové datum včetně ve formátu YYYY-MM-DD."},
                    "opponent": {"type": ["string", "null"], "description": "Část názvu soupeře."},
                    "played_only": {"type": ["boolean", "null"], "description": "True jen odehrané, false jen neodehrané, null obojí."},
                    "current_league_only": {"type": "boolean"},
                    "include_players": {"type": "boolean", "description": "Zahrne góly, karty a další importované výkony hráčů."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 30}
                  },
                  "required": ["from_date", "to_date", "opponent", "played_only", "current_league_only", "include_players", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_team_statistics",
                "description": "Čte týmové statistiky. Pro dotaz na konkrétní datum nejprve zjisti match_id pomocí find_matches.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "category": {"type": "string", "enum": ["goals", "drinks", "fines", "attendance"]},
                    "aggregation": {"type": "string", "enum": ["players", "matches"]},
                    "season_id": {"type": ["integer", "null"], "description": "Null znamená všechny sezony."},
                    "player_id": {"type": ["integer", "null"]},
                    "match_id": {"type": ["integer", "null"]},
                    "search": {"type": ["string", "null"], "description": "Část jména hráče nebo soupeře podle agregace."},
                    "split_fines_by_type": {"type": "boolean"}
                  },
                  "required": ["category", "aggregation", "season_id", "player_id", "match_id", "search", "split_fines_by_type"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_fine_summary",
                "description": "Jedním krokem vrátí přesný počet udělených pokut podle názvu, celkem, v aktuální a minulé sezoně i po sezonách. Použij vždy pro dotazy typu kolikrát byla udělena pokuta Překop v minulé sezoně a celkem; nepoužívej pro ně obecné read_team_statistics.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "fine_name": {"type": "string", "description": "Celý název pokuty nebo jeho část, například Překop."}
                  },
                  "required": ["fine_name"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_repeat_opponents",
                "description": "Jedním krokem vrátí soupeře z aktuální sezony, se kterými tým hrál už před začátkem této sezony, včetně počtu a data dřívějších zápasů. Použij pro dotazy typu zda Trus letos hraje s týmy, se kterými už někdy hrál.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {},
                  "required": [],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_team_directory",
                "description": "Vrátí ID a názvy hráčů nebo sezon aktuálního týmu.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["players", "seasons"]}
                  },
                  "required": ["dataset"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_official_football",
                "description": "Čte importované oficiální ligy týmu, ligovou tabulku, budoucí zápasy a oficiální hráčské statistiky.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["leagues", "table", "fixtures", "next_and_last", "player_stats"]},
                    "current_season": {"type": "boolean"}
                  },
                  "required": ["dataset", "current_season"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_achievements",
                "description": "Čte týmově omezené achievementy. Umí katalog s podmínkami a úspěšností, achievementy konkrétního hráče, poslední získané achievementy a týmový žebříček.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["catalog", "player", "recent", "leaderboard"]},
                    "player_id": {"type": ["integer", "null"], "description": "Pro dataset player; null znamená hráče aktuálního uživatele."},
                    "search": {"type": ["string", "null"], "description": "Část názvu, kódu nebo popisu achievementu."},
                    "accomplished_only": {"type": ["boolean", "null"], "description": "Pro hráče: true splněné, false nesplněné, null obojí."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 100}
                  },
                  "required": ["dataset", "player_id", "search", "accomplished_only", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_steps",
                "description": "Čte kroky. Dataset leaderboard vrací týmový žebříček pouze z uživatelů s aktivním souhlasem; dataset me vrací denní kroky aktuálního uživatele v zadaném období.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["leaderboard", "me"]},
                    "period": {"type": ["string", "null"], "enum": ["TODAY", "BETWEEN_MATCHES", "SINCE_LAST_MATCH", "ALL_TIME", null], "description": "Období pro leaderboard; null znamená dnešek."},
                    "from_date": {"type": ["string", "null"], "description": "Pro dataset me, počáteční datum včetně ve formátu YYYY-MM-DD."},
                    "to_date": {"type": ["string", "null"], "description": "Pro dataset me, koncové datum včetně ve formátu YYYY-MM-DD."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 366}
                  },
                  "required": ["dataset", "period", "from_date", "to_date", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_visited_countries",
                "description": "Čte navštívené země. Umí aktuálního uživatele, konkrétního hráče aktuálního týmu nebo týmový přehled seřazený podle počtu zemí.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["me", "player", "team"]},
                    "player_id": {"type": ["integer", "null"], "description": "Pro dataset player; null znamená hráče aktuálního uživatele."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 100, "description": "Maximální počet hráčů v týmovém přehledu."}
                  },
                  "required": ["dataset", "player_id", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_player_profile",
                "description": "Vrátí souhrn profilu a statistik hráče. Null player_id znamená hráče spárovaného s aktuálním uživatelem.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "player_id": {"type": ["integer", "null"]},
                    "current_season": {"type": "boolean"}
                  },
                  "required": ["player_id", "current_season"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_person_facts",
                "description": "Vyhledá hráče nebo fanouška výhradně v aktuálním app_team podle přezdívky z player, občanského jména z football_player nebo aliasu z týmového rozhovoru. Použij pro obecné dotazy typu řekni mi něco o osobě, co na ni říkáš nebo dej zajímavosti. Backend náhodně vybere přesně 2 databázové zajímavosti a u spárovaného rozhovoru další 2 odpovědi; osoba existující pouze v týmovém rozhovoru dostane 4 odpovědi. Vrať všechny vybrané položky přirozeně a rozhovory necenzuruj.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "person": {"type": "string", "description": "Samotná přezdívka, občanské jméno nebo známý alias hledané osoby."}
                  },
                  "required": ["person"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "search_interviews",
                "description": "Tematicky vyhledá původní otázky a odpovědi v rozhovorech dostupných aktuálnímu app_team. Použij pro konkrétní dotaz na názor, zkušenost nebo minulost osoby, například co si Jumbo myslí o zimě. Když person=null, vrátí nejrelevantnější odpověď za každého dostupného hráče a umožní otázky typu kteří hráči hráli v mládí fotbal. Odpovědi parafrázuj bez cenzury a nic si nevymýšlej.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "person": {"type": ["string", "null"], "description": "Přezdívka, občanské jméno nebo alias; null pro hledání napříč všemi rozhovory týmu."},
                    "topic": {"type": "string", "description": "Konkrétní věc, názor nebo zkušenost hledaná v rozhovoru, bez jména osoby."},
                    "keywords": {"type": "array", "items": {"type": "string"}, "maxItems": 8, "description": "Česká synonyma či pravděpodobné znění otázky v rozhovoru, například [fotbalové zkušenosti, před Trusem, mládí]."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 20, "description": "Pro jednu osobu obvykle 3; pro porovnání všech osob použij 20."}
                  },
                  "required": ["person", "topic", "keywords", "limit"],
                  "additionalProperties": false
                }
              },
              {
                "type": "function",
                "name": "read_app_navigation",
                "description": "Vyhledá v aktuální uživatelské příručce přesnou cestu obrazovkami, význam tlačítek, postup obsluhy a podmínky dostupnosti. Použij pro otázky typu jak něco v aplikaci přidat, upravit, otevřít, zapnout, najít nebo kam klepnout. Nečte týmová data a nic nemění.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "query": {"type": "string", "description": "Stručně formulovaný cíl nebo hledaný ovládací prvek v češtině."},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 5, "description": "Počet nejbližších částí příručky; obvykle 2 až 3."}
                  },
                  "required": ["query", "limit"],
                  "additionalProperties": false
                }
              }
            ]
            """;

    private record SeasonWindow(SeasonDTO current, SeasonDTO previous) {
    }

    private record CombinedMatchRow(Date date, Map<String, Object> row) {
    }

    private record NavigationMatch(int score, JsonNode entry) {
    }
}
