package com.jumbo.trus.service.fact;

import com.jumbo.trus.dto.SeasonDTO;
import com.jumbo.trus.dto.beer.BeerDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.match.MatchDTO;
import com.jumbo.trus.dto.match.MatchHelper;
import com.jumbo.trus.dto.player.PlayerDTO;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.MatchFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.MatchService;
import com.jumbo.trus.service.SeasonService;
import com.jumbo.trus.service.beer.BeerService;
import com.jumbo.trus.service.helper.AverageNumberTotalNumber;
import com.jumbo.trus.service.helper.Pair;
import com.jumbo.trus.service.order.OrderBeerByAttendance;
import com.jumbo.trus.service.order.OrderBeerByBeerOrLiquorNumber;
import com.jumbo.trus.service.player.PlayerService;
import com.jumbo.trus.service.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BeerFactService {

    private final PlayerService playerService;
    private final SeasonService seasonService;
    private final BeerService beerService;
    private final MatchService matchService;

    public List<String> returnBeerFacts(StatisticsFilter allSeasonPlayerFilter, StatisticsFilter allSeasonMatchFilter, StatisticsFilter currentSeasonMatchFilter) {
        SeasonDTO currentSeason = seasonService.getCurrentSeason(true, allSeasonPlayerFilter.getAppTeam());
        List<String> beerFacts = new ArrayList<>();
        beerFacts.add(getPlayerWithMostBeers(beerService.getAllDetailed(allSeasonPlayerFilter))); //1
        beerFacts.add(getMatchWithMostBeers(beerService.getAllDetailed(allSeasonMatchFilter))); //2
        beerFacts.add(getNumberOfBeersInCurrentSeason(beerService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//3
        beerFacts.add(getMatchWithMostBeersInCurrentSeason(beerService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//4
        beerFacts.add(getSeasonWithMostBeers(beerService.getAllDetailed(allSeasonMatchFilter)));//5
        beerFacts.add(getAverageNumberOfBeersInMatchForPlayersAndFans(beerService.getAllDetailed(allSeasonMatchFilter)));//6
        beerFacts.add(getAverageNumberOfBeersInMatchForPlayers(beerService.getAllDetailed(allSeasonPlayerFilter)));//7
        beerFacts.add(getAverageNumberOfBeersInMatchForFans(beerService.getAllDetailed(allSeasonPlayerFilter)));//8
        beerFacts.add(getAverageNumberOfBeersInMatch(beerService.getAllDetailed(allSeasonPlayerFilter)));//9
        beerFacts.add(getMatchWithHighestAverageBeers(beerService.getAllDetailed(allSeasonMatchFilter)));//10
        beerFacts.add(getMatchWithLowestAverageBeers(beerService.getAllDetailed(allSeasonMatchFilter)));//11
        beerFacts.add(getHighestAttendanceInMatch(beerService.getAllDetailed(allSeasonMatchFilter)));//12
        beerFacts.add(getLowestAttendanceInMatch(beerService.getAllDetailed(allSeasonMatchFilter)));//13
        ////panáky
        beerFacts.add(getPlayerWithMostLiquors(beerService.getAllDetailed(allSeasonPlayerFilter))); //14
        beerFacts.add(getMatchWithMostLiquors(beerService.getAllDetailed(allSeasonMatchFilter))); //15
        beerFacts.add(getNumberOfLiquorsInCurrentSeason(beerService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//16
        beerFacts.add(getMatchWithMostLiquorsInCurrentSeason(beerService.getAllDetailed(currentSeasonMatchFilter), currentSeasonMatchFilter.getAppTeam()));//17
        beerFacts.add(getSeasonWithMostLiquors(beerService.getAllDetailed(allSeasonMatchFilter)));//18
        beerFacts.add(getAverageNumberOfLiquorsInMatchForPlayersAndFans(beerService.getAllDetailed(allSeasonMatchFilter)));//19
        beerFacts.add(getAverageNumberOfLiquorsInMatchForPlayers(beerService.getAllDetailed(allSeasonPlayerFilter)));//20
        beerFacts.add(getAverageNumberOfLiquorsInMatchForFans(beerService.getAllDetailed(allSeasonPlayerFilter)));//21
        beerFacts.add(getAverageNumberOfLiquorsInMatch(beerService.getAllDetailed(allSeasonPlayerFilter)));//22
        beerFacts.add(getMatchWithHighestAverageLiquors(beerService.getAllDetailed(allSeasonMatchFilter)));//23
        beerFacts.add(getMatchWithLowestAverageLiquors(beerService.getAllDetailed(allSeasonMatchFilter)));//24
        //pivo
        beerFacts.add(getAverageNumberOfBeersInHomeAndAwayMatch(beerService.getAllDetailed(allSeasonMatchFilter), true));//25
        //panák
        beerFacts.add(getAverageNumberOfBeersInHomeAndAwayMatch(beerService.getAllDetailed(allSeasonMatchFilter), false));//26
        //narozky
        beerFacts.add(getMatchWithBirthday(allSeasonMatchFilter.getAppTeam()));//27
        return beerFacts;
    }

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

    private String getNumberOfBeersInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch, AppTeamEntity appTeam) {
        return "V aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " se vypilo celkem " + currentSeasonBeerDetailedResponseForMatch.getTotalBeers() + " piv.";
    }

    private String getMatchWithMostBeersInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch, AppTeamEntity appTeam) {
        List<BeerDetailedDTO> beerList = currentSeasonBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(true));
        if (beerList.isEmpty()) {
            return "Nelze najít zápas s nejvíce pivy v této sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + ", protože si zatím nikdo žádný nedal!";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce piv v aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " bylo vypito v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kde padlo " + beer.getBeerNumber() + " piv.";
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

    private String getAverageNumberOfBeersInMatchForPlayersAndFans(BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        float averageNumber = returnAverageBeerNumber(true, getAllBeerDetailedResponseForMatch).getAverage();
        return "Průměrně si každý hráč či fanoušek dá po zápase " + averageNumber + " piv";
    }

    private String getAverageNumberOfBeersInMatchForPlayers(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) returnNumberOfBeersForFansOrPlayers(false, true, getAllBeerDetailedResponseForPlayer).getTotalNumber1() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně po každém zápase hráči Trusu vypijí " + averageNumber + " piv";
    }

    private String getAverageNumberOfBeersInMatchForFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) returnNumberOfBeersForFansOrPlayers(true, true, getAllBeerDetailedResponseForPlayer).getTotalNumber1() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně po každém zápase fanoušci Trusu vypijí " + averageNumber + " piv";
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


    private String getAverageNumberOfBeersInHomeAndAwayMatch(BeerDetailedResponse getAllBeerDetailedResponseForMatch, boolean isBeer) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        if (!isBeer) {
            beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        }
        if (beerList.isEmpty() || (!isBeer && beerList.get(0).getLiquorNumber() == 0)) {
            return isBeer ? "Nelze pivka, protože si zatím nikdo žádný nedal!!!" : "Nelze porovnat panáky, protože si zatím nikdo žádný nedal!!!";
        }
        HashMap<Boolean, Integer> matchCount = new HashMap<>();
        HashMap<Boolean, Integer> beerSum = new HashMap<>();
        for (BeerDetailedDTO beer : beerList) {
            boolean isHome = beer.getMatch().isHome();
            matchCount.put(isHome, matchCount.getOrDefault(isHome, 0) + 1);
            beerSum.put(isHome, beerSum.getOrDefault(isHome, 0) + (isBeer ? beer.getBeerNumber() : beer.getLiquorNumber()));
        }
        Util util = new Util();
        float homeAvg = util.getAverage(beerSum.get(true), matchCount.get(true));
        float awayAvg = util.getAverage(beerSum.get(false), matchCount.get(false));
        return averageResponse(homeAvg, awayAvg, isBeer);
    }

    private String averageResponse(float homeAvg, float awayAvg, boolean isBeer) {
        String beerOrLiquor = isBeer ? "piv" : "panáků";
        String response = "Průměrně se na domácím zápase vypije " + homeAvg + " " + beerOrLiquor + ", oproti venkovním zápasům, kde je průměr " + awayAvg + " " + beerOrLiquor + " na zápas. ";
        return response + (isBeer ? (homeAvg > awayAvg ? "Tak to má být, soupeř musí prohrávat o 2 piva už u Průhonic!" : "Není načase změnit domácí hospodu?") :
                (homeAvg > awayAvg ? "Zdá se, že při domácím zápase se chlastá daleko líp než venku!" : "Ve školce snad nalejvá kuchař i panáky?"));
    }

    private String getMatchWithBirthday(AppTeamEntity appTeam) {
        List<MatchDTO> matches = matchService.getAll(new MatchFilter(appTeam));
        List<PlayerDTO> players = playerService.getAll(appTeam.getId());

        List<Pair<MatchDTO, PlayerDTO>> matchesWithBirthday = matches.stream()
                .flatMap(m -> players.stream().filter(p -> isSameDate(p, m)).map(p -> new Pair<>(m, p)))
                .toList();

        if (matchesWithBirthday.isEmpty()) {
            return getStringBuilder(players).toString();
        }

        List<Pair<MatchDTO, PlayerDTO>> attended = matchesWithBirthday.stream()
                .filter(pair -> pair.getFirst().getPlayerIdList().contains(pair.getSecond().getId()))
                .toList();

        if (attended.isEmpty()) {
            return getStringBuilder(matchesWithBirthday.stream().map(Pair::getSecond).toList()).toString();
        } else if (attended.size() == 1) {
            return formatSingleBirthdayMatch(attended.get(0), appTeam);
        } else {
            return formatMultipleBirthdayMatches(attended, appTeam);
        }
    }

    private String formatSingleBirthdayMatch(Pair<MatchDTO, PlayerDTO> pair, AppTeamEntity appTeam) {
        MatchHelper helper = new MatchHelper(pair.getFirst());
        BeerDTO beer = beerService.getAll(new BeerFilter(pair.getFirst().getId(), pair.getSecond().getId())).stream().findFirst().orElse(new BeerDTO(0, 0));
        return "Zatím jediný zápas, kdy někdo z Trusu zapíjel narozky byl " + helper.getMatchWithOpponentNameAndDate() + " kdy slavil " + pair.getSecond().getName() + ", který vypil " + beer.getBeerNumber() + " piv a " + beer.getLiquorNumber() + " panáků.";
    }

    private String formatMultipleBirthdayMatches(List<Pair<MatchDTO, PlayerDTO>> pairs, AppTeamEntity appTeam) {
        StringBuilder result = new StringBuilder("Velká společenská událost v podobě oslavy narozek se konala na zápasech ");
        for (int i = 0; i < pairs.size(); i++) {
            MatchDTO match = pairs.get(i).getFirst();
            PlayerDTO player = pairs.get(i).getSecond();
            BeerDTO beer = beerService.getAll(new BeerFilter(match.getId(), player.getId())).stream().findFirst().orElse(new BeerDTO(0, 0));
            result.append(new MatchHelper(match).getMatchWithOpponentNameAndDate())
                    .append(", kdy slavil narozky ").append(player.getName())
                    .append(", který vypil ").append(beer.getBeerNumber()).append(" piv a ")
                    .append(beer.getLiquorNumber()).append(" panáků");
            if (i == pairs.size() - 1) result.append(".");
            else result.append(i == pairs.size() - 2 ? " a proti " : ". Proti ");
        }
        return result.toString();
    }

    private StringBuilder getStringBuilder(List<PlayerDTO> playersWithBirthday) {
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
        return result;
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

    private String getNumberOfLiquorsInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch, AppTeamEntity appTeam) {
        if (currentSeasonBeerDetailedResponseForMatch.getTotalLiquors() == 0) {
            return "Zatím se hledá odvážlivec, co by si dal v aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " alespoň jednoho panáka na zápase Trusu." +
                    " Výherce čeká věčná sláva zvěčněná přímo v této zajímavosti do té doby, než si někdo dá panáky 2";
        }
        return "V aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " se vypilo celkem " + currentSeasonBeerDetailedResponseForMatch.getTotalLiquors() + " panáků.";
    }

    private String getMatchWithMostLiquorsInCurrentSeason(BeerDetailedResponse currentSeasonBeerDetailedResponseForMatch, AppTeamEntity appTeam) {
        List<BeerDetailedDTO> beerList = currentSeasonBeerDetailedResponseForMatch.getBeerList();
        beerList.sort(new OrderBeerByBeerOrLiquorNumber(false));
        if (beerList.isEmpty() || beerList.get(0).getLiquorNumber() == 0) {
            return "Nelze najít zápas s nejvíce panáky v této sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + ", protože si zatím nikdo žádný nedal! Aspoň jeden Liščí Trus denně je prospěšný pro zdraví, to vám potvrdí každý doktor";
        }
        BeerDetailedDTO beer = beerList.get(0);
        MatchHelper matchHelper = new MatchHelper(beer.getMatch());
        return "Nejvíce panáků v aktuální sezoně " + seasonService.getCurrentSeason(true, appTeam).getName() + " padlo v zápase " + matchHelper.getMatchWithOpponentNameAndDate() + ", kdy proběhlo celkem " + beer.getLiquorNumber() + " kořalek.";
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
        float averageNumber = returnAverageBeerNumber(false, getAllBeerDetailedResponseForPlayer).getAverage();
        return "Průměrně si každý hráč či fanoušek dá po zápase " + averageNumber + " panáků";
    }

    private String getAverageNumberOfLiquorsInMatchForPlayers(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) returnNumberOfBeersForFansOrPlayers(false, false, getAllBeerDetailedResponseForPlayer).getTotalNumber1() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně po každém zápase hráči Trusu vypijí " + averageNumber + " panáků";
    }

    private String getAverageNumberOfLiquorsInMatchForFans(BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        float averageNumber = (float) returnNumberOfBeersForFansOrPlayers(true, false, getAllBeerDetailedResponseForPlayer).getTotalNumber1() /getAllBeerDetailedResponseForPlayer.getMatchesCount();
        return "Průměrně po každém zápase fanoušci Trusu vypijí " + averageNumber + " panáků";
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
            if ((averageNumberTotalNumber.getAverage() < lowestAverage.getAverage() || matchDTO == null) && beer.getLiquorNumber() != 0) {
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

    private AverageNumberTotalNumber returnNumberOfBeersForFansOrPlayers(boolean fan, boolean beer, BeerDetailedResponse getAllBeerDetailedResponseForPlayer) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForPlayer.getBeerList();
        int beerNumber = 0;
        int fanNumber = 0;
        int playerNumber = 0;
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
                playerNumber ++;
            }
        }
        if (fan) {
            return new AverageNumberTotalNumber(beerNumber, fanNumber);
        }
        return new AverageNumberTotalNumber(beerNumber, playerNumber);
    }

    private AverageNumberTotalNumber returnAverageBeerNumber(boolean beer, BeerDetailedResponse getAllBeerDetailedResponseForMatch) {
        List<BeerDetailedDTO> beerList = getAllBeerDetailedResponseForMatch.getBeerList();
        int playerNumber = 0;
        for (BeerDetailedDTO beerDetailedDTO : beerList) {
            playerNumber+=beerDetailedDTO.getMatch().getPlayerIdList().size();
        }
        if (beer) {
            return new AverageNumberTotalNumber(getAllBeerDetailedResponseForMatch.getTotalBeers(), playerNumber);
        }
        return new AverageNumberTotalNumber(getAllBeerDetailedResponseForMatch.getTotalLiquors(), playerNumber);
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
