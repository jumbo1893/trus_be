package com.jumbo.trus.service.football.league;

import com.jumbo.trus.dto.football.LeagueDTO;
import com.jumbo.trus.service.UpdateService;
import com.jumbo.trus.service.football.pkfl.task.RetrievePkflLeagues;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeagueRetriever {

    private final UpdateService updateService;
    private final RetrievePkflLeagues retrievePkflLeagues;

    private static final String LEAGUE_UPDATE = "league_update";

    public List<LeagueDTO> retrieveLeagues() {
        if (isNeededToLoadAllLeagues()) {
            List<LeagueDTO> leaguesFromWeb = retrievePkflLeagues.getAllPastLeagues();
            if (!leaguesFromWeb.isEmpty()) {
                updateService.saveNewUpdate(LEAGUE_UPDATE);
            }
            return leaguesFromWeb;
        }
        return retrievePkflLeagues.getLeagues(null);
    }

    private boolean isNeededToLoadAllLeagues() {
        return updateService.getUpdateByName(LEAGUE_UPDATE) == null;
    }
}
