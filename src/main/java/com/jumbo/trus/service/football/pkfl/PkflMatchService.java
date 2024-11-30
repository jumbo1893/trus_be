package com.jumbo.trus.service.football.pkfl;

import com.jumbo.trus.dto.UpdateDTO;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.pkfl.*;
import com.jumbo.trus.dto.pkfl.stats.PkflAllIndividualStats;
import com.jumbo.trus.dto.pkfl.stats.PkflCardComment;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.pkfl.*;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.mapper.UpdateMapper;
import com.jumbo.trus.mapper.pkfl.*;
import com.jumbo.trus.service.football.pkfl.fact.PkflPlayerFact;
import com.jumbo.trus.service.football.pkfl.task.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;

@Service
public class PkflMatchService {

    private final static String PKFL_MATCHES = "PKFL_MATCHES";

    @Value("${pkfl.trus}")
    private String trusUrl;

    @Value("${pkfl.table}")
    private String tableUrl;

    @Autowired
    private PkflSeasonService pkflSeasonService;

    @Autowired
    private PkflMatchRepository pkflMatchRepository;

    @Autowired
    private PkflRefereeRepository pkflRefereeRepository;

    @Autowired
    private PkflStadiumRepository pkflStadiumRepository;

    @Autowired
    private PkflPlayerRepository pkflPlayerRepository;

    @Autowired
    private PkflIndividualStatsRepository pkflIndividualStatsRepository;

    @Autowired
    private PkflOpponentRepository pkflOpponentRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UpdateRepository updateRepository;

    @Autowired
    PkflMatchMapper pkflMatchMapper;

    @Autowired
    PkflRefereeMapper pkflRefereeMapper;

    @Autowired
    PkflStadiumMapper pkflStadiumMapper;

    @Autowired
    PkflPlayerMapper pkflPlayerMapper;

    @Autowired
    PkflIndividualStatsMapper pkflIndividualStatsMapper;

    @Autowired
    PkflOpponentMapper pkflOpponentMapper;

    @Autowired
    UpdateMapper updateMapper;

    public List<PkflTableTeamDTO> getTableTeams() {
        RetrievePkflTable retrieveTable = new RetrievePkflTable();
        List<PkflTableTeamDTO> pkflTableTeamDTOS = retrieveTable.getTableTeams(tableUrl);
        for (PkflTableTeamDTO team : pkflTableTeamDTOS) {
            if (team != null) {
                PkflOpponentEntity opponent = pkflOpponentRepository.getOpponentByName(team.getOpponent().getName());
                if (opponent != null) {
                    team.setOpponent(pkflOpponentMapper.toDTO(opponent));
                    List<PkflMatchEntity> matches = pkflMatchRepository.findAlreadyPlayedByOpponentId(team.getOpponent().getId());
                    if (!matches.isEmpty()) {
                        team.setPkflMatchId(matches.get(0).getId());
                    }
                }
            }
        }
        return pkflTableTeamDTOS;
    }

    public List<PkflPlayerDTO> getPlayers() {
        return pkflPlayerRepository.findAll(Sort.by(Sort.Direction.ASC, PkflPlayerEntity_.NAME)).stream().map(pkflPlayerMapper::toDTO).toList();
    }

    public List<PkflMatchDTO> getNextAndLastMatchInPkfl(Boolean updateMatches) {
        List<PkflMatchDTO> matches = new ArrayList<>();
        if (isRequiredToUpdateMatches(updateMatches)) {
            List<PkflSeasonDTO> pkflSeasonList = pkflSeasonService.getCurrentSeasons();
            getMatchesWithPossibleUpdateNeeded(pkflSeasonList);
        }
        matches.add(getNextMatchFromRepository());
        matches.add(getLastMatchFromRepository());
        return matches;
    }

    public List<PkflAllIndividualStats> getPlayerStats(boolean currentSeason) {
        updateAllMatchesIfNeeded();
        List<PkflAllIndividualStats> returnPlayerList = new ArrayList<>();
        List<PkflPlayerDTO> players = pkflPlayerRepository.findAll().stream().map(pkflPlayerMapper::toDTO).toList();
        List<Long> matchIds = new ArrayList<>();
        List<PkflSeasonDTO> pkflSeasonList = new ArrayList<>();
        if (currentSeason) {
            pkflSeasonList = pkflSeasonService.getCurrentSeasons();
            matchIds = getMatchIdsFromSeasonIds(pkflSeasonList);
        }
        for (PkflPlayerDTO player : players) {
            if (!currentSeason) {
                int numberOfMatches = pkflIndividualStatsRepository.getCount(player.getId());
                if (numberOfMatches > 0) {
                    PkflAllIndividualStats pkflAllIndividualStats = new PkflAllIndividualStats();
                    pkflAllIndividualStats.setPlayer(player);
                    pkflAllIndividualStats.setMatches(numberOfMatches);
                    pkflAllIndividualStats.setGoals(pkflIndividualStatsRepository.getGoalsSum(player.getId()));
                    pkflAllIndividualStats.setReceivedGoals(pkflIndividualStatsRepository.getReceivedGoalsSum(player.getId()));
                    pkflAllIndividualStats.setOwnGoals(pkflIndividualStatsRepository.getOwnGoalsSum(player.getId()));
                    pkflAllIndividualStats.setGoalkeepingMinutes(pkflIndividualStatsRepository.getGoalkeepingMinutesSum(player.getId()));
                    pkflAllIndividualStats.setYellowCards(pkflIndividualStatsRepository.getYellowCardsSum(player.getId()));
                    pkflAllIndividualStats.setRedCards(pkflIndividualStatsRepository.getRedCardsSum(player.getId()));
                    pkflAllIndividualStats.setBestPlayer(pkflIndividualStatsRepository.getBestPlayerSum(player.getId()));
                    pkflAllIndividualStats.setHattrick(pkflIndividualStatsRepository.getHattrickSum(player.getId()));
                    pkflAllIndividualStats.setCleanSheet(pkflIndividualStatsRepository.getCleanSheetSum(player.getId()));
                    pkflAllIndividualStats.setYellowCardComments(getCardComment(true, pkflIndividualStatsRepository.findAllYellowCardCommentsByPlayerId(player.getId()).stream().map(pkflIndividualStatsMapper::toDTO).toList()));
                    pkflAllIndividualStats.setRedCardComments(getCardComment(false, pkflIndividualStatsRepository.findAllRedCardCommentsByPlayerId(player.getId()).stream().map(pkflIndividualStatsMapper::toDTO).toList()));
                    pkflAllIndividualStats.setMatchPoints(getNumberOfPointFromMatchByPlayer(player.getId(), null));
                    returnPlayerList.add(pkflAllIndividualStats);
                }
            } else {
                int numberOfMatches = pkflIndividualStatsRepository.getCount(player.getId(), matchIds);
                if (numberOfMatches > 0) {
                    PkflAllIndividualStats pkflAllIndividualStats = new PkflAllIndividualStats();
                    pkflAllIndividualStats.setPlayer(player);
                    pkflAllIndividualStats.setMatches(numberOfMatches);
                    pkflAllIndividualStats.setGoals(pkflIndividualStatsRepository.getGoalsSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setReceivedGoals(pkflIndividualStatsRepository.getReceivedGoalsSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setOwnGoals(pkflIndividualStatsRepository.getOwnGoalsSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setGoalkeepingMinutes(pkflIndividualStatsRepository.getGoalkeepingMinutesSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setYellowCards(pkflIndividualStatsRepository.getYellowCardsSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setRedCards(pkflIndividualStatsRepository.getRedCardsSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setBestPlayer(pkflIndividualStatsRepository.getBestPlayerSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setHattrick(pkflIndividualStatsRepository.getHattrickSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setCleanSheet(pkflIndividualStatsRepository.getCleanSheetSum(player.getId(), matchIds));
                    pkflAllIndividualStats.setYellowCardComments(getCardComment(true, pkflIndividualStatsRepository.findAllYellowCardCommentsByPlayerId(player.getId(), matchIds).stream().map(pkflIndividualStatsMapper::toDTO).toList()));
                    pkflAllIndividualStats.setRedCardComments(getCardComment(false, pkflIndividualStatsRepository.findAllRedCardCommentsByPlayerId(player.getId(), matchIds).stream().map(pkflIndividualStatsMapper::toDTO).toList()));
                    pkflAllIndividualStats.setMatchPoints(getNumberOfPointFromMatchByPlayer(player.getId(), pkflSeasonList));
                    returnPlayerList.add(pkflAllIndividualStats);
                }
            }
        }
        return returnPlayerList;
    }

    private int getNumberOfPointFromMatchByPlayer(long playerId, List<PkflSeasonDTO> pkflSeasonList) {
        List<Long> matchIds = pkflIndividualStatsRepository.findAllMatchesByPlayerId(playerId);
        List<PkflMatchDTO> matchList;
        if (pkflSeasonList == null || pkflSeasonList.isEmpty()) {
            matchList = pkflMatchRepository.findAllById(matchIds).stream().map(pkflMatchMapper::toDTO).toList();
        }
        else {
            matchList = pkflMatchRepository.findAllByIdsAndSeasonIds(getIdsFromSeasonList(pkflSeasonList), matchIds).stream().map(pkflMatchMapper::toDTO).toList();
        }

        int points = 0;
        for (PkflMatchDTO match : matchList) {
            if (match.isAlreadyPlayed()) {
                if (match.getTrusGoalNumber() > match.getOpponentGoalNumber()) {
                    points += 3;
                } else if (match.getTrusGoalNumber().equals(match.getOpponentGoalNumber())) {
                    points += 1;
                }
            }
        }
        return points;
    }

    private List<PkflCardComment> getCardComment(boolean yellowCard, List<PkflIndividualStatsDTO> individualStats) {
        List<PkflCardComment> returnComments = new ArrayList<>();
        for (PkflIndividualStatsDTO stats : individualStats) {
            PkflCardComment pkflCardComment = new PkflCardComment();
            pkflCardComment.setPkflMatch(pkflMatchMapper.toDTO(pkflMatchRepository.findById(stats.getMatchId()).orElseThrow(() -> new EntityNotFoundException(String.valueOf(stats.getMatchId())))));
            if (yellowCard) {
                pkflCardComment.setComment(stats.getYellowCardComment());
            } else {
                pkflCardComment.setComment(stats.getRedCardComment());
            }
            returnComments.add(pkflCardComment);
        }
        return returnComments;
    }

    private List<Long> getMatchIdsFromSeasonIds(List<PkflSeasonDTO> pkflSeasonList) {
        List<Long> seasonIdList = getIdsFromSeasonList(pkflSeasonList);
        return pkflMatchRepository.findAllIdsBySeasonIds(seasonIdList);
    }

    private List<Long> getIdsFromSeasonList(List<PkflSeasonDTO> pkflSeasonList) {
        List<Long> seasonIdList = new ArrayList<>();
        for (PkflSeasonDTO season : pkflSeasonList) {
            seasonIdList.add(season.getId());
        }
        return seasonIdList;
    }

    public List<StringAndString> getFactsForPlayer(long playerId) {
        PkflPlayerFact playerFact = new PkflPlayerFact(pkflIndividualStatsRepository, pkflMatchMapper, pkflRefereeMapper, playerId);
        return playerFact.getStatsForPlayer();
    }

    private List<PkflMatchDTO> getAllMatches() {
        updateAllMatchesIfNeeded();
        return pkflMatchRepository.findAll().stream().map(pkflMatchMapper::toDTO).toList();
    }

    private void updateAllMatchesIfNeeded() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 1);
        if (!pkflMatchRepository.existsMatchOlderThenDate(calendar.getTime())) {
            System.out.println(calendar);
            List<PkflSeasonDTO> pkflSeasonList = pkflSeasonService.getAllSeasons();
            getMatchesWithPossibleUpdateNeeded(pkflSeasonList);
        }
    }

    public List<PkflMatchDTO> getPkflFixtures(Boolean updateMatches) {
        if (isRequiredToUpdateMatches(updateMatches)) {
            List<PkflSeasonDTO> pkflSeasonList = pkflSeasonService.getCurrentSeasons();
            getMatchesWithPossibleUpdateNeeded(pkflSeasonList);
        }
        return pkflMatchRepository.getNonPlayedMatchesOrderByDate().stream().map(pkflMatchMapper::toDTO).toList();
    }

    public PkflMatchDTO getPkflMatchByDate(Date date) {
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(date);
        endCalendar.add(Calendar.DATE, 1);
        PkflMatchEntity entity = pkflMatchRepository.findByDate(date, endCalendar.getTime());
        if (entity == null) {
            return null;
        }
        return pkflMatchMapper.toDTO(entity);
    }

    public PkflMatchDetail getPkflMatchDetail(long pkflMatchId) {
        PkflMatchDetail pkflMatchDetail = new PkflMatchDetail();
        PkflMatchEntity matchEntity = pkflMatchRepository.findById(pkflMatchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(pkflMatchId)));
        pkflMatchDetail.setPkflMatch(pkflMatchMapper.toDTO(matchEntity));
        List<PkflMatchDTO> commonMatches = pkflMatchRepository.findAlreadyPlayedByOpponentId(matchEntity.getOpponent().getId()).stream().map(pkflMatchMapper::toDTO).toList();
        pkflMatchDetail.setCommonMatches(commonMatches);
        if (commonMatches.isEmpty()) {
            return pkflMatchDetail;
        }
        int trusScore = 0;
        int opponentScore = 0;
        int numberOfWins = 0;
        int numberOfDraws = 0;
        int numberOfLosses = 0;
        for (PkflMatchDTO match : commonMatches) {
            trusScore += match.getTrusGoalNumber();
            opponentScore += match.getOpponentGoalNumber();
            if (match.getTrusGoalNumber() > match.getOpponentGoalNumber()) {
                numberOfWins++;
            } else if (match.getTrusGoalNumber().equals(match.getOpponentGoalNumber())) {
                numberOfDraws++;
            } else {
                numberOfLosses++;
            }
        }
        String aggregateScore = trusScore + ":" + opponentScore;
        String aggregateMatches = numberOfWins + "/" + numberOfDraws + "/" + numberOfLosses;
        pkflMatchDetail.setAggregateScore(aggregateScore);
        pkflMatchDetail.setAggregateMatches(aggregateMatches);
        return pkflMatchDetail;
    }

    private List<PkflMatchDTO> getMatchesWithPossibleUpdateNeeded(List<PkflSeasonDTO> seasons) {
        List<PkflMatchDTO> returnMatches = new ArrayList<>();
        for (PkflSeasonDTO season : seasons) {
            List<PkflMatchDTO> repositoryMatches = getMatchesFromRepositoryBySeason(season);
            if (isNeededToUpdateMatches(repositoryMatches)) {
                List<PkflMatchDTO> webMatches = getMatchesFromWeb(season);
                updateMatchesInRepository(webMatches, season);
                List<PkflMatchDTO> newRepoMatches = getMatchesFromRepositoryBySeason(season);
                returnMatches.addAll(newRepoMatches);
            } else {
                returnMatches.addAll(repositoryMatches);
            }
        }
        return returnMatches;
    }

    private List<PkflMatchDTO> getMatchesFromWeb(PkflSeasonDTO season) {
        List<PkflMatchDTO> pkflMatches = new ArrayList<>();
        RetrieveMatches retrieveMatches = new RetrieveMatches();
        RetrieveMatchDetail retrieveMatchDetail = new RetrieveMatchDetail();
        for (PkflMatchDTO pkflMatch : retrieveMatches.getMatches(season)) {
            pkflMatch = retrieveMatchDetail.getMatchDetail(pkflMatch);
            pkflMatches.add(pkflMatch);

        }
        return pkflMatches;
    }

    private List<PkflMatchDTO> getMatchesFromRepositoryBySeason(PkflSeasonDTO season) {
        return pkflMatchRepository.getMatchesBySeason(season.getId()).stream().map(pkflMatchMapper::toDTO).toList();
    }

    private boolean isNeededToUpdateMatches(List<PkflMatchDTO> pkflMatches) {
        if (pkflMatches.isEmpty()) {
            return true;
        }
        for (PkflMatchDTO match : pkflMatches) {
            return isRepositoryMatchNeededToUpdate(match);
        }
        return false;
    }

    private boolean isNeededToUpdateWebMatch(PkflMatchDTO match, long seasonId) {
        PkflOpponentEntity pkflOpponentEntity = pkflOpponentRepository.getOpponentByName(match.getOpponent().getName());
        if (pkflOpponentEntity == null) {
            return true;
        }
        long opponentId = pkflOpponentEntity.getId();
        return !pkflMatchRepository.existsByOpponentAndSeasonIdAndHomeMatch(opponentId, seasonId, match.isHomeMatch()) || isRepositoryMatchNeededToUpdateByWebMatch(match, opponentId, seasonId);
    }

    private boolean isRepositoryMatchNeededToUpdate(PkflMatchDTO match) {
        return !match.isAlreadyPlayed() || match.getRefereeComment().equals("Bez komentáře rozhodčího") || match.getPlayerList().isEmpty();
    }

    private boolean isRepositoryMatchNeededToUpdateByWebMatch(PkflMatchDTO webMatch, long opponentId, long seasonId) {
        try {
            PkflMatchDTO match = pkflMatchMapper.toDTO(pkflMatchRepository.getMatchByOpponentAndSeasonIdAndHomeMatch(opponentId, seasonId, webMatch.isHomeMatch()));
            return !match.isAlreadyPlayed() || match.getRefereeComment().equals("Bez komentáře rozhodčího") || match.getPlayerList().isEmpty();
        } catch (Exception e) {
            return true;
        }
    }


    private PkflMatchDTO getLastMatchFromRepository() {
        PkflMatchEntity match = pkflMatchRepository.getLastMatch();
        if (match == null) {
            return null;
        }
        return pkflMatchMapper.toDTO(match);
    }

    private PkflMatchDTO getNextMatchFromRepository() {
        PkflMatchEntity match = pkflMatchRepository.getNextMatch();
        if (match == null) {
            return null;
        }
        return pkflMatchMapper.toDTO(match);
    }

    private void updateMatchesInRepository(List<PkflMatchDTO> matches, PkflSeasonDTO season) {
        for (PkflMatchDTO match : matches) {
            if (isNeededToUpdateWebMatch(match, season.getId())) {
                match.setSeason(season);
                match.setReferee(getOrSaveRefereeToRepository(match.getReferee()));
                match.setStadium(getOrSaveStadiumToRepository(match.getStadium()));
                match.setOpponent(getOrSaveOpponentToRepository(match.getOpponent()));
                PkflMatchEntity existingEntity = findExistingPkflMatchInRepo(match);
                if (existingEntity == null) {
                    PkflMatchDTO savedMatchWithoutPlayerList = saveNewPkflMatchEntityToRepository(match);
                    updateIndividualStatsFromMatch(savedMatchWithoutPlayerList, match);
                    saveExistingEntity(savedMatchWithoutPlayerList);
                } else {
                    PkflMatchDTO pkflMatchDTO = (pkflMatchMapper.toDTO(existingEntity));
                    updateIndividualStatsFromMatch(pkflMatchDTO, match);
                    setMatchFromWebMatchWithoutPlayerList(pkflMatchDTO, match);
                    saveExistingEntity(pkflMatchDTO);
                }
            }
        }
    }

    private void updateIndividualStatsFromMatch(PkflMatchDTO savedMatch, PkflMatchDTO webMatch) {
        List<PkflIndividualStatsDTO> individualStatsFromRepo = new ArrayList<>();
        for (PkflIndividualStatsDTO individualStats : webMatch.getPlayerList()) {
            individualStats.setMatchId(savedMatch.getId());
            individualStatsFromRepo.add(getOrSaveIndividualStatsToRepository(individualStats));
        }
        savedMatch.setPlayerList(individualStatsFromRepo);
    }

    private PkflMatchEntity findExistingPkflMatchInRepo(PkflMatchDTO match) {
        if (!pkflMatchRepository.existsByOpponentAndSeasonIdAndHomeMatch(match.getOpponent().getId(), match.getSeason().getId(), match.isHomeMatch())) {
            return null;
        }
        return pkflMatchRepository.getMatchByOpponentAndSeasonIdAndHomeMatch(match.getOpponent().getId(), match.getSeason().getId(), match.isHomeMatch());
    }

    private PkflMatchDTO setNewMatchWithoutPlayerList(PkflMatchDTO pkflMatchDTO) {
        PkflMatchDTO matchToRepo = new PkflMatchDTO();
        matchToRepo.setHomeMatch(pkflMatchDTO.isHomeMatch());
        matchToRepo.setOpponent(pkflMatchDTO.getOpponent());
        matchToRepo.setRound(pkflMatchDTO.getRound());
        matchToRepo.setLeague(pkflMatchDTO.getLeague());
        matchToRepo.setDate(pkflMatchDTO.getDate());
        matchToRepo.setStadium(pkflMatchDTO.getStadium());
        matchToRepo.setReferee(pkflMatchDTO.getReferee());
        matchToRepo.setTrusGoalNumber(pkflMatchDTO.getTrusGoalNumber());
        matchToRepo.setOpponentGoalNumber(pkflMatchDTO.getOpponentGoalNumber());
        matchToRepo.setUrlResult(pkflMatchDTO.getUrlResult());
        matchToRepo.setRefereeComment(pkflMatchDTO.getRefereeComment());
        matchToRepo.setAlreadyPlayed(pkflMatchDTO.isAlreadyPlayed());
        matchToRepo.setSeason(pkflMatchDTO.getSeason());
        return matchToRepo;
    }

    private void setMatchFromWebMatchWithoutPlayerList(PkflMatchDTO repoMatch, PkflMatchDTO webMatch) {
        repoMatch.setHomeMatch(webMatch.isHomeMatch());
        repoMatch.setOpponent(webMatch.getOpponent());
        repoMatch.setRound(webMatch.getRound());
        repoMatch.setLeague(webMatch.getLeague());
        repoMatch.setDate(webMatch.getDate());
        repoMatch.setStadium(webMatch.getStadium());
        repoMatch.setReferee(webMatch.getReferee());
        repoMatch.setTrusGoalNumber(webMatch.getTrusGoalNumber());
        repoMatch.setOpponentGoalNumber(webMatch.getOpponentGoalNumber());
        repoMatch.setUrlResult(webMatch.getUrlResult());
        repoMatch.setRefereeComment(webMatch.getRefereeComment());
        repoMatch.setAlreadyPlayed(webMatch.isAlreadyPlayed());
        repoMatch.setSeason(webMatch.getSeason());
    }

    private PkflMatchDTO saveNewPkflMatchEntityToRepository(PkflMatchDTO pkflMatch) {
        PkflMatchEntity entity = pkflMatchMapper.toEntity(setNewMatchWithoutPlayerList(pkflMatch));
        entity.setMatchList(new ArrayList<>());
        return pkflMatchMapper.toDTO(pkflMatchRepository.save(entity));
    }

    private PkflMatchEntity saveExistingEntity(PkflMatchDTO pkflMatch) {
        PkflMatchEntity entity = pkflMatchMapper.toEntity(pkflMatch);
        mapPkflMatchAndMatchList(entity);
        return pkflMatchRepository.save(entity);
    }

    private void mapPkflMatchAndMatchList(PkflMatchEntity pkflMatch) {
        pkflMatch.setMatchList(new ArrayList<>());
        List<MatchEntity> matches = matchRepository.findAllByPkflMatchId(pkflMatch.getId());
        pkflMatch.getMatchList().addAll(matches);
    }

    private PkflRefereeDTO getOrSaveRefereeToRepository(PkflRefereeDTO referee) {
        if (referee == null || referee.getName().isEmpty()) {
            return null;
        }
        if (!pkflRefereeRepository.existsByName(referee.getName())) {
            PkflRefereeEntity entity = pkflRefereeMapper.toEntity(referee);
            pkflRefereeRepository.save(entity);
        }
        return pkflRefereeMapper.toDTO(pkflRefereeRepository.getRefereeByName(referee.getName()));
    }

    private PkflStadiumDTO getOrSaveStadiumToRepository(PkflStadiumDTO stadium) {
        if (stadium == null || stadium.getName().isEmpty()) {
            return null;
        }
        if (!pkflStadiumRepository.existsByName(stadium.getName())) {
            PkflStadiumEntity entity = pkflStadiumMapper.toEntity(stadium);
            pkflStadiumRepository.save(entity);
        }
        return pkflStadiumMapper.toDTO(pkflStadiumRepository.getStadiumByName(stadium.getName()));
    }

    private PkflOpponentDTO getOrSaveOpponentToRepository(PkflOpponentDTO opponent) {
        if (opponent == null || opponent.getName().isEmpty()) {
            return null;
        }
        if (!pkflOpponentRepository.existsByName(opponent.getName())) {
            PkflOpponentEntity entity = pkflOpponentMapper.toEntity(opponent);
            pkflOpponentRepository.save(entity);
        }
        return pkflOpponentMapper.toDTO(pkflOpponentRepository.getOpponentByName(opponent.getName()));
    }

    private PkflPlayerDTO getOrSavePlayerToRepository(PkflPlayerDTO player) {
        if (!pkflPlayerRepository.existsByName(player.getName())) {
            PkflPlayerEntity entity = pkflPlayerMapper.toEntity(player);
            pkflPlayerRepository.save(entity);
        }
        return pkflPlayerMapper.toDTO(pkflPlayerRepository.getPlayerByName(player.getName()));
    }

    private PkflIndividualStatsDTO getOrSaveIndividualStatsToRepository(PkflIndividualStatsDTO individualStats) {
        PkflPlayerDTO player = getOrSavePlayerToRepository(individualStats.getPlayer());
        if (!pkflIndividualStatsRepository.existsByPlayerIdAndMatchId(player.getId(), individualStats.getMatchId())) {

            individualStats.setPlayer(player);
            PkflIndividualStatsEntity entity = pkflIndividualStatsMapper.toEntity(individualStats);
            pkflIndividualStatsRepository.save(entity);
        }
        return pkflIndividualStatsMapper.toDTO(pkflIndividualStatsRepository.getPlayerByPlayerId(player.getId()));
    }

    private boolean isRequiredToUpdateMatches(Boolean updateMatches) {
        return isNeededToUpdateMatches() || (updateMatches != null && updateMatches);
    }

    private boolean isNeededToUpdateMatches() {
        UpdateDTO updateDTO = updateMapper.toDTO(updateRepository.getUpdateByName(PKFL_MATCHES));
        if (updateDTO == null) {
            UpdateDTO newUpdate = new UpdateDTO();
            Date date = new Date();
            newUpdate.setDate(date);
            newUpdate.setName(PKFL_MATCHES);
            updateRepository.save(updateMapper.toEntity(newUpdate));
            return true;
        } else if (isDateOlderThanDay(updateDTO.getDate())) {
            Date date = new Date();
            updateDTO.setDate(date);
            updateRepository.save(updateMapper.toEntity(updateDTO));
            return true;
        }
        return false;
    }

    private boolean isDateOlderThanDay(Date date) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime yesterday = now.plusDays(-1);
        return date.toInstant().isBefore(yesterday.toInstant());
    }

}
