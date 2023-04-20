package com.jumbo.trus.service;

import com.jumbo.trus.dto.beer.*;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.BeerMapper;
import com.jumbo.trus.entity.BeerEntity;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.repository.BeerRepository;
import com.jumbo.trus.entity.repository.MatchRepository;
import com.jumbo.trus.entity.repository.PlayerRepository;
import com.jumbo.trus.entity.repository.specification.BeerSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private BeerMapper beerMapper;

    @Autowired
    private BeerDetailedMapper beerDetailedMapper;

    public BeerDTO addBeer(BeerDTO beerDTO) {
        BeerEntity entity = beerMapper.toEntity(beerDTO);
        mapPlayerAndMatch(entity, beerDTO);
        BeerEntity savedEntity = beerRepository.save(entity);
        return beerMapper.toDTO(savedEntity);
    }

    public BeerMultiAddResponse addMultipleBeer(BeerListDTO beerListDTO) {
        BeerMultiAddResponse beerMultiAddResponse = new BeerMultiAddResponse();
        beerMultiAddResponse.setMatch(matchRepository.getReferenceById(beerListDTO.getMatchId()).getName());
        for (BeerNoMatchDTO beerNoMatchDTO : beerListDTO.getBeerList()) {
            BeerDTO beerDTO = new BeerDTO(beerListDTO.getMatchId(), beerNoMatchDTO);
            BeerDTO oldBeer = getBeerDtoByPlayerAndMatch(beerListDTO.getMatchId(), beerNoMatchDTO.getPlayerId());
            if (oldBeer != null && (beerDTO.getBeerNumber() != oldBeer.getBeerNumber() || beerDTO.getLiquorNumber() != oldBeer.getLiquorNumber())) {
                beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber() - oldBeer.getBeerNumber(), beerDTO.getLiquorNumber() - oldBeer.getLiquorNumber(), false);
                beerDTO.setId(oldBeer.getId());
                saveBeerToRepository(beerDTO).getMatch().getName();
            } else if (oldBeer == null && (beerDTO.getBeerNumber() != 0 || beerDTO.getLiquorNumber() != 0)) {
                beerMultiAddResponse.addBeersLiquorsAndPlayer(beerDTO.getBeerNumber(), beerDTO.getLiquorNumber(), true);
                saveBeerToRepository(beerDTO).getMatch().getName();
            }
        }
        return beerMultiAddResponse;
    }

    public List<BeerDTO> getAll(BeerFilter beerFilter) {
        BeerSpecification beerSpecification = new BeerSpecification(beerFilter);
        return beerRepository.findAll(beerSpecification, PageRequest.of(0, beerFilter.getLimit())).stream().map(beerMapper::toDTO).collect(Collectors.toList());
    }

    public BeerDetailedResponse getAllDetailed(BeerFilter beerFilter) {
        BeerDetailedResponse beerDetailedResponse = new BeerDetailedResponse();
        BeerSpecification beerSpecification = new BeerSpecification(beerFilter);
        List<BeerDetailedDTO> beerList = beerRepository.findAll(beerSpecification, PageRequest.of(0, beerFilter.getLimit())).stream().map(beerDetailedMapper::toDTO).collect(Collectors.toList());
        Set<Long> matchSet = new HashSet<>();
        Set<Long> playerSet = new HashSet<>();
        for (BeerDetailedDTO beer : beerList) {
            beerDetailedResponse.addBeers(beer.getBeerNumber());
            beerDetailedResponse.addLiquors(beer.getLiquorNumber());
            matchSet.add(beer.getMatch().getId());
            playerSet.add(beer.getPlayer().getId());
        }
        beerDetailedResponse.setBeerList(beerList);
        beerDetailedResponse.setMatchesCount(matchSet.size());
        beerDetailedResponse.setPlayersCount(playerSet.size());
        return beerDetailedResponse;
    }

    public void deleteMatch(Long beerId) {
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
        List<BeerDTO> filterList = beerRepository.findAll(beerSpecification, PageRequest.of(0, 1)).stream().map(beerMapper::toDTO).collect(Collectors.toList());
        if (filterList.isEmpty()) {
            return null;
        }
        return filterList.get(0);
    }
}
