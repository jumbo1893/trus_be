package com.jumbo.trus.service.football.stats;


import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.repository.football.FootballMatchPlayerRepository;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.service.football.pkfl.fact.helper.DoubleAndString;
import com.jumbo.trus.service.helper.DateFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class FootballPlayerFact {

    final private FootballMatchPlayerRepository footballMatchPlayerRepository;
    final private FootballMatchMapper footballMatchMapper;

    public List<StringAndString> getFactsForPlayer(Long playerId, AppTeamEntity appTeamEntity) {
        List<StringAndString> statsList = new ArrayList<>();
        statsList.add(mostGoalsInOneMatch(playerId));
        statsList.add(bestRefereeByBestPlayer(playerId));
        statsList.add(worstRefereeByBestPlayer(playerId));
        statsList.add(bestStadiumByGoals(playerId));
        statsList.add(bestOpponentByGoals(playerId, appTeamEntity));
        return statsList;
    }

    private StringAndString mostGoalsInOneMatch(Long playerId) {
        StringAndString doubleString = new StringAndString();
        List<FootballMatchPlayerEntity> highestGoals = footballMatchPlayerRepository.findAllWithHighestGoals(playerId);
        int goals = highestGoals.get(0).getGoals();
        doubleString.setTitle("Nejvíce gólů v jednom zápase: " + goals);
        if (goals != 0) {
            StringBuilder text = new StringBuilder("Zápasy:\n");
            for (FootballMatchPlayerEntity stats : highestGoals) {
                text.append(formatMatch(footballMatchMapper.toDTO(stats.getMatch()))).append("\n");
            }
            doubleString.setText(String.valueOf(text));
        } else {
            doubleString.setText("Tento dřevák zatím gól nevsítil");
        }
        return doubleString;
    }

    private StringAndString bestRefereeByBestPlayer(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageBestPlayersPerReferee(playerId);
        return processStats(
                "Největší miláček rozhodčího: ",
                result,
                "Žádný miláček není, neboť ho žádný rozhodčí nepískal aspoň 3x!",
                "Žádný miláček není, neboť hráč ještě nebyl ani jednou hvězda zápasu!",
                item -> "Rozhodčí " + item.getText() + "průměrně udělil hvězdu utkání v " + item.getTotalRoundedInString(2) + " zápasech \n",
                true
        );
    }

    private StringAndString worstRefereeByBestPlayer(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findLeastAverageBestPlayersPerReferee(playerId);
        return processStats(
                "Nejhorší průměrné hodnocení dává rozhodčí: ",
                result,
                "Pravděpodobně každý, protože hráč zatím neodehrál ani 3 zápasy pod jedním rozhodčím",
                "Nejspíš každý, protože mu zatím nikdo hvězdu utkání nedal!",
                item -> "Rozhodčí " + item.getText() + "průměrně udělil hvězdu utkání v " + item.getTotalRoundedInString(2) + " zápasech \n",
                false
        );
    }

    private StringAndString bestStadiumByGoals(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageGoalsPerStadium(playerId);
        return processStats(
                "Nejlíp se střílí na stadionu: ",
                result,
                "To zatím nevíme, jelikož na žádném stadionu hráč neodehrál aspoň 3 zápasy!",
                "No asi nikde, když hráč ještě nedal gól!",
                item -> "Na hřišti " + item.getText() + " vstřelil hráč průměrně " + item.getTotalRoundedInString(2) + " gólů na zápas\n",
                true
        );
    }

    private StringAndString bestOpponentByGoals(Long playerId, AppTeamEntity appTeamEntity) {
        Long teamId = appTeamEntity.getTeam().getId();
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageGoalsPerOpponent(teamId, playerId);
        return processStats(
                "Nejlíp se střílí proti soupeři: ",
                result,
                "Hráč ještě nehrál proti žádnému soupeři aspoň 2x!",
                "No asi nikde, když hráč ještě nedal gól!",
                item -> "Proti " + item.getText() + " vstřelil hráč průměrně " + item.getTotalRoundedInString(2) + " gólů na zápas\n",
                true
        );
    }

    private StringAndString processStats(String title, List<Object[]> result,
                                         String noDataMessage, String zeroDataMessage,
                                         Function<DoubleAndString, String> formatFunction, boolean deleteZeroResults) {
        StringAndString doubleString = new StringAndString();
        doubleString.setTitle(title);
        List<DoubleAndString> resultList = objectToDoubleAndString(result);

        if (resultList.isEmpty()) {
            doubleString.setText(noDataMessage);
        } else if (resultList.stream().allMatch(item -> item.getTotal() == 0)) {
            doubleString.setText(zeroDataMessage);
        } else {
            StringBuilder text = new StringBuilder();
            resultList.stream()
                    .filter(item -> (!deleteZeroResults || item.getTotal() != 0))
                    .forEach(item -> text.append(formatFunction.apply(item)));
            doubleString.setText(text.toString());
        }

        return doubleString;
    }

    private String formatMatch(FootballMatchDTO match) {
        return String.format("%s %s, %s",
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                DateFormatter.formatDateForFrontend(match.getDate()));
    }

    private List<DoubleAndString> objectToDoubleAndString(List<Object[]> result) {
        List<DoubleAndString> resultList = new ArrayList<>();
        for (Object[] row : result) {
            double total = (Double) row[0];
            String text = (String) row[1];
            resultList.add(new DoubleAndString(total, text));
        }
        return resultList;
    }
}
