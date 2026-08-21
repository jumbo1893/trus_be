package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.match.FootballMatchService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.football.team.TeamService;
import com.jumbo.trus.service.goal.GoalService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                case "read_team_statistics" -> readTeamStatistics(arguments, context);
                case "read_team_directory" -> readTeamDirectory(arguments, context);
                case "read_official_football" -> readOfficialFootball(arguments, context);
                case "read_player_profile" -> readPlayerProfile(arguments, context);
                default -> Map.of("error", "Neznámý read-only nástroj: " + toolName);
            };
            return serializeAndLimit(result);
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

        List<MatchEntity> matches = matchRepository.findForAi(
                context.appTeam().getId(),
                seasonId,
                fromDate,
                toDate,
                opponent,
                PageRequest.of(0, limit)
        );

        return matches.stream().map(match -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", match.getId());
            result.put("opponent", match.getName());
            result.put("date", formatDateTime(match.getDate()));
            result.put("season_id", match.getSeason() == null ? null : match.getSeason().getId());
            result.put("home", match.isHome());
            result.put("home_goals", match.getHomeGoalNumber());
            result.put("away_goals", match.getAwayGoalNumber());
            result.put("participants", match.getPlayerList().stream().map(player -> Map.of(
                    "id", player.getId(),
                    "name", player.getName(),
                    "fan", player.isFan()
            )).toList());
            return result;
        }).toList();
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
            case "table" -> teamService.getTable(context.appTeam().getTeam().getId());
            case "fixtures" -> footballMatchService.getNextMatches(context.appTeam());
            case "next_and_last" -> footballMatchService.getNextAndLastFootballMatchDetail(context.appTeam());
            case "player_stats" -> footballPlayerStatsService.getPlayerStatsForTeam(currentSeason, context.appTeam());
            default -> Map.of("error", "Neznámý fotbalový dataset: " + dataset);
        };
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

    private String requiredText(JsonNode arguments, String field) {
        String value = nullableText(arguments, field);
        if (value == null) {
            throw new IllegalArgumentException("Chybí parametr " + field);
        }
        return value;
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

    private static final String TOOL_DEFINITIONS = """
            [
              {
                "type": "function",
                "name": "find_matches",
                "description": "Najde zápasy aktuálního týmu v zadaném období. Použij nejprve pro otázky s relativním nebo konkrétním datem a následně načti statistiky podle match_id.",
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
                "description": "Čte oficiální ligovou tabulku, budoucí zápasy a oficiální hráčské statistiky týmu.",
                "strict": true,
                "parameters": {
                  "type": "object",
                  "properties": {
                    "dataset": {"type": "string", "enum": ["table", "fixtures", "next_and_last", "player_stats"]},
                    "current_season": {"type": "boolean"}
                  },
                  "required": ["dataset", "current_season"],
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
              }
            ]
            """;
}
