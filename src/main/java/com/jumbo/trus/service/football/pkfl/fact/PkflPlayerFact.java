package com.jumbo.trus.service.football.pkfl.fact;


import com.jumbo.trus.dto.helper.StringAndString;
import com.jumbo.trus.dto.pkfl.PkflMatchDTO;
import com.jumbo.trus.entity.pkfl.PkflIndividualStatsEntity;
import com.jumbo.trus.repository.PkflIndividualStatsRepository;
import com.jumbo.trus.mapper.pkfl.PkflMatchMapper;
import com.jumbo.trus.mapper.pkfl.PkflRefereeMapper;
import com.jumbo.trus.service.football.pkfl.fact.helper.DoubleAndString;
import com.jumbo.trus.service.helper.DateFormatter;

import java.util.ArrayList;
import java.util.List;

public class PkflPlayerFact {


    final private PkflIndividualStatsRepository pkflIndividualStatsRepository;

    final private PkflMatchMapper pkflMatchMapper;

    final private PkflRefereeMapper pkflRefereeMapper;

    final private long playerId;

    public PkflPlayerFact(PkflIndividualStatsRepository pkflIndividualStatsRepository, PkflMatchMapper pkflMatchMapper, PkflRefereeMapper pkflRefereeMapper, long playerId) {
        this.pkflIndividualStatsRepository = pkflIndividualStatsRepository;
        this.pkflMatchMapper = pkflMatchMapper;
        this.pkflRefereeMapper = pkflRefereeMapper;
        this.playerId = playerId;
    }

    public List<StringAndString> getStatsForPlayer() {
        List<StringAndString> statsList = new ArrayList<>();
        statsList.add(mostGoalsInOneMatch());
        statsList.add(bestRefereeByBestPlayer());
        statsList.add(worstRefereeByBestPlayer());
        statsList.add(bestStadiumByGoals());
        statsList.add(bestOpponentByGoals());
        return statsList;
    }

    private StringAndString mostGoalsInOneMatch() {
        StringAndString doubleString = new StringAndString();
        List<PkflIndividualStatsEntity> highestGoals = pkflIndividualStatsRepository.findAllWithHighestGoals(playerId);
        int goals = highestGoals.get(0).getGoals();
        doubleString.setTitle("Nejvíce gólů v jednom zápase: " + goals);
        if (goals != 0) {
            StringBuilder text = new StringBuilder("Zápasy:\n");
            for (PkflIndividualStatsEntity stats : highestGoals) {
                text.append(matchToString(pkflMatchMapper.toDTO(stats.getMatch()))).append("\n");
            }
            doubleString.setText(String.valueOf(text));
        } else {
            doubleString.setText("Tento dřevák zatím gól nevsítil");
        }
        return doubleString;
    }

    private StringAndString bestRefereeByBestPlayer() {
        StringAndString doubleString = new StringAndString();
        List<Object[]> result = pkflIndividualStatsRepository.findMostAverageBestPlayersPerReferee(playerId);
        List<DoubleAndString> resultList = objectToDoubleAndString(result);
        doubleString.setTitle("Největší miláček rozhodčího: ");
        if (resultList.isEmpty() || resultList.get(0).getTotal() == 0) {
            doubleString.setText("Žádný miláček není, hráč ještě nebyl hvězda zápasu, nebo ho žádný rozhodčí nepískal aspoň 3x!");
        }
        else {
            StringBuilder text = new StringBuilder("Rozhodčí:\n");
            for (DoubleAndString intReferee : resultList) {
                text.append(intReferee.getText()).append(" průměrně udělil hvězdu utkání v ").append(intReferee.getTotalRoundedInString(2)).append(" zápasech \n");
            }
            doubleString.setText(String.valueOf(text));

        }
        return doubleString;
    }

    private StringAndString worstRefereeByBestPlayer() {
        StringAndString doubleString = new StringAndString();
        List<Object[]> result = pkflIndividualStatsRepository.findLeastAverageBestPlayersPerReferee(playerId);
        List<DoubleAndString> resultList = objectToDoubleAndString(result);
        doubleString.setTitle("Nejhorší průměrné hodnocení dává rozhodčí: ");
        if (resultList.isEmpty()) {
            doubleString.setText("Pravděpodobně každý, protože hráč zatím neodehrál ani 3 zápasy pod jedním rozhodčím");
        }
        else {
            StringBuilder text = new StringBuilder("Rozhodčí:\n");
            for (DoubleAndString intReferee : resultList) {
                text.append(intReferee.getText()).append(" průměrně udělil hvězdu utkání v ").append(intReferee.getTotalRoundedInString(2)).append(" zápasech \n");
            }
            doubleString.setText(String.valueOf(text));

        }
        return doubleString;
    }

    private StringAndString bestStadiumByGoals() {
        StringAndString doubleString = new StringAndString();
        List<Object[]> result = pkflIndividualStatsRepository.findMostAverageGoalsPerStadium(playerId);
        List<DoubleAndString> resultList = objectToDoubleAndString(result);
        doubleString.setTitle("Nejlíp se střílí na stadionu: ");
        if (resultList.isEmpty() || resultList.get(0). getTotal()== 0) {
            doubleString.setText("Hráč ještě nedal gól, nebo nehrál na žádném stadionu aspoň 3x!");
        }
        else {
            StringBuilder text = new StringBuilder("Stadion:\n");
            for (DoubleAndString doubleAndString : resultList) {
                text.append("Na hřišti ").append(doubleAndString.getText()).append(" vstřelil hráč průměrně ").append(doubleAndString.getTotalRoundedInString(2)).append(" gólů na zápas\n");
            }
            doubleString.setText(String.valueOf(text));

        }
        return doubleString;
    }

    private StringAndString bestOpponentByGoals() {
        StringAndString doubleString = new StringAndString();
        List<Object[]> result = pkflIndividualStatsRepository.findMostAverageGoalsPerOpponent(playerId);
        List<DoubleAndString> resultList = objectToDoubleAndString(result);
        doubleString.setTitle("Nejlíp se střílí proti soupeři: ");
        if (resultList.isEmpty() || resultList.get(0).getTotal() == 0) {
            doubleString.setText("Hráč ještě nedal gól, nebo nehrál proti žádnému soupeři aspoň 2x!");
        }
        else {
            StringBuilder text = new StringBuilder("Soupeř:\n");
            for (DoubleAndString doubleAndString : resultList) {
                if(doubleAndString.getTotal() > 0) {
                    text.append("Proti ").append(doubleAndString.getText()).append(" vstřelil hráč průměrně ").append(doubleAndString.getTotalRoundedInString(2)).append(" gólů na zápas\n");
                }
            }
            doubleString.setText(String.valueOf(text));

        }
        return doubleString;
    }

    private String matchToString(PkflMatchDTO match) {
        String returnMatch;
        if (match.isHomeMatch()) {
            returnMatch = "Liščí Trus - " + match.getOpponent().getName();
        } else {
            returnMatch = match.getOpponent().getName() + " - Liščí Trus";
        }
        returnMatch += ", " + DateFormatter.formatDateForFrontend(match.getDate());
        return returnMatch;
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
