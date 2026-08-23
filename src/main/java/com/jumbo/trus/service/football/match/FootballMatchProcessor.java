package com.jumbo.trus.service.football.match;

import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.football.FootballMatchPlayerDTO;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.football.FootballMatchRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import com.jumbo.trus.service.outbox.OutboxEventPayloadFactory;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflMatchDetail;
import com.jumbo.trus.service.football.pkfl.task.helper.FootballMatchDetailTaskHelper;
import com.jumbo.trus.service.helper.Pair;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FootballMatchProcessor {

    private final Logger logger = LoggerFactory.getLogger(FootballMatchProcessor.class);

    private final FootballMatchMapper footballMatchMapper;
    private final FootballMatchRepository footballMatchRepository;
    private final PlayerProcessor playerProcessor;
    private final RetrievePkflMatchDetail retrievePkflMatchDetail;
    private final MatchRepository matchRepository;
    private final FootballMatchPlayerRepository footballMatchPlayerRepository;
    private final OutboxEventService outboxEventService;

    public List<FootballMatchDTO> getAllMatches() {
        return footballMatchRepository.findAll().stream()
                .map(footballMatchMapper::toDTO)
                .toList();
    }

    public FootballMatchDTO getMatchById(Long id) {
        return footballMatchMapper.toDTO(footballMatchRepository.findById(id).orElseThrow());
    }

    public FootballMatchDTO getNextMatch(long teamId) {
        return footballMatchMapper.toDTO(footballMatchRepository.findNextMatch(teamId));
    }

    public FootballMatchDTO getLastMatch(long teamId) {
        return footballMatchMapper.toDTO(footballMatchRepository.findLastMatch(teamId));
    }

    public List<FootballMatchDTO> getNextMatches(long teamId) {
        return footballMatchRepository.findNonPlayedNextMatches(teamId).stream()
                .map(footballMatchMapper::toDTO)
                .toList();
    }

    public List<FootballMatchDTO> getPastMatchesInLeague(long teamId, long leagueId) {
        return footballMatchRepository.findPastMatchesInLeague(teamId, leagueId).stream()
                .map(footballMatchMapper::toDTO)
                .toList();
    }

    public List<FootballMatchDTO> findMutualMatches(long teamId1, long teamId2) {
        return footballMatchRepository.findAlreadyPlayedMatchesOfTwoTeams(teamId1, teamId2).stream()
                .map(footballMatchMapper::toDTO)
                .toList();
    }

    /**
     * @param footballMatchDTO zápas načtený z webu
     * @param repositoryMatch zápas z db, pokud existuje, jinak null
     * @return Pair<MatchProcessingResult, Long> - výsledek zpracování a ID zápasu
     */
    public Pair<MatchProcessingResult, Long> processMatch(FootballMatchDTO repositoryMatch, FootballMatchDTO footballMatchDTO) {
            //matchLog("----procesuji zápas----", footballMatchDTO, repositoryMatch);
            if (isNeededToGetMatchDetails(repositoryMatch, footballMatchDTO)) {
                //logger.debug("procesuji úplný zápas");

                long savedMatchId = enhanceFootballMatchWithDetailsAndSave(footballMatchDTO, repositoryMatch);
                createUpdatedEvent(repositoryMatch, savedMatchId);
                return new Pair<>(MatchProcessingResult.FULLY_PROCESSED, savedMatchId);
            }
            else if (isNeededToSaveSimpleMatch(repositoryMatch, footballMatchDTO)) {
                //logger.debug("procesuji neúplný zápas");
                setFootballMatchId(repositoryMatch, footballMatchDTO);

                long savedMatchId = saveMatchToRepository(footballMatchDTO).getId();
                createUpdatedEvent(repositoryMatch, savedMatchId);
                return new Pair<>(MatchProcessingResult.PARTIALLY_PROCESSED, savedMatchId);
            }
        return new Pair<>(MatchProcessingResult.UNPROCESSED, repositoryMatch.getId());
    }

    private void createUpdatedEvent(FootballMatchDTO repositoryMatch, Long footballMatchId) {
        if (repositoryMatch == null) {
            return;
        }

        Map<Long, Set<Long>> matchIdsByAppTeam = new LinkedHashMap<>();
        for (MatchRepository.AppTeamMatchIdProjection affectedMatch
                : matchRepository.findAppTeamMatchIdsByFootballMatchId(footballMatchId)) {
            matchIdsByAppTeam
                    .computeIfAbsent(affectedMatch.getAppTeamId(), ignored -> new LinkedHashSet<>())
                    .add(affectedMatch.getMatchId());
        }

        Set<Long> footballPlayerIds = footballMatchPlayerRepository.findPlayerIdsByMatchId(footballMatchId);
        matchIdsByAppTeam.forEach((appTeamId, matchIds) ->
                // Scheduled PKFL synchronization has no HTTP request, so the
                // affected app team must be supplied explicitly.
                outboxEventService.createEventForTeam(
                        OutboxEventType.FOOTBALL_MATCH_UPDATED,
                        OutboxAggregateType.FOOTBALL_MATCH,
                        footballMatchId,
                        OutboxEventPayloadFactory.footballMatchUpdated(matchIds, footballPlayerIds),
                        appTeamId,
                        -1L
                )
        );
    }

    private void setFootballMatchId(FootballMatchDTO repositoryMatch, FootballMatchDTO newMatch) {
        Long id = repositoryMatch != null ? repositoryMatch.getId() : null;
        newMatch.setId(id);
    }

    public void cleanAllMatchesNotBelongingToLeague(Long leagueId, List<Long> matchesId) {
        if (matchesId == null || matchesId.isEmpty()) {
            return;
        }
        logger.debug("smazáno přebytečných zápasů: {} ", footballMatchRepository.deleteByLeagueIdAndMatchIdNotIn(leagueId, matchesId));
    }

    public FootballMatchDTO findMatchByHomeTeamAndRound(Long homeTeamId, Integer round, Long leagueId) {
        Optional<FootballMatchEntity> matchOpt = footballMatchRepository.findByHomeTeam_IdAndRoundAndLeagueId(homeTeamId, round, leagueId);
        return matchOpt.map(footballMatchMapper::toDTO) // Používá metodu mapperu
                .orElse(null);
    }

    private boolean isNeededToGetMatchDetails(FootballMatchDTO repositoryMatch, FootballMatchDTO newMatch) {
        return (newMatch.isAlreadyPlayed() && (repositoryMatch == null ||
                (repositoryMatch.isAlreadyPlayed() && isRefereeCommentNull(repositoryMatch)) || isNeededToUpdateDetailedMatch(repositoryMatch, newMatch)));
    }

    private boolean isRefereeCommentNull(FootballMatchDTO footballMatchDTO) {
        return footballMatchDTO.getRefereeComment() == null || footballMatchDTO.getRefereeComment().isEmpty() || footballMatchDTO.getRefereeComment().equals(RetrievePkflMatchDetail.NO_REFEREE_COMMENT);
    }

    private boolean isNeededToSaveSimpleMatch(FootballMatchDTO repositoryMatch, FootballMatchDTO newMatch) {
        return repositoryMatch == null || !repositoryMatch.equals(newMatch);
    }

    private boolean isNeededToUpdateDetailedMatch(FootballMatchDTO repositoryMatch, FootballMatchDTO newMatch) {
        return repositoryMatch != null && (!isRefereeCommentNull(repositoryMatch) && !repositoryMatch.equals(newMatch));
    }

    private long enhanceFootballMatchWithDetailsAndSave(FootballMatchDTO footballMatchDTO, FootballMatchDTO repositoryMatch) {
        FootballMatchDetailTaskHelper detail = fetchMatchDetails(footballMatchDTO);
        Long repositoryId = resolveRepositoryId(footballMatchDTO, repositoryMatch);
        List<FootballMatchPlayerDTO> players = processAndSetPlayers(detail, repositoryId, footballMatchDTO);

        return finalizeAndSaveMatch(footballMatchDTO, repositoryId, players);
    }

    private FootballMatchDetailTaskHelper fetchMatchDetails(FootballMatchDTO footballMatchDTO) {
        FootballMatchDetailTaskHelper detail = retrievePkflMatchDetail.getMatchDetail(footballMatchDTO);
        footballMatchDTO.setRefereeComment(detail.getRefereeComment());
        return detail;
    }

    private Long resolveRepositoryId(FootballMatchDTO footballMatchDTO, FootballMatchDTO repositoryMatch) {
        return repositoryMatch == null
                ? saveMatchToRepository(footballMatchDTO).getId()
                : repositoryMatch.getId();
    }

    private long finalizeAndSaveMatch(FootballMatchDTO footballMatchDTO, Long repositoryId, List<FootballMatchPlayerDTO> players) {
        footballMatchDTO.setId(repositoryId);
        FootballMatchEntity entity = footballMatchMapper.toEntity(footballMatchDTO);
        entity.setPlayerList(playerProcessor.convertPlayerListDtoToEntity(players));
        return saveMatchToRepository(entity).getId();
    }


    private List<FootballMatchPlayerDTO> processAndSetPlayers(FootballMatchDetailTaskHelper detail, Long repositoryId, FootballMatchDTO footballMatchDTO) {
        return playerProcessor.processPlayers(detail, repositoryId, footballMatchDTO);
    }

    private FootballMatchDTO saveMatchToRepository(FootballMatchDTO footballMatchDTO) {
        return footballMatchMapper.toDTO(footballMatchRepository.save(footballMatchMapper.toEntity(footballMatchDTO)));
    }

    private FootballMatchDTO saveMatchToRepository(FootballMatchEntity footballMatchEntity) {
        return footballMatchMapper.toDTO(footballMatchRepository.save(footballMatchEntity));
    }

    public FootballMatchDTO findFootballMatchByDate(long teamId, Date startDate, Date endDate) {
        FootballMatchEntity entity = footballMatchRepository.findByDate(teamId, startDate, endDate);
        if (entity == null) {
            return null;
        }
        return footballMatchMapper.toDTO(entity);
    }
}
