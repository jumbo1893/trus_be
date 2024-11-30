package com.jumbo.trus.service;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.match.response.SetupMatchResponse;
import com.jumbo.trus.entity.filter.BaseSeasonFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.repository.*;
import com.jumbo.trus.mapper.MatchMapper;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.repository.specification.MatchSpecification;
import com.jumbo.trus.mapper.PlayerMapper;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.order.OrderMatchByDate;
import com.jumbo.trus.service.order.OrderPlayerByName;
import com.jumbo.trus.service.football.pkfl.PkflMatchService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.jumbo.trus.config.Config.ALL_SEASON_ID;
import static com.jumbo.trus.config.Config.AUTOMATIC_SEASON_ID;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private ReceivedFineRepository receivedFineRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MatchMapper matchMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PkflMatchService pkflMatchService;

    @Autowired
    private NotificationService notificationService;

    /**
     * @param matchDTO Zápas, který přijde z FE
     * @return Zápas, který byl uložen do DB, s id
     */
    public MatchDTO addMatch(MatchDTO matchDTO) {

        MatchEntity entity = matchMapper.toEntity(matchDTO);
        entity.setPlayerList(new ArrayList<>());
        mapPlayersAndSeasonToMatch(entity, matchDTO);
        if (matchDTO.getPkflMatch() == null) {
            entity.setPkflMatch(null);
        }
        MatchEntity savedEntity = matchRepository.save(entity);
        System.out.println(savedEntity.getPkflMatch() == null);
        MatchHelper matchHelper = new MatchHelper(matchDTO);
        notificationService.addNotification("Přidán nový zápas", matchHelper.getMatchWithOpponentNameAndDate());
        return matchMapper.toDTO(savedEntity);
    }

    /**
     * @param matchFilter filtr, podle kterého chceme filtrovat seznam zápasů
     * @return seznam zápasů z DB, které projdou filtrovacími kritérii
     */
    public List<MatchDTO> getAll(MatchFilter matchFilter){
        MatchSpecification matchSpecification = new MatchSpecification(matchFilter);
        List<MatchDTO> matchDTOList = new ArrayList<>(matchRepository.findAll(matchSpecification, PageRequest.of(0, matchFilter.getLimit())).stream().map(matchMapper::toDTO).toList());
        matchDTOList.sort(new OrderMatchByDate());
        return matchDTOList;
    }


    /**
     * @param limit limit počtu zápasů
     * @param desc true = sestupně, false = vzestupně
     * @return seznam všech zápasů z DB
     */
    public List<MatchDTO> getMatchesByDate(int limit, boolean desc){
        if (desc) {
            return matchRepository.getMatchesOrderByDateDesc(limit).stream().map(matchMapper::toDTO).collect(Collectors.toList());
        }
        else {
            return matchRepository.getMatchesOrderByDateAsc(limit).stream().map(matchMapper::toDTO).collect(Collectors.toList());
        }
    }

    /**
     * @param matchId zápas, z kterého chceme playerList
     * @return seznam všech hráčů a fanoušků z konkrétního zápasu
     */
    public List<PlayerDTO> getPlayerListByMatchId(Long matchId){
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        List<PlayerDTO> players = new ArrayList<>(matchEntity.getPlayerList().stream().map(playerMapper::toDTO).toList());
        players.sort(new OrderPlayerByName());
        return players;
    }

    /**
     * @param matchId zápas, z kterého chceme playerList
     * @param fan true = pouze fanoušci, false = pouze hráči
     * @return seznam všech hráčů či fanoušků z konkrétního zápasu
     */
    public List<PlayerDTO> getPlayerListByFilteredByFansByMatchId(Long matchId, boolean fan){
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        List<PlayerDTO> playerDTOS = matchEntity.getPlayerList().stream().map(playerMapper::toDTO).toList();
        List<PlayerDTO> players = new ArrayList<>();
        for (PlayerDTO playerDTO : playerDTOS) {
            if (playerDTO.isFan() == fan) {
                players.add(playerDTO);
            }
        }
        players.sort(new OrderPlayerByName());
        return players;
    }

    /**
     * @param matchId id zápasu, který chceme editovat
     * @param matchDTO nový zápas, kterým nahradíme původní
     * @return nový zápas, který byl uložen do DB
     * @throws NotFoundException zápas nenalezen
     */
    public MatchDTO editMatch(Long matchId, MatchDTO matchDTO) throws NotFoundException {
        if (!matchRepository.existsById(matchId)) {
            throw new NotFoundException("Zápas s id " + matchId + " nenalezen v db");
        }
        MatchEntity entity = matchMapper.toEntity(matchDTO);
        entity.setId(matchId);
        mapPlayersAndSeasonToMatch(entity, matchDTO);
        if (matchDTO.getPkflMatch() == null) {
            entity.setPkflMatch(null);
        }
        MatchEntity savedEntity = matchRepository.save(entity);
        MatchHelper matchHelper = new MatchHelper(matchDTO);
        notificationService.addNotification("Upraven zápas", matchHelper.getMatchWithOpponentNameAndDate());
        return matchMapper.toDTO(savedEntity);
    }

    /**
     * @param matchId id zápasu, který chceme smazat
     */
    @Transactional
    public void deleteMatch(Long matchId) {
        matchRepository.deleteByPlayersInMatchByMatchId(matchId);
        receivedFineRepository.deleteByMatchId(matchId);
        goalRepository.deleteByMatchId(matchId);
        beerRepository.deleteByMatchId(matchId);
        MatchEntity matchEntity = matchRepository.getReferenceById(matchId);
        MatchHelper matchHelper = new MatchHelper(matchMapper.toDTO(matchEntity));
        notificationService.addNotification("Smazán zápas", matchHelper.getMatchWithOpponentNameAndDate());
        matchRepository.deleteById(matchId);
    }

    /**
     * @param matchId id zápasu, na který chceme sestavit setup response. Může být null, pak přijde setup response pro nový zápas
     * @return setup pro pole na FE
     */
    public SetupMatchResponse setupMatch(Long matchId) {
        SetupMatchResponse response = new SetupMatchResponse();
        if (matchId != null) {
            MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
            if (matchEntity != null) {
                response.setMatch(matchMapper.toDTO(matchEntity));
                response.setPrimarySeason(seasonService.getSeason(matchEntity.getSeason().getId()));
                response.setPkflMatch(pkflMatchService.getPkflMatchByDate(matchEntity.getDate()));
            }
        }
        else {
            response.setPrimarySeason(seasonService.getAutomaticSeason());
        }
        response.setSeasonList(seasonService.getAll(new SeasonFilter(false, true, true)));
        response.setFanList(playerService.getAllByFan(true));
        response.setPlayerList(playerService.getAllByFan(false));
        return response;
    }

    /**
     * @param matchId Id zápasu
     * @return zápas nalezený v DB
     */
    public MatchDTO getMatch(long matchId) {
        MatchEntity matchEntity = matchRepository.findById(matchId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(matchId)));
        return matchMapper.toDTO(matchEntity);
    }

    /**
     * @param seasonId Id sezony, ze které chceme zápas
     * @return poslední zápas v dané sezoně podle vstupu
     */
    public MatchDTO getLatestMatchBySeasonId(long seasonId) {
        MatchEntity matchEntity;
        if (seasonId == ALL_SEASON_ID) {
            List<MatchEntity> matchEntities = matchRepository.getMatchesOrderByDateDesc(1);
            if (matchEntities.isEmpty()) {
                return null;
            }
            matchEntity = matchEntities.get(0);
        }
        else {
            matchEntity = matchRepository.getLastMatchBySeasonId(seasonId);
        }
        if (matchEntity != null) {
            return matchMapper.toDTO(matchEntity);
        }
        return null;
    }

    /**
     * @param filter filtr, podle kterého chceme získat zápas.
     * @return sezona a zápas podle filtru. Pokud je zápas prázdný, vrátí se poslední zápas z dané sezony. Pokud je i sezona prázdná, vrátí se poslední zápas z aktuální sezony. Pokud aktuální sezona nemá zápas, vrátí se poslední hraný zápas
     */
    public PairSeasonMatch returnSeasonAndMatchByFilter(BaseSeasonFilter filter) {
        MatchDTO matchDTO;
        SeasonDTO seasonDTO;
        if (filter.getMatchId() != null) {
            matchDTO = getMatch(filter.getMatchId());
            if(filter.getSeasonId() != null) {
                seasonDTO = seasonService.getSeason(filter.getSeasonId());
            }
            else {
                seasonDTO = seasonService.getSeason(matchDTO.getSeasonId());
            }

        } else if (filter.getSeasonId() != null) {
            matchDTO = getLatestMatchBySeasonId(filter.getSeasonId());
            seasonDTO = seasonService.getSeason(filter.getSeasonId());
            if (matchDTO == null) {
                matchDTO = getLatestMatchBySeasonId(ALL_SEASON_ID);
                seasonDTO = seasonService.getSeason(matchDTO.getSeasonId());
            }
        }
        else {
            seasonDTO = seasonService.getCurrentSeason();
            matchDTO = getLatestMatchBySeasonId(seasonDTO.getId());
            if (matchDTO == null) {
                matchDTO = getLatestMatchBySeasonId(ALL_SEASON_ID);
                seasonDTO = seasonService.getSeason(matchDTO.getSeasonId());
            }
        }
        return new PairSeasonMatch(seasonDTO, matchDTO);
    }

    /**
     * @param match entita, do které chceme namapovat přepravku
     * @param matchDTO přepravka, kterou chceme namapovat do entity. Přepravdka obsahuje sezonu a hráče, které mapujeme
     */
    private void mapPlayersAndSeasonToMatch(MatchEntity match, MatchDTO matchDTO){
        match.setPlayerList(new ArrayList<>());
        List<PlayerEntity> people = playerRepository.findAllById(matchDTO.getPlayerIdList());
        match.getPlayerList().addAll(people);
        if(matchDTO.getSeasonId() == AUTOMATIC_SEASON_ID) {
            long newSeasonId = seasonService.getSeasonByDate(matchDTO.getDate()).getId();
            matchDTO.setSeasonId(newSeasonId);
        }
        match.setSeason(seasonRepository.getReferenceById(matchDTO.getSeasonId()));
    }

}
