package com.jumbo.trus.service.beer;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.multi.BeerListDTO;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchDTO;
import com.jumbo.trus.dto.beer.multi.BeerNoMatchWithPlayerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerSetupResponse;
import com.jumbo.trus.dto.beer.response.multi.BeerMultiAddResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.BeerMapper;
import com.jumbo.trus.repository.BeerRepository;
import com.jumbo.trus.repository.specification.BeerSpecification;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.helper.DetailedResponseHelper;
import com.jumbo.trus.service.helper.PairSeasonMatch;
import com.jumbo.trus.service.notification.NotificationService;
import com.jumbo.trus.service.notification.push.BeerNotificationMaker;
import com.jumbo.trus.service.order.OrderBeerByBeerAndLiquorNumberThenName;
import com.jumbo.trus.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeerService {

    private final BeerRepository beerRepository;
    private final MatchService matchService;
    private final PlayerService playerService;
    private final BeerMapper beerMapper;
    private final NotificationService notificationService;
    private final DetailedResponseHelper detailedResponseHelper;
    private final BeerNotificationMaker beerNotificationMaker;

    /**
     * metoda napamuje hráče a zápas z přepravky k pivu a uloží ho do DB
     * @param beerDTO Pivo, který přijde z FE
     * @return Pivo z DB
     */
    public BeerDTO addBeer(BeerDTO beerDTO, AppTeamEntity appTeam) {
        return beerMapper.toDTO(saveBeerToRepository(beerDTO, null, appTeam));
    }

    /**
     * Projde seznam piv u hráčů a v případě změny zapíše změny do db. Počet změn následně vypíše
     * @param beerListDTO List ve formě přepravky BeerListDTO, který přijde z FE. Obsahuje jak změněné počty piv u hráčů u konkrétního zápasu, tak může obsahovat i nezměněné počty
     * @return BeerMultiAddResponse - vypsaný počet změn v DB
     */
    public BeerMultiAddResponse addMultipleBeer(BeerListDTO beerListDTO, AppTeamEntity appTeam) {
        StringBuilder newBeerNotification = new StringBuilder();
        StringBuilder newLiquorNotification = new StringBuilder();
        BeerMultiAddResponse beerMultiAddResponse = new BeerMultiAddResponse();
        beerMultiAddResponse.setMatch(matchService.getMatch(beerListDTO.getMatchId()).getName());
        for (BeerNoMatchDTO beerNoMatchDTO : beerListDTO.getBeerList()) {
            processBeer(beerNoMatchDTO, beerListDTO.getMatchId(), newBeerNotification, newLiquorNotification, beerMultiAddResponse, appTeam);
        }
        notificationService.addNotification("Přidáno pivka v zápase " + beerMultiAddResponse.getMatch(), newBeerNotification+newLiquorNotification.toString());
        return beerMultiAddResponse;
    }

    private void processBeer(BeerNoMatchDTO beerNoMatchDTO, Long matchId, StringBuilder newBeerNotification, StringBuilder newLiquorNotification,
                             BeerMultiAddResponse beerMultiAddResponse, AppTeamEntity appTeam) {
        BeerDTO beerDTO = new BeerDTO(matchId, beerNoMatchDTO);
        BeerDTO oldBeer = getBeerDtoByPlayerAndMatch(matchId, beerNoMatchDTO.getPlayerId());
        if (isNeededToRewriteBeer(oldBeer, beerDTO)) {
            beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber() - oldBeer.getBeerNumber(), beerDTO.getLiquorNumber() - oldBeer.getLiquorNumber(), false);
            beerDTO.setId(oldBeer.getId());
            saveBeerToRepository(beerDTO, oldBeer, appTeam);
            setMultiBeerNotification(newBeerNotification, newLiquorNotification, beerDTO, oldBeer);
        } else if (isNeededToAddNewBeer(oldBeer, beerDTO)) {
            beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber(), beerDTO.getLiquorNumber(), true);
            saveBeerToRepository(beerDTO, null, appTeam);
            setMultiBeerNotification(newBeerNotification, newLiquorNotification, beerDTO, null);
        }
    }

    private void setMultiBeerNotification(StringBuilder newBeerNotification, StringBuilder newLiquorNotification, BeerDTO beerDTO, BeerDTO oldBeer) {
        String playerName = playerService.getPlayer(beerDTO.getPlayerId()).getName();
        if ((oldBeer != null && beerDTO.getBeerNumber() != oldBeer.getBeerNumber()) || (oldBeer == null && beerDTO.getBeerNumber() != 0)) {
            newBeerNotification.append(playerName).append(" vypil piv: ").append(beerDTO.getBeerNumber()).append("\n");
        }
        if ((oldBeer != null && beerDTO.getLiquorNumber() != oldBeer.getBeerNumber()) || (oldBeer == null && beerDTO.getLiquorNumber() != 0)) {
            newLiquorNotification.append(playerName).append(" vypil panáků: ").append(beerDTO.getLiquorNumber()).append("\n");
        }
    }

    private boolean isNeededToRewriteBeer(BeerDTO oldBeer, BeerDTO beerDTO) {
        return oldBeer != null && (beerDTO.getBeerNumber() != oldBeer.getBeerNumber() || beerDTO.getLiquorNumber() != oldBeer.getLiquorNumber());
    }

    private boolean isNeededToAddNewBeer(BeerDTO oldBeer, BeerDTO beerDTO) {
        return oldBeer == null && (beerDTO.getBeerNumber() != 0 || beerDTO.getLiquorNumber() != 0);
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
        return new BeerDetailedResponse(detailedResponseHelper.getAllDetailed(filter, DetailedResponseHelper.DetailedType.BEER));
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
        matchFilter.setAppTeam(beerFilter.getAppTeam());
        List<MatchDTO> matchList =  matchService.getAll((matchFilter));
        List<BeerNoMatchWithPlayerDTO> beerNoMatchWithPlayerDTOS = new ArrayList<>();
        if(matchDTO != null) {
            BeerFilter matchBeerFilter = new BeerFilter();
            matchBeerFilter.setMatchId(matchDTO.getId());
            matchBeerFilter.setAppTeam(beerFilter.getAppTeam());
            List<BeerDTO> beerList = getAll(matchBeerFilter);
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

    public List<BeerDTO> getTopDrinkersByMatch(long appTeamId) {
        return beerRepository.findTopDrinkersByMatchOrderedByDate(appTeamId).stream().map(beerMapper::toDTO).toList();
    }

    public BeerDTO getFirstMatchWhereLiquorMoreThanBeer(Long playerId) {
        return beerRepository.findFirstMatchWhereLiquorMoreThanBeer(playerId)
                .map(beerMapper::toDTO)
                .orElse(null);
    }

    public BeerDTO getFirstMatchWhereAtLeastBeersWithFine(Long playerId, String fineName, int beerNumber) {
        return beerRepository.findFirstMatchWhereAtLeastBeersAfterFine(playerId, fineName, beerNumber)
                .map(beerMapper::toDTO)
                .orElse(null);
    }

    public BeerDTO getFirstBeerIfPlayerDrinksAtLeastXLiquorsAndThenNotAttendInNextMatch(Long playerId, int liquorNumber) {
        return beerRepository.findBeerIfPlayerDrinksAtLeastXLiquorsAndThenNotAttendInNextMatch(playerId, liquorNumber)
                .map(beerMapper::toDTO)
                .orElse(null);
    }


    private void mapPlayerAndMatch(BeerEntity beer, BeerDTO beerDTO) {
        beer.setMatch(matchService.getMatchEntity(beerDTO.getMatchId()));
        beer.setPlayer(playerService.getPlayerEntity(beerDTO.getPlayerId()));
    }

    private BeerEntity saveBeerToRepository(BeerDTO beerDTO, BeerDTO oldBeer, AppTeamEntity appTeam) {
        BeerEntity entity = beerMapper.toEntity(beerDTO);
        entity.setAppTeam(appTeam);
        mapPlayerAndMatch(entity, beerDTO);
        BeerEntity returnEntity = beerRepository.save(entity);
        beerNotificationMaker.sendBeerNotify(entity, oldBeer);
        return returnEntity;
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
