package com.jumbo.trus.service;

import com.jumbo.trus.config.Config;
import com.jumbo.trus.dto.HomeSetup;
import com.jumbo.trus.dto.PlayerDTO;
import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedDTO;
import com.jumbo.trus.dto.receivedfine.response.get.detailed.ReceivedFineDetailedResponse;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.SeasonFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.mapper.BeerDetailedMapper;
import com.jumbo.trus.mapper.SeasonMapper;
import com.jumbo.trus.service.fact.BeerFact;
import com.jumbo.trus.service.fact.FineFact;
import com.jumbo.trus.service.fact.RandomFact;
import com.jumbo.trus.service.helper.AverageNumberTotalNumber;
import com.jumbo.trus.service.order.OrderBeerByAttendance;
import com.jumbo.trus.service.order.OrderBeerByBeerOrLiquorNumber;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineAmount;
import com.jumbo.trus.service.order.OrderReceivedFineDetailedDTOByFineNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.jumbo.trus.config.Config.OTHER_SEASON_ID;

@Service
public class HomeService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private SeasonService seasonService;

    @Autowired
    private SeasonMapper seasonMapper;

    @Autowired
    private BeerService beerService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private ReceivedFineService receivedFineService;

    @Autowired
    private BeerDetailedMapper beerDetailedMapper;

    public HomeSetup setup() {
        HomeSetup homeSetup = new HomeSetup();
        homeSetup.setNextBirthday(getUpcomingBirthday());
        homeSetup.setRandomFacts(getRandomFacts());
        return homeSetup;
    }

    private String getUpcomingBirthday() {
        return playerService.returnNextPlayerBirthdayFromList();
    }

    private List<String> getRandomFacts() {
        SeasonDTO currentSeason = seasonService.getCurrentSeason();
        StatisticsFilter allSeasonPlayerFilter = new StatisticsFilter(null, null, Config.ALL_SEASON_ID, false);
        StatisticsFilter allSeasonMatchFilter = new StatisticsFilter(null, null, Config.ALL_SEASON_ID, true);
        StatisticsFilter currentSeasonMatchFilter = new StatisticsFilter(null, null, currentSeason.getId(), true);
        StatisticsFilter currentSeasonPlayerFilter = new StatisticsFilter(null, null, currentSeason.getId(), false);
        BeerFact beerFact = new BeerFact(beerService.getAllDetailed(allSeasonMatchFilter), beerService.getAllDetailed(currentSeasonMatchFilter),
                beerService.getAllDetailed(allSeasonPlayerFilter), beerService.getAllDetailed(currentSeasonPlayerFilter));
        FineFact fineFact = new FineFact(receivedFineService.getAllDetailed(allSeasonMatchFilter), receivedFineService.getAllDetailed(currentSeasonMatchFilter),
                receivedFineService.getAllDetailed(allSeasonPlayerFilter), receivedFineService.getAllDetailed(currentSeasonPlayerFilter));
        List<String> returnList = new ArrayList<>(returnBeerFacts(beerFact));
        returnList.addAll(returnFineFacts(fineFact));
        return returnList;
    }

    public List<String> returnBeerFacts(BeerFact beerFact) {
        List<String> beerFacts = new ArrayList<>();
        beerFacts.add(getPlayerWithMostBeers(beerFact.getAllBeerDetailedResponseForPlayer())); //1
        beerFacts.add(getMatchWithMostBeers(beerFact.getAllBeerDetailedResponseForMatch())); //2
        beerFacts.add(getNumberOfBeersInCurrentSeason(beerFact.getCurrentSeasonBeerDetailedResponseForMatch()));//3
        beerFacts.add(getMatchWithMostBeersInCurrentSeason(beerFact.getCurrentSeasonBeerDetailedResponseForMatch()));//4
        beerFacts.add(getSeasonWithMostBeers(beerFact.getAllBeerDetailedResponseForMatch()));//5
        beerFacts.add(getAverageNumberOfBeersInMatchForPlayersAndFans(beerFact.getAllBeerDetailedResponseForPlayer()));//6
        beerFacts.add(getAverageNumberOfBeersInMatchForPlayers(beerFact.getAllBeerDetailedResponseForPlayer()));//7
        beerFacts.add(getAverageNumberOfBeersInMatchForFans(beerFact.getAllBeerDetailedResponseForPlayer()));//8
        beerFacts.add(getAverageNumberOfBeersInMatch(beerFact.getAllBeerDetailedResponseForPlayer()));//9
        beerFacts.add(getMatchWithHighestAverageBeers(beerFact.getAllBeerDetailedResponseForMatch()));//10
        beerFacts.add(getMatchWithLowestAverageBeers(beerFact.getAllBeerDetailedResponseForMatch()));//11
        beerFacts.add(getHighestAttendanceInMatch(beerFact.getAllBeerDetailedResponseForMatch()));//12
        beerFacts.add(getLowestAttendanceInMatch(beerFact.getAllBeerDetailedResponseForMatch()));//13
        ////panáky
        beerFacts.add(getPlayerWithMostLiquors(beerFact.getAllBeerDetailedResponseForPlayer())); //14
        beerFacts.add(getMatchWithMostLiquors(beerFact.getAllBeerDetailedResponseForMatch())); //15
        beerFacts.add(getNumberOfLiquorsInCurrentSeason(beerFact.getCurrentSeasonBeerDetailedResponseForMatch()));//16
        beerFacts.add(getMatchWithMostLiquorsInCurrentSeason(beerFact.getCurrentSeasonBeerDetailedResponseForMatch()));//17
        beerFacts.add(getSeasonWithMostLiquors(beerFact.getAllBeerDetailedResponseForMatch()));//18
        beerFacts.add(getAverageNumberOfLiquorsInMatchForPlayersAndFans(beerFact.getAllBeerDetailedResponseForPlayer()));//19
        beerFacts.add(getAverageNumberOfLiquorsInMatchForPlayers(beerFact.getAllBeerDetailedResponseForPlayer()));//20
        beerFacts.add(getAverageNumberOfLiquorsInMatchForFans(beerFact.getAllBeerDetailedResponseForPlayer()));//21
        beerFacts.add(getAverageNumberOfLiquorsInMatch(beerFact.getAllBeerDetailedResponseForPlayer()));//22
        beerFacts.add(getMatchWithHighestAverageLiquors(beerFact.getAllBeerDetailedResponseForMatch()));//23
        beerFacts.add(getMatchWithLowestAverageLiquors(beerFact.getAllBeerDetailedResponseForMatch()));//24
        //pivo
        beerFacts.add(getAverageNumberOfBeersInHomeAndAwayMatch(beerFact.getAllBeerDetailedResponseForMatch()));//25
        //panák
        beerFacts.add(getAverageNumberOfLiquorsInHomeAndAwayMatch(beerFact.getAllBeerDetailedResponseForMatch()));//26
        //narozky
        beerFacts.add(getMatchWithBirthday());//27
        return beerFacts;
    }

    public List<String> returnFineFacts(FineFact fineFact) {
        List<String> fineFacts = new ArrayList<>();
        fineFacts.add(getPlayerWithMostFines(fineFact.getAllFineDetailedResponseForPlayer())); //1
        fineFacts.add(getMatchWithMostFines(fineFact.getAllFineDetailedResponseForMatch())); //2
        fineFacts.add(getPlayerWithMostFinesAmount(fineFact.getAllFineDetailedResponseForPlayer())); //3
        fineFacts.add(getMatchWithMostFinesAmount(fineFact.getAllFineDetailedResponseForMatch())); //4
        fineFacts.add(getNumberOfFinesInCurrentSeason(fineFact.getCurrentSeasonFineDetailedResponseForMatch()));//5
        fineFacts.add(getNumberOfFinesAmountInCurrentSeason(fineFact.getCurrentSeasonFineDetailedResponseForMatch()));//6
        fineFacts.add(getMatchWithMostFinesInCurrentSeason(fineFact.getCurrentSeasonFineDetailedResponseForMatch()));//7
        fineFacts.add(getMatchWithMostFinesAmountInCurrentSeason(fineFact.getCurrentSeasonFineDetailedResponseForMatch()));//8
        fineFacts.add(getSeasonWithMostFines(fineFact.getAllFineDetailedResponseForMatch()));//9
        fineFacts.add(getSeasonWithMostFinesAmount(fineFact.getAllFineDetailedResponseForMatch()));//10
        fineFacts.add(getAverageNumberOfFinesInMatchForPlayers(fineFact.getAllFineDetailedResponseForPlayer()));//11
        fineFacts.add(getAverageNumberOfFinesAmountInMatchForPlayers(fineFact.getAllFineDetailedResponseForPlayer()));//12
        fineFacts.add(getAverageNumberOfFinesInMatch(fineFact.getAllFineDetailedResponseForPlayer()));//13
        fineFacts.add(getAverageNumberOfFinesAmountInMatch(fineFact.getAllFineDetailedResponseForPlayer()));//14
        return fineFacts;
    }


    /////////////////////////////////pivka

    private String getPlayerWithMostBeers(BeerDetailedResponse allBeerDetailedResponseForPlayer) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForPlayer.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        if (beerList.isEmpty()) {
            return "Nelze najít největšího pijana, protože si ještě nikdo nedal pivo???!!";
        }
        BeerDetailedDTO beer = beerList.get(0);
        return "Nejvíce velkých piv za historii si dal " + beer.getPlayer().getName() + ", který vypil " + beer.getBeerNumber() + " piv.";
    }

    private String getMatchWithMostBeers(BeerDetailedResponse allBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejvíce pivy, protože si zatím nikdo žádný nedal!";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce piv za historii bylo vypito v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kde padlo " + beer.getBeerNumber() + " piv.";
    }

    private String getNumberOfBeersInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason().getName() + " se vypilo celkem " + currentSeasonBeerDetailedResponseForMatch.getTotalBeers() + " piv.";
    }

    private String getMatchWithMostBeersInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = currentSeasonBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejvíce pivy v této sezoně " + seasonService.getCurrentSeason().getName() + ", protože si zatím nikdo žádný nedal!";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce piv v aktuální sezoně " + seasonService.getCurrentSeason().getName() + " bylo vypito v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kde padlo " + beer.getBeerNumber() + " piv.";
    }

    private String getSeasonWithMostBeers(BeerDetailedResponse allBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze najít sezonu s nejvíce pivy, protože si zatím nikdo pivo nedal!!!";
        }
        HashMap<Long, BeerDetailedDTO> seasonMap = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            if (!seasonMap.containsKey(beer.getMatch().getSeasonId())) {
                seasonMap.put(beer.getMatch().getSeasonId(), beer);
            }
            else {
                BeerDetailedDTO oldBeer = seasonMap.get(beer.getMatch().getSeasonId());
                oldBeer.addBeers(beer.getBeerNumber());
                oldBeer.addLiquors(beer.getLiquorNumber());
                seasonMap.put(beer.getMatch().getSeasonId(), oldBeer);
            }
        }
        List<BeerDetailedDTO> seasonBeerList = new ArrayList<>(seasonMap.values().stream().toList());
        seasonBeerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        Long seasonId  = seasonBeerList.get(0).getMatch().getSeasonId();
        StatisticsFilter filter = new StatisticsFilter(null, null, seasonId, true);
        BeerDetailedResponse seasonBeerDetail = beerService.getAllDetailed(filter);
        SeasonDTO season = seasonService.getSeason(seasonId);
        return "Nejvíce piv se vypilo v sezoně " + season.getName() + ", kdy se v " + seasonBeerDetail.getMatchesCount() + " zápasech vypilo " + seasonBeerDetail.getTotalBeers() + " piv.";
    }

    private String getAverageNumberOfBeersInMatchForPlayersAndFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) getAllBeerDetailedResponseForPlayer.getTotalBeers() /getAllBeerDetailedResponseForPlayer.getMatchesCount()/getAllBeerDetailedResponseForPlayer.getPlayersCount();
        return "Za celou historii průměrně každý hráč a fanoušek Trusu, který byl po zápase v hospodě, vypil " + averageNumber + " piv za zápas";
    }

    private String getAverageNumberOfBeersInMatchForPlayers(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = returnNumberOfBeersForFansOrPlayers(false, true, getAllBeerDetailedResponseForPlayer).getAverage() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Za celou historii průměrně každý hráč Trusu, který byl po zápase v hospodě, vypil " + averageNumber + " piv za zápas";
    }

    private String getAverageNumberOfBeersInMatchForFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = returnNumberOfBeersForFansOrPlayers(true, true, getAllBeerDetailedResponseForPlayer).getAverage() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Za celou historii průměrně každý fanoušek Trusu, který byl po zápase v hospodě, vypil " + averageNumber + " piv za zápas";
    }

    private String getAverageNumberOfBeersInMatch(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) getAllBeerDetailedResponseForPlayer.getTotalBeers() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně za celou historii se v jednom obyčejném zápase vypije " + averageNumber + " piv";
    }

    private String getMatchWithHighestAverageBeers(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejvíce průměrnými pivy, protože si zatím nikdo pivo nedal!!!";
        }
        AverageNumberTotalNumber highestAverage = new AverageNumberTotalNumber(0,0);
        MatchDTO matchDTO = null;
        for (BeerDetailedDTO beer : beerList) {
            AverageNumberTotalNumber averageNumberTotalNumber = new AverageNumberTotalNumber(beer.getBeerNumber(), beer.getMatch().getPlayerIdList().size());
            if (averageNumberTotalNumber.getAverage() > highestAverage.getAverage() || matchDTO == null) {
                highestAverage = averageNumberTotalNumber;
                matchDTO = beer.getMatch();
            }
        }
        MatchHelper matchHelper = new MatchHelper(matchDTO);
        return "Nejvyšší průměr počtu vypitých piv v zápase proběhl na zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ". Vypilo se " + highestAverage.getTotalNumber1() + " piv v " + highestAverage.getTotalNumber2()
                + " lidech, což dělá průměr " + highestAverage.getAverage() + " na hráče. Tak ještě jedno, ať to překonáme!";
    }

    private String getMatchWithLowestAverageBeers(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejméně průměrnými pivy, protože si zatím nikdo pivo nedal!!!";
        }
        AverageNumberTotalNumber lowestAverage = new AverageNumberTotalNumber(10000,0);
        MatchDTO matchDTO = null;
        for (BeerDetailedDTO beer : beerList) {
            AverageNumberTotalNumber averageNumberTotalNumber = new AverageNumberTotalNumber(beer.getBeerNumber(), beer.getMatch().getPlayerIdList().size());
            if (averageNumberTotalNumber.getAverage() < lowestAverage.getAverage() || matchDTO == null) {
                lowestAverage = averageNumberTotalNumber;
                matchDTO = beer.getMatch();
            }
        }
        MatchHelper matchHelper = new MatchHelper(matchDTO);
        return "Ostudný den Liščího Trusu, kdy byl pokořen rekord v nejnižším průměru počtu vypitých piv v zápase proběhl na zápase " + matchHelper.getMatchWithOpponentNameAndDate() +
                ". Vypilo se " + lowestAverage.getTotalNumber1() + " piv v " + lowestAverage.getTotalNumber2()
                + " lidech, což dělá průměr " + lowestAverage.getAverage() + " na hráče. Vzpomeňte si na to, až si budete objednávat další rundu!";
    }

    private String getHighestAttendanceInMatch(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejvyšší účastí, protože se zatím žádný nehrál";
        }
        beerList.sort(new OrderBeerByAttendance(true));
        BeerDetailedDTO returnBeer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(returnBeer.getMatch());
        return "Největší účast Liščího Trusu proběhla na zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kdy celkový počet účastníků byl " + returnBeer.getMatch().getPlayerIdList().size() + "." +
                " Celkově se v tomto zápase vypilo " + returnBeer.getBeerNumber() + " piv a " + returnBeer.getLiquorNumber() + " panáků.";
    }

    private String getLowestAttendanceInMatch(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejnižší účastí, protože se zatím žádný nehrál";
        }
        beerList.sort(new OrderBeerByAttendance(false));
        BeerDetailedDTO returnBeer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(returnBeer.getMatch());
        return "Nejnižší účast Liščího Trusu proběhla na zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kdy celkový počet účastníků byl " + returnBeer.getMatch().getPlayerIdList().size() + "." +
                " Celkově se v tomto zápase vypilo " + returnBeer.getBeerNumber() + " piv a " + returnBeer.getLiquorNumber() + " panáků.";
    }

    private String getAverageNumberOfBeersInHomeAndAwayMatch(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (beerList.isEmpty()) {
            return "Nelze pivka, protože si zatím nikdo žádný nedal!!!";
        }
        int homeMatches = 0;
        int awayMatches = 0;
        HashMap<Boolean, BeerDetailedDTO> homeMap = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            if (beer.getMatch().isHome()) {
                homeMatches++;
            }
            else {
                awayMatches++;
            }
            if (!homeMap.containsKey(beer.getMatch().isHome())) {
                homeMap.put(beer.getMatch().isHome(), beer);
            }
            else {
                BeerDetailedDTO oldBeer = homeMap.get(beer.getMatch().isHome());
                oldBeer.addBeers(beer.getBeerNumber());
                oldBeer.addLiquors(beer.getLiquorNumber());
                homeMap.put(beer.getMatch().isHome(), oldBeer);
            }
        }
        List<BeerDetailedDTO> homeBeerList = new ArrayList<>(homeMap.values().stream().toList());
        homeBeerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        boolean homeMatch = homeBeerList.get(0).getMatch().isHome();
        float homeAverage = 0;
        float awayAverage = 0;
        if (homeMatch) {
            if (homeMatches != 0) {
                homeAverage = (float) homeBeerList.get(0).getBeerNumber() / homeMatches;
            }
            if (awayMatches != 0) {
                awayAverage = (float) homeBeerList.get(1).getBeerNumber() / homeMatches;
            }
        }
        else {
            if (homeMatches != 0) {
                homeAverage = (float) homeBeerList.get(1).getBeerNumber() / homeMatches;
            }
            if (awayMatches != 0) {
                awayAverage = (float) homeBeerList.get(0).getBeerNumber() / homeMatches;
            }
        }
        String response = "Průměrně se na domácím zápase vypije " + homeAverage + " piv, oproti venkovním zápasům, kde je průměr " + awayAverage + " piv na zápas. ";
        if (homeMatch) {
            return response + "Tak to má být, soupeř musí prohrávat o 2 piva už u Průhonic!";
        }
        return response + "Není načase změnit domácí hospodu?";
    }

    private String getMatchWithBirthday() {
        List<MatchDTO> matchesWithBirthday = new ArrayList<>();
        List<PlayerDTO> playersWithBirthday = new ArrayList<>();
        for (MatchDTO match : matchService.getAll(new MatchFilter())) {
            for (PlayerDTO player : playerService.getAll(1000)) {
                if (isSameDate(player, match)) {
                    matchesWithBirthday.add(match);
                    playersWithBirthday.add(player);
                }
            }
        }
        //pokud se nikdo nenašel v první iteraci, tedy žádné narozky se nekrejou s datem zápasu
        if (matchesWithBirthday.isEmpty()) {
            return "Zatím se nenašel zápas kde by nějaký hráč zapíjel narozky. Už se všichni těšíme";
        } else {
            //nejprve zjišťujeme, zda hráč, který měl narozky v den zápasu, skutečně byl na zápasu přítomen
            List<MatchDTO> returnMatches = new ArrayList<>();
            List<PlayerDTO> returnPlayers = new ArrayList<>();
            for (int i = 0; i < matchesWithBirthday.size(); i++) {
                if (matchesWithBirthday.get(i).getPlayerIdList().contains(playersWithBirthday.get(i).getId())) {
                    returnPlayers.add(playersWithBirthday.get(i));
                    returnMatches.add(matchesWithBirthday.get(i));
                }
            }
            //Pokud nikdo s narozkama nebyl na zápase trusu, tak uděláme stěnu hamby
            if (returnMatches.isEmpty()) {
                StringBuilder result =
                        new StringBuilder("Zatím se nenašel zápas kde by nějaký hráč zapíjel narozky. Už se všichni těšíme. Do té doby zde máme zeď hamby pro hráče, kteří raději své narozeniny zapíjeli jinde než na Trusu. Hamba! : ");
                for (int i = 0; i < playersWithBirthday.size(); i++) {
                    result.append(playersWithBirthday.get(i).getName());
                    if (i == playersWithBirthday.size() - 1) {
                        result.append(".");
                    } else if (i == playersWithBirthday.size() - 2) {
                        result.append(" a ");
                    } else {
                        result.append(", ");
                    }
                }
                return result.toString();
            }
            //Pokud byl jenom jeden takový šťastlivec
            else if (returnMatches.size() == 1) {
                MatchHelper matchHelper = new MatchHelper(returnMatches.get(0));
                PlayerDTO playerDTO = returnPlayers.get(0);
                List<BeerDTO> beerDTOS = beerService.getAll(new BeerFilter(returnMatches.get(0).getId(), playerDTO.getId()));
                BeerDTO beerDTO;
                if (!beerDTOS.isEmpty()) {
                    beerDTO = beerDTOS.get(0);
                }
                else {
                    beerDTO = new BeerDTO();
                    beerDTO.setBeerNumber(0);
                    beerDTO.setLiquorNumber(0);
                }
                return "Zatím jediný zápas, kdy někdo z Trusu zapíjel narozky byl " + matchHelper.getMatchWithOpponentNameAndDate() + " kdy slavil " +  playerDTO.getName() + ", " +
                "který vypil " + beerDTO.getBeerNumber() + " piv a " + beerDTO.getLiquorNumber() + " panáků.";
            }
            //pokud jich bylo víc
            else {
                List<MatchHelper> matchHelpers = new ArrayList<>();
                for (MatchDTO matchDTO : returnMatches) {
                    matchHelpers.add(new MatchHelper(matchDTO));
                }
                StringBuilder result =
                        new StringBuilder("Velká společenská událost v podobě oslavy narozek se konala na zápasech ");
                for (int i = 0; i < returnMatches.size(); i++) {
                    PlayerDTO playerDTO = returnPlayers.get(i);
                    List<BeerDTO> beerDTOS = beerService.getAll(new BeerFilter(returnMatches.get(i).getId(), playerDTO.getId()));
                    BeerDTO beerDTO;
                    if (!beerDTOS.isEmpty()) {
                        beerDTO = beerDTOS.get(0);
                    }
                    else {
                        beerDTO = new BeerDTO();
                        beerDTO.setBeerNumber(0);
                        beerDTO.setLiquorNumber(0);
                    }
                    result.append(matchHelpers.get(i).getMatchWithOpponentNameAndDate()).append(", kdy slavil narozky ").append(playerDTO.getName()).append(", který vypil ").append(beerDTO.getBeerNumber()).append(" piv a ").append(beerDTO.getLiquorNumber()).append(" panáků");
                    if (i == returnMatches.size() - 1) {
                        result.append(".");
                    } else if (i == returnMatches.size() - 2) {
                        result.append(" a proti ");
                    } else {
                        result.append(". Proti ");
                    }
                }
                return result.toString();
            }
        }
    }



    ////////////////////////////tvrdej

    private String getPlayerWithMostLiquors(BeerDetailedResponse allBeerDetailedResponseForPlayer) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForPlayer.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Zatím se hledá člověk, co by si dal alespoň jednoho panáka na zápase Trusu. Výherce čeká věčná sláva zvěčněná přímo v této zajímavosti do té doby, než si někdo dá panáky 2";
        }
        BeerDetailedDTO beer = beerList.get(0);
        return "Nejvíce panáků za historii si dal " + beer.getPlayer().getName() + ", který vypil " + beer.getLiquorNumber() + " frťanů. Na výkonu to vůbec není poznat, gratulujeme!";
    }

    private String getMatchWithMostLiquors(BeerDetailedResponse allBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Nelze najít zápas s nejvíce panáky, protože si zatím nikdo žádný nedal! Jedna šťopička nikdy nikoho nezabila, tak se neostýchejte pánové!";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce panáků za historii padlo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + " s celkovým počtem " + beer.getLiquorNumber() + " frťanů. Copak to asi bylo za oslavu?";
    }

    private String getNumberOfLiquorsInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch) {
        if (currentSeasonBeerDetailedResponseForMatch.getTotalLiquors() == 0) {
            return "Zatím se hledá odvážlivec, co by si dal v aktuální sezoně " + seasonService.getCurrentSeason().getName() + " alespoň jednoho panáka na zápase Trusu." +
                    " Výherce čeká věčná sláva zvěčněná přímo v této zajímavosti do té doby, než si někdo dá panáky 2";
        }
        return "V aktuální sezoně " + seasonService.getCurrentSeason().getName() + " se vypilo celkem " + currentSeasonBeerDetailedResponseForMatch.getTotalLiquors() + " panáků.";
    }

    private String getMatchWithMostLiquorsInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = currentSeasonBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Nelze najít zápas s nejvíce panáky v této sezoně " + seasonService.getCurrentSeason().getName() + ", protože si zatím nikdo žádný nedal! Aspoň jeden Liščí Trus denně je prospěšný pro zdraví, to vám potvrdí každý doktor";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce panáků v aktuální sezoně " + seasonService.getCurrentSeason().getName() + " padlo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kdy proběhlo celkem " + beer.getLiquorNumber() + " kořalek.";
    }

    private String getSeasonWithMostLiquors(BeerDetailedResponse allBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = allBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Sezona s nejvíce panáky je... žádná! Zatím si nikdo nedal ani jeden Liščí Trus!!";
        }
        HashMap<Long, BeerDetailedDTO> seasonMap = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            if (!seasonMap.containsKey(beer.getMatch().getSeasonId())) {
                seasonMap.put(beer.getMatch().getSeasonId(), beer);
            }
            else {
                BeerDetailedDTO oldBeer = seasonMap.get(beer.getMatch().getSeasonId());
                oldBeer.addBeers(beer.getBeerNumber());
                oldBeer.addLiquors(beer.getLiquorNumber());
                seasonMap.put(beer.getMatch().getSeasonId(), oldBeer);
            }
        }
        List<BeerDetailedDTO> seasonBeerList = new ArrayList<>(seasonMap.values().stream().toList());
        seasonBeerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        Long seasonId  = seasonBeerList.get(0).getMatch().getSeasonId();
        StatisticsFilter filter = new StatisticsFilter(null, null, seasonId, true);
        BeerDetailedResponse seasonBeerDetail = beerService.getAllDetailed(filter);
        SeasonDTO season = seasonService.getSeason(seasonId);
        return "Nejvíce panáků se vypilo v sezoně " + season.getName() + ", kdy se v " + seasonBeerDetail.getMatchesCount() + " zápasech vypilo " + seasonBeerDetail.getTotalLiquors() + " tvrdýho.";
    }

    private String getAverageNumberOfLiquorsInMatchForPlayersAndFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) getAllBeerDetailedResponseForPlayer.getTotalLiquors() /getAllBeerDetailedResponseForPlayer.getMatchesCount()/getAllBeerDetailedResponseForPlayer.getPlayersCount();
        return "Za celou historii průměrně každý hráč a fanoušek Trusu, který byl přítomen u třetího poločasu, vypil " + averageNumber + " panáků za zápas";
    }

    private String getAverageNumberOfLiquorsInMatchForPlayers(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = returnNumberOfBeersForFansOrPlayers(false, false, getAllBeerDetailedResponseForPlayer).getAverage() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Za celou historii průměrně každý hráč Trusu, který byl po zápase v hospodě, vypil " + averageNumber + " panáků za zápas";
    }

    private String getAverageNumberOfLiquorsInMatchForFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = returnNumberOfBeersForFansOrPlayers(true, false, getAllBeerDetailedResponseForPlayer).getAverage() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Za celou historii průměrně každý fanoušek Trusu, který byl po zápase v hospodě, vypil " + averageNumber + " panáků za zápas";
    }

    private String getAverageNumberOfLiquorsInMatch(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) getAllBeerDetailedResponseForPlayer.getTotalLiquors() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně se v každém zápase trusu vypije " + averageNumber + " panáků";
    }

    private String getMatchWithHighestAverageLiquors(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        AverageNumberTotalNumber highestAverage = new AverageNumberTotalNumber(0,0);
        MatchDTO matchDTO = null;
        for (BeerDetailedDTO beer : beerList) {
            AverageNumberTotalNumber averageNumberTotalNumber = new AverageNumberTotalNumber(beer.getLiquorNumber(), beer.getMatch().getPlayerIdList().size());
            if (averageNumberTotalNumber.getAverage() > highestAverage.getAverage() || matchDTO == null) {
                highestAverage = averageNumberTotalNumber;
                matchDTO = beer.getMatch();
            }
        }
        if (beerList.isEmpty() || highestAverage.getTotalNumber1() == 0) {
            return "Nejvyšší průměr vypitých panáků na zápase Trusu je 0!! Nechce někdo vyměnit pivko za lahodný Liščí Trus?!?";
        }
        MatchHelper matchHelper = new MatchHelper(matchDTO);

        return "Nejvyšší průměr počtu vypitých panáků v zápase proběhl na zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ". Vypilo se " + highestAverage.getTotalNumber1() + " tvrdýho v " + highestAverage.getTotalNumber2()
                + " lidech, což dělá průměr " + highestAverage.getAverage() + " na hráče. Copak se asi tehdy slavilo?";
    }

    private String getMatchWithLowestAverageLiquors(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        AverageNumberTotalNumber lowestAverage = new AverageNumberTotalNumber(10000,0);
        MatchDTO matchDTO = null;
        int beerNumber = 0;
        for (BeerDetailedDTO beer : beerList) {
            AverageNumberTotalNumber averageNumberTotalNumber = new AverageNumberTotalNumber(beer.getLiquorNumber(), beer.getMatch().getPlayerIdList().size());
            if (averageNumberTotalNumber.getAverage() < lowestAverage.getAverage() || matchDTO == null) {
                lowestAverage = averageNumberTotalNumber;
                matchDTO = beer.getMatch();
                beerNumber = beer.getBeerNumber();
            }
        }
        if (beerList.isEmpty() || lowestAverage.getTotalNumber1() == 10000) {
            return "Zatím v žádném zápase Trusu nepadl jediný panák. To nikdo neslyšel o blahodárném drinku jménem Liščí Trus?";
        }
        MatchHelper matchHelper = new MatchHelper(matchDTO);
        return "Můžete sami posoudit, jaká ostuda se stala v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + " kdy se historicky vypil nejmenší průměr panáků." +
                " Vypilo se " + lowestAverage.getTotalNumber1() + " kořalek v " + lowestAverage.getTotalNumber2()
                + " lidech, což dělá průměr " + lowestAverage.getAverage() + " na hráče. Možná to může zachránit počet piv v zápase, který byl " + beerNumber +
                ". Vzpomeňte si na to, až si budete objednávat další rundu, ideálně slavný Liščí Trus!";
    }

    private String getAverageNumberOfLiquorsInHomeAndAwayMatch(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Nelze porovnat panáky, protože si zatím nikdo žádný nedal!!!";
        }
        int homeMatches = 0;
        int awayMatches = 0;
        HashMap<Boolean, BeerDetailedDTO> homeMap = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            if (beer.getMatch().isHome()) {
                homeMatches++;
            }
            else {
                awayMatches++;
            }
            if (!homeMap.containsKey(beer.getMatch().isHome())) {
                homeMap.put(beer.getMatch().isHome(), beer);
            }
            else {
                BeerDetailedDTO oldBeer = homeMap.get(beer.getMatch().isHome());
                oldBeer.addBeers(beer.getBeerNumber());
                oldBeer.addLiquors(beer.getLiquorNumber());
                homeMap.put(beer.getMatch().isHome(), oldBeer);
            }
        }
        List<BeerDetailedDTO> homeBeerList = new ArrayList<>(homeMap.values().stream().toList());
        homeBeerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        boolean homeMatch = homeBeerList.get(0).getMatch().isHome();
        float homeAverage = 0;
        float awayAverage = 0;
        if (homeMatch) {
            if (homeMatches != 0) {
                homeAverage = (float) homeBeerList.get(0).getLiquorNumber() / homeMatches;
            }
            if (awayMatches != 0) {
                awayAverage = (float) homeBeerList.get(1).getLiquorNumber() / homeMatches;
            }
        }
        else {
            if (homeMatches != 0) {
                homeAverage = (float) homeBeerList.get(1).getLiquorNumber() / homeMatches;
            }
            if (awayMatches != 0) {
                awayAverage = (float) homeBeerList.get(0).getLiquorNumber() / homeMatches;
            }
        }
        String response = "Průměrně se na domácím zápase vypije " + homeAverage + " panáků, oproti venkovním zápasům, kde je průměr " + awayAverage + " panáků na zápas. ";
        if (homeMatch) {
            return response + "Zdá se, že při domácím zápase se chlastá daleko líp než venku!";
        }
        return response + "Ve školce snad nalejvá kuchař i panáky?";
    }









    ///////////////////////pokuty

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

    private String getNumberOfFinesInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason().getName() + " se rozdalo již " + currentSeasonFineDetailedResponseForMatch.getFinesNumber() + " pokut.";
    }

    private String getNumberOfFinesAmountInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason().getName() + " se na pokutách vybralo již " + currentSeasonFineDetailedResponseForMatch.getFinesAmount() + " Kč.";
    }

    private String getMatchWithMostFinesInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = currentSeasonFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze najít zápas s nejvíce pokutami odehraný v této sezoně " + seasonService.getCurrentSeason().getName() + ", protože se zatím žádná pokuta neudělila!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Nejvíce pokut v aktuální sezoně " + seasonService.getCurrentSeason().getName() + " se rozdalo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ". Konečné číslo počtu pokut zní " + fine.getFineNumber() + ".";
    }

    private String getMatchWithMostFinesAmountInCurrentSeason(ReceivedFineDetailedResponse currentSeasonFineDetailedResponseForMatch) {
        List<ReceivedFineDetailedDTO> fineList = currentSeasonFineDetailedResponseForMatch.getFineList();
        fineList.sort(new OrderReceivedFineDetailedDTOByFineNumber());
        if (fineList.isEmpty()) {
            return "Nelze najít zápas, kde se v této sezoně " + seasonService.getCurrentSeason().getName() + ", nejvíce cálovalo za pokuty, protože se zatím žádná pokuta neudělila!";
        }
        ReceivedFineDetailedDTO fine = fineList.get(0);
        MatchHelper matchHelper = new MatchHelper(fine.getMatch());
        return "Největší objem peněz v aktuální sezoně " + seasonService.getCurrentSeason().getName() + " přinesl zápas " + matchHelper.getMatchWithOpponentNameAndDate() + ", kde se vybralo " + fine.getFineAmount() + " Kč.";
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


    private AverageNumberTotalNumber returnNumberOfBeersForFansOrPlayers(boolean fan, boolean beer, BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForPlayer.getBeerList();
        int beerNumber = 0;
        int fanNumber = 0;
        for (BeerDetailedDTO beerDetailedDTO : beerList) {
            if (fan && beerDetailedDTO.getPlayer().isFan()) {
                if (beer) {
                    beerNumber += beerDetailedDTO.getBeerNumber();
                }
                else {
                    beerNumber += beerDetailedDTO.getLiquorNumber();
                }
                fanNumber ++;
            } else if (!fan && !beerDetailedDTO.getPlayer().isFan()) {
                if (beer) {
                    beerNumber += beerDetailedDTO.getBeerNumber();
                }
                else {
                    beerNumber += beerDetailedDTO.getLiquorNumber();
                }
                fanNumber ++;
            }
        }
        return new AverageNumberTotalNumber(beerNumber, fanNumber);
    }

    private boolean isSameDate(PlayerDTO playerDTO, MatchDTO matchDTO) {
        Calendar playerCalendar = Calendar.getInstance();
        playerCalendar.setTime(playerDTO.getBirthday());
        Calendar matchCalendar = Calendar.getInstance();
        matchCalendar.setTime(matchDTO.getDate());
        return matchCalendar.get(Calendar.DAY_OF_MONTH) == playerCalendar.get(Calendar.DAY_OF_MONTH)
                && matchCalendar.get(Calendar.MONTH) == playerCalendar.get(Calendar.MONTH);
    }
}
