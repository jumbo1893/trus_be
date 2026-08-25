package com.jumbo.trus.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.FineDTO;
import com.jumbo.trus.dto.attendance.AttendanceDetailedDTO;
import com.jumbo.trus.dto.attendance.AttendanceDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.football.FootballPlayerDTO;
import com.jumbo.trus.dto.football.stats.FootballAllIndividualStats;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.dto.player.stats.PlayerStats;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.AttendanceService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.football.stats.FootballPlayerStatsService;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.player.PlayerStatsFacade;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Service
public class TrusBotPersonFactService {

    private static final String INTERVIEWS_RESOURCE = "/ai/trusbot-interviews.json";
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Prague");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> INTERVIEW_SEARCH_STOP_WORDS = Set.of(
            "a", "aby", "ale", "by", "byl", "byla", "byli", "co", "do", "fanousek",
            "fanousci", "hrac", "hraci", "jak", "jaka", "jake", "jaky", "je", "jeho",
            "jejich", "jsou", "kdo", "ktera", "ktere", "kteri", "ktery", "mi", "mysli",
            "na", "nebo", "o", "od", "pro", "rekl", "rika", "rikal", "se", "si", "ten",
            "to", "tom", "tomu", "v", "ve", "z", "za", "ze"
    );

    private final PlayerService playerService;
    private final PlayerStatsFacade playerStatsFacade;
    private final AttendanceService attendanceService;
    private final BeerService beerService;
    private final ReceivedFineService receivedFineService;
    private final FootballPlayerStatsService footballPlayerStatsService;
    private final InterviewDocument interviewDocument;
    private final Random random;

    @Autowired
    public TrusBotPersonFactService(
            ObjectMapper objectMapper,
            PlayerService playerService,
            PlayerStatsFacade playerStatsFacade,
            AttendanceService attendanceService,
            BeerService beerService,
            ReceivedFineService receivedFineService,
            FootballPlayerStatsService footballPlayerStatsService
    ) {
        this(
                objectMapper,
                playerService,
                playerStatsFacade,
                attendanceService,
                beerService,
                receivedFineService,
                footballPlayerStatsService,
                new Random()
        );
    }

    TrusBotPersonFactService(
            ObjectMapper objectMapper,
            PlayerService playerService,
            PlayerStatsFacade playerStatsFacade,
            AttendanceService attendanceService,
            BeerService beerService,
            ReceivedFineService receivedFineService,
            FootballPlayerStatsService footballPlayerStatsService,
            Random random
    ) {
        this.playerService = playerService;
        this.playerStatsFacade = playerStatsFacade;
        this.attendanceService = attendanceService;
        this.beerService = beerService;
        this.receivedFineService = receivedFineService;
        this.footballPlayerStatsService = footballPlayerStatsService;
        this.interviewDocument = loadInterviews(objectMapper);
        this.random = random;
    }

    public Map<String, Object> readRandomFacts(String query, AiToolContext context) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) {
            return error("INVALID_QUERY", query, "Chybí jméno nebo přezdívka hledané osoby.", List.of());
        }

        List<PlayerDTO> teamPlayers = playerService.getAll(context.appTeam().getId());
        List<PersonCandidate> candidates = findCandidates(normalizedQuery, teamPlayers, context);
        if (candidates.isEmpty()) {
            return error(
                    "NOT_FOUND",
                    query,
                    "Hráč ani fanoušek nebyl v aktuálním týmu nalezen.",
                    List.of()
            );
        }

        int bestScore = candidates.stream().mapToInt(PersonCandidate::score).max().orElse(0);
        List<PersonCandidate> bestCandidates = candidates.stream()
                .filter(candidate -> candidate.score() == bestScore)
                .toList();
        if (bestCandidates.size() != 1) {
            return error(
                    "AMBIGUOUS",
                    query,
                    "Dotaz odpovídá více osobám v aktuálním týmu. Je potřeba uvést přesnější jméno.",
                    bestCandidates.stream().map(this::candidateIdentity).toList()
            );
        }

        PersonCandidate selected = bestCandidates.get(0);
        if (selected.player() == null) {
            return interviewOnlyResult(query, selected.interview());
        }
        return databasePersonResult(query, selected.player(), selected.interview(), context);
    }

    public Map<String, Object> searchInterviewAnswers(
            String person,
            String topic,
            List<String> keywords,
            int requestedLimit,
            AiToolContext context
    ) {
        String normalizedTopic = normalize(String.join(
                " ",
                topic == null ? "" : topic,
                String.join(" ", safeList(keywords))
        ));
        if (normalizedTopic.isBlank()) {
            return error("INVALID_QUERY", topic, "Chybí téma hledané v rozhovorech.", List.of());
        }

        List<PlayerDTO> teamPlayers = playerService.getAll(context.appTeam().getId());
        List<InterviewDefinition> interviews;
        PersonCandidate selectedPerson = null;
        if (person != null && !person.isBlank()) {
            List<PersonCandidate> candidates = findCandidates(normalize(person), teamPlayers, context);
            if (candidates.isEmpty()) {
                return error(
                        "NOT_FOUND",
                        person,
                        "Hráč ani fanoušek nebyl v aktuálním týmu nalezen.",
                        List.of()
                );
            }
            int bestScore = candidates.stream().mapToInt(PersonCandidate::score).max().orElse(0);
            List<PersonCandidate> bestCandidates = candidates.stream()
                    .filter(candidate -> candidate.score() == bestScore)
                    .toList();
            if (bestCandidates.size() != 1) {
                return error(
                        "AMBIGUOUS",
                        person,
                        "Jméno odpovídá více osobám. Je potřeba ho upřesnit.",
                        bestCandidates.stream().map(this::candidateIdentity).toList()
                );
            }
            selectedPerson = bestCandidates.get(0);
            if (selectedPerson.interview() == null) {
                return error(
                        "NO_INTERVIEW",
                        person,
                        "Osoba je v aktuálním týmu, ale nemá dostupný rozhovor.",
                        List.of(candidateIdentity(selectedPerson))
                );
            }
            interviews = List.of(selectedPerson.interview());
        } else {
            interviews = eligibleInterviews(teamPlayers, context);
        }

        Set<String> searchTokens = interviewSearchTokens(normalizedTopic);
        if (selectedPerson != null) {
            for (String name : selectedPerson.player() == null
                    ? namesFor(selectedPerson.interview())
                    : namesFor(selectedPerson.player(), selectedPerson.interview())) {
                searchTokens.removeAll(interviewSearchTokens(normalize(name)));
            }
        }
        expandInterviewSearchTokens(searchTokens);
        if (searchTokens.isEmpty()) {
            return error("INVALID_QUERY", topic, "Po odebrání jména nezbylo žádné hledané téma.", List.of());
        }

        int limit = Math.max(1, Math.min(requestedLimit, 20));
        List<InterviewSearchMatch> matches = selectedPerson == null
                ? bestMatchPerInterview(interviews, teamPlayers, searchTokens, limit)
                : bestMatchesForInterview(interviews.get(0), teamPlayers, searchTokens, limit);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", matches.isEmpty() ? "NO_MATCH" : "FOUND");
        result.put("person_query", person);
        result.put("topic", topic);
        result.put("searched_interviews", interviews.size());
        result.put("matches", matches.stream().map(this::interviewSearchRow).toList());
        result.put("response_policy", Map.of(
                "paraphrase_answers", true,
                "censor_interview_language", false,
                "compare_all_returned_people", selectedPerson == null,
                "do_not_invent_missing_answers", true
        ));
        if (matches.isEmpty()) {
            result.put("message", "V dostupných rozhovorech nebyla nalezena dostatečně související odpověď.");
        }
        return result;
    }

    private Map<String, Object> databasePersonResult(
            String query,
            PlayerDTO player,
            InterviewDefinition interview,
            AiToolContext context
    ) {
        List<Map<String, Object>> databaseCandidates = databaseFacts(player, context);
        List<Map<String, Object>> selectedDatabaseFacts = randomSelection(databaseCandidates, 2);
        List<Map<String, Object>> selectedInterviewFacts = interview == null
                ? List.of()
                : interviewFacts(interview, 2);

        Map<String, Object> result = baseResult(query, playerIdentity(player, interview));
        result.put("database_facts", selectedDatabaseFacts);
        result.put("interview_facts", selectedInterviewFacts);
        result.put("response_policy", Map.of(
                "use_all_selected_database_facts", true,
                "use_all_selected_interview_facts", true,
                "paraphrase_interview_answers", true,
                "censor_interview_language", false
        ));
        return result;
    }

    private Map<String, Object> interviewOnlyResult(String query, InterviewDefinition interview) {
        Map<String, Object> result = baseResult(query, interviewIdentity(interview));
        result.put("database_facts", List.of());
        result.put("interview_facts", interviewFacts(interview, 4));
        result.put("response_policy", Map.of(
                "use_all_selected_database_facts", false,
                "use_all_selected_interview_facts", true,
                "paraphrase_interview_answers", true,
                "censor_interview_language", false
        ));
        return result;
    }

    private Map<String, Object> baseResult(String query, Map<String, Object> person) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "FOUND");
        result.put("query", query);
        result.put("person", person);
        return result;
    }

    private List<PersonCandidate> findCandidates(
            String normalizedQuery,
            List<PlayerDTO> teamPlayers,
            AiToolContext context
    ) {
        Map<Long, InterviewDefinition> linkedInterviews = new LinkedHashMap<>();
        for (InterviewDefinition interview : safeList(interviewDocument.interviews())) {
            Long playerId = interview.interviewee() == null ? null : interview.interviewee().playerId();
            if (playerId != null) {
                linkedInterviews.put(playerId, interview);
            }
        }

        List<PersonCandidate> candidates = new ArrayList<>();
        for (PlayerDTO player : safeList(teamPlayers)) {
            InterviewDefinition interview = linkedInterviews.get(player.getId());
            int score = bestScore(normalizedQuery, namesFor(player, interview));
            if (score > 0) {
                candidates.add(new PersonCandidate(player, interview, score));
            }
        }

        if (sameTeamName(context.appTeam().getName(), interviewDocument.appTeamName())) {
            for (InterviewDefinition interview : safeList(interviewDocument.interviews())) {
                Interviewee interviewee = interview.interviewee();
                if (interviewee == null
                        || interviewee.playerId() != null
                        || interviewee.fullName() == null) {
                    continue;
                }
                int score = bestScore(normalizedQuery, namesFor(interview));
                if (score > 0) {
                    candidates.add(new PersonCandidate(null, interview, score));
                }
            }
        }

        return candidates;
    }

    private List<InterviewDefinition> eligibleInterviews(
            List<PlayerDTO> teamPlayers,
            AiToolContext context
    ) {
        Set<Long> teamPlayerIds = safeList(teamPlayers).stream()
                .map(PlayerDTO::getId)
                .collect(java.util.stream.Collectors.toSet());
        boolean includeUnlinked = sameTeamName(
                context.appTeam().getName(),
                interviewDocument.appTeamName()
        );
        return safeList(interviewDocument.interviews()).stream()
                .filter(interview -> interview.interviewee() != null)
                .filter(interview -> {
                    Long playerId = interview.interviewee().playerId();
                    return playerId == null ? includeUnlinked : teamPlayerIds.contains(playerId);
                })
                .toList();
    }

    private Set<String> interviewSearchTokens(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalize(value).split(" ")) {
            if (token.length() >= 3 && !INTERVIEW_SEARCH_STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void expandInterviewSearchTokens(Set<String> tokens) {
        boolean youth = tokens.stream().anyMatch(token -> token.startsWith("mlad")
                || token.startsWith("detst")
                || token.startsWith("zacinal"));
        boolean played = tokens.stream().anyMatch(token -> token.startsWith("hral"));
        boolean football = tokens.stream().anyMatch(token -> token.startsWith("fotbal"));
        if (football && (youth || played)) {
            tokens.add("fotbal");
            tokens.add("zkusenosti");
            tokens.add("pred");
            tokens.add("trusem");
        }
    }

    private List<InterviewSearchMatch> bestMatchPerInterview(
            List<InterviewDefinition> interviews,
            List<PlayerDTO> teamPlayers,
            Set<String> searchTokens,
            int limit
    ) {
        return interviews.stream()
                .map(interview -> bestQuestion(interview, teamPlayers, searchTokens))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparingInt(InterviewSearchMatch::score)
                        .reversed()
                        .thenComparing(match -> match.interview().interviewee().fullName()))
                .limit(limit)
                .toList();
    }

    private List<InterviewSearchMatch> bestMatchesForInterview(
            InterviewDefinition interview,
            List<PlayerDTO> teamPlayers,
            Set<String> searchTokens,
            int limit
    ) {
        PlayerDTO player = playerForInterview(interview, teamPlayers);
        return safeList(interview.questions()).stream()
                .map(question -> new InterviewSearchMatch(
                        interview,
                        player,
                        question,
                        interviewQuestionScore(searchTokens, question)
                ))
                .filter(match -> match.score() >= 5)
                .sorted(Comparator.comparingInt(InterviewSearchMatch::score).reversed())
                .limit(limit)
                .toList();
    }

    private java.util.Optional<InterviewSearchMatch> bestQuestion(
            InterviewDefinition interview,
            List<PlayerDTO> teamPlayers,
            Set<String> searchTokens
    ) {
        return bestMatchesForInterview(interview, teamPlayers, searchTokens, 1)
                .stream()
                .findFirst();
    }

    private int interviewQuestionScore(Set<String> searchTokens, InterviewQuestion question) {
        Set<String> questionTokens = interviewSearchTokens(question.question());
        Set<String> answerTokens = interviewSearchTokens(question.answer());
        int score = 0;
        int covered = 0;
        for (String searchToken : searchTokens) {
            boolean exactQuestion = questionTokens.contains(searchToken);
            boolean relatedQuestion = exactQuestion || questionTokens.stream()
                    .anyMatch(token -> relatedToken(searchToken, token));
            boolean exactAnswer = answerTokens.contains(searchToken);
            boolean relatedAnswer = exactAnswer || answerTokens.stream()
                    .anyMatch(token -> relatedToken(searchToken, token));
            if (exactQuestion) {
                score += 9;
            } else if (relatedQuestion) {
                score += 7;
            }
            if (exactAnswer) {
                score += 4;
            } else if (relatedAnswer) {
                score += 2;
            }
            if (relatedQuestion || relatedAnswer) {
                covered++;
            }
        }
        if (covered == 0) {
            return 0;
        }
        return score + (covered * 10 / searchTokens.size());
    }

    private boolean relatedToken(String first, String second) {
        if (first.equals(second) || first.startsWith(second) || second.startsWith(first)) {
            return true;
        }
        int shorter = Math.min(first.length(), second.length());
        int commonPrefix = 0;
        while (commonPrefix < shorter && first.charAt(commonPrefix) == second.charAt(commonPrefix)) {
            commonPrefix++;
        }
        return shorter == 4 ? commonPrefix >= 3 : shorter > 4 && commonPrefix >= 4;
    }

    private PlayerDTO playerForInterview(
            InterviewDefinition interview,
            List<PlayerDTO> teamPlayers
    ) {
        Long playerId = interview.interviewee().playerId();
        if (playerId == null) {
            return null;
        }
        return teamPlayers.stream()
                .filter(player -> Objects.equals(player.getId(), playerId))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> interviewSearchRow(InterviewSearchMatch match) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("person", match.player() == null
                ? interviewIdentity(match.interview())
                : playerIdentity(match.player(), match.interview()));
        row.put("question_number", match.question().number());
        row.put("question", match.question().question());
        row.put("answer", match.question().answer());
        row.put("source_url", match.interview().sourceUrl());
        row.put("relevance_score", match.score());
        return row;
    }

    private List<String> namesFor(PlayerDTO player, InterviewDefinition interview) {
        Set<String> names = new LinkedHashSet<>();
        names.add(player.getName());
        FootballPlayerDTO footballPlayer = player.getFootballPlayer();
        if (footballPlayer != null) {
            names.add(footballPlayer.getName());
        }
        if (interview != null && interview.interviewee() != null) {
            names.add(interview.interviewee().fullName());
            names.addAll(safeList(interview.interviewee().aliases()));
        }
        return names.stream().filter(Objects::nonNull).toList();
    }

    private List<String> namesFor(InterviewDefinition interview) {
        Set<String> names = new LinkedHashSet<>();
        names.add(interview.interviewee().fullName());
        names.addAll(safeList(interview.interviewee().aliases()));
        return names.stream().filter(Objects::nonNull).toList();
    }

    private int bestScore(String query, List<String> names) {
        return names.stream().mapToInt(name -> nameScore(query, normalize(name))).max().orElse(0);
    }

    private int nameScore(String query, String candidate) {
        if (candidate.isBlank()) {
            return 0;
        }
        if (query.equals(candidate)) {
            return 100;
        }
        if (sameNameTokens(query, candidate)) {
            return 95;
        }
        if (candidate.length() >= 3 && containsPhrase(query, candidate)) {
            return 70;
        }
        if (query.length() >= 3 && containsPhrase(candidate, query)) {
            return 60;
        }
        return 0;
    }

    private boolean sameNameTokens(String first, String second) {
        List<String> firstTokens = sortedTokens(first);
        List<String> secondTokens = sortedTokens(second);
        return firstTokens.size() > 1 && firstTokens.equals(secondTokens);
    }

    private List<String> sortedTokens(String value) {
        return Arrays.stream(value.split(" "))
                .filter(token -> !token.isBlank())
                .sorted()
                .toList();
    }

    private boolean containsPhrase(String value, String phrase) {
        return (" " + value + " ").contains(" " + phrase + " ");
    }

    private List<Map<String, Object>> databaseFacts(PlayerDTO player, AiToolContext context) {
        List<Map<String, Object>> facts = new ArrayList<>();
        facts.add(birthdayFact(player));

        PlayerStats stats = playerStatsFacade.setupPlayerStats(player.getId(), context.appTeam(), false);
        facts.add(drinkFact(player, context, stats));
        facts.add(attendanceFact(player, context));
        if (stats != null && stats.getPlayerAchievementCount() != null) {
            Map<String, Object> achievement = fact("achievements", "Achievementy v aplikaci");
            achievement.put("accomplished", stats.getPlayerAchievementCount().getAccomplishedAchievements());
            achievement.put("available", stats.getPlayerAchievementCount().getTotalAchievements());
            facts.add(achievement);
        }

        if (!player.isFan()) {
            if (stats != null && stats.getPlayerGoalCount() != null) {
                Map<String, Object> goals = fact("manual_goals", "Góly a asistence v ručně vedené historii aplikace");
                goals.put("goals", stats.getPlayerGoalCount().getTotalGoals());
                goals.put("assists", stats.getPlayerGoalCount().getTotalAssists());
                facts.add(goals);
            }
            facts.add(fineFact(player, context));
            addOfficialFootballFact(facts, player, context);
        }

        return facts.stream().filter(Objects::nonNull).toList();
    }

    private Map<String, Object> birthdayFact(PlayerDTO player) {
        Map<String, Object> fact = fact("birthday", "Narozeniny");
        fact.put("date", formatDate(player.getBirthday()));
        return fact;
    }

    private Map<String, Object> drinkFact(PlayerDTO player, AiToolContext context, PlayerStats stats) {
        Map<String, Object> fact = fact("drinks", "Nápoje v ručně vedené historii aplikace");
        int beers = stats == null || stats.getPlayerBeerCount() == null
                ? 0
                : stats.getPlayerBeerCount().getTotalBeers();
        int liquors = stats == null || stats.getPlayerBeerCount() == null
                ? 0
                : stats.getPlayerBeerCount().getTotalLiquors();
        fact.put("beers", beers);
        fact.put("liquors", liquors);

        StatisticsFilter filter = allTimePlayerFilter(player.getId(), context);
        filter.setMatchStatsOrPlayerStats(null);
        BeerDetailedResponse details = beerService.getAllDetailed(filter);
        latestLiquor(details).ifPresent(lastLiquor -> {
            fact.put("last_liquor_match", lastLiquor.getMatch().getName());
            fact.put("last_liquor_match_date", formatDate(lastLiquor.getMatch().getDate()));
            fact.put("liquors_on_that_match", lastLiquor.getLiquorNumber());
        });
        return fact;
    }

    private java.util.Optional<BeerDetailedDTO> latestLiquor(BeerDetailedResponse details) {
        if (details == null || details.getBeerList() == null) {
            return java.util.Optional.empty();
        }
        return details.getBeerList().stream()
                .filter(beer -> beer.getLiquorNumber() > 0)
                .filter(beer -> beer.getMatch() != null && beer.getMatch().getDate() != null)
                .max(Comparator.comparing(beer -> beer.getMatch().getDate()));
    }

    private Map<String, Object> attendanceFact(PlayerDTO player, AiToolContext context) {
        AttendanceDetailedResponse attendance = attendanceService.getAllDetailed(
                allTimePlayerFilter(player.getId(), context)
        );
        Map<String, Object> fact = fact("attendance", "Docházka v ručně vedené historii aplikace");
        int matchesCount = attendance == null ? 0 : attendance.getMatchesCount();
        fact.put("matches", matchesCount);
        if (attendance != null && attendance.getAttendanceList() != null) {
            attendance.getAttendanceList().stream()
                    .map(AttendanceDetailedDTO::getMatch)
                    .filter(Objects::nonNull)
                    .filter(match -> match.getDate() != null)
                    .max(Comparator.comparing(MatchDTO::getDate))
                    .ifPresent(match -> {
                        fact.put("latest_match", match.getName());
                        fact.put("latest_match_date", formatDate(match.getDate()));
                    });
        }
        return fact;
    }

    private Map<String, Object> fineFact(PlayerDTO player, AiToolContext context) {
        StatisticsFilter filter = allTimePlayerFilter(player.getId(), context);
        filter.setMatchStatsOrPlayerStats(false);
        filter.setDetailed(false);
        filter.setSplitPlayerFinesByFine(true);
        ReceivedFineDetailedResponse fines = receivedFineService.getAllDetailed(filter);

        Map<String, Object> fact = fact("fines", "Pokuty v ručně vedené historii aplikace");
        fact.put("fine_count", fines == null ? 0 : fines.getFinesNumber());
        fact.put("total_amount", fines == null ? 0 : fines.getFinesAmount());
        if (fines != null && fines.getFineList() != null) {
            fines.getFineList().stream()
                    .filter(row -> row.getFine() != null)
                    .max(Comparator.comparingInt(ReceivedFineDetailedDTO::getFineNumber))
                    .ifPresent(row -> {
                        FineDTO fine = row.getFine();
                        fact.put("most_common_fine", fine.getName());
                        fact.put("most_common_fine_count", row.getFineNumber());
                    });
        }
        return fact;
    }

    private void addOfficialFootballFact(
            List<Map<String, Object>> facts,
            PlayerDTO player,
            AiToolContext context
    ) {
        FootballPlayerDTO footballPlayer = player.getFootballPlayer();
        if (footballPlayer == null || context.appTeam().getTeam() == null) {
            return;
        }
        try {
            FootballAllIndividualStats stats = footballPlayerStatsService.getPlayerStatsForPlayer(
                    footballPlayer.getId(),
                    context.appTeam()
            );
            if (stats == null) {
                return;
            }
            Map<String, Object> fact = fact("official_football", "Kompletní importované soutěžní statistiky");
            fact.put("matches", stats.getMatches());
            fact.put("goals", stats.getGoals());
            fact.put("yellow_cards", stats.getYellowCards());
            fact.put("red_cards", stats.getRedCards());
            fact.put("best_player_awards", stats.getBestPlayer());
            fact.put("hattricks", stats.getHattrick());
            fact.put("clean_sheets", stats.getCleanSheet());
            facts.add(fact);
        } catch (DataAccessException exception) {
            throw exception;
        } catch (RuntimeException ignored) {
            // Neúplné importované statistiky nesmí zablokovat ověřená data z aplikace.
        }
    }

    private StatisticsFilter allTimePlayerFilter(Long playerId, AiToolContext context) {
        StatisticsFilter filter = new StatisticsFilter();
        filter.setPlayerId(playerId);
        filter.setSeasonId(Config.ALL_SEASON_ID);
        filter.setAppTeam(context.appTeam());
        filter.setLimit(2_000);
        return filter;
    }

    private List<Map<String, Object>> interviewFacts(InterviewDefinition interview, int count) {
        return randomSelection(safeList(interview.questions()), count).stream()
                .map(question -> {
                    Map<String, Object> fact = new LinkedHashMap<>();
                    fact.put("question_number", question.number());
                    fact.put("question", question.question());
                    fact.put("answer", question.answer());
                    fact.put("source_url", interview.sourceUrl());
                    return fact;
                })
                .toList();
    }

    private Map<String, Object> playerIdentity(PlayerDTO player, InterviewDefinition interview) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("player_id", player.getId());
        identity.put("nickname", player.getName());
        identity.put("official_name", player.getFootballPlayer() == null
                ? null
                : player.getFootballPlayer().getName());
        identity.put("person_type", player.isFan() ? "fan" : "player");
        identity.put("active", player.isActive());
        identity.put("interview_available", interview != null);
        if (interview != null && interview.interviewee() != null) {
            identity.put("interview_name", interview.interviewee().fullName());
            identity.put("aliases", safeList(interview.interviewee().aliases()));
        }
        return identity;
    }

    private Map<String, Object> interviewIdentity(InterviewDefinition interview) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("player_id", null);
        identity.put("nickname", null);
        identity.put("official_name", interview.interviewee().fullName());
        identity.put("person_type", "interviewed_player");
        identity.put("interview_available", true);
        identity.put("interview_name", interview.interviewee().fullName());
        identity.put("aliases", safeList(interview.interviewee().aliases()));
        return identity;
    }

    private Map<String, Object> candidateIdentity(PersonCandidate candidate) {
        return candidate.player() == null
                ? interviewIdentity(candidate.interview())
                : playerIdentity(candidate.player(), candidate.interview());
    }

    private Map<String, Object> fact(String kind, String label) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("kind", kind);
        fact.put("label", label);
        return fact;
    }

    private Map<String, Object> error(
            String status,
            String query,
            String message,
            List<Map<String, Object>> candidates
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("query", query);
        result.put("message", message);
        result.put("candidates", candidates);
        return result;
    }

    private <T> List<T> randomSelection(List<T> values, int count) {
        if (values == null || values.isEmpty() || count <= 0) {
            return List.of();
        }
        List<T> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, Math.min(count, shuffled.size())));
    }

    private boolean sameTeamName(String currentTeam, String interviewTeam) {
        return normalize(currentTeam).equals(normalize(interviewTeam));
    }

    private String formatDate(Date date) {
        return date == null ? null : DATE_FORMATTER.format(date.toInstant().atZone(APP_ZONE).toLocalDate());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private InterviewDocument loadInterviews(ObjectMapper objectMapper) {
        try (InputStream input = TrusBotPersonFactService.class.getResourceAsStream(INTERVIEWS_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Chybí rozhovory TrusBota: " + INTERVIEWS_RESOURCE);
            }
            InterviewDocument document = objectMapper.readValue(input, InterviewDocument.class);
            if (document.appTeamName() == null
                    || document.appTeamName().isBlank()
                    || document.interviews() == null
                    || document.interviews().isEmpty()) {
                throw new IllegalStateException("Soubor s rozhovory TrusBota je neplatný.");
            }
            return document;
        } catch (IOException exception) {
            throw new IllegalStateException("Rozhovory TrusBota nelze načíst.", exception);
        }
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record PersonCandidate(PlayerDTO player, InterviewDefinition interview, int score) {
    }

    private record InterviewSearchMatch(
            InterviewDefinition interview,
            PlayerDTO player,
            InterviewQuestion question,
            int score
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InterviewDocument(
            int schemaVersion,
            String sourceType,
            String appTeamName,
            List<InterviewDefinition> interviews
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InterviewDefinition(
            String id,
            String sourceTitle,
            String sourceUrl,
            Interviewee interviewee,
            List<InterviewQuestion> questions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Interviewee(
            String fullName,
            List<String> aliases,
            Long playerId,
            String playerName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InterviewQuestion(int number, String question, String answer) {
    }
}
