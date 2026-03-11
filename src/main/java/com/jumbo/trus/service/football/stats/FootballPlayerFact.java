package com.jumbo.trus.service.football.stats;


import com.jumbo.trus.dto.football.FootballMatchDTO;
import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.football.FootballMatchPlayerEntity;
import com.jumbo.trus.mapper.football.FootballMatchMapper;
import com.jumbo.trus.repository.football.FootballMatchPlayerRepository;
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

    public List<StringAndString> getFactsForPlayer(Long footballPlayerId, AppTeamEntity appTeamEntity) {
        List<StringAndString> statsList = new ArrayList<>();
        addIfNotNull(statsList, mostGoalsInOneMatch(footballPlayerId));
        addIfNotNull(statsList, bestRefereeByBestPlayer(footballPlayerId));
        addIfNotNull(statsList, bestStadiumByGoals(footballPlayerId));
        addIfNotNull(statsList, bestOpponentByGoals(footballPlayerId, appTeamEntity));
        return statsList;
    }

    private void addIfNotNull(List<StringAndString> list, StringAndString value) {
        if (value != null) {
            list.add(value);
        }
    }

    private StringAndString mostGoalsInOneMatch(Long playerId) {
        StringAndString doubleString = new StringAndString();
        List<FootballMatchPlayerEntity> highestGoals = footballMatchPlayerRepository.findAllWithHighestGoals(playerId);
        int goals = highestGoals.get(0).getGoals();
        doubleString.setTitle("Nejvíce gólů v zápase:");
        if (goals != 0) {
            doubleString.setText(goals + " góly v zápase " + formatMatch(footballMatchMapper.toDTO(highestGoals.get(0).getMatch())));
        } else {
            doubleString.setText("-");
        }
        return doubleString;
    }

    private StringAndString bestRefereeByBestPlayer(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageBestPlayersPerReferee(playerId);
        return processStats(
                "Oblíbený rozhodčí: ",
                result,
                "-",
                "-",
                item -> item.getText() + " - " + item.getTotalRoundedInString(2) +" hvězdy utkání/zápas",
                true
        );
    }

    /*private StringAndString worstRefereeByBestPlayer(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findLeastAverageBestPlayersPerReferee(playerId);
        return processStats(
                "Nejhorší průměrné hodnocení dává rozhodčí: ",
                result,
                "Pravděpodobně každý, protože hráč zatím neodehrál ani 3 zápasy pod jedním rozhodčím",
                "Nejspíš každý, protože mu zatím nikdo hvězdu utkání nedal!",
                item -> "Rozhodčí " + item.getText() + "průměrně udělil hvězdu utkání v " + item.getTotalRoundedInString(2) + " zápasech \n",
                false
        );
    }*/

    private StringAndString bestStadiumByGoals(Long playerId) {
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageGoalsPerStadium(playerId);
        return processStats(
                "Oblíbený stadion:",
                result,
                "-",
                "-",
                item -> item.getText() + " - " + item.getTotalRoundedInString(2) + " gólů/zápas",
                true
        );
    }

    private StringAndString bestOpponentByGoals(Long playerId, AppTeamEntity appTeamEntity) {
        Long teamId = appTeamEntity.getTeam().getId();
        List<Object[]> result = footballMatchPlayerRepository.findMostAverageGoalsPerOpponent(teamId, playerId);
        return processStats(
                "Oblíbený soupeř:",
                result,
                "-",
                "-",
                item -> item.getText() + " - " + item.getTotalRoundedInString(2) + " gólů/zápas",
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
            return null;
        } else if (resultList.stream().allMatch(item -> item.getTotal() == 0)) {
            return null;
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
