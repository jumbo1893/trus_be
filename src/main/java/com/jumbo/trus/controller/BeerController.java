package com.jumbo.trus.controller;

import com.jumbo.trus.aspect.PostCommitTask;
import com.jumbo.trus.aspect.appteam.StoreAppTeam;
import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.multi.BeerListDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerSetupResponse;
import com.jumbo.trus.dto.beer.response.multi.BeerMultiAddResponse;
import com.jumbo.trus.dto.stats.StatsDTO;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.auth.AppTeamService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.beer.BeerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/beer")
@RequiredArgsConstructor
public class BeerController {

    private final BeerService beerService;
    private final BeerStatsService beerStatsService;
    private final AppTeamService appTeamService;

    @RoleRequired("ADMIN")
    @PostMapping("/add")
    @PostCommitTask
    @StoreAppTeam
    public BeerDTO addBeer(@RequestBody BeerDTO beerDTO) {
        return beerService.addBeer(beerDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("READER")
    @GetMapping("/get-all")
    public List<BeerDTO> getBeers(BeerFilter beerFilter) {
        beerFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return beerService.getAll(beerFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/get-all-detailed")
    public BeerDetailedResponse getDetailedBeers(StatisticsFilter filter) {
        filter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return beerService.getAllDetailed(filter);
    }

    @RoleRequired("ADMIN")
    @PostMapping("/multiple-add")
    @PostCommitTask
    @StoreAppTeam
    public BeerMultiAddResponse addMultipleBeer(@RequestBody BeerListDTO beerListDTO) {
        return beerService.addMultipleBeer(beerListDTO, appTeamService.getCurrentAppTeamOrThrow());
    }

    @RoleRequired("ADMIN")
    @DeleteMapping("/{beerId}")
    @PostCommitTask
    @StoreAppTeam
    public void deleteMatch(@PathVariable Long beerId) throws NotFoundException {
        beerService.deleteBeer(beerId);
    }

    @RoleRequired("READER")
    @GetMapping("/setup")
    public BeerSetupResponse setupBeers(BeerFilter beerFilter) {
        beerFilter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return beerService.setupBeers(beerFilter);
    }

    @RoleRequired("READER")
    @GetMapping("/stats")
    public List<StatsDTO> getBeerStats(StatisticsFilter filter) {
        filter.setAppTeam(appTeamService.getCurrentAppTeamOrThrow());
        return beerStatsService.getBeerStatistics(filter);
    }
}
