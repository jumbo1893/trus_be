package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineAmount;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineNumber;
import com.jumbo.trus.service.receivedFine.ReceivedFineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineFactService {

    private final SeasonService seasonService;
    private final ReceivedFineService receivedFineService;

    public List<String> returnFineFacts(StatisticsFilter allSeasonPlayerFilter, StatisticsFilter allSeasonMatchFilter, StatisticsFilter currentSeasonMatchFilter) {
        List<String> fineFacts = new ArrayList<>();
        fineFacts.add(getPlayerWithMostFines(receivedFineService.getAllDetailed(allSeasonPlayerFilter))); //1
        fineFacts.add(getMatchWithMostFines(receivedFineService.getAllDetailed(allSeasonMatchFilter))); //2
        fineFacts.add(getPlayerWithMostFinesAmount(receivedFineService.getAllDetailed(allSeasonPlayerFilter))); //3
        fineFacts.add(getMatchWithMostFinesAmount(receivedFineService.getAllDetailed(allSeasonMatchFilter))); //4
        fineFacts.add(getNumberOfFinesInCurrentSeason(receivedFineService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//5
        fineFacts.add(getNumberOfFinesAmountInCurrentSeason(receivedFineService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//6
        fineFacts.add(getMatchWithMostFinesInCurrentSeason(receivedFineService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//7
        fineFacts.add(getMatchWithMostFinesAmountInCurrentSeason(receivedFineService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//8
        fineFacts.add(getSeasonWithMostFines(receivedFineService.getAllDetailed(allSeasonMatchFilter)));//9
        fineFacts.add(getSeasonWithMostFinesAmount(receivedFineService.getAllDetailed(allSeasonMatchFilter)));//10
        fineFacts.add(getAverageNumberOfFinesInMatchForPlayers(receivedFineService.getAllDetailed(allSeasonPlayerFilter)));//11
        fineFacts.add(getAverageNumberOfFinesAmountInMatchForPlayers(receivedFineService.getAllDetailed(allSeasonPlayerFilter)));//12
        fineFacts.add(getAverageNumberOfFinesInMatch(receivedFineService.getAllDetailed(allSeasonPlayerFilter)));//13
        fineFacts.add(getAverageNumberOfFinesAmountInMatch(receivedFineService.getAllDetailed(allSeasonPlayerFilter)));//14
        return fineFacts;
    }

    private String getPlayerWithMostFines(ReceivedFineDetailedResponse allFineDetailedResponseForPlayer) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForPlayer.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze nalézt hráče s nejvíce pokutami, protože ještě nikdo žádnou nedostal??!!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        return "Nejvíce pokut za historii dostal hráč " + fine.getPlayer().getName() + " s počtem " + fine.getFineNumber() + " pokut. Za pokladníka děkujeme!";
    }

    private String getMatchWithMostFines(ReceivedFineDetailedResponse allFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze nalézt zápas s nejvíce pokutami, protože ještě nikdo žádnou nedostal??!!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Nejvíce pokut v historii padlo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", s počtem " + fine.getFineNumber() + " pokut. Za pokladníka děkujeme!";
    }

    private String getPlayerWithMostFinesAmount(ReceivedFineDetailedResponse allFineDetailedResponseForPlayer) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForPlayer.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineAmount());
        if (fineList.isEmpty()) {
            return "Nelze nalézt hráče, co nejvíc zaplatil na pokutách, protože ještě nikdo žádnou nedostal??!!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        return "Nejvíce pokuty stály hráče " + fine.getPlayer().getName() + ", který celkem zacáloval " + fine.getFineAmount() + "Kč pokut. Nezapomeň, je to nedílná součást tréningového procesu!";
    }

    private String getMatchWithMostFinesAmount(ReceivedFineDetailedResponse allFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineAmount());
        if (fineList.isEmpty()) {
            return "Nelze nalézt zápas, kde se nejvíc cálovalo za pokuty, protože ještě nikdo žádnou nedostal??!!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Největší objem peněz v pokutách přinesl zápas " + matchHelper.getMatchWithOpponentNameAndDate() + " kdy se vybralo " + fine.getFineAmount() + " Kč.";
    }

    private String getNumberOfFinesInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch, AppTeamEntity appTeam) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " se rozdalo již " + currentSeasonFineDetailedResponseForMatch.getFinesNumber() + " pokut.";
    }

    private String getNumberOfFinesAmountInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch, AppTeamEntity appTeam) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " se na pokutách vybralo již " + currentSeasonFineDetailedResponseForMatch.getFinesAmount() + " Kč.";
    }

    private String getMatchWithMostFinesInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch, AppTeamEntity appTeam) {
        List<ReceivedFineDetailedDTO> fineList = currentSeasonFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze najít zápas s nejvíce pokutami odehraný v této sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + ", protože se zatím žádná pokuta neudělila!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Nejvíce pokut v aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " se rozdalo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ". Konečné číslo počtu pokut zní " + fine.getFineNumber() + ".";
    }

    private String getMatchWithMostFinesAmountInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch, AppTeamEntity appTeam) {
        List<ReceivedFineDetailedDTO> fineList = currentSeasonFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze najít zápas, kde se v této sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + ", nejvíce cálovalo za pokuty, protože se zatím žádná pokuta neudělila!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Největší objem peněz v aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " přinesl zápas " + matchHelper.getMatchWithOpponentNameAndDate() + ", kde se vybralo " + fine.getFineAmount() + " Kč.";
    }

    private String getSeasonWithMostFines(ReceivedFineDetailedResponse allFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForMatch.getFineList();
        if (fineList.isEmpty()) {
            return "Nelze najít sezonu s nejvíce pokutama, protože se zatím žádná pokuta nerozdala!";
        }
        HashMap<Long, ReceivedFineDetailedDTO> seasonMap = new HashMap<>();
        for (ReceivedFineDetailedDTO fine : fineList) {
            if (!seasonMap.containsKey(fine.getMatch().getSeasonId())) {
                seasonMap.put(fine.getMatch().getSeasonId(), fine);
            }
            else {
                ReceivedFineDetailedDTO oldFine = seasonMap.get(fine.getMatch().getSeasonId());
                oldFine.addFineNumber(fine.getFineNumber());
                oldFine.addFineAmount(fine.getFineAmount());
                seasonMap.put(fine.getMatch().getSeasonId(), oldFine);
            }
        }
        List<ReceivedFineDetailedDTO> seasonFineList = new ArrayList<>(seasonMap.values().stream().toList());
        seasonFineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        Long seasonId  = seasonFineList.get(0).getMatch().getSeasonId();
        StatisticsFilter filter = new StatisticsFilter(null, null, seasonId, true);
        ReceivedFineDetailedResponse seasonFineDetail = receivedFineService.getAllDetailed(filter);
        SeasonDTO season = seasonService.getSeason(seasonId);
        return "Nejvíce pokut se rozdalo v sezoně " + season.getName() + ", kdy v " + seasonFineDetail.getMatchesCount() + " zápasech padlo " + seasonFineDetail.getFinesNumber() + " pokut.";
    }

    private String getSeasonWithMostFinesAmount(ReceivedFineDetailedResponse allFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = allFineDetailedResponseForMatch.getFineList();
        if (fineList.isEmpty()) {
            return "Nelze najít sezonu kdy se vybralo nejvíc za pokuty, protože se zatím žádná pokuta nerozdala!";
        }
        HashMap<Long, ReceivedFineDetailedDTO> seasonMap = new HashMap<>();
        for (ReceivedFineDetailedDTO fine : fineList) {
            if (!seasonMap.containsKey(fine.getMatch().getSeasonId())) {
                seasonMap.put(fine.getMatch().getSeasonId(), fine);
            }
            else {
                ReceivedFineDetailedDTO oldFine = seasonMap.get(fine.getMatch().getSeasonId());
                oldFine.addFineNumber(fine.getFineNumber());
                oldFine.addFineAmount(fine.getFineAmount());
                seasonMap.put(fine.getMatch().getSeasonId(), oldFine);
            }
        }
        List<ReceivedFineDetailedDTO> seasonFineList = new ArrayList<>(seasonMap.values().stream().toList());
        seasonFineList.sort(new OrderReceivedFineDetailedDTOByFineAmount());
        Long seasonId  = seasonFineList.get(0).getMatch().getSeasonId();
        StatisticsFilter filter = new StatisticsFilter(null, null, seasonId, true);
        ReceivedFineDetailedResponse seasonFineDetail = receivedFineService.getAllDetailed(filter);
        SeasonDTO season = seasonService.getSeason(seasonId);
        return "Nejvíc peněz na pokutách se vybralo v sezoně " + season.getName() + ", kdy v " + seasonFineDetail.getMatchesCount() + " zápasech pokladna shrábla " + seasonFineDetail.getFinesAmount() + " Kč.";
    }

    private String getAverageNumberOfFinesInMatchForPlayers(ReceivedFineDetailedResponse getAllFineDetailedResponseForPlayer) {
        float averageNumber = (float) getAllFineDetailedResponseForPlayer.getFinesNumber() /getAllFineDetailedResponseForPlayer.getMatchesCount()/getAllFineDetailedResponseForPlayer.getPlayersCount();
        return "V historii v průměru připadá " + averageNumber + " pokut na hráče pro každý zápas";
    }

    private String getAverageNumberOfFinesAmountInMatchForPlayers(ReceivedFineDetailedResponse getAllFineDetailedResponseForPlayer) {
        float averageNumber = (float) getAllFineDetailedResponseForPlayer.getFinesAmount() /getAllFineDetailedResponseForPlayer.getMatchesCount()/getAllFineDetailedResponseForPlayer.getPlayersCount();
        return "V historii v průměru připadá " + averageNumber + " Kč zaplaceno na pokutách na hráče pro každý zápas";
    }

    private String getAverageNumberOfFinesInMatch(ReceivedFineDetailedResponse getAllFineDetailedResponseForPlayer) {
        float averageNumber = (float) getAllFineDetailedResponseForPlayer.getFinesNumber() /getAllFineDetailedResponseForPlayer.getMatchesCount();
        return "V naprosto průměrném zápasu Trusu se udělí " + averageNumber + " pokut";
    }

    private String getAverageNumberOfFinesAmountInMatch(ReceivedFineDetailedResponse getAllFineDetailedResponseForPlayer) {
        float averageNumber = (float) getAllFineDetailedResponseForPlayer.getFinesAmount() /getAllFineDetailedResponseForPlayer.getMatchesCount();
        return "V naprosto průměrném zápasu Trusu se vybere " + averageNumber + " Kč na pokutách";
    }
}
