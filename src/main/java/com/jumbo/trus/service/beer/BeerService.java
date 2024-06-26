package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.*;
import com.jumbo.trus.dto.beer.multi.BeerListDTO;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchDTO;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchWithPlayerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerSetupResponse;
import com.jumbo.trus.dto.beer.response.multi.BeerMultiAddResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.entity.repository.specification.BeerStatsSpecification;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.BeerMapper;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.MatchRepository;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.entity.repository.specification.BeerSpecification;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.NotificationService;
import com.jumbo.trus.service.PlayerService;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.order.OrderBeerByBeerAndLiquorNumberThenName;
import com.jumbo.trus.service.order.OrderBeerDetailedDTOByBeerAndLiquorNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BeerService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BeerMapper beerMapper;

    @Autowired
    private BeerDetailedMapper beerDetailedMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * metoda napamuje hráče a zápas z přepravky k pivu a uloží ho do DB
     * @param beerDTO Pivo, který přijde z FE
     * @return Pivo z DB
     */
    public BeerDTO addBeer(BeerDTO beerDTO) {
        BeerEntity entity = beerMapper.toEntity(beerDTO);
        mapPlayerAndMatch(entity, beerDTO);
        BeerEntity savedEntity = beerRepository.save(entity);
        return beerMapper.toDTO(savedEntity);
    }

    /**
     * Projde seznam piv u hráčů a v případě změny zapíše změny do db. Počet změn následně vypíše
     * @param beerListDTO List ve formě přepravky BeerListDTO, který přijde z FE. Obsahuje jak změněné počty piv u hráčů u konkrétního zápasu, tak může obsahovat i nezměněné počty
     * @return BeerMultiAddResponse - vypsaný počet změn v DB
     */
    public BeerMultiAddResponse addMultipleBeer(BeerListDTO beerListDTO) {
        StringBuilder newBeerNotification = new StringBuilder();
        StringBuilder newLiquorNotification = new StringBuilder();
        BeerMultiAddResponse beerMultiAddResponse = new BeerMultiAddResponse();
        beerMultiAddResponse.setMatch(matchRepository.getReferenceById(beerListDTO.getMatchId()).getName());
        for (BeerNoMatchDTO beerNoMatchDTO : beerListDTO.getBeerList()) {
            BeerDTO beerDTO = new BeerDTO(beerListDTO.getMatchId(), beerNoMatchDTO);
            BeerDTO oldBeer = getBeerDtoByPlayerAndMatch(beerListDTO.getMatchId(), beerNoMatchDTO.getPlayerId());
            if (oldBeer != null && (beerDTO.getBeerNumber() != oldBeer.getBeerNumber() || beerDTO.getLiquorNumber() != oldBeer.getLiquorNumber())) {
                beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber() - oldBeer.getBeerNumber(), beerDTO.getLiquorNumber() - oldBeer.getLiquorNumber(), false);
                beerDTO.setId(oldBeer.getId());
                saveBeerToRepository(beerDTO);
                String playerName = playerRepository.getReferenceById(beerNoMatchDTO.getPlayerId()).getName() + " vypil ";
                if (beerDTO.getBeerNumber() != oldBeer.getBeerNumber()) {
                    newBeerNotification.append(playerName).append("piv: ").append(beerDTO.getBeerNumber()).append("\n");
                }
                if (beerDTO.getLiquorNumber() != oldBeer.getLiquorNumber()) {
                    newLiquorNotification.append(playerName).append("panáků: ").append(beerDTO.getLiquorNumber()).append("\n");
                }
            } else if (oldBeer == null && (beerDTO.getBeerNumber() != 0 || beerDTO.getLiquorNumber() != 0)) {
                beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber(), beerDTO.getLiquorNumber(), true);
                saveBeerToRepository(beerDTO);
                String playerName = playerRepository.getReferenceById(beerNoMatchDTO.getPlayerId()).getName() + " vypil ";
                if (beerDTO.getBeerNumber() != 0) {
                    newBeerNotification.append(playerName).append("piv: ").append(beerDTO.getBeerNumber()).append("\n");
                }
                if (beerDTO.getLiquorNumber() != 0) {
                    newLiquorNotification.append(playerName).append("panáků: ").append(beerDTO.getLiquorNumber()).append("\n");
                }
            }
        }
        notificationService.addNotification("Přidáno pivka v zápase " + beerMultiAddResponse.getMatch(), newBeerNotification+newLiquorNotification.toString());
        return beerMultiAddResponse;
    }

    /**
     *
     * @param beerFilter filter, podle kterého se vrací počet záznamů
     * @return záznamy na základě filtru v parametru
     */
    public List<BeerDTO> getAll(BeerFilter beerFilter) {
        BeerSpecification beerSpecification = new BeerSpecification(beerFilter);
        return beerRepository.findAll(beerSpecification, PageRequest.of(0, beerFilter.getLimit())).stream().map(beerMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * metoda prohledá záznamy v DB
     * @param filter filter, podle kterého se vrací počet záznamů. Pomocí parametru matchStatsOrPlayerStats se určřuje, zda chceme statistiky z pohledu zápasu (true) či hráče (false)
     *               detailed se nepoužívá
     * @return Vrací rozšířený seznam vypitých piv z db dle filtru
     */
    public BeerDetailedResponse getAllDetailed(StatisticsFilter filter) {
        BeerDetailedResponse beerDetailedResponse = new BeerDetailedResponse();
        BeerStatsSpecification beerSpecification = new BeerStatsSpecification(filter);
        List<BeerDetailedDTO> beerList = beerRepository.findAll(beerSpecification, PageRequest.of(0, filter.getLimit())).stream().map(beerDetailedMapper::toDTO).toList();
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();
        HashMap<Long, BeerDetailedDTO> matchMap = new HashMap<>();
        HashMap<Long, BeerDetailedDTO> playerMap = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            beerDetailedResponse.addBeers(beer.getBeerNumber());
            beerDetailedResponse.addLiquors(beer.getLiquorNumber());
            matchSet.add(beer.getMatch().getId());
            playerSet.add(beer.getPlayer().getId());
            if (filter.getMatchStatsOrPlayerStats() != null && !filter.getMatchStatsOrPlayerStats()) {
                beer.setMatch(null);
                if (!playerMap.containsKey(beer.getPlayer().getId())) {
                    playerMap.put(beer.getPlayer().getId(), beer);
                }
                else {
                    BeerDetailedDTO oldBeer = playerMap.get(beer.getPlayer().getId());
                    oldBeer.addBeers(beer.getBeerNumber());
                    oldBeer.addLiquors(beer.getLiquorNumber());
                    playerMap.put(beer.getPlayer().getId(), oldBeer);
                }
            }
            if (filter.getMatchStatsOrPlayerStats() != null && filter.getMatchStatsOrPlayerStats()) {
                beer.setPlayer(null);
                if (!matchMap.containsKey(beer.getMatch().getId())) {
                    matchMap.put(beer.getMatch().getId(), beer);
                }
                else {
                    BeerDetailedDTO oldBeer = matchMap.get(beer.getMatch().getId());
                    oldBeer.addBeers(beer.getBeerNumber());
                    oldBeer.addLiquors(beer.getLiquorNumber());
                    matchMap.put(beer.getMatch().getId(), oldBeer);
                }
            }
        }
        List<BeerDetailedDTO> returnBeerList;
        if (filter.getMatchStatsOrPlayerStats() != null && filter.getMatchStatsOrPlayerStats()) {
            returnBeerList = new ArrayList<>(matchMap.values().stream().toList());
        }
        else if (filter.getMatchStatsOrPlayerStats() != null) {
            returnBeerList = new ArrayList<>(playerMap.values().stream().toList());
        }
        else {
            returnBeerList = new ArrayList<>(beerList);
        }
        returnBeerList.sort(new OrderBeerDetailedDTOByBeerAndLiquorNumber());
        beerDetailedResponse.setBeerList(returnBeerList);
        beerDetailedResponse.setMatchesCount(matchSet.size());
        beerDetailedResponse.setPlayersCount(playerSet.size());
        return beerDetailedResponse;
    }

    /**
     * Metoda vrátí setup piv, který se použije v objektu. Jedná se o počet rozpitých piv v daném zápase pro dané hráče dle filtru
     * @param beerFilter filter, podle kterého se vrací počet záznamů
     * @return BeerSetupResponse, kde lze najít počet piv a kořalek pro zápas a jedntlivé hráče
     */
    public BeerSetupResponse setupBeers(BeerFilter beerFilter) {
        PairSeasonMatch pairSeasonMatch = matchService.returnSeasonAndMatchByFilter(beerFilter);
        SeasonDTO seasonDTO = pairSeasonMatch.getSeasonDTO();
        MatchDTO matchDTO = pairSeasonMatch.getMatchDTO();
        MatchFilter matchFilter = new MatchFilter();
        matchFilter.setSeasonId(seasonDTO.getId());
        List<MatchDTO> matchList =  matchService.getAll((matchFilter));
        List<BeerNoMatchWithPlayerDTO> beerNoMatchWithPlayerDTOS = new ArrayList<>();
        if(matchDTO != null) {
            List<BeerDTO> beerList = getAll(new BeerFilter(matchDTO.getId()));
            List<PlayerDTO> playersInMatch = matchService.getPlayerListByMatchId(matchDTO.getId());
            List<PlayerDTO> addedPlayers = new ArrayList<>();
            for (BeerDTO beerDTO : beerList) {
                PlayerDTO playerDTO = playerService.getPlayer(beerDTO.getPlayerId());
                if (playersInMatch.contains(playerDTO)) {
                    beerNoMatchWithPlayerDTOS.add(new BeerNoMatchWithPlayerDTO(beerDTO.getId(), beerDTO.getBeerNumber(), beerDTO.getLiquorNumber(), playerDTO));
                    addedPlayers.add(playerDTO);
                }
            }
            for (PlayerDTO playerDTO : playersInMatch) {
                if (!addedPlayers.contains(playerDTO)) {
                    beerNoMatchWithPlayerDTOS.add(new BeerNoMatchWithPlayerDTO(0, 0, playerDTO));
                }
            }

            beerNoMatchWithPlayerDTOS.sort(new OrderBeerByBeerAndLiquorNumberThenName());

        }

        return new BeerSetupResponse(matchDTO, seasonDTO, beerNoMatchWithPlayerDTOS, matchList);
    }

    public void deleteBeer(Long beerId) {
        beerRepository.deleteById(beerId);
    }

    private void mapPlayerAndMatch(BeerEntity beer, BeerDTO beerDTO) {
        beer.setMatch(matchRepository.getReferenceById(beerDTO.getMatchId()));
        beer.setPlayer(playerRepository.getReferenceById(beerDTO.getPlayerId()));
    }

    private BeerEntity saveBeerToRepository(BeerDTO beerDTO) {
        BeerEntity entity = beerMapper.toEntity(beerDTO);
        mapPlayerAndMatch(entity, beerDTO);
        return beerRepository.save(entity);
    }


    /**
     * @param matchId  id zápasu
     * @param playerId id hráče
     * @return null pokud neexistuje
     */
    private BeerDTO getBeerDtoByPlayerAndMatch(Long matchId, Long playerId) {
        BeerFilter beerFilter = new BeerFilter(matchId, playerId);
        BeerSpecification beerSpecification = new BeerSpecification(beerFilter);
        List<BeerDTO> filterList = beerRepository.findAll(beerSpecification, PageRequest.of(0, 1)).stream().map(beerMapper::toDTO).toList();
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }
}
